package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SynchronisedToNormalFeed implements Feed {

    private SynchronisableFeed feed;
    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    public SynchronisedToNormalFeed(SynchronisableFeed feed) {
        this.feed = feed;
    }

    @Override
    public boolean hasNext() {
        return feed.hasNext();
    }

    @Override
    public FeedObject readNext(Object caller) {
        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }
        FeedObject data = feed.getNextComposite(this);
        for (Feed listener : buffers.keySet()) {
            if (listener != caller) {
                buffers.get(listener).add(data);
            }
        }
        return data;
    }

    @Override
    public Feed getCopy() {
        return feed.getCopy();
    }

    @Override
    public long getLatestTime() {
        return feed.getLatestTime();
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }

    @Override
    public void removeChild(Feed feed) {
        buffers.remove(feed);
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return "Proxy for: \n" + feed.getDescription(startIndex, padding);
    }

    @Override
    public List getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(feed.getElementChain(element));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return feed.getNumberOfOutputs();
    }
}
