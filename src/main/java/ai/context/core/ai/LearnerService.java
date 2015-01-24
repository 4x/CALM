package ai.context.core.ai;

import ai.context.util.common.Count;
import ai.context.util.common.LabelledTuple;
import ai.context.util.common.ScratchPad;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.learning.AmalgamateUtils;
import ai.context.util.learning.ClusteredCopulae;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ai.context.util.mathematics.Operations.sum;

public class LearnerService {

    private ConcurrentHashMap<String, StateActionPair> population = new ConcurrentHashMap<String, StateActionPair>();
    private TreeMap<Integer, Count> dist = new TreeMap<>();

    private double[] correlationWeights;

    private ClusteredCopulae copulae = new ClusteredCopulae();

    private int[] populationVariation = new int[200];

    private boolean doubleCheckMerge = true;
    private double actionResolution = 1.0;

    private boolean merging = false;
    private double minDev = -1;
    private double maxDev = -1;
    private double minDevForMerge = -1;

    private LearnerService thisService = this;

    private boolean correlating = false;
    private boolean useSkewInSimilarity = false;
    private boolean useSkewInSimilarityWhenQuerying = false;
    private double sumSkewDiff = 0;
    private long skewDiffsCount = 0;

    private long pointsConsumed = 0;
    private int minDevIncrements = 0;
    private int numberOfMerges = 0;
    private int lastUpwardsCorrection = 0;
    private boolean initialMerge = true;

    public boolean isMerging() {
        return merging;
    }

    public LearnerService(){}

    public LearnerService(ConcurrentHashMap<String, StateActionPair> population, double[] correlations, ClusteredCopulae copulae, double actionResolution, int maxPopulation, double tolerance, double copulaToUniversal, double minDev, double maxDev, double minDevForMerge) {
        this.population = population;
        this.correlationWeights = correlationWeights;
        this.copulae = copulae;
        PropertiesHolder.maxPopulation = maxPopulation;
        PropertiesHolder.tolerance = tolerance;
        PropertiesHolder.copulaToUniversal = copulaToUniversal;
        this.maxDev = maxDev;
        this.minDev = minDev;
        this.minDevForMerge = minDevForMerge;
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

    public double getCopulaToUniversal() {
        return PropertiesHolder.copulaToUniversal;
    }

    public void setCopulaToUniversal(double copulaToUniversal) {
        PropertiesHolder.copulaToUniversal = copulaToUniversal;
    }

    public synchronized void addStateAction(int[] state, double movement, LabelledTuple... additionalInformation) throws LearningException {

        pointsConsumed++;
        numberOfMerges = 0;

        int actionClass = (int) Math.round(movement / actionResolution);

        if(!dist.containsKey(actionClass)){
            dist.put(actionClass, new Count());
        }
        dist.get(actionClass).val++;

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

            Set<Map.Entry<StateActionPair, Double>> entries = getSimilarStates(state, false).entrySet();
            for (Map.Entry<StateActionPair, Double> entry : entries) {
                double weight = getWeightForDeviation(entry.getValue());
                entry.getKey().newMovement(movement, weight);

                for(LabelledTuple tuple : additionalInformation) {
                    entry.getKey().getAdditionInformation(tuple.name).addValue(tuple.key, tuple.value, weight);
                }

                if (currentDev == -1 && !entry.getKey().getId().equals(id)) {
                    currentDev = entry.getValue();
                }
                if (entry.getValue() <= currentDev && entry.getValue() < minDevForMerge && !entry.getKey().getId().equals(id)) {
                    counterpart = entry.getKey();
                    currentDev = entry.getValue();
                }
            }

            if (counterpart != null) {
                merge(population.get(id), counterpart);
            }

            if (newState && population.size() > PropertiesHolder.maxPopulation) {
                mergeStates();
            }
        }
    }

    public synchronized void refreshCorrelations(int[] state, double movement) {
        copulae.addObservation(state, movement);
    }

    public double[] updateAndGetCorrelationWeights(int[] state) {
        correlationWeights = copulae.getCorrelationWeights(state);
        return correlationWeights;
    }

