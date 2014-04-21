package ai.context.util.measurement;

import java.util.Date;

public class OpenPosition {

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

    public OpenPosition(long timeOpen, double start, double targetProfit, double targetLoss, boolean isLong, long goodTillTime, boolean goodTillClosed) {
        this.timeOpen = timeOpen;
        this.start = start;
        this.price = start;
        this.takeProfit = start + targetProfit;
        this.stopLoss = start - targetLoss;
        this.isLong = isLong;
        this.target = targetProfit;
        this.goodTillTime = goodTillTime;
        this.goodTillClosed = goodTillClosed;

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
                    closingMessage = new Date(time) + ": PROFIT: Closing [LONG] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high + "} TIMEOUT";
                } else {
                    closingMessage = new Date(time) + ": LOSS: Closing [LONG] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high + "} TIMEOUT";
                }
            } else {
                if (price < start) {
                    closingMessage = new Date(time) + ": PROFIT: Closing [SHORT] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high + "} TIMEOUT";
                } else {
                    closingMessage = new Date(time) + ": LOSS: Closing [SHORT] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high + "} TIMEOUT";
                }
            }
            return true;
        }

        if (isLong) {
            if (low <= stopLoss) {
                price = stopLoss;
                closingMessage = new Date(time) + ": LOSS: Closing [LONG] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high + "}";
                return true;
            }

            if (high >= takeProfit) {
                price = takeProfit;
                closingMessage = new Date(time) + ": PROFIT: Closing [LONG] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high + "}";
                return true;
            }
        } else {
            if (high >= stopLoss) {
                price = stopLoss;
                closingMessage = new Date(time) + ": LOSS: Closing [SHORT] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high + "}";
                return true;
            }

            if (low <= takeProfit) {
                price = takeProfit;
                closingMessage = new Date(time) + ": PROFIT: Closing [SHORT] position open at " + new Date(timeOpen) + " at " + start + " for " + price + " {" + low + " - " + high + "}";
                return true;
            }
        }
        return false;
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
}
