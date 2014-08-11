package ai.context.trading;

import ai.context.runner.feeding.StateToAction;
import ai.context.runner.feeding.StateToActionSeriesCreator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class TestStateToActionCreation {

    public static void main(String[] args){
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        try {
            long start = format.parse("2007.01.01").getTime();
            long end = format.parse("2007.03.01").getTime();
            double[] preferredMoves = new double[]{0.0005, 0.00075, 0.001, 0.00125, 0.0015, 0.00175, 0.002};
            List<StateToAction> series = StateToActionSeriesCreator.createSeries("/opt/dev/data/", start, end, preferredMoves);

            System.out.println(series.size());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
