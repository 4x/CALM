package ai.context.learning;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchronisedFeed;

import java.util.List;

public class LearnerFeedFromSynchronisedFeed implements LearnerFeed, Feed {

    SynchronisedFeed feed;
    public LearnerFeedFromSynchronisedFeed(SynchronisedFeed feed)
    {
        this.feed = feed;
        feed.addChild(this);
        feed.init();
    }

    @Override
    public boolean hasNext() {
        return feed.hasNext();
    }

    @Override
    public DataObject readNext() {

        FeedObject data = feed.getNextComposite(this);
        List<Object> content = ((List)data.getData());
        double[] value = new double[]{(Double) content.get(3), (Double) content.get(1), (Double) content.get(2), (Double) content.get(0)};
        int[] signal = new int[content.size() - 5];
        if(signal == null)
        {
            return new DataObject(data.getTimeStamp(), null, null);
        }
        int index = -5;
        for(Object signalValue : content)
        {
            if(signalValue == null)
            {
                return new DataObject(data.getTimeStamp(), null, null);
            }
            if(index >= 0)
            {
                signal[index] = (Integer)signalValue;
            }
            index++;
        }
        return new DataObject(data.getTimeStamp(), signal, value);
    }

    @Override
    public FeedObject readNext(Object caller) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Feed getCopy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addChild(Feed feed) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
