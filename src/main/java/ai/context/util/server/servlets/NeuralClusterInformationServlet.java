package ai.context.util.server.servlets;

import ai.context.learning.neural.NeuronCluster;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class NeuralClusterInformationServlet extends HttpServlet {

    private NeuronCluster cluster = NeuronCluster.getInstance();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        processRequestAndWriteResponse(req, out);
    }

    public static String REQ_TYPE = "REQ_TYPE";
    public static String GET_ALL_INFO = "ALL";
    public static String GET_STATS_INFO = "STATS";
    public static String GET_CURRENT_PRED = "PRED";

    private void processRequestAndWriteResponse(HttpServletRequest req, PrintWriter out) {
        Map<String, String[]> pMap = req.getParameterMap();
        if (pMap.containsKey(REQ_TYPE)) {
            String reqType = pMap.get(REQ_TYPE)[0];

            if (reqType.equals(GET_ALL_INFO)) {
                out.print(cluster.getClusterStateJSON());
            } else if (reqType.equals(GET_STATS_INFO)) {
                out.print(cluster.getStats());
            } else if (reqType.startsWith(GET_CURRENT_PRED)) {
                out.print(cluster.getPred(Integer.parseInt(reqType.split(":")[1])));
            }
        }
    }
}
