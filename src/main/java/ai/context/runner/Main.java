package ai.context.runner;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
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
import ai.context.feed.transformer.series.learning.*;
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
import com.tictactec.ta.lib.MAType;
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
        CSVFeed feedPriceCHF = new CSVFeed(path + "feeds/USDCHF.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  dateFP);
        feedPriceCHF.setStitchableFeed(liveFXRateCHF);
        //CSVFeed feedPriceAUD = new CSVFeed(path + "feeds/AUDUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);


        SynchronisedFeed feed = buildSynchFeed(null, feedPriceEUR);
        feed = buildSynchFeed(feed, feedPriceGBP);
        feed = buildSynchFeed(feed, feedPriceCHF);
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

        DynamicPropertiesLoader.start();
        goLive();

        i = 0;
        while (true)
        {
            DataObject data = learnerFeed.readNext();
            trader.setCurrentTime(data.getTimeStamp());

            //System.out.println(new Date(data.getTimeStamp()) + " " + data);
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

            FXHLDiffFeed feedDiff = new FXHLDiffFeed(feed);
            feed.addChild(feedDiff);

            Map<String, Object> param = new HashMap<>();
            param.put("period", 10);
            TALibTransformer adx = new TALibTransformer(10, TALibTransformer.TYPE_ADX, param, new Feed[]{feedH, feedL, feedC});
            feedH.addChild(adx);
            feedL.addChild(adx);
            feedC.addChild(adx);
            bufferedTransformers.add(adx);

            param = new HashMap<>();
            param.put("period", 20);
            TALibTransformer adx1 = new TALibTransformer(20, TALibTransformer.TYPE_ADX, param, new Feed[]{feedH, feedL, feedC});
            feedH.addChild(adx1);
            feedL.addChild(adx1);
            feedC.addChild(adx1);
            bufferedTransformers.add(adx1);

            param = new HashMap<>();
            param.put("period", 50);
            TALibTransformer adx2 = new TALibTransformer(50, TALibTransformer.TYPE_ADX, param, new Feed[]{feedH, feedL, feedC});
            feedH.addChild(adx2);
            feedL.addChild(adx2);
            feedC.addChild(adx2);
            bufferedTransformers.add(adx2);

            param = new HashMap<>();
            param.put("period", 100);
            TALibTransformer adx3 = new TALibTransformer(100, TALibTransformer.TYPE_ADX, param, new Feed[]{feedH, feedL, feedC});
            feedH.addChild(adx3);
            feedL.addChild(adx3);
            feedC.addChild(adx3);
            bufferedTransformers.add(adx3);

            param = new HashMap<>();
            param.put("period", 200);
            TALibTransformer adx4 = new TALibTransformer(200, TALibTransformer.TYPE_ADX, param, new Feed[]{feedH, feedL, feedC});
            feedH.addChild(adx4);
            feedL.addChild(adx4);
            feedC.addChild(adx4);
            bufferedTransformers.add(adx4);

            param = new HashMap<>();
            param.put("period", 400);
            TALibTransformer adx5 = new TALibTransformer(400, TALibTransformer.TYPE_ADX, param, new Feed[]{feedH, feedL, feedC});
            feedH.addChild(adx5);
            feedL.addChild(adx5);
            feedC.addChild(adx5);
            bufferedTransformers.add(adx5);

            FXModuloFeed feedModulo = new FXModuloFeed(feed, 0.0001, 100);
            feed.addChild(feedModulo);

            SlopeTransformer gradH = new SlopeTransformer(20, feedH);
            feedH.addChild(gradH);
            bufferedTransformers.add(gradH);
            SlopeTransformer gradL = new SlopeTransformer(20, feedL);
            feedL.addChild(gradL);
            bufferedTransformers.add(gradL);

            SlopeTransformer gradH2 = new SlopeTransformer(100, feedH);
            feedH.addChild(gradH2);
            bufferedTransformers.add(gradH2);
            SlopeTransformer gradL2 = new SlopeTransformer(100, feedL);
            feedL.addChild(gradL2);
            bufferedTransformers.add(gradL2);

            SlopeTransformer gradH3 = new SlopeTransformer(200, feedH);
            feedH.addChild(gradH3);
            bufferedTransformers.add(gradH3);
            SlopeTransformer gradL3 = new SlopeTransformer(200, feedL);
            feedL.addChild(gradL3);
            bufferedTransformers.add(gradL3);

            SlopeTransformer gradH4 = new SlopeTransformer(400, feedH);
            feedH.addChild(gradH4);
            bufferedTransformers.add(gradH4);
            SlopeTransformer gradL4 = new SlopeTransformer(400, feedL);
            feedL.addChild(gradL4);
            bufferedTransformers.add(gradL4);

            StandardDeviationTransformer stdFeedH = new StandardDeviationTransformer(12, 2, feedH);
            feedH.addChild(stdFeedH);
            bufferedTransformers.add(stdFeedH);
            StandardDeviationTransformer stdFeedL = new StandardDeviationTransformer(12, 2, feedL);
            feedL.addChild(stdFeedL);
            bufferedTransformers.add(stdFeedL);
            StandardDeviationTransformer stdFeedV = new StandardDeviationTransformer(12, 2, feedV);
            feedV.addChild(stdFeedV);
            bufferedTransformers.add(stdFeedV);

            AmplitudeWavelengthTransformer awFeedH = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedV = new AmplitudeWavelengthTransformer(feedV, stdFeedV, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedH2 = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2 = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 4, 0.5);

            StandardDeviationTransformer stdFeedH2 = new StandardDeviationTransformer(50, 2, feedH);
            feedH.addChild(stdFeedH2);
            bufferedTransformers.add(stdFeedH2);
            StandardDeviationTransformer stdFeedL2 = new StandardDeviationTransformer(50, 2, feedL);
            feedL.addChild(stdFeedL2);
            bufferedTransformers.add(stdFeedL2);
            StandardDeviationTransformer stdFeedV2 = new StandardDeviationTransformer(50, 2, feedV);
            feedV.addChild(stdFeedV2);
            bufferedTransformers.add(stdFeedV2);

            AmplitudeWavelengthTransformer awFeedH50 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL50 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedV50 = new AmplitudeWavelengthTransformer(feedV, stdFeedV2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedH2_50 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2_50 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 4, 0.5);

            StandardDeviationTransformer stdFeedH3 = new StandardDeviationTransformer(100, 2, feedH);
            feedH.addChild(stdFeedH3);
            bufferedTransformers.add(stdFeedH3);
            StandardDeviationTransformer stdFeedL3 = new StandardDeviationTransformer(100, 2, feedL);
            feedL.addChild(stdFeedL3);
            bufferedTransformers.add(stdFeedL3);
            StandardDeviationTransformer stdFeedV3 = new StandardDeviationTransformer(100, 2, feedV);
            feedV.addChild(stdFeedV3);
            bufferedTransformers.add(stdFeedV3);

            AmplitudeWavelengthTransformer awFeedH100 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL100 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedV100 = new AmplitudeWavelengthTransformer(feedV, stdFeedV2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedH2_100 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2_100 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 4, 0.5);

            RSITransformer rsiH = new RSITransformer(20, 5, 5, MAType.Sma, feedH);
            feedH.addChild(rsiH);
            bufferedTransformers.add(rsiH);
            RSITransformer rsiL = new RSITransformer(20, 5, 5, MAType.Sma, feedL);
            feedL.addChild(rsiL);
            bufferedTransformers.add(rsiL);

            RSITransformer rsiH3 = new RSITransformer(40, 10, 10, MAType.Sma, feedH);
            feedH.addChild(rsiH3);
            bufferedTransformers.add(rsiH3);
            RSITransformer rsiL3 = new RSITransformer(40, 10, 10, MAType.Sma, feedL);
            feedL.addChild(rsiL3);
            bufferedTransformers.add(rsiL3);

            RSITransformer rsiH2 = new RSITransformer(100, 25, 25, MAType.Sma, feedH);
            feedH.addChild(rsiH2);
            bufferedTransformers.add(rsiH2);
            RSITransformer rsiL2 = new RSITransformer(100, 25, 25, MAType.Sma, feedL);
            feedL.addChild(rsiL2);
            bufferedTransformers.add(rsiL2);

            RSITransformer rsiH5 = new RSITransformer(200, 50, 50, MAType.Sma, feedH);
            feedH.addChild(rsiH5);
            bufferedTransformers.add(rsiH5);
            RSITransformer rsiL5 = new RSITransformer(100, 50, 50, MAType.Sma, feedL);
            feedL.addChild(rsiL5);
            bufferedTransformers.add(rsiL5);

            RSITransformer rsiH4 = new RSITransformer(400, 100, 100, MAType.Sma, feedH);
            feedH.addChild(rsiH4);
            bufferedTransformers.add(rsiH4);
            RSITransformer rsiL4 = new RSITransformer(400, 100, 100, MAType.Sma, feedL);
            feedL.addChild(rsiL4);
            bufferedTransformers.add(rsiL4);

            synch = new SynchronisedFeed(feedH, synch);
            synch = new SynchronisedFeed(feedL, synch);
            synch = new SynchronisedFeed(feedC, synch);
            synch = new SynchronisedFeed(feedV, synch);

            synch = new SynchronisedFeed(feedDiff, synch);
            synch = new SynchronisedFeed(feedModulo, synch);

            synch = new SynchronisedFeed(gradH, synch);
            synch = new SynchronisedFeed(gradH2, synch);
            synch = new SynchronisedFeed(gradH3, synch);
            synch = new SynchronisedFeed(gradH4, synch);

            synch = new SynchronisedFeed(gradL, synch);
            synch = new SynchronisedFeed(gradL2, synch);
            synch = new SynchronisedFeed(gradL3, synch);
            synch = new SynchronisedFeed(gradL4, synch);

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
            synch = new SynchronisedFeed(rsiH2, synch);
            synch = new SynchronisedFeed(rsiL2, synch);
            synch = new SynchronisedFeed(rsiH3, synch);
            synch = new SynchronisedFeed(rsiL3, synch);
            synch = new SynchronisedFeed(rsiH4, synch);
            synch = new SynchronisedFeed(rsiL4, synch);
            synch = new SynchronisedFeed(rsiH5, synch);
            synch = new SynchronisedFeed(rsiL5, synch);

            synch = new SynchronisedFeed(adx, synch);
            synch = new SynchronisedFeed(adx1, synch);
            synch = new SynchronisedFeed(adx2, synch);
            synch = new SynchronisedFeed(adx3, synch);
            synch = new SynchronisedFeed(adx4, synch);
            synch = new SynchronisedFeed(adx5, synch);
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
