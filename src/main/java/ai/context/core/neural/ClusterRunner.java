package ai.context.core.neural;

import ai.context.core.neural.neuron.Cluster;
import ai.context.util.server.ContextBuilder;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;

public class ClusterRunner extends Server {

    private Cluster cluster;

    public ClusterRunner(int port) {
        super(port);

        ContextHandlerCollection contexts = new ContextHandlerCollection();

        contexts.setHandlers(new Handler[] {new ContextBuilder().buildWebAppContext()});
        setHandler(contexts);

        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //cluster = new Cluster();
    }

    public static void main(String[] args) {
        new ClusterRunner(8080);
    }
}
