package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class RadarOnlineTransformer  extends OnlineTransformer{

    private Feed feedMin;
    private Feed feedMax;
    private Feed feedClose;
    private double resolution;

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
            int i = 1 + buffer.size();

            for(FeedObject point : buffer){

                Double minA = (Double) ((List)point.getData()).get(0)/resolution;
                Double maxA = (Double) ((List)point.getData()).get(1)/resolution;
                Double close = (Double) ((List)point.getData()).get(2)/resolution;

                double angle1 = Math.atan(minA/i);
                double distance1 = Math.sqrt(Math.pow(minA - origin, 2) + Math.pow(i, 2));

                double angle2 = Math.atan(maxA/i);
                double distance2 = Math.sqrt(Math.pow(maxA - origin, 2) + Math.pow(i, 2));

                double angle3 = Math.atan(close/i);
                double distance3 = Math.sqrt(Math.pow(close - origin, 2) + Math.pow(i, 2));

                points.add(new Double[]{Math.PI/4 - angle1, distance1});
                points.add(new Double[]{Math.PI/4 - angle2, distance2});
                points.add(new Double[]{Math.PI/4 - angle3, distance3});

                i--;
            }

            for(int a = 0; a < 20; a++){
                double angle = (Math.PI/40) * a;
                TreeMap<Integer, Long> histogram = new TreeMap<>();
                for(Double[] point : points){
                    int pointClass = (int) (point[0] * Math.cos(point[1] + angle));

                    if(!histogram.containsKey(pointClass)){
                        histogram.put(pointClass, 0L);
                    }

                    histogram.put(pointClass, histogram.get(pointClass) + 1);
                }
            }
        }
        else {
            buffer.clear();
            while(buffer.size() < bufferSize){
                buffer.add(new FeedObject(0, arriving.getData()));
            }
            return new Double[]{0.0, 0.0, 0.0, 0.0};
        }
        return new Double[]{};
    }

    @Override
    public Feed getCopy() {
        return new RadarOnlineTransformer(bufferSize, feedMin.getCopy(), feedMax.getCopy(), feedClose.getCopy(), resolution);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "["+startIndex+"] RadarOnlineTransformer and span: " + bufferSize + " for feed: " + feedMin.getDescription(startIndex, padding) + " and " + feedMax.getDescription(startIndex, padding) + " and " + feedClose.getDescription(startIndex, padding);
    }
}
