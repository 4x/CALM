package ai.context.util.mathematics;

public class MinMaxDiscretiser {

    private MinMaxAggregator aggregator;
    private long count = 0;

    private Double min;
    private Double max;

    public long criticalMass = 10000;
    public int clusters = 5;

    public MinMaxDiscretiser(long criticalMass, int clusters) {
        this.criticalMass = criticalMass;
        this.clusters = clusters;
        this.count = criticalMass;
    }

    public int discretise(Double value)
    {
        if(value == null)
        {
            return -1;
        }
        else if(count == criticalMass){
            if(aggregator != null){
                min = aggregator.getMin();
                max = aggregator.getMax();
            }
            aggregator = new MinMaxAggregator();
            count = 0;
        }
        aggregator.addValue(value);
        count++;

        if(min != null && max != null) {
            double position = (value - min)/(max - min);
            return (int) (position*clusters);
        }
        return -1;
    }
}
