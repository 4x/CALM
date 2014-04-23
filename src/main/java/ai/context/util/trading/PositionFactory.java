package ai.context.util.trading;

import ai.context.util.measurement.OpenPosition;

import java.util.*;

public class PositionFactory {

    private static double tradeToCapRatio = 0.05;
    private static double leverage = 1;
    private static double amount = 1000000.0;
    public static double cost = 0.0002;
    public static double rewardRiskRatio = 3.0;
    private static double minTakeProfit = 0.001;
    private static double minTakeProfitVertical = 0.001;

    private static boolean live = false;

    private static boolean verticalRisk = false;

    public static double minProbFraction = 0.75;

    private static long timeSpan = 6 * 3600 * 1000L;

    private static PositionEngine engine;

    //private static DecisionHistogram decisionHistogram = new DecisionHistogram();


    private static double accruedPnL = 0;
    private static long totalTrades = 0;
    private static long totalLoss = 0;
    private static long totalProfit = 0;
    private static double sumLoss = 0;
    private static double sumProfit = 0;

    public static OpenPosition getPosition(long time, double pivot, TreeMap<Double, Double> histogram, long timeSpan, boolean goodTillClosed) {
        if ((amount * tradeToCapRatio * leverage) < 1000) {
            return null;
        }

        Date executionInstant = new Date(time);
        Date exitTime = new Date(time + timeSpan);
        if (executionInstant.getDay() == 0 || executionInstant.getDay() == 6 || exitTime.getDay() == 0 || exitTime.getDay() == 6) {
            return null;
        }

        SortedMap<Double, Double> shortMap = histogram.descendingMap().tailMap(pivot, false);
        SortedMap<Double, Double> longMap = histogram.tailMap(pivot, false);
        //TreeMap<Double, Double> directionalFreq = new TreeMap<Double, Double>();

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

            //directionalFreq.put(amplitude, longFreq - shortFreq);
        }

        double decision = DecisionUtil.getDecision(sFreq, lFreq);
        boolean dirL = true;
        //decisionHistogram.update(sFreq, lFreq, minTakeProfit, rewardRiskRatio, Math.abs(decision));
        if (decision < 0) {
            dirL = false;
        }
        if (decision != 0) {
            OpenPosition position = new OpenPosition(time, pivot, decision, decision * 1.5, dirL, time + timeSpan, goodTillClosed);
            if (!live) {
                position.setAmount(amount * tradeToCapRatio * leverage);
                position.setCost(cost);
                amount -= position.getAmount() / leverage;
            }
            return position;
        }

        /*if(engine != null){
            return engine.getPosition(time, pivot, histogram);
        }

        double multiplier = 1.0;

        List<Double> tickList = new ArrayList<Double>();
        tickList.addAll(ticks);
        int i = 0;

        if(verticalRisk){
            boolean dirLong = true;
            int crossings = 0;
            multiplier = 1.0;

            for(Double tick : tickList){
                if(tick > minTakeProfitVertical){
                    if(directionalFreq.get(tick) > 0){
                        if(crossings == 0){
                            crossings++;
                        }
                        else if(!dirLong){
                            crossings++;
                        }
                        dirLong = true;
                    }
                    else{
                        if(crossings == 0){
                            crossings++;
                        }
                        else if(dirLong){
                            crossings++;
                        }
                        dirLong = false;
                    }
                }
            }
            if(!dirLong){
                multiplier = -1.0;
            }

            if(crossings == 1){
                double maxPayoff = 0;
                double amp = 0;
                double maxProb = 0;
                for(Double tick : tickList){
                    double longProb = lFreq.get(tick);
                    double shortProb = sFreq.get(tick);
                    maxProb = Math.max(maxProb, shortProb);
                    maxProb = Math.max(maxProb, longProb);

                    if(tick > minTakeProfitVertical){
                        if(dirLong){
                            if(longProb > shortProb * rewardRiskRatio && longProb/maxProb > minProbFraction){
                                double payoff = (longProb - shortProb) * tick;
                                if(payoff > maxPayoff){
                                    maxPayoff = payoff;
                                    amp = tick;
                                }
                            }
                        }
                        else {
                            if(shortProb > longProb * rewardRiskRatio && shortProb/maxProb > minProbFraction){
                                double payoff = (shortProb - longProb) * tick;
                                if(payoff > maxPayoff){
                                    maxPayoff = payoff;
                                    amp = tick;
                                }
                            }
                        }
                    }
                }

                if(amp > minTakeProfitVertical){
                    OpenPosition position = new OpenPosition(time, pivot, multiplier * amp, multiplier * amp, dirLong, time + timeSpan);
                    if(!live){
                        position.setAmount(amount * tradeToCapRatio * leverage);
                        position.setCost(cost);
                        amount -= position.getAmount()/leverage;
                    }
                    return position;
                }
            }
            else {
                return null;
            }
        }
        else{
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
                                if(found && tpAmp > minTakeProfit && amount > 100){

                                    Double min = null;
                                    Double slAmp = null;
                                    for(Map.Entry<Double, Double> entry : sFreq.tailMap(tick, true).entrySet()){
                                        if(entry.getKey() > tpAmp/rewardRiskRatio){
                                            break;
                                        }
                                        if(min == null){
                                            min = entry.getValue() * entry.getKey();
                                            slAmp = entry.getKey();
                                        }
                                        else {
                                            if(min < (entry.getValue() * entry.getKey())){
                                                min = entry.getValue() * entry.getKey();
                                                slAmp = entry.getKey();
                                            }
                                        }
                                    }
                                    if(slAmp != null){
                                        OpenPosition position = new OpenPosition(time, pivot, multiplier * tpAmp, multiplier * slAmp, true, time + timeSpan);
                                        if(!live){
                                            position.setAmount(amount * tradeToCapRatio * leverage);
                                            position.setCost(cost);
                                            amount -= position.getAmount()/leverage;
                                        }
                                        return position;
                                    }
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
                                if(found && tpAmp > minTakeProfit && amount > 100){

                                    Double min = null;
                                    Double slAmp = null;
                                    for(Map.Entry<Double, Double> entry : lFreq.tailMap(tick, true).entrySet()){
                                        if(entry.getKey() > tpAmp/rewardRiskRatio){
                                            break;
                                        }
                                        if(min == null){
                                            min = entry.getValue() * entry.getKey();
                                            slAmp = entry.getKey();
                                        }
                                        else {
                                            if(min < (entry.getValue() * entry.getKey())){
                                                min = entry.getValue() * entry.getKey();
                                                slAmp = entry.getKey();
                                            }
                                        }
                                    }
                                    if(slAmp != null){
                                        OpenPosition position = new OpenPosition(time, pivot, multiplier * tpAmp, multiplier * slAmp, false, time + timeSpan);
                                        if(!live){
                                            position.setAmount(amount * tradeToCapRatio * leverage);
                                            position.setCost(cost);
                                            amount -= position.getAmount()/leverage;
                                        }
                                        return position;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }*/
        return null;
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
        accruedPnL += position.getPnL();

        if (position.getPnL() > 0) {
            totalProfit++;
            sumProfit += position.getPnL();
        } else {
            totalLoss++;
            sumLoss -= position.getPnL();
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
}
