package ai.context.core.neural.messaging.information;

import java.io.Serializable;

public class Answer implements Serializable, Sourceable{

    private final String source;
    private final String destination;
    private double confidence = 0;
    private final String qID;

    public Answer(String source, String destination, String qID) {
        this.source = source;
        this.destination = destination;
        this.qID = qID;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getqID() {
        return qID;
    }
}
