package datomata.recommendations;

import datomata.recommendations.aws.S3Wrapper;
import datomata.recommendations.aws.SQSWrapper;
import org.apache.commons.logging.Log;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;

public class Root {
    private static Logger log = Logger.getLogger(S3Wrapper.class.getName());

    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Following arguments need to be provided: " +
                                "TenantId");
            return;
        }

        for(int index = 0; index < args.length; index++) {
            if(args[index] == null || args[index].length() < 1) {
                System.out.println(String.format("Invalid argument %d", args[index]));
            }
        }

        SQSWrapper wrapper = SQSWrapper.getInstance();
        BlockingQueue<String[]> queue = wrapper.setupQueueListener(args[0]);

        // Create the child worker thread
        UserQueueMessageWorker worker = new UserQueueMessageWorker(queue, args[0]);
        worker.start();

        while(true) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String s = br.readLine();
                if(s.toUpperCase() == "Q") {
                    break;
                }
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
    }
}