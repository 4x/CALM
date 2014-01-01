package ai.context.core.neural.messaging.medium;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ClusterServlet extends WebSocketServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("GET");
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest argO, String arg1) {
        System.out.println("Trying to create web socket");
        return new ClusterWebSocket();
    }
}

class ClusterWebSocket implements WebSocket.OnTextMessage {
    private WebSocket.Connection connection;

    @Override
    public void onMessage(String data) {
        System.out.println(data);
    }

    @Override
    public void onOpen(WebSocket.Connection connection) {
        this.connection = connection;
        System.out.println("Connected");
    }

    @Override
    public void onClose(int closeCode, String message) {
        System.out.println("Closed");
    }
}
