package ai.context.feed;

public interface Feed<T> {
    public boolean hasNext();
    public FeedObject<T> readNext(Object caller);
    public Feed getCopy();

    public long getLatestTime();
    public void addChild(Feed feed);

    public String getDescription(int startIndex, String padding);
}
