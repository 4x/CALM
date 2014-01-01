package ai.context.core.neural.neuron;

import ai.context.core.neural.messaging.medium.Axon;
import ai.context.core.neural.messaging.information.Answer;
import ai.context.core.neural.messaging.information.Impulse;
import ai.context.core.neural.messaging.information.Query;
import ai.context.core.neural.messaging.util.AnswerCollector;

import java.io.Serializable;
import java.util.HashMap;

public abstract class Neuron  implements Serializable {

    private Cluster cluster = Cluster.getInstance();
    private HashMap<String, Impulse> state = new HashMap<>();
    private AnswerCollector answerCollector = new AnswerCollector();

    private HashMap<String, Axon> axons = new HashMap<>();
    private final String id;

    //TODO Neuron importance
    //TODO Neuron correlations
    //TODO Neuron keywords

    protected Neuron() {
        this.id = getNeuronID();
    }

    public void accept(Impulse impulse) {
        state.put(impulse.getType(), impulse);
        onImpulse(impulse);
    }

    public abstract void accept(Query query);

    public void accept(Answer answer){
        answerCollector.addAnswer(answer);
        onAnswer(answer);
    }

    protected abstract void onAnswer(Answer answer);

    protected abstract void onImpulse(Impulse impulse);

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
