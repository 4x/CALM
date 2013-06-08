package ai.context;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.stitchable.StitchableFeed;
import ai.context.util.DataSetUtils;
import org.junit.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestStitch {

    @Test
    public void testStitch(){

        CSVFeed feed = new CSVFeed("src/test/resources/Test.csv", "yyyyMMdd HH:mm:ss", new DataType[]{DataType.LONG});
        final TestStitcher stitcher = new TestStitcher("src/test/resources/TestLive.csv", new TestFeed3());

        Runnable liveFeed = new Runnable() {
            @Override
            public void run() {
                stitcher.startPadding();
            }
        };
        new Thread(liveFeed).start();

        feed.setStitchableFeed(stitcher);

        int waiting = 150;
        for(int i = 0; i < 400; i++){
            try {
                Thread.sleep(waiting);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            FeedObject data = feed.readNext(this);
            if(feed.isStitching()){
                waiting = 50;
            }
            List list = new ArrayList<>();
            DataSetUtils.add(data.getData(), list);

            System.out.println(new Date(data.getTimeStamp()) + " " + list);
        }
    }
}

class TestFeed3 implements Feed{

    long i = 0;
    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public FeedObject readNext(Object caller) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        i++;
        return new FeedObject(i* 10000, i);
    }

    @Override
    public Feed getCopy() {
        return null;
    }

    @Override
    public void addChild(Feed feed) {
    }
}

class TestStitcher extends StitchableFeed{

    private SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

    public TestStitcher(String liveFileName, Feed liveFeed) {
        super(liveFileName, liveFeed);
    }

    @Override
    protected FeedObject formatForCSVFeed(FeedObject data) {

        String toWrite = format.format(new Date(data.getTimeStamp())) + "," + data.getData() + "\n";
        try {
            writer.write(toWrite);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
