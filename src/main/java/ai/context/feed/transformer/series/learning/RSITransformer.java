package ai.context.feed.transformer.series.learning;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import com.tictactec.ta.lib.CoreAnnotated;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;

import java.util.ArrayList;
import java.util.List;

public class RSITransformer extends BufferedTransformer {
    private int span;
    private CoreAnnotated taLib = new CoreAnnotated();

    private int fastDPeriod;
    private int fastKPeriod;
    private MAType fastDMAType;

    private Feed feed;

    public RSITransformer(int span, int fastDPeriod, int fastKPeriod, MAType fastDMAType, Feed feed)
    {
        super((10 * span), new Feed[]{feed});

        this.span = span;
        this.fastDMAType = fastDMAType;
        this.fastDPeriod = fastDPeriod;
        this.fastKPeriod = fastKPeriod;
        this.feed = feed;

        pushBackOutput(span);
    }

    @Override
    protected FeedObject[] getOutput(FeedObject[] input) {
        float[] inputArray = new float[input.length];
        double[][] outputArray = new double[2][input.length];
        FeedObject[] output = new FeedObject[input.length];

        for(int i = 0; i < input.length; i++){
            Object value = ((Object[]) input[i].getData())[0];
            inputArray[i] = ((Double)value).floatValue();
        }

        taLib.stochRsi(0, input.length - 1, inputArray, span/2 + 1, fastKPeriod, fastDPeriod, fastDMAType, new MInteger(), new MInteger(), outputArray[0], outputArray[1]);

        //System.out.println("RSI");
        for(int i = 0; i < input.length; i++){
            output[i] = new FeedObject(input[i].getTimeStamp(), new Double[]{outputArray[0][i], outputArray[1][i]});
            //System.out.println(i + " " + outputArray[0][i] + " " + outputArray[1][i]);
        }
        return output;
    }

    @Override
    public Feed getCopy() {
        return new RSITransformer(span, fastDPeriod, fastKPeriod, fastDMAType, feed.getCopy());
    }

    public void goLive(){
        goLive(span);
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        return padding + "["+startIndex+"] RSI with span: " + span + ", fast D period " + fastDPeriod + ", fast K period " + fastKPeriod + " and fastD MA type: " +fastDMAType+ " for feed: " + feed.getDescription(startIndex, padding);
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        list.add(feed.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 2;
    }
}
