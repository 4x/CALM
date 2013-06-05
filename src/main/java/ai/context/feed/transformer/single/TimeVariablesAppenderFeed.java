package ai.context.feed.transformer.single;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.Date;

public class TimeVariablesAppenderFeed implements Feed {

    private Feed rawFeed;
    public TimeVariablesAppenderFeed(Feed rawFeed)
    {
        this.rawFeed = rawFeed;
    }
    @Override
    public boolean hasNext() {
        return rawFeed.hasNext();
    }

    @Override
    public FeedObject readNext(Object caller) {

        FeedObject data = rawFeed.readNext(this);

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
}
