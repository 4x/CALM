package ai.context.core.neural.messaging.information;

import java.io.Serializable;

public class Impulse  implements Serializable {

    private final String type;
    private final long timeStamp;
    private final Object data;

    public Impulse(String type, long timeStamp, Object data) {
        this.type = type;
        this.timeStamp = timeStamp;
        this.data = data;
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
