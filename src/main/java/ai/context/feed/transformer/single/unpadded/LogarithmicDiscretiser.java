package ai.context.feed.transformer.single.unpadded;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.List;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class LogarithmicDiscretiser extends UnPaddedTransformer{
    private double resolution;
    private double benchmark;
    private int feedComponent;

    private Feed feed;

    public LogarithmicDiscretiser(double resolution, double benchmark, Feed feed, int feedComponent)
    {
        super(new Feed[]{feed});
        this.benchmark = benchmark;
        this.resolution = resolution;
        this.feedComponent = feedComponent;
        this.feed = feed;
    }
    @Override
    protected Object getOutput(Object[] input) {

        if(input == null || input[0] == null)
        {
            return null;
        }

        double value = 0;
        if(feedComponent >= 0)
        {
            value = ((Double[])input[0])[feedComponent].doubleValue();
        }
        else {
            value = ((Double)input[0]).doubleValue();
        }
        return getLogarithmicDiscretisation(value, benchmark, resolution);
    }

    @Override
    public Feed getCopy() {
        return new LogarithmicDiscretiser(resolution, benchmark, feed.getCopy(), feedComponent);
    }

    @Override
    public void addChild(Feed feed) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeChild(Feed feed) {

    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Logarithmic dicretiser with benchmark: "+benchmark+", resolution: "+resolution+", feed component: "+feedComponent+" for: " + feed.getDescription(startIndex, padding);
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
