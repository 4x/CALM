package ai.context.feed.transformer.single;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimeVariablesAppenderFeed implements Feed {

    private Feed rawFeed;
    private long timeStamp;

    public TimeVariablesAppenderFeed(Feed rawFeed) {
        this.rawFeed = rawFeed;
    }

    @Override
    public boolean hasNext() {
        return rawFeed.hasNext();
    }

    @Override
    public synchronized FeedObject readNext(Object caller) {

        FeedObject data = rawFeed.readNext(this);
        timeStamp = data.getTimeStamp();
        Date date = new Date(data.getTimeStamp());
        int day = date.getDay();
        int dayOfMonth = date.getDate();
        int month = date.getMonth();
        int hour = date.getHours();
        int min = date.getMinutes();

        return new FeedObject(data.getTimeStamp(), new Object[]{day, dayOfMonth, month, hour, min, data.getData()});
    }

    @Override
    public Feed getCopy() {
        return new TimeVariablesAppenderFeed(rawFeed.getCopy());
    }

    @Override
    public void addChild(Feed feed) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeChild(Feed feed) {

    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        String description = padding + "[" + startIndex + "] Time series appender with  [" + startIndex + "] Day of week" + "\n";
        startIndex++;
        description += padding + "  [" + startIndex + "] day of month" + "\n";
        startIndex++;
        description += padding + "  [" + startIndex + "] month of year" + "\n";
        startIndex++;
        description += padding + "  [" + startIndex + "] hour of day" + "\n";
        startIndex++;
        description += padding + "  [" + startIndex + "] minute of hour" + "\n";
        startIndex++;
        description += rawFeed.getDescription(startIndex, padding + " ") + "\n";

        return description;
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        if (element < 5) {
            list.add(this);
        } else {
            list.add(rawFeed.getElementChain(element - 5));
        }
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 5 + rawFeed.getNumberOfOutputs();
    }
}
