package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.List;

public class SimpleTrendOnlineTransformer extends OnlineTransformer {

    private final Feed feedMin;
    private final Feed feedMax;
    private final Feed feedClose;

    private double trend = 0;
    private double lastClose = 0;
    private double lambda = 0.5;
    private double res;

    public SimpleTrendOnlineTransformer(double lambda, Feed feedMin, Feed feedMax, Feed feedClose, double res) {
        super(1, feedMin, feedMax, feedClose);
        this.feedMin = feedMin;
        this.feedMax = feedMax;
        this.feedClose = feedClose;
        this.lambda = lambda;
        this.res = res;
    }

    @Override
    protected Object getOutput() {
        Double minA = (Double) ((List) arriving.getData()).get(0);
        Double maxA = (Double) ((List) arriving.getData()).get(1);
        Double close = (Double) ((List) arriving.getData()).get(2);
        if (init) {
            double move1 = (maxA - lastClose) - (lastClose - minA);
            double move2 = (maxA - close) - (close - minA);
            double moveRaw = (move1 - move2) * Math.sqrt(maxA - minA)/res;
            trend = trend * (1 - lambda) + moveRaw * lambda;
        } else {
            buffer.clear();
            while (buffer.size() < bufferSize) {
                buffer.add(arriving);
            }
        }
        lastClose = close;
        return trend;
    }

    @Override
    public Feed getCopy() {
        return new SimpleTrendOnlineTransformer(lambda, feedMin.getCopy(), feedMax.getCopy(), feedClose.getCopy(), res);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] STrend and lambda: " + lambda;
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        list.add(feedMin.getElementChain(0));
        list.add(feedMax.getElementChain(0));
        list.add(feedClose.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
