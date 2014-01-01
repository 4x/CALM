package ai.context.util.server;

import ai.context.core.neural.messaging.medium.ClusterServlet;
import org.mortbay.jetty.webapp.WebAppContext;

public class ContextBuilder {

    public static WebAppContext buildWebAppContext(){
        WebAppContext context = new WebAppContext();
        context.setResourceBase(".");
        context.setDescriptor(context.getResourceBase() + "src/main/WEB-INF/web.xml");
        context.setContextPath("/");

        context.addServlet(ClusterServlet.class, "cluster");

        return context;
    }
}
