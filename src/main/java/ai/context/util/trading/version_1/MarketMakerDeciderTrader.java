package ai.context.util.trading.version_1;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.Operations;
import ai.context.util.trading.OnTickDecider;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class MarketMakerDeciderTrader implements OnTickDecider, IStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketMakerDeciderTrader.class);

    private TreeMap<Long, Set<MarketMakerPosition>> adviceByGoodTillTime = new TreeMap<>();
    private TreeMap<Long, Set<MarketMakerPosition>> specialPositions = new TreeMap<>();
    private double pnl = 0;
    private double pnlSpecial = 0;
    private long time = 0;

    private double available = 10000;

    private IEngine engine = null;
    private TreeMap<String, IOrder> positions = new TreeMap<>();

    private double[] lastMids = new double[200];
    private CSVFeed priceFeed;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");

    public MarketMakerDeciderTrader(String priceFeedFile, String startDate, IClient client){
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        if(client == null) {
            DataType[] typesPrice = new DataType[]{
                    DataType.DOUBLE,
                    DataType.DOUBLE,
                    DataType.DOUBLE,
                    DataType.DOUBLE};
            priceFeed = new CSVFeed(priceFeedFile, "yyyy.MM.dd HH:mm:ss.SSS", typesPrice, startDate);
        } else {
            client.startStrategy(this);
        }
    }

    @Override
    public void onTick(long time, double bid, double ask){

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
                if(advice.isOpen() && !advice.isClosed()){
                    if(advice.isHasOpenedWithLong()){
                        double change = (bid - advice.getOpen());
                        pnl += change;
                        String state = "P";
                        if(change <= 0){
                            state = "L";
                        }
                        System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnl, 5) + " LONG TIMEOUT " + advice.getTimeSpan() + " " + advice.attributes);
                        closePosition(advice);
                    }
                    else {
                        double change = (advice.getOpen() - ask);
                        pnl += change;
                        String state = "P";
                        if(change <= 0){
                            state = "L";
                        }
                        System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnl, 5) + " SHORT TIMEOUT " + advice.getTimeSpan() + " " + advice.attributes);
                        closePosition(advice);
                    }
                }
            }
        }

        if(PropertiesHolder.tradeSpecial){
            while(!specialPositions.isEmpty() && specialPositions.firstKey() < time){
                Set<MarketMakerPosition> ending = specialPositions.remove(specialPositions.firstKey());

                for(MarketMakerPosition position : ending){
                    if(position.isOpen() && !position.isClosed()){
                        if(position.isHasOpenedWithLong()){
                            double change = (bid - position.getOpen());
                            pnlSpecial += change;
                            String state = "P";
                            if(change <= 0){
                                state = "L";
                            }
                            System.out.println("SPECIAL " + format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + position.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnlSpecial, 5) + " LONG TIMEOUT " + position.getTimeSpan() + " " + position.attributes);
                            closePosition(position);
                        }
                        else {
                            double change = (position.getOpen() - ask);
                            pnlSpecial += change;
                            String state = "P";
                            if(change <= 0){
                                state = "L";
                            }
                            System.out.println("SPECIAL " + format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + position.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnlSpecial, 5) + " SHORT TIMEOUT " + position.getTimeSpan() + " " + position.attributes);
                            closePosition(position);
                        }
                    }
                }
            }
        }


        for(Set<MarketMakerPosition> advices : adviceByGoodTillTime.values()){

            for(MarketMakerPosition advice : advices){
                if(!advice.isOpen() && advice.getTimeAdvised() <= time){

                    if(bid > advice.getTargetHigh() + PropertiesHolder.marketMakerBeyond && !advice.containsFlag("S")){
                        advice.addFlag("S");
                    }
                    else if(advice.containsFlag("S")
                            && bid < advice.getTargetHigh() +  PropertiesHolder.marketMakerBeyond/2
                            && avgLow.containsKey(advice.getGoodTillTime())
                            && advice.getTargetLow() > avgLow.get(advice.getGoodTillTime())
                            && avgHigh.containsKey(advice.getGoodTillTime())
                            && avgHigh.get(advice.getGoodTillTime()) - bid < PropertiesHolder.maxLeewayAmplitude
                            ){
                        advice.setHasOpenedWithShort(true, bid, time);
                    }
                    else if(ask < advice.getTargetLow() - PropertiesHolder.marketMakerBeyond && !advice.containsFlag("L")){
                        advice.addFlag("L");
                    }
                    else if(advice.containsFlag("L")
                            && ask > advice.getTargetLow() - PropertiesHolder.marketMakerBeyond/2
                            && avgHigh.containsKey(advice.getGoodTillTime())
                            && advice.getTargetHigh() < avgHigh.get(advice.getGoodTillTime())
                            && avgLow.containsKey(advice.getGoodTillTime())
                            && ask - avgLow.get(advice.getGoodTillTime()) < PropertiesHolder.maxLeewayAmplitude
                            ){
                        advice.setHasOpenedWithLong(true, ask, time);
                    }

                    if(advice.isOpen()){
                        advice.attributes.put("wait", time - advice.getTimeAdvised());

                        advice.attributes.put("targetHigh", advice.getTargetHigh() - advice.getOpen());
                        advice.attributes.put("targetLow", advice.getOpen() - advice.getTargetLow());

                        advice.attributes.put("d1", d1);
                        advice.attributes.put("d5", d5);
                        advice.attributes.put("d10", d10);
                        advice.attributes.put("d20", d20);
                        advice.attributes.put("d50", d50);
                        advice.attributes.put("d100", d100);
                        advice.attributes.put("d199", d199);


                        if(PropertiesHolder.liveTrading && engine != null){
                            try {
                                double amount = Operations.round((available * PropertiesHolder.tradeToCreditRatio) / 1000000, 4);
                                if (amount > 0 && PropertiesHolder.filterFunction.pass(advice)) {
                                    long tNow = System.currentTimeMillis();
                                    if(advice.isHasOpenedWithShort()) {
                                        String direction = "SHORT";
                                        IOrder out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.SELLLIMIT, amount,
                                                Operations.round(bid, 5), 0,
                                                Operations.round(bid + PropertiesHolder.marketMakerStopLoss, 5),
                                                Operations.round(advice.getTargetLow(), 5),
                                                advice.getGoodTillTime());
                                        LOGGER.info("OPENING: " + out.getLabel() + ", " + (out.getOriginalAmount() * 1000000D) + ", " + direction);
                                        positions.put(out.getLabel(), out);
                                        advice.setOrderId(out.getLabel());
                                        Thread.sleep(2);
                                    } else if(advice.isHasOpenedWithLong()) {
                                        String direction = "LONG";
                                        IOrder out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.BUYLIMIT, amount,
                                                Operations.round(ask, 5), 0,
                                                Operations.round(ask - PropertiesHolder.marketMakerStopLoss, 5),
                                                Operations.round(advice.getTargetHigh(), 5),
                                                advice.getGoodTillTime());
                                        LOGGER.info("OPENING: " + out.getLabel() + ", " + (out.getOriginalAmount() * 1000000D) + ", " + direction);
                                        positions.put(out.getLabel(), out);
                                        advice.setOrderId(out.getLabel());
                                        Thread.sleep(2);
                                    }
                                }
                            } catch (InterruptedException e){
                                e.printStackTrace();
                            } catch (JFException e) {
                                e.printStackTrace();
                            }
                        }

                        if(PropertiesHolder.tradeSpecial){
                            double confirmation = OrderIntelligenceEngine.getInstance().getConfirmationFor(advice);
                            if(confirmation > 0){
                                MarketMakerPosition position = new MarketMakerPosition(advice.getTimeAdvised(),
                                        advice.getOpen() + confirmation,
                                        advice.getOpen() - confirmation,
                                        advice.getHigh1(),
                                        advice.getLow1(),
                                        advice.getGoodTillTime());
                                if(advice.isHasOpenedWithShort()){
                                    position.setHasOpenedWithShort(true, advice.getOpen(), time);
                                } else{
                                    position.setHasOpenedWithLong(true, advice.getOpen(), time);
                                }

                                if(!specialPositions.containsKey(position.getGoodTillTime())){
                                    specialPositions.put(position.getGoodTillTime(), new HashSet<MarketMakerPosition>());
                                }
                                specialPositions.get(position.getGoodTillTime()).add(position);
                            }
                        }
                    }
                }
                else if(!advice.isClosed()){
                    if(advice.isHasOpenedWithLong()){
                        advice.notify(bid, bid, bid, time);
                        if(advice.getTargetHigh() <= bid || advice.getOpen() >= bid + PropertiesHolder.marketMakerStopLoss){
                            double change = (bid - advice.getOpen());
                            pnl += change;
                            String state = "P";
                            if(change <= 0){
                                state = "L";
                            }
                            System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnl, 5) + " LONG NORMAL " + advice.getTimeSpan() + " " + advice.attributes);
                            closePosition(advice);
                        }
                        else if(avgHigh.containsKey(advice.getGoodTillTime()) && avgHigh.get(advice.getGoodTillTime()) <= bid){
                            double change = (bid - advice.getOpen());
                            pnl += change;
                            String state = "P";
                            if(change <= 0){
                                state = "L";
                            }
                            System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnl, 5) + " LONG RECALC " + advice.getTimeSpan() + " " + advice.attributes);
                            closePosition(advice);
                        }
                        else if(time - advice.getOpenTime() >= PropertiesHolder.maxOpenTime){
                            double change = (bid - advice.getOpen());
                            pnl += change;
                            String state = "P";
                            if(change <= 0){
                                state = "L";
                            }
                            System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnl, 5) + " LONG FORCED_TIMEOUT " + advice.getTimeSpan() + " " + advice.attributes);
                            closePosition(advice);
                        }
                    }
                    else if(advice.isHasOpenedWithShort()){
                        advice.notify(ask, ask, ask, time);
                        if(advice.getTargetLow() >= ask || advice.getOpen() <= ask - PropertiesHolder.marketMakerStopLoss){
                            double change = (advice.getOpen() - ask);
                            pnl += change;
                            String state = "P";
                            if(change <= 0){
                                state = "L";
                            }
                            System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnl, 5) + " SHORT NORMAL " + advice.getTimeSpan() + " " + advice.attributes);
                            closePosition(advice);
                        }
                        else if(avgLow.containsKey(advice.getGoodTillTime()) && avgLow.get(advice.getGoodTillTime()) >= ask){
                            double change = (advice.getOpen() - ask);
                            pnl += change;
                            String state = "P";
                            if(change <= 0){
                                state = "L";
                            }
                            System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnl, 5) + " SHORT RECALC " + advice.getTimeSpan() + " " + advice.attributes);
                            closePosition(advice);
                        }
                        else if(time - advice.getOpenTime() >= PropertiesHolder.maxOpenTime){
                            double change = (advice.getOpen() - ask);
                            pnl += change;
                            String state = "P";
                            if(change <= 0){
                                state = "L";
                            }
                            System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnl, 5) + " SHORT FORCED_TIMEOUT " + advice.getTimeSpan() + " " + advice.attributes);
                            closePosition(advice);
                        }
                    }
                }
            }
        }

        if(PropertiesHolder.tradeSpecial){
            for(Set<MarketMakerPosition> positions : specialPositions.values()){
                for(MarketMakerPosition position : positions){
                    if(!position.isClosed()){
                        if(position.isHasOpenedWithLong()){
                            position.notify(bid, bid, bid, time);
                            if(position.getTargetHigh() <= bid || position.getTargetLow() >= bid){
                                double change = (bid - position.getOpen());
                                pnlSpecial += change;
                                String state = "P";
                                if(change <= 0){
                                    state = "L";
                                }
                                System.out.println("SPECIAL: " + format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + position.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnlSpecial, 5) + " LONG NORMAL " + position.getTimeSpan() + " " + position.attributes);
                                closePosition(position);
                            }
                        }
                        else if(position.isHasOpenedWithShort()){
                            position.notify(ask, ask, ask, time);
                            if(position.getTargetLow() >= ask || position.getTargetHigh() <= ask){
                                double change = (position.getOpen() - ask);
                                pnlSpecial += change;
                                String state = "P";
                                if(change <= 0){
                                    state = "L";
                                }
                                System.out.println("SPECIAL: " + format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + position.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnlSpecial, 5) + " SHORT NORMAL " + position.getTimeSpan() + " " + position.attributes);
                                closePosition(position);
                            }
                        }
                    }
                }
            }
        }
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void addAdvice(MarketMakerPosition advice){
        if(engine == null) {
            time = Math.max(time, advice.getTimeAdvised());

            if (!adviceByGoodTillTime.containsKey(advice.getGoodTillTime())) {
                adviceByGoodTillTime.put(advice.getGoodTillTime(), new HashSet<MarketMakerPosition>());
            }
            adviceByGoodTillTime.get(advice.getGoodTillTime()).add(advice);
        } else{
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
        }
        refreshAverages();
    }

    public void closePosition(MarketMakerPosition position){
        position.setClosed(true);
        try {
            if(engine != null && position.getOrderId() != null && positions.containsKey(position.getOrderId())){
                engine.closeOrders(positions.get(position.getOrderId()));
            }
        } catch (JFException e) {
            e.printStackTrace();
        }
    }

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

                    double w = Math.sqrt(Math.exp(-Math.pow((t - tA)/tS, 2)) * Math.exp(-Math.pow((t - tE)/PropertiesHolder.horizonUpperBound, 2))) * Math.log((Double) advice.attributes.get("cred") + 1);
                    sumW += w;

                    avgHigh += w * advice.getHigh1();
                    avgLow += w * advice.getLow1();
                }
            }

            this.avgHigh.put(t, avgHigh/sumW);
            this.avgLow.put(t, avgLow/sumW);
        }
    }

    private long lastTime = 0;
    private double lastBid = -1;
    private double lastAsk = -1;
    public void step(){
        FeedObject data;
        if(lastAsk == -1){
            data = priceFeed.readNext(this);
            lastTime = data.getTimeStamp();
            lastBid = (double) ((Object[])data.getData())[1];
            lastAsk = (double) ((Object[])data.getData())[0];
        }
        else if(lastTime <= time + PropertiesHolder.timeQuantum){
            onTick(lastTime, lastBid, lastAsk);
        }

        while(lastTime <= time + PropertiesHolder.timeQuantum){
            data = priceFeed.readNext(this);
            if(data != null){
                lastTime = data.getTimeStamp();
                lastBid = (double) ((Object[])data.getData())[1];
                lastAsk = (double) ((Object[])data.getData())[0];
                if(lastTime > time + PropertiesHolder.timeQuantum){
                    break;
                }

                onTick(lastTime, lastBid, lastAsk);
            }
            else {
                adviceByGoodTillTime.clear();
                break;
            }
        }
    }

    public Collection<MarketMakerPosition> getAdvices() {
        ArrayList<MarketMakerPosition> list = new ArrayList<>();
        for(Set<MarketMakerPosition> set : adviceByGoodTillTime.values()){
            list.addAll(set);
        }
        return list;
    }

    @Override
    public void onStart(IContext context) throws JFException {
        engine = context.getEngine();
        LOGGER.info("MarketMakerDecider started, waiting for decisions...");
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (instrument == Instrument.EURUSD) {
            onTick(tick.getTime(), tick.getBid(), tick.getAsk());
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