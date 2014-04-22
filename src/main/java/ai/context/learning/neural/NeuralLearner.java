package ai.context.learning.neural;

import ai.context.core.ai.LearnerService;
import ai.context.core.ai.LearningException;
import ai.context.core.ai.StateActionPair;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchFeed;
import ai.context.learning.DataObject;
import ai.context.learning.SelectLearnerFeed;
import ai.context.util.common.StateActionInformationTracker;
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
    private StimuliRankings stimuliRankings = StimuliRankings.getInstance();
    private long time = 0;
    private SynchFeed motherFeed;
    private SelectLearnerFeed learnerFeed;
    private Map<NeuralLearner, Integer> parentFeeds = new HashMap<>();
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


    //Performance
    private HashSet<OpenPosition> positions = new HashSet<OpenPosition>();
    private double accruedPnL = 0;

    private boolean inLiveTrading = false;
    private BlackBox blackBox;

    private boolean adapting = true;

    public NeuralLearner(long[] horizonRange, SynchFeed motherFeed, Integer[] actionElements, Integer[] sigElements, long outputFutureOffset, double resolution) {
        this.motherFeed = motherFeed;
        //learnerFeed.setName(id + "");
        this.horizonRange = horizonRange;
        horizon = (long) (Math.random() * (horizonRange[1] - horizonRange[0]) + horizonRange[0]);
        this.outputFutureOffset = outputFutureOffset;
        this.actionElements = actionElements;
        this.sigElements = sigElements;
        this.resolution = resolution;
        core.setActionResolution(resolution);

        NeuralLearner[] candidates = cluster.getNeurons();
        double chance = 3.0 / candidates.length;
        for (NeuralLearner parentCandidate : candidates) {
            if (Math.random() < chance) {
                parentFeeds.put(parentCandidate, (int) (Math.random() * parentCandidate.getNumberOfOutputs()));
                parentCandidate.addChild(this);
            }
        }
        this.learnerFeed = new SelectLearnerFeed(motherFeed, actionElements, sigElements);
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
        core = null;
        neuronRankings = null;
        stimuliRankings = null;
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
            String inputDates = new Date(data.getTimeStamp()) + " ";
            int[] signal = new int[data.getSignal().length + getParents().size()];
            int index = 0;
            for (int sig : data.getSignal()) {
                signal[index] = sig;
                index++;
            }

            for (NeuralLearner parent : getParents()) {
                FeedObject feedObject = parent.readNext(this);
                inputDates += new Date(feedObject.getTimeStamp()) + " ";

                int sig = ((int[]) (feedObject.getData()))[parentFeeds.get(parent)];
                signal[index] = sig;
                index++;
            }

            if (data.getTimeStamp() > time) {
                tStart = System.currentTimeMillis();
                time = data.getTimeStamp();
                while (!trackers.isEmpty() && trackers.get(0).getTimeStamp() < (time - horizon)) {
                    StateActionInformationTracker tracker = trackers.remove(0);
                    core.addStateAction(tracker.getState(), tracker.getMax());
                    core.addStateAction(tracker.getState(), tracker.getMin());
                    //core.addStateAction(tracker.getState(), data.getValue()[0]);

                    //System.out.println("Learned: " + Arrays.toString(tracker.getState()) + " -> {" + tracker.getMin() + ", " + tracker.getMax() + "}");
                    pointsConsumed++;
                }

                for (StateActionInformationTracker tracker : trackers) {
                    for (double newLevel : data.getValue()) {
                        tracker.aggregate(newLevel);
                    }
                }
                trackers.add(new StateActionInformationTracker(time, signal, data.getValue()[0]));

                int[] outputSignal = new int[getNumberOfOutputs()];
                if (pointsConsumed > 100) {
                    outputSignal = getSignalForDistribution(core.getActionDistribution(signal));
                }

                if (pointsConsumed > 2000) {
                    adapting = false;
                }
                long eventTime = time/* + outputFutureOffset*/;
                FeedObject output = new FeedObject(eventTime, outputSignal);
                //System.out.println("["+id+"] Output produced: " + output/* + " from dates: " + inputDates*/);
                queue.add(output);
                latency = (long) ((0.75 * latency) + (0.25 * (System.currentTimeMillis() - tStart)));
                checkPerformance(signal, data);
            }
        } catch (LearningException e) {
            System.err.println("Learning exception: " + e.getReason() + " Neuron " + id + " is dying...");
            throw e;
        } catch (Exception e) {
                /*System.err.println("Exception: " + e.getMessage() + " Neuron "+ id +" is dying...");
                e.printStackTrace();
                break;*/
        }
    }

    public void checkPerformance(int[] signal, DataObject data) {
        if (!adapting) {

            /*HashSet<OpenPosition> closed = new HashSet<OpenPosition>();
            for (OpenPosition position : positions) {
                if (position.canCloseOnBar_Pessimistic(data.getTimeStamp(), data.getValue()[1], data.getValue()[2], data.getValue()[0])) {
                    closed.add(position);
                    PositionFactory.positionClosed(position);

                    System.out.println(position.getClosingMessage() + " CHANGE: " + position.getPnL() + " CAPITAL: " + PositionFactory.getAmount() + " ACCRUED PNL: " + PositionFactory.getAccruedPnL());
                }
            }
            positions.removeAll(closed);*/


            Date executionInstant = new Date(time);
            if (/*positions.size() < 15 && */!(executionInstant.getDay() == 0 || executionInstant.getDay() == 6)) {
                TreeMap<Integer, Double> distribution = core.getActionDistribution(signal);
                TreeMap<Double, Double> prediction = new TreeMap<Double, Double>();
                for (Map.Entry<Integer, Double> entry : distribution.entrySet()) {
                    prediction.put(data.getValue()[0] + entry.getKey() * core.getActionResolution(), entry.getValue());
                }
                DecisionAggregator.aggregateDecision(data, data.getValue()[0], prediction, horizon, false);
                /*OpenPosition position = PositionFactory.getPosition(data.getTimeStamp(), data.getValue()[0], prediction, horizon, false);
                if (position != null) {
                    if (inLiveTrading) {
                        try {
                            blackBox.onDecision(position);
                        } catch (JFException e) {
                            e.printStackTrace();
                        }
                    } else {
                        positions.add(position);
                    }
                }*/
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
            stimuliRankings.newStimuli(outputElements);
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
        /*else if(pointsConsumed > 5000){
            if(cluster.getDangerLevel() * Math.random() < 2.5){
                //selectStimuli();
            }
            else {
                double rank = 0;
                double total = 0;
                boolean found = false;
                for(NeuralLearner neuron : neuronRankings.getRankings().values()){
                    if(neuron.getPointsConsumed() > 5000){
                        total++;
                        if(!found){
                            rank++;
                        }
                        if(this == neuron){
                            found = true;
                        }
                    }
                }

                if(total > 10 && rank/total < 0.1){
                    die();
                }
            }
        }*/
    }

    public void updateRankings() {
        if (paused) {
            return;
        }
        double score = 0;
        Map<Double, StateActionPair> alphas = core.getAlphaStates();
        //double[] stimuliScores = new double[sigElements.length];
        for (Map.Entry<Double, StateActionPair> entry : alphas.entrySet()) {
            score += Math.pow(entry.getKey(), 2) * Math.log(entry.getValue().getTotalWeight());
            /*int i = 0;
            for(double weight : core.getCorrelationWeightsForState(entry.getValue())){
                stimuliScores[i] += entry.getKey() * weight * Math.log(entry.getValue().getTotalWeight());
                i++;
            }*/
        }
        score /= alphas.size();
        neuronRankings.update(this, score);

        /*HashMap<Integer, Double> data = new HashMap<>();
        for(int i = 0; i < sigElements.length; i++){
            data.put(sigElements[i], stimuliScores[i]);
        }
        stimuliRankings.update(this, data);*/
    }

    /*long lastSelect = 0;
    public void selectStimuli(){
        if(paused || time - lastSelect < (Math.random() * 90 * 86400000L)){
            return;
        }
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
            if(entry.getValue() > worseRanking && entry.getValue() > level * Math.random()){
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
    }*/

    public void spawn() {
        if (paused) {
            return;
        }
        Integer[] sigElements = new Integer[this.sigElements.length];
        Set<Integer> used = new HashSet<>();
        used.addAll(Arrays.asList(actionElements));
        used.addAll(Arrays.asList(sigElements));
        List<Integer> available = new ArrayList<>(stimuliRankings.getStimuli());
        int choice = available.size();
        for (int i = 0; i < sigElements.length; i++) {
            sigElements[i] = this.sigElements[i];
            if (Math.random() > 0.5) {
                for (int tries = 0; tries < 10; tries++) {
                    int candidate = available.get(Math.min(choice - 1, (int) (Math.random() * choice)));
                    if (Math.random() > 0.2) {
                        candidate = available.get(Math.max(0, Math.min(choice - 1, choice - (int) (Math.random() * 50))));
                    }
                    if (!used.contains(candidate)) {
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

    /*public void die(){
        if(paused || time - lastSelect < (Math.random() * 90 * 86400000L)){
            return;
        }
        System.out.println(getDescription(0, "") + " dies...");
        alive = false;

        stimuliRankings.removeAllStimuli(this, this.getFlowData()[2]);
    }*/

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

    double lambda = 0.01;
    double lastMean = 0;

    public int[] getSignalForDistribution(TreeMap<Integer, Double> distribution) {
        int[] signal = new int[getNumberOfOutputs()];

        double sum = 0;
        double sumSq = 0;
        double weight = 0;
        double accruedWeight = 0;

        Integer firstQuartile = null;
        Integer median = null;
        Integer thirdQuartile = null;
        for (Map.Entry<Integer, Double> entry : distribution.entrySet()) {
            accruedWeight += entry.getValue();
            if (accruedWeight >= weight / 4 && firstQuartile == null) {
                firstQuartile = entry.getKey();
            } else if (accruedWeight >= weight / 2 && median == null) {
                median = entry.getKey();
            } else if (accruedWeight >= 3 * weight / 4 && thirdQuartile == null) {
                thirdQuartile = entry.getKey();
            }
            sum += entry.getKey() * entry.getValue();
            sumSq += Math.pow(entry.getKey() * entry.getValue(), 2);
            weight += entry.getValue();
        }

        double mean = 0;
        double stddev = 0;
        if (weight > 0) {
            mean = sum / weight;
            stddev = Math.sqrt(sumSq / weight - Math.pow(mean, 2));
        }

        if (firstQuartile == null) {
            firstQuartile = 0;
        }

        if (median == null) {
            median = 0;
        }

        if (thirdQuartile == null) {
            thirdQuartile = 0;
        }

        lastMean = (1 - lambda) * lastMean + lambda * mean;

        signal[0] = getLogarithmicDiscretisation(mean, 0, 1, 2);
        //signal[1] = getLogarithmicDiscretisation(stddev, 0, 1, 2);
        //signal[2] = getLogarithmicDiscretisation(firstQuartile, 0, 1, 2);
        //signal[3] = getLogarithmicDiscretisation(median, 0, 1, 2);
        //signal[4] = getLogarithmicDiscretisation(thirdQuartile, 0, 1, 2);
        signal[1] = getLogarithmicDiscretisation(mean - lastMean, 0, 1, 2);

        /*System.out.println(mean + ", " +
                stddev + ", " +
                firstQuartile + ", " +
                median + ", " +
                thirdQuartile + ", " +
                (mean - lastMean));*/
        return signal;
    }

    public TreeMap<Integer, Double> ask(Object[] fullSignal) {
        int[] state = new int[sigElements.length];
        for (int i = 0; i < sigElements.length; i++) {
            state[i] = (int) fullSignal[sigElements[i]];
        }
        return core.getActionDistribution(state);
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
            feedObject = queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Feed listener : buffers.keySet()) {
            if (listener != caller) {
                buffers.get(listener).add(feedObject);
            }
        }
        //System.out.println("["+id+"] RETURNING: " + feedObject);
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

    /*public void inputsRemoved(Integer[] inputs){
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
    }*/

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
        core = null;
        neuronRankings = null;
        stimuliRankings = null;
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
        return padding + "[" + id + "] Neural Learner with Actions: " + Arrays.asList(actionElements) + " and Signals: " + Arrays.asList(sigElements);
    }

    @Override
    public List getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 2;
    }
}
