package ai.context.util.trading.version_1;

import java.util.*;

public class PositionFactory {

    private static double tradeToCapRatio = 0.05;
    private static double leverage = 1;
    private static double amount = 0;
    public static double cost = 0.0002;
    public static double rewardRiskRatio = 2;
    private static double minTakeProfit = 0.001;
    private static double minTakeProfitVertical = 0.001;

    private static boolean live = false;

    private static boolean verticalRisk = false;

    public static double minProbFraction = 0.8;

    private static long timeSpan = 6 * 3600 * 1000L;

    private static PositionEngine engine;

    private static double accruedPnL = 0;
    private static long totalTrades = 0;
    private static long totalLoss = 0;
    private static long totalProfit = 0;
    private static double sumLoss = 0;
    private static double sumProfit = 0;
    private static double credThreshold = 1.0;

    public static OpenPosition getPosition(long time, double pivot, TreeMap<Double, Double> histogram, long timeSpan, boolean goodTillClosed) {
        double[] results = getDecision(time, pivot, histogram, timeSpan, null, null, null, 0.95);
        if(results == null){
            return null;
        }

        double credibility = results[0];
        double decision = results[1];
        boolean dirL = true;

        if (decision < 0) {
            dirL = false;
        }
        if (decision != 0) {
            OpenPosition position = new OpenPosition(time, pivot, decision, decision, dirL, time + timeSpan, goodTillClosed);
            position.setCredibility(credibility);
            if (!live) {
                position.setAmount(amount * tradeToCapRatio * leverage);
                position.setCost(cost);
                amount -= position.getAmount() / leverage;
            }
            return position;
        }
        return null;
    }

    public static OpenPosition getPosition(long time, double pivot, double[] results, long timeSpan, boolean goodTillClosed) {
        if(results == null){
            return null;
        }

        double credibility = results[0];
        double decision = results[1];
        boolean dirL = true;

        if (decision < 0) {
            dirL = false;
        }
        if (decision != 0) {
            OpenPosition position = new OpenPosition(time, pivot, decision, decision, dirL, time + timeSpan, goodTillClosed);
            position.setCredibility(credibility);
            if (!live) {
                position.setAmount(amount * tradeToCapRatio * leverage);
                position.setCost(cost);
                amount -= position.getAmount() / leverage;
            }
            return position;
        }
        return null;
    }

    public static double[] getDecision(long time, double pivot, TreeMap<Double, Double> histogram, long timeSpan, Double minProbFraction, Double cost, Double rewardRiskRatio, double marketMakerConfidence){
        Date executionInstant = new Date(time);
        Date exitTime = new Date(time + timeSpan);
        if (executionInstant.getDay() == 0 || executionInstant.getDay() == 6 || exitTime.getDay() == 0 || exitTime.getDay() == 6) {
            return null;
        }

        SortedMap<Double, Double> shortMap = histogram.descendingMap().tailMap(pivot, false);
        SortedMap<Double, Double> longMap = histogram.tailMap(pivot, false);

        TreeMap<Double, Double> sFreq = new TreeMap<Double, Double>();
        TreeMap<Double, Double> lFreq = new TreeMap<Double, Double>();

        Double longFreq = 0.0;
        Double shortFreq = 0.0;

        TreeSet<Double> ticks = new TreeSet<Double>();

        TreeMap<Double, Double> sM = new TreeMap<Double, Double>();
        for (Map.Entry<Double, Double> entry : shortMap.entrySet()) {
            ticks.add(pivot - entry.getKey());
            sM.put(pivot - entry.getKey(), entry.getValue());
        }

        TreeMap<Double, Double> lM = new TreeMap<Double, Double>();
        for (Map.Entry<Double, Double> entry : longMap.entrySet()) {
            ticks.add(entry.getKey() - pivot);
            lM.put(entry.getKey() - pivot, entry.getValue());
        }

        if (!lM.isEmpty()) {
            longFreq = lM.firstEntry().getValue();
        }
        if (!sM.isEmpty()) {
            shortFreq = sM.firstEntry().getValue();
        }
        for (Double amplitude : ticks.descendingSet()) {
            if (sM.containsKey(amplitude)) {
                shortFreq += sM.get(amplitude);
            }
            if (lM.containsKey(amplitude)) {
                longFreq += lM.get(amplitude);
            }

            sFreq.put(amplitude, shortFreq);
            lFreq.put(amplitude, longFreq);
        }

        double credibility = (longFreq + shortFreq)/2;
        double[] decision = DecisionUtil.getDecision(sFreq, lFreq, minProbFraction, cost, rewardRiskRatio, marketMakerConfidence);
        return new double[]{credibility, decision[0], decision[1], decision[2], decision[3], decision[4], decision[5], decision[6]};
    }

    public static double getAmount() {
        return amount;
    }

    public static void setLeverage(double leverage) {
        PositionFactory.leverage = leverage;
    }

    public static void setAmount(double amount) {
        PositionFactory.amount = amount;
    }

    public static void positionClosed(OpenPosition position) {
        amount += position.getAmount() / leverage;
        amount += position.getPnL();
        accruedPnL += position.getAbsolutePNL();

        if (position.getAbsolutePNL() > 0) {
            totalProfit++;
            sumProfit += position.getAbsolutePNL();
        } else {
            totalLoss++;
            sumLoss -= position.getAbsolutePNL();
        }
        totalTrades++;
    }

    public static void incrementAmount(double increment) {
        amount += increment;
    }

    public static void setCost(double cost) {
        PositionFactory.cost = cost;
    }

    public static void setRewardRiskRatio(double rewardRiskRatio) {
        PositionFactory.rewardRiskRatio = rewardRiskRatio;
    }

    public static void setMinTakeProfit(double minTakeProfit) {
        PositionFactory.minTakeProfit = minTakeProfit;
    }

    public static void setTradeToCapRatio(double ratio) {
        tradeToCapRatio = ratio;
    }

    public static void setLive(boolean live) {
        PositionFactory.live = live;
    }

    public static void setMinTakeProfitVertical(double minTakeProfitVertical) {
        PositionFactory.minTakeProfitVertical = minTakeProfitVertical;
    }

    public static void setVerticalRisk(boolean verticalRisk) {
        PositionFactory.verticalRisk = verticalRisk;
    }

    public static void setMinProbFraction(double minProbFraction) {
        PositionFactory.minProbFraction = minProbFraction;
    }


    public static void setEngine(PositionEngine engine) {
        PositionFactory.engine = engine;
    }

    public static void setTimeSpan(long timeSpan) {
        PositionFactory.timeSpan = timeSpan;
    }

    public static double getAccruedPnL() {
        return accruedPnL;
    }

    public static long getTimeSpan() {
        return timeSpan;
    }

    public static long getTotalTrades() {
        return totalTrades;
    }

    public static long getTotalLoss() {
        return totalLoss;
    }

    public static long getTotalProfit() {
        return totalProfit;
    }

    public static double getSumLoss() {
        return sumLoss;
    }

    public static double getSumProfit() {
        return sumProfit;
    }

    public static void setCredThreshold(double credThreshold) {
        PositionFactory.credThreshold = credThreshold;
    }

    public static double getCredThreshold() {
        return credThreshold;
    }
}
