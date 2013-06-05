package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.mathematics.SmartDiscretiser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SmartDiscretiserOnSynchronisedFeed implements Feed {

    private SynchronisedFeed feed;
    private long criticalMass = 10000;
    private int clusters = 5;

    private ArrayList<SmartDiscretiser> discretisers = new ArrayList<SmartDiscretiser>();
    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    public SmartDiscretiserOnSynchronisedFeed(SynchronisedFeed feed, long criticalMass, int clusters) {
        this.feed = feed;
        this.criticalMass = criticalMass;
        this.clusters = clusters;
    }

    @Override
    public boolean hasNext() {
        return feed.hasNext();
    }

    @Override
    public FeedObject readNext(Object caller) {

        if(buffers.containsKey(caller) && buffers.get(caller).size() > 0)
        {
            return buffers.get(caller).pollFirst();
        }
        FeedObject fO = feed.getNextComposite(this);
        long time = fO.getTimeStamp();
        List data = (List) fO.getData();
        for(Object o : data)
        {
            if(o == null || !(o instanceof Double))
            {
                return new FeedObject(time, null);
            }
        }

        int index = 0;
        List<Integer> output = new ArrayList<Integer>();
        for(Object o : data)
        {
            Double d = (Double) o;

            if(discretisers.size() <= index)
            {
                discretisers.add(new SmartDiscretiser(criticalMass, clusters));
            }
            output.add(discretisers.get(index).discretise(d));

            index++;
        }
        FeedObject feedObject = new FeedObject(time, output);
        for(Feed listener : buffers.keySet()){
            if(listener != caller){
                buffers.get(listener).add(feedObject);
            }
        }
        return feedObject;
    }

    @Override
    public Feed getCopy() {
        return new SmartDiscretiserOnSynchronisedFeed((SynchronisedFeed) feed.getCopy(), criticalMass, clusters);
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }
}
