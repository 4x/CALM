package ai.context.feed.transformer.series.live;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import com.tictactec.ta.lib.CoreAnnotated;
import com.tictactec.ta.lib.MInteger;

import java.util.ArrayList;
import java.util.List;

public class LiveMinMaxTransformer extends LiveBufferedTransformer {

    private CoreAnnotated taLib = new CoreAnnotated();
    private long span = 0;
    private Feed feed;

    public LiveMinMaxTransformer(int span, Feed feed) {
        super(span * 10, new Feed[]{feed});
        this.feed = feed;
        this.span = span;
    }

    @Override
    public FeedObject[] getOutput(FeedObject[] input) {
        float[] inputArray = new float[input.length];
        double[][] outputArray = new double[2][input.length];
        FeedObject[] output = new FeedObject[input.length];

        for (int i = 0; i < input.length; i++) {
            Object value = ((Object[]) input[i].getData())[0];
            inputArray[i] = ((Double) value).floatValue();
        }

        taLib.minMax(0, input.length - 1, inputArray, (int) span, new MInteger(), new MInteger(), outputArray[0], outputArray[1]);

        for (int i = 0; i < input.length; i++) {
            output[i] = new FeedObject(input[i].getTimeStamp(), new double[]{outputArray[0][i], outputArray[1][i]});
        }
        return output;
    }

    @Override
    public Feed getCopy() {
        return new LiveMinMaxTransformer((int) span, feed.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        return padding + "[" + startIndex + "] Live MINMAX with span: " + span + " for feed: " + feed.getDescription(startIndex, padding);
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
