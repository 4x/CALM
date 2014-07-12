package ai.context.util.trading;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.mathematics.Operations;
import ai.context.util.measurement.MarketMakerPosition;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class MarketMakerDeciderHistorical implements OnTickDecider{

    private TreeMap<Long, Set<MarketMakerPosition>> adviceByGoodTillTime = new TreeMap<>();
    private double pnl = 0;
    private long time = 0;

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
        //System.out.println("T: " + new Date(time) + " B: " + bid + " A: " + ask);
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
                        System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + ask + "  NET: " + Operations.round(pnl, 5) + " SHORT TIMEOUT " + advice.getTimeSpan());
                        advice.setClosed(true);
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
                if(!advice.isOpen() && advice.getTimeAdvised() <= time){

                    if(bid - PropertiesHolder.marketMakerBeyond > advice.getTargetHigh()){
                        advice.addFlag("A");
                    }
                    else if(advice.containsFlag("A")
                            && bid > advice.getTargetHigh()
                            && bid - PropertiesHolder.marketMakerBeyond/2 < advice.getTargetHigh()
                            && advice.getTargetLow() > avgLow){
                        advice.setHasOpenedWithShort(true, bid);
                        //System.out.println(format.format(new Date(time)) + " Selling @ " + bid);
                    }
                    else if(ask + PropertiesHolder.marketMakerBeyond < advice.getTargetLow()){
                        advice.addFlag("B");
                    }
                    else if(advice.containsFlag("B")
                            && ask < advice.getTargetLow()
                            && ask + PropertiesHolder.marketMakerBeyond/2 > advice.getTargetLow()
                            && advice.getTargetHigh() < avgHigh){
                        advice.setHasOpenedWithLong(true, ask);
                        //System.out.println(format.format(new Date(time)) + " Buying @ " + ask);
                    }
                }
                else if(!advice.isClosed()){
                    if(advice.isHasOpenedWithLong() && (advice.getTargetHigh() <= bid || advice.getOpen() >= bid + PropertiesHolder.marketMakerStopLoss)){
                        double change = (bid - advice.getOpen());
                        pnl += change;
                        String state = "P";
                        if(change <= 0){
                            state = "L";
                        }
                        System.out.println(format.format(new Date(time)) + " " + state + ": " + Operations.round(change, 5) + " OPEN: " + advice.getOpen() + " CLOSE: " + bid + " NET: " + Operations.round(pnl, 5) + " LONG NORMAL " + advice.getTimeSpan() );
                        advice.setClosed(true);
                    }
                    else if(advice.isHasOpenedWithShort() && (advice.getTargetLow() >= ask || advice.getOpen() <= ask - PropertiesHolder.marketMakerStopLoss)){
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
        else if(lastTime <= time + DecisionAggregator.getTimeQuantum()){
            onTick(lastTime, lastBid, lastAsk);
        }

        while(lastTime <= time + DecisionAggregator.getTimeQuantum()){
            data = priceFeed.readNext(this);
            if(data != null){
                lastTime = data.getTimeStamp();
                lastBid = (double) ((Object[])data.getData())[1];
                lastAsk = (double) ((Object[])data.getData())[0];
                if(lastTime > time + DecisionAggregator.getTimeQuantum()){
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
