package ai.context.util.mathematics;

public class Discretiser {

    public static int getLinearDiscretisation(double value, double benchmark, double resolution)
    {
        return (int)((value - benchmark)/resolution);
    }

    public static int getLogarithmicDiscretisation(double value, double benchmark, double resolution)
    {
        value = value - benchmark;
        value = Math.signum(value) * (Math.log(Math.abs(value/resolution) + 1));

        return (int) value;
    }
}
