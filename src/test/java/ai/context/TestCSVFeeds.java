package ai.context;

import ai.context.feed.row.CSVFeed;
import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.feed.transformer.filtered.RowBasedTransformer;
import ai.context.learning.Learner;
import ai.context.util.DataSetUtils;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestCSVFeeds {

    @Test
    public void testCSV() {
        DataType[] types = new DataType[]{
                DataType.OTHER,
                DataType.OTHER,
                DataType.INTEGER,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE};

        CSVFeed feed = new CSVFeed("C:\\Users\\Oblene\\Desktop\\Sandbox\\Data\\feeds\\Calendar_2008.csv", "yyyyMMdd HH:mm:ss", types, null);

        for (int i = 0; i < 100; i++) {
            FeedObject data = feed.readNext(this);
            Object[] array = (Object[]) data.getData();
            List<Object> list = new ArrayList<Object>();
            for (Object o : array) {
                list.add(o);
            }
            System.out.println(data.getTimeStamp() + " " + list);
        }


        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        Learner container = new Learner("tmp");
        RowBasedTransformer filtered = new RowBasedTransformer(feed.getCopy(), 60 * 60 * 1000L, new int[]{0}, new String[]{"Nonfarm Payrolls"}, new int[]{3, 4, 5}, container);
        feed.addChild(filtered);

        feed = new CSVFeed("C:\\Users\\Oblene\\Desktop\\Sandbox\\Data\\feeds\\EURUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, null);
        SynchronisedFeed sFeed = new SynchronisedFeed(feed, null);
        sFeed = new SynchronisedFeed(filtered, sFeed);

        for (int i = 0; i < 4000; i++) {
            FeedObject data = sFeed.getNextComposite(this);
            container.setCurrentTime(data.getTimeStamp());
            List<Object> list = new ArrayList<Object>();
            DataSetUtils.add(data.getData(), list);

            System.out.println(new Date(data.getTimeStamp()) + " " + list);
        }
    }

    @Test
    public void testTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        long tNow = System.currentTimeMillis();
        Date now = new Date(tNow);
        String str = format.format(now);
        System.out.println(tNow + " " + str);
        try {
            System.out.println(str + " " + format.parse(str).getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
