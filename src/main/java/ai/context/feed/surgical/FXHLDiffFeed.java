package ai.context.feed.surgical;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.List;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;


public class FXHLDiffFeed extends AbstractSurgicalFeed {

    private double resolution;
    private Feed rawFeed;

    public FXHLDiffFeed(Feed rawFeed, double resolution) {
        super(rawFeed);
        this.rawFeed = rawFeed;
        this.resolution = resolution;
    }

    @Override
    protected synchronized FeedObject operate(long time, List row) {
        return new FeedObject(time, getLogarithmicDiscretisation((Double) row.get(1) - (Double) row.get(2), 0, resolution));
    }

    @Override
    public Feed getCopy() {
        return new FXHLDiffFeed(rawFeed.getCopy(), resolution);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Difference between high and low for: " + rawFeed.getDescription(startIndex, padding);
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Parent feed giving OHCL values",
                "Resolution of values"
        };
    }

    @Override
    public List getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(rawFeed.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
