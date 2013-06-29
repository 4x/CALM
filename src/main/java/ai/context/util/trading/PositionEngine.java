package ai.context.util.trading;

import ai.context.util.measurement.OpenPosition;

import java.util.TreeMap;

public interface PositionEngine {

    public OpenPosition getPosition(long time, double pivot, TreeMap<Double, Double> histogram);
}
