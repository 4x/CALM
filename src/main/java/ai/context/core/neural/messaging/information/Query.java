package ai.context.core.neural.messaging.information;

import java.io.Serializable;

public class Query implements Serializable, Sourceable {
    private double intensity = 10;
    private final String qID;
    private final String source;

    public Query(String qID, String source){
        this.qID = qID;
        this.source = source;
    }

    public Query replicate() {
        Query query = new Query(qID, source);
        query.setIntensity(intensity);

        return query;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    public void decay(){
        intensity /= 2;
    }

    public double getIntensity() {
        return intensity;
    }

    public String getqID() {
        return qID;
    }

    public String getSource() {
        return source;
    }

    public synchronized static String getQID(){
        return "QUERY_" + Math.random() + "-" + Math.random() + "-" + Math.random();
    }
}
