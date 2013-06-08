package ai.context;

import ai.context.feed.FeedObject;
import ai.context.feed.fx.DukascopyFeed;
import ai.context.feed.stitchable.StitchableFXRate;
import ai.context.trading.DukascopyConnection;
import com.dukascopy.api.Period;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestStitchDukascopy {

    @Test
    public void testStitch(){
        StitchableFXRate feed = null;
        try {
            feed = new StitchableFXRate("src/test/resources/TestRate.csv", new DukascopyFeed(new DukascopyConnection("DEMO2xpBDn", "xpBDn").getClient(), Period.TEN_SECS));
        } catch (Exception e) {
            e.printStackTrace();
        }
        for(int i = 0; i < 1000; i++)
        {
            FeedObject data = feed.readNext(this);
            Object[] array = (Object[]) data.getData();
            List<Object> list = new ArrayList<Object>();
            for(Object o : array)
            {
                list.add(o);
            }
            System.out.println(new Date(data.getTimeStamp()) + " " + list);
        }

    }
}
