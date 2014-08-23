package ai.context.util.history;

import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

public class HistoryBarsSynch implements IStrategy {

    private long processId;
    private IClient client;
    private IHistory history;
    private IConsole console;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public HistoryBarsSynch(IClient client) {
        this.client = client;
        format.setTimeZone(TimeZone.getTimeZone("GMT"));

        processId = client.startStrategy(this);
    }

    @Override
    public void onStart(IContext context) throws JFException {
        history = context.getHistory();
        console = context.getConsole();
        context.setSubscribedInstruments(java.util.Collections.singleton(Instrument.EURUSD), true);

        try {
            getBars(format.parse("2014.08.06 09:30:00").getTime(), format.parse("2014.08.06 19:30:00").getTime(), Period.THIRTY_MINS, Instrument.EURUSD);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        client.stopStrategy(processId);
    }

    private void getBarByShift() throws JFException{
        int shift = 1;
        IBar prevBar = history.getBar(Instrument.EURUSD, Period.ONE_HOUR, OfferSide.BID, shift);
        console.getOut().println(prevBar);
    }

    private void getBarsByTimeInterval() throws JFException{
        long prevBarTime = history.getPreviousBarStart(Period.ONE_HOUR, history.getLastTick(Instrument.EURUSD).getTime());
        long startTime =  history.getTimeForNBarsBack(Period.ONE_HOUR, prevBarTime, 5);
        List<IBar> bars = history.getBars(Instrument.EURUSD, Period.ONE_HOUR, OfferSide.BID, startTime, prevBarTime);
        int last = bars.size() - 1;
        console.getOut().format(
                "Previous bar close price=%.5f; 4th to previous bar close price=%.5f",
                bars.get(last).getClose(), bars.get(0).getClose()).println();
    }

    private void getBarsByCandleInterval() throws JFException{
        long prevBarTime = history.getPreviousBarStart(Period.ONE_HOUR, history.getLastTick(Instrument.EURUSD).getTime());
        List<IBar> bars = history.getBars(Instrument.EURUSD, Period.ONE_HOUR, OfferSide.BID, Filter.NO_FILTER, 5, prevBarTime, 0);
        int last = bars.size() - 1;
        console.getOut().format(
                "Previous bar close price=%.5f; 4th to previous bar close price=%.5f",
                bars.get(last).getClose(), bars.get(0).getClose()).println();
    }

    private void getBars(long startTime, long endTime, Period period, Instrument instrument) throws JFException{
        List<IBar> bars = history.getBars(instrument, period, OfferSide.BID, startTime, endTime);
        for(IBar bar : bars){
            System.out.println(bar);
        }
    }


    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {}

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {}

    @Override
    public void onMessage(IMessage message) throws JFException {}

    @Override
    public void onAccount(IAccount account) throws JFException {}

    @Override
    public void onStop() throws JFException {
        System.out.println("Synching finished");
    }

}
