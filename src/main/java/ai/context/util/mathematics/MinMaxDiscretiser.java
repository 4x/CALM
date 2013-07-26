package ai.context.util.mathematics;

public class MinMaxDiscretiser {

    private MinMaxAggregator aggregator;
    private long count = 0;

    private Double min;
    private Double max;

    public long criticalMass = 10000;
    public int clusters = 5;

    private boolean lockable = false;
    private boolean locked = false;

    public MinMaxDiscretiser(long criticalMass, int clusters) {
        this.criticalMass = criticalMass;
        this.clusters = clusters;
        this.count = criticalMass;
    }

    public int discretise(Double value)
    {
        if(!locked){
            if(value == null)
            {
                return -1;
            }
            else if(count == criticalMass){
                if(aggregator != null){
                    min = aggregator.getMin();
                    max = aggregator.getMax();
                    if(lockable){
                        locked = true;
                    }
                }

                if(!locked){
                    aggregator = new MinMaxAggregator();
                    count = 0;
                }
            }
            aggregator.addValue(value);
            count++;
        }

        if(min != null && max != null) {
            double position = (value - min)/(max - min);
            return (int) (position*clusters);
        }
        return -1;
    }

    public void setLockable(boolean lockable) {
        this.lockable = lockable;
    }
}
