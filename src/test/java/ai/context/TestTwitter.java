package ai.context;

import ai.context.util.analysis.ReflectionHelper;
import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestTwitter {
    public static void main(String[] args) {
        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(100000);
        BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>(1000);

        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

        // Optional: set up some followings and track terms
        List<Long> followings = Lists.newArrayList(1234L, 566788L);
        List<String> terms = Lists.newArrayList("economy", "markets", "fx");
        endpoint.followings(followings);
        endpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth =
                new OAuth1("UfyBnS7xUOn732zmq75uHw",
                        "hUBqdhLffFbf8DJ6C6TmSt2HPXVzGdKD1qkOrxQBeA",
                        "2358514777-w5kd0d8ilDPgfe3AiQ9RC1wDqaKZr2uRCGdAbHk",
                        "9vTQbeLdY671WfAW1JMtUSF0JTLPwlra7NWRptvGvD3dh");

        ClientBuilder builder = new ClientBuilder()
                .name("Dandelion Machine Learning")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(endpoint)
                .processor(new StringDelimitedProcessor(msgQueue))
                .eventMessageQueue(eventQueue);                          // optional: use this if you want to process client events

        Client hosebirdClient = builder.build();
        // Attempts to establish a connection.
        hosebirdClient.connect();

        ReflectionHelper helper = new ReflectionHelper();
        long i = 0;
        while (!hosebirdClient.isDone()) {
            try {
                Object data = msgQueue.take();
                System.out.println(data);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
