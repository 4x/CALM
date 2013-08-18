package ai.context.util.analysis.feed;

import javax.swing.*;
import java.awt.*;

public class Analytics extends JPanel{

    private Graphics2D g2d;
    private int w;
    private int h;

    public Analytics(){
        setLayout(null);
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
        g2d.setColor(Color.BLUE);


    }
}
