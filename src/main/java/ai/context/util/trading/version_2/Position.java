package ai.context.util.trading.version_2;

public class Position {
    public final long expiry;
    public final long start;
    public final double initialLevel;
    public final int direction;
    public final double stopLoss;
    public final double takeProfit;

    private double close;
    private boolean closed = false;

    public Position(long expiry, long start, double initialLevel, int direction, double stopLoss, double takeProfit) {
        this.expiry = expiry;
        this.start = start;
        this.initialLevel = initialLevel;
        this.direction = direction;
        this.stopLoss = stopLoss;
        this.takeProfit = takeProfit;
    }

    public boolean notifyQuote(long time, double bid, double ask){
        if(closed){
            return true;
        }

        if(time >= expiry){
            closed = true;
            return true;
        }

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
}
