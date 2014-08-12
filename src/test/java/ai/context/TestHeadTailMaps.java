package ai.context;

import ai.context.util.trading.version_1.PositionFactory;
import org.junit.Test;

import java.util.TreeMap;

public class TestHeadTailMaps {

    @Test
    public void testMaps() {
        TreeMap<Double, Double> histogram = new TreeMap<Double, Double>();
        histogram.put(1.0, 15.0);
        histogram.put(2.0, 10.0);
        histogram.put(3.0, 10.0);
        histogram.put(4.0, 15.0);
        histogram.put(5.0, 5.0);
        histogram.put(6.0, 5.0);
        histogram.put(7.0, 5.0);
        histogram.put(12.0, 15.0);
        histogram.put(13.0, 15.0);

        PositionFactory.getPosition(0, 6.0, histogram, 6 * 3600 * 1000L, false);
    }
}
