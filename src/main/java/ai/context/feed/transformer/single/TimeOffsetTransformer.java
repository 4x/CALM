package ai.context.feed.transformer.single;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TimeOffsetTransformer implements Feed {

    private Feed rawFeed;
    private long timeStamp;
    private int offset;

    private boolean future = true;

    private LinkedList<FeedObject> buffer = new LinkedList<>();

    public TimeOffsetTransformer(Feed rawFeed, int offset) {
        this.rawFeed = rawFeed;
        this.offset = Math.abs(offset);

        if (offset < 0) {
            future = false;
        }
    }

    @Override
    public boolean hasNext() {
        return rawFeed.hasNext();
    }

    @Override
    public synchronized FeedObject readNext(Object caller) {

        FeedObject data = rawFeed.readNext(this);
        buffer.add(data);
        timeStamp = data.getTimeStamp();

        if (buffer.size() > offset) {
            FeedObject toReturn = buffer.pollFirst();
            if (!future) {
                return new FeedObject(timeStamp, toReturn.getData());
            } else {
                return new FeedObject(toReturn.getTimeStamp(), data.getData());
            }
        }

        return new FeedObject(0, null);
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
        String description = padding + "[" + startIndex + "] Future Offset Transformer with offset: " + offset + "\n";
        startIndex++;
        description += rawFeed.getDescription(startIndex, padding + " ") + "\n";

        return description;
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);

        list.add(rawFeed.getElementChain(element));

        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return rawFeed.getNumberOfOutputs();
    }
}
