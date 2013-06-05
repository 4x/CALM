package ai.context.feed.transformer.single.padded;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class PaddedTransformer implements Feed{

    private int padding;
    private Feed[] feeds;

    private Queue<FeedObject[]> inputBuildUp;

    protected PaddedTransformer(int padding, Feed[] feeds) {
        this.padding = padding;
        this.feeds = feeds;

        inputBuildUp = new ArrayBlockingQueue<FeedObject[]>(padding);
    }

    public FeedObject readNext(Object caller)
    {
        FeedObject[] data = new FeedObject[feeds.length];
        int index = 0;

        for(Feed feed : feeds)
        {
            data[index] = feed.readNext(this);
            index++;
        }

        if(inputBuildUp.size() == padding)
        {
            inputBuildUp.poll();
        }
        inputBuildUp.add(data);

        return new FeedObject(data[0].getTimeStamp(), getOutput(getPaddedInput()));
    }

    private Object[][] getPaddedInput()
    {
        Object[][] inputArray = new Object[feeds.length][padding];
        int i;
        for(i = 0; i < padding; i++)
        {
            for (int feedId = 0; feedId < inputArray.length; feedId++)
            {
                inputArray[feedId][i] = 0.0;
            }
        }

        for(FeedObject[] input : inputBuildUp)
        {
            for (int feedId = 0; feedId < inputArray.length; feedId++)
            {
                inputArray[feedId][i] = input[feedId].getData();
            }
            i++;
        }

        return inputArray;
    }

    protected abstract Object getOutput(Object[][] input);

    @Override
    public boolean hasNext() {
        return true;
    }
}
