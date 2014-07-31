package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.List;

public class SuddenShiftsOnlineTransformer  extends OnlineTransformer {

    private Feed feedMin;
    private Feed feedMax;

    private double res;
    private double move;

    private ArrayList<Double> vals = new ArrayList<>();

    public SuddenShiftsOnlineTransformer(int bufferSize, Feed feedMin, Feed feedMax, double res, double move) {
        super(bufferSize, feedMin, feedMax);
        this.feedMin = feedMin;
        this.feedMax = feedMax;
        this.res = res;
        this.move = move;
    }

    @Override
    protected Object getOutput() {
        Double minA = (Double) ((List) arriving.getData()).get(0);
        Double maxA = (Double) ((List) arriving.getData()).get(1);

        long t = arriving.getTimeStamp();

        vals.add((maxA + minA)/2);

        if(vals.size() < bufferSize + 1){
            return 0;
        }
        else {
            vals.remove(0);
        }

        int shift = 0;
        int dir = 0;
        double h = vals.get(0);
        double l = vals.get(0);

        for(int i = 0; i < bufferSize; i++){
            double v = vals.get(i);

            if(v > l + (move * res)){
                h = v;
                if(dir != 1){
                    dir = 1;
                    shift++;
                }
            }

            if(v < h - (move * res)){
                l = v;
                if(dir != -1){
                    dir = -1;
                    shift++;
                }
            }
        }

        return shift;
    }



    @Override
    public Feed getCopy() {
        return null;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] SS and span: " + bufferSize + " for feed: " + feedMin.getDescription(startIndex, padding) + " and " + feedMax.getDescription(startIndex, padding);
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        list.add(feedMin.getElementChain(0));
        list.add(feedMax.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
