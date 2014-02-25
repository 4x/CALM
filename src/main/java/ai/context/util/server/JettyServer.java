package ai.context.util.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.http.HttpServlet;

public class JettyServer extends Server{

    private WebAppContext webapp = new WebAppContext();
    public JettyServer(int port){
        super(port);

        webapp.setContextPath("/");
        webapp.setWar("/");
        webapp.setServer(this);
        this.setHandler(webapp);
    }

    public void addServlet(Class<? extends HttpServlet> servletClass, String subPath){
        webapp.addServlet(servletClass, "/" + subPath);
    }
}
