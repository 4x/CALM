package ai.context.learning.neural;

import ai.context.core.ai.LearnerService;
import ai.context.core.ai.LearningException;
import ai.context.core.ai.StateActionPair;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.manipulation.WrapperManipulatorPair;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.learning.DataObject;
import ai.context.learning.SelectLearnerFeed;
import ai.context.util.common.StateActionInformationTracker;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.discretisation.AbsoluteMovementDiscretiser;
import ai.context.util.measurement.OpenPosition;
import ai.context.util.trading.BlackBox;
import ai.context.util.trading.DecisionAggregator;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class NeuralLearner implements Feed, Runnable {

    private NeuronCluster cluster = NeuronCluster.getInstance();
    private final int id = cluster.getNewID();
    private NeuronRankings neuronRankings = NeuronRankings.getInstance();
    private long time = 0;
    private ISynchFeed motherFeed;
    private SelectLearnerFeed learnerFeed;
    private Map<NeuralLearner, Integer> parentFeeds = new HashMap<>();

    private LearnerService coreUpA;
    private LearnerService coreDownA;
    private LearnerService coreUpB;
    private LearnerService coreDownB;

    private long countA = 0;
    private long countB = 0;
    private long countForFlip = PropertiesHolder.generationLifespan;

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();
    private LinkedBlockingQueue<FeedObject> queue = new LinkedBlockingQueue<>();
    private List<StateActionInformationTracker> trackers = new LinkedList<>();

    private List<WrapperManipulatorPair> wrapperManipulatorPairs = new ArrayList<>();
    private int numberOfWrapperOutputs = 0;

    private TreeMap<Integer, Double> predictionRaw;

    private AbsoluteMovementDiscretiser discretiser;

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

    private int mainCore = 0;

    //Performance
    private HashSet<OpenPosition> positions = new HashSet<OpenPosition>();
    private double accruedPnL = 0;

    private boolean inLiveTrading = false;
    private BlackBox blackBox;

    private boolean adapting = true;

    private double confidence = 0;

    public NeuralLearner(long[] horizonRange, ISynchFeed motherFeed, Integer[] actionElements, Integer[] sigElements, String parentConfig, String wrapperConfig, long outputFutureOffset, double resolution) {
        this.motherFeed = motherFeed;
        this.horizonRange = horizonRange;
        double horizon = (Math.random() * (horizonRange[1] - horizonRange[0]) + horizonRange[0]);
        this.horizon = (long) Math.ceil(horizon / DecisionAggregator.getTimeQuantum()) * DecisionAggregator.getTimeQuantum();
        this.outputFutureOffset = outputFutureOffset;
        this.actionElements = actionElements;
        this.sigElements = sigElements;
        this.resolution = resolution;
        coreUpA = new LearnerService();
        coreUpA.setActionResolution(resolution);
        coreDownA = new LearnerService();
        coreDownA.setActionResolution(resolution);

        countForFlip = (long) (Math.random() * PropertiesHolder.generationLifespan);

        if(parentConfig == null || parentConfig.length() == 0){
            NeuralLearner[] candidates = cluster.getNeurons();
            Set<Integer> parents = new HashSet<>();
            while(parents.size() < PropertiesHolder.parentsPerNeuron && parents.size() != candidates.length){
                int parentId = (int)(candidates.length * Math.random());
                if(parentId < candidates.length){
                    parents.add(parentId);
                }
            }
            for (int parentId : parents) {
                NeuralLearner parentCandidate = candidates[parentId];
                parentFeeds.put(parentCandidate, (int) (Math.random() * parentCandidate.getNumberOfOutputs()));
                parentCandidate.addChild(this);
            }
        }
        else{
            for(String parentFeed : parentConfig.split(",")){
                NeuralLearner parent = cluster.getNeuronForId(Integer.parseInt(parentFeed.split(":")[0]));
                int feed = Integer.parseInt(parentFeed.split(":")[1]);
                parentFeeds.put(parent, feed);
                parent.addChild(this);
            }
        }

        if(wrapperConfig == null || wrapperConfig.length() == 0){
            for(int i = 0; i < PropertiesHolder.addtionalStimuliPerNeuron; i++){
                wrapperManipulatorPairs.add(cluster.assign());
            }
        }
        else{
            for(String pair : wrapperConfig.split(",")){
                String[] pairInfo = pair.split(":");
                wrapperManipulatorPairs.add(new WrapperManipulatorPair(Integer.parseInt(pairInfo[0]), pairInfo[1]));
            }
        }
        numberOfWrapperOutputs = cluster.getNumberOfOutputsFor(wrapperManipulatorPairs);
        this.learnerFeed = new SelectLearnerFeed(motherFeed, actionElements, sigElements);
        discretiser = new AbsoluteMovementDiscretiser(0.01);
        discretiser.addLayer(0.003, 0.0001);
        discretiser.addLayer(0.005, 0.0005);
        discretiser.addLayer(0.01, 0.001);
        System.out.println("New Neuron: " + getDescription(0, ""));
    }

    private long tStart = System.currentTimeMillis();

    @Override
    public void run() {
        System.out.println(getDescription(0, "") + " started...");
        while (alive) {
            try {
                step();
            } catch (LearningException e) {
                e.printStackTrace();
                break;
            }
        }
        alive = false;
        queue = null;
        buffers = null;
        learnerFeed = null;
        motherFeed = null;
        neuronRankings = null;
        time = Long.MAX_VALUE;
    }


    public void step() throws LearningException {
        try {
            while (paused) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            DataObject data = learnerFeed.readNext();


            if (data.getTimeStamp() > time) {
                time = data.getTimeStamp();
                int day = new Date(time).getDay();

                if (day != 0 && day != 6){
                    String inputDates = new Date(data.getTimeStamp()) + " ";
                    int[] signal = new int[data.getSignal().length + getParents().size() + numberOfWrapperOutputs + 2];
                    int index = 0;
                    for (int sig : data.getSignal()) {
                        signal[index] = sig;
                        index++;
                    }
                    for (NeuralLearner parent : getParents()) {
                        FeedObject feedObject = parent.readNext(this);
                        if(feedObject.getTimeStamp() != data.getTimeStamp()){
                            System.out.println("Motherfeed timestamp: " + data.getTimeStamp() + " Parent " + parent + ": " + feedObject.getTimeStamp());
                        }
                        inputDates += new Date(feedObject.getTimeStamp()) + " ";

                        int sig = ((int[]) (feedObject.getData()))[parentFeeds.get(parent)];
                        signal[index] = sig;
                        index++;
                    }

                    signal[index] = (int) ((time % (86400000))/(3 * 3600000));
                    index++;

                    int pred = 0;
                    if (pointsConsumed > 200) {
                        pred = getSignalForDistribution(getDistribution(signal), 0)[0];
                    }
                    signal[index] = pred;
                    index++;

                    for(WrapperManipulatorPair pair : wrapperManipulatorPairs){
                        FeedObject<Integer[]> sigObject = cluster.getFromFeedWrapper(pair, time + outputFutureOffset);
                        for(int sig : sigObject.getData()){
                            signal[index] = sig;
                            index++;
                        }
                    }

                    tStart = System.currentTimeMillis();
                    double result = 0 ;
                    while (!trackers.isEmpty() && trackers.get(0).getTimeStamp() < (time - horizon)) {
                        StateActionInformationTracker tracker = trackers.remove(0);
                        coreUpA.addStateAction(tracker.getState(), tracker.getMaxUp());
                        coreDownA.addStateAction(tracker.getState(), tracker.getMaxDown());
                        countA++;

                        if(PropertiesHolder.neuronReplacement){
                            if(coreUpB != null){
                                coreUpB.addStateAction(tracker.getState(), tracker.getMaxUp());
                                coreDownB.addStateAction(tracker.getState(), tracker.getMaxDown());
                                countB++;

                                if(countB == PropertiesHolder.generationLifespan){
                                    countA = countB;
                                    countB = 0;
                                    coreUpA = coreUpB;
                                    coreDownA = coreDownB;

                                    coreUpB = new LearnerService();
                                    coreUpB.setActionResolution(resolution);
                                    coreDownB = new LearnerService();
                                    coreDownB.setActionResolution(resolution);
                                    System.out.println("Neuron [" + id + "]: Cores regenerated");
                                }
                            }
                            else if(countA == countForFlip){
                                coreUpB = new LearnerService();
                                coreUpB.setActionResolution(resolution);
                                coreDownB = new LearnerService();
                                coreDownB.setActionResolution(resolution);
                                System.out.println("Neuron [" + id + "]: Initial coreB creation");
                            }
                        }

                        result += tracker.getPerformance();
                        pointsConsumed++;
                    }

                    for (StateActionInformationTracker tracker : trackers) {
                        tracker.processHigh(data.getValue()[1], time);
                        tracker.processLow(data.getValue()[2], time);
                    }

                    int[] outputSignal = new int[getNumberOfOutputs()];

                    if (pointsConsumed > 200) {
                        predictionRaw = getDistribution(signal);
                        outputSignal = getSignalForDistribution(predictionRaw, result);
                    }

                    StateActionInformationTracker tracker = new StateActionInformationTracker(time, signal, data.getValue()[0], 10 * resolution, outputSignal[0]);
                    tracker.setDiscretisation(discretiser);
                    trackers.add(tracker);

                    if (pointsConsumed > PropertiesHolder.neuronLearningPeriod) {
                        adapting = false;
                    }
                    FeedObject output = new FeedObject(time, outputSignal);
                    queue.add(output);
                    latency = (long) ((0.75 * latency) + (0.25 * (System.currentTimeMillis() - tStart)));
                    checkPerformance(predictionRaw, data);
                }
            }
        } catch (LearningException e) {
            System.err.println("Learning exception: " + e.getReason() + " Neuron " + id + " is dying...");
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TreeMap<Integer, Double> getDistribution(int[] signal){
        TreeMap<Integer, Double> dA = getDistribution(signal, false);
        TreeMap<Integer, Double> dB = getDistribution(signal, true);

        if(dB == null){
            return dA;
        }

        double fA = (double)countA/(double)(countA + countB);
        double fB = 1 - fA;

        TreeMap<Integer, Double> dist = new TreeMap<>();

        for(Map.Entry<Integer, Double> entry : dA.entrySet()){
            dist.put(entry.getKey(), entry.getValue() * fA);
        }

        for(Map.Entry<Integer, Double> entry : dB.entrySet()){
            double existing = 0;
            if(dist.containsKey(entry.getKey())){
                existing = dist.get(entry.getKey());
            }
            dist.put(entry.getKey(), entry.getValue() * fB + existing);
        }
        return dist;
    }

    private TreeMap<Integer, Double> getDistribution(int[] signal, boolean useBackUp){

        LearnerService coreUp = coreUpA;
        LearnerService coreDown = coreDownA;

        if(useBackUp){

            if(countB < 200){
                return null;
            }
            coreUp = coreUpB;
            coreDown = coreDownB;
        }

        TreeMap<Integer, Double> distribution = new TreeMap<>();
        TreeMap<Integer, Double> distUp = coreUp.getActionDistribution(signal);
        TreeMap<Integer, Double> distDown = coreDown.getActionDistribution(signal);

        double weightUp = 0;
        double weightDown = 0;
        for(double w : distUp.values()){
            weightUp += w;
        }
        for(double w : distDown.values()){
            weightDown += w;
        }

        double netWeight = weightDown + weightUp;
        double upMult = netWeight/weightUp;
        double downMult = netWeight/weightDown;

        for (Map.Entry<Integer, Double> entry : distUp.entrySet()) {
            int key = entry.getKey();
            double val = entry.getValue() * upMult;
            if(key == 0){
                key = 1;
            }
            else if(key == 1 && distribution.containsKey(key)){
                val += distribution.get(key);
            }

            distribution.put(key, val);
        }

        for (Map.Entry<Integer, Double> entry : distDown.entrySet()) {
            int key = entry.getKey();
            double val = entry.getValue() * downMult;
            if(key == 0){
                key = -1;
            }
            else if(key == -1 && distribution.containsKey(key)){
                val += distribution.get(key);
            }

            distribution.put(key, val);
        }

        return distribution;
    }

    public void checkPerformance(TreeMap<Integer, Double> predictionRaw, DataObject data) {
        if (!adapting) {
            Date executionInstant = new Date(time);
            if (!(executionInstant.getDay() == 0 || executionInstant.getDay() == 6)) {

                TreeMap<Double, Double> prediction = new TreeMap<Double, Double>();
                for (Map.Entry<Integer, Double> entry : predictionRaw.entrySet()) {
                    prediction.put(data.getValue()[0] + entry.getKey() * resolution, entry.getValue());
                }
                DecisionAggregator.aggregateDecision(data, data.getValue()[0], prediction, horizon, false);
            }

        }
    }

    private void connectOutputToMother() {
        if (!outputConnected) {
            outputConnected = true;
            int index = motherFeed.getNumberOfOutputs();
            outputElements = new Integer[getNumberOfOutputs()];
            for (int i = 0; i < outputElements.length; i++) {
                outputElements[i] = index + i;
            }
            motherFeed.addRawFeed(this);
        }
    }

    public void lifeEvent() {
        if (paused) {
            return;
        }
        if (cluster.size() < 40 || cluster.getDangerLevel() < 2 && cluster.getDangerLevel() * Math.random() < 0.1) {
            spawn();
        }
    }

    public void updateRankings() {
        if (paused) {
            return;
        }
        double score = 0;
        double n = 0;
        Map<Double, StateActionPair> alphas = coreUpA.getAlphaStates();
        for (Map.Entry<Double, StateActionPair> entry : alphas.entrySet()) {
            score += Math.pow(entry.getKey(), 2) * Math.log(entry.getValue().getTotalWeight());
        }
        n += alphas.size();

        alphas = coreDownA.getAlphaStates();
        for (Map.Entry<Double, StateActionPair> entry : alphas.entrySet()) {
            score += Math.pow(entry.getKey(), 2) * Math.log(entry.getValue().getTotalWeight());
        }
        n += alphas.size();
        score /= n;
        neuronRankings.update(this, score);
    }

    public void spawn() {
    }

    public int getID() {
        return id;
    }

    public Collection<NeuralLearner> getParents() {
        return parentFeeds.keySet();
    }

    public void togglePause() {
        paused = !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public Integer[][] getFlowData() {
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

    double lambda = 0.05;
    double lastMean = 0;
    int lastDecision = 0;
    int lastConfidence = 0;

    public int[] getSignalForDistribution(TreeMap<Integer, Double> distribution, double confidence) {
        int[] signal = new int[getNumberOfOutputs()];

        double sum = 0;
        double weight = 0;

        for (Map.Entry<Integer, Double> entry : distribution.entrySet()) {
            sum += entry.getKey() * entry.getValue();
            weight += entry.getValue();
        }

        double mean = 0;
        double stddev = 0;
        if (weight > 0) {
            mean = sum / weight;
        }

        lastMean = (1 - lambda) * lastMean + lambda * mean;
        this.confidence = (1 - lambda) * this.confidence + lambda * confidence;
        lastDecision = signal[0] = getLogarithmicDiscretisation(mean, 0, 1, 2);
        lastConfidence = getLogarithmicDiscretisation(this.confidence, 0, resolution*10, 2);
        return signal;
    }

    @Override
    public boolean hasNext() {
        if (alive) {
            return !queue.isEmpty();
        }
        return true;
    }

    @Override
    public FeedObject readNext(Object caller) {
        if (!alive) {
            return new FeedObject(Long.MAX_VALUE, null);
        }

        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }

        FeedObject feedObject = null;
        try {
            if(queue.isEmpty()){
                System.out.println("No point for child: " + ((NeuralLearner)caller).id + " from " + this.id);
            }
            feedObject = queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Feed> toRemove = new ArrayList<>();
        for (Feed listener : buffers.keySet()) {
            if (listener != caller) {
                List<FeedObject> list = buffers.get(listener);
                list.add(feedObject);
                if(list.size() > 10){
                    toRemove.add(listener);
                    System.out.println("Removing child: " + ((NeuralLearner)listener).id + " from " + this.id);
                }
            }
        }
        for(Feed remove : toRemove){
            buffers.remove(remove);
        }
        return feedObject;
    }

    public boolean hasNext(Object caller) {
        if (!buffers.containsKey(caller)) {
            return false;
        }

        if (buffers.get(caller).size() > 0) {
            return true;
        }

        return hasNext();
    }

    @Override
    public Feed getCopy() {
        return null;
    }

    public void cleanup() {
        alive = false;
        queue = null;
        buffers = null;
        learnerFeed = null;
        motherFeed = null;
        neuronRankings = null;
        time = Long.MAX_VALUE;
        learnerFeed.cleanup();
    }

    @Override
    public long getLatestTime() {
        return time;
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
        int longest = 0;
        for (LinkedList<FeedObject> list : buffers.values()) {
            if (list.size() > longest) {
                longest = list.size();
                buffers.put(feed, list);
            }
        }
    }

    @Override
    public void removeChild(Feed feed) {
        buffers.remove(feed);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        String parentDesc = "";
        for(Map.Entry<NeuralLearner, Integer> parent : parentFeeds.entrySet()){
            parentDesc += parent.getKey().getID() + ":" + parent.getValue() + ",";
        }
        if(parentDesc.length() > 0){
            parentDesc = parentDesc.substring(0, parentDesc.length() - 1);
        }
        return padding + "[" + id + "] Neural Learner with Actions: " + Arrays.asList(actionElements) + " and Signals: " + Arrays.asList(sigElements) + " and Parents: [" + parentDesc + "] and WrapperManipulators: " + wrapperManipulatorPairs;
    }

    @Override
    public List getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }

    public TreeMap<Integer, Double> getPredictionRaw() {
        return predictionRaw;
    }
}
