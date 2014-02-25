package ai.context.core.neural.neuron;

import ai.context.core.neural.messaging.information.*;
import ai.context.core.neural.messaging.medium.ClusterProxy;
import ai.context.core.neural.messaging.medium.ClusterWebSocket;
import ai.context.core.neural.messaging.util.RecentUniqueStrings;

import java.util.HashMap;

public class Cluster extends Neuron {
    private static volatile Cluster instance = null;
    private final String clusterID = getClusterID();
    private ClusterWebSocket clusterWebSocket;
    private RecentUniqueStrings lastQueries = new RecentUniqueStrings(20);

    private HashMap<String, Neuron> neurons = new HashMap<>();

    private Cluster() {
        super();
        //TODO link with main Brain (assume distributed system)
    }

    @Override
    protected void onQuery(Query query) {
        if(lastQueries.add(query.getqID())){
            Query toSend = query.replicate();
            toSend.decay();

            if(toSend.getIntensity() < 0.5){
                return;
            }
            forwardAll(toSend);
        }
    }

    @Override
    protected void onAnswer(Answer answer) {
        forward(answer);
    }

    @Override
    protected void onImpulse(Impulse impulse) {
        forward(impulse);
    }

    public void setClusterWebSocket(ClusterWebSocket clusterWebSocket) {
        this.clusterWebSocket = clusterWebSocket;
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

    private void forward(WithDestination data){
        ClusterProxy proxy = clusterWebSocket.getProxy(data.getDestination());
        if(proxy != null){
            proxy.receive(data);
        }
        else{
            Neuron receiver = neurons.get(data.getDestination());
            if(receiver != null){
                if(data instanceof Impulse){
                    receiver.accept((Impulse) data);
                }
                else if(data instanceof Answer){
                    receiver.accept((Answer) data);
                }
                else if(data instanceof Query){
                    receiver.accept((Query)data);
                }
            }
        }
    }

    private void forwardAll(Query query){
        for(ClusterProxy proxy : clusterWebSocket.getProxies()){
            proxy.receive(query);
        }
    }
}
