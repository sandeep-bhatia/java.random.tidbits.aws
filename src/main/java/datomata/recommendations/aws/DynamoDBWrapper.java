package datomata.recommendations.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.amazonaws.util.StringUtils;
import datomata.recommendations.StringConstants;
import datomata.recommendations.Utilities;

import java.lang.reflect.Array;
import java.util.*;

public class DynamoDBWrapper {

    private static DynamoDBWrapper instance;
    private DynamoDB dynamoDB;

    private DynamoDBWrapper() {
    }

    public static DynamoDBWrapper getInstance() {
        if(instance != null) {
            return instance;
        }

        instance = new DynamoDBWrapper();
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(new BasicAWSCredentials(StringConstants.ACCESS_KEY, StringConstants.ACCESS_SECRET));
        dynamoDBClient.setRegion(Region.getRegion(Regions.US_WEST_2));
        instance.dynamoDB = new DynamoDB(dynamoDBClient);
        return instance;
    }

    String readUserHistory(String tableName, String userId) {
        Table table = dynamoDB.getTable(tableName);
        Item item = table.getItem("userId", userId);
        return item.toString();
    }

    String readSimilarArticles(String tableName, String articleId) {
        Table table = dynamoDB.getTable(tableName);
        Item item = table.getItem("articleId", articleId);
        return item.toString();
    }

    public HashMap<String, String> BatchGetUserHistory(String tableName, String[] userIds) {
        ArrayList<String[]> userArrayList = splitArray(userIds, 50);
        HashMap<String, String> listElements = new HashMap<String, String>();
        for(int indexArrays = 0; indexArrays < userArrayList.size(); indexArrays++ ) {
            try {

                TableKeysAndAttributes userHistoryTableAttributes = new TableKeysAndAttributes(tableName);

                for (int index = 0; index < userArrayList.get(indexArrays).length; index++) {
                    userHistoryTableAttributes.addHashOnlyPrimaryKey(StringConstants.USER_ID_KEY, userArrayList.get(indexArrays)[index]);
                }

                BatchGetItemOutcome outcome = dynamoDB.batchGetItem(userHistoryTableAttributes);

                Map<String, KeysAndAttributes> unprocessed = null;

                do {
                    for (String table : outcome.getTableItems().keySet()) {
                        List<Item> items = outcome.getTableItems().get(table);
                        for (Item item : items) {
                            listElements.put(item.get(StringConstants.USER_ID_KEY).toString(), item.get(StringConstants.USERID_HISTORY_ATTRIBUTE).toString());
                        }
                    }

                    // Check for unprocessed keys which could happen if you exceed provisioned
                    // throughput or reach the limit on response size.
                    unprocessed = outcome.getUnprocessedKeys();

                    if (!unprocessed.isEmpty()) {
                        outcome = dynamoDB.batchGetItemUnprocessed(unprocessed);
                    }

                } while (!unprocessed.isEmpty());
            } catch (Exception e) {
                Utilities.printException(e);
            }
        }

        return listElements;
    }

    public void getSimilarsToArticleWithDateInfo(ArrayList<String> articles,
                                                 String similarArticleTable,
                                                 String articleInfoTable,
                                                 HashMap<String, String> articleSimilars,
                                                 HashMap<String, String> articleDateInfo) {

        ArrayList<String[]> userArrayList = splitArray(articles.toArray(), 75);

        for(int indexArrays = 0; indexArrays < userArrayList.size(); indexArrays++ ) {

            try {

                TableKeysAndAttributes similarArticlesTableAttributes = new TableKeysAndAttributes(similarArticleTable);
                TableKeysAndAttributes articleContentTableAttributes = new TableKeysAndAttributes(articleInfoTable);
                HashSet<String> duplicateDetection = new HashSet<String>();
                for (int index = 0; index < userArrayList.get(indexArrays).length; index++) {
                    if(!duplicateDetection.contains(userArrayList.get(indexArrays)[index])) {
                        similarArticlesTableAttributes.addHashOnlyPrimaryKey(StringConstants.ARTICLE_ID_KEY, userArrayList.get(indexArrays)[index]);
                        articleContentTableAttributes.addHashOnlyPrimaryKey(StringConstants.ARTICLE_ID_KEY, userArrayList.get(indexArrays)[index]);
                        duplicateDetection.add(userArrayList.get(indexArrays)[index]);
                    }
                }

                BatchGetItemOutcome outcome = dynamoDB.batchGetItem(similarArticlesTableAttributes, articleContentTableAttributes);

                Map<String, KeysAndAttributes> unprocessed = null;

                do {
                    for (String table : outcome.getTableItems().keySet()) {
                        List<Item> items = outcome.getTableItems().get(table);

                        for (Item item : items) {
                            if (table.toUpperCase().equals(similarArticleTable.toUpperCase())) {
                                articleSimilars.put(item.get(StringConstants.ARTICLE_ID_KEY).toString(), item.get(StringConstants.ARTICLE_SIMILARS_KEY).toString());
                            } else {
                                articleDateInfo.put(item.get(StringConstants.ARTICLE_ID_KEY).toString(), item.get(StringConstants.ARTICLE_CREATEDDATE_KEY).toString());
                            }
                        }
                    }

                    // Check for unprocessed keys which could happen if you exceed provisioned
                    // throughput or reach the limit on response size.
                    unprocessed = outcome.getUnprocessedKeys();

                    if (!unprocessed.isEmpty()) {
                        outcome = dynamoDB.batchGetItemUnprocessed(unprocessed);
                    }

                } while (!unprocessed.isEmpty());

            } catch (Exception e) {
                Utilities.printException(e);
            }
        }
    }

