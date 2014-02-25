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

    private Map<Double, NeuralLearner> rankings = new TreeMap<>();
    private ConcurrentHashMap<Object,HashMap<Integer, Double>> scores = new ConcurrentHashMap();
    public void update(NeuralLearner updater, Double score){
        Map.Entry toRemove = null;
        for(Map.Entry<Double, NeuralLearner> entry : rankings.entrySet()){
            if(entry.getValue() == updater){
                toRemove = entry;
            }
        }
        rankings.entrySet().remove(toRemove);
        rankings.put(score, updater);
    }

    public synchronized Map<Double, NeuralLearner> getRankings(){
        return rankings;
    }
}
