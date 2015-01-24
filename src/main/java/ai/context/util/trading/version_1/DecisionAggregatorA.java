package ai.context.util.trading.version_1;

import ai.context.core.ai.AdditionalStateActionInformation;
import ai.context.learning.DataObject;
import ai.context.learning.neural.NeuronCluster;
import ai.context.util.analysis.StatsHolder;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.Operations;
import ai.context.util.score.NeuronScoreKeeper;
import com.dukascopy.api.JFException;

import java.util.*;

public class DecisionAggregatorA {

    private static HashSet<OpenPosition> positions = new HashSet<>();
    private static HashSet<MarketMakerPosition> marketMakerPositions = new HashSet<>();

    private static long time = 0;
    private static TreeMap<Long, TreeMap<Double, Double>> timeBasedHistogramsPositional = new TreeMap<>();
    private static TreeMap<Long, TreeMap<Double, Double>> timeBasedHistogramsMarketMaker = new TreeMap<>();
    private static TreeMap<Long, AdditionalStateActionInformation> recoveryInformation = new TreeMap<>();
    private static TreeMap<Long, HashMap<Integer, Double[]>> timeBasedNeuronOpinions = new TreeMap<>();

    private static double latestH;
    private static double latestL;
    private static double latestC;

    private static boolean inLiveTrading = false;
    private static BlackBox blackBox;
    private static MarketMakerDeciderTrader marketMakerDeciderLive;
    private static MarketMakerDeciderTrader marketMakerDeciderTest;

    private static int decisionsCollected = 0;
    private static int participantsMM = 0;
    private static int participantsP = 0;

