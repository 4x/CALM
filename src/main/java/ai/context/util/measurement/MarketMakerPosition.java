package ai.context.util.measurement;

import ai.context.util.mathematics.Operations;

import java.util.Date;

public class MarketMakerPosition {
    private final long time;
    private final double targetHigh;
    private final double targetLow;
    private final long goodTill;

    private double high = 0;
    private double low = Double.MAX_VALUE;
    private double close;
    private String closingMessage;

    public MarketMakerPosition(long time, double targetHigh, double targetLow, long goodTill) {
        this.time = time;
        this.targetHigh = Operations.round(targetHigh, 4);
        this.targetLow = Operations.round(targetLow, 4);
        this.goodTill = goodTill;
    }

    public boolean notify(double high, double low, double close, long time){
        if(time <= goodTill){
            this.high = Operations.round(Math.max(this.high, high), 5);
            this.low = Operations.round(Math.min(this.low, low), 5);
            this.close = close;
        }

        if(time >= goodTill){
            return true;
        }
        return false;
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
        return closingMessage + " ["+ low + " - " + high + "]"  + "["+ targetLow + " - " + targetHigh + "] TARGET_PNL: " + Operations.round(targetHigh - targetLow, 4) + " LIFE_SPAN: " + (goodTill - time) + " " +(new Date(time));
    }
}

