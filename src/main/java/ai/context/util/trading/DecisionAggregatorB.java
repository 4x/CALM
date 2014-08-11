package ai.context.util.trading;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.util.mathematics.Operations;

import java.util.Map;
import java.util.TreeMap;

public class DecisionAggregatorB {
    private static long timeQuantum = 30*60*1000L;
    private static long time = 0;
    private static TreeMap<Long, TreeMap<Double, Double>> timeBasedHistograms = new TreeMap<>();

    private static boolean inLiveTrading = false;

    private static Feed priceFeed;
    private static int decisionsCollected = 0;
    private static FeedObject pricePoint;

    public static void setPriceFeed(String priceFeedFile, String startDate){
        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        priceFeed = new CSVFeed(priceFeedFile, "yyyy.MM.dd HH:mm:ss.SSS", typesPrice, startDate);
    }

    public static void aggregateDecision(long time, TreeMap<Integer, Double> pred, double res, double close, long horizon){
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

            double level = Operations.round((entry.getKey() * res) + close, 4);
            if(!hist.containsKey(level)){
                hist.put(level, 0.0);
            }
            hist.put(level, hist.get(level) + entry.getValue()/cred);
        }
    }

    private static void act(long tStart, long tEnd) {
        checkExit();
        newOrders();
        step(tStart, tEnd);
    }

    private static void checkExit() {
        while(timeBasedHistograms.firstKey() < time){
            TreeMap<Double, Double> hist = timeBasedHistograms.remove(timeBasedHistograms.firstKey());
        }
    }

    private static void newOrders() {

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

    private static void onTick(long lastTime, double lastBid, double lastAsk) {
        if(lastTime > time){

        }
    }

    public static void setPriceFeed(Feed priceFeed) {
        DecisionAggregatorB.priceFeed = priceFeed;
    }

    public static double getClose(){
        return (lastBid + lastAsk)/2;
    }
}
