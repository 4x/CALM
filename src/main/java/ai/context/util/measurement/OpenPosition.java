package ai.context.util.measurement;

import java.util.Date;

public class OpenPosition {

    private long timeOpen;
    private double target;
    private double start;
    private double takeProfit;
    private double stopLoss;

    private boolean isLong;
    private double multiplier = 1;

    private double price;

    private String closingMessage;

    public OpenPosition(long timeOpen, double start, double targetProfit, double targetLoss, boolean isLong) {
        this.timeOpen = timeOpen;
        this.start = start;
        this.price = start;
        this.takeProfit = start + targetProfit;
        this.stopLoss = start - targetLoss;
        this.isLong = isLong;
        this.target = targetProfit;

        if(!isLong)
        {
            multiplier = -1;
        }

        if(isLong){
            System.out.println(new Date(timeOpen) + ": Opening LONG position open at " + start + " TP: " + takeProfit + ", SL: " + stopLoss);
        }
        else{
            System.out.println(new Date(timeOpen) + ": Opening SHORT position open at " + start + " TP: " + takeProfit + ", SL: " + stopLoss);
        }
    }

    public boolean canClose(long time, double price)
    {
        this.price = price;
        if(isLong && (price >= takeProfit || price <= stopLoss))
        {
            return true;
        }
        else if(price >= stopLoss || price <= takeProfit){
            return true;
        }
        return false;
    }

    public boolean canCloseOnBar_Pessimistic(long time, double high, double low)
    {
        if(isLong)
        {
            if(low <= stopLoss)
            {
                price = stopLoss;
                closingMessage = new Date(time) + ": LOSS: Closing [LONG] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high +"}";
                return true;
            }

            if(high >= takeProfit)
            {
                price = takeProfit;
                closingMessage = new Date(time) + ": PROFIT: Closing [LONG] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high +"}";
                return true;
            }
        }
        else{
            if(high >= stopLoss)
            {
                price = stopLoss;
                closingMessage = new Date(time) + ": LOSS: Closing [SHORT] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high +"}";
                return true;
            }

            if(low <= takeProfit)
            {
                price = takeProfit;
                closingMessage = new Date(time) + ": PROFIT: Closing [SHORT] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high +"}";
                return true;
            }
        }
        return false;
    }

    public String getClosingMessage() {
        return closingMessage;
    }

    public double getPnL()
    {
        return multiplier * (price - start);
    }

    public double getTarget()
    {
        return target;
    }

    public long getTimeOpen() {
        return timeOpen;
    }
}
