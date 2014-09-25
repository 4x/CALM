package ai.context.util.analysis.feed.transformer;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.surgical.ExtractOneFromListFeed;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.util.common.Count;
import ai.context.util.configuration.PropertiesHolder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import static ai.context.util.mathematics.Operations.tanInverse;

public class RadarAnalysis extends JPanel{

    private Graphics2D g2d;
    private int w;
    private int h;

    private int mX;
    private int mY;

    private LinkedList<FeedObject> buffer = new LinkedList();
    private double resolution;
    private int span;

    public RadarAnalysis(double resolution, int span) {
        this.resolution = resolution;
        this.span = span;

        this.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mX = e.getX();
                mY = e.getY();
                repaint();
            }
        });
    }

    public void add(FeedObject data){

        buffer.add(data);
        //System.out.println("Added: " + data);
        if(buffer.size() > span){
            buffer.pollFirst();
        }
        else {
            return;
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(buffer.isEmpty()){
            return;
        }
        g2d = (Graphics2D) g;
        g2d.setColor(Color.BLUE);

        Dimension size = getSize();
        Insets insets = getInsets();

        w = size.width - insets.left - insets.right;
        h = size.height - insets.bottom - insets.top;


        ArrayList<Double[]> points = new ArrayList<>();

        FeedObject last = buffer.getLast();
        double origin = ((Double) ((java.util.List) last.getData()).get(2)) / resolution;
        int i = 1 + buffer.size();

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (FeedObject point : buffer) {

            Double minA = (Double) ((java.util.List) point.getData()).get(0) / resolution;
            Double maxA = (Double) ((java.util.List) point.getData()).get(1) / resolution;
            Double close = (Double) ((java.util.List) point.getData()).get(2) / resolution;

            double angle1 = tanInverse(-i, +(minA - origin));
            double distance1 = Math.sqrt(Math.pow(minA - origin, 2) + Math.pow(i, 2));

            double angle2 = tanInverse(-i, +(maxA - origin));
            double distance2 = Math.sqrt(Math.pow(maxA - origin, 2) + Math.pow(i, 2));

            double angle3 = tanInverse(-i, +(close - origin));
            double distance3 = Math.sqrt(Math.pow(close - origin, 2) + Math.pow(i, 2));

            if(i > maxX){
                maxX = i;
            }
            if(i < minX){
                minX = i;
            }

            if(maxA - origin > maxY){
                maxY = maxA - origin;
            }

            if(minA - origin < minY){
                minY = minA - origin;
            }

            points.add(new Double[]{angle1, distance1});
            points.add(new Double[]{angle2, distance2});
            points.add(new Double[]{angle3, distance3});
            i--;
        }

        for(Double[] p : points){
            int x = (int) (w + (w * (p[1] * Math.cos(p[0])) - minX)/(maxX - minX));
            int y = (int) (h/2 - (h/2 * (p[1] * Math.sin(p[0])) - minY)/(maxY - minY));
            g2d.fillOval(x, y, 3, 3);
        }

        g2d.setColor(Color.RED);
        int x = (int) (w + (w * (0) - minX)/(maxX - minX));
        int y = (int) (h/2 - (h/2 * (0) - minY)/(maxY - minY));
        g2d.fillOval(x, y, 5, 5);

        g2d.drawLine(x, y, mX, mY);


        double angle = tanInverse(mX - x, y - mY);
        g2d.drawString("Plane: " + angle, 40, 40);

        double divisor = 10;

        int tries = 0;
        TreeMap<Integer, Count> hist = new TreeMap<>();
        while(true) {
            hist.clear();
            for (Double[] p : points) {
                double c = p[1] * Math.sin(p[0] - angle);

                int cClass = (int) Math.round(c / divisor);
                if (!hist.containsKey(cClass)) {
                    hist.put(cClass, new Count());
                }
                hist.get(cClass).val++;
            }

            if(tries > 10){
                break;
            }
            if(hist.size() > 20){
                divisor *= 1.25;
            }
            else if(hist.size() < 16){
                divisor /= 1.25;
            }
            else {
                break;
            }
            tries++;
        }

        int maxC = hist.lastKey();
        int minC = hist.firstKey();

        int maxSpanC = Math.max(Math.abs(maxC), Math.abs(minC));

        g2d.translate(w/2, 200);
        g2d.rotate(3*Math.PI/2 - angle);

        for(Map.Entry<Integer, Count> e : hist.entrySet()){
            int barH = (int) ((e.getValue().val*200)/span);
            int barW = 200/maxSpanC;


            int barX = barW * e.getKey();

            g2d.fillRect(barX, 0, barW, barH);
        }

        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, 1, 200);

        AffineTransform old = g2d.getTransform();
        g2d.rotate(3*Math.PI/2 - angle);
        g2d.setTransform(old);
        g2d.translate(-w/2, -200);
    }

    public static void main(String[] args){
        RadarAnalysis radarAnalysis = new RadarAnalysis(0.0001, 50);

        JFrame frame = new JFrame("Radar Analysis");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800, 620);
        frame.setVisible(true);
        JPanel frameControl = new JPanel();
        frameControl.setLayout(new GridLayout());
        frameControl.setSize(400, 70);
        frameControl.setVisible(true);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, frameControl, radarAnalysis);
        splitPane.setDividerSize(1);
        splitPane.setDividerLocation(30);
        frame.add(splitPane);

        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = PropertiesHolder.startDateTime;

        CSVFeed feed = new CSVFeed("/opt/dev/data/feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice, dateFP);
        feed.setSkipWeekends(true);
        ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
        ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
        ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);

        SynchronisedFeed synchronisedFeed = new SynchronisedFeed(feedH, null);
        synchronisedFeed.addRawFeed(feedL);
        synchronisedFeed.addRawFeed(feedC);

        for(int i = 0; i < 3670; i++){
            radarAnalysis.add(synchronisedFeed.getNextComposite(null));
        }

        radarAnalysis.repaint();
    }
}
