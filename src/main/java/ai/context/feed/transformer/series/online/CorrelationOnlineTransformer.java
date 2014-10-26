package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class CorrelationOnlineTransformer extends OnlineTransformer {

    private Feed a;
    private Feed b;
    private int lookback;

    public CorrelationOnlineTransformer(Feed a, Feed b, int lookback) {
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
        int nPoints = 0;
        double x_sum = 0;
        double y_sum = 0;
        double x_y_sum = 0;
        double x_2_sum = 0;
        double y_2_sum = 0;

        for (FeedObject element : buffer) {
            Number xN = (Number) ((List) element.getData()).get(0);
            Number yN = (Number) ((List) element.getData()).get(1);

            double x  = 0;
            if(xN != null){
                x = xN.doubleValue();
            }

            double y = 0;
            if(yN != null){
                y = yN.doubleValue();
            }

            nPoints++;

            x_sum += x;
            y_sum += y;

            x_y_sum += (x * y);

            x_2_sum += (x * x);
            y_2_sum += (y * y);

            i++;
        }

        double currentCorrelation = (x_y_sum - (x_sum * y_sum) / nPoints) / (Math.sqrt((x_2_sum - (x_sum * x_sum) / nPoints) * (y_2_sum - (y_sum * y_sum) / nPoints)));
        if("NaN".equals("" + currentCorrelation)) {
            return 0.0;
        }
        return currentCorrelation;
    }

    @Override
    public Feed getCopy() {
        return new CorrelationOnlineTransformer(a.getCopy(), b.getCopy(), lookback);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] CORRELATION for feed: " + a.getDescription(startIndex, padding) + " and " + b.getDescription(startIndex, padding);
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
