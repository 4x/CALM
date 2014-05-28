package ai.context.util.trading;

import java.util.TreeMap;

public class DecisionUtil {

    public static double[] getDecision(TreeMap<Double, Double> sFreq, TreeMap<Double, Double> lFreq) {

        if (sFreq.isEmpty()) {
            return new double[]{0, 0};
        }

        double max = (sFreq.firstEntry().getValue() + lFreq.firstEntry().getValue()) / 2;
        double target = 0;
        double ratio = 0;
        double probFraction = 0;

        for (double amplitude : sFreq.keySet()) {

            double freqS = sFreq.get(amplitude);
            double freqL = lFreq.get(amplitude);

            double probS = freqS / max;
            double probL = freqL / max;

            probFraction = Math.max(probL, probS);
            if (probFraction < PositionFactory.minProbFraction) {
                break;
            }

            ratio = Math.max(probL, probS) / Math.min(probL, probS);
            if (amplitude > PositionFactory.cost && ratio > PositionFactory.rewardRiskRatio) {
                int multiplier = 1;
                if (probL < probS) {
                    multiplier = -1;
                }
                target = multiplier * amplitude;
            }
        }
        return new double[]{target, ratio, probFraction};
    }
}
