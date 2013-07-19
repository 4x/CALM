package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

public class StandardDeviationOnlineTransformer extends OnlineTransformer{

    private double stdDv = 0;
    private int span = 0;

    private double sumX = 0;
    private double sumX2 = 0;

    private Feed feed;

    public StandardDeviationOnlineTransformer(int span, Feed feed) {
        super(span, feed);
        this.feed = feed;
        this.span = span;
    }

    @Override
    protected Object getOutput() {
        if(init){
            sumX -= (Double)leaving.getData();
            sumX += (Double)arriving.getData();

            sumX2 -= Math.pow((Double)leaving.getData(), 2);
            sumX2 += Math.pow((Double)arriving.getData(), 2);
        }
        else {
            buffer.clear();
            while(buffer.size() < bufferSize){
                buffer.add(new FeedObject(0, arriving.getData()));
            }

            for(FeedObject element : buffer){
                double val = (Double)element.getData();
                sumX += val;
                sumX2 += (val * val);
            }
        }

        stdDv = Math.sqrt(sumX2/span - Math.pow(sumX/span,2));
        return stdDv;
    }

    @Override
    public Feed getCopy() {
        return new StandardDeviationOnlineTransformer(span, feed.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "["+startIndex+"] STDDEV and span: " + span + " for feed: " + feed.getDescription(startIndex, padding);
    }

}
