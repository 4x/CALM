package ai.context.learning.neural;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class NeuronRankings {
    private static volatile NeuronRankings instance = null;

    private NeuronRankings() {
    }

    public static NeuronRankings getInstance() {
        if (instance == null) {
            synchronized (NeuronRankings.class) {
                if (instance == null) {
                    instance = new NeuronRankings();
                }
            }
        }
        return instance;
    }

    public TreeMap<Double, String> bestInputsAfterCalibration = new TreeMap<>();
    private Map<NeuralLearner, Double> neurons = new HashMap<>();
    private Map<Double, NeuralLearner> rankings = new ConcurrentSkipListMap<>();
    private int maxSeeds = 20;

    public void update(NeuralLearner updater, Double score) {
        Map.Entry toRemove = null;
        for (Map.Entry<Double, NeuralLearner> entry : rankings.entrySet()) {
            if (entry.getValue() == updater) {
                toRemove = entry;
            }
        }

        rankings.entrySet().remove(toRemove);
        rankings.put(score, updater);
        neurons.put(updater, score);
    }

    public Map<Double, NeuralLearner> getRankings() {
        return rankings;
    }

    public void remove(NeuralLearner neuron) {
        rankings.remove(neurons.remove(neuron));
    }

    public double getScoreForNeuron(NeuralLearner neuron) {
        return neurons.get(neuron);
    }

    public void updateCalibrationInputs(){
        for(Map.Entry<NeuralLearner, Double> entry : neurons.entrySet()){
            String inputs = "";
            for(int input : entry.getKey().getFlowData()[1]){
                inputs += input + ",";
            }
            if(inputs.endsWith(",")){
                inputs = inputs.substring(0, inputs.length() - 1);
            }

            boolean found = false;
            for(String inputString : bestInputsAfterCalibration.values()){
                if(inputString.equals(inputs)){
                    found = true;
                    break;
                }
            }

            if(!found){
                bestInputsAfterCalibration.put(entry.getValue(), inputs);
            }
        }

        if(bestInputsAfterCalibration.size() > maxSeeds){
            Set<Double> toRemove = new HashSet<>();
            int nToRemove = bestInputsAfterCalibration.size() - maxSeeds;
            for(Double key : bestInputsAfterCalibration.keySet()){
                toRemove.add(key);
                nToRemove--;
                if(nToRemove == 0){
                    break;
                }
            }
            bestInputsAfterCalibration.keySet().removeAll(toRemove);
        }

        List<Integer[]> seeds = NeuronCluster.getInstance().seedFeeds;
        seeds.clear();
        int toTake = bestInputsAfterCalibration.size()/2;
        for(String inputs : bestInputsAfterCalibration.descendingMap().values()){

            String[] inputStringArr = inputs.split(",");
            Integer[] inputArr = new Integer[inputStringArr.length];
            for(int i = 0; i < inputArr.length; i++){
                inputArr[i] = Integer.parseInt(inputStringArr[i]);
            }

            seeds.add(inputArr);
            toTake--;
            if(toTake == 0){
                break;
            }
        }

        System.out.println("Seeds: " + bestInputsAfterCalibration);
    }

    public double getOverallMarking() {
        double score = 0;
        int nNeurons = 0;
        for (double subScore : neurons.values()) {
            if (subScore >= 0) {
                nNeurons++;
                score += subScore;
            }
        }
        return score / nNeurons;
    }

    public void reset() {
        neurons.clear();
        rankings.clear();
    }
}
