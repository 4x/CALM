package ai.context.util.common;

import ai.context.util.mathematics.Latcher;

public class StateActionInformationTracker {
    private final long timeStamp;
    private final int[] state;
    private final double initialLevel;
    private Latcher latcher;

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

    public void processLow(double low, long timeStamp){
        latcher.registerLow(low, timeStamp);
    }

    public double getMaxUp(){
        return latcher.getMaxUp();
    }

    public double getMaxDown(){
        return latcher.getMaxDown();
    }
}
