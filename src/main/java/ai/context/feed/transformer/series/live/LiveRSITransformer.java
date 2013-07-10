package ai.context.feed.transformer.series.live;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import com.tictactec.ta.lib.CoreAnnotated;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;

public class LiveRSITransformer extends LiveBufferedTransformer {
    private int span;
    private CoreAnnotated taLib = new CoreAnnotated();

    private int fastDPeriod;
    private int fastKPeriod;
    private MAType fastDMAType;

    private Feed feed;

    public LiveRSITransformer(int span, int fastDPeriod, int fastKPeriod, MAType fastDMAType, Feed feed)
    {
        super((10 * span), new Feed[]{feed});

        this.span = span;
        this.fastDMAType = fastDMAType;
        this.fastDPeriod = fastDPeriod;
        this.fastKPeriod = fastKPeriod;
        this.feed = feed;
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

        taLib.stochRsi(0, input.length - 1, inputArray, span, fastKPeriod, fastDPeriod, fastDMAType, new MInteger(), new MInteger(), outputArray[0], outputArray[1]);

        for(int i = 0; i < input.length; i++){
            output[i] = new FeedObject(input[i].getTimeStamp(), new Double[]{outputArray[0][i], outputArray[1][i]});
        }
        return output;
    }

    @Override
    public Feed getCopy() {
        return new LiveRSITransformer(span, fastDPeriod, fastKPeriod, fastDMAType, feed.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        return padding + "["+startIndex+"] Live RSI with span: " + span + ", fast D period " + fastDPeriod + " for feed: " + ", fast K period " + fastKPeriod + " and fastD MA type: " +fastDMAType+ " for feed: " + feed.getDescription(startIndex, padding);
    }
}
