package ai.context.core.ai;

import ai.context.util.common.Count;
import ai.context.util.learning.AmalgamateUtils;

import java.util.Map;
import java.util.TreeMap;

public class StateActionPair {

    private final String id;
    private final int[] amalgamate;
    private double actionResolution;
    private TreeMap<Integer, Count> actionDistribution = new TreeMap<>();
    private TreeMap<Double, StateActionPair> closestNeighbours = new TreeMap<>();

    private double totalWeight = 0.0;

    public StateActionPair(String id, int[] amalgamate, double actionResolution, TreeMap<Integer, Count> actionDistribution, double totalWeight) {
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
        int actionClass = (int) Math.round(movement / actionResolution);
        populate(actionClass, weight);
    }

    public void populate(int actionClass, double weight) {
        if (!actionDistribution.containsKey(actionClass)) {
            actionDistribution.put(actionClass, new Count());
        }
        actionDistribution.get(actionClass).val += weight;
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

    public TreeMap<Integer, Count> getRawActionDistribution() {

        return actionDistribution;
    }

    public TreeMap<Integer, Double> getActionDistribution() {

        TreeMap<Integer, Double> distribution = new TreeMap<>();
        for (Map.Entry<Integer, Count> entry : actionDistribution.entrySet()) {
            distribution.put(entry.getKey(), entry.getValue().val / totalWeight);
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
            mergedAmalgamate[i] = (int) Math.round(((amalgamate[i] * totalWeight) + (counterpart.getAmalgamate()[i] * counterpartWeight)) / netWeight);
        }

        StateActionPair merged = new StateActionPair(AmalgamateUtils.getAmalgamateString(mergedAmalgamate), mergedAmalgamate, actionResolution);

        for (Map.Entry<Integer, Count> entry : actionDistribution.entrySet()) {
            merged.populate(entry.getKey(), entry.getValue().val);
        }

        for (Map.Entry<Integer, Count> entry : counterpart.getRawActionDistribution().entrySet()) {
            merged.populate(entry.getKey(), entry.getValue().val);
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

    public TreeMap<Double, StateActionPair> getClosestNeighbours() {
        return closestNeighbours;
    }

    public void setClosestNeighbours(TreeMap<Double, StateActionPair> closestNeighbours) {
        this.closestNeighbours = closestNeighbours;
    }

    @Override
    public String toString() {

        String data = "";
        for (Map.Entry<Integer, Count> entry : actionDistribution.entrySet()) {
            data += entry.getKey() + ":" + entry.getValue().val + ";";
        }

        return "id=" + System.identityHashCode(this) +
                ", amalgamate=" + AmalgamateUtils.getArrayString(amalgamate) +
                ", actionResolution=" + actionResolution +
                ", actionDistribution=" + data +
                ", totalWeight=" + totalWeight;
    }
}
