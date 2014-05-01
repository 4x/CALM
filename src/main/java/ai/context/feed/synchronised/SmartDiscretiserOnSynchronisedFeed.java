package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.DataSetUtils;
import ai.context.util.mathematics.SmartDiscretiser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SmartDiscretiserOnSynchronisedFeed implements Feed {

    private SynchronisedFeed feed;
    private long criticalMass = 10000;
    private int clusters = 5;

    private long timeStamp;

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

        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }
        FeedObject fO = feed.getNextComposite(this);
        long time = fO.getTimeStamp();

        List data = new ArrayList();
        DataSetUtils.add(fO.getData(), data);
        for (Object o : data) {
            if (o == null || !(o instanceof Double)) {
                timeStamp = time;
                return new FeedObject(time, null);
            }
        }

        int index = 0;
        List<Integer> output = new ArrayList<Integer>();
        for (Object o : data) {
            Double d = (Double) o;

            if (discretisers.size() <= index) {
                discretisers.add(new SmartDiscretiser(criticalMass, clusters));
            }
            output.add(discretisers.get(index).discretise(d));

            index++;
        }
        FeedObject feedObject = new FeedObject(time, output);
        List<Feed> toRemove = new ArrayList<>();
        for (Feed listener : buffers.keySet()) {
            if (listener != caller) {
                List<FeedObject> list = buffers.get(listener);
                list.add(feedObject);
                if(list.size() > 2000){
                    toRemove.add(listener);
                }
            }
        }
        for(Feed remove : toRemove){
            buffers.remove(remove);
        }
        timeStamp = feedObject.getTimeStamp();
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

    @Override
    public void removeChild(Feed feed) {
        buffers.remove(feed);
    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Smart Discritiser with critical mass: " + criticalMass + " and degrees of feedom: " + clusters + " for feed: " + feed.getDescription(startIndex, padding + " ");
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Synchronised feed with 1 or more outputs",
                "Number of points before ready",
                "Degrees of freedom"
        };
    }

    @Override
    public List getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(feed.getElementChain(element));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return feed.getNumberOfOutputs();
    }
}
