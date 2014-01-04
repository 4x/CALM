package ai.context.core.neural.neuron;

import ai.context.core.neural.messaging.information.Answer;
import ai.context.core.neural.messaging.information.Impulse;
import ai.context.core.neural.messaging.information.Query;

import java.util.HashMap;

public class Cluster extends Neuron {
    private static volatile Cluster instance = null;
    private final String clusterID = getClusterID();

    private HashMap<String, Neuron> neurons = new HashMap<>();

    private Cluster() {
        super();
        //TODO link with main Brain (assume distributed system)
    }

    @Override
    public void accept(Query query) {

    }

    @Override
    protected void onAnswer(Answer answer) {

    }

    @Override
    protected void onImpulse(Impulse impulse) {

    }

    public static Cluster getInstance() {
        if (instance == null) {
            synchronized (Cluster.class){
                if (instance == null) {
                    instance = new Cluster();
                }
            }
        }
        return instance;
    }

    public synchronized static String getClusterID(){
        return "CLUSTER_" + Math.random() + "-" + Math.random() + "-" + Math.random();
    }

    private long time = 0;

    public long getTime() {
        return time;
    }

    //TODO Message forwarding logic
    //TODO Data feeding logic
    //TODO Neuron lifecycle + wiring based on feed hotness etc...

    public String getId(){
        return clusterID;
    }
}
