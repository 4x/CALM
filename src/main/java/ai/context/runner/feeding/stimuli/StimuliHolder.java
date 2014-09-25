package ai.context.runner.feeding.stimuli;

import ai.context.runner.feeding.StateToAction;
import ai.context.util.learning.ClusteredCopulae;

public class StimuliHolder {
    private ClusteredCopulae coreUp = new ClusteredCopulae();
    private ClusteredCopulae coreDown = new ClusteredCopulae();
    private Integer[] signalsSources;
    private long horizon;
    private double score = 0;

    public StimuliHolder(Integer[] signalsSources, long horizon) {
        this.signalsSources = signalsSources;
        this.horizon = horizon;
    }

    public void feed(StateToAction stateToAction) {
        int[] sig = new int[signalsSources.length];
        for (int i = 0; i < sig.length; i++) {
            sig[i] = stateToAction.signal[signalsSources[i]];
        }

        if(!stateToAction.horizonActions.containsKey(horizon)){
            return;
        }
        coreUp.addObservation(sig, stateToAction.horizonActions.get(horizon)[0]);
        coreDown.addObservation(sig, stateToAction.horizonActions.get(horizon)[1]);
    }

    public void reFeed(StateToAction stateToAction) {
        int[] sig = new int[signalsSources.length];
        for (int i = 0; i < sig.length; i++) {
            sig[i] = stateToAction.signal[signalsSources[i]];
        }

        double[] weights = coreUp.getCorrelationWeights(sig);

        for(double w : weights){
            if(!"NaN".equals("" + w)) {
                score += w;
            }
        }

        weights = coreDown.getCorrelationWeights(sig);

        for(double w : weights){
            if(!"NaN".equals("" + w)) {
                score += w;
            }
        }
    }

    public Integer[] getSignalsSources() {
        return signalsSources;
    }

    public long getHorizon() {
        return horizon;
    }

    public double getScore() {
        return score;
    }
}
