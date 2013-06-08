package ai.context.feed.fx;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.io.Channel;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;

public class DukascopyFeed implements IStrategy, Feed {

    private IClient client;
    private Channel<FeedObject> channel = new Channel(100);
    private Period interval;

    public DukascopyFeed(IClient client, Period interval){
        this.client = client;
        this.interval = interval;
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
        if(period == interval){
            long timeStamp = askBar.getTime();
            Double[] data = new Double[] {bidBar.getOpen(), bidBar.getHigh(), bidBar.getLow(), bidBar.getClose(), bidBar.getVolume()};

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
}
