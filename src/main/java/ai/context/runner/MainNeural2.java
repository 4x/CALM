package ai.context.runner;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.runner.feeding.MotherFeedCreator;
import ai.context.runner.feeding.NeuralLearner2;
import ai.context.runner.feeding.StateToAction;
import ai.context.runner.feeding.StateToActionSeriesCreator;
import ai.context.util.DataSetUtils;
import ai.context.util.configuration.DynamicPropertiesLoader;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.trading.version_1.DecisionAggregatorA;
import ai.context.util.trading.version_2.DecisionAggregatorB;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainNeural2 {

    public static void main(String[] args){

        DynamicPropertiesLoader.start("");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String path = "/opt/dev/data/";
        if (!(args == null && args.length > 0)) {
            path = args[0];
        }

        String initialDate = "2007.01.01";
        if (!(args == null && args.length > 1)) {
            initialDate = args[1];
        }

        String finalDate = "2013.01.01";
        if (!(args == null && args.length > 2)) {
            finalDate = args[2];
        }

        MainNeural2 process = new MainNeural2();
        process.setup(path, initialDate, finalDate);
        process.startLearning();
        process.startTrading();
    }

    private List<NeuralLearner2> neurons = new ArrayList<>();
    private List<StateToAction> series;
    private ISynchFeed motherFeed;
    private Feed priceFeed;
    private double ask;
    private double bid;

    private double res = 0.0001;

    public void setup(String path, String initialDate, String finalDate){
        motherFeed = MotherFeedCreator.getMotherFeed(path);
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        try {
            long start = format.parse(initialDate).getTime();
            long end = format.parse(finalDate).getTime();
            double[] preferredMoves = new double[]{0.0005, 0.00075, 0.001, 0.00125, 0.0015, 0.00175, 0.002};
            series = StateToActionSeriesCreator.createSeries(motherFeed, path, start, end, preferredMoves);
            priceFeed = StateToActionSeriesCreator.priceFeed;
            System.out.println(series.size());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        long[] possibleHorizons = new long[6];
        for(int i = 0; i < 6; i++){
            possibleHorizons[i] = (i + 1) * DecisionAggregatorA.getTimeQuantum();
        }

        Set<Integer> availableStimuli = new HashSet<>();
        for (int i = 0; i < motherFeed.getNumberOfOutputs(); i++) {
            availableStimuli.add(i);
        }
        for (int i = 0; i < PropertiesHolder.totalNeurons; i++) {
            int[] sigElements = new int[PropertiesHolder.coreStimuliPerNeuron];
            for (int sig = 0; sig < sigElements.length; sig++) {
                if (availableStimuli.isEmpty()) {
                    for (int index = 0; index < motherFeed.getNumberOfOutputs(); index++) {
                        availableStimuli.add(index);
                    }
                }
                List<Integer> available = new ArrayList<>(availableStimuli);
                int chosenSig = available.get((int) (Math.random() * available.size()));
                availableStimuli.remove(chosenSig);
                sigElements[sig] = chosenSig;
            }

            ArrayList<NeuralLearner2> parents = new ArrayList<>();
            while(parents.size() < PropertiesHolder.parentsPerNeuron && parents.size() != neurons.size()){
                int parentId = (int)(neurons.size() * Math.random());
                if(parentId < neurons.size()){
                    parents.add(neurons.get(parentId));
                }
            }
            NeuralLearner2[] parentsArray = new NeuralLearner2[parents.size()];

            long horizon = possibleHorizons[((int) (Math.random() * possibleHorizons.length))];
            NeuralLearner2 neuron = new NeuralLearner2(i, sigElements, parents.toArray(parentsArray), horizon, res);
            neurons.add(neuron);

            System.out.println("Created Neuron " + i + " with signal components: " + Arrays.toString(sigElements) + " and Parents: " + parents + " and horizon: " + horizon);
            ask = StateToActionSeriesCreator.ask;
            bid = StateToActionSeriesCreator.bid;
        }
    }

    public void startLearning(){
        System.out.println("Learning started");
        while (series.size() > 0){
            int index = (int) (Math.random() * series.size());
            StateToAction stateToAction = series.remove(index);
            if(stateToAction != null && stateToAction.horizonActions != null){
                for(NeuralLearner2 neuron : neurons){
                    neuron.feed(stateToAction);
                }
            }

            if(series.size() % 1000 == 0){
                System.out.println("Points remaining to learn: " + series.size());
                if((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()) + Runtime.getRuntime().freeMemory() < 1024 * 1024 * 1024){
                    System.out.println("Skipping learning further as memory running low");
                    break;
                }
            }
        }
    }

    public void startTrading(){
        System.out.println("Learning trading");
        DecisionAggregatorB.setPriceFeed(priceFeed);
        while(true){
            FeedObject data = motherFeed.getNextComposite(null);
            long tStart = data.getTimeStamp() + DecisionAggregatorA.getTimeQuantum();
            long tEnd = tStart + DecisionAggregatorA.getTimeQuantum();

            List<Object> sig = new ArrayList<>();
            DataSetUtils.add(data.getData(), sig);
            int[] signal = new int[sig.size()];
            for(int i = 0; i < signal.length; i++){
                int num = 0;
                Object raw = sig.get(i);
                if(raw instanceof Integer){
                    num = (Integer) raw;
                }
                signal[i] = num;
            }

            double close = DecisionAggregatorB.getClose();
            if(close < 0){
                close = (ask + bid)/2;
            }
            for(NeuralLearner2 neuron : neurons){
                DecisionAggregatorB.aggregateDecision(tStart, neuron.getOpinionOnContext(signal), res, close, neuron.horizon);
            }
            DecisionAggregatorB.act(tStart, tEnd);
        }
    }
}
