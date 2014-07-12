package ai.context.util.configuration;

public class PropertiesHolder {
    public static double recencyBias = 2;
    public static double tolerance = 0.01;
    public static int initialSeriesOffset = 2000;
    public static int maxPopulation = 2000;
    public static double copulaToUniversal = 20;
    public static int toleranceSearch = 2;
    public static String dukascopyLogin = "DEMO10037ZtUmzEU";
    public static String dukascopyPass = "ZtUmz";
    public static int parentsPerNeuron = 3;
    public static int addtionalStimuliPerNeuron = 0;
    public static int coreStimuliPerNeuron = 7;
    public static int totalNeurons = 150;
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
    public static boolean neuronReplacement = true;
    public static boolean tradeNormal = true;
    public static boolean tradeMarketMarker = true;
    public static boolean liveTrading = true;
    public static String startDateTime = "2006.01.01 00:00:00";

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
                '}';
    }
}
