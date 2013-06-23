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
import ai.context.feed.synchronised.SmartDiscretiserOnSynchronisedFeed;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.feed.transformer.compound.AmplitudeWavelengthTransformer;
import ai.context.feed.transformer.compound.SubtractTransformer;
import ai.context.feed.transformer.filtered.RowBasedTransformer;
import ai.context.feed.transformer.series.learning.BufferedTransformer;
import ai.context.feed.transformer.series.learning.RSITransformer;
import ai.context.feed.transformer.series.learning.SlopeTransformer;
import ai.context.feed.transformer.series.learning.StandardDeviationTransformer;
import ai.context.feed.transformer.single.TimeVariablesAppenderFeed;
import ai.context.feed.transformer.single.unpadded.LinearDiscretiser;
import ai.context.learning.DataObject;
import ai.context.learning.Learner;
import ai.context.learning.LearnerFeed;
import ai.context.learning.LearnerFeedFromSynchronisedFeed;
import ai.context.trading.DukascopyConnection;
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

    private boolean testing = false;

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
    }

    public void setup(String path)
    {
        initFXAPI();

        long interval = 1*60000L;
        setLiveFXCalendar(new StitchableFXStreetCalendarRSS(path + "tmp/FXCalendar.csv", new FXStreetCalendarRSSFeed()));
        setLiveFXRates(
                new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.FIVE_MINS, Instrument.EURUSD)),
                new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.FIVE_MINS, Instrument.GBPUSD)),
                new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(client, Period.FIVE_MINS, Instrument.USDCHF)));

        final Timer timer = new Timer();
        TimerTask checkIfGoLive = new TimerTask() {
            @Override
            public void run() {
                if(!isLive && trader.getTime() > 1368342049000L){
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
            }
        };

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

        timer.schedule(checkIfGoLive, 10000L, 500);
        timer.schedule(checkIfGoTrading, 10000L, 500);

        DataType[] typesCalendar = new DataType[]{
                DataType.OTHER,
                DataType.OTHER,
                DataType.INTEGER,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE};

        CSVFeed feedCalendar = new CSVFeed(path + "feeds/Calendar_2008.csv", "yyyyMMdd HH:mm:ss", typesCalendar, "20080101 00:00:00");
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

        CSVFeed feedPriceEUR = new CSVFeed(path + "feeds/EURUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  "2008.01.01 00:00:00");
        feedPriceEUR.setStitchableFeed(liveFXRateEUR);
        CSVFeed feedPriceGBP = new CSVFeed(path + "feeds/GBPUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  "2008.01.01 00:00:00");
        feedPriceGBP.setStitchableFeed(liveFXRateGBP);
        CSVFeed feedPriceCHF = new CSVFeed(path + "feeds/USDCHF_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  "2008.01.01 00:00:00");
        feedPriceCHF.setStitchableFeed(liveFXRateCHF);
        //CSVFeed feedPriceAUD = new CSVFeed(path + "feeds/AUDUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);


        SynchronisedFeed feed = buildSynchFeed(null, feedPriceEUR);
        feed = buildSynchFeed(feed, feedPriceGBP);
        feed = buildSynchFeed(feed, feedPriceCHF);
        SmartDiscretiserOnSynchronisedFeed sFeed = new SmartDiscretiserOnSynchronisedFeed(feed, 500, 5);
        feed.addChild(sFeed);
        TimeVariablesAppenderFeed tFeed = new TimeVariablesAppenderFeed(sFeed);
        sFeed.addChild(tFeed);

        feed = new SynchronisedFeed(feedPriceEUR, null);
        feed = addToSynchFeed(feed, f1, 50, 0);
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

            if(testing)
            {
                if(i == 550000){
                    break;
                }
            }
            else if(i  == 10000)
            {
                break;
            }
        }

        /*for(final BufferedTransformer transformer : bufferedTransformers){
            transformer.goLive();
        }*/
        LearnerFeed learnerFeed = new LearnerFeedFromSynchronisedFeed(feed);

        trader.setActionResolution(0.0001);
        trader.setTrainingLearnerFeed(learnerFeed);
        trader.setMaxPopulation(5000);
        trader.setTimeShift(4 * 12 * 5 * 60 * 1000L);
        trader.setTolerance(5);

        PositionFactory.setRewardRiskRatio(4.0);
        PositionFactory.setMinTakeProfit(0.0050);
        PositionFactory.setAmount(10000);
        PositionFactory.setCost(0.00015);
        PositionFactory.setTradeToCapRatio(0.01);
        PositionFactory.setLeverage(25);

        PositionFactory.setMinProbFraction(0.75);
        PositionFactory.setVerticalRisk(true);
        PositionFactory.setMinTakeProfitVertical(0.0020);
        LoggerTimer.turn(false);

        i = 0;
        while (true)
        {
            DataObject data = learnerFeed.readNext();
            trader.setCurrentTime(data.getTimeStamp());

            if(testing){
                System.out.println(new Date(data.getTimeStamp()) + " " + data);
            }
            i++;

            if(i  == 10000 && !testing)
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
            if(!testing){
                client = new DukascopyConnection("DEMO2LfagZ", "LfagZ").getClient();
                //client = new DukascopyConnection("DEMO2CiisQ", "CiisQ").getClient();
            }
            else {
                client = new DukascopyConnection("DEMO2Ffpfg", "Ffpfg").getClient();
            }

            blackBox = new BlackBox(client);
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
            //ExtractOneFromListFeed feedO = new ExtractOneFromListFeed(feed, 0);
            //feed.addChild(feedO);
            ExtractOneFromListFeed feedV = new ExtractOneFromListFeed(feed, 4);
            feed.addChild(feedV);

            FXHLDiffFeed feedDiff = new FXHLDiffFeed(feed);
            feed.addChild(feedDiff);

            SlopeTransformer gradH = new SlopeTransformer(12, feedH);
            feedH.addChild(gradH);
            bufferedTransformers.add(gradH);
            SlopeTransformer gradL = new SlopeTransformer(12, feedL);
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

            StandardDeviationTransformer stdFeedH = new StandardDeviationTransformer(12, 2, feedH);
            feedH.addChild(stdFeedH);
            bufferedTransformers.add(stdFeedH);
            StandardDeviationTransformer stdFeedL = new StandardDeviationTransformer(12, 2, feedL);
            feedL.addChild(stdFeedL);
            bufferedTransformers.add(stdFeedL);
            StandardDeviationTransformer stdFeedC = new StandardDeviationTransformer(12, 2, feedC);
            feedC.addChild(stdFeedC);
            bufferedTransformers.add(stdFeedC);
            //StandardDeviationTransformer stdFeedO = new StandardDeviationTransformer(12, 2, feedO);
            //feedO.addChild(stdFeedO);
            //bufferedTransformers.add(stdFeedO);
            StandardDeviationTransformer stdFeedV = new StandardDeviationTransformer(12, 2, feedV);
            feedV.addChild(stdFeedV);
            bufferedTransformers.add(stdFeedV);

            AmplitudeWavelengthTransformer awFeedH = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 2, 0.5);
            /*AmplitudeWavelengthTransformer awFeedC = new AmplitudeWavelengthTransformer(feedC, stdFeedC, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedO = new AmplitudeWavelengthTransformer(feedO, stdFeedO, 2, 0.5);*/
            AmplitudeWavelengthTransformer awFeedV = new AmplitudeWavelengthTransformer(feedV, stdFeedV, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedH2 = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2 = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 4, 0.5);

            RSITransformer rsiH = new RSITransformer(20, 5, 5, MAType.Sma, feedH);
            feedH.addChild(rsiH);
            bufferedTransformers.add(rsiH);
            RSITransformer rsiL = new RSITransformer(20, 5, 5, MAType.Sma, feedL);
            feedL.addChild(rsiL);
            bufferedTransformers.add(rsiL);

            RSITransformer rsiH2 = new RSITransformer(100, 25, 25, MAType.Sma, feedH);
            feedH.addChild(rsiH2);
            bufferedTransformers.add(rsiH2);
            RSITransformer rsiL2 = new RSITransformer(100, 25, 25, MAType.Sma, feedL);
            feedL.addChild(rsiL2);
            bufferedTransformers.add(rsiL2);

            /*AmplitudeWavelengthTransformer awFeedH1 = new AmplitudeWavelengthTransformer(feedH, stdFeedH1, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL1 = new AmplitudeWavelengthTransformer(feedL, stdFeedL1, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedC1 = new AmplitudeWavelengthTransformer(feedC, stdFeedC1, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedO1 = new AmplitudeWavelengthTransformer(feedO, stdFeedO1, 2, 0.5);;
            AmplitudeWavelengthTransformer awFeedV1 = new AmplitudeWavelengthTransformer(feedV, stdFeedV1, 2, 0.5);*/

            synch = new SynchronisedFeed(feedH, synch);
            synch = new SynchronisedFeed(feedL, synch);
            synch = new SynchronisedFeed(feedC, synch);
            /*synch = new SynchronisedFeed(feedO, synch);*/
            synch = new SynchronisedFeed(feedV, synch);

            synch = new SynchronisedFeed(feedDiff, synch);

            synch = new SynchronisedFeed(gradH, synch);
            synch = new SynchronisedFeed(gradH2, synch);
            synch = new SynchronisedFeed(gradH3, synch);

            synch = new SynchronisedFeed(gradL, synch);
            synch = new SynchronisedFeed(gradL2, synch);
            synch = new SynchronisedFeed(gradL3, synch);

            synch = new SynchronisedFeed(stdFeedH, synch);
            synch = new SynchronisedFeed(stdFeedL, synch);
            /*synch = new SynchronisedFeed(stdFeedC, synch);
            synch = new SynchronisedFeed(stdFeedO, synch);*/
            synch = new SynchronisedFeed(stdFeedV, synch);

            synch = new SynchronisedFeed(awFeedH, synch);
            synch = new SynchronisedFeed(awFeedL, synch);
            /*synch = new SynchronisedFeed(awFeedC, synch);
            synch = new SynchronisedFeed(awFeedO, synch);*/
            synch = new SynchronisedFeed(awFeedV, synch);

            synch = new SynchronisedFeed(awFeedH2, synch);
            synch = new SynchronisedFeed(awFeedL2, synch);

            synch = new SynchronisedFeed(rsiH, synch);
            synch = new SynchronisedFeed(rsiL, synch);
            synch = new SynchronisedFeed(rsiH2, synch);
            synch = new SynchronisedFeed(rsiL2, synch);
            /*synch = new SynchronisedFeed(stdFeedH1, synch);
            synch = new SynchronisedFeed(stdFeedL1, synch);
            synch = new SynchronisedFeed(stdFeedC1, synch);
            synch = new SynchronisedFeed(stdFeedO1, synch);
            synch = new SynchronisedFeed(stdFeedV1, synch);

            synch = new SynchronisedFeed(awFeedH1, synch);
            synch = new SynchronisedFeed(awFeedL1, synch);
            synch = new SynchronisedFeed(awFeedC1, synch);
            synch = new SynchronisedFeed(awFeedO1, synch);
            synch = new SynchronisedFeed(awFeedV1, synch);*/
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
        feed = new SynchronisedFeed(l1, feed);
        LinearDiscretiser l2 = new LinearDiscretiser(resolution, benchmark, s2, 0);
        feed = new SynchronisedFeed(l2, feed);

        LinearDiscretiser l3 = new LinearDiscretiser(0.1, 0, raw, 3);
        raw.addChild(l3);
        feed = new SynchronisedFeed(l3, feed);

        return feed;
    }
}
