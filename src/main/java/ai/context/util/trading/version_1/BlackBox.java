package ai.context.util.trading.version_1;

import ai.context.util.mathematics.Operations;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BlackBox implements IStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlackBox.class);
    private long maxWaitForFill = 1000L * 60L * 10L;
    private double tradeToCreditRatio = 0.1;
    private double available = 10000;
    private IEngine engine = null;

    private List<OpenPosition> waitingPositions = new ArrayList<>();

    private TreeMap<String, IOrder> positions = new TreeMap<>();
    private Set<IOrder> toClose = new HashSet<>();

    public BlackBox(IClient client) {
        client.startStrategy(this);
    }

    @Override
    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        LOGGER.info("BlackBox started, waiting for decisions...");
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {

        if (instrument == Instrument.EURUSD) {
            synchronized (waitingPositions) {
                while (!waitingPositions.isEmpty()) {
                    try {
                        OpenPosition position = waitingPositions.remove(0);
                        double amount = Operations.round((available * tradeToCreditRatio) / 1000000, 4);
                        if (amount > 0) {
                            long tNow = System.currentTimeMillis();
                            IOrder out = null;
                            String direction = "LONG";
                            if (position.isLong()) {
                                out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.BUYLIMIT, amount,
                                        Operations.round(position.getStart() - 0.0001, 5), 0.5,
                                        Operations.round(position.getStopLoss(), 5),
                                        Operations.round(position.getTakeProfit(), 5),
                                        position.getGoodTillTime());
                            } else {
                                direction = "SHORT";
                                out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.SELLLIMIT, amount,
                                        Operations.round(position.getStart() + 0.0001, 5), 0.5,
                                        Operations.round(position.getStopLoss(), 5),
                                        Operations.round(position.getTakeProfit(), 5),
                                        position.getGoodTillTime());
                            }

                            LOGGER.info("OPENING: " + out.getLabel() + ", " + (out.getOriginalAmount() * 1000000D) + ", " + direction);
                            positions.put("EUR_" + tNow, out);
                            position.setOrder(out);
                            Thread.sleep(2);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if(!toClose.isEmpty()){
                engine.closeOrders(toClose);
                toClose.clear();
            }
        }
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (instrument == Instrument.EURUSD) {
            long tNow = System.currentTimeMillis();
            for (Map.Entry<String, IOrder> out : positions.entrySet()) {
                IOrder order = out.getValue();
                if (tNow - order.getCreationTime() > maxWaitForFill && order.getState() == IOrder.State.OPENED) {
                    order.close();
                    LOGGER.info("Order " + order.getLabel() + " has not been filled yet, closing it...");
                }
            }
        }
    }

    @Override
    public void onMessage(IMessage message) throws JFException {
        if (message.getType() == IMessage.Type.ORDER_CLOSE_OK) {
            IOrder order = message.getOrder();
            positions.remove(order.getLabel());
            LOGGER.info(order.getLabel() + " has closed, PNL: " + order.getProfitLossInUSD() + ", COMMISSION: " + order.getCommissionInUSD() + ", AVAILABLE: " + available);
        } else if (message.getType() == IMessage.Type.ORDER_FILL_OK) {
            IOrder order = message.getOrder();
            positions.remove(order.getLabel());
            LOGGER.info(order.getLabel() + " has been filled at " + order.getOpenPrice() + " for " + order.getOriginalAmount());
        }
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
        available = account.getCreditLine();
    }

    @Override
    public void onStop() throws JFException {
        LOGGER.info("BlackBox stopped");
    }

    private double getOpen() throws JFException {
        double counter = 0;
        for (IOrder order : engine.getOrders()) {
            counter += order.getOriginalAmount();
        }
        return counter;
    }

    public void onDecision(OpenPosition position) throws JFException {
        synchronized (waitingPositions) {
            waitingPositions.add(position);
        }
    }

    public void toClose(IOrder order){
        if(order != null && positions.containsKey(order.getLabel())){
            toClose.add(order);
        }
    }
}
