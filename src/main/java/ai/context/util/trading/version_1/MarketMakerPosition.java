package ai.context.util.trading.version_1;

import ai.context.util.mathematics.Operations;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class MarketMakerPosition {
    private long time;
    private final double targetHigh;
    private final double targetLow;
    private final double high1;
    private final double low1;
    private long goodTill;
    private long lastTime = 0;

    private double high = 0;
    private double low = Double.MAX_VALUE;
    private double close;
    private String closingMessage;

    private boolean hasOpenedWithLong = false;
    private boolean hasOpenedWithShort = false;
    private boolean closed = false;
    private double open;
    private String orderId;

    private Set<String> flags = new HashSet<>();

    public TreeMap<String, Object> attributes = new TreeMap<>();

    public MarketMakerPosition(long time, double targetHigh, double targetLow, double high1, double low1, long goodTill) {
        this.time = time;
        this.targetHigh = targetHigh;
        this.targetLow = targetLow;
        this.goodTill = goodTill;
        this.high1 = high1;
        this.low1 = low1;

        this.attributes.put("timeSpan", (goodTill - time));
    }

    public boolean notify(double high, double low, double close, long time){
        this.lastTime = time;
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

        this.attributes.put("dir", hasOpenedWithLong ? 1 : 0);
    }

    public boolean isHasOpenedWithShort() {
        return hasOpenedWithShort;
    }

    public void setHasOpenedWithShort(boolean hasOpenedWithShort, double open) {
        this.hasOpenedWithShort = hasOpenedWithShort;
        this.open = open;

        this.attributes.put("dir", hasOpenedWithShort ? -1 : 0);
    }

    public boolean isOpen(){
        return hasOpenedWithLong || hasOpenedWithShort;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
        OrderIntelligenceEngine.getInstance().feed(this);
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

    public long getTimeSpan(){
        return (goodTill - time);
    }

    public void addFlag(String flag){
        flags.add(flag);
    }

    public void removeFlag(String flag){
        flags.remove(flag);
    }

    public boolean containsFlag(String flag){
        return flags.contains(flag);
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }
}

