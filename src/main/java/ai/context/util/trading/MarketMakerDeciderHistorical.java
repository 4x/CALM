package ai.context.util.trading;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.Operations;
import ai.context.util.measurement.MarketMakerPosition;

import java.text.SimpleDateFormat;
import java.util.*;

public class MarketMakerDeciderHistorical implements OnTickDecider{

    private TreeMap<Long, Set<MarketMakerPosition>> adviceByGoodTillTime = new TreeMap<>();
    private TreeMap<Long, Set<MarketMakerPosition>> specialPositions = new TreeMap<>();
    private double pnl = 0;
    private double pnlSpecial = 0;
    private long time = 0;

    private double[] lastMids = new double[200];
    private CSVFeed priceFeed;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");


    public MarketMakerDeciderHistorical(String priceFeedFile, String startDate){
        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        priceFeed = new CSVFeed(priceFeedFile, "yyyy.MM.dd HH:mm:ss.SSS", typesPrice, startDate);
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
                        System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnl, 5) + " LONG TIMEOUT " + advice.getTimeSpan());
                        advice.setClosed(true);
                    }
                    else {
                        double change = (advice.getOpen() - ask);
                        pnl += change;
                        String state = "P";
                        if(change <= 0){
                            state = "L";
                        }
                        System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnl, 5) + " SHORT TIMEOUT " + advice.getTimeSpan());
                        advice.setClosed(true);
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
                            System.out.println("SPECIAL " + format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + position.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnlSpecial, 5) + " LONG TIMEOUT " + position.getTimeSpan());
                            position.setClosed(true);
                        }
                        else {
                            double change = (position.getOpen() - ask);
                            pnlSpecial += change;
                            String state = "P";
                            if(change <= 0){
                                state = "L";
                            }
                            System.out.println("SPECIAL " + format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + position.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnlSpecial, 5) + " SHORT TIMEOUT " + position.getTimeSpan());
                            position.setClosed(true);
                        }
                    }
                }
            }
        }


        for(Set<MarketMakerPosition> advices : adviceByGoodTillTime.values()){
            double avgHigh = 0;
            double avgLow = 0;

            Map<Long, Double> mU = new TreeMap<>();
            Map<Long, Double> mD = new TreeMap<>();
            for(MarketMakerPosition advice : advices){
                avgHigh += advice.getHigh1();
                avgLow += advice.getLow1();

                mU.put(advice.getTimeSpan(), advice.getHigh1());
                mD.put(advice.getTimeSpan(), advice.getLow1());
            }
            avgHigh /= advices.size();
            avgLow /= advices.size();

            for(MarketMakerPosition advice : advices){
                if(!advice.isOpen() && advice.getTimeAdvised() <= time){

                    if(bid - PropertiesHolder.marketMakerBeyond > advice.getTargetHigh()){
                        advice.addFlag("A");
                    }
                    else if(advice.containsFlag("A")
                            && bid > advice.getTargetHigh()
                            && bid - PropertiesHolder.marketMakerBeyond/2 < advice.getTargetHigh()
                            && advice.getTargetLow() > avgLow
                            && avgHigh - bid < (bid - advice.getTargetLow())/2
                            && avgHigh - bid < PropertiesHolder.maxLeewayAmplitude
                            && PropertiesHolder.filterFunction.pass(advice)){
                        advice.setHasOpenedWithShort(true, bid);
                    }
                    else if(ask + PropertiesHolder.marketMakerBeyond < advice.getTargetLow()){
                        advice.addFlag("B");
                    }
                    else if(advice.containsFlag("B")
                            && ask < advice.getTargetLow()
                            && ask + PropertiesHolder.marketMakerBeyond/2 > advice.getTargetLow()
                            && advice.getTargetHigh() < avgHigh
                            && ask - avgLow < (advice.getTargetHigh() - ask)/2
                            && ask - avgLow < PropertiesHolder.maxLeewayAmplitude
                            && PropertiesHolder.filterFunction.pass(advice)){
                        advice.setHasOpenedWithLong(true, ask);
                    }

                    if(advice.isOpen()){
                        advice.attributes.put("d1", d1);
                        advice.attributes.put("d5", d5);
                        advice.attributes.put("d10", d10);
                        advice.attributes.put("d20", d20);
                        advice.attributes.put("d50", d50);
                        advice.attributes.put("d100", d100);
                        advice.attributes.put("d199", d199);

                        advice.attributes.put("wait", time - advice.getTimeAdvised());

                        for(Map.Entry<Long, Double> entry : mU.entrySet()){
                            advice.attributes.put("u_" + entry.getKey(), entry.getValue() - advice.getOpen());
                        }

                        for(Map.Entry<Long, Double> entry : mD.entrySet()){
                            advice.attributes.put("d_" + entry.getKey(), advice.getOpen() - entry.getValue());
                        }

                        advice.attributes.put("targetHigh", advice.getTargetHigh() - advice.getOpen());
                        advice.attributes.put("targetLow", advice.getOpen() - advice.getTargetLow());

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
                                    position.setHasOpenedWithShort(true, advice.getOpen());
                                } else{
                                    position.setHasOpenedWithLong(true, advice.getOpen());
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
                            System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnl, 5) + " LONG NORMAL " + advice.getTimeSpan() );
                            advice.setClosed(true);
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
                            System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnl, 5) + " SHORT NORMAL " + advice.getTimeSpan());
                            advice.setClosed(true);
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
                                System.out.println("SPECIAL: " + format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + position.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnlSpecial, 5) + " LONG NORMAL " + position.getTimeSpan() );
                                position.setClosed(true);
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
                                System.out.println("SPECIAL: " + format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + position.getOpen() + " CLOSE: " + ask + " NET: " + Operations.round(pnlSpecial, 5) + " SHORT NORMAL " + position.getTimeSpan());
                                position.setClosed(true);
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
        time = Math.max(time, advice.getTimeAdvised());

        if(!adviceByGoodTillTime.containsKey(advice.getGoodTillTime())){
            adviceByGoodTillTime.put(advice.getGoodTillTime(), new HashSet<MarketMakerPosition>());
        }
        adviceByGoodTillTime.get(advice.getGoodTillTime()).add(advice);
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
        else if(lastTime <= time + DecisionAggregatorA.getTimeQuantum()){
            onTick(lastTime, lastBid, lastAsk);
        }

        while(lastTime <= time + DecisionAggregatorA.getTimeQuantum()){
            data = priceFeed.readNext(this);
            if(data != null){
                lastTime = data.getTimeStamp();
                lastBid = (double) ((Object[])data.getData())[1];
                lastAsk = (double) ((Object[])data.getData())[0];
                if(lastTime > time + DecisionAggregatorA.getTimeQuantum()){
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
}
