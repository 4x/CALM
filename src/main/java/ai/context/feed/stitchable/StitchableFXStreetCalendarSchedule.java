package ai.context.feed.stitchable;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class StitchableFXStreetCalendarSchedule extends StitchableFeed{

    private SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

    public StitchableFXStreetCalendarSchedule(String liveFileName, Feed liveFeed) {
        super(liveFileName, liveFeed);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    protected FeedObject formatForCSVFeed(FeedObject data) {
        Object[] parts = (Object[]) data.getData();
        try {
            writer.write(format.format(new Date(data.getTimeStamp())) + "," + parts[0] + "," + parts[1] + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
