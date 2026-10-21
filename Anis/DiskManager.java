
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;    
import java.nio.ByteBuffer;         
import java.util.ArrayDeque;        

public class DiskManager implements IDiskManager {
    
    private String dmDir;                             //le chemin vers le dossier de travail
    private int pageSize;                             //la taille d'une page en bytes
    private int activePageCount;                      //nombre de pages allouées
    private ArrayDeque<IPageId> pagesDisponibles;     //une file qui stock l'id (PageId) des pages desallouées
    private RandomAccessFile fichierDisque ;          //Objet qui fait le lient entre le fichier de travail et le disque 

    //constructeur
    public DiskManager(String dmDir,byte pageSize){
        this.dmDir = null;
        this.pageSize = 0;
        this.activePageCount = 0;
        this.pagesDisponibles = new ArrayDeque<IPageId>();
        this.fichierDisque = null;
    }


    /* Initialise le DiskManager avec : 
        dmDir -> dossier de travail 
        pageSize 
    */ 
    @Override 
    public void Init(String dmDir, int pageSize) throws DiskManagerException{
        
        //vérifier que le chemin n'est pas null
        if (dmDir == null || dmDir.strip().isEmpty()){
            throw new DiskManagerException("Le chemin du fichier est invalide !");
        }
        //vérifier que pageSize > 0
        if (pageSize <= 0){
            throw new DiskManagerException("La taille d'une page est > 0 !");
        }
        //vérifier que le fichier n'est pas déjà ouvert 
        if (this.fichierDisque != null){    
            throw new DiskManagerException("Le DiskManager est déjà initialisé !");
        }
        
        //Créer le répertoire où stocker le fichier de données
        File repertoire = new File(dmDir.strip());
        
        if(!repertoire.exists()){
            if(!repertoire.mkdirs()){
                throw new DiskManagerException("Impossible de créer le répertoire !");
            }
        }

        //vérifier que le chemin correspond à un répertoire
        if(!repertoire.isDirectory()){
            throw new DiskManagerException("Le chemin indiqué ne correspond pas à un répertoire !");
        }

        // création du fichier data qui contiendra les données des pages
        File fichier = new File(repertoire,"data");  
        try {
            boolean fichierExiste = fichier.exists() && fichier.length()>0;

            this.fichierDisque = new RandomAccessFile(fichier, "rw");
            this.dmDir = repertoire.getAbsolutePath();
            this.pageSize = pageSize;

            /*Si le fichier existe déjà, on récupère le nombre de pages qu'il contient
              pour reconstruire activePageCount */
            if (fichierExiste){

                long fileLength = fichierDisque.length();
                if (fileLength % pageSize != 0) {
                    throw new DiskManagerException("Taille du fichier incompatible avec le pageSize fourni !");
                }
                this.activePageCount = (int) (fileLength / pageSize);
            } else {
            this.activePageCount = 0;
            }
            // On vide la liste des pages libres au démarrage
            this.pagesDisponibles.clear();

        } catch (IOException e){
            throw new DiskManagerException("Erreur lors de l'ouverture du fichier !");
    }
}


    /**
     * Sauvegarde et écriture des données sur le disque.
     */
    public void Save() throws DiskManagerException {
        if (this.fichierDisque == null) {
            throw new DiskManagerException("Le DiskManager n'est pas initialisé !");
        }
        try {
            // écriture des données sur le disque
            this.fichierDisque.getFD().sync();
        } catch (IOException e) {
            throw new DiskManagerException("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }   

    /**
     * Alloue une nouvelle page || réutilise une page libre || en ajoute une nouvelle).
     */
    @Override 
    public IPageId AllocPage() throws DiskManagerException{
        IPageId allocatedPage;
        if(!pagesDisponibles.isEmpty()){
            allocatedPage = pagesDisponibles.removeFirst();  //utiliser une page desallouée présente dans la file
        } else {
            allocatedPage = new PageId(activePageCount) ;    //création d'une nouvelle page si la file est vide
            activePageCount++;
        }
        return allocatedPage;
    }


    /**
     * Lit le contenu d'une page du disque dans un buffer.
     */
    @Override
    public void ReadPage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
    if (this.fichierDisque == null) {
        throw new DiskManagerException("Le DiskManager n'est pas initialisé !");
    }
    if (!isAllocated(ipid)) {
        throw new DiskManagerException("Erreur : la page n'est pas allouée !");
    }
    if (buffer == null) {
        throw new DiskManagerException("Le buffer n'est pas initialisé !");
    }

    //calcul de la position de la page que l'on doit lire sur le disque
    PageId pageId = (PageId) ipid;
    long position = (long) pageId.getPageId() * pageSize;
    byte[] donnees = new byte[pageSize];

    try {

        // Se placer au bon endroit dans le fichier et lire la page
        fichierDisque.seek(position);
        fichierDisque.readFully(donnees); 
        buffer.put(donnees);             

    } catch (IOException e) {
        throw new DiskManagerException("Erreur lors de la lecture de la page : " + e.getMessage());
    }
}

    /**
     * Écrit le contenu d'un buffer dans une page sur le disque.
     */
    public void WritePage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {
        if (this.fichierDisque == null) {
            throw new DiskManagerException("Le DiskManager n'est pas initialisé !");
        }
        if (!isAllocated(ipid)) {
            throw new DiskManagerException("La page n'est pas allouée !");
        }
        if (buffer == null) {
            throw new DiskManagerException("Le buffer est null !");
        }

        PageId pageId = (PageId) ipid;
        long position = (long) pageId.getPageId() * pageSize;
        byte[] donnees = new byte[pageSize];
        
        // Récupèrer les données depuis le buffer
        buffer.get(donnees);

        try {
            // Se placer au bon endroit et écrire les données
            fichierDisque.seek(position); //seek() -> positionner le pointeur du fichier au début de la page voulue
            fichierDisque.write(donnees); // écrire les données dans le disque
        } catch (IOException e) {
            throw new DiskManagerException("Erreur lors de l'écriture de la page : " + e.getMessage());
        }
    }

    /**
     * Désalloue une page pour qu'elle devienne disponible.
     */
    public void DeallocPage(IPageId ipid) throws DiskManagerException {
        //vérifier que l'id de ipid est correct
        if (!isIdValid(ipid)) {
        throw new DiskManagerException("Identifiant de page invalide !");
        }
        //vérifier que l'on desalloue pas une page déjà desallouée
        if (pagesDisponibles.contains(ipid)) {
        throw new DiskManagerException(
            "Double désallocation : la page est déjà désallouée !");
        }

        pagesDisponibles.add(ipid);    // ajouter l'ipid à la file si tous les tests sont passés correctement
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
            && ((PageId) ipid).getPageId() < activePageCount;
    }

    
    /**
     * Retourne le nombre total de pages actives.
     */
    public int GetActivePageCount(){
        return activePageCount;
    }

    /**
     * Retourne la taille d'une page en octets.
     */
    public int GetPageSize(){
        return pageSize;
    }
}
