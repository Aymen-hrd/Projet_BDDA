import java.util.Objects;

/**
 * Identifiant concret d'une page : (numéro de fichier, numéro de page dans le fichier).
 * Pour l'instant le DiskManager n'utilise qu'un seul fichier (fileIdx = 0),
 */
public class PageId implements IPageId {
    private final int fileIdx;
    private final int pageIdx;

    public PageId(int fileIdx, int pageIdx) {
        this.fileIdx = fileIdx;
        this.pageIdx = pageIdx;
    }

    public int getFileIdx() { return fileIdx; }
    public int getPageIdx() { return pageIdx; }

    /** Deux PageId sont égaux s'ils désignent la même page sur disque. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PageId)) return false;
        PageId p = (PageId) obj;
        return fileIdx == p.fileIdx && pageIdx == p.pageIdx;
    }

    
    @Override
    public int hashCode() { return Objects.hash(fileIdx, pageIdx); }

    @Override
    public String toString() { return "PageId(" + fileIdx + "," + pageIdx + ")"; }
}
