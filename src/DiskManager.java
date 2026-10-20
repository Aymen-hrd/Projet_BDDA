import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

public class DiskManager implements IDiskManager {

    private String dmDir;
    private int pageSize;
    private int nbPages;
    private List<PageId> pagesLibres = new ArrayList<>();

    // Constructeur sans arguments
    public DiskManager() {
        dmDir = null;
        pageSize = 0;
        nbPages = 0;
    }

    @Override
    public void Init(String dmDir, int pageSize) throws DiskManagerException {

    }

    @Override
    public void Save() throws DiskManagerException {

    }

    @Override
    public IPageId AllocPage() throws DiskManagerException {

        PageId nvllePage;
        // Vérifier qu'aucune page libre n'est disponible
        if (pagesLibres.isEmpty()) {
            nbPages += 1;
            nvllePage = new PageId(nbPages - 1);
        }

        else {
            nvllePage = pagesLibres.remove(0);

        }
        return nvllePage;

    }

    @Override
    public void ReadPage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {

    }

    @Override
    public void WritePage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException {

    }

    @Override
    public void DeallocPage(IPageId ipid) throws DiskManagerException {

        PageId ipidTemp = verifierPageId(ipid);

        // Vérifier que le numéro de page ne se trouve pas déjà dans la ArrayList et
        // l'ajouter sinon

        if (pagesLibres.contains(ipidTemp))
            throw new DiskManagerException(" La page est déjà libre");

        // Si le page n'appartient pas à la ArrayList

        pagesLibres.add(ipidTemp);

    }

    @Override
    public int GetActivePageCount() {
        return nbPages - pagesLibres.size();
    }

    @Override
    // Getter
    public int GetPageSize() {
        return pageSize;
    }



private PageId verifierPageId(IPageId ipid) throws DiskManagerException {

    if (!(ipid instanceof PageId))
        throw new DiskManagerException("L'objet mis en argument n'est pas une instance de la classe PageId");

    // Si la condition est juste on peut faire le cast

    PageId ipidTemp = (PageId) ipid;

    // Vérifier que son numéro de page est valide

    if ((ipidTemp.getNumeroPage() < 0) || ipidTemp.getNumeroPage() > nbPages - 1)
        throw new DiskManagerException("Le numéro de page est invalide");

    return ipidTemp;

}
}
