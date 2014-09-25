package ai.context.util.trading.version_1;

import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.Operations;
import ai.context.util.trading.OnTickDecider;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MarketMakerDecider implements OnTickDecider, IStrategy{

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketMakerDecider.class);

    private TreeMap<Long, Set<MarketMakerPosition>> adviceByGoodTillTime = new TreeMap<>();
    private IEngine engine = null;
    private TreeMap<String, IOrder> positions = new TreeMap<>();

    private double available = 10000;
    private double[] lastMids = new double[200];

    public MarketMakerDecider(IClient client) {
        client.startStrategy(this);
    }

    @Override
    public void onTick(long time, double bid, double ask) throws InterruptedException, JFException {

        for(int i = lastMids.length - 1; i > 0; i--){
            lastMids[i] = lastMids[i - 1];
        }
        double mid = lastMids[0] = (bid + ask)/2;
        double d1 = mid - lastMids[1];
        double d5 = mid - lastMids[5];
        double d10 = mid - lastMids[10];
        double d20 = mid - lastMids[20];
        double d50 = mid - lastMids[50];
        double d100 = mid - lastMids[100];
        double d199 = mid - lastMids[199];


        while(!adviceByGoodTillTime.isEmpty() && adviceByGoodTillTime.firstKey() < time){
            Set<MarketMakerPosition> ending = adviceByGoodTillTime.remove(adviceByGoodTillTime.firstKey());

            for(MarketMakerPosition advice : ending){
                if(advice.isOpen()){
                    try {
                        if(positions.containsKey(advice.getOrderId())){
                            engine.closeOrders(positions.get(advice.getOrderId()));
                        }
                    } catch (JFException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        for(Set<MarketMakerPosition> advices : adviceByGoodTillTime.values()){

            for(MarketMakerPosition advice : advices){
                if(!advice.isOpen()){
                    double amount = Operations.round((available * PropertiesHolder.tradeToCreditRatio) / 1000000, 4);
                    if (amount > 0) {
                        long tNow = System.currentTimeMillis();
                        IOrder out = null;
                        String direction = null;

                        advice.attributes.put("tNow", tNow);

                        if(bid - PropertiesHolder.marketMakerBeyond > advice.getTargetHigh()){
                            advice.addFlag("A");
                        }
                        else if(advice.containsFlag("A")
                                && bid > advice.getTargetHigh()
                                && bid - PropertiesHolder.marketMakerBeyond/2 < advice.getTargetHigh()
                                && avgLow.containsKey(advice.getGoodTillTime())
                                && advice.getTargetLow() > avgLow.get(advice.getGoodTillTime())
                                && avgHigh.containsKey(advice.getGoodTillTime())
                                && avgHigh.get(advice.getGoodTillTime()) - bid < PropertiesHolder.maxLeewayAmplitude
                                && advice.getHigh1() - bid < PropertiesHolder.maxLeewayAmplitude
                                && PropertiesHolder.filterFunction.pass(advice)){
                            direction = "SHORT";

                            out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.SELLLIMIT, amount,
                                    Operations.round(bid, 5), 0,
                                    Operations.round(bid + PropertiesHolder.marketMakerStopLoss, 5),
                                    Operations.round(advice.getTargetLow(), 5),
                                    advice.getGoodTillTime());
                            advice.setHasOpenedWithShort(true, bid, time);
                        }
                        else if(ask + PropertiesHolder.marketMakerBeyond < advice.getTargetLow()){
                            advice.addFlag("B");
                        }
                        else if(advice.containsFlag("B")
                                && ask < advice.getTargetLow()
                                && ask + PropertiesHolder.marketMakerBeyond/2 > advice.getTargetLow()
                                && avgHigh.containsKey(advice.getGoodTillTime())
                                && advice.getTargetHigh() < avgHigh.get(advice.getGoodTillTime())
                                && avgLow.containsKey(advice.getGoodTillTime())
                                && ask - avgLow.get(advice.getGoodTillTime()) < PropertiesHolder.maxLeewayAmplitude
                                && ask - advice.getLow1() < PropertiesHolder.maxLeewayAmplitude
                                && PropertiesHolder.filterFunction.pass(advice)){
                            direction = "LONG";

                            out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.BUYLIMIT, amount,
                                    Operations.round(ask, 5), 0,
                                    Operations.round(ask - PropertiesHolder.marketMakerStopLoss, 5),
                                    Operations.round(advice.getTargetHigh(), 5),
                                    advice.getGoodTillTime());
                            advice.setHasOpenedWithLong(true, ask, time);
                        }

                        if(out != null){
                            LOGGER.info("OPENING: " + out.getLabel() + ", " + (out.getOriginalAmount() * 1000000D) + ", " + direction);
                            positions.put(out.getLabel(), out);
                            advice.setOrderId(out.getLabel());
                            Thread.sleep(2);
                        }
                    }
                }
                else if(!advice.isClosed()){
                    if(advice.isHasOpenedWithLong()){
                        if(avgHigh.containsKey(advice.getGoodTillTime()) && avgHigh.get(advice.getGoodTillTime()) <= bid){
                            try {
                                if(positions.containsKey(advice.getOrderId())){
                                    engine.closeOrders(positions.get(advice.getOrderId()));
                                }
                            } catch (JFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(advice.isHasOpenedWithShort()){
                        if(avgLow.containsKey(advice.getGoodTillTime()) && avgLow.get(advice.getGoodTillTime()) >= ask){
                            try {
                                if(positions.containsKey(advice.getOrderId())){
                                    engine.closeOrders(positions.get(advice.getOrderId()));
                                }
                            } catch (JFException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public void addAdvice(MarketMakerPosition advice){
        if(advice.getTimeAdvised() > System.currentTimeMillis() - 30000){
            time = Math.max(time, advice.getTimeAdvised());
            if(!adviceByGoodTillTime.containsKey(advice.getGoodTillTime())){
                adviceByGoodTillTime.put(advice.getGoodTillTime(), new HashSet<MarketMakerPosition>());
            }
            adviceByGoodTillTime.get(advice.getGoodTillTime()).add(advice);
            LOGGER.info("Added advice [" + Operations.round(advice.getTargetLow(), 5) + " -> " + Operations.round(advice.getTargetHigh(), 5) + "] good till " + new Date(advice.getGoodTillTime()));
        }
        else {
            LOGGER.info("Discarded advice [" + Operations.round(advice.getTargetLow(), 5) + " -> " + Operations.round(advice.getTargetHigh(), 5) + "] expired time " + new Date(advice.getTimeAdvised()));
        }
        refreshAverages();
    }

    private long time = 0;
    private TreeMap<Long, Double> avgHigh = new TreeMap<>();
    private TreeMap<Long, Double> avgLow = new TreeMap<>();
    private void refreshAverages(){
        avgHigh.clear();
        avgLow.clear();

        for(long t = time + PropertiesHolder.timeQuantum; t <= time + PropertiesHolder.horizonUpperBound; t += PropertiesHolder.timeQuantum){
            double avgHigh = 0;
            double avgLow = 0;

            double sumW = 0;

            for(Set<MarketMakerPosition> set : adviceByGoodTillTime.values()){
                for(MarketMakerPosition advice : set){
                    double tA = advice.getTimeAdvised();
                    double tE = advice.getGoodTillTime();
                    double tS = tE - tA;

                    double w = Math.exp(-Math.pow((t - tA)/tS, 2)) * Math.exp(-Math.pow((t - tE)/PropertiesHolder.horizonUpperBound, 2));
                    sumW += w;

                    avgHigh += w * advice.getHigh1();
                    avgLow += w * advice.getLow1();
                }
            }

            this.avgHigh.put(t, avgHigh/sumW);
            this.avgLow.put(t, avgLow/sumW);
        }
    }

    @Override
    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        LOGGER.info("MarketMakerDecider started, waiting for decisions...");
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (instrument == Instrument.EURUSD) {
            try {
                onTick(tick.getTime(), tick.getBid(), tick.getAsk());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

    }

    @Override
    public void onMessage(IMessage message) throws JFException {
        if (message.getType() == IMessage.Type.ORDER_CLOSE_OK) {
            IOrder order = message.getOrder();
            positions.remove(order.getLabel());
            LOGGER.info(order.getLabel() + " has closed, PNL: " + order.getProfitLossInUSD() + ", COMMISSION: " + order.getCommissionInUSD() + ", AVAILABLE: " + available);
        } else if (message.getType() == IMessage.Type.ORDER_FILL_OK) {
            IOrder order = message.getOrder();
            LOGGER.info(order.getLabel() + " has been filled at " + order.getOpenPrice() + " for " + order.getOriginalAmount());
        }
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
        available = account.getCreditLine();
    }

    @Override
    public void onStop() throws JFException {

    }

    public List<MarketMakerPosition> getAdvices(){
        ArrayList<MarketMakerPosition> list = new ArrayList<>();
        for(Set<MarketMakerPosition> set : adviceByGoodTillTime.values()){
            list.addAll(set);
        }
        return list;
    }
}
