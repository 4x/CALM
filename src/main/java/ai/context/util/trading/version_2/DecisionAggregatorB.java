package ai.context.util.trading.version_2;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.util.mathematics.Operations;
import ai.context.util.trading.version_1.PositionFactory;

import java.util.*;

public class DecisionAggregatorB {
    private static long timeQuantum = 30*60*1000L;
    private static long time = 0;
    private static TreeMap<Long, TreeMap<Double, Double>> timeBasedHistograms = new TreeMap<>();

    private static HashSet<Position> positions = new HashSet<>();

    private static boolean inLiveTrading = false;

    private static Feed priceFeed;
    private static int decisionsCollected = 0;
    private static FeedObject pricePoint;

    private static double close;

    private static double pnl = 0;

    public static void setPriceFeed(String priceFeedFile, String startDate){
        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        priceFeed = new CSVFeed(priceFeedFile, "yyyy.MM.dd HH:mm:ss.SSS", typesPrice, startDate);
    }

    public static void aggregateDecision(long time, TreeMap<Integer, Double> pred, double res, double close, long horizon){
        DecisionAggregatorB.close = close;
        if(time > DecisionAggregatorB.time){
            DecisionAggregatorB.time = time;
            timeBasedHistograms.clear();
            decisionsCollected = 0;
        }

        long tExit = time + horizon;
        if(!timeBasedHistograms.containsKey(tExit)){
            timeBasedHistograms.put(tExit, new TreeMap<Double, Double>());
        }

        double cred = 0;
        for(double w : pred.values()){
            cred += w;
        }

        TreeMap<Double, Double> hist = timeBasedHistograms.get(tExit);
        for(Map.Entry<Integer, Double> entry : pred.entrySet()){

            double level = Operations.round((entry.getKey() * res), 4);
            if(!hist.containsKey(level)){
                hist.put(level, 0.0);
            }
            hist.put(level, hist.get(level) + entry.getValue()/cred);
        }
    }

    public static void act(long tStart, long tEnd) {
        newOrders();
        step(tStart, tEnd);
    }

    private static void newOrders() {
        for(Map.Entry<Long, TreeMap<Double, Double>> horizonEntry : timeBasedHistograms.entrySet()){
            long expiry = horizonEntry.getKey();
            double decision = getDecision(horizonEntry.getValue(), 0.5, 2);
            if(Math.abs(decision) > 5 * PositionFactory.cost){
                int dir = 1;
                if(decision < 0){
                    dir = -1;
                }
                positions.add(new Position(expiry, time, close, dir, close + decision, close - decision));
            }
        }
    }

    private static void onTick(long lastTime, double lastBid, double lastAsk) {
        HashSet<Position> toRemove = new HashSet<>();
        for(Position position : positions){
            if(position.notifyQuote(lastTime, lastBid, lastAsk)){

                pnl += position.getPnL();
                String report = new Date(lastTime) + ": [" + position.getHorizon() + "] ";
                if(position.getPnL() > 0){
                    report += "PROFIT ";
                }
                else {
                    report += "LOSS ";
                }
                report += "CHANGE: " + position.getPnL() + " ACC: " + pnl;
                System.out.print(report);

                toRemove.add(position);
            }
        }
        positions.removeAll(toRemove);
    }

    private static long lastTime = 0;
    private static double lastBid = -1;
    private static double lastAsk = -1;

    public static void step(long tStart, long tEnd){
        while (true){
            if(pricePoint != null){
                long t = pricePoint.getTimeStamp();
                lastAsk = (double) ((Object[])pricePoint.getData())[0];
                lastBid = (double) ((Object[])pricePoint.getData())[1];
                if(t > tEnd){
                    break;
                }

                if(t >= tStart){
                    onTick(t, lastBid, lastAsk);
                }
            }
            pricePoint = priceFeed.readNext(null);
        }
    }

    public static void setPriceFeed(Feed priceFeed) {
        DecisionAggregatorB.priceFeed = priceFeed;
    }

    public static double getClose(){
        return (lastBid + lastAsk)/2;
    }

    public static double getDecision(TreeMap<Double, Double> pred, double minProb, double rewardToRiskRatio){
        double decision = 0;
        SortedMap<Double, Double> sMap = pred.headMap(0.00001);
        SortedMap<Double, Double> lMap = pred.tailMap(-0.00001);

        TreeSet<Double> ticks = new TreeSet<>();
        for(Double v : sMap.keySet()){
            ticks.add(-v);
        }
        for(Double v : lMap.keySet()){
            ticks.add(v);
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
            double ratio = Math.max(pL, pS) / Math.min(pL, pS);

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

        SortedMap<Double, Double[]> inspectionMap = cum.headMap(Math.abs(decision)).tailMap(2*decision/3);
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
        return decision;
    }
}
