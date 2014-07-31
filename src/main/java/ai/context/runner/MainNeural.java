package ai.context.runner;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.fx.DukascopyFeed;
import ai.context.feed.manipulation.FeedWrapper;
import ai.context.feed.manipulation.Manipulator;
import ai.context.feed.manipulation.TimeDecaySingleSentimentManipulator;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.row.FXStreetCalendarRSSFeed;
import ai.context.feed.row.FXStreetCalendarScheduleFeed;
import ai.context.feed.row.RowFeed;
import ai.context.feed.stitchable.StitchableFXRate;
import ai.context.feed.stitchable.StitchableFXStreetCalendarRSS;
import ai.context.feed.stitchable.StitchableFXStreetCalendarSchedule;
import ai.context.feed.stitchable.StitchableFeed;
import ai.context.feed.surgical.ExtractOneFromListFeed;
import ai.context.feed.surgical.FXHLDiffFeed;
import ai.context.feed.surgical.FXModuloFeed;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.feed.synchronised.MinMaxAggregatorDiscretiser;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.feed.transformer.compound.AbsoluteAmplitudeWavelengthTransformer;
import ai.context.feed.transformer.compound.SubstractTransformer;
import ai.context.feed.transformer.series.online.*;
import ai.context.feed.transformer.single.TimeVariablesAppenderFeed;
import ai.context.feed.transformer.single.unpadded.LogarithmicDiscretiser;
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
    private StitchableFeed liveFXCalendarScheduler;
    private StitchableFeed liveFXRateEUR;
    private StitchableFeed liveFXRateGBP;
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
        DecisionAggregator.setMarketMakerDeciderHistorical(new MarketMakerDeciderHistorical(path + "feeds/EURUSD_Ticks.csv", null));

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

    public void setLiveFXCalendarSchedule(final StitchableFeed liveFXCalendarSchedule) {
        this.liveFXCalendarScheduler = liveFXCalendarSchedule;
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
        this.liveFXRateEUR = new StitchableFXRate(path + "tmp/FX1Rate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.EURUSD));
        this.liveFXRateGBP = new StitchableFXRate(path + "tmp/FX2Rate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.GBPUSD));
        this.liveFXRateJPY = new StitchableFXRate(path + "tmp/FX3Rate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.USDJPY));
    }

    private ISynchFeed initFeed(String path) {

        if (PropertiesHolder.liveTrading && !calibrating) {
            initFXAPI();

            if(PropertiesHolder.addtionalStimuliPerNeuron > 0){
                setLiveFXCalendar(new StitchableFXStreetCalendarRSS(path + "tmp/FXCalendar.csv", new FXStreetCalendarRSSFeed()));
                setLiveFXCalendarSchedule(new StitchableFXStreetCalendarSchedule(path + "tmp/FXCalendarSchedule.csv", new FXStreetCalendarScheduleFeed()));
            }

            setLiveFXRates(path);
        }

        if(PropertiesHolder.addtionalStimuliPerNeuron > 0){
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

            DataType[] typesCalendarSchedule  = new DataType[]{
                    DataType.OTHER,
                    DataType.OTHER};
            CSVFeed calendarSchedule = new CSVFeed(path + "feeds/Calendar_Schedule.csv", "yyyyMMdd HH:mm:ss", typesCalendarSchedule, dateFC);
            LookAheadScheduler scheduler = new LookAheadScheduler(calendarSchedule, 0, 1);
            rowFeeds.add(feedCalendar);
            rowFeeds.add(calendarSchedule);
            feedCalendar.setStitchableFeed(liveFXCalendar);
            calendarSchedule.setStitchableFeed(liveFXCalendarScheduler);

            FeedWrapper calendarWrapper = new FeedWrapper(feedCalendar);

            //addManipulator("Germany", "Markit Manufacturing PMI", calendarWrapper, scheduler);
            addManipulator("Germany", "Unemployment Rate s.a.", calendarWrapper, scheduler);

            //addManipulator("European Monetary Union", "Consumer Confidence", calendarWrapper, scheduler);
            addManipulator("European Monetary Union", "ECB Interest Rate Decision", calendarWrapper, scheduler);
            //addManipulator("European Monetary Union", "Economic Sentiment", calendarWrapper, scheduler);
            addManipulator("European Monetary Union", "Gross Domestic Product s.a. (QoQ)", calendarWrapper, scheduler);
            addManipulator("European Monetary Union", "Markit Manufacturing PMI", calendarWrapper, scheduler);
            addManipulator("European Monetary Union", "Producer Price Index (MoM)", calendarWrapper, scheduler);
            //addManipulator("European Monetary Union", "Retail Sales (MoM)", calendarWrapper, scheduler);
            addManipulator("European Monetary Union", "Unemployment Rate", calendarWrapper, scheduler);

            addManipulator("Japan", "BoJ Interest Rate Decision", calendarWrapper, scheduler);
            //addManipulator("Japan", "Consumer Confidence Index", calendarWrapper, scheduler);
            addManipulator("Japan", "Leading Economic Index", calendarWrapper, scheduler);
            addManipulator("Japan", "Unemployment Rate", calendarWrapper, scheduler);

            //addManipulator("United Kingdom", "CB Leading Economic Index", calendarWrapper, scheduler);
            addManipulator("United Kingdom", "Consumer Price Index (MoM)", calendarWrapper, scheduler);
            addManipulator("United Kingdom", "Gross Domestic Product (QoQ)", calendarWrapper, scheduler);
            addManipulator("United Kingdom", "Industrial Production (MoM)", calendarWrapper, scheduler);
            addManipulator("United Kingdom", "Markit Manufacturing PMI", calendarWrapper, scheduler);
            //addManipulator("United Kingdom", "Retail Price Index (MoM)", calendarWrapper, scheduler);

            //addManipulator("United States", "Average Hourly Earnings (MoM)", calendarWrapper, scheduler);
            //addManipulator("United States", "Consumer Confidence", calendarWrapper, scheduler);
            //addManipulator("United States", "Consumer Price Index (MoM)", calendarWrapper, scheduler);
            addManipulator("United States", "Fed Interest Rate Decision", calendarWrapper, scheduler);
            addManipulator("United States", "Gross Domestic Product (QoQ)", calendarWrapper, scheduler);
            addManipulator("United States", "Nonfarm Payrolls", calendarWrapper, scheduler);
            addManipulator("United States", "Producer Price Index (MoM)", calendarWrapper, scheduler);
            //addManipulator("United States", "Trade Balance", calendarWrapper, scheduler);
            addManipulator("United States", "Unemployment Rate", calendarWrapper, scheduler);


            cluster.addFeedWrapper(calendarWrapper);
        }

        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = PropertiesHolder.startDateTime;

        CSVFeed feedPriceEUR = new CSVFeed(path + "feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceEUR.setStitchableFeed(liveFXRateEUR);
        feedPriceEUR.setSkipWeekends(true);
        rowFeeds.add(feedPriceEUR);
        CSVFeed feedPriceGBP = new CSVFeed(path + "feeds/GBPUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceGBP.setStitchableFeed(liveFXRateGBP);
        feedPriceGBP.setSkipWeekends(true);
        rowFeeds.add(feedPriceGBP);
        CSVFeed feedPriceJPY = new CSVFeed(path + "feeds/USDJPY.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceJPY.setStitchableFeed(liveFXRateJPY);
        feedPriceJPY.setSkipWeekends(true);
        rowFeeds.add(feedPriceJPY);

        ISynchFeed feed = buildSynchFeed(null, 0.0001,feedPriceEUR, true);
        feed = buildSynchFeed(feed, 0.0001,feedPriceGBP, false);
        feed = buildSynchFeed(feed, 0.01, feedPriceJPY, false);

        MinMaxAggregatorDiscretiser sFeed = new MinMaxAggregatorDiscretiser(feed, PropertiesHolder.initialSeriesOffset, 12);
        sFeed.lock();

        TimeVariablesAppenderFeed tFeed = new TimeVariablesAppenderFeed(sFeed);

        ISynchFeed synchFeed = new SynchronisedFeed();
        synchFeed.addRawFeed(feedPriceEUR);
        //synchFeed.addRawFeed(feedPriceGBP);
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

    private ISynchFeed buildSynchFeed(ISynchFeed synch, double res, CSVFeed feed, boolean main) {
        if (synch == null) {
            synch = new SynchronisedFeed();
        }
        ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
        ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
        ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);
        ExtractOneFromListFeed feedV = new ExtractOneFromListFeed(feed, 4);

        synch.addRawFeed(feedV);

        MAOnlineTransformer maH10 = new MAOnlineTransformer(10, feedH);
        MAOnlineTransformer maL10 = new MAOnlineTransformer(10, feedL);

        MAOnlineTransformer maC50 = new MAOnlineTransformer(50, feedC);
        MAOnlineTransformer maC200 = new MAOnlineTransformer(200, feedC);
        MAOnlineTransformer maC400 = new MAOnlineTransformer(400, feedC);
        MAOnlineTransformer maC1000 = new MAOnlineTransformer(1000, feedC);
        MAOnlineTransformer maC4000 = new MAOnlineTransformer(4000, feedC);

        SubstractTransformer substract_MAH10 = new SubstractTransformer(feedC, maH10);
        SubstractTransformer substract_MAL10 = new SubstractTransformer(feedC, maL10);

        SubstractTransformer substract_MAC50 = new SubstractTransformer(feedC, maC50);
        SubstractTransformer substract_MAC200 = new SubstractTransformer(feedC, maC200);
        SubstractTransformer substract_MAC400 = new SubstractTransformer(feedC, maC400);
        SubstractTransformer substract_MAC1000 = new SubstractTransformer(feedC, maC1000);
        SubstractTransformer substract_MAC4000 = new SubstractTransformer(feedC, maC4000);

        synch.addRawFeed(substract_MAH10);
        synch.addRawFeed(substract_MAL10);

        synch.addRawFeed(substract_MAC50);
        synch.addRawFeed(substract_MAC200);
        synch.addRawFeed(substract_MAC400);
        synch.addRawFeed(substract_MAC1000);
        synch.addRawFeed(substract_MAC4000);

        SubstractTransformer cHDiff = new SubstractTransformer(feedH, feedC);
        LogarithmicDiscretiser cHDiffL = new LogarithmicDiscretiser(res, 0, cHDiff, -1);
        synch.addRawFeed(cHDiffL);

        SubstractTransformer cLDiff = new SubstractTransformer(feedC, feedL);
        LogarithmicDiscretiser cLDiffL = new LogarithmicDiscretiser(res, 0, cLDiff, -1);
        synch.addRawFeed(cLDiffL);

        FXHLDiffFeed feedDiff = new FXHLDiffFeed(feed, res);
        synch.addRawFeed(feedDiff);

        FXModuloFeed feedModulo = new FXModuloFeed(feed, res, 100);
        synch.addRawFeed(feedModulo);

        StandardDeviationOnlineTransformer stdFeedH = new StandardDeviationOnlineTransformer(10, feedH);
        StandardDeviationOnlineTransformer stdFeedL = new StandardDeviationOnlineTransformer(10, feedL);
        LogarithmicDiscretiser stdLH1 = new LogarithmicDiscretiser(res, 0, stdFeedH, -1);
        LogarithmicDiscretiser stdLL1 = new LogarithmicDiscretiser(res, 0, stdFeedL, -1);
        synch.addRawFeed(stdLH1);
        synch.addRawFeed(stdLL1);

        StandardDeviationOnlineTransformer stdFeedC2 = new StandardDeviationOnlineTransformer(50, feedC);
        LogarithmicDiscretiser stdLC2 = new LogarithmicDiscretiser(res, 0, stdFeedC2, -1);
        synch.addRawFeed(stdLC2);

        StandardDeviationOnlineTransformer stdFeedC3 = new StandardDeviationOnlineTransformer(100, feedC);
        LogarithmicDiscretiser stdLC3 = new LogarithmicDiscretiser(res, 0, stdFeedC3, -1);
        synch.addRawFeed(stdLC3);

        StandardDeviationOnlineTransformer stdFeedC200 = new StandardDeviationOnlineTransformer(200, feedC);
        LogarithmicDiscretiser stdLC200 = new LogarithmicDiscretiser(res, 0, stdFeedC200, -1);
        synch.addRawFeed(stdLC200);

        StandardDeviationOnlineTransformer stdFeedC400 = new StandardDeviationOnlineTransformer(400, feedC);
        LogarithmicDiscretiser stdLC400 = new LogarithmicDiscretiser(res, 0, stdFeedC400, -1);
        synch.addRawFeed(stdLC400);

        StandardDeviationOnlineTransformer stdFeedC800 = new StandardDeviationOnlineTransformer(800, feedC);
        LogarithmicDiscretiser stdLC800 = new LogarithmicDiscretiser(res, 0, stdFeedC800, -1);
        synch.addRawFeed(stdLC800);

        AbsoluteAmplitudeWavelengthTransformer awFeedH = new AbsoluteAmplitudeWavelengthTransformer(feedH, 40, 0.25, res);
        AbsoluteAmplitudeWavelengthTransformer awFeedL = new AbsoluteAmplitudeWavelengthTransformer(feedL, 40, 0.25, res);
        synch.addRawFeed(awFeedH);
        synch.addRawFeed(awFeedL);

        AbsoluteAmplitudeWavelengthTransformer awFeedC10 = new AbsoluteAmplitudeWavelengthTransformer(feedC, 10, 0.125, res);
        synch.addRawFeed(awFeedC10);
        AbsoluteAmplitudeWavelengthTransformer awFeedC20 = new AbsoluteAmplitudeWavelengthTransformer(feedC, 20, 0.125, res);
        synch.addRawFeed(awFeedC20);


        MinMaxDistanceTransformer mmdT1 = new MinMaxDistanceTransformer(10, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT2 = new MinMaxDistanceTransformer(20, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT5 = new MinMaxDistanceTransformer(50, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT7 = new MinMaxDistanceTransformer(100, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT8 = new MinMaxDistanceTransformer(400, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT9 = new MinMaxDistanceTransformer(1000, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT10 = new MinMaxDistanceTransformer(4000, feedL, feedH, feedC, res);

        synch.addRawFeed(mmdT1);
        synch.addRawFeed(mmdT2);
        synch.addRawFeed(mmdT5);
        synch.addRawFeed(mmdT7);
        synch.addRawFeed(mmdT8);
        synch.addRawFeed(mmdT9);
        synch.addRawFeed(mmdT10);

        GradientOnlineTransformer g5 = new GradientOnlineTransformer(5, feedL, feedH, feedC, res);
        GradientOnlineTransformer g10 = new GradientOnlineTransformer(10, feedL, feedH, feedC, res);
        GradientOnlineTransformer g20 = new GradientOnlineTransformer(20, feedL, feedH, feedC, res);
        GradientOnlineTransformer g50 = new GradientOnlineTransformer(50, feedL, feedH, feedC, res);
        GradientOnlineTransformer g100 = new GradientOnlineTransformer(100, feedL, feedH, feedC, res);
        GradientOnlineTransformer g200 = new GradientOnlineTransformer(200, feedL, feedH, feedC, res);

        synch.addRawFeed(g5);
        synch.addRawFeed(g10);
        synch.addRawFeed(g20);
        synch.addRawFeed(g50);
        synch.addRawFeed(g100);
        synch.addRawFeed(g200);

        if(main){
            RSIOnlineTransformer rsiH = new RSIOnlineTransformer(feedH, 5, 25, 0.5);
            RSIOnlineTransformer rsiL = new RSIOnlineTransformer(feedL, 5, 25, 0.5);

            ExtractOneFromListFeed kRSIH1 = new ExtractOneFromListFeed(rsiH, 1);
            synch.addRawFeed(kRSIH1);
            ExtractOneFromListFeed dRSIH1 = new ExtractOneFromListFeed(rsiH, 2);
            synch.addRawFeed(dRSIH1);
            ExtractOneFromListFeed kRSIL1 = new ExtractOneFromListFeed(rsiL, 1);
            synch.addRawFeed(kRSIL1);
            ExtractOneFromListFeed dRSIL1 = new ExtractOneFromListFeed(rsiL, 2);
            synch.addRawFeed(dRSIL1);

            RSIOnlineTransformer rsiH2 = new RSIOnlineTransformer(feedH, 10, 50, 0.05);
            RSIOnlineTransformer rsiL2 = new RSIOnlineTransformer(feedL, 10, 50, 0.05);

            ExtractOneFromListFeed kRSIH2 = new ExtractOneFromListFeed(rsiH2, 1);
            synch.addRawFeed(kRSIH2);
            ExtractOneFromListFeed dRSIH2 = new ExtractOneFromListFeed(rsiH2, 2);
            synch.addRawFeed(dRSIH2);
            ExtractOneFromListFeed kRSIL2 = new ExtractOneFromListFeed(rsiL2, 1);
            synch.addRawFeed(kRSIL2);
            ExtractOneFromListFeed dRSIL2 = new ExtractOneFromListFeed(rsiL2, 2);
            synch.addRawFeed(dRSIL2);

            SubstractTransformer crossHK = new SubstractTransformer(kRSIH1, kRSIH2);
            SubstractTransformer crossHD = new SubstractTransformer(dRSIH1, dRSIH2);
            SubstractTransformer crossLD = new SubstractTransformer(dRSIL1, dRSIL2);
            SubstractTransformer crossLK = new SubstractTransformer(kRSIH1, kRSIH2);

            synch.addRawFeed(crossHK);
            synch.addRawFeed(crossLK);
            synch.addRawFeed(crossHD);
            synch.addRawFeed(crossLD);

            RadarOnlineTransformer r0 = new RadarOnlineTransformer(10, feedL, feedH, feedC, res);
            RadarOnlineTransformer r1 = new RadarOnlineTransformer(20, feedL, feedH, feedC, res);
            RadarOnlineTransformer r2 = new RadarOnlineTransformer(50, feedL, feedH, feedC, res);
            synch.addRawFeed(r0);
            synch.addRawFeed(r1);
            synch.addRawFeed(r2);

            SimpleTrendOnlineTransformer t005 = new SimpleTrendOnlineTransformer(0.005, feedL, feedH, feedC, res);
            SimpleTrendOnlineTransformer t0125 = new SimpleTrendOnlineTransformer(0.125, feedL, feedH, feedC, res);
            SimpleTrendOnlineTransformer t025 = new SimpleTrendOnlineTransformer(0.25, feedL, feedH, feedC, res);
            SimpleTrendOnlineTransformer t05 = new SimpleTrendOnlineTransformer(0.5, feedL, feedH, feedC, res);
            SimpleTrendOnlineTransformer t075 = new SimpleTrendOnlineTransformer(0.75, feedL, feedH, feedC, res);

            synch.addRawFeed(t005);
            synch.addRawFeed(t0125);
            synch.addRawFeed(t025);
            synch.addRawFeed(t05);
            synch.addRawFeed(t075);

            SuddenShiftsOnlineTransformer ss10 = new SuddenShiftsOnlineTransformer(10, feedL, feedH, res, 10);
            SuddenShiftsOnlineTransformer ss20 = new SuddenShiftsOnlineTransformer(20, feedL, feedH, res, 15);
            SuddenShiftsOnlineTransformer ss50 = new SuddenShiftsOnlineTransformer(50, feedL, feedH, res, 20);
            SuddenShiftsOnlineTransformer ss100 = new SuddenShiftsOnlineTransformer(100, feedL, feedH, res, 25);
            SuddenShiftsOnlineTransformer ss200 = new SuddenShiftsOnlineTransformer(200, feedL, feedH, res, 30);
            SuddenShiftsOnlineTransformer ss400 = new SuddenShiftsOnlineTransformer(400, feedL, feedH, res, 35);
            SuddenShiftsOnlineTransformer ss800 = new SuddenShiftsOnlineTransformer(800, feedL, feedH, res, 40);

            synch.addRawFeed(ss10);
            synch.addRawFeed(ss20);
            synch.addRawFeed(ss50);
            synch.addRawFeed(ss100);
            synch.addRawFeed(ss200);
            synch.addRawFeed(ss400);
            synch.addRawFeed(ss800);
        }

        return synch;
    }

    int iManipulator = 1;
    private void addManipulator(String country, String description, FeedWrapper calendarWrapper, LookAheadScheduler scheduler){
        Manipulator manipulator = new TimeDecaySingleSentimentManipulator(country, description, scheduler);
        calendarWrapper.putManipulator("" + iManipulator, manipulator);

        iManipulator++;
    }
}

