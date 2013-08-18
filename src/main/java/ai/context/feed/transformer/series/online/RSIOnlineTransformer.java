package ai.context.feed.transformer.series.online;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RSIOnlineTransformer extends OnlineTransformer{

    private Feed input;

    private double last = 0;
    private double emaU = 0;
    private double emaD = 0;

    private double emaF = 0;

    private double lambda = 0.5;

    private int fastKPeriod = 10;
    private int slowDPeriod = 3;

    private LinkedList<Double> rsiList = new LinkedList<>();
    private LinkedList<Double> fastList = new LinkedList<>();

    public RSIOnlineTransformer(Feed input, int slowDPeriod, int fastKPeriod, double lambda) {
        super(1, input);
        this.input = input;
        this.slowDPeriod = slowDPeriod;
        this.fastKPeriod = fastKPeriod;
        this.lambda = lambda;
        while (rsiList.size() < fastKPeriod){
            rsiList.add(0.0);
        }

        while (fastList.size() < slowDPeriod){
            fastList.add(0.0);
        }
    }


    @Override
    protected Object getOutput() {

        if(!init){
            last = (Double) arriving.getData();
            buffer.clear();
            while(buffer.size() < bufferSize){
                buffer.add(new FeedObject(0, arriving.getData()));
            }
        }

        double change = (Double) arriving.getData() - last;
        if(change > 0){
            emaU = (1 - lambda)*emaU + lambda*change;
            emaD = (1 - lambda)*emaD;
        }
        else if(change < 0){
            emaU = (1 - lambda)*emaU;
            emaD = (1 - lambda)*emaD - lambda*change;
        }
        else {
            emaU = (1 - lambda)*emaU;
            emaD = (1 - lambda)*emaD;
        }

        double rsi = 0;
        if(emaD > 0){
            rsi = 100 - (100/(1 + emaU/emaD));
        }
        rsiList.add(rsi);
        rsiList.pollFirst();

        double high = rsi;
        double low = rsi;
        for(double val : rsiList){
            if(val > high){
                high = val;
            }
            else if(val < low){
                low = val;
            }
        }
        double fast = 0;
        if(high > low){
            fast = (rsi - low)/(high - low);
        }
        fastList.add(fast);
        fastList.pollFirst();
        if(!init){
            emaF = fast;
        }
        emaF = (1 - lambda) * emaF + lambda * fast;
        double sumFast = 0;
        for(Double element : fastList){
            sumFast += element;
        }
        last = (Double) arriving.getData();
        return new Double[]{rsi, fast, sumFast/slowDPeriod, emaF};
    }

    @Override
    public Feed getCopy() {
        return new RSIOnlineTransformer(input.getCopy(), slowDPeriod, fastKPeriod, lambda);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "["+startIndex+"] RSI with lambda="+ lambda + ", K=" + fastKPeriod + ", D=" + slowDPeriod +" for feed: " + input.getDescription(startIndex, padding);
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        list.add(input.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 4;
    }
}
