package ai.context.util.trading;

import ai.context.util.mathematics.Operations;
import ai.context.util.measurement.MarketMakerPosition;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class MarketMakerDecider implements OnTickDecider, IStrategy{

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketMakerDecider.class);

    private TreeMap<Long, Set<MarketMakerPosition>> adviceByGoodTillTime = new TreeMap<>();
    private IEngine engine = null;
    private TreeMap<String, IOrder> positions = new TreeMap<>();

    private double tradeToCreditRatio = 0.025;
    private double available = 10000;

    public MarketMakerDecider(IClient client) {
        client.startStrategy(this);
    }

    @Override
    public void onTick(long time, double bid, double ask) throws InterruptedException, JFException {
        while(adviceByGoodTillTime.firstKey() < time){
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
            double avgHigh = 0;
            double avgLow = 0;

            for(MarketMakerPosition advice : advices){
                avgHigh += advice.getHigh1();
                avgLow += advice.getLow1();
            }
            avgHigh /= advices.size();
            avgLow /= advices.size();

            for(MarketMakerPosition advice : advices){
                if(!advice.isOpen()){
                    double amount = Operations.round((available * tradeToCreditRatio) / 1000000, 4);
                    if (amount > 0) {
                        long tNow = System.currentTimeMillis();
                        IOrder out = null;
                        String direction = null;

                        if(bid > advice.getTargetHigh() && advice.getTargetLow() > avgLow){
                            advice.setHasOpenedWithShort(true, bid);
                            direction = "SHORT";

                            out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.SELLLIMIT, amount,
                                    Operations.round(advice.getTargetHigh() + 0.0001, 5), 0,
                                    Operations.round(advice.getTargetHigh() + 0.01, 5),
                                    Operations.round(advice.getTargetLow() - 0.0001, 5),
                                    advice.getGoodTillTime());
                        }
                        else if(ask < advice.getTargetLow() && advice.getTargetHigh() < avgHigh){
                            advice.setHasOpenedWithLong(true, ask);
                            direction = "LONG";

                            out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.BUYLIMIT, amount,
                                    Operations.round(advice.getTargetLow() - 0.0001, 5), 0,
                                    Operations.round(advice.getTargetLow() - 0.01, 5),
                                    Operations.round(advice.getTargetHigh() + 0.0001, 5),
                                    advice.getGoodTillTime());
                        }
                        LOGGER.info("OPENING: " + out.getLabel() + ", " + (out.getOriginalAmount() * 1000000D) + ", " + direction);
                        positions.put(out.getLabel(), out);
                        advice.setOrderId(out.getLabel());
                        Thread.sleep(2);
                    }
                }
            }
        }
    }

    public void addAdvice(MarketMakerPosition advice){
        if(!adviceByGoodTillTime.containsKey(advice.getGoodTillTime())){
            adviceByGoodTillTime.put(advice.getGoodTillTime(), new HashSet<MarketMakerPosition>());
        }
        adviceByGoodTillTime.get(advice.getGoodTillTime()).add(advice);
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
}