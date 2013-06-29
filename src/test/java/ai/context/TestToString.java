package ai.context;

import ai.context.core.StateActionPair;
import ai.context.util.learning.AmalgamateUtils;
import ai.context.util.learning.ClusteredCopulae;
import ai.context.util.mathematics.CorrelationCalculator;
import org.junit.Test;

import java.util.HashMap;

public class TestToString {

    @Test
    public void testToString()
    {
        CorrelationCalculator c1 = new CorrelationCalculator();
        CorrelationCalculator c2 = new CorrelationCalculator();
        CorrelationCalculator c3 = new CorrelationCalculator();

        System.out.println(c1.toString());
        System.out.println(c2.toString());
        System.out.println(c3.toString());

        ClusteredCopulae copulae1 = new ClusteredCopulae();
        ClusteredCopulae copulae2 = new ClusteredCopulae();
        ClusteredCopulae copulae3 = new ClusteredCopulae();

        System.out.println(copulae1.toString());
        System.out.println(copulae2.toString());
        System.out.println(copulae3.toString());

        int[] sig = new int[]{0,1,2,3,4};
        StateActionPair pair1 = new StateActionPair(AmalgamateUtils.getAmalgamateString(sig), sig, 1);
        StateActionPair pair2 = new StateActionPair(AmalgamateUtils.getAmalgamateString(sig), sig, 1);
        StateActionPair pair3 = new StateActionPair(AmalgamateUtils.getAmalgamateString(sig), sig, 1);

        System.out.println(pair1.toString());
        System.out.println(pair2.toString());
        System.out.println(pair3.toString());

        HashMap<Integer, HashMap<Integer, Object>> map = new HashMap<>();
        map.put(1, new HashMap<Integer, Object>());
        map.put(2, new HashMap<Integer, Object>());
        map.put(3, new HashMap<Integer, Object>());
        map.put(4, new HashMap<Integer, Object>());

        map.get(1).put(1, "A");
        map.get(1).put(2, "B");
        map.get(1).put(3, "B");
        map.get(1).put(4, "C");

        map.get(2).put(5, "D");
        map.get(3).put(6, "E");
        map.get(2).put(7, "F");
        map.get(2).put(8, "G");

        map.get(3).put(9, "H");
        map.get(4).put(1, "A");
        map.get(2).put(2, "I");
        map.get(3).put(1, "J");

        map.get(4).put(7, new Object());

        map.get(4).put(8, 1);

        System.out.println(AmalgamateUtils.getMapString(map));

    }

}
