package ai.context.core.neural.messaging;

import java.io.Serializable;

public class Query implements Serializable {
    private double intensity = 10;
    private final String qID;

    public Query(){
        this.qID = getQID();
    }

    public Query(String qID){
        this.qID = qID;
    }

    public Query replicate() {
        Query query = new Query(qID);
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

    public synchronized static String getQID(){
        return "QUERY_" + Math.random() + "-" + Math.random() + "-" + Math.random();
    }
}
