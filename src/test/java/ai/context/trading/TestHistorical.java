package ai.context.trading;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.surgical.AbstractSurgicalFeed;
import ai.context.feed.surgical.ExtractOneFromListFeed;
import ai.context.feed.synchronised.SmartDiscretiserOnSynchronisedFeed;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.learning.DataObject;
import ai.context.learning.Learner;
import ai.context.learning.LearnerFeed;
import ai.context.learning.LearnerFeedFromSynchronisedFeed;
import ai.context.feed.transformer.compound.AmplitudeWavelengthTransformer;
import ai.context.feed.transformer.filtered.RowBasedTransformer;
import ai.context.feed.transformer.series.StandardDeviationTransformer;
import ai.context.feed.transformer.single.TimeVariablesAppenderFeed;
import ai.context.util.DataSetUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestHistorical {

    private Learner trader;
    private LearnerFeed learnerFeed;

    public static void main(String[] args)
    {
        TestHistorical test = new TestHistorical();
        String path = "C:\\Users\\Oblene\\Desktop\\Sandbox\\Data\\";
        if(!(args == null || args.length == 0))
        {
            path = args[0];
        }
        test.setTraderOutput(path);
        test.setup(path);
        test.trade();
    }

    public void setTraderOutput(String output){
        trader = new Learner("C:\\Users\\Oblene\\Desktop\\Sandbox");
    }
    @Before
    public void setup(String path)
    {
        DataType[] typesCalendar = new DataType[]{
                DataType.OTHER,
                DataType.OTHER,
                DataType.INTEGER,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE};

        CSVFeed feedCalendar = new CSVFeed(path + "feeds\\Calendar_2008.csv", "yyyyMMdd HH:mm:ss", typesCalendar);
        RowBasedTransformer f1 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0}, new String[]{"Nonfarm Payrolls"}, new int[]{3, 4, 5}, trader);

        RowBasedTransformer f2 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Consumer Price Index \\(MoM\\)", "United Kingdom"}, new int[]{3, 4, 5}, trader);
        RowBasedTransformer f3 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Consumer Price Index \\(MoM\\)", "United States"}, new int[]{3, 4, 5}, trader);
        RowBasedTransformer f4 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Consumer Price Index \\(MoM\\)", "Germany"}, new int[]{3, 4, 5}, trader);

        RowBasedTransformer f5 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Unemployment Rate", "European Monetary Union"}, new int[]{3, 4, 5}, trader);
        RowBasedTransformer f6 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Unemployment Rate", "United States"}, new int[]{3, 4, 5}, trader);
        RowBasedTransformer f7 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Unemployment Rate", "United Kingdom"}, new int[]{3, 4, 5}, trader);

        RowBasedTransformer f8 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Unemployment Change", "United Kingdom"}, new int[]{3, 4, 5}, trader);
        RowBasedTransformer f9 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Unemployment Change", "United States"}, new int[]{3, 4, 5}, trader);
        RowBasedTransformer f10 = new RowBasedTransformer(feedCalendar.getCopy(), 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Unemployment Change", "Germany"}, new int[]{3, 4, 5}, trader);



        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        CSVFeed feedPriceEUR = new CSVFeed(path + "feeds\\EURUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);
        CSVFeed feedPriceGBP = new CSVFeed(path + "feeds\\GBPUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);
        CSVFeed feedPriceCHF = new CSVFeed(path + "feeds\\USDCHF_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);
        CSVFeed feedPriceAUD = new CSVFeed(path + "feeds\\AUDUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);

        
        SynchronisedFeed feed = buildSynchFeed(feedPriceEUR, feedPriceGBP, feedPriceAUD, feedPriceCHF);
        feed = new SynchronisedFeed(f1, feed);
        feed = new SynchronisedFeed(f2, feed);
        feed = new SynchronisedFeed(f3, feed);
        feed = new SynchronisedFeed(f4, feed);
        /*feed = new SynchronisedFeed(f5, feed);
        feed = new SynchronisedFeed(f6, feed);
        feed = new SynchronisedFeed(f7, feed);
        feed = new SynchronisedFeed(f8, feed);
        feed = new SynchronisedFeed(f9, feed);
        feed = new SynchronisedFeed(f10, feed);*/
        feed.init();

        SmartDiscretiserOnSynchronisedFeed sFeed = new SmartDiscretiserOnSynchronisedFeed(feed, 5000, 5);
        TimeVariablesAppenderFeed tFeed = new TimeVariablesAppenderFeed(sFeed);

        feed = new SynchronisedFeed(feedPriceEUR.getCopy(), null);
        feed = new SynchronisedFeed(tFeed, feed);
        learnerFeed = new LearnerFeedFromSynchronisedFeed(feed);

        trader.setActionResolution(0.0001);
        trader.setTrainingLearnerFeed(learnerFeed);
        trader.setMaxPopulation(2500);
        trader.setTimeShift(6* 60 * 60 * 1000L);
        trader.setTolerance(5);

        int i = 0;
        while (true)
        {
            DataObject data = learnerFeed.readNext();
            List list = new ArrayList();
            DataSetUtils.add(data.getValue(), list);
            DataSetUtils.add(data.getSignal(), list);
            String toPrint = "";
            for(Object o : list)
            {
                toPrint += o + " ";
            }
            System.out.println("[" + i + "] " + new Date(data.getTimeStamp()) + " " + toPrint);
            i++;

            if(i  == 10000)
            {
                break;
            }
        }
    }

    @Test
    public void trade()
    {
        //new Thread(trader).start();
        trader.run();
    }

    
    private SynchronisedFeed buildSynchFeed(CSVFeed ... feeds)
    {
        SynchronisedFeed synch = null;
        for(CSVFeed feed : feeds){
            ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed.getCopy(), 1);
            ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed.getCopy(), 2);
            ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed.getCopy(), 3);
            ExtractOneFromListFeed feedO = new ExtractOneFromListFeed(feed.getCopy(), 0);
            ExtractOneFromListFeed feedV = new ExtractOneFromListFeed(feed.getCopy(), 4);

            FXHLDiffFeed feedDiff = new FXHLDiffFeed(feed.getCopy());

            StandardDeviationTransformer stdFeedH = new StandardDeviationTransformer(5, 2, feedH.getCopy());
            StandardDeviationTransformer stdFeedL = new StandardDeviationTransformer(5, 2, feedL.getCopy());
            StandardDeviationTransformer stdFeedC = new StandardDeviationTransformer(5, 2, feedC.getCopy());
            StandardDeviationTransformer stdFeedO = new StandardDeviationTransformer(5, 2, feedO.getCopy());
            StandardDeviationTransformer stdFeedV = new StandardDeviationTransformer(5, 2, feedV.getCopy());

            AmplitudeWavelengthTransformer awFeedH = new AmplitudeWavelengthTransformer(feedH.getCopy(), stdFeedH.getCopy(), 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL = new AmplitudeWavelengthTransformer(feedH.getCopy(), stdFeedL.getCopy(), 2, 0.5);
            AmplitudeWavelengthTransformer awFeedC = new AmplitudeWavelengthTransformer(feedH.getCopy(), stdFeedC.getCopy(), 2, 0.5);
            AmplitudeWavelengthTransformer awFeedO = new AmplitudeWavelengthTransformer(feedH.getCopy(), stdFeedO.getCopy(), 2, 0.5);
            AmplitudeWavelengthTransformer awFeedV = new AmplitudeWavelengthTransformer(feedH.getCopy(), stdFeedV.getCopy(), 2, 0.5);

            synch = new SynchronisedFeed(feedH, synch);
            synch = new SynchronisedFeed(feedL, synch);
            synch = new SynchronisedFeed(feedC, synch);
            synch = new SynchronisedFeed(feedO, synch);
            synch = new SynchronisedFeed(feedV, synch);

            synch = new SynchronisedFeed(feedDiff, synch);

            synch = new SynchronisedFeed(stdFeedH, synch);
            synch = new SynchronisedFeed(stdFeedL, synch);
            synch = new SynchronisedFeed(stdFeedC, synch);
            synch = new SynchronisedFeed(stdFeedO, synch);
            synch = new SynchronisedFeed(stdFeedV, synch);

            synch = new SynchronisedFeed(awFeedH, synch);
            synch = new SynchronisedFeed(awFeedL, synch);
            synch = new SynchronisedFeed(awFeedC, synch);
            synch = new SynchronisedFeed(awFeedO, synch);
            synch = new SynchronisedFeed(awFeedV, synch);
        }
        return synch;
    }
}

class FXHLDiffFeed extends AbstractSurgicalFeed{

    private Feed rawFeed;
    public FXHLDiffFeed(Feed rawFeed) {
        super(rawFeed);
        this.rawFeed = rawFeed;
    }

    @Override
    protected FeedObject operate(long time, List row) {
        return new FeedObject(time, ((Double)row.get(1) - (Double)row.get(2)));
    }

    @Override
    public Feed getCopy() {
        return new FXHLDiffFeed(rawFeed.getCopy());
    }
}
