package ai.context.learning.neural;

import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.server.JettyServer;
import ai.context.util.server.servlets.NeuralClusterInformationServlet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NeuronCluster {

    private JettyServer server = new JettyServer(8000);
    private static volatile NeuronCluster instance = null;
    private NeuronCluster(){
        service.submit(environmentCheck);
        PropertiesHolder.maxPopulation = 200;
        PropertiesHolder.tolerance = 0.1;

        server.addServlet(NeuralClusterInformationServlet.class, "info");
    }

    public static NeuronCluster getInstance() {
        if (instance == null) {
            synchronized (NeuronRankings.class){
                if (instance == null) {
                    instance = new NeuronCluster ();
                }
            }
        }
        return instance;
    }

    public void startServer(){
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long totalPointsConsumed = 0;
    private long minLatency = 100L;
    private double dangerLevel = 1;
    private ExecutorService service = Executors.newCachedThreadPool();
    private Set<NeuralLearner> neurons = Collections.newSetFromMap(new ConcurrentHashMap<NeuralLearner, Boolean>());

    public void start(NeuralLearner neuron){
        neurons.add(neuron);
        service.execute(neuron);
    }

    public double getDangerLevel() {
        return dangerLevel;
    }

    private Runnable environmentCheck = new Runnable() {
        @Override
        public void run() {
            while (true){
                try {
                    Thread.sleep((long) (Math.random() * 10000));
                    totalPointsConsumed = 0;
                    double latency = 0;
                    Set<NeuralLearner> toRemove = new HashSet<>();
                    for(NeuralLearner neuron : neurons){
                        if(!neuron.isAlive()){
                            toRemove.add(neuron);
                        }
                        else {
                            latency += neuron.getLatency();
                            neuron.updateRankings();
                            totalPointsConsumed += neuron.getPointsConsumed();
                            System.err.println(neuron.getDescription(0, "") + ": Latency: " + neuron.getLatency() + " Time: " + new Date(neuron.getLatestTime()));
                        }
                    }
                    neurons.removeAll(toRemove);
                    latency /= neurons.size();
                    System.err.println("Mean Latency: " + latency + ", Points Consumed: " + totalPointsConsumed);
                    dangerLevel = latency/minLatency;

                    for(NeuralLearner neuron : neurons){
                        if(Math.random() > 0.95){
                            neuron.lifeEvent();
                        }
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
}
