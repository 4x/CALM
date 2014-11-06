package ai.context.util.score;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class NeuronScoreKeeper {

    public static TreeMap<Double, HashMap<Integer, Double>> scores = new TreeMap<>();

    static {
        scores.put(0.5, new HashMap<Integer, Double>());
        scores.put(0.25, new HashMap<Integer, Double>());
        scores.put(0.125, new HashMap<Integer, Double>());
        scores.put(0.0625, new HashMap<Integer, Double>());
        scores.put(0.03125, new HashMap<Integer, Double>());
        scores.put(0.015625, new HashMap<Integer, Double>());
        scores.put(0.0078125, new HashMap<Integer, Double>());
        scores.put(0.00390625, new HashMap<Integer, Double>());
        scores.put(0.001953125, new HashMap<Integer, Double>());
    }

    public static double getWeightFor(int neuronId) {
        double weight = 0.1;
        int div = 1;
        for(HashMap<Integer, Double> scoreMap : scores.values()){
            if(scoreMap.containsKey(neuronId)){
                weight += scoreMap.get(neuronId);
                div++;
            }
        }
        return weight/div;
    }

    public static String getInfoFor(int neuronId) {
        String info = neuronId + ": ";
        for(Map.Entry<Double, HashMap<Integer, Double>> entry : scores.entrySet()){
            if(entry.getValue().containsKey(neuronId)){
                info += "\n\t" + entry.getKey() + " -> " + entry.getValue().get(neuronId);
            }
        }
        return info;
    }

    public static void scoreNeurons(double ampD, double ampU, HashMap<Integer, Double[]> opinions){
        for(Map.Entry<Integer, Double[]> opinionEntry : opinions.entrySet()){

            Double[] opinion = opinionEntry.getValue();
            double weight = Math.exp(-Math.sqrt(Math.pow((opinion[0] - ampU) / ampU, 2) + Math.pow((opinion[1] - ampD) / ampD, 2)));
            int neuronId = opinionEntry.getKey();

            for(Map.Entry<Double, HashMap<Integer, Double>> scoreEntry : scores.entrySet()){
                double lambda = scoreEntry.getKey();
                HashMap<Integer, Double> scoreMap = scoreEntry.getValue();

                if(!scoreMap.containsKey(neuronId)){
                    scoreMap.put(neuronId, weight);
                }
                else {
                    scoreMap.put(neuronId, lambda * weight + (1 - lambda) * scoreMap.get(neuronId));
                }
            }
        }
    }
}
