package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchronisedFeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class CompoundedTransformer implements Feed {
    private SynchronisedFeed feed;
    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    private long timeStamp;

    protected CompoundedTransformer(Feed[] feeds) {
        for (Feed feed : feeds) {
            this.feed = new SynchronisedFeed(feed, this.feed);
        }
    }

    public synchronized FeedObject readNext(Object caller) {
        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }

        FeedObject data = feed.getNextComposite(this);
        timeStamp = data.getTimeStamp();
        FeedObject feedObject = new FeedObject(data.getTimeStamp(), getOutput(data.getData()));
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
        return feedObject;
    }

    protected abstract Object getOutput(Object input);

    @Override
    public boolean hasNext() {
        return true;
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
        return timeStamp;
    }
}
