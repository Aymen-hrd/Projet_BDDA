package tp01_disque;

import commun.IPageId;

/**
 * la classe PageId sert uniquement à identifier une page 
 * Elle ne gère pas sont état.
 * Une BDD contiendra des miliers de pages, surcharger les objets de type PageId 
 * serait une mauvaise idée, cela serait couteux en mémoire et en performance.
 */
public class PageId implements IPageId {

    private final int pageId ;

    //constructeur 
    public PageId(int pageId){
        this.pageId = pageId;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PageId)) {
            return false;
        }
        PageId other = (PageId) obj;
        return pageId == other.getPageId();
    }

    public int getPageId(){
        return pageId;
    }
}
