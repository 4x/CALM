package ai.context.feed;

import java.util.Date;

import static ai.context.util.learning.AmalgamateUtils.getStringForObject;

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

    @Override
    public String toString() {
        return "FeedObject{" +
                "timeStamp=" + new Date(timeStamp) +
                ", data=" + getStringForObject(data) +
                '}';
    }
}
