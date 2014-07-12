package ai.context.feed.row;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.XmlReader;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.*;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class FXStreetCalendarScheduleFeed extends RowFeed {

    private Thread updater;
    private long pollingFrequency = 86400000L;
    private long reached = 0;
    private HashSet<Integer> existing = new HashSet<Integer>();

    private XmlReader reader = null;
    private SyndFeed feed;

    private long timeStamp;

    private int max = 1000;
    private Queue<FeedObject> queue = new ArrayBlockingQueue<FeedObject>(max);

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();


    private boolean closed = false;
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");


    public FXStreetCalendarScheduleFeed() {

        updater = new Thread() {
            @Override
            public void run() {
            while (!closed) {
                try {
                    if (queue.size() < max) {

                        InputStream input = new URL("http://www.fxstreet.com/economic-calendar/calendar.ics").openStream();
                        CalendarBuilder builder = new CalendarBuilder();
                        net.fortuna.ical4j.model.Calendar calendar = builder.build(input);

                        for(int i = 0; i < calendar.getComponents().size(); i++){
                            Component c = (Component) calendar.getComponents().get(i);

                            Property dStr = c.getProperties().getProperty("DTSTAMP");

                            if(dStr != null){
                                long t = format.parse(dStr.getValue()).getTime();
                                String location = c.getProperties().getProperty("LOCATION").getValue();
                                String type = c.getProperties().getProperty("SUMMARY").getValue().substring(4).split(" \\[")[0];

                                Object[] data = new Object[]{type, location};
                                queue.add(new FeedObject(t, data));
                            }
                        }
                        input.close();
                    }

                    Thread.sleep(pollingFrequency);
                } catch (Exception e) {
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
        return !closed && queue.size() > 0;
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
        return new FXStreetCalendarScheduleFeed();
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
        return padding + "[" + startIndex + "] FX Street Calendar Schedule";
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
        return 2;
    }
}
