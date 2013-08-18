package ai.context.feed.surgical;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.List;

public class ExtractOneFromListFeed extends AbstractSurgicalFeed {

    private Feed rawFeed;
    private int interestedIndex;

    public ExtractOneFromListFeed(Feed rawFeed, int interestedIndex) {
        super(rawFeed);
        this.rawFeed = rawFeed;
        this.interestedIndex = interestedIndex;
    }

    @Override
    protected synchronized FeedObject operate(long time, List row) {
        if(row.size() <= interestedIndex)
        {
            return new FeedObject(time, null);
        }
        return new FeedObject(time, row.get(interestedIndex));
    }

    @Override
    public Feed getCopy() {
        return new ExtractOneFromListFeed(rawFeed.getCopy(), interestedIndex);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Extracting element: " + interestedIndex + " from Feed: " + rawFeed.getDescription(startIndex, padding);
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Parent Feed",
                "Element to extract from parent output"
        };
    }

    @Override
    public List getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(rawFeed.getElementChain(interestedIndex));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
