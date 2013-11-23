package ai.context.core.neural;

import ai.context.core.neural.messaging.Impulse;
import ai.context.core.neural.messaging.Query;
import ai.context.core.neural.messaging.util.RecentUniqueStrings;

public class Axon {
    private RecentUniqueStrings lastQueries = new RecentUniqueStrings(20);
    private Neuron neuronA;
    private Neuron neuronB;

    public synchronized void setEndPoint(Neuron neuron){
        if(neuronA == null){
            neuronA = neuron;
        }
        else if(neuronB == null){
            neuronB = neuron;
        }
    }

    public void transmit(Impulse impulse, Neuron transmitter){
        if(neuronA == transmitter){
            neuronB.accept(impulse);
        }
        else if(neuronB == transmitter){
            neuronA.accept(impulse);
        }
    }

    public void query(Query query, Neuron transmitter){
        if(lastQueries.add(query.getqID())){
            query.decay();
            if(query.getIntensity() < 0.5){
                return;
            }
            if(neuronA == transmitter){
                neuronB.accept(query);
            }
            else if(neuronB == transmitter){
                neuronA.accept(query);
            }
        }
    }
}
