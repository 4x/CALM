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
import ai.context.util.communication.Notifiable;
import ai.context.util.common.GetFirstLine;
import ai.context.util.measurement.LoggerTimer;
import ai.context.util.trading.BlackBox;
import ai.context.util.trading.PositionFactory;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.IClient;
import com.tictactec.ta.lib.MAType;

import java.util.*;

public class LearnHistorical implements Notifiable{

    private Learner trader;
    private LearnerFeed learnerFeed;

    private Set<BufferedTransformer> bufferedTransformers = new HashSet<>();
    private StitchableFeed liveFXCalendar;
    private StitchableFeed liveFXRate;
    private BlackBox blackBox;
    private IClient client;

    private boolean live = false;

    public static void main(String[] args)
    {
        final LearnHistorical test = new LearnHistorical();
        String path = "C:/Users/Oblene/Desktop/Sandbox/Data/";
        if(!(args == null || args.length == 0))
        {
            path = args[0];
        }
        test.initFXAPI();

        test.setLiveFXCalendar(new StitchableFXStreetCalendarRSS(path + "tmp/FXCalendar.csv", new FXStreetCalendarRSSFeed()));
        test.setLiveFXRate(new StitchableFXRate(path + "tmp/FXRate.csv", new DukascopyFeed(test.getClient(), Period.FIVE_MINS, Instrument.EURUSD)));

        System.out.println("Please wait while live feeds are initialised");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /*try {
            System.out.println("Live feeds are up and running, press enter to see the starts of the live feeds");
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        System.out.println("From FXCalendar:\n" + GetFirstLine.fromFile(test.getLiveFXCalendar().getLiveFileName()));
        System.out.println("From FXRate:\n" + GetFirstLine.fromFile(test.getLiveFXRate().getLiveFileName()));

        /*System.out.println("\nPlease patch the relevant historical files and press enter to start");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }*/



        TimerTask checkIfGoLive = new TimerTask() {
            @Override
            public void run() {
                if(!test.live && test.trader.getTime() > 1368342049000L){
                    for(BufferedTransformer transformer : test.bufferedTransformers){
                        transformer.goLive();
                        System.err.println(transformer + " going live...");
                    }
                    test.live = true;
                    System.err.println("Going LIVE!!!");
                }
            }
        };

        TimerTask checkIfGoTrading = new TimerTask() {
            @Override
            public void run() {
                if(Math.abs(test.trader.getTime() - System.currentTimeMillis()) < 10 * 60 * 1000L){
                    test.trader.setLive();
                }
            }
        };

        Timer timer = new Timer();
        //timer.schedule(checkIfGoLive, 6 * 60 * 60000L, 60000);
        timer.schedule(checkIfGoLive, 10000L, 1000);

        timer.schedule(checkIfGoTrading, 6 * 60 * 60000L, 60000);

        test.setTraderOutput(path);
        test.setup(path);
        test.trade();
    }

