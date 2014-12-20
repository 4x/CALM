package ai.context.feed.fx;

import ai.context.trading.DukascopyConnection;
import ai.context.util.configuration.PropertiesHolder;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TickDataGetter implements IStrategy {

    private Instrument instrument;
    private long start;
    private String file;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
    private long thirtyDays = 30 * 86400000L;

    public TickDataGetter(String path, IClient client, String asset, String startDate) throws ParseException {

        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        start = format.parse(startDate).getTime();
        instrument = Instrument.valueOf(asset);
        file = path;

        client.startStrategy(this);
    }

    @Override
    public void onStart(IContext context) throws JFException {
        IHistory history = context.getHistory();
        long end = history.getLastTick(instrument).getTime();
        long currentTime = start;

        try {
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
            //Time,Ask,Bid,AskVolume,BidVolume
            //2006.01.02 00:00:05.378,1.18444,1.18428,39.1,26.1

            while (currentTime < end) {
                long stepEnd = Math.min(currentTime + thirtyDays, end);
                System.out.println("Trying to add batch: (" + format.format(new Date(currentTime + 1)) + " -> " + format.format(new Date(stepEnd)));

                List<ITick> tickList = history.getTicks(instrument, currentTime + 1, stepEnd);
                System.out.println("Got " + tickList.size() + " ticks for padding... (" + format.format(new Date(currentTime)) + " -> " + format.format(new Date(stepEnd)));

                for (ITick tick : tickList) {
                    String lineOut = format.format(new Date(tick.getTime()));
                    lineOut += "," + tick.getAsk();
                    lineOut += "," + tick.getBid();
                    lineOut += "," + tick.getAskVolume();
                    lineOut += "," + tick.getBidVolume() + "\n";
                    bufferWriter.write(lineOut);
                }

                currentTime = stepEnd;
            }
            bufferWriter.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {

    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

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

    public static void main(String[] args){
        try {
            IClient client = new DukascopyConnection(PropertiesHolder.dukascopyLogin, PropertiesHolder.dukascopyPass).getClient();
            new TickDataGetter(args[0], client, args[1], args[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
