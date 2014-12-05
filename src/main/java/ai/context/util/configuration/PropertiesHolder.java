package ai.context.util.configuration;

import ai.context.util.common.JavaScriptBooleanFilterFunction;

import java.util.TreeMap;

public class PropertiesHolder {
    public static boolean isLiveAccount = false;
    public static double recencyBias = 2;
    public static double tolerance = 0.01;
    public static int initialSeriesOffset = 500;
    public static int maxPopulation = 500;
    public static double copulaToUniversal = 20;
    public static int toleranceSearch = 2;
    public static String dukascopyLogin = "DEMO10037rcNpzEU";
    public static String dukascopyPass = "rcNpz";
    public static int parentsPerNeuron = 3;
    public static int addtionalStimuliPerNeuron = 0;
    public static int coreStimuliPerNeuron = 7;
    public static int totalNeurons = 10;
    public static int totalRandomisedNeurons = 10;
    public static int generationNeurons = 50;
    public static int httpPort = 8055;
    public static boolean normalisationOfSuggestion = true;
    public static double additionalPassRatio = 0;
    public static long horizonLowerBound = 12 * 60 * 60 * 1000L;
    public static long horizonUpperBound = 24 * 60 * 60 * 1000L;
    public static long neuronLearningPeriod = 500;
    public static long generationLifespan = 5000;
    public static long minGenerationLifespan = 5000;
    public static double marketMakerConfidence = 0.8;
    public static double marketMakerLeeway = 0.5;
    public static double marketMakerAmplitude = 5;
    public static double marketMakerBeyond = 0.0002;
    public static double marketMakerStopLoss = 0.0015;
    public static boolean neuronReplacement = false;
    public static boolean tradeNormal = false;
    public static boolean tradeMarketMarker = true;
    public static boolean liveTrading = true;
    public static String startDateTime = "2006.01.01 00:00:00";
    public static boolean tradeSpecial = false;
    public static double tradeToCreditRatio = 0.05;
    public static double maxLeewayAmplitude = 0.0005;
    public static long pointsToLearn = 1000;
    public static long timeQuantum = 30 * 60 * 1000L;
    public static JavaScriptBooleanFilterFunction filterFunction = new JavaScriptBooleanFilterFunction();
    public static String fxFolder = "30min/";
    public static String mainAsset = "EURUSD";
    public static String ticksFile = "EURUSD_Ticks.csv";
    public static long maxOpenTime = 3600000L;
    public static int numberOfMergeTries = 2;
    public static boolean useStimuliGenerator = false;
    public static String stimuliFile = "EURUSD_Ticks.csv";
    public static boolean useStimuliFile = false;
    public static TreeMap<Integer, Integer> neuronOpinions = new TreeMap<>();

    public static String getInfo() {
        return "PropertiesHolder{" +
                "\nisLiveAccount=" + isLiveAccount +
                ",\ndukascopyLogin=" + dukascopyLogin +
                ",\ndukascopyPass=" + dukascopyPass +
                ",\nrecencyBias=" + recencyBias +
                ",\ntolerance=" + tolerance +
                ",\ninitialSeriesOffset=" + initialSeriesOffset +
                ",\nmaxPopulation=" + maxPopulation +
                ",\ncopulaToUniversal=" + copulaToUniversal +
                ",\ntoleranceSearch=" + toleranceSearch +
                ",\nparentsPerNeuron=" + parentsPerNeuron +
                ",\naddtionalStimuliPerNeuron=" + addtionalStimuliPerNeuron +
                ",\ncoreStimuliPerNeuron=" + coreStimuliPerNeuron +
                ",\ntotalNeurons=" + totalNeurons +
                ",\ntotalRandomisedNeurons=" + totalRandomisedNeurons +
                ",\ngenerationNeurons=" + generationNeurons +
                ",\nhttpPort=" + httpPort +
                ",\nnormalisationOfSuggestion=" + normalisationOfSuggestion +
                ",\nadditionalPassRatio=" + additionalPassRatio +
                ",\nhorizonLowerBound=" + horizonLowerBound +
                ",\nhorizonUpperBound=" + horizonUpperBound +
                ",\nneuronLearningPeriod=" + neuronLearningPeriod +
                ",\ngenerationLifespan=" + generationLifespan +
                ",\nminGenerationLifespan=" + minGenerationLifespan +
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
                ",\nmaxOpenTime=" + maxOpenTime +
                ",\nnumberOfMergeTries=" + numberOfMergeTries +
                ",\nmainAsset=" + mainAsset +
                ",\nuseStimuliGenerator=" + useStimuliGenerator +
                ",\nuseStimuliFile=" + useStimuliFile +
                ",\nstimuliFile=" + stimuliFile +
                '}';
    }
}
