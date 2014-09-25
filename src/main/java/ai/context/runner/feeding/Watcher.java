package ai.context.runner.feeding;

public class Watcher {
    public final long horizon;
    public final long tEnd;
    public final StateToAction listener;
    public final double initialAsk;
    public final double initialBid;

    private double high;
    private double low;

    private int dir = 0;
    private boolean closed = false;

    public Watcher(long horizon, long tEnd, StateToAction listener, double initialAsk, double initialBid) {
        this.horizon = horizon;
        this.tEnd = tEnd;
        this.listener = listener;
        this.initialAsk = initialAsk;
        this.initialBid = initialBid;

        this.high = initialBid;
        this.low = initialAsk;
    }

    public boolean addPoint(long timeStamp, double ask, double bid){

        if(closed){
            return true;
        }

        if(timeStamp > tEnd){
            listener.horizonActions.put(horizon, new Double[]{high - initialAsk, initialAsk - low});
            closed = true;
            return true;
        }

        if(ask < low){
            low = ask;
        }

        if(bid > high){
            high = bid;
        }
        return false;
    }
}
