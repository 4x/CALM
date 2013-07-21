package ai.context.util.analysis;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.TreeMap;

public class DecisionHistogram extends JPanel{

    private Graphics2D g2d;
    private int w;
    private int h;

    private TreeMap<Double, Double> sFreq = new TreeMap<>();
    private TreeMap<Double, Double> lFreq = new TreeMap<>();

    private double minTakeProfit = 0;
    private double ratio = 2;
    private double decision = 0;

    public DecisionHistogram(){
        JFrame frame = new JFrame("Decision Histogram");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(this);
        frame.setSize(800, 420);
        frame.setVisible(true);
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);

        g2d = (Graphics2D) g;
        g2d.setColor(Color.BLUE);

        Dimension size = getSize();
        Insets insets = getInsets();

        w = size.width - insets.left - insets.right;
        h = size.height - insets.bottom - insets.top;

        g2d.clearRect(0, 0, w, h);

        if(!sFreq.isEmpty()){
            double maxX = sFreq.lastKey();
            double maxY = 0;

            for(double y : sFreq.values()){
                if (y > maxY){
                    maxY = y;
                }
            }

            for(double y : lFreq.values()){
                if (y > maxY){
                    maxY = y;
                }
            }

            double lastX = 0;
            double lastYS = 0;
            double lastYL = 0;

            for (double x : sFreq.keySet()){
                double yS = sFreq.get(x);
                double yL = lFreq.get(x);

                g2d.setColor(Color.BLUE);
                g2d.draw(new Line2D.Double(w*(lastX/maxX), h - (lastYL/maxY)*h, w * (x/maxX), h - (yL/maxY)*h));

                g2d.setColor(Color.RED);
                g2d.draw(new Line2D.Double(w*(lastX/maxX), h - (lastYS/maxY)*h, w * (x/maxX), h - (yS/maxY)*h));

                if(x > minTakeProfit){
                    if(yS/yL > ratio){
                        g2d.setColor(new Color(255, 0, 0, 50));
                        g2d.fillRect((int)(w*(lastX/maxX)), 0, (int)(w * (x - lastX)/maxX), h);
                    }
                    else if(yL/yS > ratio){
                        g2d.setColor(new Color(0, 0, 255, 50));
                        g2d.fillRect((int)(w*(lastX/maxX)), 0, (int)(w * (x - lastX)/maxX), h);
                    }
                }

                lastX = x;
                lastYL = yL;
                lastYS = yS;
            }

            g2d.setColor(Color.GREEN);
            g2d.draw(new Line2D.Double(w*(minTakeProfit/maxX), 0, w*(minTakeProfit/maxX), h));

            g2d.setColor(Color.BLACK);
            g2d.draw(new Line2D.Double(w*(decision/maxX), 0, w*(decision/maxX), h));
        }
    }

    public void update(TreeMap<Double, Double> sFreq, TreeMap<Double, Double> lFreq, double minTakeProfit, double ratio, double decision){
        this.sFreq = sFreq;
        this.lFreq = lFreq;
        this.minTakeProfit = minTakeProfit;
        this.ratio = ratio;
        this.decision = decision;

        this.repaint();
    }
}
