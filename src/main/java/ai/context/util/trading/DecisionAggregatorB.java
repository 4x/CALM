package ai.context.util.trading;

import java.util.TreeMap;

public class DecisionAggregatorB {
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

    public static void aggregateDecision(TreeMap<Integer, Double> pred, double res, double close){

    }
}
