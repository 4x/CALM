package ai.context.feed;

public class FeedObject<T> {
    private final long timeStamp;
    private final T data;

    public FeedObject(long timeStamp, T data) {
        this.timeStamp = timeStamp;
        this.data = data;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public T getData() {
        return data;
    }
}
