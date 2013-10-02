package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.*;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;
import static ai.context.util.mathematics.Operations.tanInverse;

public class RadarOnlineTransformer  extends OnlineTransformer{

    private Feed feedMin;
    private Feed feedMax;
    private Feed feedClose;
    private double resolution;

    private double lastTop = 0;
    private double lastBottom = 0;
    private double lastTSpace = 0;
    private double lastBSpace = 0;

    public RadarOnlineTransformer(int bufferSize, Feed feedMin, Feed feedMax, Feed feedClose, double resolution) {
        super(bufferSize, feedMin, feedMax, feedClose);
        this.feedMin = feedMin;
        this.feedMax = feedMax;
        this.feedClose = feedClose;
        this.resolution = resolution;
    }

    @Override
    protected Object getOutput() {
        ArrayList<Double[]> points = new ArrayList<>();

        if(init){
            FeedObject last = buffer.getLast();
            double origin =  ((Double) ((List)last.getData()).get(2))/resolution;
            origin +=  ((Double) ((List)last.getData()).get(0))/resolution;
            origin +=  ((Double) ((List)last.getData()).get(1))/resolution;
            origin = origin/3;
            int i = 1 + buffer.size();

            for(FeedObject point : buffer){

                Double minA = (Double) ((List)point.getData()).get(0)/resolution;
                Double maxA = (Double) ((List)point.getData()).get(1)/resolution;
                Double close = (Double) ((List)point.getData()).get(2)/resolution;

                double angle1 = tanInverse(-i, +(minA - origin));
                double distance1 = Math.sqrt(Math.pow(minA - origin, 2) + Math.pow(i, 2));

                double angle2 = tanInverse(-i, +(maxA - origin));
                double distance2 = Math.sqrt(Math.pow(maxA - origin, 2) + Math.pow(i, 2));

                double angle3 = tanInverse(-i, +(close - origin));
                double distance3 = Math.sqrt(Math.pow(close - origin, 2) + Math.pow(i, 2));

                points.add(new Double[]{angle1, distance1});
                points.add(new Double[]{angle2, distance2});
                points.add(new Double[]{angle3, distance3});

                i--;
            }

            double bottom = 0;
            double bottomSpace = 0;
            double top = 0;
            double topSpace = 0;

            double maxBottom = 0;
            double maxTop = 0;

            int tightness = 2;
            int divisions = tightness * 10;
            for(int a = -10; a < 10; a++){
                double angle = (Math.PI/divisions) * a;
                TreeMap<Integer, Double> histogram = new TreeMap<>();
                for(Double[] point : points){
                    int pointClass = (int) (point[1] * Math.sin(point[0] + angle))/1;

                    if(!histogram.containsKey(pointClass)){
                        histogram.put(pointClass, 0D);
                    }

                    histogram.put(pointClass, histogram.get(pointClass) + 1);
                }

                List<Double[]> changes = new ArrayList<Double[]>();

                int inspect = 10;
                int c = 1;
                double sum = 0;
                for(Map.Entry<Integer, Double> entry : histogram.entrySet()){
                    sum += entry.getValue();
                    double rise = sum/c;
                    if(c > inspect && rise > maxTop){
                        maxTop = rise;
                        top = a;
                        topSpace = entry.getKey();
                    }

                    changes.add(new Double[]{rise, 0.0});
                    c++;
                }

                SortedMap<Integer, Double> desc = histogram.descendingMap();
                sum = 0;
                c = 1;
                for(Map.Entry<Integer, Double> entry : desc.entrySet()){
                    sum += entry.getValue();
                    double fall =  sum/c;
                    if(c > inspect && fall > maxBottom){
                        maxBottom = fall;
                        bottom = a;
                        bottomSpace = entry.getKey();
                    }

                    changes.get(c - 1)[1] = fall;
                    c++;
                }

                /*if(last.getTimeStamp() == 180){
                    *//*for(Map.Entry<Integer, Double> entry : histogram.entrySet()){
                        System.out.println(a + "," + entry.getKey() + "," + entry.getValue());
                    }*//*

                    //System.out.println(">> " + a + " " + maxTop + " " + maxBottom + " " + top + " " + bottom);

                    for(Double[] val : changes){
                        System.out.println(a + "," +val[0] + "," + val[1]);
                    }
                }*/
            }
            double convergence = 0;
            double lambda = 1;
            lastTop = (1 - lambda) * lastTop + lambda * top;
            lastBottom = (1 - lambda) * lastBottom + lambda * bottom;
            lastTSpace = (1 - lambda) * lastTSpace + lambda * topSpace;
            lastBSpace = (1 - lambda) * lastBSpace + lambda * bottomSpace;

            if(lastTop != lastBottom){
                convergence = getLogarithmicDiscretisation(Math.abs(lastTSpace + lastBSpace)/Math.abs(lastTop - lastBottom), 0, resolution);
            }

            return  new Double[]{lastTop, lastTSpace, lastBottom, lastBSpace, convergence};
        }
        else {
            buffer.clear();
            while(buffer.size() < bufferSize){
                buffer.add(new FeedObject(0, arriving.getData()));
            }
            return new Double[]{0.0, 0.0, 0.0, 0.0, 0.0};
        }
    }

    @Override
    public Feed getCopy() {
        return new RadarOnlineTransformer(bufferSize, feedMin.getCopy(), feedMax.getCopy(), feedClose.getCopy(), resolution);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "["+startIndex+"] RadarOnlineTransformer and span: " + bufferSize + " for feed: " + feedMin.getDescription(startIndex, padding) + " and " + feedMax.getDescription(startIndex, padding) + " and " + feedClose.getDescription(startIndex, padding);
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        list.add(feedMin.getElementChain(0));
        list.add(feedMax.getElementChain(0));
        list.add(feedClose.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 5;
    }
}
