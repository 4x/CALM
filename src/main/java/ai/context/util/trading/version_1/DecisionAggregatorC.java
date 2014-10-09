package ai.context.util.trading.version_1;

import ai.context.learning.DataObject;
import ai.context.learning.neural.NeuronCluster;
import ai.context.util.analysis.StatsHolder;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.Operations;
import com.dukascopy.api.JFException;

import java.util.*;

public class DecisionAggregatorC {
    private static HashSet<OpenPosition> positions = new HashSet<>();
    private static HashSet<MarketMakerPosition> marketMakerPositions = new HashSet<>();

    private static long timeQuantum = 30*60*1000L;
    private static long time = 0;
    private static TreeMap<Long, TreeMap<Double, Double>> timeBasedHistograms = new TreeMap<>();

    private static double latestH;
    private static double latestL;
    private static double latestC;

    private static boolean inLiveTrading = false;
    private static BlackBox blackBox;
    private static MarketMakerDeciderTrader marketMakerDecider;
    private static MarketMakerDeciderTrader marketMakerDeciderTrader;

    private static int decisionsCollected = 0;
    private static int participants = 0;

    public static void aggregateDecision(DataObject data, double pivot, TreeMap<Double, Double> histogram, long timeSpan, boolean goodTillClosed){

        long time = data.getTimeStamp();
        latestH = data.getValue()[1];
        latestL = data.getValue()[2];
        latestC = data.getValue()[0];

        if(time > DecisionAggregatorC.time){
            DecisionAggregatorC.time = time;
            timeBasedHistograms.clear();
            decisionsCollected = 0;
        }

        decisionsCollected++;
        if(decisionsCollected == NeuronCluster.getInstance().size()){
            checkExit();
            decide();
        }

        Date executionInstant = new Date(time);
        Date exitTime = new Date(time + timeSpan);
        if (histogram == null || executionInstant.getDay() == 0 || executionInstant.getDay() == 6 || exitTime.getDay() == 0 || exitTime.getDay() == 6) {
            return;
        }


        double decision = getDecision(histogram, PositionFactory.minProbFraction, PositionFactory.rewardRiskRatio);
        double[] results = new double[10];

        if(results[4] + results[5] < PositionFactory.cost * PropertiesHolder.marketMakerAmplitude){
            return;
        }

        if(PropertiesHolder.tradeNormal){
            if(results[1] == 0){
                return;
            }
        }

        participants++;
        double cred = 1;
        if(PropertiesHolder.normalisationOfSuggestion){
            cred = 0;
            for(double w : histogram.values()){
                cred += w;
            }
        }

        long tExit = (long) Math.ceil((double) timeSpan / (double) timeQuantum) * timeQuantum;
        if(!timeBasedHistograms.containsKey(tExit)){
            timeBasedHistograms.put(tExit, new TreeMap<Double, Double>());
        }

        TreeMap<Double, Double> hist = timeBasedHistograms.get(tExit);
        for(Map.Entry<Double, Double> entry : histogram.entrySet()){
            if(!hist.containsKey(entry.getKey())){
                hist.put(entry.getKey(), 0.0);
            }
            hist.put(entry.getKey(), hist.get(entry.getKey()) + entry.getValue()/cred);
        }
    }

