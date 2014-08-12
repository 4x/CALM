package ai.context.util.trading.version_1;

import java.util.TreeMap;

public interface PositionEngine {

    public OpenPosition getPosition(long time, double pivot, TreeMap<Double, Double> histogram);
}
