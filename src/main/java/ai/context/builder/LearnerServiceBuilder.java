package ai.context.builder;

import ai.context.core.ai.LearnerService;
import ai.context.core.ai.StateActionPair;
import ai.context.util.learning.ClusteredCopulae;
import ai.context.util.mathematics.CorrelationCalculator;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class LearnerServiceBuilder{

    public static void save(LearnerService learner, String folder, long time){

        CorrelationCaculatorBuilder.createCheckPoint(time, folder);
        ClusteredCopulaeBuilder.createCheckPoint(time, folder);
        StateActionPairBuilder.createCheckPoint(time, folder);

        for(CorrelationCalculator calculator : learner.getCorrelationCalculators().values()){
            CorrelationCaculatorBuilder.save(calculator);
        }

        for(Map.Entry<Integer, HashMap<Integer, TreeMap<Integer, CorrelationCalculator>>> e1 : learner.getCopulae().getVariableClusteredCorrelations().entrySet()){
            for(Map.Entry<Integer, TreeMap<Integer, CorrelationCalculator>> e2 : e1.getValue().entrySet()){
                for(Map.Entry<Integer, CorrelationCalculator> e3 : e2.getValue().entrySet()){
                    CorrelationCaculatorBuilder.save(e3.getValue());
                    //System.err.println("Saving: " + e3.getValue());
                }
            }
        }

        ClusteredCopulaeBuilder.save(learner.getCopulae());
        //System.err.println("Saving: " + learner.getCopulae());

        for(StateActionPair pair : learner.getPopulation().values()){
            StateActionPairBuilder.save(pair);
            //System.err.println("Saving: " + pair);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(folder + "/LearnerService_" + time, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
        try {
            writer.write(learner.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static LearnerService load(String folder, long time){

        CorrelationCaculatorBuilder.setFolderPath(folder);
        ClusteredCopulaeBuilder.setFolderPath(folder);
        StateActionPairBuilder.setFolderPath(folder);

        CorrelationCaculatorBuilder.load(time);
        ClusteredCopulaeBuilder.load(time);
        StateActionPairBuilder.load(time);

        try {
            InputStream inputStream = new FileInputStream(folder + "/LearnerService_" + time);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String[] data = reader.readLine().split(",");

            ConcurrentHashMap<String, StateActionPair> population = StateActionPairBuilder.getPopulation();
            ConcurrentSkipListMap<Integer, ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>>> indices = new ConcurrentSkipListMap<>();

            /*for(String indexData : data[1].split("=")[1].split(";")){

                String[] parts = indexData.split(":");
                int e1 = Integer.parseInt(parts[0]);
                int e2 = Integer.parseInt(parts[1]);
                String sapID = parts[2];

                if(!indices.containsKey(e1)){
                    indices.put(e1, new ConcurrentSkipListMap<Integer, CopyOnWriteArraySet<StateActionPair>>());
                }

                if(!indices.get(e1).containsKey(e2)){
                    indices.get(e1).put(e2, new CopyOnWriteArraySet<StateActionPair>());
                }
                if(population.containsKey(sapID)){
                    indices.get(e1).get(e2).add(population.get(sapID));
                }
                else {
                    System.err.println("SAP not found: " + sapID);
                }
            }*/

            String[] cwParts = data[2].split("=")[1].split(":");
            double [] correlationWeights = new double[cwParts.length];
            for(int i = 0; i < correlationWeights.length; i++){
                correlationWeights[i] = Double.parseDouble(cwParts[i]);
            }

            TreeMap<Integer, CorrelationCalculator> correlationCalculators = new TreeMap<Integer, CorrelationCalculator>();
            for(String ccData : data[3].split("=")[1].split(";")){

                String[] parts = ccData.split(":");
                int e1 = Integer.parseInt(parts[0]);
                String ccID = parts[1];

                CorrelationCalculator correlationCalculator = CorrelationCaculatorBuilder.getCalculator(ccID);
                if(ccID != null){
                    correlationCalculators.put(e1, correlationCalculator);
                }
                else {
                    System.err.println("CCalc not found: " + ccID);
                }
            }

            String[] cParts = data[4].split("=")[1].split(":");
            double [] correlations = new double[cParts.length];
            for(int i = 0; i < correlations.length; i++){
                correlations[i] = Double.parseDouble(cParts[i]);
            }

            ClusteredCopulae copulae = ClusteredCopulaeBuilder.getCopulae(data[5].split("=")[1]);
            if(copulae == null){
                System.err.println("Copulae not found: " + data[5].split("=")[1]);
            }

            TreeMap<Integer, Long> distribution = new TreeMap<Integer, Long>();
            for(String dData : data[6].split("=")[1].split(";")){

                String[] parts = dData.split(":");
                int e1 = Integer.parseInt(parts[0]);
                long e2 = Long.parseLong(parts[1]);

                distribution.put(e1, e2);
            }

            double actionResolution = Double.parseDouble(data[7].split("=")[1]);

            int maxPopulation = Integer.parseInt(data[8].split("=")[1]);
            double tolerance = Double.parseDouble(data[9].split("=")[1]);
            double copulaToUniversal = Double.parseDouble(data[10].split("=")[1]);
            double minDev = Double.parseDouble(data[11].split("=")[1]);
            double maxDev = Double.parseDouble(data[12].split("=")[1]);
            double minDevForMerge = Double.parseDouble(data[13].split("=")[1]);

            return new LearnerService(population, indices, correlationWeights, correlationCalculators, correlations, copulae, actionResolution, maxPopulation, tolerance, copulaToUniversal, minDev, maxDev, minDevForMerge, distribution);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
