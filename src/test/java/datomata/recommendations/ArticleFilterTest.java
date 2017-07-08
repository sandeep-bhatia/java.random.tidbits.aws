package datomata.recommendations;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.MutableDateTime;
import org.junit.Test;

import java.util.HashMap;
import java.util.Random;

import static org.junit.Assert.*;

public class ArticleFilterTest {
    @Test
    public void filter() throws Exception {
        Random randomGenerator = new Random();

        //create some test data
        HashMap<String, String> articleSimiliars = new HashMap<String, String>();
        for(int index = 0; index < 10; index++) {
            articleSimiliars.put(Integer.toString(index), Integer.toString(randomGenerator.nextInt(9)));
        }

        HashMap<String, String> articleDateInfo = new HashMap<String, String>();
        MutableDateTime epoch = new MutableDateTime();


        for(int index = 0; index < 10; index++) {
            epoch.setDate(System.currentTimeMillis()); //Set to Epoch time
            epoch.addDays(randomGenerator.nextInt(5) * -1);
            articleDateInfo.put(Integer.toString(index), Long.toString(epoch.getMillis()));
        }

        ArticleFilter.filter(articleSimiliars, articleDateInfo);
    }

}