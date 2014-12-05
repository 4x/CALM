package ai.context.util.server.servlets;

import ai.context.learning.neural.NeuronCluster;
import ai.context.util.common.ScratchPad;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class LiveTradingServlet  extends HttpServlet {

    private NeuronCluster cluster = NeuronCluster.getInstance();
    public static String CMD = "CMD";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> pMap = req.getParameterMap();
        if (pMap.containsKey(CMD)) {
            String cmd = pMap.get(CMD)[0];

            if(cmd.equals(ScratchPad.CAPTCHA_IMAGE)){
                if(ScratchPad.memory.containsKey(ScratchPad.CAPTCHA_IMAGE)) {
                    BufferedImage image = (BufferedImage) ScratchPad.memory.get(ScratchPad.CAPTCHA_IMAGE);
                    resp.setContentType("image/jpeg");
                    OutputStream out = resp.getOutputStream();

                    ImageIO.write(image, "bmp", out);
                    out.close();
                } else {
                    resp.setContentType("application/text");
                    PrintWriter out = resp.getWriter();
                    out.write("Sorry but there is no captcha at the moment...");
                    out.close();
                }
            } else if(cmd.equals(ScratchPad.CAPTCHA_IMAGE_RESPONSE)){
                if(ScratchPad.memory.containsKey(ScratchPad.CAPTCHA_IMAGE)) {
                    String pin = pMap.get(ScratchPad.CAPTCHA_IMAGE_RESPONSE)[0];
                    ScratchPad.memory.put(ScratchPad.CAPTCHA_IMAGE_RESPONSE, pin);

                    Semaphore lock = (Semaphore) ScratchPad.memory.get(ScratchPad.CAPTCHA_IMAGE_LOCK);
                    lock.release(1);

                    resp.setContentType("application/text");
                    PrintWriter out = resp.getWriter();
                    out.write("PIN accepted, please check logs to see if login was successful...");
                    out.close();
                } else {
                    resp.setContentType("application/text");
                    PrintWriter out = resp.getWriter();
                    out.write("Sorry but there is no captcha at the moment...");
                    out.close();
                }
            }
        }
    }
}
