package ai.context.util.trading;

import ai.context.learning.DataObject;
import ai.context.learning.neural.NeuronCluster;
import ai.context.util.analysis.StatsHolder;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.Operations;
import ai.context.util.measurement.MarketMakerPosition;
import ai.context.util.measurement.OpenPosition;
import com.dukascopy.api.JFException;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

public class DecisionAggregator {

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
    private static MarketMakerDecider marketMakerDecider;
    private static MarketMakerDeciderHistorical marketMakerDeciderHistorical;

    private static int decisionsCollected = 0;
    private static int participants = 0;

    public static void aggregateDecision(DataObject data, double pivot, TreeMap<Double, Double> histogram, long timeSpan, boolean goodTillClosed){

        long time = data.getTimeStamp();
        latestH = data.getValue()[1];
        latestL = data.getValue()[2];
        latestC = data.getValue()[0];

        if(time > DecisionAggregator.time){
            DecisionAggregator.time = time;
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

        double[] results = PositionFactory.getDecision(data.getTimeStamp(), pivot, histogram, timeSpan, null, null, null, 0.5);

        participants++;
        double cred = 1;
        if(PropertiesHolder.normalisationOfSuggestion){
            cred = Math.sqrt(results[0]);
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

    public static  void decide(){
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
                        advice.adjustTimes(DecisionAggregator.getTimeQuantum());
                        marketMakerDecider.addAdvice(advice);
                    }
                    else if(marketMakerDeciderHistorical != null){
                        MarketMakerPosition advice = new MarketMakerPosition(time, latestC + results[4],  latestC - results[5], latestC + results[6],  latestC - results[7], time + entry.getKey());
                        advice.adjustTimes(DecisionAggregator.getTimeQuantum());
                        marketMakerDeciderHistorical.addAdvice(advice);
                    }
                    else {
                        marketMakerPositions.add(new MarketMakerPosition(time, latestC + results[4],  latestC - results[5], latestC + results[6],  latestC - results[7], time + entry.getKey()));
                    }
                }
            }
        }
        participants = 0;
        if(marketMakerDeciderHistorical != null){
            marketMakerDeciderHistorical.setTime(time);
            marketMakerDeciderHistorical.step();
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
        DecisionAggregator.blackBox = blackBox;
    }

    public static void setMarketMakerDecider(MarketMakerDecider marketMakerDecider) {
        DecisionAggregator.marketMakerDecider = marketMakerDecider;
    }

    public static void setMarketMakerDeciderHistorical(MarketMakerDeciderHistorical marketMakerDeciderHistorical) {
        DecisionAggregator.marketMakerDeciderHistorical = marketMakerDeciderHistorical;
    }

    public static void setInLiveTrading(boolean inLiveTrading) {
        DecisionAggregator.inLiveTrading = inLiveTrading;
        System.out.println("Going into LIVE TRADING");
    }

    public static boolean isInLiveTrading() {
        return inLiveTrading;
    }

    public static long getTimeQuantum() {
        return timeQuantum;
    }
}
