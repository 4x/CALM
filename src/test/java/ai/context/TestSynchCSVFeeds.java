package ai.context;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.feed.transformer.filtered.RowBasedTransformer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestSynchCSVFeeds {

    DataType[] typesCalendar = new DataType[]{
            DataType.OTHER,
            DataType.OTHER,
            DataType.INTEGER,
            DataType.EXTRACTABLE_DOUBLE,
            DataType.EXTRACTABLE_DOUBLE,
            DataType.EXTRACTABLE_DOUBLE};

    CSVFeed feedCalendar = new CSVFeed("C:\\Users\\Oblene\\Desktop\\Sandbox\\Data\\feeds\\Calendar_2008.csv", "yyyyMMdd HH:mm:ss", typesCalendar);
    CSVFeed feedCalendar1 = new CSVFeed("C:\\Users\\Oblene\\Desktop\\Sandbox\\Data\\feeds\\Calendar_2008.csv", "yyyyMMdd HH:mm:ss", typesCalendar);
    RowBasedTransformer filtered = new RowBasedTransformer(feedCalendar1, 10, new int[]{0}, new String[]{"Nonfarm Payrolls"}, new int[]{3, 4, 5}, null);

    DataType[] typesPrice = new DataType[]{
            DataType.DOUBLE,
            DataType.DOUBLE,
            DataType.DOUBLE,
            DataType.DOUBLE,
            DataType.DOUBLE};

    CSVFeed feedPriceEUR = new CSVFeed("C:\\Users\\Oblene\\Desktop\\Sandbox\\Data\\feeds\\EURUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);
    CSVFeed feedPriceGBP = new CSVFeed("C:\\Users\\Oblene\\Desktop\\Sandbox\\Data\\feeds\\GBPUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice);

    @Test
    public void testCSV()
    {
        SynchronisedFeed sFeed = new SynchronisedFeed(feedCalendar, null);
        sFeed = new SynchronisedFeed(feedPriceEUR, sFeed);
        sFeed = new SynchronisedFeed(feedPriceGBP, sFeed);
        sFeed = new SynchronisedFeed(filtered, sFeed);

        sFeed.init();

        for(int i = 0; i < 4000; i++)
        {
            FeedObject data = sFeed.getNextComposite(this);

            List<Object> list = new ArrayList<Object>();

            System.out.println(i + " " + new Date(data.getTimeStamp()) + " " + data.getData());
        }
    }

}
