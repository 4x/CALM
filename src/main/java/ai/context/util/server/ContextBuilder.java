package ai.context.util.server;

import org.eclipse.jetty.webapp.WebAppContext;

public class ContextBuilder {

    public static WebAppContext buildWebAppContext() {
        WebAppContext context = new WebAppContext();
        context.setResourceBase(".");
        context.setDescriptor(context.getResourceBase() + "src/main/WEB-INF/web.xml");
        context.setContextPath("/");
        return context;
    }
}
