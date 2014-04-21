package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class MinMaxDistanceTransformer extends OnlineTransformer {

    private Feed feedMin;
    private Feed feedMax;
    private Feed feedClose;
    private double min;
    private double max;
    private LinkedList<Double> mins = new LinkedList<>();
    private LinkedList<Double> maxs = new LinkedList<>();

    public MinMaxDistanceTransformer(int bufferSize, Feed feedMin, Feed feedMax, Feed feedClose) {
        super(bufferSize, feedMin, feedMax, feedClose);
        this.feedMin = feedMin;
        this.feedMax = feedMax;
        this.feedClose = feedClose;
    }

    @Override
    protected Object getOutput() {
        Double minA = (Double) ((List) arriving.getData()).get(0);
        Double maxA = (Double) ((List) arriving.getData()).get(1);
        Double close = (Double) ((List) arriving.getData()).get(2);

        if (init) {

            mins.add(minA);
            maxs.add(maxA);
            mins.pollFirst();
            maxs.pollFirst();

            if (minA < min) {
                min = minA;
            }

            if (maxA > max) {
                max = maxA;
            }

            Double minL = (Double) ((List) leaving.getData()).get(0);
            Double maxL = (Double) ((List) leaving.getData()).get(1);

            if (minL <= min) {
                refreshMin();
            }

            if (maxL >= min) {
                refreshMax();
            }
        } else {
            buffer.clear();
            while (buffer.size() < bufferSize) {
                buffer.add(new FeedObject(0, arriving.getData()));

                mins.add(minA);
                maxs.add(maxA);
                min = minA;
                max = maxA;
            }
        }
        return new Double[]{this.min, this.max, (double) getLogarithmicDiscretisation(max - close, 0, 0.0001), (double) getLogarithmicDiscretisation(close - min, 0, 0.0001)};
    }

    private void refreshMin() {
        min = mins.getFirst();
        for (double val : mins) {
            if (val < min) {
                min = val;
            }
        }
    }

    private void refreshMax() {
        max = maxs.getFirst();
        for (double val : maxs) {
            if (val > max) {
                max = val;
            }
        }
    }

    @Override
    public Feed getCopy() {
        return new MinMaxDistanceTransformer(bufferSize, feedMin.getCopy(), feedMax.getCopy(), feedClose.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] MINMAXDistance and span: " + bufferSize + " for feed: " + feedMin.getDescription(startIndex, padding) + " and " + feedMax.getDescription(startIndex, padding) + " and " + feedClose.getDescription(startIndex, padding);
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
        return 4;
    }
}
