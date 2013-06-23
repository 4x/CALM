package ai.context.feed.transformer.single.unpadded;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.DataSetUtils;

import java.util.ArrayList;
import java.util.List;

import static ai.context.util.mathematics.Discretiser.getLinearDiscretisation;

public class LinearDiscretiser implements Feed{

    private double resolution;
    private double benchmark;
    private int feedComponent;

    private Feed feed;

    private long timeStamp;

    public LinearDiscretiser(double resolution, double benchmark, Feed feed, int feedComponent)
    {
        this.benchmark = benchmark;
        this.resolution = resolution;
        this.feedComponent = feedComponent;
        this.feed = feed;
    }

    public synchronized FeedObject readNext(Object caller) {

        FeedObject in = feed.readNext(this);
        Object input = in.getData();
        if(input == null)
        {
            return new FeedObject(in.getTimeStamp(), null);
        }

        List list = new ArrayList<>();
        DataSetUtils.add(input, list);
        double value = 0;

        if(list.size() <= feedComponent || list.get(feedComponent) == null)
        {
            return new FeedObject(in.getTimeStamp(), null);
        }
        value = (Double)list.get(feedComponent);

        Object data = getLinearDiscretisation(value, benchmark, resolution);
        timeStamp = in.getTimeStamp();
        return new FeedObject(in.getTimeStamp(), data);
    }

    @Override
    public Feed getCopy() {
        return new LinearDiscretiser(resolution, benchmark, feed.getCopy(), feedComponent);
    }

    @Override
    public void addChild(Feed feed) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasNext() {
        return feed.hasNext();
    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }
}
