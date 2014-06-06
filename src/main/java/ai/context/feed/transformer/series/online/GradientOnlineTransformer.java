package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class GradientOnlineTransformer extends OnlineTransformer {

    private Feed feedMin;
    private Feed feedMax;
    private Feed feedClose;

    private LinkedList<Double> vals = new LinkedList<>();

    public GradientOnlineTransformer(int bufferSize, Feed feedMin, Feed feedMax, Feed feedClose) {
        super(bufferSize, feedMin, feedMax, feedClose);
        this.feedMin = feedMin;
        this.feedMax = feedMax;
        this.feedClose = feedClose;
    }

    double xMean = 0;
    double x2MeanMinusXMean2 = 0;

    @Override
    protected Object getOutput() {
        Double minA = (Double) ((List) arriving.getData()).get(0);
        Double maxA = (Double) ((List) arriving.getData()).get(1);
        Double close = (Double) ((List) arriving.getData()).get(2);

        long t = arriving.getTimeStamp();
        double val = (minA + maxA + close)/3;
        double grad = 0;
        double diff = 0;
        if (init) {
            vals.add(val);
            vals.pollFirst();

            double yMean = 0;
            double xyMean = 0;

            for(int i = 0; i < bufferSize; i++){
                double y = vals.get(i);
                yMean += y;
                xyMean += i * y;
            }
            yMean /= bufferSize;
            xyMean /= bufferSize;

            grad = (xyMean - xMean*yMean)/x2MeanMinusXMean2;
            diff = yMean + grad*xMean - close;
        } else {
            buffer.clear();
            double x2Mean = 0;
            while (buffer.size() < bufferSize) {
                x2Mean += buffer.size() * buffer.size();
                buffer.add(new FeedObject(0, arriving.getData()));
                vals.add(val);
            }
            x2Mean /= bufferSize;
            xMean = (bufferSize - 1)/2.0;
            x2MeanMinusXMean2 = x2Mean - (xMean * xMean);
        }
        return new Double[]{(double) getLogarithmicDiscretisation(grad, 0, 0.00001), (double)getLogarithmicDiscretisation(diff, 0, 0.0001)};
    }



    @Override
    public Feed getCopy() {
        return new GradientOnlineTransformer(bufferSize, feedMin.getCopy(), feedMax.getCopy(), feedClose.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Gradient and span: " + bufferSize + " for feed: " + feedMin.getDescription(startIndex, padding) + " and " + feedMax.getDescription(startIndex, padding) + " and " + feedClose.getDescription(startIndex, padding);
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
        return 2;
    }
}
