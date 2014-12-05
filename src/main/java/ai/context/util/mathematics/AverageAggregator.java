package ai.context.util.mathematics;

public class AverageAggregator {
    private double sum = 0;
    private double count = 0;

    public AverageAggregator() {
    }

    public AverageAggregator(double sum, double count) {
        this.sum = sum;
        this.count = count;
    }

    public void addPoint(double point){
        sum += point;
        count++;
    }

    public void adjust(double sumDelta, double countDelta){
        sum += sumDelta;
        count += countDelta;
    }

    public double getAverage(){
        return sum/count;
    }

    public double getSum() {
        return sum;
    }

    public double getCount() {
        return count;
    }

    public AverageAggregator getComposite(AverageAggregator partner){
        return new AverageAggregator(sum + partner.getSum(), count + partner.getCount());
    }
}
