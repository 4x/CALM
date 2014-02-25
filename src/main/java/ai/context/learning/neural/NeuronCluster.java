package ai.context.learning.neural;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NeuronCluster {

    private static volatile NeuronCluster instance = null;
    private NeuronCluster(){
        service.submit(environmentCheck);
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

    private long minLatency = 1000000L;
    private double dangerLevel = 1;
    private ExecutorService service = Executors.newCachedThreadPool();
    private HashSet<NeuralLearner> neurons = new HashSet<>();

    public void start(NeuralLearner neuron){
        neurons.add(neuron);
        service.submit(neuron);
    }

    public double getDangerLevel() {
        return dangerLevel;
    }

    private Runnable environmentCheck = new Runnable() {
        @Override
        public void run() {
            while (true){
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                double latency = 0;
                Set<NeuralLearner> toRemove = new HashSet<>();
                for(NeuralLearner neuron : neurons){
                    if(!neuron.isAlive()){
                        toRemove.add(neuron);
                    }
                    else {
                        latency += neuron.getLatency();
                        neuron.updateRankings();
                    }
                }
                neurons.removeAll(toRemove);
                latency /= neurons.size();
                dangerLevel = latency/minLatency;
            }
        }
    };
}