    private Map<StateActionPair, Double> getSimilarStates(int[] state, boolean useSkewInSimilarity) {
        updateAndGetCorrelationWeights(state);

        TreeMap<Double, StateActionPair> top = new TreeMap<>();
        for (StateActionPair pair : population.values()) {
            double deviation =  StateActionPair.getDeviation(state, pair.getAmalgamate(), correlationWeights);
            top.put(deviation, pair);
        }
        double meanSkew = 0;
        HashMap<StateActionPair, Double> skews = new HashMap<>();

        int cutOff = (int) (top.size() * PropertiesHolder.tolerance * 5);
        int skewCutOff = cutOff/3;
        double skewW = 0;
        if (doubleCheckMerge) {
            int i = 0;
            TreeMap<Double, StateActionPair> top2 = new TreeMap<Double, StateActionPair>();
            for (Map.Entry<Double, StateActionPair> pair : top.entrySet()) {
                updateAndGetCorrelationWeights(pair.getValue().getAmalgamate());
                double deviation = (StateActionPair.getDeviation(pair.getValue().getAmalgamate(), state, correlationWeights) + pair.getKey())/2;
                top2.put(deviation, pair.getValue());
                if(i++ > cutOff){
                    break;
                }

                if(useSkewInSimilarity  || this.useSkewInSimilarity || useSkewInSimilarityWhenQuerying) {
                    double skew = getSkewness(pair.getValue());
                    skews.put(pair.getValue(), skew);
                    if (i < skewCutOff) {
                        meanSkew += skew * pair.getKey();
                        skewW += pair.getKey();
                    }
                }
            }
            top = top2;
        }

        if(useSkewInSimilarity || this.useSkewInSimilarity || useSkewInSimilarityWhenQuerying) {
            meanSkew /= skewW;
            boolean meanSkewUnset = false;
            if ("NaN".equals("" + meanSkew)) {
                ScratchPad.incrementCountFor(ScratchPad.MEAN_SKEW_NAN);
                meanSkewUnset = true;
            }
            if(this.useSkewInSimilarity && useSkewInSimilarity){
                TreeMap<Double, StateActionPair> top2 = new TreeMap<Double, StateActionPair>();
                for (Map.Entry<Double, StateActionPair> pair : top.entrySet()) {
                    double skewDiff = 1;
                    if(!meanSkewUnset){
                        skewDiff = Math.abs(skews.get(pair.getValue()) - meanSkew);

                        sumSkewDiff += skewDiff;
                        skewDiffsCount++;
                    }

                    double dev = pair.getKey();
                    if(dev == 0){
                        dev = 0.0001;
                    }
                    dev = pair.getKey() * Math.log(skewDiff + 1);
                    top2.put(dev, pair.getValue());
                }
                top = top2;
            }
        }


        HashMap<StateActionPair, Double> map = new HashMap<StateActionPair, Double>();
        TreeMap<Double, StateActionPair> closestNeighbours = new TreeMap<>();

        int i = 0;
        for (Map.Entry<Double, StateActionPair> pair : top.entrySet()) {
            map.put(pair.getValue(), pair.getKey());
            pair.getValue().setClosestNeighbours(closestNeighbours);
            closestNeighbours.put(pair.getKey(), pair.getValue());

            if ((minDev == -1 || minDev > pair.getKey())) {
                minDev = pair.getKey();
            }

            if ((maxDev == -1 || maxDev < pair.getKey())) {
                maxDev = pair.getKey();
            }

            i++;
            if (i >= population.size() * PropertiesHolder.tolerance && i >= 2) {
                break;
            }
        }

        StateActionPair itself = population.get(AmalgamateUtils.getAmalgamateString(state));
        if(itself != null) {
            map.put(itself, 0D);
        }
        return map;
    }

    private synchronized double getWeightForDeviation(double deviation) {
        if (deviation == 0) {
            return 1;
        }

        double x = Math.exp(deviation);
        double min = Math.exp(minDev);
        double max = Math.exp(maxDev);

        if (max == min) {
            return 0;
        }
        return (max - x) / (max - min);
    }

