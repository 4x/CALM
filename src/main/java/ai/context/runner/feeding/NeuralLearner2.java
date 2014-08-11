package ai.context.runner.feeding;

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

        discretiser = new AbsoluteMovementDiscretiser(0.01);
        discretiser.addLayer(0.003, 0.0001);
        discretiser.addLayer(0.005, 0.0005);
        discretiser.addLayer(0.01, 0.001);
    }

    public void feed(StateToAction stateToAction){
        double movement = stateToAction.horizonActions.get(horizon);
        int[] signal = getSignalState(stateToAction.signal);

        if(pointsLearned > 200){
            getDistributionFor(signal);
        }

        try {
            core.addStateAction(signal, discretiser.process(movement));
            pointsLearned++;
        } catch (LearningException e) {
            e.printStackTrace();
        }
    }

    public TreeMap<Integer, Double> getOpinionOnContext(int[] rawSignal){
        return getDistributionFor(getSignalState(rawSignal));
    }

    private int[] getSignalState(int[] rawSignal){
        int[] signal = new int[signalComponents.length + parents.length + 1];

        int index = 0;
        for(int i = 0; i < signal.length; i++){
            index++;
            signal[index] = rawSignal[signalComponents[index]];
        }

        for(int i = 0; i < parents.length; i++){
            index++;
            signal[index] = parents[i].getSignal();
        }
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
        state[state.length] = currentSignal = getLogarithmicDiscretisation(mean, 0, 1, 2);

        //Going again
        dist = core.getActionDistribution(state);
        mean = 0;
        weight = 0;
        for(Map.Entry<Integer, Double> entry : dist.entrySet()){
            mean += entry.getKey() * entry.getValue();
            weight += entry.getValue();
        }
        mean /= weight;
        currentSignal = getLogarithmicDiscretisation(mean, 0, 1, 2);

        return dist;
    }

    public int getSignal(){
        return currentSignal;
    }

    @Override
    public String toString() {
        return "" + id;
    }
}
