package ai.context.feed.manipulation;

import ai.context.feed.FeedObject;
import ai.context.util.analysis.LookAheadScheduler;
import ai.context.util.mathematics.Discretiser;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TimeDecaySingleSentimentManipulator implements Manipulator{

    private final String location;
    private final String type;
    private LookAheadScheduler schedule;

    private TreeMap<Long, FeedObject> mem = new TreeMap<>();

    public TimeDecaySingleSentimentManipulator(String location, String type, LookAheadScheduler schedule) {
        this.schedule = schedule;
        this.location = location;
        this.type = type;
    }

    @Override
    public FeedObject<Integer[]> manipulate(long t, TreeMap<Long, Set<FeedObject>> history) {

        if(mem.containsKey(t)){
            return mem.get(t);
        }

        long lastSeen = 0;
        double satisfaction = 0;
        double lastSatisfaction = 0;
        for(Map.Entry<Long, Set<FeedObject>> entry : history.entrySet()){
            if(t < entry.getKey()){
                break;
            }
            for(FeedObject feedObject : entry.getValue()){
                Object[] data = (Object[]) feedObject.getData();
                if(isOfInterest(feedObject)){
                    lastSeen = feedObject.getTimeStamp();
                    Double actual = (Double) data[3];
                    Double previous = (Double) data[4];
                    Double consensus = (Double) data[5];

                    if(consensus != 0 ){
                        lastSatisfaction = (0.75 * (actual - consensus)/consensus);
                    }
                    if(previous != 0 ){
                        lastSatisfaction += (0.25 *(actual - previous)/previous);
                    }
                    satisfaction = satisfaction * 0.5 + lastSatisfaction * 0.5;
                }
            }
        }

        int comingUp = 0;
        try{
            comingUp = (int) (6 * Math.exp(-(double) schedule.getTimeToNext(t, new String[]{type, location}) / (double) (1 * 86400 * 1000L)));
        }catch (Exception e){
            e.printStackTrace();
        }

        int decay = (int) (6 * Math.exp((double) (lastSeen - t) / (double) (1 * 86400 * 1000L))) - comingUp;
        int index = Discretiser.getLogarithmicDiscretisation(
                (lastSatisfaction - satisfaction) / Math.pow(satisfaction, 2) + lastSatisfaction,
                0, 0.01, 10.0);
        FeedObject toStore = new FeedObject<>(t, new Integer[]{decay, index});
        if(mem.size() > 10){
            mem.remove(mem.firstKey());
        }
        mem.put(t, toStore);
        return toStore;
    }

    @Override
    public boolean isOfInterest(FeedObject data) {
        Object[] contents = (Object[]) data.getData();
        String type = (String) contents[0];
        String location = (String) contents[1];
        return (type.contains(this.type) && location.equals(this.location));
    }

    @Override
    public int getNumberOfOutputs() {
        return 2;
    }
}
