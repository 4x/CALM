package ai.context.util.trading;

import ai.context.core.ai.LearnerService;
import ai.context.core.ai.LearningException;
import ai.context.learning.neural.NeuronRankings;
import ai.context.util.common.Count;
import ai.context.util.measurement.MarketMakerPosition;

import java.util.*;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class OrderIntelligenceEngine {
    private volatile static OrderIntelligenceEngine instance;

    public static OrderIntelligenceEngine getInstance() {
        if (instance == null) {
            synchronized (NeuronRankings.class) {
                if (instance == null) {
                    instance = new OrderIntelligenceEngine();
                }
            }
        }
        return instance;
    }

    private boolean init = false;
    private long points = 0;

    private double resolution = 0.0001;

    private Map<Integer, Set<String>> neuronsUpConf = new HashMap<>();
    private Map<Integer, Set<String>> neuronsDownConf = new HashMap<>();

    private Map<Integer, LearnerService> neuronsUp = new HashMap<>();
    private Map<Integer, LearnerService> neuronsDown = new HashMap<>();

    private void setUpNeurons(Set<String> stimuli){
        for(int i = 0; i < 25; i++){
            List<String> list = new ArrayList<>(stimuli);
            HashSet<String> set = new HashSet();
            neuronsUpConf.put(i, set);
            for(int sN = 0; sN < 5; sN++){
                int s = (int) Math.min(list.size(), list.size() * Math.random());
                set.add(list.remove(s));
            }
            set.add("dir");
            LearnerService service = new LearnerService();
            service.setActionResolution(resolution);
            neuronsUp.put(i, service);
        }

        for(int i = 0; i < 25; i++){
            List<String> list = new ArrayList<>(stimuli);
            HashSet<String> set = new HashSet();
            neuronsDownConf.put(i, set);
            for(int sN = 0; sN < 5; sN++){
                int s = (int) Math.min(list.size(), list.size() * Math.random());
                set.add(list.remove(s));
            }
            set.add("dir");
            LearnerService service = new LearnerService();
            service.setActionResolution(resolution);
            neuronsDown.put(i, service);
        }

        init = true;
    }

    public void feed(MarketMakerPosition advice){
        if(!init){
            setUpNeurons(advice.attributes.keySet());
        }

        Map<String, Integer> stimuli = getStimuli(advice);

        for(Map.Entry<Integer, Set<String>> entry : neuronsUpConf.entrySet()){
            int[] signal = new int[entry.getValue().size()];
            int i = 0;
            for(String key : entry.getValue()){
                if(stimuli.containsKey(key)){
                    signal[i] = stimuli.get(key);
                }
                i++;
            }
            try {
                double val = advice.getHigh() - advice.getOpen();
                neuronsUp.get(entry.getKey()).addStateAction(signal, val);
                //System.out.println("Fed: " + Arrays.toString(signal) + ": "  + val);
            } catch (LearningException e) {
                e.printStackTrace();
            }
        }

        for(Map.Entry<Integer, Set<String>> entry : neuronsDownConf.entrySet()){
            int[] signal = new int[entry.getValue().size()];
            int i = 0;
            for(String key : entry.getValue()){
                if(stimuli.containsKey(key)){
                    signal[i] = stimuli.get(key);
                }
                i++;
            }
            try {
                double val = advice.getOpen() - advice.getLow();
                neuronsDown.get(entry.getKey()).addStateAction(signal, val);
                //System.out.println("Fed: " + Arrays.toString(signal) + ": "  + val);
            } catch (LearningException e) {
                e.printStackTrace();
            }
        }
        points++;
    }

    public double getConfirmationFor(MarketMakerPosition advice){
        if(points < 1000){
            return 0;
        }

        Map<String, Integer> stimuli = getStimuli(advice);

        TreeMap<Integer, Count> distUp = new TreeMap<>();
        TreeMap<Integer, Count> distDown = new TreeMap<>();

        for(Map.Entry<Integer, Set<String>> entry : neuronsUpConf.entrySet()){
            int[] signal = new int[entry.getValue().size()];
            int i = 0;
            for(String key : entry.getValue()){
                if(stimuli.containsKey(key)){
                    signal[i] = stimuli.get(key);
                }
                i++;
            }
            TreeMap<Integer, Double> dist = neuronsUp.get(entry.getKey()).getActionDistribution(signal);

            for(Map.Entry<Integer, Double> dEntry : dist.entrySet()){
                if(!distUp.containsKey(dEntry.getKey())){
                    distUp.put(dEntry.getKey(), new Count());
                }
                distUp.get(dEntry.getKey()).val += dEntry.getValue();
            }
        }

        for(Map.Entry<Integer, Set<String>> entry : neuronsDownConf.entrySet()){
            int[] signal = new int[entry.getValue().size()];
            int i = 0;
            for(String key : entry.getValue()){
                if(stimuli.containsKey(key)){
                    signal[i] = stimuli.get(key);
                }
                i++;
            }
            TreeMap<Integer, Double> dist = neuronsDown.get(entry.getKey()).getActionDistribution(signal);

            for(Map.Entry<Integer, Double> dEntry : dist.entrySet()){
                if(!distDown.containsKey(dEntry.getKey())){
                    distDown.put(dEntry.getKey(), new Count());
                }
                distDown.get(dEntry.getKey()).val += dEntry.getValue();
            }
        }

        double cumU = 0;
        for(Count c : distUp.descendingMap().values()){
            cumU += c.val;
            c.val = cumU;
        }

        double cumD = 0;
        for(Count c : distDown.descendingMap().values()){
            cumD += c.val;
            c.val = cumD;
        }

        int maxUp = 0;
        for(Map.Entry<Integer, Count> entry : distUp.entrySet()){
            if(entry.getValue().val/cumU > PositionFactory.minProbFraction){
                maxUp = entry.getKey();
            }
        }

        int maxDown = 0;
        for(Map.Entry<Integer, Count> entry : distDown.entrySet()){
            if(entry.getValue().val/cumD > PositionFactory.minProbFraction){
                maxDown = entry.getKey();
            }
        }

        //System.out.println(stimuli + ", mD: " + maxDown + ", mU: " + maxUp + ", cumD: " + cumD + ", cumU: " + cumU);
        if(advice.attributes.get("dir") == 1){
            if(maxUp > 10 && maxUp > maxDown * PositionFactory.rewardRiskRatio){
                return maxUp * resolution;
            }
        } else if(advice.attributes.get("dir") == -1){
            if(maxDown > 10 && maxDown > maxUp * PositionFactory.rewardRiskRatio){
                return maxDown * resolution;
            }
        }

        return 0;
    }

    private Map<String, Integer> getStimuli(MarketMakerPosition advice){
        Map<String, Integer> stimuli = new HashMap<>();
        for(Map.Entry<String, Object> entry : advice.attributes.entrySet()){
            if(entry.getKey().equals("cred")){
                stimuli.put(entry.getKey(), getLogarithmicDiscretisation((Double)entry.getValue(), 0, 0.5));
            }
            else if(entry.getKey().startsWith("dU_") || entry.getKey().startsWith("dD_") ){
                stimuli.put(entry.getKey(), (int)((Double)entry.getValue() / resolution * 10));
            }
            else if(entry.getValue() instanceof Long){
                stimuli.put(entry.getKey(), (int)((Long)entry.getValue()/ DecisionAggregatorA.getTimeQuantum()));
            }
            else if(entry.getValue() instanceof Double){
                stimuli.put(entry.getKey(), getLogarithmicDiscretisation((Double)entry.getValue(), 0, resolution));
            }
            else {
                stimuli.put(entry.getKey(), (Integer) entry.getValue());
            }
        }
        return stimuli;
    }

    public void setResolution(double resolution) {
        this.resolution = resolution;
    }
}
