package ai.context.feed.transformer.series.online;


import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.List;

public class DeltaOnlineTransformer extends OnlineTransformer {

    private Feed feed;

    public DeltaOnlineTransformer(int bufferSize, Feed feed) {
        super(bufferSize, feed);
        this.feed = feed;
    }

    @Override
    protected Object getOutput() {
        if (init) {
            return (Double) arriving.getData() - (Double) leaving.getData();
        } else {
            buffer.clear();
            while (buffer.size() < bufferSize) {
                buffer.add(new FeedObject(0, arriving.getData()));
            }

            return 0.0;
        }
    }

    @Override
    public Feed getCopy() {
        return new DeltaOnlineTransformer(bufferSize, feed.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] DELTA and span: " + bufferSize + " for feed: " + feed.getDescription(startIndex, padding);
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        list.add(feed.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
