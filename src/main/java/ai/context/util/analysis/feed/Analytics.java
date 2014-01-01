package ai.context.util.analysis.feed;

import ai.context.core.ai.StateActionPair;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class Analytics extends JPanel{

    private Graphics2D g2d;
    private int w;
    private int h;

    private Workspace workspace;
    private StateActionPair strategy;

    public Analytics(Workspace workspace) {
        setLayout(null);
        this.workspace = workspace;
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);

        g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(1));


        Dimension size = getSize();
        Insets insets = getInsets();

        w = size.width - insets.left - insets.right;
        h = size.height - insets.bottom - insets.top;

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, w, h);
        g2d.setColor(Color.WHITE);


        g2d.setFont(new Font("Monospaced", Font.BOLD, 20));
        g2d.drawString("Strategy:", 50, 70);

        g2d.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2d.drawString("Top Factors:", 50, 110);

        g2d.drawString("Factor Index", 50, 130);
        g2d.drawString("Value", 180, 130);
        g2d.drawString("Influence", 280, 130);

        if(strategy != null){
            g2d.drawString("" + strategy.getId(), 250, 70);

            Map<Double, Integer> factors = workspace.getLearner().getFactorInfluences(strategy);
            int i = 0;
            for(Map.Entry<Double, Integer> entry : factors.entrySet()){
                i++;
                if(i == 10){
                    break;
                }

                g2d.setFont(new Font("Monospaced", Font.BOLD, 10));
                g2d.drawString(entry.getValue().toString(), 50, 140 + (i * 12));

                g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
                g2d.drawString(strategy.getAmalgamate()[entry.getValue()] + "", 180, 140 + (i * 12));

                int width = (int) ((w - 330) * entry.getKey());
                g2d.fillRect(280,  135 + (i * 12), width, 5);
                g2d.drawString(entry.getKey().toString(), 290 + width, 140 + (i * 12));

            }
        }

        g2d.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2d.drawString("Possibility Diagram:", 50, 300);

        g2d.setStroke(new BasicStroke(5));
        int height = h - (320 + 50);
        int width = w - (50 + 50);
        g2d.drawLine(50, 320 + height, 50 + width, 320 + height);

        if(strategy != null){
            g2d.setStroke(new BasicStroke(3));
            g2d.setFont(new Font("Monospaced", Font.BOLD, 10));

            double maxY = 0;
            double actionRes = strategy.getActionResolution();
            int minX = strategy.getActionDistribution().firstKey();
            int maxX = strategy.getActionDistribution().lastKey();

            g2d.drawString((actionRes * minX) + "", 50, 320 + height + 15);
            g2d.drawString((actionRes * maxX) + "", 50 + width, 320 + height + 15);

            if(minX * maxX < 0){
                int zeroPos = ((0 - minX) * (width))/(maxX - minX) + 50;
                g2d.drawLine(zeroPos, 320, zeroPos, 320 + height);
            }
            else{
                g2d.drawLine(50, 320, 50, 320 + height);
            }

            for(Map.Entry<Integer, Double> entry : strategy.getActionDistribution().entrySet()){
                if(entry.getValue() > maxY){
                    maxY = entry.getValue();
                }
            }

            for(Map.Entry<Integer, Double> entry : strategy.getActionDistribution().entrySet()){
                if(entry.getKey() < 0){
                    Color color = new Color(1F, 0F, 0F, 0.5F);
                    g2d.setColor(color);
                }
                else {
                    Color color = new Color(0F, 0F, 1F, 0.5F);
                    g2d.setColor(color);
                }

                int xPos = ((entry.getKey() - minX) * (width))/(maxX - minX) + 50;
                int y = (int) ((entry.getValue() / maxY) * height);

                g2d.drawLine(xPos,  320 + height, xPos,  320 + height - y);
            }
        }
    }

    public void update(StateActionPair strategy){
        this.strategy = strategy;
        repaint();
    }
}
