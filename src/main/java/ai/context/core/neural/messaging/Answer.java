package ai.context.core.neural.messaging;

import java.io.Serializable;

public class Answer implements Serializable{

    private double confidence = 0;
    private final String qID;

    public Answer(String qID) {
        this.qID = qID;
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
