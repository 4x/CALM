package ai.context.util.feeding.stimuli;

public class StimuliInformation {
    private Integer[] signalsSources;
    private long horizon;
    private double score = 0;

    public StimuliInformation(Integer[] signalsSources, long horizon, double score) {
        this.signalsSources = signalsSources;
        this.horizon = horizon;
        this.score = score;
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
