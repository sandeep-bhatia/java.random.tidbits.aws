package datomata.recommendations;

import com.sun.corba.se.impl.javax.rmi.CORBA.Util;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.MutableDateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

public class ArticleFilter {
    private static int LOW_INDEX = 1;
    private static int MED_INDEX = 3;
    private static int HIGH_INDEX = 5;

    public static ArrayList<Article> filter(HashMap<String, String> articleSimilars, HashMap<String, String> articleDateInfo) {
        MutableDateTime epoch = new MutableDateTime();
        epoch.setDate(0); //Set to Epoch time
        DateTime now = new DateTime();

        Days days = Days.daysBetween(epoch, now);

        int daysBaseline = days.getDays() - 5;

        HashMap<String, Article[]> similarArticlesMap = new HashMap<String, Article[]>();
        ArrayList<Article> recommendedArticles = new ArrayList<Article>();

        //get all candidate articles and evaluate their article info
        for (String key : articleSimilars.keySet()) {

            String flattenedSimilars = articleSimilars.get(key);
            String[] similars = flattenedSimilars.split(",");

            if (similars != null && similars.length > 0) {

                Article article = new Article();

                for (int index = 0; index < similars.length; index++) {
                    int similarityIndex = (index <= LOW_INDEX) ? HIGH_INDEX : (index <= MED_INDEX) ? MED_INDEX : LOW_INDEX;

                    article.setSimilarIndex(similarityIndex);
                    if (articleDateInfo.containsKey(similars[index])) {
                        try {
                            String createdDate = articleDateInfo.get(similars[index]);
                            long timeStamp = Long.parseLong(createdDate);
                            int createdDateDays = Days.daysBetween(epoch, new DateTime(timeStamp)).getDays();
                            int differenceDays = createdDateDays - daysBaseline;

                            if (differenceDays < 0) {
                                continue;
                            }

                            article.setDateOffset(differenceDays);
                        } catch (Exception ex) {
                            Utilities.printException(ex);
                            article.setDateOffset(0);
                        }
                    } else {
                        article.setDateOffset(0);
                    }

                    article.ArticleId = similars[index];
                    article.VisitedArticleId = key;
                    recommendedArticles.add(article);
                }
            }
        }
        Collections.sort(recommendedArticles, new ArticleComparer());
        return recommendedArticles;
    }
}

