package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class CrossingSeriesOnlineTransformer extends OnlineTransformer {

    private Feed a;
    private Feed b;
    private int lookback;

    public CrossingSeriesOnlineTransformer(Feed a, Feed b, int lookback) {
        super(lookback, a, b);
        this.a = a;
        this.b = b;
        this.lookback = lookback;
    }

    @Override
    protected Object getOutput() {

        if (!init) {
            buffer.clear();
            while (buffer.size() < bufferSize) {
                buffer.add(new FeedObject(0, arriving.getData()));
            }
        }

        TreeMap<Integer, Integer> crossings = new TreeMap<>();
        boolean aOverB = true;
        int i = 0;
        for (FeedObject element : buffer) {
            Double aVal = (Double) ((List) element.getData()).get(0);
            Double bVal = (Double) ((List) element.getData()).get(1);
            if (i > 0) {
                if (aOverB && bVal > aVal) {
                    crossings.put(i, -1);
                } else if (!aOverB && bVal < aVal) {
                    crossings.put(i, 1);
                }
            }

            if (bVal > aVal) {
                aOverB = false;
            } else if (aVal > bVal) {
                aOverB = true;
            }

            i++;
        }

        if (crossings.size() > 0) {
            double val = ((double) (crossings.lastEntry().getValue() * (lookback - crossings.lastKey()))) / lookback;
            return val / crossings.size();
        }

        return 0.0;
    }

    @Override
    public Feed getCopy() {
        return new CrossingSeriesOnlineTransformer(a.getCopy(), b.getCopy(), lookback);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] CROSSING for feed: " + a.getDescription(startIndex, padding) + " and " + b.getDescription(startIndex, padding);
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(a.getElementChain(0));
        list.add(b.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
