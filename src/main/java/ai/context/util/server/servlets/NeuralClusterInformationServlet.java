package ai.context.util.server.servlets;

import ai.context.learning.neural.NeuronCluster;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class NeuralClusterInformationServlet extends HttpServlet{

    private NeuronCluster cluster = NeuronCluster.getInstance();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);

        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
    }
}
