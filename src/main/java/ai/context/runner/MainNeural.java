package ai.context.runner;

import ai.context.feed.synchronised.ISynchFeed;
import ai.context.learning.neural.NeuralLearner;
import ai.context.learning.neural.NeuronCluster;
import ai.context.runner.feeding.MotherFeedCreator;
import ai.context.runner.feeding.StateToAction;
import ai.context.runner.feeding.StateToActionSeriesCreator;
import ai.context.runner.feeding.stimuli.StimuliGenerator;
import ai.context.runner.feeding.stimuli.StimuliHolder;
import ai.context.util.configuration.DynamicPropertiesLoader;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.trading.version_1.DecisionAggregatorA;
import ai.context.util.trading.version_1.MarketMakerDeciderTrader;
import com.dukascopy.api.system.IClient;
import scala.actors.threadpool.Arrays;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainNeural {

    private NeuronCluster cluster = NeuronCluster.getInstance();
    private String config;
    private String path;

    private IClient client;
    private List<StateToAction> series;

    private boolean useStimuliGenerator = true;

    public static void main(String[] args) {
        DynamicPropertiesLoader.start("");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MainNeural process = new MainNeural();
        String path = "/opt/dev/data/";
        if (!(args == null || args.length == 0)) {
            path = args[0];
            if (args.length >= 2) {
                process.configureFrom(args[1]);
            }
        }
        process.setup(path);
        NeuronCluster.getInstance().start();
    }

    public MainNeural() {
        cluster.setContainer(this);
    }

    private void configureFrom(String config) {
        this.config = config;
    }

    public void setup(String path) {

        this.path = path;
        ISynchFeed motherFeed = MotherFeedCreator.getMotherFeed(path);
        client = MotherFeedCreator.getClient();

        NeuronCluster.getInstance().setMotherFeed(motherFeed);
        if (PropertiesHolder.tradeMarketMarker) {
            DecisionAggregatorA.setMarketMakerDeciderTest(new MarketMakerDeciderTrader(path + "feeds/" + PropertiesHolder.ticksFile, null, null));
            if(PropertiesHolder.liveTrading) {
                DecisionAggregatorA.setMarketMakerDeciderLive(new MarketMakerDeciderTrader(null, null, client));
            }
        }

        long[] horizonRange = new long[]{PropertiesHolder.horizonLowerBound, PropertiesHolder.horizonUpperBound};
        Integer[] actionElements = new Integer[]{3, 1, 2, 0};
        long outputFutureOffset = 30 * 60 * 1000L;
        double resolution = 0.0001;
        Set<Integer> availableStimuli = new HashSet<>();
        for (int i = 0; i < motherFeed.getNumberOfOutputs(); i++) {
            availableStimuli.add(i);
        }
        availableStimuli.removeAll(Arrays.asList(actionElements));

        if(useStimuliGenerator){
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
            long start = 0;
            try {
                start = format.parse(PropertiesHolder.startDateTime).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long end = start + (5L*365L*86400000L);
            series = StateToActionSeriesCreator.createSeries(motherFeed, path, start, end, 20);
            System.out.println("STA series created from: " + PropertiesHolder.startDateTime + " to: " + format.format(new Date(end)));
            StimuliGenerator stimuliGenerator = new StimuliGenerator();
            stimuliGenerator.process(series, motherFeed, StateToActionSeriesCreator.horizons);
            System.out.println("Top stimuli generated");

            for (StimuliHolder holder : stimuliGenerator.getTop(PropertiesHolder.totalNeurons)) {
                NeuronCluster.getInstance().start(new NeuralLearner(holder.getHorizon(), motherFeed, actionElements, holder.getSignalsSources(), null, null, outputFutureOffset, resolution));
            }
            stimuliGenerator.reset();
        } else if (config != null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(config));
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    if (sCurrentLine.startsWith("New Neuron:")) {

                        Integer[] actArr = null;
                        Integer[] sigArr = null;
                        String parents = null;
                        String wrappers = null;

                        String[] parts = sCurrentLine.split("\\[");
                        if (parts.length > 2) {
                            String actions = parts[2].substring(0, parts[2].indexOf(']'));
                            List<Integer> actionArray = new ArrayList<>();
                            for (String action : actions.split(",")) {
                                actionArray.add(Integer.parseInt(action.replaceAll(" ", "")));
                            }
                            actArr = new Integer[actionArray.size()];
                            for (int i = 0; i < actArr.length; i++) {
                                actArr[i] = actionArray.get(i);
                            }
                            System.out.println("Actions: " + actionArray);
                        }
                        if (parts.length > 3) {
                            String signals = parts[3].substring(0, parts[3].indexOf(']'));
                            List<Integer> sigArray = new ArrayList<>();
                            for (String signal : signals.split(",")) {
                                sigArray.add(Integer.parseInt(signal.replaceAll(" ", "")));
                            }
                            sigArr = new Integer[sigArray.size()];
                            for (int i = 0; i < sigArr.length; i++) {
                                sigArr[i] = sigArray.get(i);
                            }
                            System.out.println("Signals: " + sigArray);
                        }
                        if (parts.length > 4) {
                            parents = parts[4].substring(0, parts[4].indexOf(']'));
                            System.out.println("Parents: " + parents);
                        }

                        if (parts.length > 5) {
                            wrappers = parts[5].substring(0, parts[5].indexOf(']'));
                            System.out.println("WrapperManipulators: " + parents);
                        }
                        NeuronCluster.getInstance().start(new NeuralLearner(horizonRange, motherFeed, actArr, sigArr, parents, wrappers, outputFutureOffset, resolution));
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            for (Integer[] sigElements : cluster.seedFeeds) {
                NeuronCluster.getInstance().start(new NeuralLearner(horizonRange, motherFeed, actionElements, sigElements, null, null, outputFutureOffset, resolution));
            }
            for (int i = 0; i < PropertiesHolder.totalNeurons - cluster.seedFeeds.size(); i++) {
                Integer[] sigElements = new Integer[PropertiesHolder.coreStimuliPerNeuron];
                for (int sig = 0; sig < sigElements.length; sig++) {
                    if (availableStimuli.isEmpty()) {
                        for (int index = 0; index < motherFeed.getNumberOfOutputs(); index++) {
                            availableStimuli.add(index);
                        }
                        availableStimuli.removeAll(Arrays.asList(actionElements));
                    }
                    List<Integer> available = new ArrayList<>(availableStimuli);
                    int chosenSig = available.get((int) (Math.random() * available.size()));
                    availableStimuli.remove(chosenSig);
                    sigElements[sig] = chosenSig;
                }
                NeuronCluster.getInstance().start(new NeuralLearner(horizonRange, motherFeed, actionElements, sigElements, null, null, outputFutureOffset, resolution));
            }
        }
    }
}

