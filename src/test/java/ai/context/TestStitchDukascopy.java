package ai.context;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.fx.DukascopyFeed;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.stitchable.StitchableFXRate;
import ai.context.trading.DukascopyConnection;
import ai.context.util.configuration.PropertiesHolder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestStitchDukascopy {

    @Test
    public void testStitch() {
        StitchableFXRate feed = null;
        try {
            feed = new StitchableFXRate("src/test/resources/TestRate.csv", new DukascopyFeed(new DukascopyConnection(PropertiesHolder.dukascopyLogin, PropertiesHolder.dukascopyPass).getClient(), Period.TEN_SECS, Instrument.EURUSD, "src/test/resources/TestRateHist.csv"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        CSVFeed feedPriceEUR = new CSVFeed("src/test/resources/TestRateHist.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, null);
        feedPriceEUR.setSkipWeekends(false);
        feedPriceEUR.setStitchableFeed(feed);
        for (int i = 0; i < 2000; i++) {
            FeedObject data = feedPriceEUR.readNext(this);
            Object[] array = (Object[]) data.getData();
            List<Object> list = new ArrayList<Object>();
            for (Object o : array) {
                list.add(o);
            }
            System.out.println(new Date(data.getTimeStamp()) + " " + list);
        }

    }
}
