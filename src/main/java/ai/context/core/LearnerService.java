package ai.context.core;

import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.learning.AmalgamateUtils;
import ai.context.util.learning.ClusteredCopulae;
import ai.context.util.mathematics.CorrelationCalculator;

import java.util.*;
import java.util.concurrent.*;

import static ai.context.util.mathematics.Operations.sum;

public class LearnerService {

    private ConcurrentHashMap<String, StateActionPair> population = new ConcurrentHashMap<String, StateActionPair>();
    private ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>>> indices = new ConcurrentSkipListMap<>();

    private double [] correlationWeights;

    private TreeMap<Integer, CorrelationCalculator> correlationCalculators = new TreeMap<Integer, CorrelationCalculator>();
    private double[] correlations;

    private ClusteredCopulae copulae = new ClusteredCopulae();

    private TreeMap<Integer, Long> deviationDistributions = new TreeMap<Integer, Long>();
    private long deviationDistributionSize = 0;

    private TreeMap<Integer, Long> distribution = new TreeMap<Integer, Long>();

    private double actionResolution = 1.0;

    private double minDevForMerge = 0.0;
    private boolean merging = false;

    private LearnerService thisService = this;

    private ExecutorService mergeExecutor = Executors.newCachedThreadPool();

    private Runnable batchMergeTask = new Runnable() {
        @Override
        public void run() {
            if(!merging){
                thisService.mergeStates();
            }
        }
    };

    public boolean isMerging() {
        return merging;
    }

    private class MergeTask implements Runnable{

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
    };

    public LearnerService() {
    }

