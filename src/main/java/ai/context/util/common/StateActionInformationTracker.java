package ai.context.util.common;

import ai.context.util.mathematics.MinMaxAggregator;

public class StateActionInformationTracker {
    private final long timeStamp;
    private final int[] state;
    private final double initialLevel;
    MinMaxAggregator aggregator = new MinMaxAggregator();

    public StateActionInformationTracker(long timeStamp, int[] state, double initialLevel) {
        this.timeStamp = timeStamp;
        this.state = state;
        this.initialLevel = initialLevel;
    }

    public void aggregate(double newLevel) {
        aggregator.addValue(newLevel - initialLevel);
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int[] getState() {
        return state;
    }

    public double getMax() {
        return aggregator.getMax();
    }

    public double getMin() {
        return aggregator.getMin();
    }
}
