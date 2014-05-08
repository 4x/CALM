package ai.context.learning.neural;

import ai.context.container.TimedContainer;
import ai.context.core.ai.LearningException;
import ai.context.feed.synchronised.SynchFeed;
import ai.context.util.common.MapUtils;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.Operations;
import ai.context.util.server.JettyServer;
import ai.context.util.server.servlets.NeuralClusterInformationServlet;
import ai.context.util.trading.DecisionAggregator;
import ai.context.util.trading.PositionFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class NeuronCluster implements TimedContainer{

    private NeuronRankings rankings = NeuronRankings.getInstance();
    private StimuliRankings stimuliRankings = StimuliRankings.getInstance();
    private SynchFeed motherFeed;
    private JettyServer server = new JettyServer(8055);
    private static volatile NeuronCluster instance = null;
    private AtomicInteger newID = new AtomicInteger(0);

    private Map<Integer, NeuralLearner> outputToNeuron = new HashMap<>();

    private long meanTime = 0;
    private long totalPointsConsumed = 0;
    private long minLatency = 50L;
    private double dangerLevel = 1;
    private ExecutorService service = Executors.newCachedThreadPool();
    private List<NeuralLearner> neurons = new CopyOnWriteArrayList<>();

    private NeuronCluster() {
        PropertiesHolder.maxPopulation = 500;
        PropertiesHolder.tolerance = 0.02;

        server.addServlet(NeuralClusterInformationServlet.class, "info");
    }

    public void start() {
        service.submit(stepper);
        service.submit(environmentCheck);
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
        //service.execute(neuron);
    }

    public void setMotherFeed(SynchFeed motherFeed) {
        this.motherFeed = motherFeed;
    }

    public double getDangerLevel() {
        return dangerLevel/* * (0.5 + 0.5 * Math.sin(check))*/;
    }

    private Runnable stepper = new Runnable() {
        @Override
        public void run() {
            while (true) {
                for (NeuralLearner neuron : neurons) {
                    try {
                        neuron.step();
                        while (parentsHaveNext(neuron)) {
                            neuron.step();
                        }
                    } catch (LearningException e) {
                        e.printStackTrace();
                        neuron.cleanup();
                    }
                }

                /*for(NeuralLearner neuron : neurons){
                    if(Math.random() > 0.99){
                        neuron.lifeEvent();
                    }
                }*/

                if (neurons.isEmpty()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private Runnable environmentCheck = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(10000);
                    /*outputToNeuron.clear();
                    for(NeuralLearner neuron : neurons){
                        if(neuron.getFlowData()[2] != null){
                            for(int i : neuron.getFlowData()[2]){
                                outputToNeuron.put(i, neuron);
                            }
                        }
                    }*/
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
                            //System.err.println(neuron.getDescription(0, "") + ": Latency: " + neuron.getLatency() + " Time: " + new Date(neuron.getLatestTime()));
                            meanT += neuron.getLatestTime();
                        }
                    }

                    for (NeuralLearner removed : toRemove) {
                        neurons.remove(removed);
                        /*for(NeuralLearner neuron : neurons){
                            neuron.inputsRemoved(removed.getFlowData()[2]);
                        }
                        motherFeed.removeRawFeed(removed);*/
                        removed.cleanup();
                    }
                    //stimuliRankings.clearAndRepopulateStimuli(motherFeed.getNumberOfOutputs());
                    for (NeuralLearner neuron : neurons) {
                        neuron.updateRankings();
                    }

                    meanTime = meanT / neurons.size();
                    latency /= neurons.size();
                    System.err.println("Mean Latency: " + Operations.round(latency, 3) + ", Points Consumed: " + totalPointsConsumed + ", Overall Score: " + Operations.round(rankings.getOverallMarking(), 4) + " as of " + new Date(meanTime));
                    dangerLevel = latency / minLatency;

                    if(!DecisionAggregator.isInLiveTrading() && meanTime > (System.currentTimeMillis() - 10 * 60000L)){
                        DecisionAggregator.setInLiveTrading(true);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

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

    @Override
    public long getTime() {
        return meanTime;
    }
}
