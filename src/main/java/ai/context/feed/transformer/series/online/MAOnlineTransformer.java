package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

public class MAOnlineTransformer extends OnlineTransformer{

    private Feed feed;
    private double sum = 0;

    public MAOnlineTransformer(int bufferSize, Feed feed) {
        super(bufferSize, feed);
        this.feed = feed;
    }

    @Override
    protected Object getOutput() {
        if(init){
            sum -= (Double)leaving.getData();
            sum += (Double)arriving.getData();
        }
        else {
            buffer.clear();
            while(buffer.size() < bufferSize){
                buffer.add(new FeedObject(0, arriving.getData()));
            }
            sum = (Double)arriving.getData() * bufferSize;
        }
        return sum/bufferSize;
    }

    @Override
    public Feed getCopy() {
        return new MAOnlineTransformer(bufferSize, feed.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "["+startIndex+"] MA and span: " + bufferSize + " for feed: " + feed.getDescription(startIndex, padding);
    }
}
