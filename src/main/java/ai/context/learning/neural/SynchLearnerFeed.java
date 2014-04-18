package ai.context.learning.neural;

import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchFeed;
import ai.context.learning.DataObject;
import ai.context.learning.LearnerFeed;
import ai.context.learning.SelectLearnerFeed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SynchLearnerFeed implements LearnerFeed {

    private SynchFeed synchFeed = new SynchFeed();
    SelectLearnerFeed dataFeed;
    private List<Integer> fromParentFeeds = new ArrayList<>();

    public SynchLearnerFeed(SelectLearnerFeed feed, Map<NeuralLearner, Integer> parentFeeds) {
        this.dataFeed = feed;
        synchFeed.addRawFeed(feed);
        int feedNumber = 1;
        for(NeuralLearner parent : parentFeeds.keySet()){
            synchFeed.addRawFeed(parent);
            fromParentFeeds.add(feedNumber + parentFeeds.get(parent));
            feedNumber  += parent.getNumberOfOutputs();
        }
    }


    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public DataObject readNext() {
        FeedObject feedObject = synchFeed.getNextComposite(this);
        long timeStamp = feedObject.getTimeStamp();
        List<Object> data = (List<Object>) feedObject.getData();
        DataObject dataObject = (DataObject) data.get(0);
        int[] signal = new int[dataObject.getSignal().length + fromParentFeeds.size()];
        int sig = 0;
        for(int i = 0; i < dataObject.getSignal().length; i++){
            signal[sig] = dataObject.getSignal()[i];
            sig++;
        }

        for(int extract : fromParentFeeds){
            signal[sig] = (int) data.get(extract);
            sig++;
        }

        return new DataObject(timeStamp, signal, dataObject.getValue());
    }

    @Override
    public String getDescription() {
        return null;
    }

    public void cleanup() {
        dataFeed.cleanup();
        synchFeed.cleanup();
    }
}
