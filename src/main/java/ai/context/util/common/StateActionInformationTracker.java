package ai.context.util.common;

import ai.context.util.mathematics.Discretisation;
import ai.context.util.mathematics.Latcher;

public class StateActionInformationTracker {
    private final long timeStamp;
    private final int[] state;
    private final double initialLevel;
    private Latcher latcher;
    private Discretisation discretisation;

    public StateActionInformationTracker(long timeStamp, int[] state, double initialLevel, double significantMovement) {
        this.timeStamp = timeStamp;
        this.state = state;
        this.initialLevel = initialLevel;

        this.latcher = new Latcher(initialLevel, timeStamp, significantMovement);
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int[] getState() {
        return state;
    }

    public void processHigh(double high, long timeStamp){
        latcher.registerHigh(high, timeStamp);
    }

    public void setDiscretisation(Discretisation discretisation){
        this.discretisation = discretisation;
    }

    public void processLow(double low, long timeStamp){
        latcher.registerLow(low, timeStamp);
    }

    public double getMaxUp(){
        if(discretisation != null){
            return discretisation.process(latcher.getMaxUp());
        }
        return latcher.getMaxUp();
    }

    public double getMaxDown(){
        if(discretisation != null){
            return discretisation.process(latcher.getMaxDown());
        }
        return latcher.getMaxDown();
    }
}
