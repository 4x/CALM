package ai.context;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.transformer.compound.AmplitudeWavelengthTransformer;
import ai.context.feed.transformer.series.learning.MATransformer;
import ai.context.feed.transformer.series.learning.RSITransformer;
import ai.context.feed.transformer.series.learning.StandardDeviationTransformer;
import ai.context.feed.transformer.series.learning.VarianceTransformer;
import ai.context.feed.transformer.single.unpadded.LinearDiscretiser;
import ai.context.feed.transformer.single.unpadded.LogarithmicDiscretiser;
import com.tictactec.ta.lib.MAType;
import org.junit.Test;

public class TestTransformer {

    @Test
    public void testGoLive()
    {
        TestFeed1 primary = new TestFeed1();
        MATransformer transformer = new MATransformer(MAType.Sma, 10, primary);
        primary.addChild(transformer);

        long t = System.currentTimeMillis();
        for (int i = 0; i < 145; i++)
        {
            FeedObject data = transformer.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData() + " " + primary.getLatestTime());
        }
        System.err.println(System.currentTimeMillis() - t);

        transformer.goLive();
        t = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
        {
            FeedObject data = transformer.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData() + " " + primary.getLatestTime());
        }
        System.err.println(System.currentTimeMillis() - t);
    }

    @Test
    public void testVariance()
    {
        VarianceTransformer transformer = new VarianceTransformer(10, 1, new TestFeed1());

        for (int i = 0; i < 100; i++)
        {
            FeedObject data = transformer.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData());
        }
    }

    @Test
    public void testRSI()
    {
        RSITransformer transformer = new RSITransformer(20, 5, 20, MAType.Sma, new TestFeed1());

        for (int i = 0; i < 100; i++)
        {
            FeedObject data = transformer.readNext(this);
            Object[] value = (Object[]) data.getData();
            if(value == null)
            {
                value = new Object[]{null, null};
            }
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + value[0] + " " + value[1]);
        }
    }

    @Test
    public void testLinearDiscretiserOnRSI()
    {
        RSITransformer transformer = new RSITransformer(20, 5, 20, MAType.Sma, new TestFeed1());
        LinearDiscretiser discretiser = new LinearDiscretiser(10, 50, transformer, 0);

        for (int i = 0; i < 100; i++)
        {
            FeedObject data = discretiser.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData());
        }
    }

    @Test
    public void testLogarithmicDiscretiserOnRSI()
    {
        RSITransformer transformer = new RSITransformer(20, 5, 20, MAType.Sma, new TestFeed1());
        LogarithmicDiscretiser discretiser = new LogarithmicDiscretiser(2, 50, transformer, 0);

        for (int i = 0; i < 100; i++)
        {
            FeedObject data = discretiser.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData());
        }
    }

    @Test
    public void testAmplitudeWavelengthTransformer()
    {
        StandardDeviationTransformer transformer = new StandardDeviationTransformer(10, 1, new TestFeed1());
        AmplitudeWavelengthTransformer awT = new AmplitudeWavelengthTransformer(new TestFeed1(), transformer, 0.1, 0.5);

        for (int i = 0; i < 1000; i++)
        {
            FeedObject data = awT.readNext(this);
            if(data.getData() != null)
            {
                double[] value = (double[]) data.getData();
                System.out.println("[" + i + "] " + data.getTimeStamp() + " " + value[0] + " " + value[1] + " " + value[2] + " " + value[3]);
            }
        }
    }
}

class TestFeed1 implements Feed{

    long t = 0;

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public FeedObject readNext(Object caller) {
        t++;
        System.out.println("From primary source: " + t);
        return new FeedObject(t, (double) t % 20);
    }

    @Override
    public Feed getCopy() {
        return new TestFeed1();
    }

    @Override
    public void addChild(Feed feed) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getLatestTime() {
        return t;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
