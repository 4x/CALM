package ai.context.builder;

import ai.context.core.ai.StateActionPair;

import java.io.*;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class StateActionPairBuilder {

    private static ConcurrentHashMap<String, StateActionPair> population = new ConcurrentHashMap<String, StateActionPair>();
    private static long timeOfSaving = 0;

    private static String folderPath;
    private static BufferedWriter writer;

    public synchronized static void save(StateActionPair pair) {
        String id = "" + System.identityHashCode(pair);
        try {
            writer.write(pair.toString() + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        population.put(id, pair);
    }

    public static void load(long timeOfSaving) {
        population.clear();
        try {
            if (writer != null) {
                writer.close();
            }

            InputStream inputStream = new FileInputStream(folderPath + "/StateActionPair_" + timeOfSaving);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                String id = parts[0].split("=")[1];
                String[] idParts = parts[1].split("=")[1].split(":");
                int[] amalgamate = new int[idParts.length];
                for (int i = 0; i < amalgamate.length; i++) {
                    amalgamate[i] = Integer.parseInt(idParts[i]);
                }
                double resolution = Double.parseDouble(parts[2].split("=")[1]);
                double totalWeight = Double.parseDouble(parts[4].split("=")[1]);
                String dist = parts[3].split("=")[1];
                TreeMap<Integer, Double> actionDistribution = new TreeMap<Integer, Double>();
                for (String distPart : dist.split(";")) {
                    int value = Integer.parseInt(distPart.split(":")[0]);
                    double intensity = Double.parseDouble(distPart.split(":")[1]);
                    actionDistribution.put(value, intensity);
                }

                StateActionPair stateActionPair = new StateActionPair(id, amalgamate, resolution, actionDistribution, totalWeight);
                population.put(id, stateActionPair);
            }

            System.err.println("SAP Population: " + population.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized static void createCheckPoint(long timeOfSaving, String folderPath) {
        StateActionPairBuilder.folderPath = folderPath;
        StateActionPairBuilder.timeOfSaving = timeOfSaving;

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(folderPath + "/StateActionPair_" + timeOfSaving, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        writer = new BufferedWriter(new OutputStreamWriter(fos));
        population.clear();
    }

    public static StateActionPair getStateActionPair(String id) {
        return population.get(id);
    }

    public static ConcurrentHashMap<String, StateActionPair> getPopulation() {
        return population;
    }

    public static long getTimeOfSaving() {
        return timeOfSaving;
    }

    public static void setFolderPath(String folderPath) {
        StateActionPairBuilder.folderPath = folderPath;
    }

    public static void setTimeOfSaving(long timeOfSaving) {
        StateActionPairBuilder.timeOfSaving = timeOfSaving;
    }

    public static String getFolderPath() {
        return folderPath;
    }
}
