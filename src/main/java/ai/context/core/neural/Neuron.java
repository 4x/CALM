package ai.context.core.neural;

import ai.context.core.neural.messaging.Answer;
import ai.context.core.neural.messaging.Impulse;
import ai.context.core.neural.messaging.Query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class Neuron  implements Serializable {

    private Cluster cluster = Cluster.getInstance();
    private HashMap<String, Impulse> state = new HashMap<>();

    private Set<Axon> axons = new HashSet<>();

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
}
