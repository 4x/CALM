package ai.context.feed.transformer.series.live;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import com.tictactec.ta.lib.CoreAnnotated;
import com.tictactec.ta.lib.MInteger;

import java.util.ArrayList;
import java.util.List;

public class LiveSlopeTransformer extends LiveBufferedTransformer {
    private int span;
    private CoreAnnotated taLib = new CoreAnnotated();

    private Feed feed;

    public LiveSlopeTransformer(int span, Feed feed) {
        super((10 * span), new Feed[]{feed});

        this.span = span;
        this.feed = feed;
    }

    @Override
    protected FeedObject[] getOutput(FeedObject[] input) {
        float[] inputArray = new float[input.length];
        double[] outputArray = new double[input.length];
        FeedObject[] output = new FeedObject[input.length];

        for (int i = 0; i < input.length; i++) {
            Object value = ((Object[]) input[i].getData())[0];
            inputArray[i] = ((Double) value).floatValue();
        }

        taLib.linearRegSlope(0, input.length - 1, inputArray, span, new MInteger(), new MInteger(), outputArray);

        for (int i = 0; i < input.length; i++) {
            output[i] = new FeedObject(input[i].getTimeStamp(), outputArray[i]);
        }
        return output;
    }

    @Override
    public Feed getCopy() {
        return new LiveSlopeTransformer(span, feed.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        return padding + "[" + startIndex + "] Live SLOPE with span: " + span + " for feed: " + feed.getDescription(startIndex, padding);
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
        return 1;
    }
}
