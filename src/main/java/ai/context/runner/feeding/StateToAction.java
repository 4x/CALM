package ai.context.runner.feeding;

import java.util.TreeMap;

public class StateToAction {
    public final long timeStamp;
    public final int[] signal;
    public final TreeMap<Long, Double[]> horizonActions = new TreeMap<>();

    public StateToAction(long timeStamp, int[] signal) {
        this.timeStamp = timeStamp;
        this.signal = signal;
    }
}
