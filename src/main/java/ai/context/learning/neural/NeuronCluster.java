package ai.context.learning.neural;

import ai.context.container.TimedContainer;
import ai.context.core.ai.LearningException;
import ai.context.feed.FeedObject;
import ai.context.feed.manipulation.FeedWrapper;
import ai.context.feed.manipulation.WrapperManipulatorPair;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.runner.MainNeural;
import ai.context.runner.MainNeural2;
import ai.context.util.common.MapUtils;
import ai.context.util.common.ScratchPad;
import ai.context.util.common.email.EmailSendingService;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.Operations;
import ai.context.util.server.JettyServer;
import ai.context.util.server.servlets.LiveTradingServlet;
import ai.context.util.server.servlets.NeuralClusterInformationServlet;
import ai.context.util.server.servlets.ScriptingServlet;
import ai.context.util.trading.version_1.DecisionAggregatorA;
import ai.context.util.trading.version_1.MarketMakerPosition;
import ai.context.util.trading.version_1.PositionFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class NeuronCluster implements TimedContainer{

    private NeuronRankings rankings = NeuronRankings.getInstance();
    private ISynchFeed motherFeed;
    private JettyServer server = new JettyServer(PropertiesHolder.httpPort);
    private static volatile NeuronCluster instance = null;
    private AtomicInteger newID = new AtomicInteger(0);

    private Map<Integer, NeuralLearner> outputToNeuron = new HashMap<>();
    private Map<Integer, NeuralLearner> idToNeuron = new HashMap<>();

    public List<Integer[]> seedFeeds = new ArrayList<>();
    public AtomicInteger calibrationCount = new AtomicInteger(0);
    public int maxCalibrationIterations = 5;

    private long calibrationPoints = 20000;
    private long meanTime = 0;
    private long totalPointsConsumed = 0;
    private int consumed = 0;
    private long minLatency = 50L;
    private double dangerLevel = 1;
    private ExecutorService service = Executors.newCachedThreadPool();
    private List<NeuralLearner> neurons = new CopyOnWriteArrayList<>();
    private List<FeedWrapper> feedWrappers = new ArrayList<>();

    private EmailSendingService emailSendingService = new EmailSendingService();

    public MainNeural container;
    public MainNeural2 container2;

    private NeuronCluster() {
        server.addServlet(NeuralClusterInformationServlet.class, "info");
        server.addServlet(ScriptingServlet.class, "scripting");
        server.addServlet(LiveTradingServlet.class, "live");
    }

    public void start() {
        service.submit(stepper);
        service.submit(environmentCheck);
        service.submit(emailSendingService);
    }

    public void setContainer(MainNeural container) {
        this.container = container;
    }

    private boolean parentsHaveNext(NeuralLearner neuron) {
        boolean hasNext = false;
        Collection<NeuralLearner> parents = neuron.getParents();
        for (NeuralLearner parent : parents) {
            hasNext = parent.hasNext(neuron);
            if (!hasNext) {
                return false;
            }
        }
        return hasNext;
    }

    public static NeuronCluster getInstance() {
        if (instance == null) {
            synchronized (NeuronRankings.class) {
                if (instance == null) {
                    instance = new NeuronCluster();
                    instance.startServer();
                }
            }
        }
        return instance;
    }

    public void startServer() {
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(NeuralLearner neuron) {
        neurons.add(neuron);
        idToNeuron.put(neuron.getID(), neuron);
    }

    public void addFeedWrapper(FeedWrapper wrapper){
        feedWrappers.add(wrapper);
    }

    public WrapperManipulatorPair assign(){
        int wrapper = (int) (Math.random() * feedWrappers.size());
        if(wrapper == feedWrappers.size()){
            return null;
        }
        else {
            List<String> possibilities = feedWrappers.get(wrapper).getPossibleManipulators();
            int m = (int) (Math.random() * possibilities.size());
            if(m == possibilities.size()){
                return null;
            }
            String manipulator = possibilities.get(m);
            return new WrapperManipulatorPair(wrapper, manipulator);
        }
    }

    public FeedObject<Integer[]> getFromFeedWrapper(WrapperManipulatorPair id, long time){
        return feedWrappers.get(id.getWrapperId()).getAtTimeForManipulator(time, id.getManipulatorId());
    }

    public int getNumberOfOutputsFor(List<WrapperManipulatorPair> pairs){
        int n = 0;

        for(WrapperManipulatorPair pair : pairs){
            n += feedWrappers.get(pair.getWrapperId()).getManipulator(pair.getManipulatorId()).getNumberOfOutputs();
        }
        return n;
    }

    public void setMotherFeed(ISynchFeed motherFeed) {
        this.motherFeed = motherFeed;
    }

    public double getDangerLevel() {
        return dangerLevel;
    }

    private Runnable stepper = new Runnable() {
        @Override
        public void run() {
            while (true) {
                boolean fed = false;
                for (NeuralLearner neuron : neurons) {
                    try {
                        neuron.step();
                        while (parentsHaveNext(neuron)) {
                            neuron.step();
                        }
                        fed = true;
                    } catch (LearningException e) {
                        e.printStackTrace();
                        neuron.cleanup();
                    }
                }
                if (neurons.isEmpty()) {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //tryAndCreateNewGeneration();
                if(fed){
                    consumed++;
                }
            }
        }
    };

    private Runnable environmentCheck = new Runnable() {
        @Override
        public void run() {
        while (true) {
            try {
                Thread.sleep(600000);
                if(!neurons.isEmpty()){
                    totalPointsConsumed = 0;
                    double latency = 0;
                    long meanT = 0;
                    Set<NeuralLearner> toRemove = new HashSet<>();
                    for (NeuralLearner neuron : neurons) {
                        if (!neuron.isAlive()) {
                            toRemove.add(neuron);
                        } else {
                            latency += neuron.getLatency();
                            totalPointsConsumed += neuron.getPointsConsumed();
                            meanT += neuron.getLatestTime();
                        }
                    }

                    for (NeuralLearner removed : toRemove) {
                        neurons.remove(removed);
                        removed.cleanup();
                    }
                    for (NeuralLearner neuron : neurons) {
                        neuron.updateRankings();
                    }

                    meanTime = meanT / neurons.size();
                    latency /= neurons.size();
                    System.err.println("Mean Latency: " + Operations.round(latency, 3) + ", Points Consumed: " + totalPointsConsumed + ", Overall Score: " + Operations.round(rankings.getOverallMarking(), 4) + " as of " + new Date(meanTime));
                    dangerLevel = latency / minLatency;

                    if(!DecisionAggregatorA.isInLiveTrading() && meanTime > (System.currentTimeMillis() - 120 * 60000L)){
                        DecisionAggregatorA.setInLiveTrading(true);
                        //DecisionAggregatorC.setInLiveTrading(true);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        }
    };

    int year = 0;
    public void tryAndCreateNewGeneration(){
        int year = new Date(meanTime).getYear();
        if(year > this.year){
            this.year = year;
            generateNeurons();
        }
    }

    public void addTask(Runnable task){
        service.submit(task);
    }

    public int size() {
        return neurons.size();
    }

    public String getClusterStateJSON() {
        Map<NeuralLearner, Double> scores = MapUtils.reverse(rankings.getRankings());
        Map<Integer, String> feedSources = new HashMap<>();
        for (int i = 0; i < motherFeed.getNumberOfOutputs(); i++) {
            feedSources.put(i, "MOTHER FEED");
        }

        Map<String, Integer> neuronMapping = new HashMap<>();
        String nodes = "\"nodes\": [";
        int iNeuron = 0;
        for (NeuralLearner neuron : neurons) {
            String id = "Neuron " + neuron.getID();
            neuronMapping.put(id, iNeuron);
            iNeuron++;
            if (neuron.getFlowData()[2] != null) {
                for (int i : neuron.getFlowData()[2]) {
                    feedSources.put(i, id);
                }
            }
            double score = 0;
            if (scores.containsKey(neuron)) {
                score = scores.get(neuron);
            }
            score = Math.min(score, 1.0);
            score = Math.max(score, 0.0);
            if ((score + "").equals("NaN")) {
                ScratchPad.incrementCountFor(ScratchPad.NEURON_SCORE_NAN);
                score = 0.0;
            }
            nodes += "{\"x\": 0, \"y\": 0, " +
                    "\"name\" : \"" + id + "\", " +
                    "\"score\" : " + score +
                    "},";
        }
        nodes += "{\"x\": 0, \"y\": 0, \"name\" : \"MOTHER FEED\", \"score\" : 0.0},";
        nodes += "{\"x\": 0, \"y\": 0, \"name\" : \"DEAD NEURONS\", \"score\" : 0.0}";
        nodes += "]";
        neuronMapping.put("MOTHER FEED", iNeuron);
        neuronMapping.put("DEAD NEURONS", iNeuron++);

        HashSet<String> existingLinks = new HashSet<>();

        String links = "\"links\": [";
        for (NeuralLearner neuron : neurons) {
            String id = "Neuron " + neuron.getID();
            for (int i : neuron.getFlowData()[0]) {
                Integer source = neuronMapping.get(feedSources.get(i));
                if (source == null) {
                    source = iNeuron;
                }
                String newLink = "{\"source\": " + source
                        + ", \"target\": " + neuronMapping.get(id)
                        + "},";
                if (!existingLinks.contains(newLink)) {
                    existingLinks.add(newLink);
                    links += newLink;
                }
            }

            for (int i : neuron.getFlowData()[1]) {
                Integer source = neuronMapping.get(feedSources.get(i));
                if (source == null) {
                    source = iNeuron;
                }
                String newLink = "{\"source\": " + source
                        + ", \"target\": " + neuronMapping.get(id)
                        + "},";
                if (!existingLinks.contains(newLink)) {
                    existingLinks.add(newLink);
                    links += newLink;
                }
            }

            for (NeuralLearner parent : neuron.getParents()) {
                String newLink = "{\"source\": " + neuronMapping.get("Neuron " + parent.getID())
                        + ", \"target\": " + neuronMapping.get(id)
                        + "},";
                if (!existingLinks.contains(newLink)) {
                    existingLinks.add(newLink);
                    links += newLink;
                }
            }
        }
        links = links.substring(0, links.length() - 1);
        links += "]";
        return "{" + nodes + "," + links + "}";
    }

    public void generateNeurons(){
        long[] horizonRange = new long[]{PropertiesHolder.horizonLowerBound, PropertiesHolder.horizonUpperBound};
        Integer[] actionElements = new Integer[]{3, 1, 2, 0};
        long outputFutureOffset = 30 * 60 * 1000L;
        double resolution = 0.0001;
        Set<Integer> availableStimuli = new HashSet<>();
        for (int i = 0; i < motherFeed.getNumberOfOutputs(); i++) {
            availableStimuli.add(i);
        }
        availableStimuli.removeAll(Arrays.asList(actionElements));
        for (int i = 0; i < PropertiesHolder.generationNeurons; i++) {
            Integer[] sigElements = new Integer[PropertiesHolder.coreStimuliPerNeuron];
            for (int sig = 0; sig < sigElements.length; sig++) {
                if (availableStimuli.isEmpty()) {
                    for (int index = 0; index < motherFeed.getNumberOfOutputs(); index++) {
                        availableStimuli.add(index);
                    }
                    availableStimuli.removeAll(Arrays.asList(actionElements));
                }
                List<Integer> available = new ArrayList<>(availableStimuli);
                int chosenSig = available.get((int) (Math.random() * available.size()));
                availableStimuli.remove(chosenSig);
                sigElements[sig] = chosenSig;
            }
            NeuronCluster.getInstance().start(new NeuralLearner(horizonRange, motherFeed, actionElements, sigElements, null, null, outputFutureOffset, resolution));
        }
    }

    public NeuralLearner getNeuronForOutput(int sig) {
        return outputToNeuron.get(sig);
    }

    public NeuralLearner[] getNeurons() {
        return neurons.toArray(new NeuralLearner[neurons.size()]);
    }

    public int getNewID() {
        return newID.incrementAndGet();
    }

    public long getMeanTime() {
        return meanTime;
    }

    public void setMeanTime(long meanTime) {
        this.meanTime = meanTime;
    }

    public void reset(){
        neurons = new CopyOnWriteArrayList<>();

        rankings.reset();
        motherFeed = null;
        newID = new AtomicInteger(0);

        outputToNeuron = new HashMap<>();
        idToNeuron = new HashMap<>();

        meanTime = 0;
        totalPointsConsumed = 0;
        minLatency = 50L;
        dangerLevel = 1;
    }

    public String getStats() {
        String stats = "TIME: " + meanTime + ", STATS: ";
        stats += "TOTAL TRADES: " + PositionFactory.getTotalTrades() + ", ";
        stats += "TRADES PROFIT: " + PositionFactory.getTotalProfit() + ", ";
        stats += "TRADES LOST: " + PositionFactory.getTotalLoss() + ", ";
        stats += "AVERAGE PROFIT: " + PositionFactory.getSumProfit() / PositionFactory.getTotalProfit() + ", ";
        stats += "AVERAGE LOST: " + PositionFactory.getSumLoss() / PositionFactory.getTotalLoss() + ", ";
        stats += "PNL: " + PositionFactory.getAccruedPnL();
        return stats;
    }

    public String getPred(int neuronId){
        if(container2 != null){
            TreeMap<Integer, Double> predRaw = container2.neurons.get(neuronId - 1).getPredictionRaw();
            String toReturn = "";
            for(Map.Entry<Integer, Double> entry : predRaw.entrySet()){
                toReturn += entry.getKey() + "," + entry.getValue() + "\n";
            }
            return toReturn;
        }

        TreeMap<Integer, Double> predRaw = idToNeuron.get(neuronId).getPredictionRaw();
        String toReturn = "";
        for(Map.Entry<Integer, Double> entry : predRaw.entrySet()){
            toReturn += entry.getKey() + "," + entry.getValue() + "\n";
        }
        return toReturn;
    }

    @Override
    public long getTime() {
        return meanTime;
    }

    public NeuralLearner getNeuronForId(int id) {
        return idToNeuron.get(id);
    }

    public EmailSendingService getEmailSendingService() {
        return emailSendingService;
    }

    public Collection<MarketMakerPosition> getMarketMakerPositions() {
        return DecisionAggregatorA.getMarketMakerPositions();
    }

    public int getConsumed() {
        return consumed;
    }

    public String getStringForStimuli(int n){
        String s = "";
        for(FeedObject data : motherFeed.getLatest(n)){
            List<Object> content = ((List) data.getData());
            s += data.getTimeStamp();
            for(Object c : content){
                s += "," + c;
            }
            s += "\n";
        }
        return s;
    }
}