    public void storeUserRecommendations(String userRecommendationsTable, HashMap<String, HashSet<String>> recommendedArticlesPerUser) {
        ArrayList<HashMap<String, HashSet<String>>> breakdownUpdates = new ArrayList<HashMap<String, HashSet<String>>>();

        int index = 0;
        HashMap<String, HashSet<String>> userSet = new HashMap<String, HashSet<String>>();

        for (String userId : recommendedArticlesPerUser.keySet()) {
            userSet.put(userId, recommendedArticlesPerUser.get(userId));
            index++;

            if(index > 20) {
                breakdownUpdates.add(userSet);
                userSet = new HashMap<String, HashSet<String>>();
                index = 0;
            }
        }

        if(userSet.size() > 0) {
            breakdownUpdates.add(userSet);
        }

        for(int breakIndex = 0; breakIndex < breakdownUpdates.size(); breakIndex++) {
            storeUserRecommendationsBatched(userRecommendationsTable, breakdownUpdates.get(breakIndex));
        }
    }

    public void storeUserRecommendationsBatched(String userRecommendationsTable, HashMap<String, HashSet<String>> recommendedArticlesPerUser) {
        try {
            // Add a new item to Forum
            TableWriteItems recommendationTableItems = new TableWriteItems(userRecommendationsTable);
            int index = 0;
            ArrayList<Item> collectionItems = new ArrayList<Item>();
            for (String key : recommendedArticlesPerUser.keySet()) {
                String flattenedRecommendations = flattenHashMapToCommaSeperatedString(recommendedArticlesPerUser.get(key));
                collectionItems.add(new Item()
                        .withPrimaryKey(StringConstants.USER_ID_KEY, key)
                        .withString(StringConstants.RECOMMENDATIONS_KEY, flattenedRecommendations));

            }

            recommendationTableItems.withItemsToPut(collectionItems);
            BatchWriteItemOutcome outcome = dynamoDB.batchWriteItem(recommendationTableItems);

            do {
                // Check for unprocessed keys which could happen if you exceed provisioned throughput
                Map<String, List<WriteRequest>> unprocessedItems = outcome.getUnprocessedItems();

                if (outcome.getUnprocessedItems().size() > 0) {
                    outcome = dynamoDB.batchWriteItemUnprocessed(unprocessedItems);
                }

            } while (outcome.getUnprocessedItems().size() > 0);

        }  catch (Exception e) {
            Utilities.printException(e);
        }
    }

    private String flattenHashMapToCommaSeperatedString(HashSet<String> strings) {
        StringBuilder resultBuilder = new StringBuilder();
        for(String value: strings) {
            resultBuilder.append(value);
            resultBuilder.append(",");
        }

        return resultBuilder.toString();
    }

    private ArrayList<String[]> splitArray(Object[] array, int size) {
        int index = 0;
        ArrayList<String[]> arrays = new ArrayList<String[]>();
        while(index < array.length) {
            int arraySize = (array.length - index) > size ? size : array.length - index;
            String[] currentArray = new String[arraySize];
            int copiedIndex = 0;
            for (int copyIndex = index; copyIndex < index + arraySize; copyIndex++) {
                currentArray[copiedIndex] = array[copyIndex].toString();
                copiedIndex++;
            }

            index = index + arraySize;
            arrays.add(currentArray);
        }

        return arrays;
    }
}
