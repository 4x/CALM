package ai.context.core.neural.messaging.information;

import java.io.Serializable;

public class Impulse  implements Serializable, Sourceable, WithDestination {

    private final String source;
    private final String destination;
    private final String type;
    private final long timeStamp;
    private final Object data;

    public Impulse(String source, String destination, String type, long timeStamp, Object data) {
        this.source = source;
        this.destination = destination;
        this.type = type;
        this.timeStamp = timeStamp;
        this.data = data;
    }

    public String getDestination() {
        return destination;
    }

    public String getSource() {
        return source;
    }

    public String getType() {
        return type;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public Object getData() {
        return data;
    }
}
