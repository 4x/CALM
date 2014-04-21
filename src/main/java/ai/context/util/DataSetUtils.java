package ai.context.util;

import java.util.List;

public class DataSetUtils {
    public static void add(Object currentData, List data) {
        if (currentData instanceof int[]) {
            int[] array = (int[]) currentData;
            for (int o : array) {
                add(o, data);
            }
        } else if (currentData instanceof double[]) {
            double[] array = (double[]) currentData;
            for (double o : array) {
                add(o, data);
            }
        } else if (currentData instanceof Object[]) {
            Object[] array = (Object[]) currentData;
            for (Object o : array) {
                add(o, data);
            }
        } else if (currentData instanceof List) {
            List list = (List) currentData;

            for (Object o : list) {
                add(o, data);
            }
        } else {
            data.add(currentData);
        }
    }
}
