package ai.context.util.trading;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LearningAnalyser extends JPanel{
    public static void main(String[] args){

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {}
        new LearningAnalyser();
    }

    private int neurons = 250;
    private String address = "http://hyophorbe-associates.com:8056";

    private TreeMap<Integer, TreeMap<Integer, Double>> dists;
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;

    private TreeMap<Integer, Double> maxYMap = new TreeMap<>();
    private double maxY = 0;

    public LearningAnalyser(){
        reload();

        JFrame frame = new JFrame("Learning Analyser");
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.pack();

        Insets insets = frame.getInsets();
        frame.setSize(new Dimension(insets.left + insets.right + 800, insets.top + insets.bottom + 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        JButton reload = new JButton("Refresh");
        reload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }
        });
        reload.setBounds(10, 10, 100, 25);
        this.setLayout(null);
        this.add(reload);
    }

    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        int w = this.getWidth();
        int h = this.getHeight();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, w, h);

        int aX = (1 + Math.max(maxX, Math.abs(minX))/10) * 10;

        g2d.setColor(new Color(255, 0, 0 , 30));
        //g2d.setColor(Color.RED);
        for(Map.Entry<Integer, TreeMap<Integer, Double>> entry : dists.entrySet()){
            int lastXPos = -1;
            int lastYPos = -1;
            double maxY = this.maxY;
            maxY = maxYMap.get(entry.getKey());
            for(Map.Entry<Integer, Double> p : entry.getValue().entrySet()){
                int x = p.getKey();
                int xPos = (x * w/2)/aX + w/2;

                double y = p.getValue();
                int yPos = (int) ((h - 25) - ((h - 30) * y)/maxY);

                if(lastXPos != -1){
                    g2d.drawLine(lastXPos, lastYPos, xPos, yPos);
                    //g2d.fillRect(xPos, yPos, 1, 1);
                }
                lastXPos = xPos;
                lastYPos = yPos;
            }
        }

        g2d.setFont(new Font("Monospace", 0, 8));
        for(int x = -aX; x <= aX; x += 10){
            g2d.setColor(Color.WHITE);
            g2d.drawString("" + x, (x * w/2)/aX + w/2, h - 15);
            g2d.setColor(new Color(255, 255, 255 , 30));
            g2d.drawLine((x * w/2)/aX + w/2, h - 25, (x * w/2)/aX + w/2, 0);
        }
        g2d.drawLine(0, h - 25, w, h - 25);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    public void reload(){

        final TreeMap<Integer, TreeMap<Integer, Double>> dists = new TreeMap<>();

        final int threads = 8;
        final CountDownLatch latch = new CountDownLatch(threads);
        final AtomicInteger workId = new AtomicInteger(0);
        final ExecutorService pool = Executors.newFixedThreadPool(threads);

        long t = System.currentTimeMillis();

        for(int thread = 0; thread < threads; thread++){
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        int i;
                        while((i = workId.incrementAndGet()) <= neurons){
                            TreeMap<Integer, Double> s = new TreeMap<>();

                            URL url  = new URL(address + "/info?REQ_TYPE=PRED:" + i);
                            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                            String inputLine;
                            while ((inputLine = in.readLine()) != null){
                                String[] parts = inputLine.split(",");
                                s.put(Integer.valueOf(parts[0]), Double.valueOf(parts[1]));
                            }

                            TreeMap<Integer, Double> d = new TreeMap<>();
                            double cum = 0;
                            for(Map.Entry<Integer, Double> entry : s.headMap(0).entrySet()){
                                cum += entry.getValue();
                                d.put(entry.getKey(), cum);
                            }
                            cum = 0;
                            for(Map.Entry<Integer, Double> entry : s.descendingMap().headMap(0).entrySet()){
                                cum += entry.getValue();
                                d.put(entry.getKey(), cum);
                            }
                            dists.put(i, d);
                            in.close();
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    latch.countDown();
                }
            };

            pool.submit(r);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(dists.size());
        System.out.println(System.currentTimeMillis() - t);
        this.dists = dists;

        refresh();
        repaint();
    }

    public void refresh(){
        for(Map.Entry<Integer, TreeMap<Integer, Double>> entry : dists.entrySet()){
            double maxY = 0;
            for(Map.Entry<Integer, Double> p : entry.getValue().entrySet()){
                if(p.getKey() < minX){
                    minX = p.getKey();
                }
                else if(p.getKey() > maxX){
                    maxX = p.getKey();
                }

                if(p.getValue() > maxY){
                    maxY = p.getValue();
                }
            }
            maxYMap.put(entry.getKey(), maxY);
            if(maxY > this.maxY){
                this.maxY = maxY;
            }
        }
    }
}
