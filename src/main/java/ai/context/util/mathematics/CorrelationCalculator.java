package ai.context.util.mathematics;

public class CorrelationCalculator {

    private double currentCorrelation = 0;

    private long nPoints = 0;

    private double x_sum = 0;
    private double y_sum = 0;

    private double x_y_sum = 0;

    private double x_2_sum = 0;
    private double y_2_sum = 0;

    public double getCorrelationCoefficient(double x, double y){
        nPoints++;

        x_sum += x;
        y_sum += y;

        x_y_sum += (x * y);

        x_2_sum += (x * x);
        y_2_sum += (y * y);

        if(nPoints < 3)
        {
            return 0;
        }

        currentCorrelation = (x_y_sum - (x_sum * y_sum)/nPoints) / (Math.sqrt((x_2_sum - (x_sum * x_sum)/nPoints) * (y_2_sum - (y_sum * y_sum)/nPoints)));
        return currentCorrelation;
    }

    public double getCurrentCorrelation()
    {
        return currentCorrelation;
    }
}
