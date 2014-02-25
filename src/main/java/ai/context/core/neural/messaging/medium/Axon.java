package ai.context.core.neural.messaging.medium;

import ai.context.core.neural.messaging.information.Impulse;
import ai.context.core.neural.messaging.information.Query;
import ai.context.core.neural.messaging.util.RecentUniqueStrings;
import ai.context.core.neural.neuron.Neuron;

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
            Query toSend = query.replicate();
            toSend.decay();
            if(toSend.getIntensity() < 0.5){
                return;
            }
            if(neuronA == transmitter){
                neuronB.accept(toSend);
            }
            else if(neuronB == transmitter){
                neuronA.accept(query);
            }
        }
    }
}
