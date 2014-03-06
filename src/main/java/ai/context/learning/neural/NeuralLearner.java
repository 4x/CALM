package ai.context.learning.neural;

import ai.context.core.ai.LearnerService;
import ai.context.core.ai.StateActionPair;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.feed.synchronised.SynchFeed;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.learning.DataObject;
import ai.context.learning.SelectLearnerFeed;
import ai.context.util.common.MapUtils;
import ai.context.util.common.StateActionInformationTracker;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class NeuralLearner implements Feed, Runnable{

    private NeuronCluster cluster = NeuronCluster.getInstance();
    private final int id = cluster.getNewID();
    private NeuronRankings neuronRankings = NeuronRankings.getInstance();
    private StimuliRankings stimuliRankings = StimuliRankings.getInstance();
    private long time = 0;
    private SynchFeed motherFeed;
    private SelectLearnerFeed learnerFeed;
    private LearnerService core = new LearnerService();

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();
    private LinkedBlockingQueue<FeedObject> queue = new LinkedBlockingQueue<>();
    private List<StateActionInformationTracker> trackers = new LinkedList<>();

    private final long horizon;
    private long oneHour = 60 * 60 * 1000L;
    private long outputFutureOffset = 5 * 60 * 1000L;
    private boolean alive = true;
    private boolean outputConnected = false;
    private boolean paused = false;

    private Integer[] actionElements;
    private Integer[] sigElements;
    private long[] horizonRange;
    private double resolution;

    private long latency = 0;
    private long pointsConsumed = 0;

    private Integer[] outputElements;

    public NeuralLearner(long[] horizonRange, SynchFeed motherFeed, Integer[] actionElements, Integer[] sigElements, long outputFutureOffset, double resolution){
        this.motherFeed = motherFeed;
        this.learnerFeed = new SelectLearnerFeed(motherFeed, actionElements, sigElements);
        learnerFeed.setName(id + "");
        this.horizonRange = horizonRange;
        horizon = (long) (Math.random() * (horizonRange[1] - horizonRange[0]) + horizonRange[0]);
        this.outputFutureOffset = outputFutureOffset;
        this.actionElements = actionElements;
        this.sigElements = sigElements;
        this.resolution = resolution;
        core.setActionResolution(resolution);

        System.out.println("New Neuron: " + getDescription(0, ""));
    }

    private long tStart = System.currentTimeMillis();
    @Override
    public void run() {
        System.out.println(getDescription(0, "") + " started...");
        while (alive){
            while (paused || (cluster.getMeanTime() > 0 && (time - cluster.getMeanTime()) > oneHour)){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            DataObject data = learnerFeed.readNext();
            if(data.getTimeStamp() > time){
                tStart = System.currentTimeMillis();
                time = data.getTimeStamp();
                while(!trackers.isEmpty() && trackers.get(0).getTimeStamp() < (time - horizon)){
                    StateActionInformationTracker tracker = trackers.remove(0);
                    core.addStateAction(tracker.getState(), tracker.getMax());
                    core.addStateAction(tracker.getState(), tracker.getMin());

                    //System.out.println("Learned: " + Arrays.toString(tracker.getState()) + " -> {" + tracker.getMin() + ", " + tracker.getMax() + "}");
                    pointsConsumed++;
                }
                for(StateActionInformationTracker tracker : trackers){
                    for(double newLevel : data.getValue()){
                        tracker.aggregate(newLevel);
                    }
                }
                trackers.add(new StateActionInformationTracker(time, data.getSignal(), data.getValue()[0]));

                int[] outputSignal = new int[getNumberOfOutputs()];
                if(pointsConsumed > 100){
                    outputSignal = getSignalForDistribution(core.getActionDistribution(data.getSignal()));
                }
                long eventTime = time + outputFutureOffset;
                FeedObject output = new FeedObject(eventTime, outputSignal);
                //System.out.println("["+id+"] Output produced: " + output);
                queue.add(output);

                if(!outputConnected){
                    outputConnected = true;
                    int index = motherFeed.getNumberOfOutputs();
                    outputElements = new Integer[getNumberOfOutputs()];
                    for(int i = 0; i < outputElements.length; i++){
                        outputElements[i] = index + i;
                    }
                    stimuliRankings.newStimuli(outputElements);
                    motherFeed.addRawFeed(this);
                }
                latency = (long) ((0.75 * latency) + (0.25 * (System.currentTimeMillis() - tStart)));
            }
        }
        alive = false;
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
        if(paused){
            return;
        }
        if(cluster.size() < 20 || cluster.getDangerLevel() < 2 && cluster.getDangerLevel() * Math.random() < 0.1){
            spawn();
        }
        else if(pointsConsumed > 5000){
            if(cluster.getDangerLevel() * Math.random() < 1.5){
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

                if(cluster.size() > 10 && rank/neuronRankings.getRankings().size() < 0.25){
                    die();
                }
            }
        }
    }

    public void updateRankings(){
        if(paused){
            return;
        }
        double score = 0;
        Map<Double, StateActionPair> alphas = core.getAlphaStates();
        double[] stimuliScores = new double[sigElements.length];
        for(Map.Entry<Double, StateActionPair> entry: alphas.entrySet()){
            score += Math.pow(entry.getKey(), 2);
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

    long lastSelect = 0;
    public void selectStimuli(){
        if(paused){
            return;
        }
        if(time - lastSelect > (Math.random() * 10 * 86400000L)){
            Map<Integer, Double> rankings = MapUtils.reverse(stimuliRankings.getRankings());
            double worseRanking = Double.MAX_VALUE;
            int worstSigPos = 0;
            for(int i = 0; i < sigElements.length; i++){
                int sig = sigElements[i];
                if(rankings.containsKey(sig) && rankings.get(sig) < worseRanking){
                    worseRanking = rankings.get(sig);
                    worstSigPos = i;
                }
            }
            rankings.keySet().removeAll(Arrays.asList(sigElements));
            rankings.keySet().retainAll(stimuliRankings.getStimuli());
            double level = 0;
            int chosen = sigElements[worstSigPos];
            for(Map.Entry<Integer, Double> entry : rankings.entrySet()){
                if(entry.getValue() > 4 * worseRanking && entry.getValue() > level * Math.random()){
                    level = entry.getValue() * 2;
                    chosen = entry.getKey();
                }
            }
            if(chosen != sigElements[worstSigPos]){
                System.out.println(getDescription(0, "") + " replaced signal at position " + worstSigPos + " (" + sigElements[worstSigPos] + " -> " + chosen + ")");
                sigElements[worstSigPos] = chosen;
                lastSelect = time;
            }
            learnerFeed.setSignalElements(sigElements);
        }
    }

    public void spawn(){
        if(paused){
            return;
        }
        Integer[] sigElements = new Integer[this.sigElements.length];
        Set<Integer> used = new HashSet<>();
        used.addAll(Arrays.asList(actionElements));
        used.addAll(Arrays.asList(sigElements));
        List<Integer> available = new ArrayList<>(stimuliRankings.getStimuli());
        int choice = available.size();
        for(int i = 0 ; i < sigElements.length; i++){
            sigElements[i] = this.sigElements[i];
            if(Math.random() > 0.75){
                for(int tries = 0; tries < 10; tries++){
                    int candidate = available.get(Math.min(choice - 1,(int) (Math.random() * choice)));
                    if(Math.random() > 0.2){
                        candidate = available.get(Math.max(0, Math.min(choice - 1, choice - (int) (Math.random() * 30))));
                    }
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
        if(paused && time - lastSelect > (Math.random() * 20 * 86400000L)){
            return;
        }
        System.out.println(getDescription(0, "") + " dies...");
        alive = false;

        stimuliRankings.removeAllStimuli(this, this.getFlowData()[2]);
    }

    public int getID(){
        return id;
    }

    public void togglePause(){
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public Integer[][] getFlowData(){
        return new Integer[][]{actionElements, sigElements, outputElements};
    }

    public boolean isAlive() {
        return alive;
    }

    public long getLatency() {
        return latency;
    }

    public long getPointsConsumed() {
        return pointsConsumed;
    }

    double lambda = 0.01;
    double lastMean = 0;
    public int[] getSignalForDistribution(TreeMap<Integer, Double> distribution){
        int[] signal = new int[getNumberOfOutputs()];

        double sum = 0;
        double sumSq = 0;
        double weight = 0;
        double accruedWeight = 0;

        Integer firstQuartile = null;
        Integer median = null;
        Integer thirdQuartile = null;
        for(Map.Entry<Integer, Double> entry : distribution.entrySet()){
            accruedWeight += entry.getValue();
            if(accruedWeight >= weight/4 && firstQuartile == null){
                firstQuartile = entry.getKey();
            }
            else if(accruedWeight >= weight/2 && median == null){
                median = entry.getKey();
            }
            else if(accruedWeight >= 3*weight/4 && thirdQuartile == null){
                thirdQuartile = entry.getKey();
            }
            sum += entry.getKey() * entry.getValue();
            sumSq += Math.pow(entry.getKey() * entry.getValue(), 2);
            weight += entry.getValue();
        }

        double mean = 0;
        double stddev = 0;
        if(weight > 0){
            mean = sum/weight;
            stddev = Math.sqrt(sumSq/weight - Math.pow(mean,2));
        }

        if(firstQuartile == null){
            firstQuartile = 0;
        }

        if(median == null){
            median = 0;
        }

        if(thirdQuartile == null){
            thirdQuartile = 0;
        }

        lastMean = (1 - lambda) * lastMean + lambda * mean;

        signal[0] = getLogarithmicDiscretisation(mean, 0, 1, 2);
        signal[1] = getLogarithmicDiscretisation(stddev, 0, 1, 2);
        signal[2] = getLogarithmicDiscretisation(firstQuartile, 0, 1, 2);
        signal[3] = getLogarithmicDiscretisation(median, 0, 1, 2);
        signal[4] = getLogarithmicDiscretisation(thirdQuartile, 0, 1, 2);
        signal[5] = getLogarithmicDiscretisation(mean - lastMean, 0, 1, 2);

        /*System.out.println(mean + ", " +
                stddev + ", " +
                firstQuartile + ", " +
                median + ", " +
                thirdQuartile + ", " +
                (mean - lastMean));*/
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
        if(alive){
            return !queue.isEmpty();
        }
        return true;
    }

    @Override
    public FeedObject readNext(Object caller) {
        if(!alive){
            return new FeedObject(Long.MAX_VALUE, null);
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
        //System.out.println("["+id+"] RETURNING: " + feedObject);
        return feedObject;
    }

    public void inputsRemoved(Integer[] inputs){
        for(int i = 0; i < actionElements.length; i++){
            if(actionElements[i] > inputs[inputs.length - 1]){
                actionElements[i] = actionElements[i] - inputs.length;
            }
        }
        for(int i = 0; i < sigElements.length; i++){
            if(sigElements[i] > inputs[inputs.length - 1]){
                sigElements[i] = sigElements[i] - inputs.length;
            }
        }
        if(learnerFeed != null){
            learnerFeed.setActionElements(actionElements, sigElements);
        }

        if(outputElements[0] > inputs[inputs.length - 1]){
            for(int i = 0; i < outputElements.length; i++){
                outputElements[i] = outputElements[i] - inputs.length;
            }
        }
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
        return padding + "[" + id + "] Neural Learner with Actions: " +  Arrays.asList(actionElements) + " and Signals: " + Arrays.asList(sigElements);
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
