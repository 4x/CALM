package ai.context.util.analysis;

import javax.swing.*;
import java.awt.*;
import java.util.TreeMap;
import java.util.TreeSet;

public class SuccessMap extends JPanel{

    private Graphics2D g2d;
    private int w;
    private int h;

    private TreeSet<Integer> ySet;
    private TreeMap<Integer, TreeMap<Integer, Double>> successMap;

    public SuccessMap(TreeSet<Integer> ySet, TreeMap<Integer, TreeMap<Integer, Double>> successMap)
    {
        this.ySet = ySet;
        this.successMap = successMap;

        JFrame frame = new JFrame("Success Map");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.setSize(400, 420);
        frame.setVisible(true);
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);

        g2d = (Graphics2D) g;
        g2d.setColor(Color.blue);

        Dimension size = getSize();
        Insets insets = getInsets();

        w = size.width - insets.left - insets.right;
        h = size.height - insets.top - insets.bottom;

        int yMin = ySet.first();
        int yMax = ySet.last();

        int xMin = successMap.firstKey();
        int xMax = successMap.lastKey();

        double maxIntensity = 0;

        for(TreeMap<Integer, Double> subMap : successMap.values())
        {
            for(Double intensity : subMap.values())
            {
                if(maxIntensity < intensity)
                {
                    maxIntensity = intensity;
                }
            }
        }

        g2d.setColor(Color.black);
        g2d.fillRect(0, 0, w, h);

        for(int x : successMap.keySet())
        {
            TreeMap<Integer, Double> subMap = successMap.get(x);
            for(int y : subMap.keySet())
            {
                double intensity = subMap.get(y);

                Color color = new Color(1F, 1F, 1F, (float)(intensity / maxIntensity));
                g2d.setColor(color);
                g2d.fillRect(w * (x - xMin) / (xMax - xMin), h - (h * (y - yMin) / (yMax - yMin)), 2 , 2);
            }
        }
        Color color = new Color(1F, 0.5F, 0.5F, 0.5F);
        g2d.setColor(color);
        g2d.fillRect(w * (0 - xMin) / (xMax - xMin), 0, 1 , h);
        g2d.fillRect(0,  h - (h * (0 - yMin) / (yMax - yMin)), w , 1);
    }
}
