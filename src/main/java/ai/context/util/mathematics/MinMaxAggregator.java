package ai.context.util.mathematics;

public class MinMaxAggregator {

    private Double min;
    private Double max;

    public boolean addValue(double value) {
        boolean altered = false;
        if (max == null) {
            max = value;
        } else if (value > max) {
            max = value;
            altered = true;
        }

        if (min == null) {
            min = value;
        } else if (value < min) {
            min = value;
            altered = true;
        }
        return altered;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public double getMid() {
        return (min + max) / 2;
    }
}
