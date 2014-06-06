package ai.context.util.trading;

import ai.context.learning.DataObject;
import ai.context.learning.neural.NeuronCluster;
import ai.context.util.mathematics.Operations;
import ai.context.util.measurement.OpenPosition;
import com.dukascopy.api.JFException;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

public class DecisionAggregator {

    private static HashSet<OpenPosition> positions = new HashSet<OpenPosition>();

    private static long timeQuantum = 60*60*1000L;
    private static long time = 0;
    private static TreeMap<Long, TreeMap<Double, Double>> timeBasedHistograms = new TreeMap<>();

    private static double latestH;
    private static double latestL;
    private static double latestC;

    private static boolean inLiveTrading = false;
    private static BlackBox blackBox;

    private static int decisionsCollected = 0;
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
        if (executionInstant.getDay() == 0 || executionInstant.getDay() == 6 || exitTime.getDay() == 0 || exitTime.getDay() == 6) {
            return;
        }

        double[] results = PositionFactory.getDecision(data.getTimeStamp(), pivot, histogram, timeSpan);
        if(results == null
                || results[2] < PositionFactory.rewardRiskRatio + 0.25
                /*|| results[3] < (PositionFactory.minProbFraction + 1)/2*/){
            return;
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
            hist.put(entry.getKey(), hist.get(entry.getKey()) + entry.getValue());
        }
    }

    public static  void decide(){
        for(Map.Entry<Long, TreeMap<Double, Double>> entry : timeBasedHistograms.entrySet()){
            OpenPosition position = PositionFactory.getPosition(time, latestC, entry.getValue(), entry.getKey(), false);
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
                positions.add(position);
            }
        }
    }

    public static void checkExit(){
        HashSet<OpenPosition> closed = new HashSet<OpenPosition>();
        HashSet<OpenPosition> newOpen = new HashSet<>();
        for (OpenPosition position : positions) {
            if (position.canCloseOnBar_Pessimistic(time, latestH, latestL, latestC)) {
                closed.add(position);
                PositionFactory.positionClosed(position);
                if(blackBox != null){
                    blackBox.toClose(position.getOrder());
                }

                System.out.println("CHANGE: " + Operations.round(position.getAbsolutePNL(), 4) + " ACCRUED PNL: " +  Operations.round(PositionFactory.getAccruedPnL(), 4) + " CRED: " + Operations.round(position.getCredibility(), 2) + " " + position.getClosingMessage());
            }
        }
        positions.removeAll(closed);
        positions.addAll(newOpen);
    }

    public static void setBlackBox(BlackBox blackBox) {
        DecisionAggregator.blackBox = blackBox;
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
