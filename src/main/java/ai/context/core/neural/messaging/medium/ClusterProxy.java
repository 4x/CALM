package ai.context.core.neural.messaging.medium;

import ai.context.core.neural.messaging.information.*;
import ai.context.core.neural.neuron.Cluster;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClusterProxy {
    private final String id;
    private final Cluster cluster = Cluster.getInstance();
    private boolean live = false;
    private boolean isClient = true;
    private Session session;
    private WebSocket.Connection connection;
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
        isClient = false;
    }

    public void connectClient(String address){
        WebSocketClientFactory factory = new WebSocketClientFactory();
        WebSocketClient client = factory.newWebSocketClient();

        try {
            connection = client.open(new URI(address), new WebSocket.OnTextMessage() {
                @Override
                public void onOpen(Connection connection) {
                    live = true;
                }

                @Override
                public void onClose(int closeCode, String message) {
                    live = false;
                }

                @Override
                public void onMessage(String json) {
                    try {
                        Object data = JsonReader.jsonToJava(json);
                        if(data instanceof SessionAuth){
                            connection.sendMessage(JsonWriter.objectToJson(new SessionAuthResponse(((SessionAuth)data).getAuth(), cluster.getId())));
                        }
                        else if(data instanceof Sourceable){
                            Sourceable sourceable = (Sourceable) data;
                            thisProxy.send(sourceable);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 10000, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void receive(Object data){
        try {
            String json = JsonWriter.objectToJson(data);
            if(isClient){
                connection.sendMessage(json);
            }
            else{
                session.getRemote().sendString(json);
            }
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
