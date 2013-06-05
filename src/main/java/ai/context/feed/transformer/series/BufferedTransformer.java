package ai.context.feed.transformer.series;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class BufferedTransformer implements Feed{

    private int bufferSize;
    private Feed[] feeds;

    private Queue<FeedObject[]> inputBuildup;
    private Queue<FeedObject> outputQueue;
    private Queue<FeedObject> outputBuildup;
    private LinkedList<FeedObject> finalOutputQueue = new LinkedList<FeedObject>();

    private Queue<Long> timeStamps;

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    protected BufferedTransformer(int bufferSize, Feed[] feeds) {
        this.bufferSize = bufferSize;
        this.feeds = feeds;

        inputBuildup = new ArrayBlockingQueue<FeedObject[]>(bufferSize);

        outputBuildup = new ArrayBlockingQueue<FeedObject>(bufferSize);
        outputQueue = new ArrayBlockingQueue<FeedObject>(bufferSize);
        timeStamps = new ArrayBlockingQueue<Long>(2 * bufferSize);
    }

    protected abstract FeedObject[] getOutput(FeedObject[] input);

    public FeedObject readNext(Object caller){

        if(buffers.containsKey(caller) && buffers.get(caller).size() > 0)
        {
            return buffers.get(caller).pollFirst();
        }

        if(outputQueue.size() < bufferSize/2)
        {
            if(inputBuildup.size() == bufferSize)
            {
                inputBuildup.poll();
                nextInput();

                FeedObject[] outputArray = getOutput(getArray(inputBuildup));
                for (FeedObject out : outputArray)
                {
                    outputBuildup.add(out);
                }

                while (inputBuildup.size() > bufferSize/2)
                {
                    inputBuildup.poll();
                }
            }

            if(!outputBuildup.isEmpty())
            {
                outputQueue = outputBuildup;
                outputBuildup = new ArrayBlockingQueue<FeedObject>(bufferSize);
            }
            else {
                while (inputBuildup.size() < bufferSize)
                {
                    nextInput();
                }

                FeedObject[] outputArray = getOutput(getArray(inputBuildup));
                for (FeedObject out : outputArray)
                {
                    outputQueue.add(out);
                }

                while (inputBuildup.size() > bufferSize/2)
                {
                    inputBuildup.poll();
                }
            }
        }
        else {
            nextInput();
        }

        FeedObject output = outputQueue.poll();
        long timeStamp = timeStamps.poll();
        if(!outputBuildup.isEmpty())
        {
            outputBuildup.poll();
        }
        finalOutputQueue.add(output);

        FeedObject feedObject = new FeedObject(timeStamp, finalOutputQueue.poll().getData());
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
            index++;
        }

        inputBuildup.add(data);
        timeStamps.add(data[0].getTimeStamp());
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

    protected void pushBackOutput(int nEntries)
    {
        for(int i = 0; i < nEntries; i++)
        {
            finalOutputQueue.add(new FeedObject(0, null));
        }
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }
}
