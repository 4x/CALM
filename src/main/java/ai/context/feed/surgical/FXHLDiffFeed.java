package ai.context.feed.surgical;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.List;


public class FXHLDiffFeed extends AbstractSurgicalFeed{

    private Feed rawFeed;
    public FXHLDiffFeed(Feed rawFeed) {
        super(rawFeed);
        this.rawFeed = rawFeed;
    }

    @Override
    protected synchronized FeedObject operate(long time, List row) {
        return new FeedObject(time, ((Double)row.get(1) - (Double)row.get(2)));
    }

    @Override
    public Feed getCopy() {
        return new FXHLDiffFeed(rawFeed.getCopy());
    }
}
