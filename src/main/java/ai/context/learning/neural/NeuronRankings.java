package ai.context.learning.neural;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class NeuronRankings {
    private static volatile NeuronRankings instance = null;
    private NeuronRankings(){}

    public static NeuronRankings getInstance() {
        if (instance == null) {
            synchronized (NeuronRankings.class){
                if (instance == null) {
                    instance = new NeuronRankings ();
                }
            }
        }
        return instance;
    }

    private Map<NeuralLearner, Double> neurons = new HashMap<>();
    private Map<Double, NeuralLearner> rankings = new TreeMap<>();
    private ConcurrentHashMap<Object,HashMap<Integer, Double>> scores = new ConcurrentHashMap();
    public void update(NeuralLearner updater, Double score){

        Integer[] inputs = updater.getFlowData()[1];
        for(int sig : inputs){
            NeuralLearner parent = NeuronCluster.getInstance().getNeuronForOutput(sig);
            if(parent != null && neurons.containsKey(parent)){
                double pScore = neurons.get(parent);
                rankings.remove(pScore);
                double lambda = 1.0/updater.getNumberOfOutputs();
                pScore = (1 - lambda) * pScore + lambda * score;
                rankings.put(pScore, parent);
            }
        }

        Map.Entry toRemove = null;
        synchronized (rankings){
            for(Map.Entry<Double, NeuralLearner> entry : rankings.entrySet()){
                if(entry.getValue() == updater){
                    toRemove = entry;
                }
            }
        }
        rankings.entrySet().remove(toRemove);
        rankings.put(score, updater);
        neurons.put(updater, score);
    }

    public Map<Double, NeuralLearner> getRankings(){
        return rankings;
    }

    public void remove(NeuralLearner neuron){
        synchronized (rankings){
            rankings.remove(neurons.remove(neuron));
        }
    }

    public double getScoreForNeuron(NeuralLearner neuron){
        return neurons.get(neuron);
    }
}
