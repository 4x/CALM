package ai.context.core.neural.messaging.medium;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ai.context.core.neural.messaging.information.SessionAuth;
import ai.context.core.neural.messaging.information.SessionAuthResponse;
import ai.context.core.neural.messaging.information.Sourceable;
import ai.context.core.neural.neuron.Cluster;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class ClusterClientSocket {

    private Cluster cluster = Cluster.getInstance();
    private ClusterProxy proxy;
    private final CountDownLatch closeLatch;
    private Session session;

    public ClusterClientSocket(ClusterProxy proxy) {
        this.proxy = proxy;
        this.closeLatch = new CountDownLatch(1);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.printf("Got connect: %s%n", session);
        this.session = session;
        proxy.setSession(session);
        proxy.setLive(true);
    }

    @OnWebSocketMessage
    public void onMessage(String json) {
        try {
            Object data = JsonReader.jsonToJava(json);
            if(data instanceof SessionAuth){
                session.getRemote().sendString(JsonWriter.objectToJson(new SessionAuthResponse(((SessionAuth) data).getAuth(), cluster.getId())));
            }
            else if(data instanceof Sourceable){
                Sourceable sourceable = (Sourceable) data;
                proxy.send(sourceable);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}