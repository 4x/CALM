package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchronisedFeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class OnlineTransformer implements Feed {

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    protected LinkedList<FeedObject> buffer = new LinkedList();

    protected FeedObject arriving;
    protected FeedObject leaving;
    protected int bufferSize = 10;
    protected boolean init = false;


    protected SynchronisedFeed source;
    protected long timeStamp;

    public OnlineTransformer(int bufferSize, Feed... feeds) {
        this.bufferSize = bufferSize;

        for (Feed feed : feeds) {
            source = new SynchronisedFeed(feed, source);
        }
        source.addChild(this);
        while (buffer.size() < bufferSize) {
            buffer.add(new FeedObject(0, null));
        }
    }

    @Override
    public FeedObject readNext(Object caller) {
        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }

        arriving = source.getNextComposite(this);

        if (init) {
            buffer.add(arriving);
            leaving = buffer.pollFirst();
        } else {
            while (buffer.size() < bufferSize) {
                buffer.add(arriving);
            }
        }

        timeStamp = arriving.getTimeStamp();
        FeedObject feedObject = new FeedObject(timeStamp, getOutput());
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
        init = true;
        return feedObject;
    }

    protected abstract Object getOutput();

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
