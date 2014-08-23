package ai.context.feed.fx;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.io.Channel;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class DukascopyFeed implements IStrategy, Feed {

    private static final Logger LOGGER = LoggerFactory.getLogger(DukascopyFeed.class);

    private IClient client;
    private Channel<FeedObject> channel = new Channel(10000);
    private Period interval;
    private long timeStamp;
    private String flatFile;

    private Instrument instrument;

    public DukascopyFeed(IClient client, Period interval, Instrument instrument, String flatFile) {
        this.client = client;
        this.interval = interval;
        this.instrument = instrument;
        this.flatFile = flatFile;
        client.startStrategy(this);
    }

    @Override
    public void onStart(IContext context) throws JFException {
        if(flatFile != null){
            IHistory history = context.getHistory();
            try {
                BufferedReader br = new BufferedReader(new FileReader(flatFile));
                String line = null;
                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    line = sCurrentLine;
                }

                String parts[] = line.split(",");
                SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                format.setTimeZone(TimeZone.getTimeZone("GMT"));

                long start = format.parse(parts[0]).getTime() + interval.getInterval();
                br.close();

                //long prevBarTime = history.getPreviousBarStart(interval, history.getLastTick(instrument).getTime());
                long prevBarTime = (System.currentTimeMillis() / interval.getInterval() - 1) * interval.getInterval();

                List<IBar> bars = new ArrayList<>();
                while(true) {
                    try {
                        System.out.println("Trying to synch " + flatFile + " from " + new Date(start) + " to " + new Date(prevBarTime));
                        bars = history.getBars(instrument, interval, OfferSide.BID, start, prevBarTime);
                        break;
                    } catch (Exception e){
                        prevBarTime = prevBarTime - interval.getInterval();
                        if(prevBarTime <= start){
                            System.err.println("Error: Aborting synching of flatFile");
                            break;
                        }
                        System.err.println("Error: " + e.getMessage() + " -> Moving endTime to " + new Date(prevBarTime));
                    }
                }

                FileWriter fileWritter = new FileWriter(flatFile,true);
                BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                for(IBar bar : bars){
                    String timeStamp = format.format(new Date(bar.getTime()));
                    double open = bar.getOpen();
                    double high = bar.getHigh();
                    double low = bar.getLow();
                    double close = bar.getClose();
                    double volume = bar.getVolume();

                    bufferWritter.write(timeStamp + "," + open + "," + high + "," + low + "," + close + "," + volume + "\n");
                }
                bufferWritter.close();
                System.out.println("Finished synching " + flatFile);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        //System.out.println(instrument + " " + tick);
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
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
    }

    @Override
    public void onStop() throws JFException {
    }

    @Override
    public boolean hasNext() {
        return channel.size() > 0;
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
