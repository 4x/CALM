package ai.context;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.manipulation.FeedWrapper;
import ai.context.feed.manipulation.Manipulator;
import ai.context.feed.manipulation.TimeDecaySingleSentimentManipulator;
import ai.context.feed.row.CSVFeed;
import ai.context.util.analysis.LookAheadScheduler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestWrapperManipulator {
    @Test
    public void testCSV() {
        DataType[] types = new DataType[]{
                DataType.OTHER,
                DataType.OTHER,
                DataType.INTEGER,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE};

        CSVFeed feed = new CSVFeed("/opt/dev/data/feeds/Calendar.csv", "yyyyMMdd HH:mm:ss", types, null);
        CSVFeed scheduleFeed = new CSVFeed("/opt/dev/data/feeds/Calendar.csv", "yyyyMMdd HH:mm:ss", types, null);
        LookAheadScheduler scheduler = new LookAheadScheduler(scheduleFeed, 0, 1);


        for (int i = 0; i < 100; i++) {
            FeedObject data = feed.readNext(this);
            Object[] array = (Object[]) data.getData();
            List<Object> list = new ArrayList<Object>();
            for (Object o : array) {
                list.add(o);
            }
            System.out.println(data.getTimeStamp() + " " + list);
        }

        FeedWrapper wrapper = new FeedWrapper(feed);
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

        wrapper.putManipulator("1", manipulator1);
        wrapper.putManipulator("2", manipulator2);
        wrapper.putManipulator("3", manipulator3);
        wrapper.putManipulator("4", manipulator4);
        wrapper.putManipulator("5", manipulator5);
        wrapper.putManipulator("6", manipulator6);
        wrapper.putManipulator("7", manipulator7);
        wrapper.putManipulator("8", manipulator8);
        wrapper.putManipulator("9", manipulator9);
        wrapper.putManipulator("10", manipulator10);
        wrapper.putManipulator("11", manipulator11);
        wrapper.putManipulator("12", manipulator12);
        wrapper.putManipulator("13", manipulator13);
        wrapper.putManipulator("14", manipulator14);
        wrapper.putManipulator("15", manipulator15);
        wrapper.putManipulator("16", manipulator16);

        for(long t = 1199262600000L; t < 1199264100000L + 10 * 86400000L; t += 1800000L){
            /*for(int i = 0; i < 16; i++){
                System.out.println(new Date(t) + " " + (1 + i) + " " + wrapper.getAtTimeForManipulator(t, "" + (1 + i)));
            }*/
            System.out.println(new Date(t) + " " + wrapper.getAtTimeForManipulator(t, "16"));
        }

    }
}
