package ai.context.feed.stitchable;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class StitchableFXRate extends StitchableFeed {

    private SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public StitchableFXRate(String liveFileName, Feed liveFeed) {
        super(liveFileName, liveFeed);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    protected FeedObject formatForCSVFeed(FeedObject data) {
        Object[] parts = (Object[]) data.getData();
        try {
            writer.write(format.format(new Date(data.getTimeStamp())) + "," + parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3] + "," + parts[4] + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Backup file",
                "Live feed"
        };
    }
}
