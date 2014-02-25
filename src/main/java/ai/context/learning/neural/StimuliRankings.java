package ai.context.learning.neural;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class StimuliRankings {
    private static volatile StimuliRankings instance = null;
    private StimuliRankings(){}

    public static StimuliRankings getInstance() {
        if (instance == null) {
            synchronized (StimuliRankings.class){
                if (instance == null) {
                    instance = new StimuliRankings ();
                }
            }
        }
        return instance;
    }

    private Map<Double, Integer> rankings;
    private ConcurrentHashMap<Object,HashMap<Integer, Double>> scores = new ConcurrentHashMap();
    public void update(Object updater, HashMap<Integer, Double> stimuliScores){
        rankings = null;
        scores.put(updater, stimuliScores);
    }

    public synchronized Map<Double, Integer> getRankings(){
        if(rankings != null){
            return rankings;
        }
        Map<Integer, Double> tmpScores = new HashMap<>();
        for(HashMap<Integer, Double> subScores : scores.values()){
            for(Map.Entry<Integer, Double> entry : subScores.entrySet()){
                if(!tmpScores.containsKey(entry.getKey())){
                    tmpScores.put(entry.getKey(), 0.0);
                }
                tmpScores.put(entry.getKey(), tmpScores.get(entry.getKey()) + entry.getValue());
            }
        }

        TreeMap<Double, Integer> sorted = new TreeMap<>();
        for(Map.Entry<Integer, Double> entry : tmpScores.entrySet()){
            sorted.put(entry.getValue(), entry.getKey());
        }

        return rankings = sorted.descendingMap();
    }
}
