package ai.context.util.trading.version_2;

import java.util.Arrays;
import java.util.Date;

public class Position {
    public final long expiry;
    public final long start;
    public final double initialLevel;
    public final int direction;
    public final double stopLoss;
    public final double takeProfit;

    private double close;
    private boolean closed = false;
    private boolean timeOut = false;
    private String report = "";
    private String firstQuoteTime = null;
    private int quotesSeen = 0;
    private Object[] info;

    public Position(long expiry, long start, double initialLevel, int direction, double stopLoss, double takeProfit, Object[] info) {
        this.expiry = expiry;
        this.start = start;
        this.initialLevel = initialLevel;
        this.close = initialLevel;
        this.direction = direction;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
        this.info = info;

        report = new Date(start) + " -> " + new Date(expiry);
    }

    public boolean notifyQuote(long time, double bid, double ask){
        if(firstQuoteTime == null){
            firstQuoteTime = "" + new Date(time);
        }
        if(closed){
            return true;
        }

        if(time >= expiry){
            timeOut = true;
            closed = true;
            return true;
        }
        quotesSeen++;

        if(direction > 0){
            if(bid - initialLevel >= takeProfit){
                close = initialLevel + takeProfit;
                closed = true;
                return true;
            }
            else if(initialLevel - bid >= stopLoss){
                close = initialLevel - stopLoss;
                closed = true;
                return true;
            }
            else {
                close = bid;
                return false;
            }
        }
        else {
            if(initialLevel - ask >= takeProfit){
                close = initialLevel - takeProfit;
                closed = true;
                return true;
            }
            else if(ask - initialLevel >= stopLoss){
                close = initialLevel + stopLoss;
                closed = true;
                return true;
            }
            else {
                close = ask;
                return false;
            }
        }
    }

    public double getPnL(){
        return direction * (close - initialLevel);
    }

    public long getHorizon(){
        return expiry - start;
    }

    public boolean isTimeOut() {
        return timeOut;
    }

    public double getAmplitude(){
        return Math.abs(takeProfit);
    }

    public String getReport(){
        return report + " " + firstQuoteTime;
    }

    public int getQuotesSeen() {
        return quotesSeen;
    }

    public String getInfo() {
        return Arrays.toString(info);
    }
}
