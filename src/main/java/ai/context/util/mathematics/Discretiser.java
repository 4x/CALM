package ai.context.util.mathematics;

public class Discretiser {

    public static int getLinearDiscretisation(double value, double benchmark, double resolution) {
        return (int) (0.5 + (value - benchmark) / resolution);
    }

    public static int getLogarithmicDiscretisation(double value, double benchmark, double resolution) {
        value = value - benchmark;
        value = Math.signum(value) * (Math.log(Math.abs(value / resolution) + 1));

        return (int) (value + 0.5);
    }

    public static int getLogarithmicDiscretisation(double value, double benchmark, double resolution, double base) {
        value = value - benchmark;
        value = Math.signum(value) * Math.log(Math.abs(value / resolution) + 1) / Math.log(base);

        return (int) (value + 0.5);
    }
}
