package ai.context.core.ai;

import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.learning.AmalgamateUtils;
import ai.context.util.learning.ClusteredCopulae;
import ai.context.util.mathematics.CorrelationCalculator;
import ai.context.util.mathematics.Operations;

import java.util.*;
import java.util.concurrent.*;

import static ai.context.util.mathematics.Operations.sum;

public class LearnerService {

    private ConcurrentHashMap<String, StateActionPair> population = new ConcurrentHashMap<String, StateActionPair>();
    private ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>>> indices = new ConcurrentSkipListMap<>();

    private double[] correlationWeights;

    private TreeMap<Integer, CorrelationCalculator> correlationCalculators = new TreeMap<Integer, CorrelationCalculator>();
    private double[] correlations;

    private ClusteredCopulae copulae = new ClusteredCopulae();
    private ClusteredCopulae magnitudeCopulae = new ClusteredCopulae();

    private TreeMap<Integer, Long> distribution = new TreeMap<Integer, Long>();

    private boolean doubleCheckMerge = true;
    private double actionResolution = 1.0;

    private boolean merging = false;
    private double minDev = -1;
    private double maxDev = -1;
    private double minDevForMerge = -1;

    private LearnerService thisService = this;

    private boolean correlating = false;

    private ExecutorService mergeExecutor = Executors.newSingleThreadExecutor();

