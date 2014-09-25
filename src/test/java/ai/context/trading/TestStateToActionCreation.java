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
            List<StateToAction> series = StateToActionSeriesCreator.createSeries("/opt/dev/data/", start, end, 10);

            System.out.println(series.size());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
