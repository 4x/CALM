package ai.context.util.mathematics;

public class Operations {

    public static double roundFloor(double number, int dps){
        double multiplier = Math.pow(10, dps);
        number = number * multiplier;
        number = Math.floor(number);
        number = number/multiplier;

        return number;
    }

    public static double round(double number, int dps){
        double multiplier = Math.pow(10, dps);
        number = number * multiplier;
        number = Math.floor(number + 0.5);
        number = number/multiplier;

        return number;
    }

    public static double sum(double[] values){
        double sum = 0;
        for(double value : values){
            sum += value;
        }
        return sum;
    }

    public static double tanInverse(double x, double y){

        if(x > 0 && y > 0){
            return Math.atan(y/x);
        }

        if(y > 0 && x < 0){
            return Math.PI - Math.atan(y/x);
        }

        if(y < 0 && x < 0){
            return Math.PI + Math.atan(y/x);
        }

        if(x == 0 && y > 0){
            return Math.PI/2;
        }

        if(x == 0 && y < 0){
            return 3*Math.PI/2;
        }

        if(y == 0 && x >= 0){
            return 0;
        }

        if(y == 0 && x < 0){
            return Math.PI;
        }

        return 0;
    }
}
