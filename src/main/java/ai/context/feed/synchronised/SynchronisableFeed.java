package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.DataSetUtils;
import ai.context.util.SharedMutableObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class SynchronisableFeed implements ISynchFeed {

    protected List<SynchronisableFeed> feeds = new ArrayList<SynchronisableFeed>();
    private SharedMutableObject<Long> timeStamp = new SharedMutableObject<Long>(-1L);
    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();
    private SharedMutableObject<SynchronisableFeed> leader = new SharedMutableObject<SynchronisableFeed>(this);

    private FeedObject current;
    private Object currentData;

    private List<FeedObject> latest = new ArrayList<>();

    protected SynchronisableFeed(SynchronisableFeed sibling) {
        if (sibling != null) {
            this.timeStamp = sibling.getTimeStamp();
            this.feeds = sibling.getFeeds();
            this.leader = sibling.getLeader();
        }
        feeds.add(this);
    }

    public synchronized FeedObject getNextComposite(Object caller) {
        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }
        for (SynchronisableFeed feed : feeds) {
            feed.checkLeader();
        }
        List<Object> data = new ArrayList<Object>();

        long currentTimeStamp = timeStamp.getValue();
        for (SynchronisableFeed feed : feeds) {
            if (leader.getValue() == feed) {
                feed.currentData = feed.current.getData();
                DataSetUtils.add(feed.currentData, data);
                feed.current = null;
            } else {
                if (feed.current.getTimeStamp() == currentTimeStamp) {
                    feed.currentData = feed.current.getData();
                    feed.current = null;
                }

                DataSetUtils.add(feed.currentData, data);
            }
        }

        long timeToReturn = timeStamp.getValue();
        timeStamp.setValue(-1L);

        FeedObject feedObject = new FeedObject(timeToReturn, data);
        if (feeds.size() == 1) {
            feedObject = new FeedObject(timeToReturn, currentData);
        }
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

        latest.add(feedObject);
        if(latest.size() > 500){
            latest.remove(0);
        }
        return feedObject;
    }

    public synchronized FeedObject getNext() {
        FeedObject toReturn = null;

        if (leader.getValue() == this) {
            toReturn = current;
            currentData = current.getData();
            current = null;
            timeStamp.setValue(-1L);
            for (SynchronisableFeed feed : feeds) {
                feed.checkLeader();
            }
        } else {
            toReturn = new FeedObject(timeStamp.getValue(), currentData);
        }

        return toReturn;
    }

    public SharedMutableObject<Long> getTimeStamp() {
        return timeStamp;
    }

    public List<SynchronisableFeed> getFeeds() {
        return feeds;
    }

    public SharedMutableObject<SynchronisableFeed> getLeader() {
        return leader;
    }

    public void checkLeader() {
        if (current == null) {
            current = readNext(this);

            if(current == null){
                System.err.println("Stopping run as end of data");
                System.exit(0);
            }
        }
        if (timeStamp.getValue() < 0) {
            timeStamp.setValue(current.getTimeStamp());
            leader.setValue(this);
        } else if (current.getTimeStamp() <= timeStamp.getValue()) {
            timeStamp.setValue(current.getTimeStamp());
            leader.setValue(this);
        }
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }

    @Override
    public void removeChild(Feed feed) {
        buffers.remove(feed);
    }

    @Override
    public long getLatestTime() {
        return timeStamp.getValue();
    }

    @Override
    public List<Feed> rawFeeds() {
        List<Feed> feeds = new ArrayList<>();
        for(SynchronisableFeed feed : this.feeds){
            feeds.add(feed.getRawFeed());
        }
        return feeds;
    }

    @Override
    public List<FeedObject> getLatest(int n) {
        int start = Math.max(0, latest.size() - n);
        return latest.subList(start, latest.size() - 1);
    }
}
