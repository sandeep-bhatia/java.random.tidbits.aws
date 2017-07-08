package datomata.recommendations;

import datomata.recommendations.aws.DynamoDBWrapper;
import datomata.recommendations.aws.S3Wrapper;
import datomata.recommendations.aws.SQSWrapper;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

public class UserQueueMessageWorker extends Thread {
    private final BlockingQueue<String[]> queue;
    private final String tenantId;
    private static Logger log = Logger.getLogger(S3Wrapper.class.getName());


    public UserQueueMessageWorker(BlockingQueue<String[]> queue, String tenantId) {
        this.queue = queue;
        this.tenantId = tenantId;
    }

    public void run() {
        while ( true ) {
            try {
                String[] s = queue.take();
                calculateRecommendations(s[0]);
                SQSWrapper.getInstance().deleteQueueMessage(this.tenantId, s[1]);
            } catch (Exception ex) {
                Utilities.printException(ex);
            }
        }
    }

    private void calculateRecommendations(String s) {
        String[] users = s.split(",");
        HashMap<String, String> userHistories = DynamoDBWrapper.getInstance().BatchGetUserHistory(S3Wrapper.getUserHistoryTable(this.tenantId), users);
        HashMap<String, String> userArticleMap = new HashMap<String, String>();
        HashMap<String, ArrayList<String>> articleUserMap = new HashMap<String, ArrayList<String>>();
        ArrayList<String> articles = new ArrayList();

        for (String key : userHistories.keySet()) {
            String historyFlattenedList = userHistories.get(key);

            log.info(String.format("%s: %s, user and history available history", key, historyFlattenedList));
            if(historyFlattenedList != null && historyFlattenedList.length() > 0) {
                String[] articlesHistory = historyFlattenedList.split(",");

                for(int index = 0; index < articlesHistory.length; index++) {
                    userArticleMap.put(key, articlesHistory[index]);

                    if(!articleUserMap.containsKey(articlesHistory[index])) {
                        articleUserMap.put(articlesHistory[index], new ArrayList<String>());
                    }

                    articleUserMap.get(articlesHistory[index]).add(key);
                    articles.add(articlesHistory[index]);
                }
            }
        }

        HashMap<String, String> articleSimilars = new HashMap<String, String>();
        HashMap<String, String> articleDateInfo = new HashMap<String, String>();

        DynamoDBWrapper.getInstance().getSimilarsToArticleWithDateInfo(articles,
                                                        S3Wrapper.getSimilarArticlesTable(this.tenantId),
                                                        S3Wrapper.getArticleContentTable(this.tenantId),
                                                        articleSimilars,
                                                        articleDateInfo);

        ArrayList<Article> recommendedArticles = ArticleFilter.filter(articleSimilars, articleDateInfo);

        HashMap<String, HashSet<String>> recommendedArticlesPerUser = new HashMap<String, HashSet<String>>();

        for(int index = 0; index < recommendedArticles.size(); index++) {
            Article currentArticleInfo = recommendedArticles.get(index);

            if(articleUserMap.containsKey(currentArticleInfo.VisitedArticleId)) {
                ArrayList<String> currentArticleUsers = articleUserMap.get(currentArticleInfo.VisitedArticleId);
                if(currentArticleUsers != null && currentArticleUsers.size() > 0) {
                    for(int currentUserIndex = 0; currentUserIndex < currentArticleUsers.size(); currentUserIndex++) {
                        if(!recommendedArticlesPerUser.containsKey(currentArticleUsers.get(currentUserIndex))) {
                            recommendedArticlesPerUser.put(currentArticleUsers.get(currentUserIndex), new HashSet<String>());
                        }

                        HashSet<String> recommendations = recommendedArticlesPerUser.get(currentArticleUsers.get(currentUserIndex));
                        if(!recommendations.contains(currentArticleInfo.ArticleId)) {
                            recommendations.add(currentArticleInfo.ArticleId);
                        }
                        recommendedArticlesPerUser.put(currentArticleUsers.get(currentUserIndex), recommendations);
                    }
                }
            }
        }

        DynamoDBWrapper.getInstance().storeUserRecommendations(S3Wrapper.getUserRecommendationsTable(this.tenantId), recommendedArticlesPerUser);
    }
}
