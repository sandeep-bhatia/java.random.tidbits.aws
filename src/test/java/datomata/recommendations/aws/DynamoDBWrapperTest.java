package datomata.recommendations.aws;

public class DynamoDBWrapperTest {
    @org.junit.Before
    public void setUp() throws Exception {

    }

    @org.junit.After
    public void tearDown() throws Exception {

    }

    @org.junit.Test
    public void readUserHistory() throws Exception {

    }

    @org.junit.Test
    public void readSimilarArticles() throws Exception {
    }

    @org.junit.Test
    public void batchGet() throws Exception {
        DynamoDBWrapper.getInstance().BatchGetUserHistory("PunjabKesari.UserHistory", new String[] {"786"});
    }
}
