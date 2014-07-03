package ai.context;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.row.FXStreetCalendarRSSFeed;
import ai.context.feed.stitchable.StitchableFXStreetCalendarRSS;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestRSS {

    @Test
    public void testRSS() {

        FXStreetCalendarRSSFeed feed = new FXStreetCalendarRSSFeed();

        for (int i = 0; i < 1000; i++) {
            FeedObject data = feed.readNext(this);
            Object[] array = (Object[]) data.getData();
            List<Object> list = new ArrayList<Object>();
            for (Object o : array) {
                list.add(o);
            }
            System.out.println(new Date(data.getTimeStamp()) + " " + list);
        }
        feed.close();
    }

    @Test
    public void testSplit() {
        String data = "SG: Gross Domestic Product (QoQ): 1.800 % Thu May 23 01:00:00 BST 2013 <table cellpadding=\"5\"><tr><td><strong>Date (GMT)</strong></td><td><strong>Event</strong></td><td><strong>Cons.</strong></td><td><strong>Actual</strong></td><td><strong>Previous</strong></td></tr><tr><td>May 23 00:00</td><td>Gross Domestic Product (QoQ)</td><td></td><td>1.800  %</td><td>3.300  %</td></tr></table><br /><div class=\"feedflare\">\n" +
                "<a href=\"http://feeds.fxstreet.com/~ff/fundamental/economic-calendar?a=ZgRwkOalyss:9wefCOA6NlY:yIl2AUoC8zA\"><img src=\"http://feeds.feedburner.com/~ff/fundamental/economic-calendar?d=yIl2AUoC8zA\" border=\"0\"></img></a> <a href=\"http://feeds.fxstreet.com/~ff/fundamental/economic-calendar?a=ZgRwkOalyss:9wefCOA6NlY:qj6IDK7rITs\"><img src=\"http://feeds.feedburner.com/~ff/fundamental/economic-calendar?d=qj6IDK7rITs\" border=\"0\"></img></a> <a href=\"http://feeds.fxstreet.com/~ff/fundamental/economic-calendar?a=ZgRwkOalyss:9wefCOA6NlY:F7zBnMyn0Lo\"><img src=\"http://feeds.feedburner.com/~ff/fundamental/economic-calendar?i=ZgRwkOalyss:9wefCOA6NlY:F7zBnMyn0Lo\" border=\"0\"></img></a>\n" +
                "</div>\n";

        for (String part : data.split("<td>")) {
            System.out.println(part);
        }

    }

    @Test
    public void testRSSInStitch() {
        StitchableFXStreetCalendarRSS liveFeed = new StitchableFXStreetCalendarRSS("src/test/resources/TestRSS.csv", new FXStreetCalendarRSSFeed());

        DataType[] types = new DataType[]{
                DataType.OTHER,
                DataType.OTHER,
                DataType.INTEGER,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE};

        CSVFeed feed = new CSVFeed("src/test/resources/TestCalendar.csv", "yyyyMMdd HH:mm:ss", types, null);
        feed.setStitchableFeed(liveFeed);

        for (int i = 0; i < 1000; i++) {
            FeedObject data = feed.readNext(this);
            Object[] array = (Object[]) data.getData();
            List<Object> list = new ArrayList<Object>();
            for (Object o : array) {
                list.add(o);
            }
            System.out.println(new Date(data.getTimeStamp()) + " " + list);
        }
    }
}
