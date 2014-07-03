package ai.context.util.trading;

import java.util.TreeMap;

public class DecisionUtil {

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

        double max = (sFreq.firstEntry().getValue() + lFreq.firstEntry().getValue()) / 2;
        double target = 0;
        double ratio = 0;
        double probFraction = 0;
        double payoff = 0;
        double high = 0;
        double low = 0;
        double high1 = 0;
        double low1 = 0;

        for (double amplitude : sFreq.keySet()) {

            double freqS = sFreq.get(amplitude);
            double freqL = lFreq.get(amplitude);

            double probS = freqS / max;
            double probL = freqL / max;

            if(probS > marketMakerConfidence){
                low = amplitude;
            }
            if(probL > marketMakerConfidence){
                high = amplitude;
            }

            if(probS > 0.75){
                low1 = amplitude;
            }
            if(probL > 0.75){
                high1 = amplitude;
            }


            probFraction = Math.max(probL, probS);
            if (probFraction < minProbFraction) {
                break;
            }

            ratio = Math.max(probL, probS) / Math.min(probL, probS);
            if (amplitude > cost && ratio > rewardRiskRatio) {
                double thisPayoff = amplitude * Math.abs(probL - probS);
                if(thisPayoff > payoff){
                    int multiplier = 1;
                    if (probL < probS) {
                        multiplier = -1;
                    }
                    target = multiplier * amplitude;
                    payoff = thisPayoff;
                }
            }
        }
        return new double[]{target, ratio, probFraction, high, low, high1, low1};
    }
}
