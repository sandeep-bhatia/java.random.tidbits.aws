package datomata.recommendations;

public class Article {
    public String VisitedArticleId;
    public String ArticleId;
    private int DateOffset = -10;
    private int SimilarIndex = 0;
    private int weight = -10;

    public void setDateOffset(int dateOffset) {
        this.DateOffset = dateOffset;
        this.setWeight();
    }

    public void setSimilarIndex(int similarIndex) {
        this.SimilarIndex = similarIndex;
        this.setWeight();
    }

    public int getWeight() {
        return weight;
    }

    private void setWeight() {
        weight = DateOffset + SimilarIndex;
    }
}
