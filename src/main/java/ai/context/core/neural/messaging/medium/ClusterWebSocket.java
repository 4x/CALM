package ai.context.core.neural.messaging.medium;

import ai.context.core.neural.messaging.information.SessionAuth;
import ai.context.core.neural.messaging.information.SessionAuthResponse;
import ai.context.core.neural.neuron.Cluster;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.io.IOException;
import java.util.HashMap;

public class ClusterWebSocket implements WebSocketListener {

    private HashMap<String, ClusterProxy> neuronToCluster = new HashMap<>();
    private HashMap<String, ClusterProxy> clusterIdToProxy = new HashMap<>();
    private String sessionAuth;
    private Session session;
    private Cluster cluster = Cluster.getInstance();

    @Override
    public void onWebSocketBinary(byte[] bytes, int i, int i2) {
        System.out.println("1");
    }

    @Override
    public void onWebSocketClose(int i, String s) {
        System.out.println("2");
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.sessionAuth = "SESSION_" + Math.random() + "-" + Math.random() + "-" + Math.random();
        this.session = session;
        try {
            session.getRemote().sendString(JsonWriter.objectToJson(sessionAuth));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("3");
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        System.out.println("4");
    }

    @Override
    public void onWebSocketText(String s) {
        try {
            Object data = JsonReader.jsonToJava(s);

            if(data instanceof SessionAuth){
                session.getRemote().sendString(JsonWriter.objectToJson(new SessionAuthResponse(((SessionAuth)data).getAuth(), cluster.getId())));
            }
            else if(data instanceof SessionAuthResponse){
                SessionAuthResponse response = (SessionAuthResponse) data;
                if(response.getAuthID().equals(sessionAuth)){
                    if(!clusterIdToProxy.containsKey(response.getClusterID())){
                        clusterIdToProxy.put(response.getClusterID(), new ClusterProxy(response.getClusterID()));
                    }
                    clusterIdToProxy.get(response.getClusterID()).setSession(session);
                    clusterIdToProxy.get(response.getClusterID()).setLive(true);
                }
            }
            else {

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}