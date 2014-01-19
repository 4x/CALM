package ai.context.core.neural.messaging.medium;

import ai.context.core.neural.messaging.information.*;
import ai.context.core.neural.neuron.Cluster;
import com.cedarsoftware.util.io.JsonWriter;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;

public class ClusterProxy {
    private final String id;
    private final Cluster cluster = Cluster.getInstance();
    private boolean live = false;
    private Session session;
    private ClusterProxy thisProxy = this;

    //TODO info

    public ClusterProxy(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void connectClient(String address){
        WebSocketClient client = new WebSocketClient();

        try {
            client.start();
            URI echoUri = new URI(address);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(new ClusterClientSocket(this), echoUri, request);
            System.out.printf("Connecting to : %s%n", echoUri);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receive(Object data){
        try {
            String json = JsonWriter.objectToJson(data);
            session.getRemote().sendString(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(Object data){
        if(data instanceof Impulse){
            cluster.accept((Impulse) data);
        }
        else if(data instanceof Answer){
            cluster.accept((Answer) data);
        }
        else if(data instanceof Query){
            cluster.accept((Query) data);
        }
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }
}
