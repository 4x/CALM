package ai.context.util.mathematics;

public class MinMaxAggregator {

    private Double min;
    private Double max;

    public void addValue(double value) {
        if (max == null) {
            max = value;
        } else if (value > max) {
            max = value;
        }

        if (min == null) {
            min = value;
        } else if (value < min) {
            min = value;
        }
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }
}
