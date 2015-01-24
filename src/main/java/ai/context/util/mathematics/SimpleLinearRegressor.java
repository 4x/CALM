package ai.context.util.mathematics;

public class SimpleLinearRegressor {
    public double grad = 0;
    public double intercept = 0;

    private double xySum = 0;
    private double xSum = 0;
    private double n = 0;
    private double ySum = 0;
    private double x2Sum = 0;

    public void aggregate(double x, double y){
        xySum += (x * y);
        xSum += x;
        ySum += y;
        x2Sum += (x * x);
        n++;

        grad = (xySum - (xSum * ySum)/n)/(x2Sum - (xSum * xSum)/n);
        intercept = (ySum/n) - grad*(xSum/n);
    }
}
