package tp01_disque;

import commun.IDiskManager;
import commun.IPageId;
import commun.DiskManagerException;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;    
import java.nio.ByteBuffer;         
import java.util.ArrayDeque;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;       

public class DiskManager implements IDiskManager {
    
    private String dmDir;                             //le chemin vers le dossier de travail
    private int pageSize;                             //la taille d'une page en bytes
    private int totalPageCount;                       //nombre de pages allouées
    private ArrayDeque<IPageId> pagesDisponibles;     //une file qui stock l'id (PageId) des pages desallouées
    private RandomAccessFile fichierDisque ;          //Objet qui fait le lient entre le fichier de travail et le disque 
    private boolean isInitialized;                    // Permet de s'assurer qu'Init n'est appelé qu'une seule fois

    //constructeur
    public DiskManager(){
        this.dmDir = null;
        this.pageSize = 0;
        this.totalPageCount = 0;
        this.pagesDisponibles = new ArrayDeque<IPageId>();
        this.fichierDisque = null;
        this.isInitialized = false;
    }


    /* Initialise le DiskManager avec : 
        dmDir -> dossier de travail 
        pageSize 
    */ 
    @Override 
   public void Init(String dmDir, int pageSize) throws DiskManagerException {
        if (this.isInitialized) {
            throw new DiskManagerException("La méthode Init a déjà été appelée !");
        }
        if (dmDir == null || dmDir.strip().isEmpty()){
            throw new DiskManagerException("Le chemin du fichier est invalide !");
        }
        if (pageSize <= 0){
            throw new DiskManagerException("La taille d'une page doit être strictement supérieure à 0 !");
        }
        if (this.fichierDisque != null){    
            throw new DiskManagerException("Le DiskManager est déjà initialisé !");
        }
        
        File repertoire = new File(dmDir.strip());
        if (!repertoire.exists()){
            if (!repertoire.mkdirs()){
                throw new DiskManagerException("Impossible de créer le répertoire !");
            }
        }
        if (!repertoire.isDirectory()){
            throw new DiskManagerException("Le chemin indiqué ne correspond pas à un répertoire !");
        }

        File fichierData = new File(repertoire, "data");  
        File fichierMeta = new File(repertoire, "metadata");  

        try {
            boolean dataExiste = fichierData.exists() && fichierData.length() > 0;
            boolean metaExiste = fichierMeta.exists();

            this.fichierDisque = new RandomAccessFile(fichierData, "rw");
            this.dmDir = repertoire.getAbsolutePath();
            this.pageSize = pageSize;

            // Si un fichier de métadonnées existe, on recharge l'état exact 
            if (metaExiste) {
                try (DataInputStream dis = new DataInputStream(new FileInputStream(fichierMeta))) {
                    int savedPageSize = dis.readInt();
                    if (savedPageSize != pageSize) {
                        throw new DiskManagerException("Incompatibilité de pageSize avec le dossier existant !");
                    }
                    this.totalPageCount = dis.readInt();
                    int freeCount = dis.readInt();
                    this.pagesDisponibles.clear();
                    for (int i = 0; i < freeCount; i++) {
                        int freeId = dis.readInt();
                        this.pagesDisponibles.add(new PageId(freeId));
                    }
                }
            } else if (dataExiste) {
                long fileLength = fichierDisque.length();
                if (fileLength % pageSize != 0) {
                    throw new DiskManagerException("Taille du fichier incompatible avec le pageSize fourni !");
                }
                this.totalPageCount = (int) (fileLength / pageSize);
                this.pagesDisponibles.clear();
            } else {
                this.totalPageCount = 0;
                this.pagesDisponibles.clear();
            }
        this.isInitialized = true;
        } catch (IOException e){
            throw new DiskManagerException("Erreur lors de l'ouverture du fichier : " + e.getMessage());
        }
        
    }


