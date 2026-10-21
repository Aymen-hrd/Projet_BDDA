import java.util.List;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class DiskManager implements IDiskManager {
    
    private String dmDir;                               //le chemin vers le fichier ".db" de la bdd
    private byte pageSize;                             //la taille d'une page en bytes
    private int activePageCount;                      //nombre de pages en cours d'utilisation: utile pour donner l'id de chaque page
    private ArrayDeque<IPageId> pagesDisponibles;    //une file qui stock les pages desallouées
    private RandomAccessFile fichierDisque ;             

    //constructeur
    public DiskManager(String dmDir,byte pageSize){
        this.dmDir = null;
        this.pageSize = 0;
        this.activePageCount = 0;
        this.pagesDisponibles = new ArrayDeque<IPageId>();
    }


     /**
     * Initializes the disk manager with a target working directory and a page size.
     * <p>
     * Binds the disk manager to the new directory.
     * <ul>
     * 	 <li>if the disk manager is already bound to a directory, fail.</li> 
     *   <li>If the directory already contains data, reuse it if compatible.</li> 
     *   <li>If the directory is empty, initialize it for subsequent use.</li>
     *   <li>If the directory does not exist, create then use as above.</li>   
     * </ul>
     *
     * @param dmDir    the absolute path to a valid working directory.
     * @param pageSize the size of a single page in bytes.
     * @throws DiskManagerException 
     */
    @Override 
    public void Init(String dmDir, int pageSize) throws DiskManagerException{

    }


     /**
     * Persists the current state and metadata of the disk manager
     * to the working directory. 
     *
     * Cette méthode fera des opérations d'écriture dans le disque
     * 
     * @throws DiskManagerException
     */
    public void Save() throws DiskManagerException{

    }

     /**
     * Allocates a new page requested by the upper layers of the DBMS.
     * <p>
     * Allocation policy:
     * <ol>
     *   <li>If a previously deallocated page is available, reuse it.</li>
     *   <li>Otherwise, append a new page to the disk storage.</li>
     * </ol>
     *
     * @return the {@link IPageId} identifying the newly allocated page.
     * @throws DiskManagerException
     */
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
     * Reads the on-disk content of the page designated by {@code ipid}
     * into the provided buffer.
     * <p>
     * Note: The caller is responsible for allocating and passing the destination buffer.
     *
     * @param ipid   the identifier of the page to read.
     * @param buffer the buffer into which data will be copied
     * @throws DiskManagerException
     */
    public void ReadPage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException{
        
        
    }

    /**
     * Writes the content of the provided buffer into the page designated by {@code ipid}.
     * This is the counterpart of {@link #ReadPage(IPageId, ByteBuffer)}.
     *
     * @param ipid   the identifier of the target page.
     * @param buffer the buffer containing the data to write.
     * @throws DiskManagerException
     */
    public void WritePage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException{
        
        
    }

    /**
     * Deallocates the page identified by {@code ipid}, marking it as available
     * for future allocations.
     *
     * @param ipid the identifier of the page to free.
     * @throws DiskManagerException
     */
    public void DeallocPage(IPageId ipid) throws DiskManagerException {
   
        if (!isIdValid(ipid)) {
        throw new DiskManagerException("Identifiant de page invalide !");
        }

        if (pagesDisponibles.contains(ipid)) {
        throw new DiskManagerException(
            "Double désallocation : la page est déjà désallouée !");
        }

        pagesDisponibles.add(ipid);    // ajouter l'ipid à la file si tous les tests sont passés correctement
}

    /**
     * vérifie que la page avec l'id ipid est bien allouée
     * @param ipid l'identifiant de la page à vérifier 
     * @throws DiskManagerException
     */
    private boolean isAllocated(IPageId ipid){
        if (!isIdValid(ipid)) 
            return false;
        return !pagesDisponibles.contains(ipid);

        /*for (IPageId p:pagesDisponibles){
            if (p.equals(ipid)){
                return false; 
            }
        }*/
    }

    /**
     * vérfie que l'id d'une page est valide
     * @param ipid
     * @return
     */
    private boolean isIdValid(IPageId ipid) {
    return ipid instanceof PageId
            && ((PageId) ipid).getPageId() >= 0
            && ((PageId) ipid).getPageId() < activePageCount;
    }

    
    /**
     * Returns the total number of pages currently in use (allocated and not yet deallocated).
     *
     * @return the count of active allocated pages.
     */
    public int GetActivePageCount(){
        return activePageCount;
    }

    /**
     * Returns the configured page size in bytes managed by this disk manager.
     *
     * @return the page size in bytes.
     */
    public int GetPageSize(){
        return pageSize;
    }
}
