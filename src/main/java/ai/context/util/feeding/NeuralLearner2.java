package ai.context.util.feeding;

import ai.context.core.ai.LearnerService;
import ai.context.core.ai.LearningException;
import ai.context.util.mathematics.discretisation.AbsoluteMovementDiscretiser;

import java.util.Map;
import java.util.TreeMap;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class NeuralLearner2 {

    private final LearnerService core;
    private final int id;
    private final int[] signalComponents;
    private final NeuralLearner2[] parents;
    public final long horizon;
    private final double resolution;
    private final AbsoluteMovementDiscretiser discretiser;
    private TreeMap<Integer, Double> predictionRaw;

    private int currentSignal = 0;
    private long pointsLearned = 0;

    public NeuralLearner2(int id, int[] signalComponents, NeuralLearner2[] parents, long horizon, double resolution) {
        this.id = id;
        this.signalComponents = signalComponents;
        this.parents = parents;
        this.horizon = horizon;
        this.resolution = resolution;

        this.core = new LearnerService();
        core.setActionResolution(resolution);

        discretiser = new AbsoluteMovementDiscretiser(0.005);
        discretiser.addLayer(0.001, 0.0001);
        discretiser.addLayer(0.002, 0.0002);
        discretiser.addLayer(0.005, 0.0005);
    }

    public void feed(StateToAction stateToAction){
        double movement = 0;
        if(stateToAction.horizonActions.containsKey(horizon)){
            Double[] movements = stateToAction.horizonActions.get(horizon);
            movement = movements[0];
            if(movements[1] > movements[0]){
                movement = -movements[1];
            }
        }
        int[] signal = getSignalState(stateToAction.timeStamp, stateToAction.signal);

        if(pointsLearned > 200){
            getDistributionFor(signal);
        }

        try {
            movement = discretiser.process(movement);
            core.addStateAction(signal, movement);
            //System.out.println("[" + id + "] " + Arrays.asList(signal) + " -> " + movement);
            pointsLearned++;
        } catch (LearningException e) {
            e.printStackTrace();
        }
    }

    public TreeMap<Integer, Double> getOpinionOnContext(long timeStamp, int[] rawSignal){
        return getDistributionFor(getSignalState(timeStamp, rawSignal));
    }

    private int[] getSignalState(long timeStamp, int[] rawSignal){
        int[] signal = new int[signalComponents.length + parents.length + 2];

        int index = 0;
        for(int i = 0; i < signalComponents.length; i++){
            signal[index] = rawSignal[signalComponents[index]];
            index++;
        }

        for(int i = 0; i < parents.length; i++){
            signal[index] = parents[i].getSignal();
            index++;
        }

        signal[index] = (int) ((timeStamp % (86400000))/(2 * 3600000));
        index++;

        return signal;
    }

    public TreeMap<Integer, Double> getDistributionFor(int[] state){
        TreeMap<Integer, Double> dist = core.getActionDistribution(state);
        double mean = 0;
        double weight = 0;
        for(Map.Entry<Integer, Double> entry : dist.entrySet()){
            mean += entry.getKey() * entry.getValue();
            weight += entry.getValue();
        }
        mean /= weight;
        mean = Math.pow(mean, 3);
        state[state.length - 1] = currentSignal = getLogarithmicDiscretisation(mean, 0, 1, 2);

        //Going again
        dist = core.getActionDistribution(state);
        mean = 0;
        weight = 0;
        for(Map.Entry<Integer, Double> entry : dist.entrySet()){
            mean += entry.getKey() * entry.getValue();
            weight += entry.getValue();
        }
        mean /= weight;
        mean = Math.pow(mean, 3);
        currentSignal = getLogarithmicDiscretisation(mean, 0, 1, 2);
        predictionRaw = dist;
        return dist;
    }

    public int getSignal(){
        return currentSignal;
    }

    public TreeMap<Integer, Double> getPredictionRaw() {
        return predictionRaw;
    }

    @Override
    public String toString() {
        return "" + id;
    }
}