    public LearnerService(ConcurrentHashMap<String, StateActionPair> population, ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>>> indices, double[] correlationWeights, TreeMap<Integer, CorrelationCalculator> correlationCalculators, double[] correlations, ClusteredCopulae copulae, TreeMap<Integer, Long> deviationDistributions, long deviationDistributionSize, double actionResolution, int maxPopulation, double tolerance, double copulaToUniversal, double minDevForMerge, TreeMap<Integer, Long> distribution) {
        this.population = population;
        this.indices = indices;
        this.correlationWeights = correlationWeights;
        this.correlationCalculators = correlationCalculators;
        this.correlations = correlations;
        this.copulae = copulae;
        this.deviationDistributions = deviationDistributions;
        this.deviationDistributionSize = deviationDistributionSize;
        this.actionResolution = actionResolution;
        PropertiesHolder.maxPopulation = maxPopulation;
        PropertiesHolder.tolerance = tolerance;
        PropertiesHolder.copulaToUniversal = copulaToUniversal;
        this.minDevForMerge = minDevForMerge;
        this.distribution = distribution;
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

    public synchronized void addStateAction(int[] state, double movement){

        int actionClass = (int) (movement / actionResolution);
        if(!distribution.containsKey(actionClass))
        {
            distribution.put(actionClass, 0L);
        }

        distribution.put(actionClass, distribution.get(actionClass) + 1);

        refreshCorrelations(state, movement);
        boolean newState = false;

        String id = AmalgamateUtils.getAmalgamateString(state);

        if(!population.containsKey(id))
        {
            newState = true;
            StateActionPair stateActionPair = new StateActionPair(id, state, actionResolution);
            addState(stateActionPair);
        }

        Set<Map.Entry<StateActionPair, Double>> entries = getSimilarStates(state).entrySet();
        for(Map.Entry<StateActionPair, Double> entry : entries)
        {
            double weight = getWeightForDeviation(entry.getValue());
            entry.getKey().newMovement(movement, weight);
        }

        if(newState && population.size() > PropertiesHolder.maxPopulation)
        {
            mergeExecutor.execute(batchMergeTask);
            //mergeStates();
        }

    }

    private synchronized void refreshCorrelations(int[] state, double movement)
    {
        copulae.addObservation(state, movement);

        correlations = new double[state.length];

        for (int index = 0; index < state.length; index++)
        {
            if(!correlationCalculators.containsKey(index))
            {
                correlationCalculators.put(index, new CorrelationCalculator());
            }
            CorrelationCalculator calculator = correlationCalculators.get(index);
            correlations[index] = calculator.getCorrelationCoefficient(state[index], movement);
        }

        double[] weights = copulae.getCorrelationWeights(state);
        for (int i = 0; i < weights.length; i++)
        {
            weights[i] += Math.abs((correlations[i])/PropertiesHolder.copulaToUniversal);
        }
    }

    public double[] updateAndGetCorrelationWeights(int[] state){
        correlationWeights = copulae.getCorrelationWeights(state);
        if(correlations != null)
        {
            for (int i = 0; i < correlationWeights.length; i++)
            {
                double correlation = correlations[i];
                if(correlation >= 0 && !Double.isInfinite(correlation))
                {
                    correlationWeights[i] += Math.abs((correlations[i])/PropertiesHolder.copulaToUniversal);
                }
            }
        }
        return correlationWeights;
    }

    private Map<StateActionPair, Double> getSimilarStates(int[] state)
    {
        updateAndGetCorrelationWeights(state);

        /*HashSet<StateActionPair> set = new HashSet<StateActionPair>();
        TreeMap<Double, Integer> priorityOrderedSignals = new TreeMap<Double, Integer>();
        int signalIndex = 0;

        for(double weight : correlationWeights)
        {
            priorityOrderedSignals.put(weight, signalIndex);
            signalIndex++;
        }

        for(Map.Entry<Double, Integer> entry : priorityOrderedSignals.descendingMap().entrySet())
        {
            int index = entry.getValue();
            if(indices.containsKey(index))
            {
                int positionInTree = state[index];

                TreeMap<Integer, CopyOnWriteArraySet<StateActionPair>> head = new TreeMap<Integer, CopyOnWriteArraySet<StateActionPair>>();
                head.putAll(indices.get(index).headMap(positionInTree));
                TreeMap<Integer, CopyOnWriteArraySet<StateActionPair>> tail = new TreeMap<Integer, CopyOnWriteArraySet<StateActionPair>>();
                tail.putAll(indices.get(index).tailMap(positionInTree));

                for(int position : head.descendingKeySet())
                {
                    set.addAll(head.get(position));
                    if(set.size() > PropertiesHolder.maxPopulation*PropertiesHolder.tolerance*index/state.length)
                    {
                        break;
                    }
                }
                for(int position : tail.keySet())
                {
                    set.addAll(tail.get(position));
                    if(set.size() > 2*PropertiesHolder.maxPopulation*PropertiesHolder.tolerance*index/state.length)
                    {
                        break;
                    }
                }
            }
        }*/

        TreeMap<Double, StateActionPair> top = new TreeMap<Double, StateActionPair>();
        for(StateActionPair pair : population.values())
        {
            double deviation = getDeviation(state, pair);
            top.put(deviation, pair);
            populateDeviationPopulation(deviation);
        }

        HashMap<StateActionPair, Double> map = new HashMap<StateActionPair, Double>();
        int i = 0;
        for(Map.Entry<Double,StateActionPair> pair : top.entrySet()){
            map.put(pair.getValue(), pair.getKey());

            i++;
            if(i >= population.size()*PropertiesHolder.tolerance && i >= 2){
                break;
            }
        }
        return map;
    }

    private double getDeviation(int[] state, StateActionPair pair){
        return pair.getDeviation(state, correlationWeights) / Math.pow(Math.log(Math.E + pair.getTotalWeight()), 1 / PropertiesHolder.recencyBias);
    }

    private synchronized void populateDeviationPopulation(double deviation)
    {
        int deviationClass = (int) (deviation * 100);
        if(!deviationDistributions.containsKey(deviationClass)){
            deviationDistributions.put(deviationClass, 0L);
        }
        deviationDistributions.put(deviationClass, deviationDistributions.get(deviationClass) + 1);
        deviationDistributionSize++;
    }

    private synchronized double getWeightForDeviation(double deviation){
        int deviationClass = (int) (deviation * 100);
        int nBetter = 0;

        for(Map.Entry<Integer, Long> entry : deviationDistributions.entrySet())
        {
            if(deviationClass <= entry.getKey())
            {
                break;
            }
            nBetter += entry.getValue();
        }
        double weight = 1.0 - ((double) nBetter / (double) deviationDistributionSize);
        weight = Math.pow(weight * Math.log(weight * (Math.E - 1) + 1), 2);
        return weight;
    }

    private synchronized void mergeStates()
    {
        merging = true;
        double minDevForMerge = 2 * getMinDevForMerge();
        long t = System.currentTimeMillis();
        System.err.println("Starting merge process (" + minDevForMerge + ")");

        while (population.size() > PropertiesHolder.maxPopulation/2)
        {
            boolean merged = false;
            for(StateActionPair pair : population.values())
            {
                merged = false;
                updateAndGetCorrelationWeights(pair.getAmalgamate());

                for(StateActionPair counterpart : population.values()){
                    if(getDeviation(pair.getAmalgamate(), counterpart) < minDevForMerge && !pair.getId().equals(counterpart.getId()))
                    {
                        merge(pair, counterpart);
                        merged = true;
                        break;
                    }
                }
                if(merged)
                {
                    break;
                }
            }
            if(!merged)
            {
                this.minDevForMerge = minDevForMerge;
                minDevForMerge = getMinDevForMerge();
            }
        }

        System.err.println("Ending merge: " + (System.currentTimeMillis() - t) + "ms");
        merging = false;
    }

    private void merge(StateActionPair sap1, StateActionPair sap2){

        if(population.containsKey(sap1.getId()) && population.containsKey(sap2.getId())){
            StateActionPair newState = sap1.merge(sap2);
            removeState(sap1);
            removeState(sap2);

            addState(newState);
        }
    }

    private double getMinDevForMerge()
    {
        if(minDevForMerge == 0){
            long found = 0;
            double minDev = minDevForMerge;

            long threshold = deviationDistributionSize/4;

            for(Map.Entry<Integer, Long> entry : deviationDistributions.entrySet())
            {
                double dev = ((double) entry.getKey()) /100;
                if(minDev < dev)
                {
                    minDev = dev;
                }

                found += entry.getValue();
                if(found > threshold && minDev > 0)
                {
                    break;
                }
            }

            minDevForMerge = minDev;
            return minDev;
        }
        else {
            double adjustment = ((double) (population.size() - getMaxPopulation()/2))/(getMaxPopulation()) + 1;
            adjustment = Math.pow(adjustment, 2);
            minDevForMerge = adjustment * minDevForMerge;
        }

        return minDevForMerge;
    }

    private synchronized void removeState(StateActionPair pair)
    {
        if(population.containsKey(pair.getId())){
            population.remove(pair.getId());

            for(int index = 0; index < pair.getAmalgamate().length; index++)
            {
                int signalValue = pair.getAmalgamate()[index];
                indices.get(index).get(signalValue).remove(pair);
            }
        }
    }

    private synchronized void addState(StateActionPair pair)
    {
        if(!population.containsKey(pair.getId())){
            population.put(pair.getId(), pair);
            int[] state = pair.getAmalgamate();
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
            }
        }
    }

