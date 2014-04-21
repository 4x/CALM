package ai.context.util.trading;

import ai.context.util.mathematics.Operations;
import ai.context.util.measurement.OpenPosition;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BlackBox implements IStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlackBox.class);
    private double leverage = 50;
    private long maxWaitForFill = 1000L * 60L * 5L;
    private double available = 0;
    private IEngine engine = null;

    private List<OpenPosition> waitingPositions = new ArrayList<>();

    private TreeMap<String, IOrder> positions = new TreeMap<>();

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
                        double amount = Operations.roundFloor(((available / 100) * leverage) / 1000000, 4);
                        if (amount > 0) {
                            long tNow = System.currentTimeMillis();
                            IOrder out = null;
                            String direction = "LONG";
                            if (position.isLong()) {
                                out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.BUYLIMIT, amount,
                                        Operations.roundFloor(position.getStart() - 0.0001, 5), 0.8,
                                        Operations.roundFloor(position.getStopLoss(), 5),
                                        Operations.roundFloor(position.getTakeProfit(), 5),
                                        position.getGoodTillTime() + Period.FIVE_MINS.getInterval());
                            } else {
                                direction = "SHORT";
                                out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.SELLLIMIT, amount,
                                        Operations.roundFloor(position.getStart() + 0.0001, 5), 0.8,
                                        Operations.roundFloor(position.getStopLoss(), 5),
                                        Operations.roundFloor(position.getTakeProfit(), 5),
                                        position.getGoodTillTime() + Period.FIVE_MINS.getInterval());
                            }

                            LOGGER.info("OPENING: " + out.getLabel() + ", " + (out.getOriginalAmount() * 1000000D) + ", " + direction);
                            positions.put("EUR_" + tNow, out);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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
        available = account.getBalance() - (1000000D * getOpen()) / leverage;
        //available = account.getCreditLine();
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
}
