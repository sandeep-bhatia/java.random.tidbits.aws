package datomata.recommendations;

import java.util.Comparator;

public class ArticleComparer implements Comparator<Article> {
    public int compare(Article o1, Article o2) {
        return o1.getWeight() - o2.getWeight();
    }
}
