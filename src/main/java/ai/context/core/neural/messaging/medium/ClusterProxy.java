package ai.context.core.neural.messaging.medium;

import ai.context.core.neural.messaging.information.Answer;
import ai.context.core.neural.messaging.information.Impulse;
import ai.context.core.neural.messaging.information.Query;
import ai.context.core.neural.neuron.Cluster;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;

public class ClusterProxy {
    private final String id;
    private final Cluster cluster = Cluster.getInstance();
    private boolean live = false;
    private Session session;

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

    public void receive(Object data){
        try {
            String json = JsonWriter.objectToJson(data);
            session.getRemote().sendString(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String json){
        try {
            Object data = JsonReader.jsonToJava(json);
            if(data instanceof Impulse){
                cluster.accept((Impulse) data);
            }
            else if(data instanceof Answer){
                cluster.accept((Answer) data);
            }
            else if(data instanceof Query){
                cluster.accept((Query) data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }
}
