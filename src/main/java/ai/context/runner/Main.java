package ai.context.runner;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.fx.DukascopyFeed;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.row.FXStreetCalendarRSSFeed;
import ai.context.feed.stitchable.StitchableFXRate;
import ai.context.feed.stitchable.StitchableFXStreetCalendarRSS;
import ai.context.feed.stitchable.StitchableFeed;
import ai.context.feed.surgical.ExtractOneFromListFeed;
import ai.context.feed.surgical.FXHLDiffFeed;
import ai.context.feed.surgical.FXModuloFeed;
import ai.context.feed.synchronised.SmartDiscretiserOnSynchronisedFeed;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.feed.transformer.compound.AmplitudeWavelengthTransformer;
import ai.context.feed.transformer.compound.SubtractTransformer;
import ai.context.feed.transformer.filtered.RowBasedTransformer;
import ai.context.feed.transformer.series.learning.BufferedTransformer;
import ai.context.feed.transformer.series.online.CrossingSeriesOnlineTransformer;
import ai.context.feed.transformer.series.online.DeltaOnlineTransformer;
import ai.context.feed.transformer.series.online.RSIOnlineTransformer;
import ai.context.feed.transformer.series.online.StandardDeviationOnlineTransformer;
import ai.context.feed.transformer.single.TimeVariablesAppenderFeed;
import ai.context.feed.transformer.single.unpadded.LinearDiscretiser;
import ai.context.learning.DataObject;
import ai.context.learning.Learner;
import ai.context.learning.LearnerFeed;
import ai.context.learning.LearnerFeedFromSynchronisedFeed;
import ai.context.trading.DukascopyConnection;
import ai.context.util.configuration.DynamicPropertiesLoader;
import ai.context.util.measurement.LoggerTimer;
import ai.context.util.trading.BlackBox;
import ai.context.util.trading.PositionFactory;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static ai.context.util.common.DateUtils.getTimeFromString_YYYYMMddHHmmss;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private Learner trader;
    private boolean isLive = false;

    private Set<BufferedTransformer> bufferedTransformers = new HashSet<>();
    private StitchableFeed liveFXCalendar;
    private StitchableFeed liveFXRateEUR;
    private StitchableFeed liveFXRateGBP;
    private StitchableFeed liveFXRateCHF;

    private IClient client;
    private BlackBox blackBox;

    private boolean testing = true;
    private String dukascopyUsername = "DEMO2LfagZ";
    private String dukascopyPassword = "LfagZ";

    private boolean successfullMemeryLoading = false;

    public static void main(String[] args)
    {
        Main test = new Main();
        String path = "C:/Users/Oblene/Desktop/Sandbox/Data/";
        if(!(args == null || args.length == 0))
        {
            path = args[0];
        }
        test.setTraderOutput(path);
        test.setup(path);
        test.trade();
    }

    public void setTraderOutput(String output){
        trader = new Learner(output);
        trader.setBlackBox(blackBox);
        successfullMemeryLoading = trader.loadMemories("./memories", getTimeFromString_YYYYMMddHHmmss("20130201000000"));
    }

    private void goLive(){
        for(final BufferedTransformer transformer : bufferedTransformers){

            Runnable makeAlive = new Runnable() {
                @Override
                public void run() {
                    LOGGER.info(transformer + " going live...");
                    transformer.goLive();
                }
            };
            new Thread(makeAlive).start();
        }
        isLive = true;
        LOGGER.info("Going LIVE!!!");
    }

    public void setup(String path)
    {
        if(!testing){
            initFXAPI();

            setLiveFXCalendar(new StitchableFXStreetCalendarRSS(path + "tmp/FXCalendar.csv", new FXStreetCalendarRSSFeed()));
            setLiveFXRates(
                    new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.FIVE_MINS, Instrument.EURUSD)),
                    new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.FIVE_MINS, Instrument.GBPUSD)),
                    new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.FIVE_MINS, Instrument.USDCHF)));
        }

        final Timer timer = new Timer();
        TimerTask checkIfGoTrading = new TimerTask() {
            @Override
            public void run() {
                if(!trader.isLive() && Math.abs(trader.getTime() - System.currentTimeMillis()) < 15 * 60 * 1000L){
                    trader.setLive();
                    LOGGER.info("LIVE TRADING");
                    timer.cancel();
                }
            }
        };

        timer.schedule(checkIfGoTrading, 10000L, 500);

        DataType[] typesCalendar = new DataType[]{
                DataType.OTHER,
                DataType.OTHER,
                DataType.INTEGER,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE};

        String dateFC = "20080101 00:00:00";
        if(successfullMemeryLoading){
            dateFC = "20130201 00:00:00";
        }

        long interval = 1*60000L;
        CSVFeed feedCalendar = new CSVFeed(path + "feeds/Calendar_2008.csv", "yyyyMMdd HH:mm:ss", typesCalendar, dateFC);
        feedCalendar.setStitchableFeed(liveFXCalendar);
        feedCalendar.setPaddable(true);
        feedCalendar.setInterval(interval);
        RowBasedTransformer f1 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0}, new String[]{"Nonfarm Payrolls"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f1);

        RowBasedTransformer f2 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Consumer Price Index \\(MoM\\)", "United Kingdom"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f2);
        RowBasedTransformer f3 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Consumer Price Index \\(MoM\\)", "United States"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f3);
        RowBasedTransformer f4 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Consumer Price Index \\(MoM\\)", "Germany"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f4);

        RowBasedTransformer f5 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Producer Price Index \\(MoM\\)", "European Monetary Union"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f5);
        RowBasedTransformer f6 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Producer Price Index \\(MoM\\)", "United States"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f6);
        RowBasedTransformer f7 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Retail Price Index \\(MoM\\)", "United Kingdom"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f7);
        RowBasedTransformer f8 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Manufacturing Production \\(MoM\\)", "United Kingdom"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f8);
        RowBasedTransformer f9 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Producer Price Index \\(MoM\\)", "Germany"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f9);
        RowBasedTransformer f10 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"BoE Interest Rate Decision", "United Kingdom"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f10);
        RowBasedTransformer f11 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Fed Interest Rate Decision", "United States"}, new int[]{3, 4, 5}, trader);
        feedCalendar.addChild(f11);


        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = "2008.01.01 00:00:00";
        if(successfullMemeryLoading){
            dateFP = "2013.02.01 00:00:00";
        }
        CSVFeed feedPriceEUR = new CSVFeed(path + "feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  dateFP);
        feedPriceEUR.setStitchableFeed(liveFXRateEUR);
        CSVFeed feedPriceGBP = new CSVFeed(path + "feeds/GBPUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  dateFP);
        feedPriceGBP.setStitchableFeed(liveFXRateGBP);
        //CSVFeed feedPriceCHF = new CSVFeed(path + "feeds/USDCHF.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  dateFP);
        //feedPriceCHF.setStitchableFeed(liveFXRateCHF);
        //CSVFeed feedPriceAUD = new CSVFeed(path + "feeds/AUDUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);


        SynchronisedFeed feed = buildSynchFeed(null, feedPriceEUR);
        feed = buildSynchFeed(feed, feedPriceGBP);
        //feed = buildSynchFeed(feed, feedPriceCHF);
        SmartDiscretiserOnSynchronisedFeed sFeed = new SmartDiscretiserOnSynchronisedFeed(feed, 5000, 5);
        //MinMaxAggregatorDiscretiser sFeed = new MinMaxAggregatorDiscretiser(feed, 5000, 10);
        feed.addChild(sFeed);
        TimeVariablesAppenderFeed tFeed = new TimeVariablesAppenderFeed(sFeed);
        sFeed.addChild(tFeed);

        feed = new SynchronisedFeed(feedPriceEUR, null);
        feed = addToSynchFeed(feed, f1, 25, 100);
        feed = addToSynchFeed(feed, f2, 0.1, 0);
        feed = addToSynchFeed(feed, f3, 0.1, 0);
        feed = addToSynchFeed(feed, f4, 0.1, 0);
        feed = addToSynchFeed(feed, f5, 0.1, 0);
        feed = addToSynchFeed(feed, f6, 0.1, 0);
        feed = addToSynchFeed(feed, f7, 0.1, 0);
        feed = addToSynchFeed(feed, f8, 0.1, 0);
        feed = addToSynchFeed(feed, f9, 0.1, 0);
        feed = addToSynchFeed(feed, f10, 0.1, 0);
        feed = addToSynchFeed(feed, f11, 0.1, 0);
        feed = new SynchronisedFeed(tFeed, feed);

        int i = 0;
        while (true)
        {
            FeedObject data = feed.getNextComposite(this);
            trader.setCurrentTime(data.getTimeStamp());
            i++;

            if(i  == 20000)
            {
                break;
            }
        }

        LearnerFeed learnerFeed = new LearnerFeedFromSynchronisedFeed(feed);
        LOGGER.info(learnerFeed.getDescription());

        trader.setActionResolution(0.0001);
        trader.setTrainingLearnerFeed(learnerFeed);
        trader.setMaxPopulation(5000);
        trader.setTolerance(0.05);

        PositionFactory.setRewardRiskRatio(1.25);
        PositionFactory.setMinTakeProfit(0.0050);
        PositionFactory.setAmount(10000);
        PositionFactory.setCost(0.00015);
        PositionFactory.setTradeToCapRatio(0.01);
        PositionFactory.setLeverage(25);
        PositionFactory.setTimeSpan(4 * 12 * 5 * 60 * 1000L);

        PositionFactory.setMinProbFraction(0.5);
        PositionFactory.setVerticalRisk(true);
        PositionFactory.setMinTakeProfitVertical(0.0020);
        LoggerTimer.turn(false);

        DynamicPropertiesLoader.start("C:/Dev/Source/CALM/src/main/resources");
        goLive();

        i = 0;
        while (true)
        {
            DataObject data = learnerFeed.readNext();
            trader.setCurrentTime(data.getTimeStamp());

            System.out.println(new Date(data.getTimeStamp()) + " " + data);
            i++;

            if(i  == 10000)
            {
                break;
            }
        }
    }

    public void trade()
    {
        trader.run();
    }

    public void initFXAPI(){
        try {
            client = new DukascopyConnection(dukascopyUsername, dukascopyPassword).getClient();
            blackBox = new BlackBox(client);
            trader.setBlackBox(blackBox);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLiveFXCalendar(final StitchableFeed liveFXCalendar) {
        this.liveFXCalendar = liveFXCalendar;
    }

    public void setLiveFXRates(StitchableFeed liveFXRateEUR,StitchableFeed liveFXRateGBP,StitchableFeed liveFXRateCHF) {
        this.liveFXRateEUR = liveFXRateEUR;
        this.liveFXRateGBP = liveFXRateGBP;
        this.liveFXRateCHF = liveFXRateCHF;
    }

    private SynchronisedFeed buildSynchFeed( SynchronisedFeed synch, CSVFeed ... feeds)
    {
        for(CSVFeed feed : feeds){
            ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
            feed.addChild(feedH);
            ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
            feed.addChild(feedL);
            ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);
            feed.addChild(feedC);
            ExtractOneFromListFeed feedV = new ExtractOneFromListFeed(feed, 4);
            feed.addChild(feedV);

            DeltaOnlineTransformer dH = new DeltaOnlineTransformer(1, feedH);
            DeltaOnlineTransformer dL = new DeltaOnlineTransformer(1, feedH);
            DeltaOnlineTransformer dC = new DeltaOnlineTransformer(1, feedH);
            DeltaOnlineTransformer dV = new DeltaOnlineTransformer(1, feedH);

            DeltaOnlineTransformer dH1 = new DeltaOnlineTransformer(2, feedH);
            DeltaOnlineTransformer dL1 = new DeltaOnlineTransformer(2, feedH);
            DeltaOnlineTransformer dC1 = new DeltaOnlineTransformer(2, feedH);
            DeltaOnlineTransformer dV1 = new DeltaOnlineTransformer(2, feedH);

            DeltaOnlineTransformer dH2 = new DeltaOnlineTransformer(4, feedH);
            DeltaOnlineTransformer dL2 = new DeltaOnlineTransformer(4, feedH);
            DeltaOnlineTransformer dC2 = new DeltaOnlineTransformer(4, feedH);
            DeltaOnlineTransformer dV2 = new DeltaOnlineTransformer(4, feedH);

            DeltaOnlineTransformer dH3 = new DeltaOnlineTransformer(8, feedH);
            DeltaOnlineTransformer dL3 = new DeltaOnlineTransformer(8, feedH);
            DeltaOnlineTransformer dC3 = new DeltaOnlineTransformer(8, feedH);
            DeltaOnlineTransformer dV3 = new DeltaOnlineTransformer(8, feedH);

            DeltaOnlineTransformer dH4 = new DeltaOnlineTransformer(16, feedH);
            DeltaOnlineTransformer dL4 = new DeltaOnlineTransformer(16, feedH);
            DeltaOnlineTransformer dC4 = new DeltaOnlineTransformer(16, feedH);
            DeltaOnlineTransformer dV4 = new DeltaOnlineTransformer(16, feedH);

            DeltaOnlineTransformer dH5 = new DeltaOnlineTransformer(32, feedH);
            DeltaOnlineTransformer dL5 = new DeltaOnlineTransformer(32, feedH);
            DeltaOnlineTransformer dC5 = new DeltaOnlineTransformer(32, feedH);
            DeltaOnlineTransformer dV5 = new DeltaOnlineTransformer(32, feedH);

            DeltaOnlineTransformer dH6 = new DeltaOnlineTransformer(64, feedH);
            DeltaOnlineTransformer dL6 = new DeltaOnlineTransformer(64, feedH);
            DeltaOnlineTransformer dC6 = new DeltaOnlineTransformer(64, feedH);
            DeltaOnlineTransformer dV6 = new DeltaOnlineTransformer(64, feedH);

            FXHLDiffFeed feedDiff = new FXHLDiffFeed(feed);
            feed.addChild(feedDiff);

            FXModuloFeed feedModulo = new FXModuloFeed(feed, 0.0001, 100);
            feed.addChild(feedModulo);

            StandardDeviationOnlineTransformer stdFeedH = new StandardDeviationOnlineTransformer(12, feedH);
            StandardDeviationOnlineTransformer stdFeedL = new StandardDeviationOnlineTransformer(12, feedL);
            StandardDeviationOnlineTransformer stdFeedV = new StandardDeviationOnlineTransformer(12, feedV);

            AmplitudeWavelengthTransformer awFeedH = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedV = new AmplitudeWavelengthTransformer(feedV, stdFeedV, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedH2 = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2 = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 4, 0.5);

            StandardDeviationOnlineTransformer stdFeedH2 = new StandardDeviationOnlineTransformer(50, feedH);
            StandardDeviationOnlineTransformer stdFeedL2 = new StandardDeviationOnlineTransformer(50, feedL);
            StandardDeviationOnlineTransformer stdFeedV2 = new StandardDeviationOnlineTransformer(50, feedV);

            AmplitudeWavelengthTransformer awFeedH50 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL50 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedV50 = new AmplitudeWavelengthTransformer(feedV, stdFeedV2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedH2_50 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2_50 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 4, 0.5);

            StandardDeviationOnlineTransformer stdFeedH3 = new StandardDeviationOnlineTransformer(100, feedH);
            StandardDeviationOnlineTransformer stdFeedL3 = new StandardDeviationOnlineTransformer(100, feedL);
            StandardDeviationOnlineTransformer stdFeedV3 = new StandardDeviationOnlineTransformer(100, feedV);

            AmplitudeWavelengthTransformer awFeedH100 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL100 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedV100 = new AmplitudeWavelengthTransformer(feedV, stdFeedV2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedH2_100 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2_100 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 4, 0.5);


            RSIOnlineTransformer rsiH = new RSIOnlineTransformer(feedH, 5, 5, 0.5);
            RSIOnlineTransformer rsiL = new RSIOnlineTransformer(feedL, 5, 5, 0.5);
            
            ExtractOneFromListFeed kRSIH1 = new ExtractOneFromListFeed(rsiH, 1);            
            rsiH.addChild(kRSIH1);
            ExtractOneFromListFeed dRSIH1 = new ExtractOneFromListFeed(rsiH, 2);
            rsiH.addChild(dRSIH1);
            ExtractOneFromListFeed kRSIL1 = new ExtractOneFromListFeed(rsiL, 1);
            rsiL.addChild(kRSIL1);
            ExtractOneFromListFeed dRSIL1 = new ExtractOneFromListFeed(rsiL, 2);
            rsiL.addChild(dRSIL1);
            CrossingSeriesOnlineTransformer crossH1 = new CrossingSeriesOnlineTransformer(kRSIH1, dRSIH1, 10);
            CrossingSeriesOnlineTransformer crossL1 = new CrossingSeriesOnlineTransformer(kRSIL1, dRSIL1, 10);

            RSIOnlineTransformer rsiH3 = new RSIOnlineTransformer(feedH, 5, 20, 0.1);
            RSIOnlineTransformer rsiL3 = new RSIOnlineTransformer(feedL, 5, 20, 0.1);

            ExtractOneFromListFeed kRSIH3 = new ExtractOneFromListFeed(rsiH3, 1);
            rsiH.addChild(kRSIH3);
            ExtractOneFromListFeed dRSIH3 = new ExtractOneFromListFeed(rsiH3, 2);
            rsiH.addChild(dRSIH3);
            ExtractOneFromListFeed kRSIL3 = new ExtractOneFromListFeed(rsiL3, 1);
            rsiL.addChild(kRSIL3);
            ExtractOneFromListFeed dRSIL3 = new ExtractOneFromListFeed(rsiL3, 2);
            rsiL.addChild(dRSIL3);
            CrossingSeriesOnlineTransformer crossH3 = new CrossingSeriesOnlineTransformer(kRSIH3, dRSIH3, 10);
            CrossingSeriesOnlineTransformer crossL3 = new CrossingSeriesOnlineTransformer(kRSIL3, dRSIL3, 10);

            RSIOnlineTransformer rsiH2 = new RSIOnlineTransformer(feedH, 5, 40, 0.05);
            RSIOnlineTransformer rsiL2 = new RSIOnlineTransformer(feedL, 5, 40, 0.05);

            ExtractOneFromListFeed kRSIH2 = new ExtractOneFromListFeed(rsiH2, 1);
            rsiH.addChild(kRSIH2);
            ExtractOneFromListFeed dRSIH2 = new ExtractOneFromListFeed(rsiH2, 2);
            rsiH.addChild(dRSIH2);
            ExtractOneFromListFeed kRSIL2 = new ExtractOneFromListFeed(rsiL2, 1);
            rsiL.addChild(kRSIL2);
            ExtractOneFromListFeed dRSIL2 = new ExtractOneFromListFeed(rsiL2, 2);
            rsiL.addChild(dRSIL2);
            CrossingSeriesOnlineTransformer crossH2 = new CrossingSeriesOnlineTransformer(kRSIH2, dRSIH2, 10);
            CrossingSeriesOnlineTransformer crossL2 = new CrossingSeriesOnlineTransformer(kRSIL2, dRSIL2, 10);

            RSIOnlineTransformer rsiH5 = new RSIOnlineTransformer(feedH, 5, 10, 0.025);
            RSIOnlineTransformer rsiL5 = new RSIOnlineTransformer(feedL, 5, 10, 0.025);

            ExtractOneFromListFeed kRSIH5 = new ExtractOneFromListFeed(rsiH5, 1);
            rsiH.addChild(kRSIH5);
            ExtractOneFromListFeed dRSIH5 = new ExtractOneFromListFeed(rsiH5, 2);
            rsiH.addChild(dRSIH5);
            ExtractOneFromListFeed kRSIL5 = new ExtractOneFromListFeed(rsiL5, 1);
            rsiL.addChild(kRSIL5);
            ExtractOneFromListFeed dRSIL5 = new ExtractOneFromListFeed(rsiL5, 2);
            rsiL.addChild(dRSIL5);
            CrossingSeriesOnlineTransformer crossH5 = new CrossingSeriesOnlineTransformer(kRSIH5, dRSIH5, 10);
            CrossingSeriesOnlineTransformer crossL5 = new CrossingSeriesOnlineTransformer(kRSIL5, dRSIL5, 10);

            RSIOnlineTransformer rsiH4 = new RSIOnlineTransformer(feedH, 5, 10, 0.2);
            RSIOnlineTransformer rsiL4 = new RSIOnlineTransformer(feedL, 5, 10, 0.2);

            ExtractOneFromListFeed kRSIH4 = new ExtractOneFromListFeed(rsiH4, 1);
            rsiH.addChild(kRSIH4);
            ExtractOneFromListFeed dRSIH4 = new ExtractOneFromListFeed(rsiH4, 2);
            rsiH.addChild(dRSIH4);
            ExtractOneFromListFeed kRSIL4 = new ExtractOneFromListFeed(rsiL4, 1);
            rsiL.addChild(kRSIL4);
            ExtractOneFromListFeed dRSIL4 = new ExtractOneFromListFeed(rsiL4, 2);
            rsiL.addChild(dRSIL4);
            CrossingSeriesOnlineTransformer crossH4 = new CrossingSeriesOnlineTransformer(kRSIH4, dRSIH4, 10);
            CrossingSeriesOnlineTransformer crossL4 = new CrossingSeriesOnlineTransformer(kRSIL4, dRSIL4, 10);

            synch = new SynchronisedFeed(feedH, synch);
            synch = new SynchronisedFeed(feedL, synch);
            synch = new SynchronisedFeed(feedC, synch);
            synch = new SynchronisedFeed(feedV, synch);

            synch = new SynchronisedFeed(dH, synch);
            synch = new SynchronisedFeed(dL, synch);
            synch = new SynchronisedFeed(dC, synch);
            synch = new SynchronisedFeed(dV, synch);
            
            synch = new SynchronisedFeed(dH1, synch);
            synch = new SynchronisedFeed(dL1, synch);
            synch = new SynchronisedFeed(dC1, synch);
            synch = new SynchronisedFeed(dV1, synch);

            synch = new SynchronisedFeed(dH2, synch);
            synch = new SynchronisedFeed(dL2, synch);
            synch = new SynchronisedFeed(dC2, synch);
            synch = new SynchronisedFeed(dV2, synch);

            synch = new SynchronisedFeed(dH3, synch);
            synch = new SynchronisedFeed(dL3, synch);
            synch = new SynchronisedFeed(dC3, synch);
            synch = new SynchronisedFeed(dV3, synch);

            synch = new SynchronisedFeed(dH4, synch);
            synch = new SynchronisedFeed(dL4, synch);
            synch = new SynchronisedFeed(dC4, synch);
            synch = new SynchronisedFeed(dV4, synch);

            synch = new SynchronisedFeed(dH5, synch);
            synch = new SynchronisedFeed(dL5, synch);
            synch = new SynchronisedFeed(dC5, synch);
            synch = new SynchronisedFeed(dV5, synch);

            synch = new SynchronisedFeed(dH6, synch);
            synch = new SynchronisedFeed(dL6, synch);
            synch = new SynchronisedFeed(dC6, synch);
            synch = new SynchronisedFeed(dV6, synch);

            synch = new SynchronisedFeed(feedDiff, synch);
            synch = new SynchronisedFeed(feedModulo, synch);

            synch = new SynchronisedFeed(stdFeedH, synch);
            synch = new SynchronisedFeed(stdFeedL, synch);
            synch = new SynchronisedFeed(stdFeedV, synch);

            synch = new SynchronisedFeed(stdFeedH2, synch);
            synch = new SynchronisedFeed(stdFeedL2, synch);
            synch = new SynchronisedFeed(stdFeedV2, synch);

            synch = new SynchronisedFeed(stdFeedH3, synch);
            synch = new SynchronisedFeed(stdFeedL3, synch);
            synch = new SynchronisedFeed(stdFeedV3, synch);

            synch = new SynchronisedFeed(awFeedH, synch);
            synch = new SynchronisedFeed(awFeedL, synch);
            synch = new SynchronisedFeed(awFeedV, synch);
            synch = new SynchronisedFeed(awFeedH2, synch);
            synch = new SynchronisedFeed(awFeedL2, synch);

            synch = new SynchronisedFeed(awFeedH50, synch);
            synch = new SynchronisedFeed(awFeedL50, synch);
            synch = new SynchronisedFeed(awFeedV50, synch);
            synch = new SynchronisedFeed(awFeedH2_50, synch);
            synch = new SynchronisedFeed(awFeedL2_50, synch);

            synch = new SynchronisedFeed(awFeedH100, synch);
            synch = new SynchronisedFeed(awFeedL100, synch);
            synch = new SynchronisedFeed(awFeedV100, synch);
            synch = new SynchronisedFeed(awFeedH2_100, synch);
            synch = new SynchronisedFeed(awFeedL2_100, synch);

            synch = new SynchronisedFeed(rsiH, synch);
            synch = new SynchronisedFeed(rsiL, synch);
            synch = new SynchronisedFeed(crossH1, synch);
            synch = new SynchronisedFeed(crossL1, synch);
            synch = new SynchronisedFeed(rsiH2, synch);
            synch = new SynchronisedFeed(rsiL2, synch);
            synch = new SynchronisedFeed(crossH2, synch);
            synch = new SynchronisedFeed(crossL2, synch);
            synch = new SynchronisedFeed(rsiH3, synch);
            synch = new SynchronisedFeed(rsiL3, synch);
            synch = new SynchronisedFeed(crossH3, synch);
            synch = new SynchronisedFeed(crossL3, synch);
            synch = new SynchronisedFeed(rsiH4, synch);
            synch = new SynchronisedFeed(rsiL4, synch);
            synch = new SynchronisedFeed(crossH4, synch);
            synch = new SynchronisedFeed(crossL4, synch);
            synch = new SynchronisedFeed(rsiH5, synch);
            synch = new SynchronisedFeed(rsiL5, synch);
            synch = new SynchronisedFeed(crossH5, synch);
            synch = new SynchronisedFeed(crossL5, synch);
        }
        return synch;
    }

    private SynchronisedFeed addToSynchFeed(SynchronisedFeed feed, RowBasedTransformer raw, double resolution, double benchmark){

        LinearDiscretiser l0 = new LinearDiscretiser(resolution, benchmark, raw, 0);
        raw.addChild(l0);
        feed = new SynchronisedFeed(l0, feed);

        ExtractOneFromListFeed e1 = new ExtractOneFromListFeed(raw, 0);
        raw.addChild(e1);
        ExtractOneFromListFeed e2 = new ExtractOneFromListFeed(raw, 1);
        raw.addChild(e2);
        ExtractOneFromListFeed e3 = new ExtractOneFromListFeed(raw, 2);
        raw.addChild(e3);

        SubtractTransformer s1 = new SubtractTransformer(e1, e2);
        e1.addChild(s1);
        e2.addChild(s1);

        SubtractTransformer s2 = new SubtractTransformer(e1, e3);
        e1.addChild(s2);
        e3.addChild(s2);

        LinearDiscretiser l1 = new LinearDiscretiser(resolution, benchmark, s1, 0);
        s1.addChild(l1);
        feed = new SynchronisedFeed(l1, feed);

        LinearDiscretiser l2 = new LinearDiscretiser(resolution, benchmark, s2, 0);
        s2.addChild(l2);
        feed = new SynchronisedFeed(l2, feed);

        LinearDiscretiser l3 = new LinearDiscretiser(0.1, 0, raw, 3);
        raw.addChild(l3);
        feed = new SynchronisedFeed(l3, feed);

        return feed;
    }
}
