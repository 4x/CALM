package ai.context.builder;

import ai.context.util.mathematics.CorrelationCalculator;

import java.io.*;
import java.util.HashMap;

public class CorrelationCaculatorBuilder {

    private static HashMap<String, CorrelationCalculator> calculators = new HashMap<>();
    private static long timeOfSaving = 0;

    private static String folderPath;
    private static BufferedWriter writer;


    public synchronized static void save(CorrelationCalculator calculator) {
        String id = "" + System.identityHashCode(calculator);
        try {
            writer.write(calculator.toString() + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        calculators.put(id, calculator);
    }

    public static void load(long timeOfSaving) {
        calculators.clear();
        try {
            if (writer != null) {
                writer.close();
            }

            InputStream inputStream = new FileInputStream(folderPath + "/CorrelationCalculators_" + timeOfSaving);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                String id = parts[0].split("=")[1];
                double currentCorrelation = Double.parseDouble(parts[1].split("=")[1]);
                long nPoints = Long.parseLong(parts[2].split("=")[1]);
                double x_sum = Double.parseDouble(parts[3].split("=")[1]);
                double y_sum = Double.parseDouble(parts[4].split("=")[1]);
                double x_y_sum = Double.parseDouble(parts[5].split("=")[1]);
                double x_2_sum = Double.parseDouble(parts[6].split("=")[1]);
                double y_2_sum = Double.parseDouble(parts[7].split("=")[1]);

                CorrelationCalculator calculator = new CorrelationCalculator(currentCorrelation, nPoints, x_sum, y_sum, x_y_sum, x_2_sum, y_2_sum);
                calculators.put(id, calculator);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized static void createCheckPoint(long timeOfSaving, String folderPath) {
        CorrelationCaculatorBuilder.folderPath = folderPath;
        CorrelationCaculatorBuilder.timeOfSaving = timeOfSaving;

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(folderPath + "/CorrelationCalculators_" + timeOfSaving, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        writer = new BufferedWriter(new OutputStreamWriter(fos));
        calculators.clear();
    }

    public static CorrelationCalculator getCalculator(String id) {
        return calculators.get(id);
    }

    public static long getTimeOfSaving() {
        return timeOfSaving;
    }

    public static void setFolderPath(String folderPath) {
        CorrelationCaculatorBuilder.folderPath = folderPath;
    }

    public static String getFolderPath() {
        return folderPath;
    }

    public static void setTimeOfSaving(long timeOfSaving) {
        CorrelationCaculatorBuilder.timeOfSaving = timeOfSaving;
    }
}
