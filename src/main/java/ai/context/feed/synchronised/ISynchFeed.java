package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

public interface ISynchFeed extends Feed{
    public FeedObject getNextComposite(Object caller);
}
