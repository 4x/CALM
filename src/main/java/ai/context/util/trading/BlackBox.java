package ai.context.util.trading;

import ai.context.util.mathematics.Operations;
import ai.context.util.measurement.OpenPosition;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

public class BlackBox implements IStrategy{

    private static final Logger LOGGER = LoggerFactory.getLogger(BlackBox.class);
    private double leverage = 50;
    private long maxWaitForFill = 1000L * 60L * 5L;
    private double available = 0;
    private IEngine engine = null;

    private TreeMap<String, IOrder> positions = new TreeMap<>();

    public BlackBox(IClient client){
        client.startStrategy(this);
    }

    @Override
    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        LOGGER.info("BlackBox started, waiting for decisions...");
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        long tNow = System.currentTimeMillis();
        for(Map.Entry<String, IOrder> out : positions.entrySet()){
            IOrder order = out.getValue();
            if(tNow - order.getCreationTime() > maxWaitForFill && order.getState() == IOrder.State.OPENED){
                order.close();
            }
        }
    }

    @Override
    public void onMessage(IMessage message) throws JFException {
        if(message.getType() == IMessage.Type.ORDER_CLOSE_OK){
            IOrder order = message.getOrder();
            positions.remove(order.getLabel());
            LOGGER.info(order.getLabel() + " has closed, PNL: " + order.getProfitLossInUSD() + ", COMMISSION: " + order.getCommissionInUSD());
        }
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
        available = account.getBalance() - getOpen()/leverage;
    }

    @Override
    public void onStop() throws JFException {
        LOGGER.info("BlackBox stopped");
    }

    private double getOpen() throws JFException{
        double counter = 0;
        for (IOrder order : engine.getOrders()) {
            counter += order.getOriginalAmount();
        }
        return counter;
    }

    public void onDecision(OpenPosition position) throws JFException {

        double amount = Operations.roundFloor(((available / 100) * leverage)/1000000, 4);
        if(amount > 0){
            long tNow = System.currentTimeMillis();
            IOrder out = null;
            String direction = "LONG";
            if(position.isLong()){
                out = engine.submitOrder("" + tNow, Instrument.EURUSD, IEngine.OrderCommand.BUYLIMIT, amount,
                        Operations.roundFloor(position.getStart(), 5), 0.8,
                        Operations.roundFloor(position.getStopLoss(), 5),
                        Operations.roundFloor(position.getTakeProfit(), 5));
            }
            else {
                direction = "SHORT";
                out = engine.submitOrder("" + tNow, Instrument.EURUSD, IEngine.OrderCommand.SELLLIMIT, amount,
                        Operations.roundFloor(position.getStart(), 5), 0.8,
                        Operations.roundFloor(position.getStopLoss(), 5),
                        Operations.roundFloor(position.getTakeProfit(), 5));
            }

            LOGGER.info("OPENING: " + out.getLabel() + ", " + out.getOriginalAmount() + ", " + direction);
            positions.put("" + tNow, out);
        }
    }
}
