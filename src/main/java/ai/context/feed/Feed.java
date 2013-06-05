package ai.context.feed;

public interface Feed<T> {
    public boolean hasNext();
    public FeedObject<T> readNext(Object caller);
    public Feed getCopy();

    public void addChild(Feed feed);
}
