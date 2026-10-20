public class PageId implements IPageId {

    private int numeroPage; 

    //Constructeur 
    public PageId(int numeroPage) {
        this.numeroPage = numeroPage; 
    }

    //Le Getter 
    public int getNumeroPage() {
        return numeroPage; 
    }

    //Redéfintion de equals 
    @Override
    public boolean equals(Object obj) {
        if (( obj == null) || !(obj instanceof PageId) ) 
            return false; 

        //Raccourci dans le cas ou on compare avec l'objet lui meme
        if (obj == this)
            return true;

        //Transtypage de l'objet obj en PageId

        PageId objTemp = (PageId) obj; 

        return (this.numeroPage == objTemp.getNumeroPage()); 
    }

    @Override
    public int hashCode() {
        return numeroPage; 
    }



}