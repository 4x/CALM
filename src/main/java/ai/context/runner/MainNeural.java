package ai.context.runner;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.fx.DukascopyFeed;
import ai.context.feed.manipulation.FeedWrapper;
import ai.context.feed.manipulation.Manipulator;
import ai.context.feed.manipulation.TimeDecaySingleSentimentManipulator;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.row.FXStreetCalendarRSSFeed;
import ai.context.feed.row.RowFeed;
import ai.context.feed.stitchable.StitchableFXRate;
import ai.context.feed.stitchable.StitchableFXStreetCalendarRSS;
import ai.context.feed.stitchable.StitchableFeed;
import ai.context.feed.surgical.ExtractOneFromListFeed;
import ai.context.feed.surgical.FXHLDiffFeed;
import ai.context.feed.surgical.FXModuloFeed;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.feed.synchronised.MinMaxAggregatorDiscretiser;
import ai.context.feed.synchronised.SynchFeed;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.feed.transformer.compound.AmplitudeWavelengthTransformer;
import ai.context.feed.transformer.compound.SubstractTransformer;
import ai.context.feed.transformer.filtered.RowBasedTransformer;
import ai.context.feed.transformer.series.online.*;
import ai.context.feed.transformer.single.TimeVariablesAppenderFeed;
import ai.context.feed.transformer.single.unpadded.LinearDiscretiser;
import ai.context.learning.neural.NeuralLearner;
import ai.context.learning.neural.NeuronCluster;
import ai.context.learning.neural.NeuronRankings;
import ai.context.trading.DukascopyConnection;
import ai.context.util.analysis.LookAheadScheduler;
import ai.context.util.configuration.DynamicPropertiesLoader;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.trading.DecisionAggregator;
import ai.context.util.trading.MarketMakerDecider;
import ai.context.util.trading.MarketMakerDeciderHistorical;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.IClient;
import scala.actors.threadpool.Arrays;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MainNeural {

    private NeuronCluster cluster = NeuronCluster.getInstance();
    private boolean calibrating = true;
    private String config;
    private String path;
    private String dukascopyUsername = PropertiesHolder.dukascopyLogin;
    private String dukascopyPassword = PropertiesHolder.dukascopyPass;

    private Set<RowFeed> rowFeeds = new HashSet<>();

    private StitchableFeed liveFXCalendar;
    private StitchableFeed liveFXRateEUR;
    private StitchableFeed liveFXRateGBP;
    private StitchableFeed liveFXRateCHF;
    private StitchableFeed liveFXRateJPY;

    private IClient client;
    public static void main(String[] args) {
        DynamicPropertiesLoader.start("");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MainNeural process = new MainNeural(false);
        String path = "/opt/dev/data/";
        if (!(args == null || args.length == 0)) {
            path = args[0];
            if(args.length >= 2){
                process.configureFrom(args[1]);
            }
        }
        process.setup(path);
    }

    public MainNeural(boolean calibrating) {
        this.calibrating = calibrating;
        cluster.setContainer(this);
    }

    private void configureFrom(String config) {
        this.config = config;
    }

    public void setup(String path) {

        this.path = path;
        ISynchFeed motherFeed = initFeed(path);
        NeuronCluster.getInstance().setMotherFeed(motherFeed);
        DecisionAggregator.setMarketMakerDeciderHistorical(new MarketMakerDeciderHistorical(path + "feeds/EURUSD_Ticks.csv"));

        long[] horizonRange = new long[]{PropertiesHolder.horizonLowerBound, PropertiesHolder.horizonUpperBound};
        Integer[] actionElements = new Integer[]{3, 1, 2, 0};
        long outputFutureOffset = 30 * 60 * 1000L;
        double resolution = 0.0001;
        Set<Integer> availableStimuli = new HashSet<>();
        for (int i = 0; i < motherFeed.getNumberOfOutputs(); i++) {
            availableStimuli.add(i);
        }
        availableStimuli.removeAll(Arrays.asList(actionElements));

        if(calibrating){
            int newNeurons = 20;
            if(!cluster.seedFeeds.isEmpty()){
                newNeurons = cluster.seedFeeds.size();
            }

            for (Integer[] sigElements : cluster.seedFeeds) {
                NeuronCluster.getInstance().start(new NeuralLearner(horizonRange, motherFeed, actionElements, sigElements, null, null, outputFutureOffset, resolution));
            }
            for (int i = 0; i < newNeurons; i++) {
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
        else if(config != null){
            try {
                BufferedReader br = new BufferedReader(new FileReader(config));
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    if(sCurrentLine.startsWith("New Neuron:")){

                        Integer[] actArr = null;
                        Integer[] sigArr = null;
                        String parents = null;
                        String wrappers = null;

                        String[] parts = sCurrentLine.split("\\[");
                        if(parts.length > 2){
                            String actions = parts[2].substring(0, parts[2].indexOf(']'));
                            List<Integer> actionArray = new ArrayList<>();
                            for(String action : actions.split(",")){
                                actionArray.add(Integer.parseInt(action.replaceAll(" ", "")));
                            }
                            actArr = new Integer[actionArray.size()];
                            for(int i = 0; i < actArr.length; i++){
                                actArr[i] = actionArray.get(i);
                            }
                            System.out.println("Actions: " + actionArray);
                        }
                        if(parts.length > 3){
                            String signals = parts[3].substring(0, parts[3].indexOf(']'));
                            List<Integer> sigArray = new ArrayList<>();
                            for(String signal : signals.split(",")){
                                sigArray.add(Integer.parseInt(signal.replaceAll(" ", "")));
                            }
                            sigArr = new Integer[sigArray.size()];
                            for(int i = 0; i < sigArr.length; i++){
                                sigArr[i] = sigArray.get(i);
                            }
                            System.out.println("Signals: " + sigArray);
                        }
                        if(parts.length > 4){
                            parents = parts[4].substring(0, parts[4].indexOf(']'));
                            System.out.println("Parents: " + parents);
                        }

                        if(parts.length > 5){
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
        }
        else {
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

    public void initFXAPI() {
        try {
            client = new DukascopyConnection(dukascopyUsername, dukascopyPassword).getClient();
            //DecisionAggregator.setBlackBox(new BlackBox(client));
            DecisionAggregator.setMarketMakerDecider(new MarketMakerDecider(client));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLiveFXCalendar(final StitchableFeed liveFXCalendar) {
        this.liveFXCalendar = liveFXCalendar;
    }

    public boolean isCalibrating() {
        return calibrating;
    }

    public void nextCalibrationRound(){
        NeuronRankings.getInstance().updateCalibrationInputs();
        cluster.reset();
        for(RowFeed feed : rowFeeds){
            feed.close();
        }

        MainNeural process = null;
        if(cluster.calibrationCount.incrementAndGet() > cluster.maxCalibrationIterations){
            process = new MainNeural(false);

        }
        else{
            process = new MainNeural(true);
        }
        process.configureFrom(config);
        process.setup(path);
    }

    public void setLiveFXRates(String path) {
        this.liveFXRateEUR = new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.EURUSD));
        this.liveFXRateGBP = new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.GBPUSD));
        //this.liveFXRateCHF = new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.FIVE_MINS, Instrument.USDCHF));
        this.liveFXRateJPY = new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.USDJPY));
    }

    private ISynchFeed initFeed(String path) {

        if (PropertiesHolder.liveTrading && !calibrating) {
            initFXAPI();

            setLiveFXCalendar(new StitchableFXStreetCalendarRSS(path + "tmp/FXCalendar.csv", new FXStreetCalendarRSSFeed()));
            setLiveFXRates(path);
        }

        DataType[] typesCalendar = new DataType[]{
                DataType.OTHER,
                DataType.OTHER,
                DataType.INTEGER,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE};

        String dateFC = "20060101 00:00:00";
        long interval = 1*60000L;
        CSVFeed feedCalendar = new CSVFeed(path + "feeds/Calendar.csv", "yyyyMMdd HH:mm:ss", typesCalendar, dateFC);
        CSVFeed calendarSchedule = new CSVFeed(path + "feeds/Calendar.csv", "yyyyMMdd HH:mm:ss", typesCalendar, dateFC);
        LookAheadScheduler scheduler = new LookAheadScheduler(calendarSchedule, 0, 1);
        rowFeeds.add(feedCalendar);
        rowFeeds.add(calendarSchedule);
        feedCalendar.setStitchableFeed(liveFXCalendar);
        feedCalendar.setPaddable(true);
        feedCalendar.setInterval(interval);

        FeedWrapper calendarWrapper = new FeedWrapper(feedCalendar);
        Manipulator manipulator1 = new TimeDecaySingleSentimentManipulator("Germany", "Markit Manufacturing PMI", scheduler);
        Manipulator manipulator2 = new TimeDecaySingleSentimentManipulator("European Monetary Union", "Markit Manufacturing PMI", scheduler);
        Manipulator manipulator3 = new TimeDecaySingleSentimentManipulator("United Kingdom", "Markit Manufacturing PMI", scheduler);
        Manipulator manipulator4 = new TimeDecaySingleSentimentManipulator("Germany", "Unemployment Rate s.a.", scheduler);
        Manipulator manipulator5 = new TimeDecaySingleSentimentManipulator("United States", "Unemployment Rate", scheduler);
        Manipulator manipulator6 = new TimeDecaySingleSentimentManipulator("United States", "Producer Price Index (MoM)", scheduler);
        Manipulator manipulator7 = new TimeDecaySingleSentimentManipulator("European Monetary Union", "Producer Price Index (MoM)", scheduler);
        Manipulator manipulator8 = new TimeDecaySingleSentimentManipulator("European Monetary Union", "Unemployment Rate", scheduler);
        Manipulator manipulator9 = new TimeDecaySingleSentimentManipulator("European Monetary Union", "Retail Sales (MoM)", scheduler);
        Manipulator manipulator10 = new TimeDecaySingleSentimentManipulator("European Monetary Union", "Gross Domestic Product s.a. (QoQ)", scheduler);
        Manipulator manipulator11 = new TimeDecaySingleSentimentManipulator("European Monetary Union", "ECB Interest Rate Decision", scheduler);
        Manipulator manipulator12 = new TimeDecaySingleSentimentManipulator("European Monetary Union", "Economic Sentiment", scheduler);
        Manipulator manipulator13 = new TimeDecaySingleSentimentManipulator("Japan", "BoJ Interest Rate Decision", scheduler);
        Manipulator manipulator14 = new TimeDecaySingleSentimentManipulator("United States", "Gross Domestic Product (QoQ)", scheduler);
        Manipulator manipulator15 = new TimeDecaySingleSentimentManipulator("United Kingdom", "Gross Domestic Product (QoQ)", scheduler);
        Manipulator manipulator16 = new TimeDecaySingleSentimentManipulator("United States", "Nonfarm Payrolls", scheduler);

        calendarWrapper.putManipulator("1", manipulator1);
        calendarWrapper.putManipulator("2", manipulator2);
        calendarWrapper.putManipulator("3", manipulator3);
        calendarWrapper.putManipulator("4", manipulator4);
        calendarWrapper.putManipulator("5", manipulator5);
        calendarWrapper.putManipulator("6", manipulator6);
        calendarWrapper.putManipulator("7", manipulator7);
        calendarWrapper.putManipulator("8", manipulator8);
        calendarWrapper.putManipulator("9", manipulator9);
        calendarWrapper.putManipulator("10", manipulator10);
        calendarWrapper.putManipulator("11", manipulator11);
        calendarWrapper.putManipulator("12", manipulator12);
        calendarWrapper.putManipulator("13", manipulator13);
        calendarWrapper.putManipulator("14", manipulator14);
        calendarWrapper.putManipulator("15", manipulator15);
        calendarWrapper.putManipulator("16", manipulator16);

        cluster.addFeedWrapper(calendarWrapper);


        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = PropertiesHolder.startDateTime;

        CSVFeed feedPriceEUR = new CSVFeed(path + "feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceEUR.setStitchableFeed(liveFXRateEUR);
        rowFeeds.add(feedPriceEUR);
        CSVFeed feedPriceGBP = new CSVFeed(path + "feeds/GBPUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceGBP.setStitchableFeed(liveFXRateGBP);
        rowFeeds.add(feedPriceGBP);
        CSVFeed feedPriceJPY = new CSVFeed(path + "feeds/USDJPY.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceJPY.setStitchableFeed(liveFXRateJPY);
        rowFeeds.add(feedPriceJPY);

        ISynchFeed feed = buildSynchFeed(null, 0.0001,feedPriceEUR);
        feed = buildSynchFeed(feed, 0.0001,feedPriceGBP);
        feed = buildSynchFeed(feed, 0.01, feedPriceJPY);

        MinMaxAggregatorDiscretiser sFeed = new MinMaxAggregatorDiscretiser(feed, PropertiesHolder.initialSeriesOffset, 6);
        sFeed.lock();

        feed.addChild(sFeed);
        TimeVariablesAppenderFeed tFeed = new TimeVariablesAppenderFeed(sFeed);
        sFeed.addChild(tFeed);

        ISynchFeed synchFeed = new SynchronisedFeed();
        synchFeed.addRawFeed(feedPriceEUR);
        synchFeed.addRawFeed(tFeed);


        int i = 0;
        while (true) {
            FeedObject data = synchFeed.getNextComposite(this);
            NeuronCluster.getInstance().setMeanTime(data.getTimeStamp());
            i++;

            if (i == PropertiesHolder.initialSeriesOffset + 1) {
                System.out.println(PropertiesHolder.initialSeriesOffset + " -> " + new Date(data.getTimeStamp()) + " " + data);
                break;
            }
            //System.out.println(new Date(data.getTimeStamp()) + " " + data);
        }
        return synchFeed;
    }

    private ISynchFeed buildSynchFeed(ISynchFeed synch, double res, CSVFeed... feeds) {
        if (synch == null) {
            synch = new SynchronisedFeed();
        }
        for (CSVFeed feed : feeds) {
            ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
            feed.addChild(feedH);
            ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
            feed.addChild(feedL);
            ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);
            feed.addChild(feedC);
            ExtractOneFromListFeed feedV = new ExtractOneFromListFeed(feed, 4);
            feed.addChild(feedV);

            //synch.addRawFeed(feedH);
            //synch.addRawFeed(feedL);
            //synch.addRawFeed(feedC);
            synch.addRawFeed(feedV);

           /* DeltaOnlineTransformer dH = new DeltaOnlineTransformer(2, feedH);
            DeltaOnlineTransformer dL = new DeltaOnlineTransformer(2, feedL);

            DeltaOnlineTransformer dV = new DeltaOnlineTransformer(2, feedV);

            DeltaOnlineTransformer dH3 = new DeltaOnlineTransformer(10, feedH);
            DeltaOnlineTransformer dL3 = new DeltaOnlineTransformer(10, feedL);*/


            MAOnlineTransformer maH10 = new MAOnlineTransformer(10, feedH);
            MAOnlineTransformer maL10 = new MAOnlineTransformer(10, feedL);
            //MAOnlineTransformer maC10 = new MAOnlineTransformer(10, feedC);

            MAOnlineTransformer maH50 = new MAOnlineTransformer(50, feedH);
            MAOnlineTransformer maL50 = new MAOnlineTransformer(50, feedL);
            //MAOnlineTransformer maC50 = new MAOnlineTransformer(50, feedC);

            /*MAOnlineTransformer maH100 = new MAOnlineTransformer(100, feedH);
            MAOnlineTransformer maL100 = new MAOnlineTransformer(100, feedL);*/

            MAOnlineTransformer maH200 = new MAOnlineTransformer(200, feedH);
            MAOnlineTransformer maL200 = new MAOnlineTransformer(200, feedL);

            //CrossingSeriesOnlineTransformer crossMAH10_50 = new CrossingSeriesOnlineTransformer(maH10, maH50, 10);
            //CrossingSeriesOnlineTransformer crossMAL10_50 = new CrossingSeriesOnlineTransformer(maL10, maL50, 10);
            /*CrossingSeriesOnlineTransformer crossMAH10_100 = new CrossingSeriesOnlineTransformer(maH10, maH100, 10);
            CrossingSeriesOnlineTransformer crossMAL10_100 = new CrossingSeriesOnlineTransformer(maL10, maL100, 10);*/
            //CrossingSeriesOnlineTransformer crossMAH10_200 = new CrossingSeriesOnlineTransformer(maH10, maH200, 10);
            //CrossingSeriesOnlineTransformer crossMAL10_200 = new CrossingSeriesOnlineTransformer(maL10, maL200, 10);
            /*CrossingSeriesOnlineTransformer crossMAH50_100 = new CrossingSeriesOnlineTransformer(maH50, maH100, 10);
            CrossingSeriesOnlineTransformer crossMAL50_100 = new CrossingSeriesOnlineTransformer(maL50, maL100, 10);*/
            //CrossingSeriesOnlineTransformer crossMAC10_50 = new CrossingSeriesOnlineTransformer(maC10, maC50, 10);
            //CrossingSeriesOnlineTransformer crossMAH50_200 = new CrossingSeriesOnlineTransformer(maH50, maH200, 10);
            //CrossingSeriesOnlineTransformer crossMAL50_200 = new CrossingSeriesOnlineTransformer(maL50, maL200, 10);

            //CrossingSeriesOnlineTransformer crossC_MA50 = new CrossingSeriesOnlineTransformer(feedC, maC50, 3);
            SubstractTransformer substract_MAH10 = new SubstractTransformer(feedC, maH10);
            SubstractTransformer substract_MAH50 = new SubstractTransformer(feedC, maH50);
            SubstractTransformer substract_MAH200 = new SubstractTransformer(feedC, maH200);
            SubstractTransformer substract_MAL10 = new SubstractTransformer(feedC, maL10);
            SubstractTransformer substract_MAL50 = new SubstractTransformer(feedC, maL50);
            SubstractTransformer substract_MAL200 = new SubstractTransformer(feedC, maL200);

            synch.addRawFeed(substract_MAH10);
            synch.addRawFeed(substract_MAH50);
            synch.addRawFeed(substract_MAH200);
            synch.addRawFeed(substract_MAL10);
            synch.addRawFeed(substract_MAL50);
            synch.addRawFeed(substract_MAL200);

            /*synch.addRawFeed(crossMAH10_50);
            synch.addRawFeed(crossMAL10_50);
            synch.addRawFeed(crossMAH10_100);
            synch.addRawFeed(crossMAL10_100);
            synch.addRawFeed(crossMAH10_200);
            synch.addRawFeed(crossMAL10_200);
            synch.addRawFeed(crossMAH50_100);
            synch.addRawFeed(crossMAL50_100);
            //synch.addRawFeed(crossMAC10_50);
            synch.addRawFeed(crossMAH50_200);
            synch.addRawFeed(crossMAL50_200);*/

            FXHLDiffFeed feedDiff = new FXHLDiffFeed(feed, res);
            feed.addChild(feedDiff);
            synch.addRawFeed(feedDiff);

            FXModuloFeed feedModulo = new FXModuloFeed(feed, res, 100);
            feed.addChild(feedModulo);
            synch.addRawFeed(feedModulo);

            StandardDeviationOnlineTransformer stdFeedH = new StandardDeviationOnlineTransformer(10, feedH);
            StandardDeviationOnlineTransformer stdFeedL = new StandardDeviationOnlineTransformer(10, feedL);
            //StandardDeviationOnlineTransformer stdFeedV = new StandardDeviationOnlineTransformer(12, feedV);

            //LogarithmicDiscretiser stdLH1 = new LogarithmicDiscretiser(0.0001, 0, stdFeedH, -1);
            //LogarithmicDiscretiser stdLL1 = new LogarithmicDiscretiser(0.0001, 0, stdFeedL, -1);
            //LogarithmicDiscretiser stdLV1 = new LogarithmicDiscretiser(0.00001, 0, stdFeedV, -1);


            AmplitudeWavelengthTransformer awFeedH = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 2, 0.5);
            //AmplitudeWavelengthTransformer awFeedV = new AmplitudeWavelengthTransformer(feedV, stdFeedV, 2, 0.5);
            //AmplitudeWavelengthTransformer awFeedH2 = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 4, 0.5);
            //AmplitudeWavelengthTransformer awFeedL2 = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 4, 0.5);

            StandardDeviationOnlineTransformer stdFeedH2 = new StandardDeviationOnlineTransformer(50, feedH);
            StandardDeviationOnlineTransformer stdFeedL2 = new StandardDeviationOnlineTransformer(50, feedL);
            //StandardDeviationOnlineTransformer stdFeedV2 = new StandardDeviationOnlineTransformer(50, feedV);

            AmplitudeWavelengthTransformer awFeedH50 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 3, 0.5);
            AmplitudeWavelengthTransformer awFeedL50 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 3, 0.5);
            //AmplitudeWavelengthTransformer awFeedV50 = new AmplitudeWavelengthTransformer(feedV, stdFeedV2, 2, 0.5);
            //AmplitudeWavelengthTransformer awFeedH2_50 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 4, 0.5);
            //AmplitudeWavelengthTransformer awFeedL2_50 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 4, 0.5);

            StandardDeviationOnlineTransformer stdFeedH3 = new StandardDeviationOnlineTransformer(100, feedH);
            StandardDeviationOnlineTransformer stdFeedL3 = new StandardDeviationOnlineTransformer(100, feedL);

            //LogarithmicDiscretiser stdLH3 = new LogarithmicDiscretiser(0.0001, 0, stdFeedH3, -1);
            //LogarithmicDiscretiser stdLL3 = new LogarithmicDiscretiser(0.0001, 0, stdFeedL3, -1);
            //StandardDeviationOnlineTransformer stdFeedV3 = new StandardDeviationOnlineTransformer(100, feedV);

            AmplitudeWavelengthTransformer awFeedH100 = new AmplitudeWavelengthTransformer(feedH, stdFeedH3, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL100 = new AmplitudeWavelengthTransformer(feedL, stdFeedL3, 4, 0.5);
            //AmplitudeWavelengthTransformer awFeedV100 = new AmplitudeWavelengthTransformer(feedV, stdFeedV3, 2, 0.5);
            //AmplitudeWavelengthTransformer awFeedH2_100 = new AmplitudeWavelengthTransformer(feedH, stdFeedH3, 4, 0.5);
            //AmplitudeWavelengthTransformer awFeedL2_100 = new AmplitudeWavelengthTransformer(feedL, stdFeedL3, 4, 0.5);


            RSIOnlineTransformer rsiH = new RSIOnlineTransformer(feedH, 5, 5, 0.5);
            RSIOnlineTransformer rsiL = new RSIOnlineTransformer(feedL, 5, 5, 0.5);

            ExtractOneFromListFeed kRSIH1 = new ExtractOneFromListFeed(rsiH, 1);
            rsiH.addChild(kRSIH1);
            synch.addRawFeed(kRSIH1);
            ExtractOneFromListFeed dRSIH1 = new ExtractOneFromListFeed(rsiH, 2);
            rsiH.addChild(dRSIH1);
            synch.addRawFeed(dRSIH1);
            ExtractOneFromListFeed kRSIL1 = new ExtractOneFromListFeed(rsiL, 1);
            rsiL.addChild(kRSIL1);
            synch.addRawFeed(kRSIL1);
            ExtractOneFromListFeed dRSIL1 = new ExtractOneFromListFeed(rsiL, 2);
            rsiL.addChild(dRSIL1);
            synch.addRawFeed(dRSIL1);
            //CrossingSeriesOnlineTransformer crossH1 = new CrossingSeriesOnlineTransformer(kRSIH1, dRSIH1, 5);
            //CrossingSeriesOnlineTransformer crossL1 = new CrossingSeriesOnlineTransformer(kRSIL1, dRSIL1, 5);

            /*RSIOnlineTransformer rsiH3 = new RSIOnlineTransformer(feedH, 5, 20, 0.1);
            RSIOnlineTransformer rsiL3 = new RSIOnlineTransformer(feedL, 5, 20, 0.1);

            ExtractOneFromListFeed kRSIH3 = new ExtractOneFromListFeed(rsiH3, 1);
            rsiH3.addChild(kRSIH3);
            ExtractOneFromListFeed dRSIH3 = new ExtractOneFromListFeed(rsiH3, 2);
            rsiH3.addChild(dRSIH3);
            ExtractOneFromListFeed kRSIL3 = new ExtractOneFromListFeed(rsiL3, 1);
            rsiL3.addChild(kRSIL3);
            ExtractOneFromListFeed dRSIL3 = new ExtractOneFromListFeed(rsiL3, 2);
            rsiL3.addChild(dRSIL3);
            CrossingSeriesOnlineTransformer crossH3 = new CrossingSeriesOnlineTransformer(kRSIH3, dRSIH3, 10);
            CrossingSeriesOnlineTransformer crossL3 = new CrossingSeriesOnlineTransformer(kRSIL3, dRSIL3, 10);*/

            RSIOnlineTransformer rsiH2 = new RSIOnlineTransformer(feedH, 10, 50, 0.05);
            RSIOnlineTransformer rsiL2 = new RSIOnlineTransformer(feedL, 10, 50, 0.05);

            ExtractOneFromListFeed kRSIH2 = new ExtractOneFromListFeed(rsiH2, 1);
            rsiH2.addChild(kRSIH2);
            synch.addRawFeed(kRSIH2);
            ExtractOneFromListFeed dRSIH2 = new ExtractOneFromListFeed(rsiH2, 2);
            rsiH2.addChild(dRSIH2);
            synch.addRawFeed(dRSIH2);
            ExtractOneFromListFeed kRSIL2 = new ExtractOneFromListFeed(rsiL2, 1);
            rsiL2.addChild(kRSIL2);
            synch.addRawFeed(kRSIL2);
            ExtractOneFromListFeed dRSIL2 = new ExtractOneFromListFeed(rsiL2, 2);
            rsiL2.addChild(dRSIL2);
            synch.addRawFeed(dRSIL2);
            //CrossingSeriesOnlineTransformer crossH2 = new CrossingSeriesOnlineTransformer(kRSIH2, dRSIH2, 5);
            //CrossingSeriesOnlineTransformer crossL2 = new CrossingSeriesOnlineTransformer(kRSIL2, dRSIL2, 5);

            CrossingSeriesOnlineTransformer crossHK = new CrossingSeriesOnlineTransformer(kRSIH1, kRSIH2, 10);
            CrossingSeriesOnlineTransformer crossHD = new CrossingSeriesOnlineTransformer(dRSIH1, dRSIH2, 10);
            CrossingSeriesOnlineTransformer crossLD = new CrossingSeriesOnlineTransformer(dRSIL1, dRSIL2, 10);
            CrossingSeriesOnlineTransformer crossLK = new CrossingSeriesOnlineTransformer(kRSIH1, kRSIH2, 10);

            /*RSIOnlineTransformer rsiH5 = new RSIOnlineTransformer(feedH, 5, 10, 0.01);
            RSIOnlineTransformer rsiL5 = new RSIOnlineTransformer(feedL, 5, 10, 0.01);

            ExtractOneFromListFeed kRSIH5 = new ExtractOneFromListFeed(rsiH5, 1);
            rsiH5.addChild(kRSIH5);
            ExtractOneFromListFeed dRSIH5 = new ExtractOneFromListFeed(rsiH5, 2);
            rsiH5.addChild(dRSIH5);
            ExtractOneFromListFeed kRSIL5 = new ExtractOneFromListFeed(rsiL5, 1);
            rsiL5.addChild(kRSIL5);
            ExtractOneFromListFeed dRSIL5 = new ExtractOneFromListFeed(rsiL5, 2);
            rsiL5.addChild(dRSIL5);
            CrossingSeriesOnlineTransformer crossH5 = new CrossingSeriesOnlineTransformer(kRSIH5, dRSIH5, 10);
            CrossingSeriesOnlineTransformer crossL5 = new CrossingSeriesOnlineTransformer(kRSIL5, dRSIL5, 10);

            RSIOnlineTransformer rsiH4 = new RSIOnlineTransformer(feedH, 5, 10, 0.2);
            RSIOnlineTransformer rsiL4 = new RSIOnlineTransformer(feedL, 5, 10, 0.2);

            ExtractOneFromListFeed kRSIH4 = new ExtractOneFromListFeed(rsiH4, 1);
            rsiH4.addChild(kRSIH4);
            ExtractOneFromListFeed dRSIH4 = new ExtractOneFromListFeed(rsiH4, 2);
            rsiH4.addChild(dRSIH4);
            ExtractOneFromListFeed kRSIL4 = new ExtractOneFromListFeed(rsiL4, 1);
            rsiL4.addChild(kRSIL4);
            ExtractOneFromListFeed dRSIL4 = new ExtractOneFromListFeed(rsiL4, 2);
            rsiL4.addChild(dRSIL4);
            CrossingSeriesOnlineTransformer crossH4 = new CrossingSeriesOnlineTransformer(kRSIH4, dRSIH4, 10);
            CrossingSeriesOnlineTransformer crossL4 = new CrossingSeriesOnlineTransformer(kRSIL4, dRSIL4, 10);*/

            MinMaxDistanceTransformer mmdT1 = new MinMaxDistanceTransformer(10, feedL, feedH, feedC);
            MinMaxDistanceTransformer mmdT2 = new MinMaxDistanceTransformer(50, feedL, feedH, feedC);
            MinMaxDistanceTransformer mmdT3 = new MinMaxDistanceTransformer(100, feedL, feedH, feedC);
            MinMaxDistanceTransformer mmdT4 = new MinMaxDistanceTransformer(200, feedL, feedH, feedC);
            MinMaxDistanceTransformer mmdT5 = new MinMaxDistanceTransformer(800, feedL, feedH, feedC);


            synch.addRawFeed(mmdT1);
            synch.addRawFeed(mmdT2);
            synch.addRawFeed(mmdT3);
            synch.addRawFeed(mmdT4);
            synch.addRawFeed(mmdT5);

            RadarOnlineTransformer r1 = new RadarOnlineTransformer(50, feedL, feedH, feedC, res);
            RadarOnlineTransformer r2 = new RadarOnlineTransformer(100, feedL, feedH, feedC, res);
            RadarOnlineTransformer r3 = new RadarOnlineTransformer(200, feedL, feedH, feedC, res);
            //RadarOnlineTransformer r4 = new RadarOnlineTransformer(800, feedL, feedH, feedC, 0.0001);

            GradientOnlineTransformer g10 = new GradientOnlineTransformer(10, feedL, feedH, feedC, res);
            GradientOnlineTransformer g20 = new GradientOnlineTransformer(20, feedL, feedH, feedC, res);
            GradientOnlineTransformer g40 = new GradientOnlineTransformer(40, feedL, feedH, feedC, res);
            GradientOnlineTransformer g100 = new GradientOnlineTransformer(100, feedL, feedH, feedC, res);
            GradientOnlineTransformer g200 = new GradientOnlineTransformer(200, feedL, feedH, feedC, res);

            synch.addRawFeed(r1);
            synch.addRawFeed(r2);
            synch.addRawFeed(r3);
            //synch.addRawFeed(r4);

            synch.addRawFeed(g10);
            synch.addRawFeed(g20);
            synch.addRawFeed(g40);
            synch.addRawFeed(g100);
            synch.addRawFeed(g200);

            //synch.addRawFeed(dH);
            //synch.addRawFeed(dL);
            //synch.addRawFeed(dC);
            //synch.addRawFeed(dV);

            /*synch.addRawFeed(dH1);
            synch.addRawFeed(dL1);
            synch.addRawFeed(dC1);
            synch.addRawFeed(dV1);

            synch.addRawFeed(dH2);
            synch.addRawFeed(dL2);
            synch.addRawFeed(dC2);
            synch.addRawFeed(dV2);*/

            //synch.addRawFeed(dH3);
            //synch.addRawFeed(dL3);

            /*synch.addRawFeed(dH4);
            synch.addRawFeed(dL4);
            synch.addRawFeed(dC4);
            synch.addRawFeed(dV4);

            synch.addRawFeed(dH5);
            synch.addRawFeed(dL5);
            synch.addRawFeed(dC5);
            synch.addRawFeed(dV5);*/

            //synch.addRawFeed(feedDiff);
            //synch.addRawFeed(feedModulo);

            synch.addRawFeed(stdFeedH);
            synch.addRawFeed(stdFeedL);

            synch.addRawFeed(stdFeedH2);
            synch.addRawFeed(stdFeedL2);

            synch.addRawFeed(stdFeedH3);
            synch.addRawFeed(stdFeedL3);

            synch.addRawFeed(awFeedH);
            synch.addRawFeed(awFeedL);
            //synch.addRawFeed(awFeedV);
            //synch.addRawFeed(awFeedH2);
            //synch.addRawFeed(awFeedL2);

            synch.addRawFeed(awFeedH50);
            synch.addRawFeed(awFeedL50);
            //synch.addRawFeed(awFeedV50);
            //synch.addRawFeed(awFeedH2_50);
            //synch.addRawFeed(awFeedL2_50);

            synch.addRawFeed(awFeedH100);
            synch.addRawFeed(awFeedL100);
            //synch.addRawFeed(awFeedV100);
            //synch.addRawFeed(awFeedH2_100);
            //synch.addRawFeed(awFeedL2_100);

            //synch.addRawFeed(rsiH);
            //synch.addRawFeed(rsiL);
            //synch.addRawFeed(crossH1);
            //synch.addRawFeed(crossL1);
            //synch.addRawFeed(rsiH2);
            //synch.addRawFeed(rsiL2);
            //synch.addRawFeed(crossH2);
            //synch.addRawFeed(crossL2);

            synch.addRawFeed(crossHK);
            synch.addRawFeed(crossLK);
            synch.addRawFeed(crossHD);
            synch.addRawFeed(crossLD);
            /*synch.addRawFeed(rsiH3);
            synch.addRawFeed(rsiL3);
            synch.addRawFeed(crossH3);
            synch.addRawFeed(crossL3);
            synch.addRawFeed(rsiH4);
            synch.addRawFeed(rsiL4);
            synch.addRawFeed(crossH4);
            synch.addRawFeed(crossL4);
            synch.addRawFeed(rsiH5);
            synch.addRawFeed(rsiL5);
            synch.addRawFeed(crossH5);
            synch.addRawFeed(crossL5);*/
        }
        return synch;
    }

    private SynchFeed addToSynchFeed(SynchFeed feed, RowBasedTransformer raw, double resolution, double benchmark) {

        LinearDiscretiser l0 = new LinearDiscretiser(resolution, benchmark, raw, 0);
        raw.addChild(l0);
        feed.addRawFeed(l0);

        ExtractOneFromListFeed e1 = new ExtractOneFromListFeed(raw, 0);
        raw.addChild(e1);
        ExtractOneFromListFeed e2 = new ExtractOneFromListFeed(raw, 1);
        raw.addChild(e2);
        ExtractOneFromListFeed e3 = new ExtractOneFromListFeed(raw, 2);
        raw.addChild(e3);

        SubstractTransformer s1 = new SubstractTransformer(e1, e2);
        e1.addChild(s1);
        e2.addChild(s1);

        SubstractTransformer s2 = new SubstractTransformer(e1, e3);
        e1.addChild(s2);
        e3.addChild(s2);

        LinearDiscretiser l1 = new LinearDiscretiser(resolution, benchmark, s1, 0);
        s1.addChild(l1);
        feed.addRawFeed(l1);

        LinearDiscretiser l2 = new LinearDiscretiser(resolution, benchmark, s2, 0);
        s2.addChild(l2);
        feed.addRawFeed(l2);

        LinearDiscretiser l3 = new LinearDiscretiser(0.1, 0, raw, 3);
        raw.addChild(l3);
        feed.addRawFeed(l3);

        return feed;
    }
}
