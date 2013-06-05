package ai.context.feed.transformer.series;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import com.tictactec.ta.lib.CoreAnnotated;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;

public class MATransformer extends BufferedTransformer{

    private int span;
    private MAType type;
    private CoreAnnotated taLib = new CoreAnnotated();

    private Feed feed;

    public MATransformer(MAType type, int span, Feed feed)
    {
        super((10*span), new Feed[]{feed});

        this.span = span;
        this.type = type;
        this.feed = feed;

        pushBackOutput(span - 1);
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

        taLib.movingAverage(0, input.length - 1, inputArray, span, type, new MInteger(), new MInteger(), outputArray);

        for(int i = 0; i < input.length; i++){
            output[i] = new FeedObject(input[i].getTimeStamp(), outputArray[i]);
        }
        return output;
    }

    @Override
    public Feed getCopy() {
        return new MATransformer(type, span, feed.getCopy());
    }
}
