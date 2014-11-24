package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.List;

public interface ISynchFeed extends Feed {
    public FeedObject getNextComposite(Object caller);
    public void addRawFeed(Feed feed);

    public List<? extends Feed> rawFeeds();
    public Feed getRawFeed();

    public List<FeedObject> getLatest(int n);
}
