package datomata.recommendations.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import datomata.recommendations.StringConstants;
import datomata.recommendations.Utilities;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class S3Wrapper {

    private static AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(StringConstants.ACCESS_KEY, StringConstants.ACCESS_SECRET));
    static ArrayList<String> settings;
    static Logger log = Logger.getLogger(S3Wrapper.class.getName());

    public static void ReadSimiliarArticles(String arg) {
        try {
            log.info(String.format("%s bucket, %s key being read for similar articles", arg, StringConstants.SIMILAR_KEY));
            S3Object s3object = s3Client.getObject(new GetObjectRequest(arg, StringConstants.SIMILAR_KEY));

            log.info("Content-Type: "  + s3object.getObjectMetadata().getContentType());
            InputStream input = s3object.getObjectContent();

            log.info("Reading Similar articles contents from S3");
            String data = ReadData(input);

            log.info("Similar articles content read from S3");

        } catch (AmazonServiceException ase) {
            Utilities.printAWSException(ase);
        } catch (AmazonClientException ace) {
            Utilities.printException(ace);
        }
        catch(IOException ioex) {
          Utilities.printException(ioex);
        }
    }

    public static String getSimilarArticlesBucket(String tenantId) {
        readTasteConfig(tenantId);

        log.info(String.format("Settings are retrieved for the taste with settings size %s", tenantId));
        return settings.get(0);
    }

    public static String getUserHistoryTable(String tenantId) {
        readTasteConfig(tenantId);

        log.info(String.format("Settings are retrieved for the taste with settings size %s", tenantId));
        return settings.get(2);
    }

    public static String getUserRecommendationsTable(String tenantId) {
        readTasteConfig(tenantId);

        log.info(String.format("Settings are retrieved for the taste with settings size %s", tenantId));
        return settings.get(3);
    }

    public static String getUserMonitoringQueueUrl(String tenantId) {
        readTasteConfig(tenantId);

        log.info(String.format("Settings are retrieved for the taste with settings size %s", tenantId));
        return settings.get(4);
    }

    public static String getUserMonitoringArnUrl(String tenantId) {
        readTasteConfig(tenantId);

        log.info(String.format("Settings are retrieved for the taste with settings size %s", tenantId));
        return settings.get(5);
    }

    public static String getArticleContentTable(String tenantId) {
        readTasteConfig(tenantId);

        log.info(String.format("Settings are retrieved for the taste with settings size %s", tenantId));
        return settings.get(6);
    }

    public static String getSimilarArticlesTable(String tenantId) {
        readTasteConfig(tenantId);

        log.info(String.format("Settings are retrieved for the taste with settings size %s", tenantId));
        return settings.get(7);
    }

    public static void readTasteConfig(String tenant) {
        try {

            if(settings != null && settings.size() > 0){
                return;
            }

            log.info(String.format("%s bucket, %s key being read for taste config", "datomata.taste.config", StringConstants.SIMILAR_KEY));
            S3Object s3object = s3Client.getObject(new GetObjectRequest(StringConstants.TASTE_CONFIG_BUCKET, tenant + ".config"));

            log.info("Content-Type: "  + s3object.getObjectMetadata().getContentType());
            InputStream input = s3object.getObjectContent();
            ReadSettings(input);
        } catch (AmazonServiceException ase) {
            Utilities.printAWSException(ase);
        } catch (AmazonClientException ace) {
            Utilities.printException(ace);
        }
        catch(IOException ex) {
            Utilities.printException(ex);
        }
    }

    public static String ReadData(InputStream input)  throws IOException {
        // Read one text line at a time and display.
        BufferedReader reader = new BufferedReader(new
                InputStreamReader(input));
        StringBuilder builder = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if(line == null) {
                break;
            }

            builder.append(line);
        }

        return builder.toString();
    }

    public static void ReadSettings(InputStream input)  throws IOException {
        settings = new ArrayList<String>();
        // Read one text line at a time and display.
        BufferedReader reader = new BufferedReader(new
                InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if(line == null) {
                break;
            }
            settings.add(line);
        }
    }
}
