package ai.context.core.ai;

import ai.context.util.learning.AmalgamateUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

public class StateActionPair {

    private final String id;
    private final int[] amalgamate;
    private double actionResolution;
    private TreeMap<Integer, Double> actionDistribution = new TreeMap<Integer, Double>();
    private HashSet<StateActionPair> closestNeighbours = new HashSet<>();

    private double totalWeight = 0.0;

    public StateActionPair(String id, int[] amalgamate, double actionResolution, TreeMap<Integer, Double> actionDistribution, double totalWeight) {
        this.id = id;
        this.amalgamate = amalgamate;
        this.actionResolution = actionResolution;
        this.actionDistribution = actionDistribution;
        this.totalWeight = totalWeight;
    }

    public StateActionPair(String id, int[] amalgamate, double actionResolution) {
        this.id = id;
        this.amalgamate = amalgamate;
        this.actionResolution = actionResolution;
    }

    public void newMovement(double movement, double weight) {
        int actionClass = (int) (movement / actionResolution);
        populate(actionClass, weight);
    }

    public void populate(int actionClass, double weight) {
        if (!actionDistribution.containsKey(actionClass)) {
            actionDistribution.put(actionClass, 0.0);
        }
        double currentWeight = actionDistribution.get(actionClass);
        actionDistribution.put(actionClass, weight + currentWeight);
        totalWeight += weight;
    }

    public String getId() {
        return id;
    }

    public int[] getAmalgamate() {
        return amalgamate;
    }

    public double getActionResolution() {
        return actionResolution;
    }

    public TreeMap<Integer, Double> getRawActionDistribution() {

        return actionDistribution;
    }

    public TreeMap<Integer, Double> getActionDistribution() {

        TreeMap<Integer, Double> distribution = new TreeMap<Integer, Double>();
        for (Map.Entry<Integer, Double> entry : actionDistribution.entrySet()) {
            distribution.put(entry.getKey(), entry.getValue() / totalWeight);
        }

        return distribution;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public StateActionPair merge(StateActionPair counterpart) {
        int[] mergedAmalgamate = new int[amalgamate.length];
        double counterpartWeight = counterpart.getTotalWeight();
        double netWeight = totalWeight + counterpartWeight;

        for (int i = 0; i < amalgamate.length; i++) {
            mergedAmalgamate[i] = (int) (((amalgamate[i] * totalWeight) + (counterpart.getAmalgamate()[i] * counterpartWeight)) / netWeight);
        }

        StateActionPair merged = new StateActionPair(AmalgamateUtils.getAmalgamateString(mergedAmalgamate), mergedAmalgamate, actionResolution);

        for (Map.Entry<Integer, Double> entry : actionDistribution.entrySet()) {
            merged.populate(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Integer, Double> entry : counterpart.getRawActionDistribution().entrySet()) {
            merged.populate(entry.getKey(), entry.getValue());
        }

        return merged;
    }

    public double getDeviation(int[] counterpart, double[] correlationWeights) {
        double deviation = 0;

        for (int i = 0; i < amalgamate.length || i < counterpart.length; i++) {
            if (correlationWeights[i] >= 0) {
                deviation += (Math.abs(amalgamate[i] - counterpart[i]) * correlationWeights[i]);
            }
        }
        return deviation;
    }

    public HashSet<StateActionPair> getClosestNeighbours() {
        return closestNeighbours;
    }

    public void setClosestNeighbours(HashSet<StateActionPair> closestNeighbours) {
        this.closestNeighbours = closestNeighbours;
    }

    @Override
    public String toString() {

        String data = "";
        for (Map.Entry<Integer, Double> entry : actionDistribution.entrySet()) {
            data += entry.getKey() + ":" + entry.getValue() + ";";
        }

        return "id=" + System.identityHashCode(this) +
                ", amalgamate=" + AmalgamateUtils.getArrayString(amalgamate) +
                ", actionResolution=" + actionResolution +
                ", actionDistribution=" + data +
                ", totalWeight=" + totalWeight;
    }
}
