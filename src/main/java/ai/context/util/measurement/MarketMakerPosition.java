package ai.context.util.measurement;

import ai.context.util.mathematics.Operations;

import java.util.Date;

public class MarketMakerPosition {
    private long time;
    private final double targetHigh;
    private final double targetLow;
    private final double high1;
    private final double low1;
    private long goodTill;

    private double high = 0;
    private double low = Double.MAX_VALUE;
    private double close;
    private String closingMessage;

    private boolean hasOpenedWithLong = false;
    private boolean hasOpenedWithShort = false;
    private boolean closed = false;
    private double open;
    private String orderId;

    public MarketMakerPosition(long time, double targetHigh, double targetLow, double high1, double low1, long goodTill) {
        this.time = time;
        this.targetHigh = targetHigh;
        this.targetLow = targetLow;
        this.goodTill = goodTill;
        this.high1 = high1;
        this.low1 = low1;
    }

    public boolean notify(double high, double low, double close, long time){
        if(time <= goodTill){
            this.high = Math.max(this.high, high);
            this.low = Math.min(this.low, low);
            this.close = close;
        }

        if(time >= goodTill){
            return true;
        }
        return false;
    }

    public boolean isHasOpenedWithLong() {
        return hasOpenedWithLong;
    }

    public void setHasOpenedWithLong(boolean hasOpenedWithLong, double open) {
        this.hasOpenedWithLong = hasOpenedWithLong;
        this.open = open;
    }

    public boolean isHasOpenedWithShort() {
        return hasOpenedWithShort;
    }

    public void setHasOpenedWithShort(boolean hasOpenedWithShort, double open) {
        this.hasOpenedWithShort = hasOpenedWithShort;
        this.open = open;
    }

    public boolean isOpen(){
        return hasOpenedWithLong || hasOpenedWithShort;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public double getOpen() {
        return open;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public double getPnL(){
        if(high >= targetHigh && low <= targetLow){
            closingMessage = "L||H";
            return (targetHigh - targetLow);
        }
        else if(high < targetHigh && low > targetLow){
            closingMessage = "|LH|";
            return 0;
        }
        else if(high >= targetHigh){
            closingMessage = "|L|H";
            return targetHigh - close;
        }
        else {
            closingMessage = "L|H|";
            return close - targetLow;
        }
    }

    public String getClosingMessage() {
        return closingMessage
                + " ["+ Operations.round(low, 5)
                + " - " + Operations.round(high, 5)
                + "]"  + "["+ Operations.round(targetLow, 5)
                + " - " + Operations.round(targetHigh, 5)
                + "] TARGET_PNL: " + Operations.round(targetHigh - targetLow, 5)
                + " LIFE_SPAN: " + (goodTill - time)
                + " " +(new Date(time));
    }

    public Long getGoodTillTime() {
        return goodTill;
    }

    public double getTargetHigh() {
        return targetHigh;
    }

    public double getTargetLow() {
        return targetLow;
    }

    public double getHigh1() {
        return high1;
    }

    public double getLow1() {
        return low1;
    }

    public void adjustTimes(long timeQuantum) {
        time += timeQuantum;
        goodTill += timeQuantum;
    }

    public long getTimeAdvised() {
        return time;
    }
}

