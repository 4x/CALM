package ai.context.learning;

public class DataObject {

    private final long timeStamp;
    private final int[] signal;
    private final double[] value;

    public DataObject(long timeStamp, int[] signal, double[] value) {
        this.timeStamp = timeStamp;
        this.signal = signal;
        this.value = value;
    }

    public int[] getSignal() {
        return signal;
    }

    public double[] getValue() {
        return value;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
