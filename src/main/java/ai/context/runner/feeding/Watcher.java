package ai.context.runner.feeding;

public class Watcher {
    public final long horizon;
    public final long tEnd;
    public final StateToAction listener;
    public final double movement;
    public final double initialAsk;
    public final double initialBid;

    private double high;
    private double low;

    private int dir = 0;
    private boolean closed = false;

    public Watcher(long horizon, long tEnd, StateToAction listener, double movement, double initialAsk, double initialBid) {
        this.horizon = horizon;
        this.tEnd = tEnd;
        this.listener = listener;
        this.movement = movement;
        this.initialAsk = initialAsk;
        this.initialBid = initialBid;

        this.high = initialBid;
        this.low = initialAsk;
    }

    public boolean addPoint(double timeStamp, double ask, double bid){
        if(timeStamp > tEnd){
            if(high - initialAsk > movement){
                listener.horizonActions.put(horizon, high - initialAsk);
            }
            else if(initialBid - low > movement){
                listener.horizonActions.put(horizon, low - initialBid);
            }
            else {
                listener.horizonActions.put(horizon, 0.0);
            }
            return true;
        }

        if(closed){
            return true;
        }

        if(ask < low){
            low = ask;
        }

        if(bid > high){
            high = bid;
        }

        if(dir == 0){
            if(high - initialAsk > movement){
                dir = 1;
            }

            if(initialBid - low > movement){
                dir = -1;
            }
        }
        else {
            if(dir == 1 && initialBid - low > movement){
                listener.horizonActions.put(horizon, high - initialAsk);
                closed = true;
                return true;
            }
            else if(high - initialAsk > movement){
                listener.horizonActions.put(horizon, low - initialBid);
                closed = true;
                return true;
            }
        }
        return false;
    }
}