    public static void decide(){
        for(Map.Entry<Long, TreeMap<Double, Double>> entry : timeBasedHistograms.entrySet()){

            double[] results = PositionFactory.getDecision(time, latestC, entry.getValue(), entry.getKey(),null, null, null, PropertiesHolder.marketMakerConfidence);

            if(PropertiesHolder.tradeNormal){
                OpenPosition position = PositionFactory.getPosition(time, latestC, results, entry.getKey(), false);
                if (position != null) {
                    if (inLiveTrading) {
                        int startHour = new Date().getHours();
                        if(position.getCredibility() > PositionFactory.getCredThreshold()){
                            try {
                                if(blackBox != null){
                                    blackBox.onDecision(position);
                                }
                            } catch (JFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    position.setParticipants(participants);
                    positions.add(position);
                }
            }

            if(PropertiesHolder.tradeMarketMarker){
                if(results[4] + results[5] > PositionFactory.cost * PropertiesHolder.marketMakerAmplitude){
                    if(inLiveTrading && marketMakerDecider != null){
                        MarketMakerPosition advice = new MarketMakerPosition(time, latestC + results[4],  latestC - results[5], latestC + results[6],  latestC - results[7], time + entry.getKey());
                        advice.adjustTimes(PropertiesHolder.timeQuantum);
                        marketMakerDecider.addAdvice(advice);
                    }
                    else if(marketMakerDeciderTrader != null){
                        MarketMakerPosition advice = new MarketMakerPosition(time, latestC + results[4],  latestC - results[5], latestC + results[6],  latestC - results[7], time + entry.getKey());
                        advice.adjustTimes(PropertiesHolder.timeQuantum);
                        advice.attributes.put("cred", results[0]);

                        for(int i = 0; i < DecisionUtil.getDecilesU().length; i++){
                            advice.attributes.put("dU_" + i, DecisionUtil.getDecilesU()[i]);
                        }

                        for(int i = 0; i < DecisionUtil.getDecilesD().length; i++){
                            advice.attributes.put("dD_" + i, DecisionUtil.getDecilesD()[i]);
                        }

                        marketMakerDeciderTrader.addAdvice(advice);
                    }
                    else {
                        marketMakerPositions.add(new MarketMakerPosition(time, latestC + results[4],  latestC - results[5], latestC + results[6],  latestC - results[7], time + entry.getKey()));
                    }
                }
            }
        }
        participants = 0;
        if(marketMakerDeciderTrader != null){
            marketMakerDeciderTrader.setTime(time);
            marketMakerDeciderTrader.step();
        }
    }

    public static void checkExit(){
        HashSet<OpenPosition> closed = new HashSet<OpenPosition>();
        for (OpenPosition position : positions) {
            if (position.canCloseOnBar_Pessimistic(time, latestH, latestL, latestC)) {
                closed.add(position);
                PositionFactory.positionClosed(position);
                if(blackBox != null){
                    blackBox.toClose(position.getOrder());
                }

                System.out.println("CHANGE: " + Operations.round(position.getAbsolutePNL(), 4) + " ACCRUED PNL: " +  Operations.round(PositionFactory.getAccruedPnL(), 4) + " CRED: " + Operations.round(position.getCredibility(), 2) + " Participants: " + position.getParticipants() + " " + position.getClosingMessage());
            }
        }
        positions.removeAll(closed);

        HashSet<MarketMakerPosition> mClosed = new HashSet<>();
        for (MarketMakerPosition position : marketMakerPositions) {
            if (position.notify(latestH, latestL, latestC, time)) {
                StatsHolder.pnLMarketMaker += position.getPnL();
                String state = "P";
                if(position.getPnL() < 0){
                    state = "L";
                }
                else if(position.getPnL() == 0){
                    state = "N";
                }
                System.out.println("MMP_CHANGE: " + state + " " + Operations.round(position.getPnL(), 4) + " ACCRUED PNL: " +  Operations.round(StatsHolder.pnLMarketMaker, 4) + " " + position.getClosingMessage());
                mClosed.add(position);
            }
        }
        marketMakerPositions.removeAll(mClosed);
    }

    public static void setBlackBox(BlackBox blackBox) {
        DecisionAggregatorC.blackBox = blackBox;
    }

    public static void setMarketMakerDecider(MarketMakerDeciderTrader marketMakerDecider) {
        DecisionAggregatorC.marketMakerDecider = marketMakerDecider;
    }

    public static void setMarketMakerDeciderTrader(MarketMakerDeciderTrader marketMakerDeciderTrader) {
        DecisionAggregatorC.marketMakerDeciderTrader = marketMakerDeciderTrader;
    }

    public static void setInLiveTrading(boolean inLiveTrading) {
        DecisionAggregatorC.inLiveTrading = inLiveTrading;
        System.out.println("Going into LIVE TRADING");
    }

    public static boolean isInLiveTrading() {
        return inLiveTrading;
    }

    public static long getTimeQuantum() {
        return timeQuantum;
    }

    public static HashSet<MarketMakerPosition> getMarketMakerPositions() {
        return marketMakerPositions;
    }

    public static double getDecision(TreeMap<Double, Double> pred, double minProb, double rewardToRiskRatio){
        double decision = 0;
        SortedMap<Double, Double> sMap = pred.headMap(0.00001);
        SortedMap<Double, Double> lMap = pred.tailMap(-0.00001);

        if(sMap.containsKey(0D)){
            sMap.put(0D, sMap.get(0D)/4);
        }
        if(lMap.containsKey(0D)){
            lMap.put(0D, lMap.get(0D)/4);
        }

        TreeSet<Double> ticks = new TreeSet<>();
        for(Double v : sMap.keySet()){
            ticks.add(Math.abs(v));
        }
        for(Double v : lMap.keySet()){
            ticks.add(Math.abs(v));
        }

        TreeMap<Double, Double[]> cum = new TreeMap<>();
        double cumS = 0;
        double cumL = 0;

        for(double tick : ticks.descendingSet()){
            if(sMap.containsKey(-tick)){
                cumS += sMap.get(-tick);
            }
            if(lMap.containsKey(tick)){
                cumL += lMap.get(tick);
            }

            if(tick < 0.00001){
                cumL = Math.max(cumL, cumS);
                cumS = cumL;
            }
            Double[] comparison = new Double[]{cumS, cumL};
            cum.put(tick, comparison);
        }

        double max = cumL;

        for(Map.Entry<Double, Double[]> entry : cum.entrySet()){
            double amp = entry.getKey();
            double pS = entry.getValue()[0]/max;
            double pL = entry.getValue()[1]/max;
            double prob = Math.max(pS, pL);
            double min = Math.min(pL, pS);
            double ratio = 10;
            if(min > 0){
                ratio = prob / min;
            }

            if(prob >= minProb && ratio > rewardToRiskRatio){
                double mult = 1;
                if(pS > pL){
                    mult = -1;
                }
                decision = amp * mult;
            }
            else if(minProb > prob){
                break;
            }
        }

        if(Math.abs(decision) > 0){
            SortedMap<Double, Double[]> inspectionMap = cum.headMap(Math.abs(decision)).tailMap(Math.abs(9*decision/10));
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
            if(!((decision < 0 && dir == 'S') || (decision > 0 && dir == 'L'))){
                decision = 0;
            }
        }
        return decision;
    }
}
