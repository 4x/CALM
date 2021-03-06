package ai.context.learning.neural;

import ai.context.core.ai.*;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.manipulation.WrapperManipulatorPair;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.learning.DataObject;
import ai.context.learning.SelectLearnerFeed;
import ai.context.util.common.LabelledTuple;
import ai.context.util.common.ScratchPad;
import ai.context.util.common.StateActionInformationTracker;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.discretisation.AbsoluteMovementDiscretiser;
import ai.context.util.trading.version_1.OpenPosition;
import ai.context.util.trading.version_1.BlackBox;
import ai.context.util.trading.version_1.DecisionAggregatorA;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class NeuralLearner implements Feed, Runnable {

    private final String RECOVERY_STRING = "RECOVERY";
    private NeuronCluster cluster = NeuronCluster.getInstance();
    private final int id = cluster.getNewID();
    private NeuronRankings neuronRankings = NeuronRankings.getInstance();
    private long time = 0;
    private ISynchFeed motherFeed;
    private SelectLearnerFeed learnerFeed;
    private Map<NeuralLearner, Integer> parentFeeds = new HashMap<>();

    boolean oneCore = false;
    private LearnerService core;

    private LearnerService coreUpA;
    private LearnerService coreDownA;
    private LearnerService coreUpB;
    private LearnerService coreDownB;

    private long generationalLifespan = (long) (Math.random() * (PropertiesHolder.generationLifespan - PropertiesHolder.minGenerationLifespan) + PropertiesHolder.minGenerationLifespan);
    private long countA = 0;
    private long countB = 0;

    private long weightA = 0;
    private long weightB = 0;
    private long countForFlip = generationalLifespan;
    private int generation = 0;

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();
    private LinkedBlockingQueue<FeedObject> queue = new LinkedBlockingQueue<>();
    private List<StateActionInformationTracker> trackers = new LinkedList<>();

    private List<WrapperManipulatorPair> wrapperManipulatorPairs = new ArrayList<>();
    private int numberOfWrapperOutputs = 0;

    private TreeMap<Integer, Double> predictionRaw;

    private AbsoluteMovementDiscretiser discretiser;

    private long horizon;
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


    private double lambda = 0.05;
    private double lastMean = 0;
    private int lastDecision = 0;
    private int lastConfidence = 0;

    private double ampUX = 0;
    private double ampDX = 0;
    private double ampUA = 0;
    private double ampDA = 0;
    private double ampUB = 0;
    private double ampDB = 0;

    private AdditionalStateActionInformation recInfoA;
    private AdditionalStateActionInformation recInfoB;
    private AdditionalStateActionInformation recInfoX;
    private double pCutOff = 0.5;

    public NeuralLearner(long horizon, ISynchFeed motherFeed, Integer[] actionElements, Integer[] sigElements, String parentConfig, String wrapperConfig, long outputFutureOffset, double resolution) {
        this.horizon = horizon;
        this.motherFeed = motherFeed;
        this.horizonRange = horizonRange;
        this.outputFutureOffset = outputFutureOffset;
        this.actionElements = actionElements;
        this.sigElements = sigElements;
        this.resolution = resolution;

        if(oneCore){
            core = new LearnerService();
            core.setActionResolution(resolution);
        }
        else {
            coreUpA = new LearnerService();
            coreUpA.setActionResolution(resolution);
            coreDownA = new LearnerService();
            coreDownA.setActionResolution(resolution);
        }

        countForFlip = (long) (Math.random() * generationalLifespan);

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
        discretiser = new AbsoluteMovementDiscretiser(0.005);
        discretiser.addLayer(0.002, 0.0001);
        discretiser.addLayer(0.005, 0.0005);
        System.out.println("New Neuron: " + getDescription(0, ""));
    }

    public NeuralLearner(long[] horizonRange, ISynchFeed motherFeed, Integer[] actionElements, Integer[] sigElements, String parentConfig, String wrapperConfig, long outputFutureOffset, double resolution) {
        this(0, motherFeed, actionElements, sigElements, parentConfig, wrapperConfig, outputFutureOffset, resolution);
        double horizon = (Math.random() * (horizonRange[1] - horizonRange[0]) + horizonRange[0]);
        this.horizon = (long) Math.ceil(horizon / PropertiesHolder.timeQuantum) * PropertiesHolder.timeQuantum;
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
                    int[] signal = new int[data.getSignal().length + getParents().size() + numberOfWrapperOutputs + 3];
                    int index = 0;
                    for (int sig : data.getSignal()) {
                        signal[index] = sig;
                        index++;
                    }
                    for (NeuralLearner parent : getParents()) {
                        FeedObject feedObject = parent.readNext(this);
                        if(feedObject.getTimeStamp() != data.getTimeStamp()){
                            System.err.println("Motherfeed timestamp: " + data.getTimeStamp() + " Parent " + parent + ": " + feedObject.getTimeStamp());
                        }

                        int sig = ((int[]) (feedObject.getData()))[parentFeeds.get(parent)];
                        signal[index] = sig;
                        index++;
                    }

                    signal[index] = (int) ((time % (86400000))/(2 * 3600000));
                    index++;

                    for(WrapperManipulatorPair pair : wrapperManipulatorPairs){
                        FeedObject<Integer[]> sigObject = cluster.getFromFeedWrapper(pair, time + outputFutureOffset);
                        for(int sig : sigObject.getData()){
                            signal[index] = sig;
                            index++;
                        }
                    }

                    if (pointsConsumed > 50) {
                        getSignalForDistribution(getDistribution(signal), 0);
                    }
                    signal[index] = getLogarithmicDiscretisation(ampDX, 0, 1, 2);
                    index++;
                    signal[index] = getLogarithmicDiscretisation(ampUX, 0, 1, 2);
                    index++;

                    ScratchPad.memory.put("NEURON_SIG_[" + id + "]", Arrays.toString(signal));

                    tStart = System.currentTimeMillis();
                    double result = 0 ;
                    while (!trackers.isEmpty() && trackers.get(0).getTimeStamp() < (time - horizon)) {
                        StateActionInformationTracker tracker = trackers.remove(0);

                        double recovery = tracker.getMaxUp() - tracker.getMaxDown();
                        int trackerState = tracker.getTimeState();
                        int recoveryId = 0;

                        LabelledTuple[] tuplesUp = new LabelledTuple[0];
                        LabelledTuple[] tuplesDown = new LabelledTuple[0];
                        if(trackerState == -1){
                            recoveryId = getLogarithmicDiscretisation(tracker.getMaxUp(), 0, resolution);
                            tuplesUp = new LabelledTuple[]{new LabelledTuple(this.RECOVERY_STRING, recoveryId, recovery)};
                        } else if(trackerState == 1){
                            recoveryId = getLogarithmicDiscretisation(tracker.getMaxDown(), 0, resolution);
                            tuplesDown = new LabelledTuple[]{new LabelledTuple(this.RECOVERY_STRING, recoveryId, recovery)};
                        }


                        if(oneCore){
                            core.addStateAction(tracker.getState(), tracker.getMaxUp());
                            core.addStateAction(tracker.getState(), tracker.getMaxDown());
                        }
                        else{
                            coreUpA.addStateAction(tracker.getState(), tracker.getMaxUp(), tuplesUp);
                            coreDownA.addStateAction(tracker.getState(), tracker.getMaxDown(), tuplesDown);

                            countA++;
                            if(PropertiesHolder.neuronReplacement){
                                if(coreUpB != null){
                                    coreUpB.addStateAction(tracker.getState(), tracker.getMaxUp(), tuplesUp);
                                    coreDownB.addStateAction(tracker.getState(), tracker.getMaxDown(), tuplesDown);
                                    countB++;

                                    if(countB == generationalLifespan){
                                        countA = countB;
                                        countB = 0;
                                        coreUpA = coreUpB;
                                        coreDownA = coreDownB;

                                        coreUpB = new LearnerService();
                                        coreUpB.setActionResolution(resolution);
                                        coreDownB = new LearnerService();
                                        coreDownB.setActionResolution(resolution);
                                        generation++;
                                        System.out.println("Neuron [" + id + "]: Cores regenerated, Generation: " + generation);
                                    }
                                }
                                else if(countA == countForFlip){
                                    coreUpB = new LearnerService();
                                    coreUpB.setActionResolution(resolution);
                                    coreDownB = new LearnerService();
                                    coreDownB.setActionResolution(resolution);
                                    System.out.println("Neuron [" + id + "]: Initial coreB creation");
                                }

                                weightA = (coreUpA.getCount() + coreDownA.getCount())/2;
                                if(coreUpB != null) {
                                    weightB = (coreUpA.getCount() + coreDownA.getCount()) / 2;
                                }
                            }
                        }

                        result += tracker.getPerformance();
                        pointsConsumed++;
                    }

                    for (StateActionInformationTracker tracker : trackers) {
                        tracker.processHighAndLow(data.getValue()[1], data.getValue()[2], time);
                    }
                    ScratchPad.memory.put("NEURON_OBS_[" + id + "]", Arrays.toString(data.getValue()));

                    int[] outputSignal = new int[getNumberOfOutputs()];

                    if (pointsConsumed > 50) {
                        predictionRaw = getDistribution(signal);
                        outputSignal = getSignalForDistribution(predictionRaw, result);
                    }

                    StateActionInformationTracker tracker = new StateActionInformationTracker(time, signal, data.getValue()[0], 20 * resolution, outputSignal[0]);
                    tracker.setDiscretisation(discretiser);
                    trackers.add(tracker);

                    if (pointsConsumed > PropertiesHolder.neuronLearningPeriod) {
                        adapting = false;
                    }
                    FeedObject output = new FeedObject(time, outputSignal);
                    queue.add(output);
                    latency = (long) ((0.75 * latency) + (0.25 * (System.currentTimeMillis() - tStart)));
                    checkPerformance(predictionRaw, recInfoX, data);
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
        if(oneCore){
            return core.getActionDistribution(signal);
        }

        TreeMap<Integer, Double> dA = getDistribution(signal, false);
        TreeMap<Integer, Double> dB = getDistribution(signal, true);

        ampUX = ampUA;
        ampDX = ampDA;
        recInfoX = recInfoA;
        if(dB == null){
            return dA;
        }
        if(dA == null){
            return null;
        }

        double fA = (double)weightA/(double)(weightA + weightB);
        double fB = 1 - fA;

        ampUX = fA * ampUA + fB * ampUB;
        ampDX = fA * ampDA + fB * ampDB;
        recInfoX = new AdditionalStateActionInformation();
        recInfoX.incorporate(recInfoA, fA);
        recInfoX.incorporate(recInfoB, fB);

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

            if(countB < 50){
                ampDB = 0;
                ampUB = 0;
                return null;
            }
            coreUp = coreUpB;
            coreDown = coreDownB;
        }

        TreeMap<Integer, Double> distribution = new TreeMap<>();
        if(coreUp.getPopulation().size() < 10 || coreDown.getPopulation().size() < 10){
            if(useBackUp){
                ampDB = 0;
                ampUB = 0;
            }
            else {
                ampDA = 0;
                ampUA = 0;
            }
            return null;
        }

        ActionInformationBundle bundleUp = coreUp.getActionInformation(signal, RECOVERY_STRING);
        TreeMap<Integer, Double> distUp = bundleUp.distribution;
        ActionInformationBundle bundleDown = coreDown.getActionInformation(signal, RECOVERY_STRING);
        TreeMap<Integer, Double> distDown = bundleDown.distribution;

        AdditionalStateActionInformation recoveryInfo = new AdditionalStateActionInformation();
        recoveryInfo.incorporate(bundleUp.actionInformationMap.get(RECOVERY_STRING), 1);
        recoveryInfo.incorporate(bundleDown.actionInformationMap.get(RECOVERY_STRING), 1);

        double weightUp = 0;
        double weightDown = 0;
        for(double w : distUp.values()){
            weightUp += w;
        }
        for(double w : distDown.values()){
            weightDown += w;
        }

        double ampU = 0;
        double ampD = 0;
        double netWeight = weightDown + weightUp;
        double upMult = netWeight/weightUp;
        double downMult = netWeight/weightDown;

        double cumU = 0;
        for (Map.Entry<Integer, Double> entry : distUp.entrySet()) {
            int key = entry.getKey();
            if(key < 0){
                System.err.println("NEURON_[" + id + "] - unexpected negative key in distUp");
            }
            double val = entry.getValue() * upMult;
            if(key == 0){
                key = 1;
            }
            else if(key == 1 && distribution.containsKey(key)){
                val += distribution.get(key);
            }

            cumU += val;
            if(cumU/netWeight < pCutOff){
                ampU = key;
            }

            distribution.put(key, val);
        }

        boolean ampDFound = false;
        double cumD = 0;
        for (Map.Entry<Integer, Double> entry : distDown.entrySet()) {
            int key = entry.getKey();
            if(key > 0){
                System.err.println("NEURON_[" + id + "] - unexpected positive key in distDown");
            }
            double val = entry.getValue() * downMult;
            if(key == 0){
                key = -1;
            }
            else if(key == -1 && distribution.containsKey(key)){
                val += distribution.get(key);
            }

            cumD += val;
            if(!ampDFound && cumD/netWeight > (1 - pCutOff)){
                ampD = key;
                ampDFound = true;
            }

            distribution.put(key, val);
        }

        if(useBackUp){
            ampDB = ampD;
            ampUB = ampU;
            recInfoB = recoveryInfo;
        }
        else {
            ampDA = ampD;
            ampUA = ampU;
            recInfoA = recoveryInfo;
        }

        return distribution;
    }

    public void checkPerformance(TreeMap<Integer, Double> predictionRaw, AdditionalStateActionInformation recInfoX, DataObject data) {
        if (!adapting) {
            Date executionInstant = new Date(time);
            if (!(executionInstant.getDay() == 0 || executionInstant.getDay() == 6)) {

                TreeMap<Double, Double> prediction = new TreeMap<Double, Double>();
                if(predictionRaw != null) {
                    for (Map.Entry<Integer, Double> entry : predictionRaw.entrySet()) {
                        prediction.put(data.getValue()[0] + entry.getKey() * resolution, entry.getValue());
                    }
                }
                DecisionAggregatorA.aggregateDecision(data, data.getValue()[0], prediction, recInfoX, horizon, false);
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

        if(oneCore){
            double score = 0;
            Map<Double, StateActionPair> alphas = core.getAlphaStates();
            for (Map.Entry<Double, StateActionPair> entry : alphas.entrySet()) {
                score += Math.pow(entry.getKey(), 2);
            }
            double n = alphas.size();
            score = Math.sqrt(score)/n;
            if(!"NaN".equals("" + score)){
                neuronRankings.update(this, score);
            } else{
                ScratchPad.incrementCountFor(ScratchPad.NEURON_RANKING_NAN);
            }
            return;
        }

        double score = 0;
        double n = 0;
        Map<Double, StateActionPair> alphas = coreUpA.getAlphaStates();
        for (Map.Entry<Double, StateActionPair> entry : alphas.entrySet()) {
            double increment = Math.pow(entry.getKey(), 2);
            if(!"NaN".equals(increment + "")) {
                score += increment;
            } else{
                ScratchPad.incrementCountFor(ScratchPad.NEURON_RANKING_NAN);
            }
        }
        n += alphas.size();

        alphas = coreDownA.getAlphaStates();
        for (Map.Entry<Double, StateActionPair> entry : alphas.entrySet()) {
            double increment = Math.pow(entry.getKey(), 2);
            if(!"NaN".equals(increment + "")) {
                score += increment;
            } else{
                ScratchPad.incrementCountFor(ScratchPad.NEURON_RANKING_NAN);
            }
        }
        n += alphas.size();
        score = Math.sqrt(score/n);
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

    public int[] getSignalForDistribution(TreeMap<Integer, Double> distribution, double confidence) {
        int[] signal = new int[getNumberOfOutputs()];

        double mean = 0;
        double stddev = 0;
        if(distribution != null) {
            double sum = 0;
            double weight = 0;

            for (Map.Entry<Integer, Double> entry : distribution.entrySet()) {
                sum += entry.getKey() * entry.getValue();
                weight += entry.getValue();
            }


            if (weight > 0) {
                mean = sum / weight;
            }
        }

        lastMean = (1 - lambda) * lastMean + lambda * mean;
        this.confidence = (1 - lambda) * this.confidence + lambda * confidence;
        lastDecision = signal[0] = getLogarithmicDiscretisation(mean, 0, 1, 2);
        PropertiesHolder.neuronOpinions.put(this.getID(), lastDecision);
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
