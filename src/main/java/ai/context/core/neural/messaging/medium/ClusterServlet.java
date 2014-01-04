package ai.context.core.neural.messaging.medium;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ClusterServlet extends WebSocketServlet {


    public ClusterServlet(){
        System.out.println("Cluster Servlet Started");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("GET");
        response.getWriter().write("Test!");
    }

    @Override
    public void configure(WebSocketServletFactory factory){
        // set a 10 second idle timeout
        factory.getPolicy().setIdleTimeout(10000);
        // register my socket
        factory.register(ClusterWebSocket.class);
    }
}