    private synchronized void mergeStates(){
        merging = true;
        double m = getMinDevToMerge((double)(population.size() - PropertiesHolder.maxPopulation/2)/(double)(population.size()));
        if(m > minDevForMerge){
            minDevForMerge = m;
        }

        long t = System.currentTimeMillis();
        System.err.println("Starting merge process (" + minDevForMerge + ")");

        boolean targetReached = false;

        int runsSinceLastMerge = 0;
        int runs = 0;
        int totalMerged = 0;
        while (true) {
            if(numberOfMerges >= PropertiesHolder.numberOfMergeTries){
                break;
            }
            numberOfMerges++;
            runs++;
            runsSinceLastMerge++;
            HashSet<StateActionPair> pairs = new HashSet<>();
            pairs.addAll(population.values());

            double meanWeight = 0;
            int x = 0;
            int y = 0;
            int z = 0;
            int a = 0;
            int sinceLastMerge = 0;
            HashSet<StateActionPair> check = new HashSet<>();
            for (StateActionPair pair : pairs) {
                if (population.containsKey(pair.getId())) {
                    x++;
                    for (Map.Entry<Double, StateActionPair> entry : pair.getClosestNeighbours().entrySet()) {
                        StateActionPair counterpart = entry.getValue();
                        check.addAll(pair.getClosestNeighbours().values());
                        y++;
                        if (pair != counterpart && population.containsKey(counterpart.getId())) {
                            z++;
                            if (entry.getKey() <= minDevForMerge && merge(pair, counterpart)) {
                                meanWeight += (pair.getTotalWeight() + counterpart.getTotalWeight())/2;
                                sinceLastMerge = 0;
                                runsSinceLastMerge = 0;
                                a++;
                                totalMerged++;
                                if (population.size() < getMaxPopulation() / 2) {
                                    targetReached = true;
                                }
                            }
                        }
                    }
                    if (targetReached) {
                        break;
                    }
                    sinceLastMerge++;
                }
            }

            System.err.println("Run: " + runs + " x: " + x + " y: " + y + " z: " + z + " Check: " + check.size() + " a: " + a + " MW: " + (meanWeight/a));

            minDevForMerge *= 1.25;
            m = getMinDevToMerge((double)(population.size() - PropertiesHolder.maxPopulation/2)/(double)(population.size()));
            if(m > minDevForMerge){
                minDevForMerge = m;
            }
            minDevIncrements++;

            System.err.println("Using minDevForMerge: " + minDevForMerge);
            if(runsSinceLastMerge > 2){
                System.err.println("Skipping merging for now");
                break;
            }

            if (targetReached) {
                break;
            }
        }
        System.err.println("Ending merge: " + (System.currentTimeMillis() - t) + "ms  (" + minDevForMerge + ") Points consumed: " + pointsConsumed);

        if(minDevIncrements > 10 * PropertiesHolder.numberOfMergeTries){
            reset();
        }
        initialMerge = false;
        merging = false;
    }

    private double getMinDevToMerge(double fractionToMerge){
        for (StateActionPair pair : population.values()) {
            getSimilarStates(pair.getAmalgamate(), true);
        }

        double max = 0;
        TreeMap<Double, Count> dist = new TreeMap<>();
        for (StateActionPair pair : population.values()) {
            for (Map.Entry<Double, StateActionPair> entry : pair.getClosestNeighbours().entrySet()) {
                if(!dist.containsKey(entry.getKey())){
                    dist.put(entry.getKey(), new Count());
                }
                dist.get(entry.getKey()).val += 1;
                max++;
            }
        }

        double cum = 0;
        double toReturn = -1;
        for(Map.Entry<Double, Count> entry: dist.entrySet()){
            if(entry.getKey() > 0){
                cum += entry.getValue().val;
                toReturn = entry.getKey();
                if(cum/max > fractionToMerge){
                    break;
                }
            }
        }
        return toReturn;
    }

    public double getSkewDiff(StateActionPair sap1, StateActionPair sap2){
        return Math.abs(getSkewness(sap1) - getSkewness(sap2));
    }

