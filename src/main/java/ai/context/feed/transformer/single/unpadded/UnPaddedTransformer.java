package ai.context.feed.transformer.single.unpadded;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

public abstract class UnPaddedTransformer implements Feed {
    private Feed[] feeds;

    private long timeStamp = 0;

    protected UnPaddedTransformer(Feed[] feeds) {
        this.feeds = feeds;
    }

    public synchronized FeedObject readNext(Object caller) {
        Object[] data = new Object[feeds.length];
        int index = 0;

        for (Feed feed : feeds) {
            FeedObject feedObject = feed.readNext(this);
            timeStamp = feedObject.getTimeStamp();
            data[index] = feedObject.getData();
            index++;
        }

        return new FeedObject(timeStamp, getOutput(data));
    }

    protected abstract Object getOutput(Object[] input);

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }
}
