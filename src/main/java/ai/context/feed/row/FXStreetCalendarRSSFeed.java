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

public class FXStreetCalendarRSSFeed extends RowFeed {

    private Thread updater;
    private long pollingFrequency = 10000;
    private long reached = 0;
    private HashSet<Integer> existing = new HashSet<Integer>();

    private XmlReader reader = null;
    private SyndFeed feed;

    private long timeStamp;

    private int max = 100;
    private Queue<FeedObject> queue = new ArrayBlockingQueue<FeedObject>(max);

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();


    private boolean closed = false;
    private URL url;

    public FXStreetCalendarRSSFeed() {

        try {
            url = new URL("http://feeds.fxstreet.com/fundamental/economic-calendar?format=xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
        updater = new Thread() {
            @Override
            public void run() {
                while (!closed) {
                    if (queue.size() < max) {
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                            reader = new XmlReader(url);
                            feed = new SyndFeedInput().build(reader);


                            TreeMap<Long, LinkedList<SyndEntry>> list = new TreeMap<>();
                            for (Iterator i = feed.getEntries().iterator(); i.hasNext(); ) {
                                SyndEntry entry = (SyndEntry) i.next();

                                long t = entry.getPublishedDate().getTime();
                                if (t >= reached && !existing.contains(entry.getTitle().hashCode())) {
                                    if (!list.containsKey(t)) {
                                        list.put(t, new LinkedList<SyndEntry>());
                                    }
                                    list.get(t).add(entry);
                                }
                            }

                            while (!list.isEmpty()) {
                                try {
                                    SyndEntry entry = list.firstEntry().getValue().poll();
                                    if (list.firstEntry().getValue().isEmpty()) {
                                        list.remove(list.firstKey());
                                    }
                                    Date date = entry.getPublishedDate();

                                    String title = entry.getTitle();

                                    String country = FXStreetCountryMapping.getMapping(title.split(":")[0]);
                                    String event = title.split(":")[1].substring(1);

                                    if (reached < date.getTime()) {
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
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            //e.printStackTrace();
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

        updater.start();
    }

    public void close() {
        closed = true;
        try {
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        return !closed;
    }

    @Override
    public synchronized FeedObject readNext(Object caller) {
        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }
        synchronized (queue) {
            while (queue.isEmpty()) {
                try {
                    Thread.sleep(pollingFrequency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            FeedObject feedObject = queue.poll();
            List<Feed> toRemove = new ArrayList<>();
            for (Feed listener : buffers.keySet()) {
                if (listener != caller) {
                    List<FeedObject> list = buffers.get(listener);
                    list.add(feedObject);
                    if(list.size() > 2000){
                        toRemove.add(listener);
                    }
                }
            }
            for(Feed remove : toRemove){
                buffers.remove(remove);
            }
            timeStamp = feedObject.getTimeStamp();
            return feedObject;
        }
    }

    @Override
    public RowFeed getCopy() {
        return new FXStreetCalendarRSSFeed();
    }

    @Override
    public void removeChild(Feed feed) {
        buffers.remove(feed);
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] FX Street Calendar Feed";
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[0];
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 6;
    }
}
