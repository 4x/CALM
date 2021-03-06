package ai.context.feed.surgical;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.List;


public class FXModuloFeed extends AbstractSurgicalFeed {

    private Feed rawFeed;
    private double resolution;
    private int modulo;

    public FXModuloFeed(Feed rawFeed, double resolution, int modulo) {
        super(rawFeed);
        this.rawFeed = rawFeed;
        this.resolution = resolution;
        this.modulo = modulo;
    }

    @Override
    protected synchronized FeedObject operate(long time, List row) {
        return new FeedObject(time, ((Double) row.get(3) / resolution) % modulo);
    }

    @Override
    public Feed getCopy() {
        return new FXModuloFeed(rawFeed.getCopy(), resolution, modulo);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Modulo 100 for CLOSE from: " + rawFeed.getDescription(startIndex, padding);
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Parent feed",
                "Resolution of values",
                "Modulo"
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