    private boolean merge(StateActionPair sap1, StateActionPair sap2) {
        lastUpwardsCorrection++;
        if(population.size() < PropertiesHolder.maxPopulation/3){
            if(lastUpwardsCorrection > 25) {
                minDevForMerge /= 1.1;
                int lastUpwardsCorrection = 0;
            }
            return false;
        }

        if (population.containsKey(sap1.getId()) && population.containsKey(sap2.getId())) {
            StateActionPair newState = sap1.merge(sap2);
            removeState(sap1);
            removeState(sap2);

            addState(newState);
            return true;
        }
        return false;
    }

    private synchronized void removeState(StateActionPair pair) {
        if (population.containsKey(pair.getId())) {
            population.remove(pair.getId());
        }
    }

    private synchronized void addState(StateActionPair pair) {
        if (!population.containsKey(pair.getId())) {
            population.put(pair.getId(), pair);
        }
    }

    public TreeMap<Integer, Double> getActionDistribution(int[] state) {
        TreeMap<Integer, Double> distribution = new TreeMap<Integer, Double>();
        for (Map.Entry<StateActionPair, Double> entry : getSimilarStates(state, useSkewInSimilarityWhenQuerying).entrySet()) {
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

    public ActionInformationBundle getActionInformation(int[] state, String ... addtionalInfomation) {
        TreeMap<Integer, Double> distribution = new TreeMap<Integer, Double>();
        HashMap<String, AdditionalStateActionInformation> actionInformationMap = new HashMap<>();
        for(String info : addtionalInfomation){
            actionInformationMap.put(info, new AdditionalStateActionInformation());
        }

        for (Map.Entry<StateActionPair, Double> entry : getSimilarStates(state, useSkewInSimilarityWhenQuerying).entrySet()) {
            double weight = getWeightForDeviation(entry.getValue());
            StateActionPair pair = entry.getKey();

            for (Map.Entry<Integer, Double> distEntry : pair.getActionDistribution().entrySet()) {
                double value = 0;
                if (distribution.containsKey(distEntry.getKey())) {
                    value = distribution.get(distEntry.getKey());
                }
                distribution.put(distEntry.getKey(), value + (weight * distEntry.getValue()));
            }

            for(String info : addtionalInfomation){
                AdditionalStateActionInformation pairInfo = pair.getAdditionInformation(info);
                if(pairInfo != null) {
                    actionInformationMap.get(info).incorporate(pairInfo, weight);
                }
            }
        }
        return new ActionInformationBundle(distribution, actionInformationMap);
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

    public double getSkewness(StateActionPair pair){
        double skew = 0;
        for(Map.Entry<Integer, Double> entry : pair.getActionDistribution().entrySet()){
            skew += entry.getKey() * entry.getValue();
        }
        return skew;
    }

    public double[] getCorrelationWeightsForState(StateActionPair sap) {
        return copulae.getCorrelationWeights(sap.getAmalgamate());
    }

    public ConcurrentHashMap<String, StateActionPair> getPopulation() {
        return population;
    }

    public ClusteredCopulae getCopulae() {
        return copulae;
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

    public int getMinDevIncrements() {
        return minDevIncrements;
    }

    private void reset(){
        sumSkewDiff = 0;
        skewDiffsCount = 0;

        pointsConsumed = 0;
        minDevIncrements = 0;
        numberOfMerges = 0;
        lastUpwardsCorrection = 0;
        initialMerge = true;

        merging = false;
        minDev = -1;
        maxDev = -1;
        minDevForMerge = -1;

        population = new ConcurrentHashMap<String, StateActionPair>();
        copulae = new ClusteredCopulae();
        populationVariation = new int[200];
        System.err.println("Resetting this LearnerService...");
    }

    @Override
    public String toString() {
        return "population=" + AmalgamateUtils.getMapString(population) +
                ", correlationWeights=" + AmalgamateUtils.getArrayString(correlationWeights) +
                ", copulae=" + System.identityHashCode(copulae) +
                ", actionResolution=" + actionResolution +
                ", maxPopulation=" + PropertiesHolder.maxPopulation +
                ", tolerance=" + PropertiesHolder.tolerance +
                ", copulaToUniversal=" + PropertiesHolder.copulaToUniversal +
                ", minDev=" + minDev +
                ", maxDev=" + maxDev +
                ", minDevForMerge=" + minDevForMerge;
    }

    public long getCount() {
        return pointsConsumed;
    }
}
