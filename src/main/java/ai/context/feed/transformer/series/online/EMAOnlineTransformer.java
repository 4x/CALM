package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.List;

public class EMAOnlineTransformer extends OnlineTransformer {

    private Feed feed;
    private double ema = 0;
    private double lambda = 0.5;

    public EMAOnlineTransformer(int lambda, Feed feed) {
        super(1, feed);
        this.feed = feed;
        this.lambda = lambda;
    }

    @Override
    protected Object getOutput() {
        if (init) {
            ema = ((Double) arriving.getData() * lambda) + (ema * (1 - lambda));
        } else {
            buffer.clear();
            while (buffer.size() < bufferSize) {
                buffer.add(arriving);
            }
            ema = (Double) arriving.getData();
        }
        return ema;
    }

    @Override
    public Feed getCopy() {
        return new EMAOnlineTransformer(bufferSize, feed.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] EMA and lambda: " + bufferSize + " for feed: " + feed.getDescription(startIndex, padding);
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
