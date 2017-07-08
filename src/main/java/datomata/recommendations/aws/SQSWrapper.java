package datomata.recommendations.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import datomata.recommendations.StringConstants;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class SQSWrapper {

    private static SQSWrapper instance;
    private static Logger log = Logger.getLogger(S3Wrapper.class.getName());
    private AmazonSQSClient sqs;
    private Timer timer;

    private SQSWrapper() {
    }

    public static SQSWrapper getInstance() {
        if(instance == null) {
            instance = new SQSWrapper();
            instance.sqs = new AmazonSQSClient(new BasicAWSCredentials(StringConstants.ACCESS_KEY, StringConstants.ACCESS_SECRET));
            instance.sqs.setRegion(Region.getRegion(Regions.US_WEST_2));
        }

        return instance;
    }

    public ArrayList<String> getUserIds() {
        return null;
    }

    public void cancelTimer(String tenantId) {
        timer.cancel();
    }

    public BlockingQueue<String[]> setupQueueListener(final String tenantId) {

        // Create a synchronous queue
        final BlockingQueue<String[]> queue = new SynchronousQueue<String[]>();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                while(true) {
                    try {
                        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(S3Wrapper.getUserMonitoringQueueUrl(tenantId));
                        receiveMessageRequest.setMaxNumberOfMessages(10);
                        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
                        for (Message message : messages) {
                            log.info("MessageId:     " + message.getMessageId());
                            log.info("ReceiptHandle: " + message.getReceiptHandle());
                            log.info("MD5OfBody:     " + message.getMD5OfBody());
                            log.info("Body:          " + message.getBody());

                            String userIdsFlattened = message.getBody();
                            String[] queueEntry = new String[2];
                            queueEntry[0] = userIdsFlattened;
                            queueEntry[1] = message.getReceiptHandle();
                            queue.put(queueEntry);
                        }
                    } catch (Exception ex) {
                        log.info(String.format("Error in receiving SQS messages from User Activity Queue %s", ex.getMessage()));
                    }
                }
            }
        }, 1, 15);

        return queue;
    }

    public void deleteQueueMessage(String tenantId, String recipientHandle) {
        sqs.deleteMessage(new DeleteMessageRequest()
                .withQueueUrl(S3Wrapper.getUserMonitoringQueueUrl(tenantId))
                .withReceiptHandle(recipientHandle));
    }
}