    public void initFXAPI(){
        try {
            //client = new DukascopyConnection("DEMO2UANZY", "UANZY").getClient();
            client = new DukascopyConnection("DEMO2Ffpfg", "Ffpfg").getClient();

            blackBox = new BlackBox(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTraderOutput(String output){
        trader = new Learner(output);
        trader.setBlackBox(blackBox);
    }

    public void setLiveFXCalendar(final StitchableFeed liveFXCalendar) {
        this.liveFXCalendar = liveFXCalendar;
        //liveFXCalendar.setNotifiable(this);
        /*Runnable liveFeed = new Runnable() {
            @Override
            public void run() {
                liveFXCalendar.startPadding();
            }
        };
        new Thread(liveFeed).start();*/
    }

    public void setLiveFXRate(final StitchableFeed liveFXRate) {
        this.liveFXRate = liveFXRate;
        //liveFXRate.setNotifiable(this);
        /*Runnable liveFeed = new Runnable() {
            @Override
            public void run() {
                liveFXRate.startPadding();
            }
        };
        new Thread(liveFeed).start();*/
    }

    public StitchableFeed getLiveFXCalendar() {
        return liveFXCalendar;
    }

    public StitchableFeed getLiveFXRate() {
        return liveFXRate;
    }

    public void setup(String path)
    {
        DataType[] typesCalendar = new DataType[]{
                DataType.OTHER,
                DataType.OTHER,
                DataType.INTEGER,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE};

        CSVFeed feedCalendar = new CSVFeed(path + "feeds/Calendar_2008.csv", "yyyyMMdd HH:mm:ss", typesCalendar, null);
        feedCalendar.setStitchableFeed(liveFXCalendar);
        feedCalendar.setInterval(5 * 60 * 1000L);
        feedCalendar.setPaddable(true);
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




        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        CSVFeed feedPriceEUR = new CSVFeed(path + "feeds/EURUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, null);
        feedPriceEUR.setStitchableFeed(liveFXRate);
        feedPriceEUR.setInterval(10 * 1000L);
        //CSVFeed feedPriceGBP = new CSVFeed(path + "feeds/GBPUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);
        //CSVFeed feedPriceCHF = new CSVFeed(path + "feeds/USDCHF_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);
        //CSVFeed feedPriceAUD = new CSVFeed(path + "feeds/AUDUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);


        SynchronisedFeed feed = buildSynchFeed(feedPriceEUR);
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
        feed = new SynchronisedFeed(tFeed, feed);
        int i = 0;
        while (true)
        {
            FeedObject data = feed.getNextComposite(this);
            trader.setCurrentTime(data.getTimeStamp());

            i++;

            if(i  == 550000)
            {
                break;
            }
        }

        learnerFeed = new LearnerFeedFromSynchronisedFeed(feed);

        trader.setActionResolution(0.0001);
        trader.setTrainingLearnerFeed(learnerFeed);
        trader.setMaxPopulation(5000);
        trader.setTimeShift(4 * 12 * 5 * 60 * 1000L);
        trader.setTolerance(5);

        PositionFactory.setRewardRiskRatio(3.0);
        PositionFactory.setMinTakeProfit(0.0050);
        PositionFactory.setAmount(10000);
        PositionFactory.setCost(0.00008);
        PositionFactory.setTradeToCapRatio(0.01);
        LoggerTimer.turn(false);

        i = 0;
        while (true)
        {
            DataObject data = learnerFeed.readNext();
            trader.setCurrentTime(data.getTimeStamp());

            System.out.println(new Date(data.getTimeStamp()) + " " + data);
            i++;

            /*if(i  == 10000)
            {
                break;
            }*/
        }
    }

    public void trade()
    {
        //new Thread(trader).start();
        trader.run();
    }


    private SynchronisedFeed buildSynchFeed(CSVFeed ... feeds)
    {
        SynchronisedFeed synch = null;
        for(CSVFeed feed : feeds){
            ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
            feed.addChild(feedH);
            ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
            feed.addChild(feedL);
            ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);
            feed.addChild(feedC);
            ExtractOneFromListFeed feedO = new ExtractOneFromListFeed(feed, 0);
            feed.addChild(feedO);
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
            StandardDeviationTransformer stdFeedO = new StandardDeviationTransformer(12, 2, feedO);
            feedO.addChild(stdFeedO);
            bufferedTransformers.add(stdFeedO);
            StandardDeviationTransformer stdFeedV = new StandardDeviationTransformer(12, 2, feedV);
            feedV.addChild(stdFeedV);
            bufferedTransformers.add(stdFeedV);

            AmplitudeWavelengthTransformer awFeedH = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 2, 0.5);
            feedH.addChild(awFeedH);
            stdFeedH.addChild(awFeedH);
            AmplitudeWavelengthTransformer awFeedL = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 2, 0.5);
            feedL.addChild(awFeedL);
            stdFeedL.addChild(awFeedL);
            /*AmplitudeWavelengthTransformer awFeedC = new AmplitudeWavelengthTransformer(feedC, stdFeedC, 2, 0.5);
            feedC.addChild(awFeedC);
            stdFeedC.addChild(awFeedC);
            AmplitudeWavelengthTransformer awFeedO = new AmplitudeWavelengthTransformer(feedO, stdFeedO, 2, 0.5);
            feedO.addChild(awFeedO);
            stdFeedO.addChild(awFeedO);*/
            AmplitudeWavelengthTransformer awFeedV = new AmplitudeWavelengthTransformer(feedV, stdFeedV, 2, 0.5);
            feedV.addChild(awFeedV);
            stdFeedV.addChild(awFeedV);

            AmplitudeWavelengthTransformer awFeedH2 = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 4, 0.5);
            feedH.addChild(awFeedH2);
            stdFeedH.addChild(awFeedH2);
            AmplitudeWavelengthTransformer awFeedL2 = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 4, 0.5);
            feedL.addChild(awFeedL2);
            stdFeedL.addChild(awFeedL2);

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

            /*LiveStandardDeviationTransformer stdFeedH1 = new LiveStandardDeviationTransformer(5, 2, feedH);
            feedH.addChild(stdFeedH1);
            LiveStandardDeviationTransformer stdFeedL1 = new LiveStandardDeviationTransformer(5, 2, feedL);
            feedL.addChild(stdFeedL1);
            LiveStandardDeviationTransformer stdFeedC1 = new LiveStandardDeviationTransformer(5, 2, feedC);
            feedC.addChild(stdFeedC1);
            LiveStandardDeviationTransformer stdFeedO1 = new LiveStandardDeviationTransformer(5, 2, feedO);
            feedO.addChild(stdFeedO1);
            LiveStandardDeviationTransformer stdFeedV1 = new LiveStandardDeviationTransformer(5, 2, feedV);
            feedV.addChild(stdFeedV1);

            AmplitudeWavelengthTransformer awFeedH1 = new AmplitudeWavelengthTransformer(feedH, stdFeedH1, 2, 0.5);
            feedH.addChild(awFeedH1);
            stdFeedH1.addChild(awFeedH1);
            AmplitudeWavelengthTransformer awFeedL1 = new AmplitudeWavelengthTransformer(feedL, stdFeedL1, 2, 0.5);
            feedL.addChild(awFeedL1);
            stdFeedL1.addChild(awFeedL1);
            AmplitudeWavelengthTransformer awFeedC1 = new AmplitudeWavelengthTransformer(feedC, stdFeedC1, 2, 0.5);
            feedC.addChild(awFeedC1);
            stdFeedC1.addChild(awFeedC1);
            AmplitudeWavelengthTransformer awFeedO1 = new AmplitudeWavelengthTransformer(feedO, stdFeedO1, 2, 0.5);
            feedO.addChild(awFeedO1);
            stdFeedO1.addChild(awFeedO1);
            AmplitudeWavelengthTransformer awFeedV1 = new AmplitudeWavelengthTransformer(feedV, stdFeedV1, 2, 0.5);
            feedV.addChild(awFeedV1);
            stdFeedV1.addChild(awFeedV1);
*/
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

    int countLive = 0;
    @Override
    public void notifyFor(String event) {
        countLive++;
        trader.setLive();
    }

    public IClient getClient() {
        return client;
    }
}
