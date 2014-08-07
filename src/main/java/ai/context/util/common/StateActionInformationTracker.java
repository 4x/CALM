package ai.context.util.common;

import ai.context.util.mathematics.Discretisation;
import ai.context.util.mathematics.Latcher;
import ai.context.util.mathematics.MinMaxAggregator;

public class StateActionInformationTracker {
    private final long timeStamp;
    private final int[] state;
    private final double initialLevel;
    private Latcher latcher;
    private MinMaxAggregator aggregator;
    private Discretisation discretisation;

    private double performance = 0;
    private int expectation;
    private long start;
    private Object lockedTime;

    public StateActionInformationTracker(long timeStamp, int[] state, double initialLevel, double significantMovement, int expectation) {
        this.timeStamp = timeStamp;
        this.state = state;
        this.initialLevel = initialLevel;
        this.expectation = expectation;
        this.start = timeStamp;

        this.latcher = new Latcher(initialLevel, timeStamp, significantMovement);
        this.aggregator = new MinMaxAggregator();
        aggregator.addValue(0.0);
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int[] getState() {
        return state;
    }

    public void processHigh(double high, long timeStamp){
        latcher.registerHigh(high, timeStamp);
        aggregator.addValue(high - initialLevel);
    }

    public void setDiscretisation(Discretisation discretisation){
        this.discretisation = discretisation;
    }

    public void processLow(double low, long timeStamp){
        latcher.registerLow(low, timeStamp);
        aggregator.addValue(low - initialLevel);
    }

    public void processHighAndLow(double high, double low, long timeStamp){
        latcher.registerBounds(high, low, timeStamp);
    }

    public double getMaxUp(){
        //double val = aggregator.getMax();
        double val = latcher.getMaxUp();
        if(discretisation != null){
            return discretisation.process(val);
        }
        return val;
    }

    public double getMaxDown(){
        //double val = aggregator.getMin();
        double val = latcher.getMaxDown();
        if(discretisation != null){
            return discretisation.process(val);
        }
        return val;
    }

    public double getPerformance(){
        return aggregator.getMid() * expectation;
    }

    public long getStart() {
        return start;
    }

    public long getLockedTime() {
        return latcher.getEnd();
    }
}
