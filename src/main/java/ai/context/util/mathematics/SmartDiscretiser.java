package ai.context.util.mathematics;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

public class SmartDiscretiser {

    private TreeMap<Double, Long> distribution = new TreeMap<Double, Long>();
    private long count = 0;

    private TreeSet<Double> pivots = new TreeSet<Double>();

    public long criticalMass = 10000;
    public int clusters = 5;

    public SmartDiscretiser(long criticalMass, int clusters) {
        this.criticalMass = criticalMass;
        this.clusters = clusters;
    }

    public int discretise(Double value) {
        if (value == null) {
            return -1;
        }
        count++;
        if (count < criticalMass) {
            if (!distribution.containsKey(value)) {
                distribution.put(value, 0L);
            }

            distribution.put(value, distribution.get(value) + 1);
        } else if (count == criticalMass) {
            pivots.clear();
            long clusterSize = criticalMass / clusters;
            Iterator<Double> iterator = distribution.keySet().iterator();
            double pivot = 0;
            for (int i = 0; i < clusters; i++) {
                long clusterCount = 0;
                while (iterator.hasNext()) {
                    pivot = iterator.next();
                    clusterCount += distribution.get(pivot);
                    if (clusterCount > clusterSize) {
                        pivots.add(pivot);
                        break;
                    }
                }
            }
            count = 0;
            distribution.clear();
        }

        if (!pivots.isEmpty()) {
            int toReturn = 0;
            for (double pivot : pivots) {
                if (value < pivot) {
                    break;
                }
                toReturn++;
            }
            return toReturn;
        }
        return -1;
    }
}
