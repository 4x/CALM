package ai.context.learning;

import java.util.Arrays;

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

    @Override
    public String toString() {
        return "DataObject{" +
                "timeStamp=" + timeStamp +
                ", signal=" + Arrays.toString(signal) +
                ", value=" + Arrays.toString(value) +
                '}';
    }
}
