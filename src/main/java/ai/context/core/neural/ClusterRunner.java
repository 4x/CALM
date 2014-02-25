package ai.context.core.neural;

import ai.context.core.neural.neuron.Cluster;
import ai.context.util.server.ContextBuilder;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

public class ClusterRunner extends Server {

    private Cluster cluster;

    public ClusterRunner(int port) {
        super(port);

        ContextHandlerCollection contexts = new ContextHandlerCollection();

        contexts.setHandlers(new Handler[] {new ContextBuilder().buildWebAppContext()});
        setHandler(new ContextBuilder().buildWebAppContext());

        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        cluster = Cluster.getInstance();
    }

    public static void main(String[] args) {
        new ClusterRunner(6112);
    }
}
