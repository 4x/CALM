package ai.context.util;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.Servlet;

public class SpringContextHelper {

    private ApplicationContext context;

    public SpringContextHelper(Servlet servlet) {
        context = WebApplicationContextUtils.getRequiredWebApplicationContext(servlet.getServletConfig().getServletContext());
    }

    public Object getBean(String beanRef) {
        return context.getBean(beanRef);
    }
}
