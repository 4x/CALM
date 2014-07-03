package ai.context.util.trading;

public interface OnTickDecider {
    public void onTick(long time, double bid, double ask) throws Exception;
}