    public static void aggregateDecision(DataObject data, double pivot, TreeMap<Double, Double> histogram, AdditionalStateActionInformation recInfoX, long timeSpan, boolean goodTillClosed){

        long time = data.getTimeStamp();
        latestH = data.getValue()[1];
        latestL = data.getValue()[2];
        latestC = data.getValue()[0];

        if(time > DecisionAggregatorA.time){
            DecisionAggregatorA.time = time;
            timeBasedHistogramsPositional.clear();
            timeBasedHistogramsMarketMaker.clear();
            timeBasedNeuronOpinions.clear();
            recoveryInformation.clear();
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

        double[] results = PositionFactory.getDecision(data.getTimeStamp(), pivot, histogram, timeSpan, null, null, null, PropertiesHolder.marketMakerConfidence);
        if(results[4] + results[5] > PositionFactory.cost * PropertiesHolder.marketMakerAmplitude){
            participantsMM++;
            double cred = 1;
            if(PropertiesHolder.normalisationOfSuggestion){
                cred = results[0];
            } else{
                cred = results[0] / Math.log(1 + results[0]);
            }

            long tExit = (long) Math.ceil((double) timeSpan / (double) PropertiesHolder.timeQuantum) * PropertiesHolder.timeQuantum;
            if(!timeBasedHistogramsMarketMaker.containsKey(tExit)){
                timeBasedHistogramsMarketMaker.put(tExit, new TreeMap<Double, Double>());
                timeBasedNeuronOpinions.put(tExit, new HashMap<Integer, Double[]>());
                recoveryInformation.put(tExit, new AdditionalStateActionInformation());
            }

            TreeMap<Double, Double> hist = timeBasedHistogramsMarketMaker.get(tExit);
            timeBasedNeuronOpinions.get(tExit).put(decisionsCollected, new Double[]{results[4], results[5], Math.log(1 + results[0])});
            double weight = NeuronScoreKeeper.getWeightFor(decisionsCollected);
            for(Map.Entry<Double, Double> entry : histogram.entrySet()){
                if(!hist.containsKey(entry.getKey())){
                    hist.put(entry.getKey(), 0.0);
                }
                hist.put(entry.getKey(), hist.get(entry.getKey()) + weight * entry.getValue()/cred);
            }
            recoveryInformation.get(tExit).incorporate(recInfoX, weight);
        }

        if(PropertiesHolder.tradeNormal){
            if(results[1] > 0){
                participantsP++;
                double cred = 1;
                if(PropertiesHolder.normalisationOfSuggestion){
                    cred = results[0];
                }

                long tExit = (long) Math.ceil((double) timeSpan / (double) PropertiesHolder.timeQuantum) * PropertiesHolder.timeQuantum;
                if(!timeBasedHistogramsPositional.containsKey(tExit)){
                    timeBasedHistogramsPositional.put(tExit, new TreeMap<Double, Double>());
                }

                TreeMap<Double, Double> hist = timeBasedHistogramsPositional.get(tExit);
                for(Map.Entry<Double, Double> entry : histogram.entrySet()){
                    if(!hist.containsKey(entry.getKey())){
                        hist.put(entry.getKey(), 0.0);
                    }
                    hist.put(entry.getKey(), hist.get(entry.getKey()) + entry.getValue()/cred);
                }
            }
        }
    }

    public static void decide() {

        if (PropertiesHolder.tradeNormal) {
            for (Map.Entry<Long, TreeMap<Double, Double>> entry : timeBasedHistogramsPositional.entrySet()) {

                double[] results = PositionFactory.getDecision(time, latestC, entry.getValue(), entry.getKey(), null, null, null, PropertiesHolder.marketMakerConfidence);
                OpenPosition position = PositionFactory.getPosition(time, latestC, results, entry.getKey(), false);
                if (position != null) {
                    if (inLiveTrading) {
                        int startHour = new Date().getHours();
                        if (position.getCredibility() > PositionFactory.getCredThreshold()) {
                            try {
                                if (blackBox != null) {
                                    blackBox.onDecision(position);
                                }
                            } catch (JFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    position.setParticipants(participantsP);
                    positions.add(position);
                }
            }
            participantsP = 0;
        }

        if (PropertiesHolder.tradeMarketMarker) {

            for (Map.Entry<Long, TreeMap<Double, Double>> entry : timeBasedHistogramsMarketMaker.entrySet()) {

                double[] results = PositionFactory.getDecision(time, latestC, entry.getValue(), entry.getKey(), null, null, null, PropertiesHolder.marketMakerConfidence);

                if (results[4] + results[5] > PositionFactory.cost * PropertiesHolder.marketMakerAmplitude) {

                    MarketMakerPosition advice = new MarketMakerPosition(time, latestC, latestC + results[4], latestC - results[5], latestC + results[6], latestC - results[7], time + entry.getKey(), recoveryInformation.get(entry.getKey()));
                    advice.adjustTimes(PropertiesHolder.timeQuantum);
                    advice.attributes.put("cred", results[0]);
                    advice.constituentOpinions = timeBasedNeuronOpinions.get(entry.getKey());
                    for (int i = 0; i < DecisionUtil.getDecilesU().length; i++) {
                        advice.attributes.put("dU_" + i, DecisionUtil.getDecilesU()[i]);
                    }

                    for (int i = 0; i < DecisionUtil.getDecilesD().length; i++) {
                        advice.attributes.put("dD_" + i, DecisionUtil.getDecilesD()[i]);
                    }

                    if (inLiveTrading && marketMakerDeciderLive != null) {
                        marketMakerDeciderLive.addAdvice(advice);
                    } else if (marketMakerDeciderTest != null) {
                       marketMakerDeciderTest.addAdvice(advice);
                    } else {
                        marketMakerPositions.add(new MarketMakerPosition(time, latestC, latestC + results[4], latestC - results[5], latestC + results[6], latestC - results[7], time + entry.getKey(), recoveryInformation.get(entry.getKey())));
                    }
                }
            }
            participantsMM = 0;
            if (!inLiveTrading && marketMakerDeciderTest != null) {
                marketMakerDeciderTest.step();
            }
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
        DecisionAggregatorA.blackBox = blackBox;
    }

    public static void setMarketMakerDeciderLive(MarketMakerDeciderTrader marketMakerDeciderLive) {
        DecisionAggregatorA.marketMakerDeciderLive = marketMakerDeciderLive;
    }

    public static void setMarketMakerDeciderTest(MarketMakerDeciderTrader marketMakerDeciderTest) {
        DecisionAggregatorA.marketMakerDeciderTest = marketMakerDeciderTest;
    }

    public static void setInLiveTrading(boolean inLiveTrading) {
        DecisionAggregatorA.inLiveTrading = inLiveTrading;
        System.out.println("Going into LIVE TRADING");
        NeuronCluster.getInstance().getEmailSendingService().queueEmail(
                "algo@balgobin.london",
                "hans@balgobin.london",
                "Algo Going Live", "We are now in live trading....");

    }

    public static boolean isInLiveTrading() {
        return inLiveTrading;
    }

    public static Collection<MarketMakerPosition> getMarketMakerPositions() {
        if (inLiveTrading && marketMakerDeciderLive != null) {
            return marketMakerDeciderLive.getAdvices();
        } else if (marketMakerDeciderTest != null) {
            return marketMakerDeciderTest.getAdvices();
        }
        return marketMakerPositions;
    }
}
