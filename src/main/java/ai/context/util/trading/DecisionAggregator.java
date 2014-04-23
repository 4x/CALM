package ai.context.util.trading;

import ai.context.learning.DataObject;
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

    public static void aggregateDecision(DataObject data, double pivot, TreeMap<Double, Double> histogram, long timeSpan, boolean goodTillClosed){
        long time = data.getTimeStamp();
        latestH = data.getValue()[1];
        latestL = data.getValue()[2];
        latestC = data.getValue()[0];

        Date executionInstant = new Date(time);
        Date exitTime = new Date(time + timeSpan);
        if (executionInstant.getDay() == 0 || executionInstant.getDay() == 6 || exitTime.getDay() == 0 || exitTime.getDay() == 6) {
            return;
        }

        if(DecisionAggregator.time == 0){
            DecisionAggregator.time = time;
        }
        else if(time > DecisionAggregator.time){
            DecisionAggregator.time = time;
            checkExit();
            decide();
            timeBasedHistograms.clear();
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
        if (positions.size() < 100) {
            for(Map.Entry<Long, TreeMap<Double, Double>> entry : timeBasedHistograms.entrySet()){
                OpenPosition position = PositionFactory.getPosition(time, latestC, entry.getValue(), entry.getKey(), false);
                if (position != null) {
                    if (inLiveTrading) {
                        try {
                            blackBox.onDecision(position);
                        } catch (JFException e) {
                            e.printStackTrace();
                        }
                    } else {
                        positions.add(position);
                    }
                }
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

                System.out.println(position.getClosingMessage() + " CHANGE: " + position.getPnL() + " CAPITAL: " + PositionFactory.getAmount() + " ACCRUED PNL: " + PositionFactory.getAccruedPnL());
            }
            else{
                long timeSpan = position.getGoodTillTime() - time;
                if(timeBasedHistograms.containsKey(timeSpan)){
                    TreeMap<Double, Double> prediction = timeBasedHistograms.remove(timeSpan);
                    OpenPosition newPosition = PositionFactory.getPosition(time, latestC, prediction, timeSpan, false);
                    if (position != null) {
                        if (inLiveTrading) {
                            try {
                                blackBox.onDecision(position);
                            } catch (JFException e) {
                                e.printStackTrace();
                            }
                        } else {
                            if(position.isLong() != newPosition.isLong()){
                                position.close(latestC, time);
                                closed.add(position);
                                PositionFactory.positionClosed(position);

                                System.out.println(position.getClosingMessage() + " CHANGE: " + position.getPnL() + " CAPITAL: " + PositionFactory.getAmount() + " ACCRUED PNL: " + PositionFactory.getAccruedPnL());
                            }
                            newOpen.add(position);
                        }
                    }
                }
            }
        }
        positions.removeAll(closed);
        positions.addAll(newOpen);
    }
}
