package ai.context.feed.row;

import ai.context.feed.Feed;

public abstract class RowFeed implements Feed {

    public abstract RowFeed getCopy();
    public abstract void close();
}
