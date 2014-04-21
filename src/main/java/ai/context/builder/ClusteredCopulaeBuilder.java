package ai.context.builder;

import ai.context.util.learning.ClusteredCopulae;
import ai.context.util.mathematics.CorrelationCalculator;

import java.io.*;
import java.util.HashMap;
import java.util.TreeMap;

public class ClusteredCopulaeBuilder {

    private static HashMap<String, ClusteredCopulae> copulaes = new HashMap<>();
    private static long timeOfSaving = 0;

    private static String folderPath;
    private static BufferedWriter writer;


    public synchronized static void save(ClusteredCopulae copulae) {
        String id = "" + System.identityHashCode(copulae);
        try {
            writer.write(copulae.toString() + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        copulaes.put(id, copulae);
    }

    public static void load(long timeOfSaving) {
        copulaes.clear();
        try {
            if (writer != null) {
                writer.close();
            }

            InputStream inputStream = new FileInputStream(folderPath + "/ClusteredCopulae_" + timeOfSaving);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                String id = parts[0].split("=")[1];
                String data = parts[1].split("=")[1];

                HashMap<Integer, HashMap<Integer, TreeMap<Integer, CorrelationCalculator>>> variableClusteredCorrelations = new HashMap<Integer, HashMap<Integer, TreeMap<Integer, CorrelationCalculator>>>();
                for (String element : data.split(";")) {

                    String[] elementParts = element.split(":");
                    int e1 = Integer.parseInt(elementParts[0]);
                    int e2 = Integer.parseInt(elementParts[1]);
                    int e3 = Integer.parseInt(elementParts[2]);

                    CorrelationCalculator calculator = CorrelationCaculatorBuilder.getCalculator(elementParts[3]);
                    if (!variableClusteredCorrelations.containsKey(e1)) {
                        variableClusteredCorrelations.put(e1, new HashMap<Integer, TreeMap<Integer, CorrelationCalculator>>());
                    }
                    if (!variableClusteredCorrelations.get(e1).containsKey(e2)) {
                        variableClusteredCorrelations.get(e1).put(e2, new TreeMap<Integer, CorrelationCalculator>());
                    }
                    variableClusteredCorrelations.get(e1).get(e2).put(e3, calculator);

                    if (calculator == null) {
                        System.err.println("Calculator not found for copulae " + elementParts[3]);
                    }
                }

                ClusteredCopulae copulae = new ClusteredCopulae(variableClusteredCorrelations);
                copulaes.put(id, copulae);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized static void createCheckPoint(long timeOfSaving, String folderPath) {
        ClusteredCopulaeBuilder.folderPath = folderPath;
        ClusteredCopulaeBuilder.timeOfSaving = timeOfSaving;

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(folderPath + "/ClusteredCopulae_" + timeOfSaving, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        writer = new BufferedWriter(new OutputStreamWriter(fos));
        copulaes.clear();
    }

    public static ClusteredCopulae getCopulae(String id) {
        return copulaes.get(id);
    }

    public static long getTimeOfSaving() {
        return timeOfSaving;
    }

    public static void setFolderPath(String folderPath) {
        ClusteredCopulaeBuilder.folderPath = folderPath;
    }

    public static String getFolderPath() {
        return folderPath;
    }

    public static void setTimeOfSaving(long timeOfSaving) {
        ClusteredCopulaeBuilder.timeOfSaving = timeOfSaving;
    }
}
