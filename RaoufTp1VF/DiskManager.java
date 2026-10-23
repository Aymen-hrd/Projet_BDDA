import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Gestionnaire de l'espace disque.
 *
 * Organisation du dossier de travail (dmDir) :
 *   - Data0.bin : les pages, stockées les unes après les autres
 *                 (page i à l'offset i * pageSize)
 *   - dm.meta   : les métadonnées du DiskManager
 *                 (pageSize, nombre de pages, liste des pages désallouées)
 */
public class DiskManager implements IDiskManager {
    private static final String DATA_FILE = "Data0.bin";
    private static final String META_FILE = "dm.meta";

    private Path dir;                       // null tant que Init n'a pas réussi
    private int pageSize;
    private int nbPages;                    // nombre total de pages dans Data0.bin
    private Deque<Integer> freePages;       // pages désallouées, réutilisables
    private RandomAccessFile dataFile;
    private boolean initCalled;             // Init ne peut être appelé qu'une fois

    /** Constructeur sans arguments (exigé par le sujet). */
    public DiskManager() {
        this.dir = null;
        this.pageSize = 0;
        this.nbPages = 0;
        this.freePages = new ArrayDeque<>();
        this.initCalled = false;
        this.dataFile = null;

    }

    // =====================================================================
    //  Méthodes de l'interface IDiskManager
    // =====================================================================

    @Override
    public void Init(String dmDir, int pageSize) throws DiskManagerException {
        if (initCalled) {
            throw new DiskManagerException("Init a déjà été appelé sur ce DiskManager");
        }
        

        if (dmDir == null || dmDir.isEmpty()) {
            throw new DiskManagerException("Chemin du dossier de travail invalide");
        }
        if (pageSize <= 0) {
            throw new DiskManagerException("Taille de page invalide : " + pageSize);
        }

        Path d;
        try {
            d = Paths.get(dmDir).toAbsolutePath();
        } catch (InvalidPathException e) {
            throw new DiskManagerException("Chemin invalide : " + dmDir, e);
        }

        // Dossier inexistant -> on le crée ; chemin qui désigne un fichier -> erreur
        if (Files.exists(d) && !Files.isDirectory(d)) {
            throw new DiskManagerException(d + " existe mais n'est pas un dossier");
        }
        try {
            Files.createDirectories(d);
        } catch (IOException e) {
            throw new DiskManagerException("Impossible de créer le dossier " + d, e);
        }

        Path meta = d.resolve(META_FILE);
        Path data = d.resolve(DATA_FILE);

        int loadedNbPages = 0;
        Deque<Integer> loadedFree = new ArrayDeque<>();

        if (Files.exists(meta)) {
            // Le dossier contient déjà des données : on les recharge et on les vérifie
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(meta)))) {
                int savedPageSize = in.readInt();
                if (savedPageSize != pageSize) {
                    throw new DiskManagerException("Taille de page incompatible : dossier = "
                            + savedPageSize + ", demandée = " + pageSize);
                }
                loadedNbPages = in.readInt();
                int nbFree = in.readInt();
                if (loadedNbPages < 0 || nbFree < 0 || nbFree > loadedNbPages) {
                    throw new DiskManagerException("Métadonnées corrompues dans " + meta);
                }
                for (int i = 0; i < nbFree; i++) {
                    int p = in.readInt();
                    if (p < 0 || p >= loadedNbPages || loadedFree.contains(p)) {
                        throw new DiskManagerException("Page libre invalide dans " + meta + " : " + p);
                    }
                    loadedFree.addLast(p);
                }
            } catch (EOFException e) {
                throw new DiskManagerException("Fichier de métadonnées tronqué : " + meta, e);
            } catch (IOException e) {
                throw new DiskManagerException("Lecture impossible de " + meta, e);
            }

            try {
                long expected = (long) loadedNbPages * pageSize;
                long actual = Files.exists(data) ? Files.size(data) : 0;
                if (actual < expected) {
                    throw new DiskManagerException("Fichier de données incomplet : " + actual
                            + " octets au lieu d'au moins " + expected);
                }
            } catch (IOException e) {
                throw new DiskManagerException("Accès impossible à " + data, e);
            }
        } else if (Files.exists(data)) {
            // Des pages sans métadonnées : on ne sait pas les interpréter, on refuse
            throw new DiskManagerException("Le dossier contient " + DATA_FILE
                    + " mais pas " + META_FILE + " : données incohérentes");
        }

        try {
            this.dataFile = new RandomAccessFile(data.toFile(), "rw");
        } catch (IOException e) {
            throw new DiskManagerException("Ouverture impossible de " + data, e);
        }

        this.dir = d;
        this.pageSize = pageSize;
        this.nbPages = loadedNbPages;
        this.freePages = loadedFree;
        initCalled = true;

        // Dossier neuf : on écrit tout de suite les métadonnées (taille de page)
        if (!Files.exists(meta)) {
            Save();
        }
    }

    @Override
    public void Save() throws DiskManagerException {
        checkInitialized();
        Path meta = dir.resolve(META_FILE);
        Path tmp = dir.resolve(META_FILE + ".tmp");
        try {
            // On écrit dans un fichier temporaire puis on le renomme :
            // si le programme plante pendant l'écriture, l'ancien dm.meta reste intact.
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeInt(pageSize);
                out.writeInt(nbPages);
                out.writeInt(freePages.size());
                for (int p : freePages) out.writeInt(p);
            }
            dataFile.getFD().sync();
            Files.move(tmp, meta, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DiskManagerException("Échec de la sauvegarde dans " + dir, e);
        }
    }

    @Override
    public IPageId AllocPage() throws DiskManagerException {
        checkInitialized();
        if (!freePages.isEmpty()) {
            return new PageId(0, freePages.pollFirst());
        }
        if (nbPages == Integer.MAX_VALUE) {
            throw new DiskManagerException("Nombre maximal de pages atteint");
        }
        try {
            dataFile.setLength((long) (nbPages + 1) * pageSize);   // agrandit le fichier
        } catch (IOException e) {
            throw new DiskManagerException("Impossible d'agrandir le fichier de données", e);
        }
        return new PageId(0, nbPages++);
    }

    @Override
    public void ReadPage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
        checkInitialized();
        PageId pid = checkActivePage(ipid);
        if (buffer != null) buffer.clear();
        checkBuffer(buffer);
        FileChannel ch = dataFile.getChannel();
        long offset = (long) pid.getPageIdx() * pageSize;
        // On lit dans une vue limitée à pageSize octets, sans toucher position/limite du buffer
        ByteBuffer dst = buffer.duplicate();
        dst.limit(dst.position() + pageSize);
        try {
            int read = 0;
            while (read < pageSize) {
                int n = ch.read(dst, offset + read);
                if (n < 0) {
                    throw new DiskManagerException("Fin de fichier inattendue en lisant " + pid);
                }
                read += n;
            }
        } catch (IOException e) {
            throw new DiskManagerException("Erreur de lecture de " + pid, e);
        }finally{
            buffer.clear();
        }
    }

    @Override
    public void WritePage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
        checkInitialized();
        PageId pid = checkActivePage(ipid);
        if (buffer != null) buffer.clear();
        checkBuffer(buffer);
        FileChannel ch = dataFile.getChannel();
        long offset = (long) pid.getPageIdx() * pageSize;
        ByteBuffer src = buffer.duplicate();
        src.limit(src.position() + pageSize);
        try {
            int written = 0;
            while (written < pageSize) {
                written += ch.write(src, offset + written);
            }
        } catch (IOException e) {
            throw new DiskManagerException("Erreur d'écriture de " + pid, e);
        }finally{
            buffer.clear();
        }
    }

    @Override
    public void DeallocPage(IPageId ipid) throws DiskManagerException {
        checkInitialized();
        PageId pid = checkActivePage(ipid);   // refuse une page déjà désallouée
        freePages.addLast(pid.getPageIdx());
    }

    @Override
    public int GetActivePageCount() {
        return nbPages - freePages.size();
    }

    @Override
    public int GetPageSize() {
        return pageSize;
    }

    // =====================================================================
    //  Méthodes supplémentaires
    // =====================================================================

    /** Ferme le fichier de données (sans sauvegarder). */
    public void close() throws DiskManagerException {
        if (dataFile != null) {
            try {
                dataFile.close();
            } catch (IOException e) {
                throw new DiskManagerException("Erreur à la fermeture du fichier de données", e);
            } finally {
                dataFile = null;
                dir = null;
            }
        }
    }

    private void checkInitialized() throws DiskManagerException {
        if (dir == null || dataFile == null) {
            throw new DiskManagerException("DiskManager non initialisé (appeler Init d'abord)");
        }
    }

    /** Vérifie que ipid est un PageId valide désignant une page actuellement allouée. */
    private PageId checkActivePage(IPageId ipid) throws DiskManagerException {
        if (ipid == null) {
            throw new DiskManagerException("PageId null");
        }
        if (!(ipid instanceof PageId)) {
            throw new DiskManagerException("Type d'identifiant non reconnu : "
                    + ipid.getClass().getName());
        }
        PageId pid = (PageId) ipid;
        if (pid.getFileIdx() != 0 || pid.getPageIdx() < 0 || pid.getPageIdx() >= nbPages) {
            throw new DiskManagerException("Page inexistante : " + pid);
        }
        if (freePages.contains(pid.getPageIdx())) {
            throw new DiskManagerException("Page non allouée (désallouée) : " + pid);
        }
        return pid;
    }

    private void checkBuffer(ByteBuffer buffer) throws DiskManagerException {
        if (buffer == null) {
            throw new DiskManagerException("Buffer null");
        }
        if (buffer.remaining() < pageSize) {
            throw new DiskManagerException("Buffer trop petit : " + buffer.remaining()
                    + " octets disponibles, il en faut " + pageSize);
        }
    }
}
