package ai.context.util.analysis;

import au.com.bytecode.opencsv.CSVReader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class TimeSeriesChart extends JPanel {

    private Graphics2D g2d;
    private int w;
    private int h;
    private int mouseX = 0;

    private TreeMap<String, LinkedList<Double>> series = new TreeMap<>();
    private long lastN;

    private JTextField tFile = new JTextField("Filename");
    private JTextField tSeries = new JTextField("Series Configuration");
    private JTextField tLastN = new JTextField("Elements Required");
    private JButton go = new JButton("Update Chart");

    private Color[] colors;

    public TimeSeriesChart() {
        JFrame frame = new JFrame("Time Series Chart");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800, 620);
        frame.setVisible(true);


        this.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                repaint();
            }
        });

        JPanel frameControl = new JPanel();
        frameControl.setLayout(new GridLayout());
        frameControl.setSize(400, 70);
        frameControl.setVisible(true);
        frameControl.add(tFile);
        frameControl.add(tSeries);
        frameControl.add(tLastN);
        frameControl.add(go);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, frameControl, this);
        splitPane.setDividerSize(1);
        splitPane.setDividerLocation(30);
        frame.add(splitPane);


        go.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String file = tFile.getText();
                String config = tSeries.getText();

                String[] parts = config.split(";");
                String[] names = new String[parts.length];
                int[] indices = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    String[] x = parts[i].split(":");
                    names[i] = x[0];
                    indices[i] = Integer.parseInt(x[1]);
                }
                long lastN = Long.parseLong(tLastN.getText());

                update(file, names, indices, lastN);
            }

            @Override
            public void mousePressed(MouseEvent e) {
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
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g2d = (Graphics2D) g;
        g2d.setColor(Color.BLUE);

        Dimension size = getSize();
        Insets insets = getInsets();

        w = size.width - insets.left - insets.right;
        h = size.height - insets.bottom - insets.top;

        g2d.clearRect(0, 0, w, h);

        if (series.isEmpty()) {
            return;
        }

        int cHeight = (h - 100) / series.size();
        double xStep = (w - 100.0) / lastN;
        int index = 1;
        for (Map.Entry<String, LinkedList<Double>> element : series.entrySet()) {
            String name = element.getKey();
            LinkedList<Double> values = element.getValue();

            double min = values.getFirst();
            double max = min;

            for (double value : values) {
                if (min > value) {
                    min = value;
                }
                if (max < value) {
                    max = value;
                }

            }

            int n = 0;
            double lastY = 50 + index * cHeight;
            double lastX = 50;

            g2d.setColor(colors[index - 1]);
            g2d.fillRect(0, (int) (lastY - cHeight) + 2, w, cHeight - 2);

            g2d.setColor(Color.BLACK);
            g2d.drawString(name, 30, (float) (lastY - cHeight / 2));
            g2d.drawString(max + "", 10, (float) (lastY - (cHeight - 20)));
            g2d.drawString(min + "", 10, (float) (lastY));

            for (double value : values) {

                double x = n * xStep + 50;
                double y = 50 + (index * cHeight) - ((value - min) / (max - min)) * (cHeight - 20);

                g2d.draw(new Line2D.Double(lastX, lastY, x, y));
                lastX = x;
                lastY = y;
                n++;
            }
            index++;

            g2d.setColor(Color.RED);
            g2d.draw(new Line2D.Double(mouseX, 0, mouseX, h));
        }

    }

    public void update(String file, String[] names, int[] seriesIndex, long lastN) {

        colors = null;
        this.lastN = lastN;
        series.clear();
        colors = new Color[names.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new Color((int) (100 + Math.random() * 150), (int) (100 + Math.random() * 150), (int) (100 + Math.random() * 150));
        }
        for (String name : names) {
            series.put(name, new LinkedList<Double>());
        }
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file)), ',', '"', 0);
            String[] line;
            while ((line = reader.readNext()) != null) {
                for (int i = 0; i < names.length; i++) {
                    String name = names[i];
                    Double value = Double.parseDouble(line[seriesIndex[i]]);

                    LinkedList<Double> element = series.get(name);
                    element.add(value);

                    if (element.size() > lastN) {
                        element.removeFirst();
                    }

                    //System.out.println(name + " " + element.size());
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.repaint();
    }

    public static void main(String[] args) {
        TimeSeriesChart chart = new TimeSeriesChart();
    }
}
