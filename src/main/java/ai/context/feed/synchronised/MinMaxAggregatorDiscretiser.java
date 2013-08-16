package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.mathematics.MinMaxDiscretiser;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MinMaxAggregatorDiscretiser implements Feed {


    private SynchronisedFeed feed;
    private long criticalMass = 10000;
    private int clusters = 5;

    private long timeStamp;
    private BufferedWriter fileOutputStream;

    private ArrayList<MinMaxDiscretiser> discretisers = new ArrayList<MinMaxDiscretiser>();
    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    private boolean lockable = false;

    public MinMaxAggregatorDiscretiser(SynchronisedFeed feed, long criticalMass, int clusters) {
        this.feed = feed;
        this.criticalMass = criticalMass;
        this.clusters = clusters;

        try {
            fileOutputStream = new BufferedWriter(new FileWriter("SIGNAL_"+ System.currentTimeMillis() + ".csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        String toPrintOut = "";
        for(Object o : data)
        {
            toPrintOut += o + ",";
            if(o == null || !(o instanceof Number))
            {
                System.out.println(toPrintOut);
                timeStamp = time;
                return new FeedObject(time, null);
            }
        }

        int index = 0;
        List<Integer> output = new ArrayList<Integer>();
        String toPrint = "";
        for(Object o : data)
        {
            double d = ((Number) o).doubleValue();

            if(discretisers.size() <= index)
            {
                MinMaxDiscretiser discretiser = new MinMaxDiscretiser(criticalMass, clusters);
                discretiser.setLockable(lockable);
                discretisers.add(discretiser);
            }
            int signal = discretisers.get(index).discretise(d);
            output.add(signal);

            toPrint += d + "," + signal + ",";

            index++;
        }
        //appendToFile(toPrint);

        FeedObject feedObject = new FeedObject(time, output);
        for(Feed listener : buffers.keySet()){
            if(listener != caller){
                buffers.get(listener).add(feedObject);
            }
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
    public long getLatestTime() {
        return timeStamp;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Smart Discritiser with critical mass: " + criticalMass + " and degrees of feedom: " + clusters + " for feed: " + feed.getDescription(startIndex, padding + " ");
    }

    private void appendToFile(String data){
        try {
            fileOutputStream.write(data + "\n");
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void lock(){
        lockable = true;
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Synchronised feed with 1 or more outputs",
                "Number of points before ready",
                "Degrees of freedom"
        };
    }
}