    public TreeMap<Integer, Double> getActionDistribution(int[] state)
    {
        TreeMap<Integer, Double> distribution = new TreeMap<Integer, Double>();
        for(Map.Entry<StateActionPair, Double> entry : getSimilarStates(state).entrySet())
        {
            double weight = getWeightForDeviation(entry.getValue());
            StateActionPair pair = entry.getKey();

            for(Map.Entry<Integer, Double> distEntry : pair.getActionDistribution().entrySet())
            {
                double value = 0;
                if(distribution.containsKey(distEntry.getKey())){
                    value = distribution.get(distEntry.getKey());
                }
                distribution.put(distEntry.getKey(), value + (weight * distEntry.getValue()));
            }
        }
        return distribution;
    }

    public Map<Integer, Double> getCorrelationMap()
    {
        TreeMap<Integer, Double> correlations = new TreeMap<Integer, Double>();
        int index = 0;
        for(double weight : correlationWeights){
            correlations.put(index, weight);
            index++;
        }

        return correlations;
    }

    public TreeMap<Double, StateActionPair> getAlphaStates(){
        TreeMap<Double, StateActionPair> alpha = new TreeMap<>();
        for(StateActionPair pair : population.values()){
            double score = sum(updateAndGetCorrelationWeights(pair.getAmalgamate()));
            alpha.put(score, pair);
        }
        return alpha;
    }

    public ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>> getIndexForVariable(int variable)
    {
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

    public StateActionPair getStateActionPair(String id){
        return population.get(id);
    }

    @Override
    public String toString() {
        return "population=" + AmalgamateUtils.getMapString(population) +
                ", indices=" + AmalgamateUtils.getMapString(indices) +
                ", correlationWeights=" +  AmalgamateUtils.getArrayString(correlationWeights) +
                ", correlationCalculators=" + AmalgamateUtils.getMapString(correlationCalculators) +
                ", correlations=" + AmalgamateUtils.getArrayString(correlations) +
                ", copulae=" + System.identityHashCode(copulae) +
                ", deviationDistributions=" + AmalgamateUtils.getMapString(deviationDistributions) +
                ", deviationDistributionSize=" + deviationDistributionSize +
                ", distribution=" + AmalgamateUtils.getMapString(distribution) +
                ", actionResolution=" + actionResolution +
                ", maxPopulation=" + PropertiesHolder.maxPopulation +
                ", tolerance=" + PropertiesHolder.tolerance +
                ", copulaToUniversal=" + PropertiesHolder.copulaToUniversal +
                ", minDevForMerge=" + minDevForMerge;
    }
}
