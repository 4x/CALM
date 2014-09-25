package ai.context.util.configuration;

import ai.context.util.common.JavaScriptBooleanFilterFunction;

public class PropertiesHolder {
    public static double recencyBias = 2;
    public static double tolerance = 0.01;
    public static int initialSeriesOffset = 2000;
    public static int maxPopulation = 1000;
    public static double copulaToUniversal = 20;
    public static int toleranceSearch = 2;
    public static String dukascopyLogin = "DEMO10037rcNpzEU";
    public static String dukascopyPass = "rcNpz";
    public static int parentsPerNeuron = 3;
    public static int addtionalStimuliPerNeuron = 0;
    public static int coreStimuliPerNeuron = 7;
    public static int totalNeurons = 10;
    public static int generationNeurons = 50;
    public static int httpPort = 8055;
    public static boolean normalisationOfSuggestion = true;
    public static double additionalPassRatio = 0;
    public static long horizonLowerBound = 12 * 60 * 60 * 1000L;
    public static long horizonUpperBound = 24 * 60 * 60 * 1000L;
    public static long neuronLearningPeriod = 10000;
    public static long generationLifespan = 100000;
    public static double marketMakerConfidence = 0.95;
    public static double marketMakerLeeway = 0.8;
    public static double marketMakerAmplitude = 2;
    public static double marketMakerBeyond = 0.0005;
    public static double marketMakerStopLoss = 0.005;
    public static boolean neuronReplacement = false;
    public static boolean tradeNormal = true;
    public static boolean tradeMarketMarker = false;
    public static boolean liveTrading = false;
    public static String startDateTime = "2006.01.01 00:00:00";
    public static boolean tradeSpecial = false;
    public static double tradeToCreditRatio = 0.005;
    public static double maxLeewayAmplitude = 0.00075;
    public static long pointsToLearn = 1000;
    public static long timeQuantum = 30 * 60 * 1000L;
    public static JavaScriptBooleanFilterFunction filterFunction = new JavaScriptBooleanFilterFunction();
    public static String fxFolder = "";
    public static String ticksFile = "EURUSD_Ticks.csv";

    public static String getInfo() {
        return "PropertiesHolder{" +
                "\nrecencyBias=" + recencyBias +
                ",\ntolerance=" + tolerance +
                ",\ninitialSeriesOffset=" + initialSeriesOffset +
                ",\nmaxPopulation=" + maxPopulation +
                ",\ncopulaToUniversal=" + copulaToUniversal +
                ",\ntoleranceSearch=" + toleranceSearch +
                ",\ndukascopyLogin=" + dukascopyLogin +
                ",\ndukascopyPass=" + dukascopyPass +
                ",\nparentsPerNeuron=" + parentsPerNeuron +
                ",\naddtionalStimuliPerNeuron=" + addtionalStimuliPerNeuron +
                ",\ncoreStimuliPerNeuron=" + coreStimuliPerNeuron +
                ",\ntotalNeurons=" + totalNeurons +
                ",\ngenerationNeurons=" + generationNeurons +
                ",\nhttpPort=" + httpPort +
                ",\nnormalisationOfSuggestion=" + normalisationOfSuggestion +
                ",\nadditionalPassRatio=" + additionalPassRatio +
                ",\nhorizonLowerBound=" + horizonLowerBound +
                ",\nhorizonUpperBound=" + horizonUpperBound +
                ",\nneuronLearningPeriod=" + neuronLearningPeriod +
                ",\ngenerationLifespan=" + generationLifespan +
                ",\nmarketMakerConfidence=" + marketMakerConfidence +
                ",\nmarketMakerLeeway=" + marketMakerLeeway +
                ",\nmarketMakerAmplitude=" + marketMakerAmplitude +
                ",\nmarketMakerBeyond=" + marketMakerBeyond +
                ",\nmarketMakerStopLoss=" + marketMakerStopLoss +
                ",\nneuronReplacement=" + neuronReplacement +
                ",\ntradeNormal=" + tradeNormal +
                ",\ntradeMarketMarker=" + tradeMarketMarker +
                ",\nstartDateTime=" + startDateTime +
                ",\nliveTrading=" + liveTrading +
                ",\ntradeSpecial=" + tradeSpecial +
                ",\ntradeToCreditRatio=" + tradeToCreditRatio +
                ",\nmaxLeewayAmplitude=" + maxLeewayAmplitude +
                ",\npointsToLearn=" + pointsToLearn +
                ",\ntimeQuantum=" + timeQuantum +
                ",\nfxFolder=" + fxFolder +
                ",\nticksFile=" + ticksFile +
                '}';
    }
}
