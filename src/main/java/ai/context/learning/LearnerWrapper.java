package ai.context.learning;

import ai.context.core.LearnerService;
import ai.context.core.StateActionPair;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchronisedFeed;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LearnerWrapper {

    public SynchronisedFeed feed;
    private int presentFeedFields;
    private int presentCloseField;
    private int signalFields;
    private int horizon;

    private boolean paused = false;
    private boolean killed = false;

    private LearnerService learner = new LearnerService();
    private LinkedList<DataObject> history = new LinkedList<>();

    public LearnerWrapper(Feed signalFeed, Feed presentFeed, int presentCloseField, int horizon, double actionResolution, double tolerance, int maxPop){
        feed = new SynchronisedFeed(presentFeed, feed);
        feed = new SynchronisedFeed(signalFeed, feed);
        this.presentCloseField = presentCloseField;
        this.horizon = horizon;
        presentFeedFields = presentFeed.getNumberOfOutputs();
        signalFields = signalFeed.getNumberOfOutputs();

        learner.setActionResolution(actionResolution);
        learner.setMaxPopulation(maxPop);
        learner.setTolerance(tolerance);
    }

    public void start(){
        Runnable player = new Runnable() {
            @Override
            public void run() {
                while (!killed){
                    while (!paused){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    FeedObject data = feed.getNextComposite(this);
                    double[] values = new double[presentFeedFields];
                    int[] signal = new int[presentFeedFields];

                    for(int i = 0; i < presentFeedFields; i++){
                        values[i] = (double) ((List)data.getData()).get(i);
                    }

                    for(int i = 0; i < signalFields; i++){
                        signal[i] = (int) ((List)data.getData()).get(i + presentFeedFields);
                    }

                    DataObject snapshot = new DataObject(data.getTimeStamp(), signal, values);
                    history.add(snapshot);

                    if(history.size() > horizon){
                        DataObject now = history.pollFirst();

                        double nowClose = now.getValue()[presentCloseField];
                        double maxPosMovement = 0;
                        double maxNegMovement = 0;
                        for(DataObject future : history){
                            for(double val : future.getValue()){
                                double movement = val - nowClose;
                                if(movement > maxPosMovement){
                                    maxPosMovement = movement;
                                }
                                if(movement < maxNegMovement){
                                    maxNegMovement = movement;
                                }
                            }
                        }

                        int[] nowSignal = now.getSignal();
                        learner.addStateAction(nowSignal, maxNegMovement);
                        learner.addStateAction(nowSignal, maxPosMovement);
                    }
                }
            }
        };

        new Thread(player).start();
    }

    public Map<Double, StateActionPair> getAlphas(){
        return learner.getAlphaStates().descendingMap();
    }

    public Map<Double, Integer> getFactorInfluences(StateActionPair state){
        double[] influences = learner.updateAndGetCorrelationWeights(state.getAmalgamate());
        TreeMap<Double, Integer> scores = new TreeMap<>();
        for(int i = 0; i < influences.length; i++){
            scores.put(influences[i], i);
        }

        return scores.descendingMap();
    }

    public void kill() {
        killed = true;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }
}
