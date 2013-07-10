package ai.context.feed.transformer.series.live;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import com.tictactec.ta.lib.CoreAnnotated;
import com.tictactec.ta.lib.MInteger;

public class LiveStandardDeviationTransformer extends LiveBufferedTransformer{
    private int span;
    private double nDeviations;
    private CoreAnnotated taLib = new CoreAnnotated();

    private Feed feed;

    public LiveStandardDeviationTransformer(int span, double nDeviations, Feed feed)
    {
        super((10 * span), new Feed[]{feed});

        this.span = span;
        this.nDeviations = nDeviations;
        this.feed = feed;
    }

    @Override
    protected FeedObject[] getOutput(FeedObject[] input) {
        float[] inputArray = new float[input.length];
        double[] outputArray = new double[input.length];
        FeedObject[] output = new FeedObject[input.length];

        for(int i = 0; i < input.length; i++){
            Object value = ((Object[]) input[i].getData())[0];
            inputArray[i] = ((Double)value).floatValue();
        }

        taLib.stdDev(0, input.length - 1, inputArray, span, nDeviations, new MInteger(), new MInteger(), outputArray);

        for(int i = 0; i < input.length; i++){
            output[i] = new FeedObject(input[i].getTimeStamp(), outputArray[i]);
        }
        return output;
    }

    @Override
    public LiveStandardDeviationTransformer getCopy() {
        return new LiveStandardDeviationTransformer(span, nDeviations, feed.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        return padding + "["+startIndex+"] Live STDDEV with span: " + span + " for feed: " + feed.getDescription(startIndex, padding);
    }
}
