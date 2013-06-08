package ai.context.feed.row;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.mapping.FXStreetCountryMapping;
import ai.context.util.StringUtils;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class FXStreetCalendarRSSFeed extends RowFeed{

    private Thread updater;
    private long pollingFrequency = 5000;
    private long reached = 0;
    private HashSet<Integer> existing = new HashSet<Integer>();

    private XmlReader reader = null;
    private SyndFeed feed;

    private int max = 100;
    private Queue<FeedObject> queue = new ArrayBlockingQueue<FeedObject>(max);

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();


    private boolean closed = false;

    public FXStreetCalendarRSSFeed() {

        updater = new Thread() {
            @Override
            public void run() {
                while(!closed)
                {
                    if(queue.size() < max)
                    {
                        Stack<SyndEntry> stack = new Stack<SyndEntry>();
                        for (Iterator i = feed.getEntries().iterator(); i.hasNext();) {
                            SyndEntry entry = (SyndEntry) i.next();

                            if(entry.getPublishedDate().getTime() >= reached && !existing.contains(entry.getTitle().hashCode()))
                            {
                                stack.add(entry);
                            }
                        }

                        while(!stack.empty())
                        {
                            SyndEntry entry = stack.pop();
                            Date date = entry.getPublishedDate();

                            String title = entry.getTitle();

                            String country = FXStreetCountryMapping.getMapping(title.split(":")[0]);
                            String event = title.split(":")[1].substring(1);

                            if(reached < date.getTime())
                            {
                                reached = date.getTime();
                                existing.clear();
                            }
                            existing.add(title.hashCode());

                            String content = entry.getDescription().getValue();
                            String[] parts = content.split("<td>");

                            int volatility = 0;

                            double consensus = StringUtils.extractDouble(parts[8]);
                            double actual = StringUtils.extractDouble(parts[9]);
                            double previous = StringUtils.extractDouble(parts[10]);

                            Object[] data = new Object[]{event, country, volatility, actual, previous, consensus};
                            queue.add(new FeedObject(date.getTime(), data));
                        }
                    }
                    try {
                        Thread.sleep(pollingFrequency);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        try {
            URL url = new URL("http://feeds.fxstreet.com/fundamental/economic-calendar?format=xml");
            reader = new XmlReader(url);
            feed = new SyndFeedInput().build(reader);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        updater.start();
    }

    public void close()
    {
        closed = true;
        try{
            reader.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        return !closed;
    }

    @Override
    public FeedObject readNext(Object caller) {
        if(buffers.containsKey(caller) && buffers.get(caller).size() > 0)
        {
            return buffers.get(caller).pollFirst();
        }
        synchronized (queue)
        {
            while (queue.isEmpty())
            {
                try {
                    Thread.sleep(pollingFrequency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            FeedObject feedObject = queue.poll();
            for(Feed listener : buffers.keySet()){
                if(listener != caller){
                    buffers.get(listener).add(feedObject);
                }
            }
            return feedObject;
        }
    }

    @Override
    public RowFeed getCopy() {
        return new FXStreetCalendarRSSFeed();
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }
}
