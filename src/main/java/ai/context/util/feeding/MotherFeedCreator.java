package ai.context.util.feeding;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
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
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.feed.synchronised.MinMaxAggregatorDiscretiser;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.feed.transformer.compound.AbsoluteAmplitudeWavelengthTransformer;
import ai.context.feed.transformer.compound.SubstractTransformer;
import ai.context.feed.transformer.series.online.*;
import ai.context.feed.transformer.single.TimeVariablesAppenderFeed;
import ai.context.feed.transformer.single.unpadded.LogarithmicDiscretiser;
import ai.context.learning.neural.NeuronCluster;
import ai.context.trading.DukascopyConnection;
import ai.context.util.analysis.LookAheadScheduler;
import ai.context.util.configuration.PropertiesHolder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.IClient;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MotherFeedCreator {

    private static int iManipulator = 1;
    private static IClient client;
    private static Set<RowFeed> rowFeeds = new HashSet<>();

    private static Set<Feed> feedsForCorrelation = new HashSet<>();

    private static StitchableFeed liveFXCalendar;
    private static StitchableFeed liveFXCalendarScheduler;
    private static StitchableFeed liveFXRateEUR;
    private static StitchableFeed liveFXRateGBP;
    private static StitchableFeed liveFXRateJPY;
    private static StitchableFeed liveFXRateAUD;
    private static StitchableFeed liveFXRateCHF;

    public static ISynchFeed getMotherFeed(String path){
        if (PropertiesHolder.liveTrading) {
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
            addManipulator("United Kingdom", "Unemployment Rate", calendarWrapper, scheduler);
            //addManipulator("United Kingdom", "Retail Price Index (MoM)", calendarWrapper, scheduler);

            //addManipulator("United States", "Average Hourly Earnings (MoM)", calendarWrapper, scheduler);
            //addManipulator("United States", "Consumer Confidence", calendarWrapper, scheduler);
            addManipulator("United States", "Consumer Price Index (MoM)", calendarWrapper, scheduler);
            addManipulator("United States", "Fed Interest Rate Decision", calendarWrapper, scheduler);
            addManipulator("United States", "Gross Domestic Product (QoQ)", calendarWrapper, scheduler);
            addManipulator("United States", "Nonfarm Payrolls", calendarWrapper, scheduler);
            addManipulator("United States", "Producer Price Index (MoM)", calendarWrapper, scheduler);
            //addManipulator("United States", "Trade Balance", calendarWrapper, scheduler);
            addManipulator("United States", "Unemployment Rate", calendarWrapper, scheduler);


            NeuronCluster.getInstance().addFeedWrapper(calendarWrapper);
        }

        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = PropertiesHolder.startDateTime;

        CSVFeed feedPriceEUR = new CSVFeed(path + "feeds/" + PropertiesHolder.fxFolder + "EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceEUR.setStitchableFeed(liveFXRateEUR);
        feedPriceEUR.setSkipWeekends(true);
        rowFeeds.add(feedPriceEUR);

        CSVFeed feedPriceGBP = new CSVFeed(path + "feeds/" + PropertiesHolder.fxFolder + "GBPUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceGBP.setStitchableFeed(liveFXRateGBP);
        feedPriceGBP.setSkipWeekends(true);
        rowFeeds.add(feedPriceGBP);

        CSVFeed feedPriceJPY = new CSVFeed(path + "feeds/" + PropertiesHolder.fxFolder + "USDJPY.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceJPY.setStitchableFeed(liveFXRateJPY);
        feedPriceJPY.setSkipWeekends(true);
        rowFeeds.add(feedPriceJPY);

        CSVFeed feedPriceAUD = new CSVFeed(path + "feeds/" + PropertiesHolder.fxFolder + "AUDUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceAUD.setStitchableFeed(liveFXRateAUD);
        feedPriceAUD.setSkipWeekends(true);
        rowFeeds.add(feedPriceAUD);

        CSVFeed feedPriceCHF = new CSVFeed(path + "feeds/" + PropertiesHolder.fxFolder + "USDCHF.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feedPriceCHF.setStitchableFeed(liveFXRateCHF);
        feedPriceCHF.setSkipWeekends(true);
        rowFeeds.add(feedPriceCHF);

        ISynchFeed feed = buildSynchFeed(null, 0.0001,feedPriceEUR, true);
        feed = buildSynchFeed(feed, 0.0001,feedPriceGBP, false);
        feed = buildSynchFeed(feed, 0.01, feedPriceJPY, false);
        feed = buildSynchFeed(feed, 0.0001, feedPriceCHF, false);
        feed = buildSynchFeed(feed, 0.0001, feedPriceAUD, false);

        createCorrelationFeeds(feed, new ExtractOneFromListFeed(feedPriceEUR, 3));

        MinMaxAggregatorDiscretiser sFeed = new MinMaxAggregatorDiscretiser(feed, PropertiesHolder.initialSeriesOffset, 12);
        sFeed.lock();

        TimeVariablesAppenderFeed tFeed = new TimeVariablesAppenderFeed(sFeed);

        ISynchFeed synchFeed = new SynchronisedFeed();
        synchFeed.addRawFeed(feedPriceEUR);
        synchFeed.addRawFeed(tFeed);

        int i = 0;
        while (true) {
            FeedObject data = synchFeed.getNextComposite(null);
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

    public static void initFXAPI() {
        try {
            MotherFeedCreator.client = new DukascopyConnection(PropertiesHolder.dukascopyLogin, PropertiesHolder.dukascopyPass).getClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setLiveFXRates(String path) {
        liveFXRateEUR = new StitchableFXRate(path + "tmp/FX1Rate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.EURUSD, path + "feeds/" + PropertiesHolder.fxFolder + "EURUSD.csv"));
        liveFXRateGBP = new StitchableFXRate(path + "tmp/FX2Rate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.GBPUSD, path + "feeds/" + PropertiesHolder.fxFolder + "GBPUSD.csv"));
        liveFXRateJPY = new StitchableFXRate(path + "tmp/FX3Rate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.USDJPY, path + "feeds/" + PropertiesHolder.fxFolder + "USDJPY.csv"));
        liveFXRateCHF = new StitchableFXRate(path + "tmp/FX4Rate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.USDCHF, path + "feeds/" + PropertiesHolder.fxFolder + "USDCHF.csv"));
        liveFXRateAUD = new StitchableFXRate(path + "tmp/FX5Rate.csv", new DukascopyFeed(client, Period.THIRTY_MINS, Instrument.AUDUSD, path + "feeds/" + PropertiesHolder.fxFolder + "AUDUSD.csv"));
    }

    private static ISynchFeed createCorrelationFeeds(ISynchFeed synchFeed, Feed observable){
        for(Feed feed : feedsForCorrelation){
            synchFeed.addRawFeed(new CorrelationOnlineTransformer(observable, feed, 20));
            synchFeed.addRawFeed(new CorrelationOnlineTransformer(observable, feed, 50));
            synchFeed.addRawFeed(new CorrelationOnlineTransformer(observable, feed, 200));
        }
        return synchFeed;
    }

    public static void setLiveFXCalendar(final StitchableFeed liveFXCalendar) {
        MotherFeedCreator.liveFXCalendar = liveFXCalendar;
    }

    public static void setLiveFXCalendarSchedule(final StitchableFeed liveFXCalendarSchedule) {
        MotherFeedCreator.liveFXCalendarScheduler = liveFXCalendarSchedule;
    }

    private static ISynchFeed buildSynchFeed(ISynchFeed synch, double res, CSVFeed feed, boolean main) {
        if (synch == null) {
            synch = new SynchronisedFeed();
        }
        ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
        ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
        ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);
        ExtractOneFromListFeed feedV = new ExtractOneFromListFeed(feed, 4);

        //synch.addRawFeed(feedV);

        MAOnlineTransformer maV5 = new MAOnlineTransformer(5, feedV);
        SubstractTransformer substract_MAV5 = new SubstractTransformer(feedV, maV5);
        synch.addRawFeed(substract_MAV5);
        feedsForCorrelation.add(substract_MAV5);

        MAOnlineTransformer maV10 = new MAOnlineTransformer(10, feedV);
        SubstractTransformer substract_MAV10 = new SubstractTransformer(feedV, maV10);
        synch.addRawFeed(substract_MAV10);
        feedsForCorrelation.add(substract_MAV10);

        MAOnlineTransformer maV30 = new MAOnlineTransformer(30, feedV);
        SubstractTransformer substract_MAV30 = new SubstractTransformer(feedV, maV30);
        synch.addRawFeed(substract_MAV30);
        feedsForCorrelation.add(substract_MAV30);

        MAOnlineTransformer maC5 = new MAOnlineTransformer(5, feedC);
        MAOnlineTransformer maC10 = new MAOnlineTransformer(10, feedC);
        MAOnlineTransformer maC50 = new MAOnlineTransformer(50, feedC);
        MAOnlineTransformer maC100 = new MAOnlineTransformer(100, feedC);

        SubstractTransformer substract_MAC5 = new SubstractTransformer(feedC, maC5);
        SubstractTransformer substract_MAC10 = new SubstractTransformer(feedC, maC10);
        SubstractTransformer substract_MAC50 = new SubstractTransformer(feedC, maC50);
        SubstractTransformer substract_MAC100 = new SubstractTransformer(feedC, maC100);

        synch.addRawFeed(substract_MAC5);
        synch.addRawFeed(substract_MAC10);
        synch.addRawFeed(substract_MAC50);
        synch.addRawFeed(substract_MAC100);

        feedsForCorrelation.add(substract_MAC5);
        feedsForCorrelation.add(substract_MAC10);
        feedsForCorrelation.add(substract_MAC50);
        feedsForCorrelation.add(substract_MAC100);

        MAOnlineTransformer maH5 = new MAOnlineTransformer(5, feedH);
        MAOnlineTransformer maH10 = new MAOnlineTransformer(10, feedH);
        MAOnlineTransformer maH50 = new MAOnlineTransformer(50, feedH);
        MAOnlineTransformer maH100 = new MAOnlineTransformer(100, feedH);

        SubstractTransformer substract_MAH5 = new SubstractTransformer(feedH, maH5);
        SubstractTransformer substract_MAH10 = new SubstractTransformer(feedH, maH10);
        SubstractTransformer substract_MAH50 = new SubstractTransformer(feedH, maH50);
        SubstractTransformer substract_MAH100 = new SubstractTransformer(feedH, maH100);

        synch.addRawFeed(substract_MAH5);
        synch.addRawFeed(substract_MAH10);
        synch.addRawFeed(substract_MAH50);
        synch.addRawFeed(substract_MAH100);

        MAOnlineTransformer maL5 = new MAOnlineTransformer(5, feedL);
        MAOnlineTransformer maL10 = new MAOnlineTransformer(10, feedL);
        MAOnlineTransformer maL50 = new MAOnlineTransformer(50, feedL);
        MAOnlineTransformer maL100 = new MAOnlineTransformer(100, feedL);

        SubstractTransformer substract_MAL5 = new SubstractTransformer(feedL, maL5);
        SubstractTransformer substract_MAL10 = new SubstractTransformer(feedL, maL10);
        SubstractTransformer substract_MAL50 = new SubstractTransformer(feedL, maL50);
        SubstractTransformer substract_MAL100 = new SubstractTransformer(feedL, maL100);

        synch.addRawFeed(substract_MAL5);
        synch.addRawFeed(substract_MAL10);
        synch.addRawFeed(substract_MAL50);
        synch.addRawFeed(substract_MAL100);

        SubstractTransformer cHDiff = new SubstractTransformer(feedH, feedC);
        LogarithmicDiscretiser cHDiffL = new LogarithmicDiscretiser(res, 0, cHDiff, -1);
        synch.addRawFeed(cHDiffL);

        SubstractTransformer cLDiff = new SubstractTransformer(feedC, feedL);
        LogarithmicDiscretiser cLDiffL = new LogarithmicDiscretiser(res, 0, cLDiff, -1);
        synch.addRawFeed(cLDiffL);

        SubstractTransformer cHLDiff = new SubstractTransformer(cHDiff, cLDiff);
        LogarithmicDiscretiser cHLDiffL = new LogarithmicDiscretiser(res, 0, cHLDiff, -1);
        synch.addRawFeed(cHLDiffL);

        FXHLDiffFeed feedDiff = new FXHLDiffFeed(feed, res);
        synch.addRawFeed(feedDiff);

        StandardDeviationOnlineTransformer stdFeedC2 = new StandardDeviationOnlineTransformer(10, feedC);
        LogarithmicDiscretiser stdLC2 = new LogarithmicDiscretiser(res, 0, stdFeedC2, -1);
        synch.addRawFeed(stdLC2);

        StandardDeviationOnlineTransformer stdFeedC3 = new StandardDeviationOnlineTransformer(50, feedC);
        LogarithmicDiscretiser stdLC3 = new LogarithmicDiscretiser(res, 0, stdFeedC3, -1);
        synch.addRawFeed(stdLC3);

        SimpleTrendOnlineTransformer t0005 = new SimpleTrendOnlineTransformer(0.005, feedL, feedH, feedC, res);
        SimpleTrendOnlineTransformer t005 = new SimpleTrendOnlineTransformer(0.05, feedL, feedH, feedC, res);
        SimpleTrendOnlineTransformer t075 = new SimpleTrendOnlineTransformer(0.75, feedL, feedH, feedC, res);
        SimpleTrendOnlineTransformer t0125 = new SimpleTrendOnlineTransformer(0.125, feedL, feedH, feedC, res);
        SimpleTrendOnlineTransformer t025 = new SimpleTrendOnlineTransformer(0.25, feedL, feedH, feedC, res);
        SimpleTrendOnlineTransformer t05 = new SimpleTrendOnlineTransformer(0.5, feedL, feedH, feedC, res);
        synch.addRawFeed(t0125);
        synch.addRawFeed(t025);
        synch.addRawFeed(t05);
        synch.addRawFeed(t0005);
        synch.addRawFeed(t005);
        synch.addRawFeed(t075);
        feedsForCorrelation.add(t0005);
        feedsForCorrelation.add(t005);
        feedsForCorrelation.add(t075);
        feedsForCorrelation.add(t05);
        feedsForCorrelation.add(t025);
        feedsForCorrelation.add(t0125);

        SuddenShiftsOnlineTransformer ss10 = new SuddenShiftsOnlineTransformer(10, feedL, feedH, res, 7.5);
        SuddenShiftsOnlineTransformer ss50 = new SuddenShiftsOnlineTransformer(50, feedL, feedH, res, 12.5);
        SuddenShiftsOnlineTransformer ss5 = new SuddenShiftsOnlineTransformer(5, feedL, feedH, res, 7.5);
        SuddenShiftsOnlineTransformer ss20 = new SuddenShiftsOnlineTransformer(20, feedL, feedH, res, 10);
        SuddenShiftsOnlineTransformer ss100 = new SuddenShiftsOnlineTransformer(100, feedL, feedH, res, 15);
        synch.addRawFeed(ss20);
        synch.addRawFeed(ss100);
        synch.addRawFeed(ss10);
        synch.addRawFeed(ss50);

        AbsoluteAmplitudeWavelengthTransformer awFeedC5 = new AbsoluteAmplitudeWavelengthTransformer(feedC, 5, 0.25, res);
        synch.addRawFeed(awFeedC5);
        AbsoluteAmplitudeWavelengthTransformer awFeedC50 = new AbsoluteAmplitudeWavelengthTransformer(feedC, 50, 0.25, res);
        synch.addRawFeed(awFeedC50);
        AbsoluteAmplitudeWavelengthTransformer awFeedC10 = new AbsoluteAmplitudeWavelengthTransformer(feedC, 10, 0.125, res);
        synch.addRawFeed(awFeedC10);
        AbsoluteAmplitudeWavelengthTransformer awFeedC20 = new AbsoluteAmplitudeWavelengthTransformer(feedC, 20, 0.125, res);
        synch.addRawFeed(awFeedC20);

        MinMaxDistanceTransformer mmdT0 = new MinMaxDistanceTransformer(5, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT1 = new MinMaxDistanceTransformer(10, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT2 = new MinMaxDistanceTransformer(20, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT5 = new MinMaxDistanceTransformer(50, feedL, feedH, feedC, res);
        MinMaxDistanceTransformer mmdT7 = new MinMaxDistanceTransformer(100, feedL, feedH, feedC, res);

        synch.addRawFeed(mmdT0);
        synch.addRawFeed(mmdT1);
        synch.addRawFeed(mmdT2);
        synch.addRawFeed(mmdT5);
        synch.addRawFeed(mmdT7);

        RSIOnlineTransformer rsiC = new RSIOnlineTransformer(feedC, 5, 25, 0.5);
        RSIOnlineTransformer rsiC2 = new RSIOnlineTransformer(feedC, 10, 50, 0.05);
        synch.addRawFeed(rsiC);
        synch.addRawFeed(rsiC2);

        RSIOnlineTransformer rsiH = new RSIOnlineTransformer(feedH, 5, 25, 0.5);
        RSIOnlineTransformer rsiH2 = new RSIOnlineTransformer(feedH, 10, 50, 0.05);
        synch.addRawFeed(rsiH);
        synch.addRawFeed(rsiH2);

        RSIOnlineTransformer rsiL = new RSIOnlineTransformer(feedL, 5, 25, 0.5);
        RSIOnlineTransformer rsiL2 = new RSIOnlineTransformer(feedL, 10, 50, 0.05);
        synch.addRawFeed(rsiL);
        synch.addRawFeed(rsiL2);

        RadarOnlineTransformer r0 = new RadarOnlineTransformer(10, feedL, feedH, feedC, res);
        RadarOnlineTransformer r1 = new RadarOnlineTransformer(20, feedL, feedH, feedC, res);
        RadarOnlineTransformer r2 = new RadarOnlineTransformer(50, feedL, feedH, feedC, res);
        RadarOnlineTransformer r3 = new RadarOnlineTransformer(100, feedL, feedH, feedC, res);
        RadarOnlineTransformer r4 = new RadarOnlineTransformer(200, feedL, feedH, feedC, res);
        synch.addRawFeed(r0);
        synch.addRawFeed(r1);
        synch.addRawFeed(r2);
        synch.addRawFeed(r3);
        synch.addRawFeed(r4);

        return synch;
    }

    private static void addManipulator(String country, String description, FeedWrapper calendarWrapper, LookAheadScheduler scheduler){
        Manipulator manipulator = new TimeDecaySingleSentimentManipulator(country, description, scheduler);
        calendarWrapper.putManipulator("" + iManipulator, manipulator);

        iManipulator++;
    }

    public static IClient getClient(){
        return client;
    }

    public static Set<RowFeed> getRowFeeds() {
         return rowFeeds;
    }
}

