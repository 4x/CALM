package ai.context.feed.manipulation;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.*;

public class FeedWrapper {
    private Feed feed;
    private long time = 0;
    private TreeMap<Long, Set<FeedObject>> history = new TreeMap<>();
    private HashMap<String, Manipulator> manipulators = new HashMap<>();

    public FeedWrapper(Feed feed) {
        this.feed = feed;
    }

    public List<String> getPossibleManipulators(){
        return new ArrayList<>(manipulators.keySet());
    }

    public FeedObject<Integer[]> getAtTimeForManipulator(long time, String manipulator){
        while(this.time <= time){
            FeedObject data = feed.readNext(this);
            this.time = data.getTimeStamp();

            boolean isOfInterest = false;

            for(Manipulator m : manipulators.values()){
                if(m.isOfInterest(data)){
                    isOfInterest = true;
                    break;
                }
            }
            if(isOfInterest){
                if(!history.containsKey(this.time)){
                    history.put(this.time, new HashSet<FeedObject>());
                }

                history.get(this.time).add(data);
                if(history.size() > manipulators.size()*4){
                    history.remove(history.firstKey());
                }
            }
        }
        if(!manipulators.containsKey(manipulator)){
            return new FeedObject(time, new Integer[]{0});
        }
        return manipulators.get(manipulator).manipulate(time, history);
    }

    public void putManipulator(String manipulatorId, Manipulator manipulator){
        this.manipulators.put(manipulatorId, manipulator);
    }

    public Manipulator getManipulator(String manipulatorId) {
        return manipulators.get(manipulatorId);
    }
}
