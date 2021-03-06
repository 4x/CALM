package ai.context.util.mathematics;

public class Latcher {
    private final double open;
    private final long start;
    private final double quantum;

    private double maxUp = 0;
    private double maxDown = 0;

    private double high = Double.MIN_VALUE;
    private double low = Double.MAX_VALUE;
    private long end;

    private long lockedTime = Long.MAX_VALUE;

    public Latcher(double open, long start, double quantum) {
        this.open = open;
        this.start = start;
        this.quantum = quantum;
    }

    public void registerBounds(double high, double low, long time){
        if(time < lockedTime){
            double deltaUp = high - open;
            double deltaDown = open - low;

            double tmpMaxUp = maxUp;
            double tmpMaxDown = maxDown;

            if(deltaUp > maxUp && this.high - low < quantum){
                tmpMaxUp = deltaUp;
                end = time;
            }
            else if(maxUp > quantum && this.high - low >= quantum){
                lockedTime = time;
                return;
            }

            if(deltaDown > maxDown && high - this.low < quantum){
                tmpMaxDown = deltaDown;
                end = time;
            }
            else if(maxDown > quantum && high - this.low >= quantum){
                lockedTime = time;
                return;
            }

            maxUp = tmpMaxUp;
            maxDown = tmpMaxDown;
            this.high = Math.max(this.high, high);
            this.low = Math.min(this.low, low);
        }
    }


    public void registerHigh(double val, long time){
        if(time <= lockedTime){
            double delta = val - open;
            if(delta > maxUp){
                maxUp = delta;
                end = time;
            }
            else if(maxUp > quantum && maxUp - delta > quantum && delta/maxUp < 0.5){
                lockedTime = time;
            }
        }
    }

    public void registerLow(double val, long time){
        if(time <= lockedTime){
            double delta = open - val;
            if(delta > maxDown){
                maxDown = delta;
                end = time;
            }
            else if(maxDown > quantum && maxDown - delta > quantum && delta/maxDown < 0.5){
                lockedTime = time;
            }
        }
    }

    public double getMaxUp() {
        return maxUp;
    }

    public double getMaxDown() {
        return -maxDown;
    }

    public double getMid(){
        return (maxUp - maxDown)/2;
    }

    public long getEnd() {
        return end;
    }
}
