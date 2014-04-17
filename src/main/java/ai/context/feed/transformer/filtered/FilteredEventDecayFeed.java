package ai.context.feed.transformer.filtered;

import ai.context.container.TimedContainer;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;

import java.util.HashMap;
import java.util.LinkedList;

public abstract class FilteredEventDecayFeed implements Feed {

    private Feed rawFeed;
    protected double intensity = 0.0;
    protected double halfLife;
    private long lastSeen;

    private long tRaw = 0;

    private FeedObject previousRaw;


    private TimedContainer container;

    private Object data;
    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    private CSVFeed csvFeed;

    public FilteredEventDecayFeed(Feed rawFeed, double halfLife, TimedContainer container) {
        this.rawFeed = rawFeed;
        if(rawFeed instanceof CSVFeed){
            csvFeed = (CSVFeed) rawFeed;
        }
        this.halfLife = halfLife;
        this.container = container;
        this.lastSeen = (long) (10*halfLife);
    }

    @Override
    public boolean hasNext() {
        return rawFeed.hasNext();
    }

    @Override
    public synchronized FeedObject readNext(Object caller) {

        if(buffers.containsKey(caller) && buffers.get(caller).size() > 0)
        {
            return buffers.get(caller).pollFirst();
        }

        FeedObject raw = null;
        while (tRaw <= container.getTime()){
            raw = rawFeed.readNext(this);

            if(raw == previousRaw){
                break;
            }
            else {
                previousRaw = raw;
            }
            tRaw = raw.getTimeStamp();
            if(pass(raw))
            {
                if(container != null)
                {
                    lastSeen = raw.getTimeStamp();
                }
                else {
                    lastSeen = 0;
                }
                data = process(raw.getData());
                break;
            }
        }

        if(container != null)
        {
            if(container.getTime() - lastSeen > 10 * halfLife){
                intensity = 0;
            }
            else {
                intensity = Math.exp(-((double)(container.getTime() - lastSeen)/halfLife));
            }
        }
        else {
            intensity = Math.exp(-((double)lastSeen/halfLife));
            lastSeen++;
        }

        FeedObject feedObject = new FeedObject(tRaw, new Object[]{data, intensity});
        for(Feed listener : buffers.keySet()){
            if(listener != caller){
                buffers.get(listener).add(feedObject);
            }
        }
        return feedObject;
    }

    protected abstract boolean pass(FeedObject raw);

    protected abstract Object process(Object rawData);

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
        return tRaw;
    }
}
