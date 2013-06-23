package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

public class SynchronisedFeed extends SynchronisableFeed{

    private Feed rawFeed;
    public SynchronisedFeed(Feed rawFeed, SynchronisableFeed sibling) {
        super(sibling);
        this.rawFeed = rawFeed;
        rawFeed.addChild(this);
    }

    @Override
    public boolean hasNext() {
        return rawFeed.hasNext();
    }

    @Override
    public FeedObject readNext(Object caller) {
        return rawFeed.readNext(this);
    }

    @Override
    public Feed getCopy() {
        SynchronisedFeed copy = null;

        for(Feed feed : feeds)
        {
            copy = new SynchronisedFeed(feed.getCopy(), copy);
        }
        return copy;
    }

    public Feed getRawFeed() {
        return rawFeed;
    }
}
