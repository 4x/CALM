package ai.context.core.neural;

import ai.context.core.neural.messaging.Answer;
import ai.context.core.neural.messaging.Impulse;
import ai.context.core.neural.messaging.Query;

import java.io.Serializable;
import java.util.HashMap;

public abstract class Neuron  implements Serializable {

    private Cluster cluster = Cluster.getInstance();
    private HashMap<String, Impulse> state = new HashMap<>();

    private HashMap<String, Axon> axons = new HashMap<>();
    private final String id;

    protected Neuron() {
        this.id = getNeuronID();
    }

    public void accept(Impulse impulse) {
        state.put(impulse.getType(), impulse);
        onImpulse();
    }

    public void accept(Query query) {
        //To change body of created methods use File | Settings | File Templates.
    }

    public void accept(Answer answer) {
        //To change body of created methods use File | Settings | File Templates.
    }

    protected void onImpulse(){

    }

    public void connect(Neuron neighbour){
        if(!axons.containsKey(neighbour.getId())){
            Axon axon = new Axon();
            axon.setEndPoint(this);
            axon.setEndPoint(neighbour);
            axons.put(neighbour.getId(), axon);
            neighbour.accept(this, axon);
        }
    }

    public void accept(Neuron neighbour, Axon axon){
        if(!axons.containsKey(neighbour.getId())){
            axons.put(neighbour.getId(), axon);
        }
    }

    public synchronized static String getNeuronID(){
        return "NEURON_" + Math.random() + "-" + Math.random() + "-" + Math.random();
    }

    public String getId(){
        return id;
    }
}
