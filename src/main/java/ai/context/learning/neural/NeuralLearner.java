package ai.context.learning.neural;

import ai.context.core.ai.LearnerService;
import ai.context.core.ai.StateActionPair;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.learning.DataObject;
import ai.context.learning.SelectLearnerFeed;
import ai.context.util.common.MapUtils;
import ai.context.util.common.StateActionInformationTracker;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class NeuralLearner implements Feed, Runnable{

    private NeuronCluster cluster = NeuronCluster.getInstance();
    private NeuronRankings neuronRankings = NeuronRankings.getInstance();
    private StimuliRankings stimuliRankings = StimuliRankings.getInstance();
    private long time = 0;
    private SynchronisedFeed motherFeed;
    private SelectLearnerFeed learnerFeed;
    private LearnerService core = new LearnerService();

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();
    private LinkedBlockingQueue<FeedObject> queue = new LinkedBlockingQueue<>();
    private List<StateActionInformationTracker> trackers = new LinkedList<>();

    private final long horizon;
    private long outputFutureOffset = 5 * 60 * 1000L;
    private boolean alive;
    private boolean outputConnected = false;

    private Integer[] actionElements;
    private Integer[] sigElements;
    private long[] horizonRange;
    private double resolution;

    private long latency = 0;

    public NeuralLearner(long[] horizonRange, SynchronisedFeed motherFeed, Integer[] actionElements, Integer[] sigElements, long outputFutureOffset, double resolution){
        this.motherFeed = motherFeed;
        this.learnerFeed = new SelectLearnerFeed(motherFeed, actionElements, sigElements);
        this.horizonRange = horizonRange;
        horizon = (long) (Math.random() * (horizonRange[1] - horizonRange[0]) + horizonRange[0]);
        this.outputFutureOffset = outputFutureOffset;
        this.actionElements = actionElements;
        this.sigElements = sigElements;
        this.resolution = resolution;
        core.setActionResolution(resolution);
    }

    @Override
    public void run() {
        long pointsConsumed = 0;
        while (alive){
            DataObject data = learnerFeed.readNext();
            if(data.getTimeStamp() > time){
                long tStart = System.nanoTime();
                time = data.getTimeStamp();
                while(trackers.get(0).getTimeStamp() < (time - horizon)){
                    StateActionInformationTracker tracker = trackers.remove(0);
                    core.addStateAction(tracker.getState(), tracker.getMax());
                    core.addStateAction(tracker.getState(), tracker.getMin());
                }
                for(StateActionInformationTracker tracker : trackers){
                    for(double newLevel : data.getValue()){
                        tracker.aggregate(newLevel);
                    }
                }
                trackers.add(new StateActionInformationTracker(time, data.getSignal(), data.getValue()[0]));

                int[] outputSignal = new int[getNumberOfOutputs()];
                if(pointsConsumed < 100){
                    outputSignal = getSignalForDistribution(core.getActionDistribution(data.getSignal()));
                }
                long eventTime = time + outputFutureOffset;
                FeedObject output = new FeedObject(eventTime, outputSignal);
                queue.add(output);
                pointsConsumed++;

                if(Math.random() > 0.9999){
                    lifeEvent();
                }
                latency = System.nanoTime() - tStart;
            }
        }
        queue = null;
        buffers = null;
        learnerFeed = null;
        motherFeed = null;
        core = null;
        neuronRankings = null;
        stimuliRankings = null;
        time = Long.MAX_VALUE;
    }

    public void lifeEvent(){
        if(cluster.getDangerLevel() * Math.random() < 0.25){
            spawn();
        }
        else if(cluster.getDangerLevel() * Math.random() < 5){
            selectStimuli();
        }
        else {
            double rank = 0;
            for(NeuralLearner neuron : neuronRankings.getRankings().values()){
                rank++;
                if(this == neuron){
                    break;
                }
            }

            if(rank/neuronRankings.getRankings().size() > 0.9){
                die();
            }
        }
    }

    public void updateRankings(){
        double score = 0;
        Map<Double, StateActionPair> alphas = core.getAlphaStates();
        double[] stimuliScores = new double[sigElements.length];
        for(Map.Entry<Double, StateActionPair> entry: alphas.entrySet()){
            score += entry.getKey();
            int i = 0;
            for(double weight : core.getCorrelationWeightsForState(entry.getValue())){
                stimuliScores[i] += entry.getKey() * weight;
                i++;
            }
        }
        score /= alphas.size();
        neuronRankings.update(this, score);

        HashMap<Integer, Double> data = new HashMap<>();
        for(int i = 0; i < sigElements.length; i++){
            data.put(sigElements[i], stimuliScores[i]/alphas.size());
        }
        stimuliRankings.update(this, data);
    }

    public void selectStimuli(){
        Map<Integer, Double> rankings = MapUtils.reverse(stimuliRankings.getRankings());
        double worseRanking = Double.MAX_VALUE;
        int worstSigPos = 0;
        for(int i = 0; i < sigElements.length; i++){
            int sig = sigElements[i];
            if(rankings.get(sig) < worseRanking){
                worseRanking = rankings.get(sig);
                worstSigPos = i;
            }
        }
        rankings.keySet().removeAll(Arrays.asList(sigElements));
        double level = 0;
        int chosen = sigElements[worstSigPos];
        for(Map.Entry<Integer, Double> entry : rankings.entrySet()){
            if(entry.getValue() > level * Math.random()){
                level = entry.getValue() * 2;
                chosen = entry.getKey();
            }
        }
        sigElements[worstSigPos] = chosen;
        learnerFeed.setSignalElements(sigElements);
    }

    public void spawn(){

        Integer[] sigElements = this.sigElements;
        Set<Integer> used = new HashSet<>();
        used.addAll(Arrays.asList(actionElements));
        used.addAll(Arrays.asList(sigElements));
        int choice = motherFeed.getNumberOfOutputs();
        for(int i = 0 ; i < sigElements.length; i++){
            if(Math.random() > 0.5){
                for(int tries = 0; tries < 10; tries++){
                    int candidate = (int) (Math.random() * choice);
                    if(!used.contains(candidate)){
                        sigElements[i] = candidate;
                        used.add(candidate);
                        break;
                    }
                }
            }
        }

        NeuralLearner child = new NeuralLearner(horizonRange, motherFeed, actionElements, sigElements, outputFutureOffset, resolution);
        cluster.start(child);
    }

    public void die(){
        alive = false;
    }

    public boolean isAlive() {
        return alive;
    }

    public long getLatency() {
        return latency;
    }

    double lambda = 0.01;
    double lastMean = 0;
    public int[] getSignalForDistribution(TreeMap<Integer, Double> distribution){
        int[] signal = new int[getNumberOfOutputs()];

        double sum = 0;
        double sumSq = 0;
        double weight = 0;
        for(Map.Entry<Integer, Double> entry : distribution.entrySet()){
            sum += entry.getKey() * entry.getValue();
            sumSq += Math.pow(entry.getKey() * entry.getValue(), 2);
            weight += entry.getValue();
        }
        double mean = sum/weight;
        double stddev = Math.sqrt(sumSq/weight - Math.pow(sum/weight,2));

        double accruedWeight = 0;
        Double firstQuartile = null;
        Double median = null;
        Double thirdQuartile = null;
        for(Map.Entry<Integer, Double> entry : distribution.entrySet()){
            accruedWeight += entry.getValue();
            if(accruedWeight >= weight/4 && firstQuartile == null){
                firstQuartile = entry.getValue();
            }
            else if(accruedWeight >= weight/2 && median == null){
                median = entry.getValue();
            }
            else if(accruedWeight >= 3*weight/4 && thirdQuartile == null){
                thirdQuartile = entry.getValue();
            }
        }

        lastMean = (1 - lambda) * lastMean + lambda * mean;

        signal[0] = getLogarithmicDiscretisation(mean, 0, 1, 2);
        signal[1] = getLogarithmicDiscretisation(stddev, 0, 1, 2);
        signal[2] = getLogarithmicDiscretisation(firstQuartile, 0, 1, 2);
        signal[3] = getLogarithmicDiscretisation(median, 0, 1, 2);
        signal[4] = getLogarithmicDiscretisation(thirdQuartile, 0, 1, 2);
        signal[5] = getLogarithmicDiscretisation(mean - lastMean, 0, 1, 2);
        return signal;
    }

    public TreeMap<Integer, Double> ask(Object[] fullSignal){
        int[] state = new int[sigElements.length];
        for(int i = 0; i < sigElements.length; i++){
            state[i] = (int) fullSignal[sigElements[i]];
        }
        return core.getActionDistribution(state);
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public FeedObject readNext(Object caller) {
        if(!alive){
            return new FeedObject(Long.MAX_VALUE, null);
        }

        if(!outputConnected){
            new SynchronisedFeed(this, motherFeed);
        }

        if(buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }

        FeedObject feedObject = null;
        try {
            feedObject = queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(Feed listener : buffers.keySet()){
            if(listener != caller){
                buffers.get(listener).add(feedObject);
            }
        }
        return feedObject;
    }

    @Override
    public Feed getCopy() {
        return null;
    }

    @Override
    public long getLatestTime() {
        return time;
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Neural Learner for "+  learnerFeed.getDescription();
    }

    @Override
    public List getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 6;
    }
}
