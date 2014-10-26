package ai.context.feed.fx;

import ai.context.trading.DukascopyConnection;
import ai.context.util.configuration.PropertiesHolder;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TickDataFiller implements IStrategy{

    private String file;
    private Instrument instrument;
    private IClient client;

    private long id;

    public TickDataFiller(String path, IClient client) {

        this.file = path;
        if(!path.endsWith(".csv")){
            file += "/" + PropertiesHolder.ticksFile;
        }
        this.instrument = Instrument.valueOf(PropertiesHolder.mainAsset);
        this.client = client;

        id = client.startStrategy(this);
    }

    @Override
    public void onStart(IContext context) throws JFException {

        IHistory history = context.getHistory();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = null;
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                line = sCurrentLine;
            }
            br.close();

            String parts[] = line.split(",");
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            long start = format.parse(parts[0]).getTime();

            System.out.println("Starting to pad from " + parts[0] + "...");

            List<ITick> tickList = history.getTicks(instrument, start + 1, history.getLastTick(instrument).getTime());

            System.out.println("Got " + tickList.size() + " ticks for padding...");

            FileWriter fileWriter = new FileWriter(file,true);
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
            //Time,Ask,Bid,AskVolume,BidVolume
            //2006.01.02 00:00:05.378,1.18444,1.18428,39.1,26.1

            for(ITick tick : tickList){
                String lineOut = format.format(new Date(tick.getTime()));
                lineOut += "," + tick.getAsk();
                lineOut += "," + tick.getBid();
                lineOut += "," + tick.getAskVolume();
                lineOut += "," + tick.getBidVolume() + "\n";
                bufferWriter.write(lineOut);
            }
            bufferWriter.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        client.stopStrategy(id);
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
        System.out.println("Tick data padded...");
    }

    public static void main(String[] args){
        String path = "/opt/dev/data/feeds";
        if(args.length == 1){
            path = args[0];
        }
        try {
            IClient client = new DukascopyConnection(PropertiesHolder.dukascopyLogin, PropertiesHolder.dukascopyPass).getClient();
            new TickDataFiller(path, client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
