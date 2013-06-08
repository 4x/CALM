package ai.context.util.mathematics;

public class Operations {

    public static double roundFloor(double number, int dps){
        double multiplier = Math.pow(10, dps);
        number = number * multiplier;
        number = Math.floor(number);
        number = number/multiplier;

        return number;
    }
}
