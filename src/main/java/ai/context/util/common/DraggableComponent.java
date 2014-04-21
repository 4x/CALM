package ai.context.util.common;

import ai.context.util.analysis.feed.WorkArea;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class DraggableComponent extends JPanel {

    private volatile int screenX = 0;
    private volatile int screenY = 0;
    private volatile int myX = 0;
    private volatile int myY = 0;

    protected WorkArea area;

    public DraggableComponent() {
        setBorder(new LineBorder(Color.BLUE, 3));
        setBackground(Color.ORANGE);
        setBounds(0, 0, 100, 100);
        setOpaque(true);

        addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                screenX = e.getXOnScreen();
                screenY = e.getYOnScreen();

                myX = getX();
                myY = getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

        });
        addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
                int deltaX = e.getXOnScreen() - screenX;
                int deltaY = e.getYOnScreen() - screenY;

                setLocation(myX + deltaX, myY + deltaY);
                area.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }

        });
    }

    public void setWorkArea(WorkArea area) {
        this.area = area;
    }

    public void deselect() {

    }

    ;

}