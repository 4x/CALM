package ai.context.util.trading;

import ai.context.util.measurement.OpenPosition;

import java.util.*;

public class PositionFactory {

    private static double confidenceThreshold = 0.5;
    private static double rewardRiskRatio = 2.0;
    private static double minTakeProfit = 0.001;
    public static OpenPosition getPosition(long time, double pivot, TreeMap<Double, Double> histogram)
    {
        SortedMap<Double, Double> shortMap = histogram.descendingMap().tailMap(pivot, false);
        SortedMap<Double, Double> longMap = histogram.tailMap(pivot, false);
        TreeMap<Double, Double> directionalFreq = new TreeMap<Double, Double>();

        TreeMap<Double, Double> sFreq = new TreeMap<Double, Double>();
        TreeMap<Double, Double> lFreq = new TreeMap<Double, Double>();

        Double longFreq = 0.0;
        Double shortFreq = 0.0;
        Double average = 0.0;

        TreeSet<Double> ticks = new TreeSet<Double>();

        TreeMap<Double, Double> sM = new TreeMap<Double, Double>();
        for(Map.Entry<Double, Double> entry : shortMap.entrySet())
        {
            ticks.add(pivot - entry.getKey());
            ticks.add((pivot - entry.getKey())/rewardRiskRatio);
            sM.put(pivot - entry.getKey(), entry.getValue());
        }

        TreeMap<Double, Double> lM = new TreeMap<Double, Double>();
        for(Map.Entry<Double, Double> entry : longMap.entrySet())
        {
            ticks.add(entry.getKey() - pivot);
            ticks.add((entry.getKey() - pivot)/rewardRiskRatio);
            lM.put(entry.getKey() - pivot, entry.getValue());
        }

        if(!lM.isEmpty()){
            longFreq = lM.firstEntry().getValue();
        }
        if(!sM.isEmpty()){
            shortFreq = sM.firstEntry().getValue();
        }
        for(Double amplitude : ticks.descendingSet())
        {
            if(sM.containsKey(amplitude))
            {
                shortFreq += sM.get(amplitude);
            }
            if(lM.containsKey(amplitude))
            {
                longFreq += lM.get(amplitude);
            }

            sFreq.put(amplitude, shortFreq);
            lFreq.put(amplitude, longFreq);

            directionalFreq.put(amplitude, longFreq - shortFreq);
        }

        boolean goLong = true;
        int confidence = 0;
        double amplitude = 0;
        double multiplier = 1.0;

        //System.out.println("++++++++++++++++++++++");
        List<Double> tickList = new ArrayList<Double>();
        tickList.addAll(ticks);
        int i = 0;
        for(Double tick : tickList){
            //System.out.println(tick + " " + lFreq.get(tick) + " " + sFreq.get(tick));
            i++;
            if(directionalFreq.get(tick) > 0){
                if(tickList.size() > i){
                    if(sFreq.get(tick) > sFreq.get(tickList.get(i))){
                        multiplier = 1.0;
                        double tp = rewardRiskRatio * tick;
                        if( lFreq.tailMap(tp, true).size() > 0){
                            Double freqSL = sFreq.get(tick);
                            boolean found = false;
                            Double tpAmp = tp;
                            for(Map.Entry<Double, Double> further : lFreq.tailMap(tp, true).entrySet()){
                                if(further.getValue() > freqSL){
                                    found = true;
                                    tpAmp = further.getKey();
                                }
                                else {
                                    break;
                                }
                            }
                            if(found && tpAmp > minTakeProfit){
                                return new OpenPosition(time, pivot, multiplier * tpAmp, multiplier * (tick + minTakeProfit/5) , true);
                            }
                        }
                    }
                }
            }
            else{
                if(tickList.size() > i){
                    if(lFreq.get(tick) > lFreq.get(tickList.get(i))){
                        multiplier = -1.0;
                        double tp = rewardRiskRatio * tick;
                        if(sFreq.tailMap(tp, true).size() > 0){
                            Double freqSL = lFreq.get(tick);
                            boolean found = false;
                            Double tpAmp = tp;
                            for(Map.Entry<Double, Double> further : sFreq.tailMap(tp, true).entrySet()){
                                if(further.getValue() > freqSL){
                                    found = true;
                                    tpAmp = further.getKey();
                                }
                                else {
                                    break;
                                }
                            }
                            if(found && tpAmp > minTakeProfit){
                                return new OpenPosition(time, pivot, multiplier * tpAmp, multiplier * (tick + minTakeProfit/5) , false);
                            }
                        }
                    }
                }
            }
        }

        /*for(Map.Entry<Double, Double> entry : directionalFreq.entrySet())
        {

            if(entry.getValue() > 0)
            {
                if(goLong)
                {
                    if(entry.getValue() > confidenceThreshold)
                    {
                        amplitude = entry.getKey();
                        confidence++;
                    }
                    else if(confidence > ticks.size()/2 && confidence > 5)
                    {
                        int multiplier = 1;
                        if(!goLong)
                        {
                            multiplier = -1;
                        }
                        return new OpenPosition(time, pivot, multiplier * amplitude, multiplier * amplitude/2, goLong);
                    }
                }
                else {
                    goLong = true;
                    confidence = 0;
                }
            }
            else {
                if(!goLong)
                {
                    if(entry.getValue() < -confidenceThreshold)
                    {
                        amplitude = entry.getKey();
                        confidence++;
                    }
                    else if(confidence > ticks.size()/2 && confidence > 5)
                    {
                        int multiplier = 1;
                        if(!goLong)
                        {
                            multiplier = -1;
                        }
                        return new OpenPosition(time, pivot, multiplier * amplitude, multiplier * amplitude/2, goLong);
                    }
                }
                else {
                    goLong = false;
                    confidence = 0;
                }
            }
            if(confidence > ticks.size()/2 && confidence > 5)
            {
                int multiplier = 1;
                if(!goLong)
                {
                    multiplier = -1;
                }
                return new OpenPosition(time, pivot, multiplier * amplitude, multiplier * amplitude/2, goLong);
            }
        }
        */return null;
    }

    public static void setConfidenceThreshold(double confidenceThreshold) {
        PositionFactory.confidenceThreshold = confidenceThreshold;
    }

    public static void setRewardRiskRatio(double rewardRiskRatio) {
        PositionFactory.rewardRiskRatio = rewardRiskRatio;
    }

    public static void setMinTakeProfit(double minTakeProfit) {
        PositionFactory.minTakeProfit = minTakeProfit;
    }
}
