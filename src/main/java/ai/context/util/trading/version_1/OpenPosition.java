package ai.context.util.trading.version_1;

import ai.context.util.mathematics.Operations;
import com.dukascopy.api.IOrder;

import java.util.Date;

public class OpenPosition {

    private IOrder order;
    private boolean goodTillClosed = false;
    private double amount = 1.0;
    private double cost = 0.0;
    private long timeOpen;
    private double target;
    private double start;
    private double takeProfit;
    private double stopLoss;

    private boolean isLong;
    private double multiplier = 1;

    private double price;

    private String closingMessage;

    private long goodTillTime;
    private long timeSpan;
    private double credibility = 0;

    private int participants = 0;

    public OpenPosition(long timeOpen, double start, double targetProfit, double targetLoss, boolean isLong, long goodTillTime, boolean goodTillClosed) {
        this.timeOpen = timeOpen;
        this.start = start;
        this.price = start;
        this.takeProfit = start + targetProfit;
        this.stopLoss = start - targetLoss;
        this.isLong = isLong;
        this.target = Operations.round(Math.abs(targetProfit), 5);
        this.goodTillTime = goodTillTime;
        this.goodTillClosed = goodTillClosed;
        this.timeSpan = goodTillTime - timeOpen;

        if (!isLong) {
            multiplier = -1;
        }
    }

    public boolean canClose(long time, double price) {
        this.price = price;
        if (isLong && (price >= takeProfit || price <= stopLoss)) {
            return true;
        } else if (price >= stopLoss || price <= takeProfit) {
            return true;
        }
        return false;
    }

    public boolean canCloseOnBar_Pessimistic(long time, double high, double low, double close) {
        if (!goodTillClosed && time >= goodTillTime) {
            price = close;

            if (isLong) {
                if (price > start) {
                    closingMessage = new Date(time) + ": PROFIT: [LONG] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " TIMEOUT";
                } else {
                    closingMessage = new Date(time) + ": LOSS: [LONG] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " TIMEOUT";
                }
            } else {
                if (price < start) {
                    closingMessage = new Date(time) + ": PROFIT: [SHORT] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " TIMEOUT";
                } else {
                    closingMessage = new Date(time) + ": LOSS: [SHORT] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " TIMEOUT";
                }
            }
            return true;
        }

        if (isLong) {
            if (low <= stopLoss) {
                price = stopLoss;
                closingMessage = new Date(time) + ": LOSS: [LONG] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " NORMAL";
                return true;
            }

            if (high >= takeProfit) {
                price = takeProfit;
                closingMessage = new Date(time) + ": PROFIT: [LONG] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " NORMAL";
                return true;
            }
        } else {
            if (high >= stopLoss) {
                price = stopLoss;
                closingMessage = new Date(time) + ": LOSS: [SHORT] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " NORMAL";
                return true;
            }

            if (low <= takeProfit) {
                price = takeProfit;
                closingMessage = new Date(time) + ": PROFIT: [SHORT] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " NORMAL";
                return true;
            }
        }

        if((double)(time - timeOpen)/(double)(goodTillTime - timeOpen) > 0.8){
            if (isLong) {
                if (close > (start + 5*cost)) {
                    price = close;
                    closingMessage = new Date(time) + ": PROFIT: [LONG] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " LOCKING_PROFIT";
                    return true;
                }
            } else {
                if (close < (start - 5*cost)) {
                    price = close;
                    closingMessage = new Date(time) + ": PROFIT: [SHORT] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " LOCKING_PROFIT";
                    return true;
                }
            }
        }

        return false;
    }

    public void close(double price, long time){
        this.price = price;
        if (isLong) {
            if (price > start) {
                closingMessage = new Date(time) + ": PROFIT: [LONG] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " FORCED";
            } else {
                closingMessage = new Date(time) + ": LOSS: [LONG] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " FORCED";
            }
        } else {
            if (price < start) {
                closingMessage = new Date(time) + ": PROFIT: [SHORT] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " FORCED";
            } else {
                closingMessage = new Date(time) + ": LOSS: [SHORT] " + (time - timeOpen)/1000 + "s " + target + " " + timeSpan + " FORCED";
            }
        }
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getClosingMessage() {
        return closingMessage;
    }

    public double getPnL() {
        return amount * ((multiplier * (price - start)) - cost);
    }

    public double getClosingAmount() {
        return amount + getPnL();
    }

    public double getAbsolutePNL(){
        return (multiplier * (price - start)) - cost;
    }

    public double getTarget() {
        return target;
    }

    public long getTimeOpen() {
        return timeOpen;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isLong() {
        return isLong;
    }

    public double getStart() {
        return start;
    }

    public double getTakeProfit() {
        return takeProfit;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public long getGoodTillTime() {
        return goodTillTime;
    }

    public long getTimeSpan() {
        return timeSpan;
    }

    public double getCredibility() {
        return credibility;
    }

    public void setCredibility(double credibility) {
        this.credibility = credibility;
    }

    public IOrder getOrder() {
        return order;
    }

    public void setOrder(IOrder order) {
        this.order = order;
    }

    public int getParticipants() {
        return participants;
    }

    public void setParticipants(int participants) {
        this.participants = participants;
    }
}
