package ai.context.feed.transformer.series.learning;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import com.tictactec.ta.lib.CoreAnnotated;
import com.tictactec.ta.lib.MInteger;

public class MinMaxTransformer extends BufferedTransformer{

    private CoreAnnotated taLib = new CoreAnnotated();
    private long span = 0;
    private Feed feed;

    public MinMaxTransformer(int span, Feed feed) {
        super(span*10, new Feed[]{feed});
        this.feed = feed;
        this.span = span;
        pushBackOutput(span);
    }

    @Override
    public  FeedObject[] getOutput(FeedObject[] input) {
        float[] inputArray = new float[input.length];
        double[][] outputArray = new double[2][input.length];
        FeedObject[] output = new FeedObject[input.length];

        for(int i = 0; i < input.length; i++){
            Object value = ((Object[]) input[i].getData())[0];
            inputArray[i] = ((Double)value).floatValue();
        }

        taLib.minMax(0, input.length - 1, inputArray, (int) span, new MInteger(), new MInteger(), outputArray[0], outputArray[1]);

        for(int i = 0; i < input.length; i++){
            output[i] = new FeedObject(input[i].getTimeStamp(), new double[] {outputArray[0][i], outputArray[1][i]});
        }
        return output;
    }

    @Override
    public Feed getCopy() {
        return new MinMaxTransformer((int)span, feed.getCopy());
    }

    public void goLive(){
        goLive(span);
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        return padding + "["+startIndex+"] MINMAX with span: " + span + " for feed: " + feed.getDescription(startIndex, padding);
    }
}
