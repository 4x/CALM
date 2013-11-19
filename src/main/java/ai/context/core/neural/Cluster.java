package ai.context.core.neural;

public class Cluster extends Neuron {
    private static volatile Cluster instance = null;

    private Cluster() {
        //TODO link with main Brain (assume distributed system)
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

    private long time = 0;

    public long getTime() {
        return time;
    }

    //TODO Message forwarding logic
}
