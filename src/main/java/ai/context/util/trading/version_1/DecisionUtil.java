package ai.context.util.trading.version_1;

import ai.context.util.configuration.PropertiesHolder;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class DecisionUtil {

    private static double[] decilesU = new double[10];
    private static double[] decilesD = new double[10];

    public static double[] getDecision(TreeMap<Double, Double> sFreq, TreeMap<Double, Double> lFreq, Double minProbFraction, Double cost, Double rewardRiskRatio, double marketMakerConfidence) {

        if (sFreq.isEmpty()) {
            return new double[]{0, 0, 0, 0, 0, 0, 0};
        }

        if(cost == null){
            cost = PositionFactory.cost;
        }

        if(minProbFraction == null){
            minProbFraction = PositionFactory.minProbFraction;
        }

        if(rewardRiskRatio == null){
            rewardRiskRatio = PositionFactory.rewardRiskRatio;
        }

        double max = Math.max(sFreq.firstEntry().getValue(), lFreq.firstEntry().getValue());
        double target = 0;
        double ratio = 0;
        double probFraction = 0;
        double payoff = 0;
        double high = 0;
        double low = 0;
        double high1 = 0;
        double low1 = 0;

        int flip = 0;

        TreeMap<Double, Double[]> cum = new TreeMap<>();
        for (double amplitude : sFreq.keySet()) {

            double freqS = sFreq.get(amplitude);
            double freqL = lFreq.get(amplitude);

            double probS = freqS / max;
            double probL = freqL / max;

            Double[] comparison = new Double[]{probS, probL};
            cum.put(amplitude, comparison);

            if(probS > marketMakerConfidence){
                low = amplitude;
            }
            if(probL > marketMakerConfidence){
                high = amplitude;
            }

            if(probS > PropertiesHolder.marketMakerLeeway){
                low1 = amplitude;
            }
            if(probL > PropertiesHolder.marketMakerLeeway){
                high1 = amplitude;
            }

            int dU = (int) (Math.min(9, (1 - probL) * 10));
            int dD = (int) (Math.min(9, (1 - probS) * 10));

            decilesU[dU] = amplitude;
            decilesD[dD] = amplitude;

            probFraction = Math.max(probL, probS);
            ratio = Math.max(probL, probS) / Math.min(probL, probS);
            if (    probFraction > minProbFraction
                    && amplitude > cost
                    && ratio > rewardRiskRatio
                    && amplitude > PositionFactory.cost * PropertiesHolder.marketMakerAmplitude) {

                int multiplier = 1;
                if (probL < probS) {
                    multiplier = -1;
                }
                target = multiplier * amplitude;
            }
        }

        if(Math.abs(target) > 0){
            SortedMap<Double, Double[]> inspectionMap = cum.headMap(Math.abs(target)).tailMap(Math.abs(4*target/5));
            char dir = 'X';
            for(Map.Entry<Double, Double[]> entry : inspectionMap.entrySet()){
                double pS = entry.getValue()[0]/max;
                double pL = entry.getValue()[1]/max;
                if(dir == 'X'){
                    if(pS > pL){
                        dir = 'S';
                    }
                    else if(pL > pS){
                        dir = 'L';
                    }
                }
                else if(dir == 'S'){
                    if(pS <= pL){
                        dir = 'O';
                    }
                }
                else if(dir == 'L'){
                    if(pL <= pS){
                        dir = 'O';
                    }
                }
            }
            if(!((target < 0 && dir == 'S') || (target > 0 && dir == 'L'))){
                target = 0;
            }
        }
        return new double[]{target, ratio, probFraction, high, low, high1, low1};
    }

    public static double[] getDecilesU() {
        return decilesU;
    }

    public static double[] getDecilesD() {
        return decilesD;
    }
}
