package ai.context.util.trading;

import ai.context.util.configuration.PropertiesHolder;

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
                    && amplitude > PositionFactory.cost * PropertiesHolder.marketMakerAmplitude/2) {

                double thisPayoff = amplitude * Math.abs(probL - probS);
                double initialMult = Math.signum(target);
                if(thisPayoff > payoff){
                    int multiplier = 1;
                    if (probL < probS) {
                        multiplier = -1;
                    }
                    target = multiplier * amplitude;
                    payoff = thisPayoff;

                    if(target * initialMult < 0){
                        flip++;
                    }
                }
            }
        }

        if(flip > 1){
            target = 0;
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
