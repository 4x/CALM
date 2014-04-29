package ai.context.util.trading;

import java.util.TreeMap;

public class DecisionUtil {

    public static double getDecision(TreeMap<Double, Double> sFreq, TreeMap<Double, Double> lFreq) {

        if (sFreq.isEmpty()) {
            return 0;
        }

        double max = (sFreq.firstEntry().getValue() + lFreq.firstEntry().getValue()) / 2;
        double payoff = 0;
        double target = 0;

        for (double amplitude : sFreq.keySet()) {

            double freqS = sFreq.get(amplitude);
            double freqL = lFreq.get(amplitude);

            double probS = freqS / max;
            double probL = freqL / max;

            if (Math.max(probL, probS) < PositionFactory.minProbFraction) {
                break;
            }

            if (amplitude > 3 * PositionFactory.cost && Math.max(probL, probS) / Math.min(probL, probS) > PositionFactory.rewardRiskRatio) {
                double thisPayoff = Math.abs(probL - probS) * amplitude;
                int multiplier = 1;
                if (probL < probS) {
                    multiplier = -1;
                }
                if (thisPayoff > payoff) {
                    payoff = thisPayoff;
                    target = multiplier * amplitude;
                }
            }
        }

        return target;
    }
}
