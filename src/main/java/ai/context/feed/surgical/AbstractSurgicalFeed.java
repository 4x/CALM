package ai.context.feed.surgical;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.DataSetUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractSurgicalFeed implements Feed {

    private long timeStamp;
    private Feed feed;
    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    public AbstractSurgicalFeed(Feed rawFeed) {
        this.feed = rawFeed;
        rawFeed.addChild(this);
    }

    @Override
    public boolean hasNext() {
        return feed.hasNext();
    }

    @Override
    public synchronized FeedObject readNext(Object caller) {

        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }
        FeedObject rawData = feed.readNext(this);
        List data = new ArrayList();
        DataSetUtils.add(rawData.getData(), data);
        FeedObject feedObject = operate(rawData.getTimeStamp(), data);
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

    protected abstract FeedObject operate(long time, List row);

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
        return timeStamp;
    }
}
