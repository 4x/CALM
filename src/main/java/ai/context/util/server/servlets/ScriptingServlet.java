package ai.context.util.server.servlets;

import ai.context.learning.neural.NeuronCluster;
import ai.context.util.configuration.PropertiesHolder;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;

public class ScriptingServlet extends HttpServlet {

    private NeuronCluster cluster = NeuronCluster.getInstance();
    private ScriptEngine jsEngine;
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private PrintStream ps = new PrintStream(baos);
    private final PrintStream oldOut = System.out;
    private final PrintStream oldErr = System.err;

    public ScriptingServlet() {

        System.setOut(ps);
        System.setErr(ps);

        jsEngine = PropertiesHolder.filterFunction.getEngine();
        jsEngine.put("out", System.out);
        jsEngine.put("cluster", cluster);
        jsEngine.put("filterFunction", PropertiesHolder.filterFunction);

        System.setOut(oldOut);
        System.setErr(oldErr);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/text");
        PrintWriter out = resp.getWriter();
        processRequestAndWriteResponse(req, out);
    }

    public static String CMD = "CMD";

    private void processRequestAndWriteResponse(HttpServletRequest req, PrintWriter out) {
        Map<String, String[]> pMap = req.getParameterMap();
        if (pMap.containsKey(CMD)) {
            String cmd = pMap.get(CMD)[0];

            System.setOut(ps);
            System.setErr(ps);
            try {
                jsEngine.eval(cmd);
                ps.flush();
                out.print(baos.toString());
                baos.reset();
            } catch (Exception e) {
                jsEngine.put("error", e);
                out.print("Error! Check 'error'");
            }
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }
}