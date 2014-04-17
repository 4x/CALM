package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.learning.neural.NeuralLearner;

import java.util.List;

public class RawFeedWrapper implements Feed{

    private final Feed rawFeed;
    private FeedObject comingData;
    private FeedObject latestData;

    public RawFeedWrapper(Feed rawFeed) {
        this.rawFeed = rawFeed;
        rawFeed.addChild(this);
        this.latestData = rawFeed.readNext(this);
    }

    public synchronized FeedObject getLatestDataAtTime(long time){
        FeedObject toReturn = new FeedObject(time, null);
        if(latestData.getTimeStamp() <= time){
            if(comingData == null && (!(rawFeed instanceof NeuralLearner) || rawFeed.hasNext())){
                comingData = rawFeed.readNext(this);
            }

            if(comingData != null){
                while ((!(rawFeed instanceof NeuralLearner) || rawFeed.hasNext()) && comingData.getTimeStamp() <= time){
                    latestData = comingData;
                    comingData = rawFeed.readNext(this);
                }
            }
            toReturn = new FeedObject(time, latestData.getData());
        }
        return toReturn;
    }

    public long getHeadTimeStamp(){
        return latestData.getTimeStamp();
    }

    public long getNextTimeStamp(){
        if(comingData == null){
            return latestData.getTimeStamp();
        }
        return comingData.getTimeStamp();
    }

    public Feed getRawFeed(){
        return rawFeed;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public FeedObject readNext(Object caller) {
        return null;
    }

    @Override
    public Feed getCopy() {
        return null;
    }

    @Override
    public long getLatestTime() {
        return 0;
    }

    @Override
    public void addChild(Feed feed) {
    }

    @Override
    public void removeChild(Feed feed) {
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return null;
    }

    @Override
    public List getElementChain(int element) {
        return null;
    }

    @Override
    public int getNumberOfOutputs() {
        return 0;
    }
}