    private Runnable batchMergeTask = new Runnable() {
        @Override
        public void run() {
            if (!merging) {
                try {
                    thisService.mergeStates();
                } catch (LearningException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public boolean isMerging() {
        return merging;
    }

    private class MergeTask implements Runnable {

        private StateActionPair sap1;
        private StateActionPair sap2;

        private MergeTask(StateActionPair sap1, StateActionPair sap2) {
            this.sap1 = sap1;
            this.sap2 = sap2;
        }

        @Override
        public void run() {
            thisService.merge(sap1, sap2);
        }
    }

    ;

    public LearnerService() {
    }

    public LearnerService(ConcurrentHashMap<String, StateActionPair> population, ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>>> indices, double[] correlationWeights, TreeMap<Integer, CorrelationCalculator> correlationCalculators, double[] correlations, ClusteredCopulae copulae, double actionResolution, int maxPopulation, double tolerance, double copulaToUniversal, double minDev, double maxDev, double minDevForMerge, TreeMap<Integer, Long> distribution) {
        this.population = population;
        this.indices = indices;
        this.correlationWeights = correlationWeights;
        this.correlationCalculators = correlationCalculators;
        this.correlations = correlations;
        this.copulae = copulae;
        PropertiesHolder.maxPopulation = maxPopulation;
        PropertiesHolder.tolerance = tolerance;
        PropertiesHolder.copulaToUniversal = copulaToUniversal;
        this.distribution = distribution;
        this.maxDev = maxDev;
        this.minDev = minDev;
        this.minDevForMerge = minDevForMerge;
    }


    public double[] getCorrelations() {
        return correlations;
    }

    public int getMaxPopulation() {
        return PropertiesHolder.maxPopulation;
    }

    public void setMaxPopulation(int maxPopulation) {
        PropertiesHolder.maxPopulation = maxPopulation;
    }

    public double getActionResolution() {
        return actionResolution;
    }

    public void setActionResolution(double actionResolution) {
        this.actionResolution = actionResolution;
    }

    public double getTolerance() {
        return PropertiesHolder.tolerance;
    }

    public void setTolerance(double tolerance) {
        PropertiesHolder.tolerance = tolerance;
    }

    public TreeMap<Integer, Long> getDistribution() {
        return distribution;
    }

    public double getCopulaToUniversal() {
        return PropertiesHolder.copulaToUniversal;
    }

    public void setCopulaToUniversal(double copulaToUniversal) {
        PropertiesHolder.copulaToUniversal = copulaToUniversal;
    }

    public synchronized void addStateAction(int[] state, double movement) throws LearningException {

        int actionClass = (int) Operations.round(movement / actionResolution, 0);
        if (!distribution.containsKey(actionClass)) {
            distribution.put(actionClass, 0L);
        }

        distribution.put(actionClass, distribution.get(actionClass) + 1);

        refreshCorrelations(state, movement);

        if (!correlating) {
            boolean newState = false;

            String id = AmalgamateUtils.getAmalgamateString(state);

            if (!population.containsKey(id)) {
                newState = true;
                StateActionPair stateActionPair = new StateActionPair(id, state, actionResolution);
                addState(stateActionPair);
            }

            StateActionPair counterpart = null;
            double currentDev = -1;

            Set<Map.Entry<StateActionPair, Double>> entries = getSimilarStates(state).entrySet();
            for (Map.Entry<StateActionPair, Double> entry : entries) {
                double weight = getWeightForDeviation(entry.getValue());
                entry.getKey().newMovement(movement, weight);

                if (currentDev == -1 && !entry.getKey().getId().equals(id)) {
                    currentDev = entry.getValue();
                }
                if (entry.getValue() <= currentDev && entry.getValue() < minDevForMerge && !entry.getKey().getId().equals(id)) {
                    counterpart = entry.getKey();
                    currentDev = entry.getValue();
                }
            }

            if (counterpart != null) {
                if (doubleCheckMerge) {
                    updateAndGetCorrelationWeights(counterpart.getAmalgamate());
                    currentDev += getDeviation(counterpart.getAmalgamate(), population.get(id));
                    currentDev /= 2;

                    if (currentDev < minDevForMerge) {
                        merge(population.get(id), counterpart);
                    }
                } else {
                    merge(population.get(id), counterpart);
                }
            }

            if (newState && population.size() > PropertiesHolder.maxPopulation) {
                mergeStates();
            }
        }
    }

    public synchronized void refreshCorrelations(int[] state, double movement) {
        copulae.addObservation(state, movement);
        magnitudeCopulae.addObservation(state, Math.abs(movement));

        correlations = new double[state.length];

        for (int index = 0; index < state.length; index++) {
            if (!correlationCalculators.containsKey(index)) {
                correlationCalculators.put(index, new CorrelationCalculator());
            }
            CorrelationCalculator calculator = correlationCalculators.get(index);
            correlations[index] = calculator.getCorrelationCoefficient(state[index], movement);
        }
    }

    public double[] updateAndGetCorrelationWeights(int[] state) {
        correlationWeights = copulae.getCorrelationWeights(state);
        double[] magCorrelations = magnitudeCopulae.getCorrelationWeights(state);
        for (int i = 0; i < correlationWeights.length; i++) {
            double correlation = magCorrelations[i];
            if (correlation >= 0 && !Double.isInfinite(correlation)) {
                //correlationWeights[i] = (0.75 * correlationWeights[i] + 0.25 * correlation);
                correlationWeights[i] = Math.sqrt(correlationWeights[i] * correlation);
            }
        }
        return correlationWeights;
    }

    private Map<StateActionPair, Double> getSimilarStates(int[] state) {
        updateAndGetCorrelationWeights(state);

        TreeMap<Double, StateActionPair> top = new TreeMap<Double, StateActionPair>();
        for (StateActionPair pair : population.values()) {
            double deviation = getDeviation(state, pair);
            top.put(deviation, pair);
        }

        if (doubleCheckMerge) {
            TreeMap<Double, StateActionPair> top2 = new TreeMap<Double, StateActionPair>();
            for (Map.Entry<Double, StateActionPair> pair : top.entrySet()) {
                updateAndGetCorrelationWeights(pair.getValue().getAmalgamate());
                double deviation = (getDeviation(pair.getValue().getAmalgamate(), state, correlationWeights) + pair.getKey()) / 2;
                top2.put(deviation, pair.getValue());
            }
            top = top2;
        }

        HashMap<StateActionPair, Double> map = new HashMap<StateActionPair, Double>();
        HashSet<StateActionPair> closestNeighbours = new HashSet<>();

        int i = 0;
        for (Map.Entry<Double, StateActionPair> pair : top.entrySet()) {
            map.put(pair.getValue(), pair.getKey());
            pair.getValue().setClosestNeighbours(closestNeighbours);
            closestNeighbours.add(pair.getValue());

            if ((minDev == -1 || minDev > pair.getKey()) && pair.getKey() > 0) {
                minDev = pair.getKey();
            }

            if ((maxDev == -1 || maxDev < pair.getKey()) && pair.getKey() > 0) {
                maxDev = pair.getKey();
            }

            i++;
            if (i >= population.size() * PropertiesHolder.tolerance && i >= 2) {
                break;
            }
        }
        return map;
    }

    private double getDeviation(int[] state, StateActionPair pair) {
        return pair.getDeviation(state, correlationWeights) /*/ Math.pow(Math.log(Math.E + pair.getTotalWeight()), 1 / PropertiesHolder.recencyBias)*/;
    }

    private synchronized double getWeightForDeviation(double deviation) {
        if (deviation == 0) {
            return 1;
        }

        double x = Math.log(deviation);
        double min = Math.log(minDev);
        double max = Math.log(maxDev);

        if (max == min) {
            return 0;
        }
        return Math.pow((max - x) / (max - min), 2);
    }

    private synchronized void mergeStates() throws LearningException {
        merging = true;
        if (minDevForMerge == -1) {
            minDevForMerge = 4 * (minDev + maxDev) / getMaxPopulation();
        } else if (minDevForMerge < 0) {
            throw new LearningException("Negative Deviation For Merge");
        }

        long t = System.currentTimeMillis();
        System.err.println("Starting merge process (" + minDevForMerge + ")");

        boolean targetReached = false;

        int runs = 0;
        while (true) {
            runs++;
            HashSet<StateActionPair> pairs = new HashSet<>();
            pairs.addAll(population.values());
            for (StateActionPair pair : population.values()) {
                getSimilarStates(pair.getAmalgamate());
            }

            int x = 0;
            int y = 0;
            int z = 0;
            int a = 0;
            HashSet<StateActionPair> check = new HashSet<>();
            for (StateActionPair pair : pairs) {
                if (population.values().contains(pair)) {
                    x++;
                    for (StateActionPair counterpart : pair.getClosestNeighbours()) {
                        check.addAll(pair.getClosestNeighbours());
                        y++;
                        if (pair != counterpart && population.values().contains(counterpart)) {
                            updateAndGetCorrelationWeights(pair.getAmalgamate());
                            double deviation = getDeviation(pair.getAmalgamate(), counterpart);
                            if (doubleCheckMerge) {
                                updateAndGetCorrelationWeights(counterpart.getAmalgamate());
                                deviation += getDeviation(counterpart.getAmalgamate(), pair);
                                deviation /= 2;
                            }
                            z++;
                            if (deviation <= minDevForMerge) {
                                merge(pair, counterpart);
                                a++;
                                if (population.size() < getMaxPopulation() / 2) {
                                    targetReached = true;
                                }
                                break;
                            }
                        }
                    }
                    if (targetReached) {
                        break;
                    }
                }
            }

            System.err.println("Run: " + runs + " x: " + x + " y: " + y + " z: " + z + " Check: " + check.size() + " a: " + a);
            if (!targetReached && (runs % 4 == 0 || a < (double) population.size() / 20)) {
                minDevForMerge = minDevForMerge * ((double) population.size() / (getMaxPopulation() / 2)) * 1.01;
                System.err.println("MinDevForMerge pushed to: " + minDevForMerge);
            } else if (targetReached) {
                break;
            }
        }
        System.err.println("Ending merge: " + (System.currentTimeMillis() - t) + "ms  (" + minDevForMerge + ")");
        merging = false;
    }

    private void merge(StateActionPair sap1, StateActionPair sap2) {

        if (population.containsKey(sap1.getId()) && population.containsKey(sap2.getId())) {
            StateActionPair newState = sap1.merge(sap2);
            removeState(sap1);
            removeState(sap2);

            addState(newState);
        }
    }

    private synchronized void removeState(StateActionPair pair) {
        if (population.containsKey(pair.getId())) {
            population.remove(pair.getId());

            /*for(int index = 0; index < pair.getAmalgamate().length; index++)
            {
                int signalValue = pair.getAmalgamate()[index];
                indices.get(index).get(signalValue).remove(pair);
            }*/
        }
    }

    private synchronized void addState(StateActionPair pair) {
        if (!population.containsKey(pair.getId())) {
            population.put(pair.getId(), pair);
            /*int[] state = pair.getAmalgamate();
            for(int index = 0; index < state.length; index++)
            {
                int signalValue = state[index];
                ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>> subIndex = indices.get(index);
                if(!indices.containsKey(index))
                {
                    subIndex = new ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>>();
                    indices.put(index, subIndex);
                }
                if(!subIndex.containsKey(signalValue))
                {
                    subIndex.put(signalValue, new CopyOnWriteArraySet<StateActionPair>());
                }
                subIndex.get(signalValue).add(pair);
            }*/
        }
    }

    public TreeMap<Integer, Double> getActionDistribution(int[] state) {
        TreeMap<Integer, Double> distribution = new TreeMap<Integer, Double>();
        for (Map.Entry<StateActionPair, Double> entry : getSimilarStates(state).entrySet()) {
            double weight = getWeightForDeviation(entry.getValue());
            StateActionPair pair = entry.getKey();

            for (Map.Entry<Integer, Double> distEntry : pair.getActionDistribution().entrySet()) {
                double value = 0;
                if (distribution.containsKey(distEntry.getKey())) {
                    value = distribution.get(distEntry.getKey());
                }
                distribution.put(distEntry.getKey(), value + (weight * distEntry.getValue()));
            }
        }
        return distribution;
    }

    public Map<Integer, Double> getCorrelationMap() {
        TreeMap<Integer, Double> correlations = new TreeMap<Integer, Double>();
        int index = 0;
        for (double weight : correlationWeights) {
            correlations.put(index, weight);
            index++;
        }

        return correlations;
    }

    public TreeMap<Double, StateActionPair> getAlphaStates() {
        TreeMap<Double, StateActionPair> alpha = new TreeMap<>();
        for (StateActionPair pair : population.values()) {
            double score = sum(updateAndGetCorrelationWeights(pair.getAmalgamate()));
            alpha.put(score, pair);
        }
        return alpha;
    }

    public double[] getCorrelationWeightsForState(StateActionPair sap) {
        return copulae.getCorrelationWeights(sap.getAmalgamate());
    }

    public ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>> getIndexForVariable(int variable) {
        return indices.get(variable);
    }

    public ConcurrentHashMap<String, StateActionPair> getPopulation() {
        return population;
    }

    public ClusteredCopulae getCopulae() {
        return copulae;
    }

    public TreeMap<Integer, CorrelationCalculator> getCorrelationCalculators() {
        return correlationCalculators;
    }

    public void setCorrelationCalculators(TreeMap<Integer, CorrelationCalculator> correlationCalculators) {
        this.correlationCalculators = correlationCalculators;
    }

    public void setCopulae(ClusteredCopulae copulae) {
        this.copulae = copulae;
    }

    public StateActionPair getStateActionPair(String id) {
        return population.get(id);
    }

    public void setCorrelating(boolean correlating) {
        this.correlating = correlating;
    }

    public double getDeviation(int[] state, int[] counterpart, double[] correlationWeights) {
        double deviation = 0;

        for (int i = 0; i < state.length || i < counterpart.length; i++) {
            if (correlationWeights[i] >= 0) {
                deviation += (Math.abs(state[i] - counterpart[i]) * correlationWeights[i]);
            }
        }
        return deviation;
    }

    @Override
    public String toString() {
        return "population=" + AmalgamateUtils.getMapString(population) +
                ", indices=" + AmalgamateUtils.getMapString(indices) +
                ", correlationWeights=" + AmalgamateUtils.getArrayString(correlationWeights) +
                ", correlationCalculators=" + AmalgamateUtils.getMapString(correlationCalculators) +
                ", correlations=" + AmalgamateUtils.getArrayString(correlations) +
                ", copulae=" + System.identityHashCode(copulae) +
                ", distribution=" + AmalgamateUtils.getMapString(distribution) +
                ", actionResolution=" + actionResolution +
                ", maxPopulation=" + PropertiesHolder.maxPopulation +
                ", tolerance=" + PropertiesHolder.tolerance +
                ", copulaToUniversal=" + PropertiesHolder.copulaToUniversal +
                ", minDev=" + minDev +
                ", maxDev=" + maxDev +
                ", minDevForMerge=" + minDevForMerge;
    }
}
