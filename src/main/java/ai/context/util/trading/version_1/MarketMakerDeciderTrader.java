package ai.context.util.trading.version_1;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.learning.neural.NeuronCluster;
import ai.context.util.common.email.EmailSendingService;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.AverageAggregator;
import ai.context.util.mathematics.MinMaxAggregator;
import ai.context.util.mathematics.Operations;
import ai.context.util.mathematics.SimpleLinearRegressor;
import ai.context.util.trading.OnTickDecider;
import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class MarketMakerDeciderTrader implements OnTickDecider, IStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketMakerDeciderTrader.class);

    private Set<MarketMakerPosition> advices = new HashSet<>();
    private TreeMap<Long, Set<MarketMakerPosition>> adviceByGoodTillTime = new TreeMap<>();
    private TreeMap<Long, MinMaxAggregator> highLowByAdvisedTime = new TreeMap<>();

    private EmailSendingService emailSendingService = NeuronCluster.getInstance().getEmailSendingService();

    private double pnl = 0;
    private double pnlSpecial = 0;
    private long time = 0;

    private double available = 10000;

    private double resolution = 0.0001;

    private double equity = 0;

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

        for(MinMaxAggregator aggregator : highLowByAdvisedTime.values()){
            aggregator.addValue(bid);
        }

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
            advices.removeAll(ending);
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

        for(Set<MarketMakerPosition> advices : adviceByGoodTillTime.values()){

            for(MarketMakerPosition advice : advices){
                if(!advice.isOpen() && advice.getTimeAdvised() <= time){

                    if (!advice.containsFlag("S") && bid > advice.getTargetHigh() + PropertiesHolder.marketMakerBeyond) {
                        advice.addFlag("S");
                    } else if (advice.containsFlag("S")
                            && bid < advice.getTargetHigh() + PropertiesHolder.marketMakerBeyond / 2
                            && avgLow.containsKey(advice.getGoodTillTime())
                            && advice.getTargetLow() > avgLow.get(advice.getGoodTillTime())
                            && avgHigh.containsKey(advice.getGoodTillTime())
                            && avgHigh.get(advice.getGoodTillTime()) - bid < PropertiesHolder.maxLeewayAmplitude
                            ) {
                        advice.setHasOpenedWithShort(true, bid, time);
                    } else if (!advice.containsFlag("L") && ask < advice.getTargetLow() - PropertiesHolder.marketMakerBeyond) {
                        advice.addFlag("L");
                    } else if (advice.containsFlag("L")
                            && ask > advice.getTargetLow() - PropertiesHolder.marketMakerBeyond / 2
                            && avgHigh.containsKey(advice.getGoodTillTime())
                            && advice.getTargetHigh() < avgHigh.get(advice.getGoodTillTime())
                            && avgLow.containsKey(advice.getGoodTillTime())
                            && ask - avgLow.get(advice.getGoodTillTime()) < PropertiesHolder.maxLeewayAmplitude
                            ) {
                        advice.setHasOpenedWithLong(true, ask, time);
                    }

                    if (advice.isOpen()) {
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
                        advice.attributes.put("avgW", averagesWeights.get(advice.getGoodTillTime()));
                        advice.attributes.put("avgWL", averagesWeightsBasedOnL.get(advice.getGoodTillTime()));
                        advice.attributes.put("avgWH", averagesWeightsBasedOnH.get(advice.getGoodTillTime()));
                        advice.attributes.put("gradHLineA", gradHLineA);
                        advice.attributes.put("gradLLineA", gradLLineA);
                        advice.attributes.put("gradHLineE", gradHLineE);
                        advice.attributes.put("gradLLineE", gradLLineE);

                        double expectedRecovery = 0;
                        double recoveryRatio = 1;

                        if(advice.recoveryInfo != null) {
                            double up = Math.abs(bid - advice.getPivot());
                            double down = Math.abs(advice.getPivot() - ask);
                            double amp = Math.max(up, down);
                            if (ask < advice.getPivot()) {
                                amp *= -1;
                            }

                            int dir = 1;
                            if(amp < 0){
                                dir = -1;
                            }

                            int recoveryKey = getLogarithmicDiscretisation(amp, 0, resolution);
                            AverageAggregator a1 = advice.recoveryInfo.getData().get(recoveryKey);
                            AverageAggregator a2 = advice.recoveryInfo.getData().get(-recoveryKey);
                            if(a1 != null && a2 != null){
                                recoveryRatio = a1.getSum()/a2.getSum();
                            }
                            double sum = 0;
                            double count = 0;
                            if(a1 != null) {
                                sum += a1.getSum();
                                count += a1.getCount();
                            }
                            for (double i = 1; i < 2; i++) {
                                AverageAggregator averageAggregator = advice.recoveryInfo.getData().get((int)(recoveryKey + (dir * i)));
                                if (averageAggregator != null) {
                                    double coefficient = Math.pow(2, -i);
                                    sum += coefficient * averageAggregator.getSum();
                                    count += coefficient * averageAggregator.getCount();
                                }
                            }
                            expectedRecovery = sum / count;
                        }
                        advice.attributes.put("recovery", expectedRecovery);
                        advice.attributes.put("recoveryRatio", recoveryRatio);

                        if (PropertiesHolder.liveTrading && engine != null) {
                            try {
                                double amount = Operations.round((available * PropertiesHolder.tradeToCreditRatio) / 1000000, 4);
                                if (amount > 0 && PropertiesHolder.filterFunction.pass(advice)) {
                                    long tNow = System.currentTimeMillis();
                                    String info = null;
                                    if (advice.isHasOpenedWithShort()) {
                                        String direction = "SHORT";
                                        IOrder out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.SELLLIMIT, amount,
                                                Operations.round(bid, 5), 0,
                                                Operations.round(bid + PropertiesHolder.marketMakerStopLoss, 5),
                                                Operations.round(advice.getTargetLow(), 5),
                                                advice.getGoodTillTime());

                                        info = "OPENING: " + out.getLabel() + ", " + (out.getOriginalAmount() * 1000000D) + ", " + direction;
                                        LOGGER.info(info);

                                        positions.put(out.getLabel(), out);
                                        advice.setOrderId(out.getLabel());
                                        Thread.sleep(2);
                                    } else if (advice.isHasOpenedWithLong()) {
                                        String direction = "LONG";
                                        IOrder out = engine.submitOrder("EUR_" + tNow, Instrument.EURUSD, IEngine.OrderCommand.BUYLIMIT, amount,
                                                Operations.round(ask, 5), 0,
                                                Operations.round(ask - PropertiesHolder.marketMakerStopLoss, 5),
                                                Operations.round(advice.getTargetHigh(), 5),
                                                advice.getGoodTillTime());

                                        info = "OPENING: " + out.getLabel() + ", " + (out.getOriginalAmount() * 1000000D) + ", " + direction;
                                        LOGGER.info(info);

                                        positions.put(out.getLabel(), out);
                                        advice.setOrderId(out.getLabel());
                                        Thread.sleep(2);
                                    }
                                    emailSendingService.queueEmail("algo@balgobin.london", "hans@balgobin.london","Algo Trade Open", info);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (JFException e) {
                                e.printStackTrace();
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

            advices.add(advice);
            if(!highLowByAdvisedTime.containsKey(advice.getTimeAdvised())){
                highLowByAdvisedTime.put(advice.getTimeAdvised(), new MinMaxAggregator());
            }
        } else{
            if(advice.getTimeAdvised() > System.currentTimeMillis() - 30000){
                time = Math.max(time, advice.getTimeAdvised());
                if(!adviceByGoodTillTime.containsKey(advice.getGoodTillTime())){
                    adviceByGoodTillTime.put(advice.getGoodTillTime(), new HashSet<MarketMakerPosition>());
                }
                adviceByGoodTillTime.get(advice.getGoodTillTime()).add(advice);

                advices.add(advice);
                if(!highLowByAdvisedTime.containsKey(advice.getTimeAdvised())){
                    highLowByAdvisedTime.put(advice.getTimeAdvised(), new MinMaxAggregator());
                }
                LOGGER.info("Added advice [" + Operations.round(advice.getTargetLow(), 5) + " -> " + Operations.round(advice.getTargetHigh(), 5) + "] good till " + new Date(advice.getGoodTillTime()));
            }
            else {
                LOGGER.info("Discarded advice [" + Operations.round(advice.getTargetLow(), 5) + " -> " + Operations.round(advice.getTargetHigh(), 5) + "] expired time " + new Date(advice.getTimeAdvised()));
            }
        }
        refreshHighLows();
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

    private void refreshHighLows(){
        long earliest = Long.MAX_VALUE;

        for(MarketMakerPosition advice : advices){
            if(advice.getTimeAdvised() < earliest){
                earliest = advice.getTimeAdvised();
            }
        }

        while(highLowByAdvisedTime.firstKey() < earliest){
            highLowByAdvisedTime.remove(highLowByAdvisedTime.firstKey());
        }
    }

    private TreeMap<Long, Double> avgHigh = new TreeMap<>();
    private TreeMap<Long, Double> avgLow = new TreeMap<>();
    private TreeMap<Long, Double> averagesWeights = new TreeMap<>();
    private TreeMap<Long, Double> averagesWeightsBasedOnH = new TreeMap<>();
    private TreeMap<Long, Double> averagesWeightsBasedOnL = new TreeMap<>();

    private double gradHLineA = 0;
    private double gradLLineA = 0;
    private double gradHLineE = 0;
    private double gradLLineE = 0;

    private void refreshAverages(){
        avgHigh.clear();
        avgLow.clear();
        averagesWeights.clear();
        averagesWeightsBasedOnH.clear();
        averagesWeightsBasedOnL.clear();

        SimpleLinearRegressor sAH = new SimpleLinearRegressor();
        SimpleLinearRegressor sAL = new SimpleLinearRegressor();
        SimpleLinearRegressor sEH = new SimpleLinearRegressor();
        SimpleLinearRegressor sEL = new SimpleLinearRegressor();
        for(long t = time + PropertiesHolder.timeQuantum; t <= time + PropertiesHolder.horizonUpperBound; t += PropertiesHolder.timeQuantum){
            double avgHigh = 0;
            double avgLow = 0;

            double avgHDisparity = 0;
            double avgLDisparity = 0;

            double sumW = 0;

            for(MarketMakerPosition advice : advices){
                double tA = advice.getTimeAdvised();
                double tE = advice.getGoodTillTime();
                double tS = tE - tA;

                double w = Math.sqrt(Math.exp(-Math.pow((t - tA)/tS, 2)) * Math.exp(-Math.pow((t - tE)/PropertiesHolder.horizonUpperBound, 2))) * Math.log((Double) advice.attributes.get("cred") + 1);
                sumW += w;

                avgHigh += (w * advice.getHigh1());
                avgLow += (w * advice.getLow1());

                MinMaxAggregator mmAgg = highLowByAdvisedTime.get(advice.getTimeAdvised());
                if(mmAgg.getMax() != null) {
                    avgHDisparity += (w * getLogarithmicDiscretisation(mmAgg.getMax() - advice.getHigh1(), 0, resolution));
                    avgLDisparity += (w * getLogarithmicDiscretisation(advice.getLow1() - mmAgg.getMin(), 0, resolution));
                }

                for(int i = 0; i < w*20; i++) {
                    sAH.aggregate(tA, advice.getHigh1()/resolution);
                    sAL.aggregate(tA, advice.getLow1()/resolution);
                    sEH.aggregate(tE, advice.getHigh1()/resolution);
                    sEL.aggregate(tE, advice.getLow1()/resolution);
                }
            }

            this.avgHigh.put(t, avgHigh/sumW);
            this.avgLow.put(t, avgLow/sumW);
            averagesWeights.put(t, sumW);
            averagesWeightsBasedOnH.put(t, avgHDisparity/sumW);
            averagesWeightsBasedOnL.put(t, avgLDisparity/sumW);
        }
        gradHLineA = sAH.grad;
        gradLLineA = sAL.grad;
        gradHLineE = sEH.grad;
        gradLLineE = sEL.grad;
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
            try {
                data = priceFeed.readNext(this);
                if (data != null) {
                    lastTime = data.getTimeStamp();
                    lastBid = (double) ((Object[]) data.getData())[1];
                    lastAsk = (double) ((Object[]) data.getData())[0];
                    if (lastTime > time + PropertiesHolder.timeQuantum) {
                        break;
                    }

                    onTick(lastTime, lastBid, lastAsk);
                } else {
                    adviceByGoodTillTime.clear();
                    break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public Collection<MarketMakerPosition> getAdvices() {
        return advices;
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
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {}

    @Override
    public void onMessage(IMessage message) throws JFException {
        if (message.getType() == IMessage.Type.ORDER_CLOSE_OK) {
            IOrder order = message.getOrder();
            positions.remove(order.getLabel());

            String info = order.getLabel() + " has closed, PNL: " + order.getProfitLossInUSD() + ", COMMISSION: " + order.getCommissionInUSD() + ", AVAILABLE: " + available;
            LOGGER.info(info);
            emailSendingService.queueEmail("algo@balgobin.london", "hans@balgobin.london","Algo Trade Close", info + " Equity: " + equity);
        } else if (message.getType() == IMessage.Type.ORDER_FILL_OK) {
            IOrder order = message.getOrder();
            LOGGER.info(order.getLabel() + " has been filled at " + order.getOpenPrice() + " for " + order.getOriginalAmount());
        }
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
        available = account.getCreditLine();
        equity = account.getEquity();
    }

    @Override
    public void onStop() throws JFException {}
}
