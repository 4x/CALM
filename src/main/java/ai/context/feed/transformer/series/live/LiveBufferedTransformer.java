package ai.context.feed.transformer.series.live;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public abstract class LiveBufferedTransformer implements Feed {

    private int span;
    private Feed[] feeds;
    private long timeStamp = 0;

    private LinkedList<FeedObject[]> inputBuildup = new LinkedList<>();
    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    protected LiveBufferedTransformer(int span, Feed[] feeds) {
        this.span = span;
        this.feeds = feeds;
    }

    protected abstract FeedObject[] getOutput(FeedObject[] input);

    @Override
    public synchronized FeedObject readNext(Object caller) {
        if(buffers.containsKey(caller) && buffers.get(caller).size() > 0)
        {
            return buffers.get(caller).pollFirst();
        }

        nextInput();
        FeedObject feedObject = null;
        if(inputBuildup.size() < span){
            feedObject = new FeedObject(timeStamp, null);
        }
        else {
            FeedObject[] outputArray = getOutput(getArray(inputBuildup));
            feedObject = new FeedObject(timeStamp, outputArray[0].getData());
            inputBuildup.pollFirst();
        }

        for(Feed listener : buffers.keySet()){
            if(listener != caller){
                buffers.get(listener).add(feedObject);
            }
        }
        return feedObject;
    }

    private void nextInput()
    {
        FeedObject[] data = new FeedObject[feeds.length];
        int index = 0;

        for(Feed feed : feeds)
        {
            data[index] = feed.readNext(this);
            timeStamp = data[index].getTimeStamp();
            index++;
        }

        inputBuildup.add(data);
    }

    private FeedObject[] getArray(Queue<FeedObject[]> inputs)
    {
        FeedObject[] outputs = new FeedObject[inputs.size()];

        int outIndex = 0;
        for(FeedObject[] input : inputs){
            long timeStamp = input[0].getTimeStamp();
            Object[] data = new Object[input.length];
            int index = 0;
            for(FeedObject value : input)
            {
                data[index] = value.getData();
                index++;
            }

            FeedObject<Object[]> output = new FeedObject<Object[]>(timeStamp, data);
            outputs[outIndex] = output;
            outIndex++;
        }
        return outputs;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }
}
