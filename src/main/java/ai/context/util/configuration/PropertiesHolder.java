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

    public static String getInfo() {
        return "PropertiesHolder{" +
                "recencyBias=" + recencyBias +
                ", tolerance=" + tolerance +
                ", initialSeriesOffset=" + initialSeriesOffset +
                ", maxPopulation=" + maxPopulation +
                ", copulaToUniversal=" + copulaToUniversal +
                ", toleranceSearch=" + toleranceSearch +
                ", dukascopyLogin='" + dukascopyLogin + '\'' +
                ", dukascopyPass='" + dukascopyPass + '\'' +
                ", parentsPerNeuron=" + parentsPerNeuron +
                ", addtionalStimuliPerNeuron=" + addtionalStimuliPerNeuron +
                ", coreStimuliPerNeuron=" + coreStimuliPerNeuron +
                ", totalNeurons=" + totalNeurons +
                ", httpPort=" + httpPort +
                ", normalisationOfSuggestion=" + normalisationOfSuggestion +
                ", additionalPassRatio=" + additionalPassRatio +
                ", horizonLowerBound=" + horizonLowerBound +
                ", horizonUpperBound=" + horizonUpperBound +
                ", neuronLearningPeriod=" + neuronLearningPeriod +
                '}';
    }
}
