package ai.context.feed.fx;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.io.Channel;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DukascopyFeed implements IStrategy, Feed {

    private static final Logger LOGGER = LoggerFactory.getLogger(DukascopyFeed.class);

    private IClient client;
    private Channel<FeedObject> channel = new Channel(10000);
    private Period interval;
    private long timeStamp;

    private Instrument instrument;

    public DukascopyFeed(IClient client, Period interval, Instrument instrument) {
        this.client = client;
        this.interval = interval;
        this.instrument = instrument;
        client.startStrategy(this);
    }

    @Override
    public void onStart(IContext context) throws JFException {
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (period == interval && instrument == this.instrument) {
            timeStamp = askBar.getTime();
            Double[] data = new Double[]{bidBar.getOpen(), bidBar.getHigh(), bidBar.getLow(), bidBar.getClose(), bidBar.getVolume()};

            LOGGER.info("ADDED candlestick: " + new Date(timeStamp) + " at " + new Date());
            channel.put(new FeedObject(timeStamp, data));
        }
    }

    @Override
    public void onMessage(IMessage message) throws JFException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onStop() throws JFException {
        //To change body of implemented methods use File | Settings | File Templates.
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
        return padding + "[" + startIndex + "] Dukascopy feed for OHLC for Instrument: " + instrument + " and Period: " + interval;
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Duckascopy Client Connection",
                "Tick Period",
                "Instrument"
        };
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
