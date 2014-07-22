package ai.context;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.surgical.ExtractOneFromListFeed;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.feed.transformer.compound.AbsoluteAmplitudeWavelengthTransformer;
import ai.context.feed.transformer.compound.AmplitudeWavelengthTransformer;
import ai.context.feed.transformer.series.learning.MATransformer;
import ai.context.feed.transformer.series.learning.RSITransformer;
import ai.context.feed.transformer.series.learning.StandardDeviationTransformer;
import ai.context.feed.transformer.series.learning.VarianceTransformer;
import ai.context.feed.transformer.series.online.GradientOnlineTransformer;
import ai.context.feed.transformer.series.online.MinMaxDistanceTransformer;
import ai.context.feed.transformer.series.online.RSIOnlineTransformer;
import ai.context.feed.transformer.series.online.RadarOnlineTransformer;
import ai.context.feed.transformer.single.unpadded.LinearDiscretiser;
import ai.context.feed.transformer.single.unpadded.LogarithmicDiscretiser;
import ai.context.util.DataSetUtils;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.learning.AmalgamateUtils;
import com.tictactec.ta.lib.MAType;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TestTransformer {

    @Test
    public void testGoLive() {
        TestFeed1 primary = new TestFeed1();
        MATransformer transformer = new MATransformer(MAType.Sma, 10, primary);
        primary.addChild(transformer);

        long t = System.currentTimeMillis();
        for (int i = 0; i < 145; i++) {
            FeedObject data = transformer.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData() + " " + primary.getLatestTime());
        }
        System.err.println(System.currentTimeMillis() - t);

        transformer.goLive();
        t = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            FeedObject data = transformer.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData() + " " + primary.getLatestTime());
        }
        System.err.println(System.currentTimeMillis() - t);
    }

    @Test
    public void testVariance() {
        VarianceTransformer transformer = new VarianceTransformer(10, 1, new TestFeed1());

        for (int i = 0; i < 100; i++) {
            FeedObject data = transformer.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData());
        }
    }

    @Test
    public void testRSI() {
        RSITransformer transformer = new RSITransformer(20, 5, 20, MAType.Sma, new TestFeed1());

        for (int i = 0; i < 100; i++) {
            FeedObject data = transformer.readNext(this);
            Object[] value = (Object[]) data.getData();
            if (value == null) {
                value = new Object[]{null, null};
            }
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + value[0] + " " + value[1]);
        }
    }

    @Test
    public void testLinearDiscretiserOnRSI() {
        RSITransformer transformer = new RSITransformer(20, 5, 20, MAType.Sma, new TestFeed1());
        LinearDiscretiser discretiser = new LinearDiscretiser(10, 50, transformer, 0);

        for (int i = 0; i < 100; i++) {
            FeedObject data = discretiser.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData());
        }
    }

    @Test
    public void testLogarithmicDiscretiserOnRSI() {
        RSITransformer transformer = new RSITransformer(20, 5, 20, MAType.Sma, new TestFeed1());
        LogarithmicDiscretiser discretiser = new LogarithmicDiscretiser(2, 50, transformer, 0);

        for (int i = 0; i < 100; i++) {
            FeedObject data = discretiser.readNext(this);
            System.out.println("[" + i + "] " + data.getTimeStamp() + " " + data.getData());
        }
    }

    @Test
    public void testAmplitudeWavelengthTransformer() {
        StandardDeviationTransformer transformer = new StandardDeviationTransformer(10, 1, new TestFeed1());
        AmplitudeWavelengthTransformer awT = new AmplitudeWavelengthTransformer(new TestFeed1(), transformer, 0.1, 0.5, 0.1);

        for (int i = 0; i < 1000; i++) {
            FeedObject data = awT.readNext(this);
            if (data.getData() != null) {
                double[] value = (double[]) data.getData();
                System.out.println("[" + i + "] " + data.getTimeStamp() + " " + value[0] + " " + value[1] + " " + value[2] + " " + value[3]);
            }
        }
    }

    @Test
    public void testRadar() {
        TestFeed3Outputs series = new TestFeed3Outputs();
        Feed close = new ExtractOneFromListFeed(series, 0);
        Feed high = new ExtractOneFromListFeed(series, 1);
        Feed low = new ExtractOneFromListFeed(series, 2);

        series.addChild(close);
        series.addChild(high);
        series.addChild(low);

        RadarOnlineTransformer transformer = new RadarOnlineTransformer(100, low, high, close, 1);
        SynchronisedFeed feed = new SynchronisedFeed(close, null);
        feed = new SynchronisedFeed(low, feed);
        feed = new SynchronisedFeed(high, feed);
        feed = new SynchronisedFeed(transformer, feed);

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("RADAR.csv"));
            for (int i = 0; i < 600; i++) {
                FeedObject data = feed.getNextComposite(this);
                appendToFile(AmalgamateUtils.getCSVString(data.getData()), out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGradient(){
        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = PropertiesHolder.startDateTime;

        CSVFeed feed = new CSVFeed("/opt/dev/data/feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feed.setSkipWeekends(true);
        ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
        ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
        ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);

        GradientOnlineTransformer transformer = new GradientOnlineTransformer(50, feedL, feedH, feedC, 0.0001);

        for (int i = 0; i < 2000; i++) {
            FeedObject data = transformer.readNext(this);
            System.out.println(data);
        }
    }

    @Test
    public void testMinMax(){
        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = PropertiesHolder.startDateTime;

        CSVFeed feed = new CSVFeed("/opt/dev/data/feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feed.setSkipWeekends(true);
        ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
        ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
        ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);

        MinMaxDistanceTransformer transformer = new MinMaxDistanceTransformer(50, feedL, feedH, feedC, 0.0001);

        for (int i = 0; i < 2000; i++) {
            FeedObject data = transformer.readNext(this);
            System.out.println(data);
        }
    }

    @Test
    public void testRSI1(){
        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = PropertiesHolder.startDateTime;

        CSVFeed feed = new CSVFeed("/opt/dev/data/feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feed.setSkipWeekends(true);
        ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
        ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
        ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);

        RSIOnlineTransformer transformer = new RSIOnlineTransformer(feedH, 5, 25, 0.5);

        for (int i = 0; i < 2000; i++) {
            FeedObject data = transformer.readNext(this);
            System.out.println(data);
        }
    }

    @Test
    public void testAbsoluteAmplitudeWavelength(){
        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = PropertiesHolder.startDateTime;

        CSVFeed feed = new CSVFeed("/opt/dev/data/feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feed.setSkipWeekends(true);
        ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
        ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
        ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);

        AbsoluteAmplitudeWavelengthTransformer transformer = new AbsoluteAmplitudeWavelengthTransformer(feedH, 10, 0.125, 0.0001);

        for (int i = 0; i < 2000; i++) {
            FeedObject data = transformer.readNext(this);
            List<Double>  out = new ArrayList<>();
            DataSetUtils.add(data.getData(), out);
            System.out.println(data.getTimeStamp() + " " + out);
        }
    }

    @Test
    public void testRadar1(){
        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = PropertiesHolder.startDateTime;

        CSVFeed feed = new CSVFeed("/opt/dev/data/feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feed.setSkipWeekends(true);
        ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
        ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
        ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);

        RadarOnlineTransformer transformer = new RadarOnlineTransformer(100, feedL, feedH, feedC, 0.0001);

        for (int i = 0; i < 2000; i++) {
            FeedObject data = transformer.readNext(this);
            List<Double>  out = new ArrayList<>();
            DataSetUtils.add(data.getData(), out);
            System.out.println(data.getTimeStamp() + " " + out);
        }
    }


    private void appendToFile(String data, BufferedWriter out) {
        try {
            out.write(data + "\n");
            out.flush();
        } catch (Exception e) {
        }
    }
}

class TestFeed1 implements Feed {

    long t = 0;

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public void removeChild(Feed feed) {

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

    @Override
    public List getElementChain(int element) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getNumberOfOutputs() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

class TestFeed3Outputs implements Feed {

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();
    long t = 0;
    double lastValue = 0;

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public void removeChild(Feed feed) {

    }

    @Override
    public FeedObject readNext(Object caller) {
        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }
        t++;

        double r1 = Math.random();
        double r2 = Math.random();
        double r3 = Math.random();

        List<Double> output = new ArrayList<>();
        FeedObject feedObject = new FeedObject(t, output);

        double multiplier = 2;
        if (t < 400) {
            output.add(lastValue + multiplier * (200 - (t % 200)) * (r1 - 0.5));
            output.add(lastValue + multiplier * (200 - (t % 200)) * (r2 - 0.5));
            output.add(lastValue + multiplier * (200 - (t % 200)) * (r3 - 0.5));
        } else {
            output.add(lastValue + multiplier * (t % 200) * (r1 - 0.5));
            output.add(lastValue + multiplier * (t % 200) * (r2 - 0.5));
            output.add(lastValue + multiplier * (t % 200) * (r3 - 0.5));
        }

        for (Feed listener : buffers.keySet()) {
            if (listener != caller) {
                buffers.get(listener).add(feedObject);
            }
        }
        return feedObject;
    }

    @Override
    public Feed getCopy() {
        return new TestFeed1();
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }

    @Override
    public long getLatestTime() {
        return t;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List getElementChain(int element) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getNumberOfOutputs() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}