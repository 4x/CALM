package ai.context.learning;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.ISynchFeed;
import scala.actors.threadpool.Arrays;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SelectLearnerFeed implements LearnerFeed, Feed {

    ISynchFeed feed;
    private String name;
    private long timeStamp;
    private Integer[] actionElements;
    private Integer[] signalElements;

    public SelectLearnerFeed(ISynchFeed feed, Integer[] actionElements, Integer[] signalElements){
        this.feed = feed;
        feed.addChild(this);

        this.actionElements = actionElements;
        if(signalElements != null){
            this.signalElements = signalElements;
        }
        else{
            HashSet<Integer> actionSet = new HashSet<>();
            actionSet.addAll(Arrays.asList(actionElements));
            int outputs = feed.getNumberOfOutputs();
            signalElements = new Integer[outputs - actionElements.length];
            int sig = 0;
            for(int i = 0; i < feed.getNumberOfOutputs(); i++){
                if(!actionSet.contains(i)){
                    signalElements[sig] = i;
                    sig++;
                }
            }
        }
    }

    public void setActionElements(Integer[] actionElements, Integer[] signalElements){
        this.actionElements = actionElements;
        if(signalElements != null){
            this.signalElements = signalElements;
        }
        else{
            HashSet<Integer> actionSet = new HashSet<>();
            actionSet.addAll(Arrays.asList(actionElements));
            int outputs = feed.getNumberOfOutputs();
            signalElements = new Integer[outputs - actionElements.length];
            int sig = 0;
            for(int i = 0; i < feed.getNumberOfOutputs(); i++){
                if(!actionSet.contains(i)){
                    signalElements[sig] = i;
                    sig++;
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSignalElements(Integer[] signalElements){
        this.signalElements = signalElements;
    }

    @Override
    public boolean hasNext() {
        return feed.hasNext();
    }

    @Override
    public DataObject readNext() {

        FeedObject data = feed.getNextComposite(this);
        //System.out.println("[" +name + "]" + " reads " + data);
        List<Object> content = ((List)data.getData());
        double[] value = new double[actionElements.length];
        for(int i = 0; i < actionElements.length; i++){
            value[i] = (Double) content.get(actionElements[i]);
        }

        int[] signal = new int[signalElements.length];
        if(signal == null){
            return new DataObject(data.getTimeStamp(), null, value);
        }
        for(int i = 0; i < signalElements.length; i++){
            Object sig = content.get(signalElements[i]);
            if(sig == null || !(sig instanceof Integer)){
                signal[i] = 0;
            }
            else{
                signal[i] = (Integer) content.get(signalElements[i]);
            }
        }

        timeStamp = data.getTimeStamp();
        return new DataObject(data.getTimeStamp(), signal, value);
    }

    @Override
    public FeedObject readNext(Object caller) {
        DataObject dataObject = readNext();
        return new FeedObject(dataObject.getTimeStamp(), dataObject);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Feed getCopy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addChild(Feed feed) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeChild(Feed feed) {

    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }

    @Override
    public String getDescription() {
        return getDescription(0, "");
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return "[" + startIndex + "] Select Learner feed from: \n" + feed.getDescription(startIndex, padding + "   ");
    }

    @Override
    public List getElementChain(int element) {
        List<Object> list = new ArrayList<Object>();

        list.add(this);
        list.add(feed.getElementChain(element));
        return list;
    }

    public void cleanup(){
        feed.removeChild(this);
    }

    @Override
    public int getNumberOfOutputs() {
        return feed.getNumberOfOutputs();
    }
}

