package ai.context.trading;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.io.Channel;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MockFXFeed implements Feed {

    private Channel<FeedObject> channel = new Channel(10000);
    private long timeStamp;

    private long interval = 1000;

    public MockFXFeed(final long interval) {
        this.interval = interval;

        TimerTask updater = new TimerTask() {
            @Override
            public void run() {
                timeStamp = System.currentTimeMillis() - interval;
                Double[] data = new Double[] {0D, 0D, 0D, 0D, 0D};

                channel.put(new FeedObject(timeStamp, data));
            }
        };

        Timer timer = new Timer();
        timer.schedule(updater, interval, interval);
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public FeedObject readNext(Object caller) {
        return channel.get();
    }

    @Override
    public Feed getCopy() {
        return null;
    }

    @Override
    public void addChild(Feed feed) {
    }

    @Override
    public void removeChild(Feed feed) {

    }

    @Override
    public long getLatestTime() {
        return timeStamp;
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
