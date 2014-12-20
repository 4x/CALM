package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.List;

import static ai.context.util.mathematics.Discretiser.getLogarithmicDiscretisation;

public class AccumulationDistributionOnlineTransformer extends OnlineTransformer {

    private Feed feedPrice;
    private Feed feedVolume;
    private double lastADI = 0;

    private ArrayList<Double> vals = new ArrayList<>();

    private double lambda = 0.01;
    private double volumeEMA = 0;

    public AccumulationDistributionOnlineTransformer(int bufferSize, Feed feedPrice, Feed feedVolume) {
        super(bufferSize, feedPrice, feedVolume);
        this.feedPrice = feedPrice;
        this.feedVolume = feedVolume;
    }

    @Override
    protected Object getOutput() {
        Double price = (Double) ((List) arriving.getData()).get(0);
        Double volume = (Double) ((List) arriving.getData()).get(1);
        volumeEMA = (1 - lambda) * volumeEMA + lambda * volume;
        vals.add(price);
        if(vals.size() < bufferSize + 1){
            return 0;
        }
        else {
            vals.remove(0);
        }


        double h = vals.get(0);
        double l = vals.get(0);

        for(int i = 0; i < bufferSize; i++){
            double v = vals.get(i);

            if(v > h){
                h = v;
            }

            if(v < l){
                l = v;
            }
        }

        lastADI += ((price - l) - (h - price))/(h - l) * volume;

        return getLogarithmicDiscretisation(lastADI, 0, Math.max(1, volumeEMA/10));
    }



    @Override
    public Feed getCopy() {
        return null;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] VL and span: " + bufferSize + " for feed: " + feedPrice.getDescription(startIndex, padding) + " and " + feedVolume.getDescription(startIndex, padding);
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        list.add(feedPrice.getElementChain(0));
        list.add(feedVolume.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