    /**
     * Sauvegarde et écriture des données sur le disque.
     */
    public void Save() throws DiskManagerException {
        if (!this.isInitialized || this.fichierDisque == null) {
            throw new DiskManagerException("Le DiskManager n'est pas initialisé !");
        }
        try {
            // Forcer l'écriture physique des données sur le disque
            this.fichierDisque.getFD().sync();

            // Sauvegarder les métadonnées (pageSize, totalPageCount, pages libres)
            File fichierMeta = new File(this.dmDir, "metadata");
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(fichierMeta))) {
                dos.writeInt(this.pageSize);
                dos.writeInt(this.totalPageCount);
                dos.writeInt(this.pagesDisponibles.size());
                for (IPageId ipid : this.pagesDisponibles) {
                    dos.writeInt(((PageId) ipid).getPageId());
                }
            }
        } catch (IOException e) {
            throw new DiskManagerException("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    /**
     * Alloue une nouvelle page || réutilise une page libre || en ajoute une nouvelle).
     */
    @Override 
    public IPageId AllocPage() throws DiskManagerException{
        if (!this.isInitialized || this.fichierDisque == null) {
            throw new DiskManagerException("Le DiskManager n'est pas initialisé !");
        }
        IPageId allocatedPage;
        if(!pagesDisponibles.isEmpty()){
            allocatedPage = pagesDisponibles.removeFirst();  //utiliser une page desallouée présente dans la file
        } else {
            allocatedPage = new PageId(totalPageCount) ;    //création d'une nouvelle page si la file est vide
            totalPageCount++;
        }
        return allocatedPage;
    }


    /**
     * Lit le contenu d'une page du disque dans un buffer.
     */
    @Override
    public void ReadPage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
        if (!this.isInitialized || this.fichierDisque == null) {
            throw new DiskManagerException("Le DiskManager n'est pas initialisé !");
        }
        if (!isAllocated(ipid)) {
            throw new DiskManagerException("Erreur : la page n'est pas allouée !");
        }
        if (buffer == null) {
            throw new DiskManagerException("Le buffer est null !");
        }
        
        // Remettre le curseur à 0 (Point 7)
        buffer.position(0);
        
        // Vérifier que le buffer a exactement la taille d'une page (Point 6)
        if (buffer.remaining() != pageSize) {
            throw new DiskManagerException("La taille du buffer doit être exactement égale au pageSize !");
        }

        PageId pageId = (PageId) ipid;
        long position = (long) pageId.getPageId() * pageSize;
        byte[] donnees = new byte[pageSize];

        try {
            fichierDisque.seek(position);
            fichierDisque.readFully(donnees); 
            buffer.put(donnees);
            buffer.flip(); // Prépare le buffer pour la lecture (position = 0, limit = pageSize)

        } catch (IOException e) {
            throw new DiskManagerException("Erreur lors de la lecture de la page : " + e.getMessage());
        }
    }

    /**
     * Écrit le contenu d'un buffer dans une page sur le disque.
     */
    public void WritePage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
        if (!this.isInitialized || this.fichierDisque == null) {
            throw new DiskManagerException("Le DiskManager n'est pas initialisé !");
        }
        if (!isAllocated(ipid)) {
            throw new DiskManagerException("La page n'est pas allouée !");
        }
        if (buffer == null) {
            throw new DiskManagerException("Le buffer est null !");
        }

        // Remettre le curseur à 0 (Point 7)
        buffer.position(0);

        // Vérifier que le buffer a exactement la taille d'une page (Point 6)
        if (buffer.remaining() != pageSize) {
            throw new DiskManagerException("La taille du buffer doit être exactement égale au pageSize !");
        }

        PageId pageId = (PageId) ipid;
        long position = (long) pageId.getPageId() * pageSize;
        byte[] donnees = new byte[pageSize];
        
        buffer.get(donnees);

        try {
            fichierDisque.seek(position); 
            fichierDisque.write(donnees); 
        } catch (IOException e) {
            throw new DiskManagerException("Erreur lors de l'écriture de la page : " + e.getMessage());
        }
    }

    /**
     * Désalloue une page pour qu'elle devienne disponible.
     */
    public void DeallocPage(IPageId ipid) throws DiskManagerException {
        if (!this.isInitialized || this.fichierDisque == null) {
            throw new DiskManagerException("Le DiskManager n'est pas initialisé !");
        }
        if (!isIdValid(ipid)) {
            throw new DiskManagerException("Identifiant de page invalide !");
        }
        if (pagesDisponibles.contains(ipid)) {
            throw new DiskManagerException("Double désallocation : la page est déjà désallouée !");
        }

        pagesDisponibles.add(ipid);    
    }

    /**
     * Vérifie si une page est bien allouée (et non libre).
     */
    private boolean isAllocated(IPageId ipid){
        if (!isIdValid(ipid)) 
            return false;
        return !pagesDisponibles.contains(ipid);
    }

    /**
     * Vérifie si l'ID d'une page est valide (dans les limites).
     */
    private boolean isIdValid(IPageId ipid) {
    return ipid instanceof PageId
            && ((PageId) ipid).getPageId() >= 0
            && ((PageId) ipid).getPageId() < totalPageCount;
    }

    
    /**
     * Retourne le nombre total de pages actives.
     */
    public int GetActivePageCount() {
        return totalPageCount - pagesDisponibles.size();
    }

    /**
     * Retourne la taille d'une page en octets.
     */
    public int GetPageSize() {
        return pageSize;
    }

    
}
