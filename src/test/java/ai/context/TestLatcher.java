package ai.context;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.surgical.ExtractOneFromListFeed;
import ai.context.util.common.StateActionInformationTracker;
import ai.context.util.mathematics.discretisation.AbsoluteMovementDiscretiser;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

public class TestLatcher {
    @Test
    public void testLatch(){
        List<StateActionInformationTracker> trackers = new LinkedList<>();
        long time = 0;
        long horizon = 300*60000L;

        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = "2012.05.01 00:00:00";

        CSVFeed feed = new CSVFeed("/opt/dev/data/feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feed.setSkipWeekends(true);
        ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
        ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
        ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);

        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        AbsoluteMovementDiscretiser discretiser = new AbsoluteMovementDiscretiser(0.01);
        discretiser.addLayer(0.003, 0.0001);
        discretiser.addLayer(0.005, 0.0005);
        discretiser.addLayer(0.01, 0.001);


        for (int i = 0; i < 700; i++) {

            FeedObject h = feedH.readNext(null);
            FeedObject l = feedL.readNext(null);
            FeedObject c = feedC.readNext(null);

            time = c.getTimeStamp();

            while (!trackers.isEmpty() && trackers.get(0).getTimeStamp() < (time - horizon)) {
                StateActionInformationTracker tracker = trackers.remove(0);

                System.out.println(format.format(time) + "," + tracker.getMaxDown()/0.0001 + "," + tracker.getMaxUp()/0.0001 + "," + (tracker.getLockedTime() - tracker.getStart()));
            }

            for (StateActionInformationTracker tracker : trackers) {
                tracker.processHighAndLow((Double) h.getData(), (Double) c.getData(), time);
            }

            StateActionInformationTracker tracker = new StateActionInformationTracker(time, null, (Double)c.getData(), 0.001, 0);
            tracker.setDiscretisation(discretiser);
            trackers.add(tracker);

        }
    }
}
