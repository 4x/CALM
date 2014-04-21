package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.DataSetUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SynchFeed implements ISynchFeed {
    private long time = 0;
    private Map<Feed, LinkedList<FeedObject>> buffers = new ConcurrentHashMap<>();
    private List<RawFeedWrapper> rawFeeds = new CopyOnWriteArrayList<>();
    private Map<Feed, RawFeedWrapper> mapping = new ConcurrentHashMap<>();

    @Override
    public synchronized FeedObject getNextComposite(Object caller) {
        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }

        List<Object> data = new ArrayList<Object>();
        refreshTimes();
        for (RawFeedWrapper wrapper : rawFeeds) {
            DataSetUtils.add(wrapper.getLatestDataAtTime(time).getData(), data);
        }

        FeedObject feedObject = new FeedObject(time, data);
        for (Feed listener : buffers.keySet()) {
            if (listener != caller) {
                buffers.get(listener).add(feedObject);
            }
        }
        return feedObject;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public FeedObject readNext(Object caller) {
        return getNextComposite(caller);
    }

    @Override
    public Feed getCopy() {
        return null;
    }

    @Override
    public long getLatestTime() {
        return time;
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
        int longest = 0;
        for (LinkedList<FeedObject> list : buffers.values()) {
            if (list.size() > longest) {
                longest = list.size();
                buffers.put(feed, list);
            }
        }
    }

    @Override
    public void removeChild(Feed feed) {
        buffers.remove(feed);
    }

    public void addRawFeed(Feed rawFeed) {
        RawFeedWrapper wrapper = new RawFeedWrapper(rawFeed);
        mapping.put(rawFeed, wrapper);
        rawFeeds.add(wrapper);
    }

    public void removeRawFeed(Feed rawFeed) {
        rawFeeds.remove(mapping.remove(rawFeed));
    }

    public void refreshTimes() {
        TreeSet<Long> times = new TreeSet<>();
        for (RawFeedWrapper wrapper : rawFeeds) {
            times.add(wrapper.getHeadTimeStamp());
            times.add(wrapper.getNextTimeStamp());
        }
        Iterator<Long> iterator = times.iterator();
        long t = iterator.next();
        while (time >= t) {
            if (iterator.hasNext()) {
                t = iterator.next();
            }
        }
        time = t;
    }

    public void cleanup() {
        for (RawFeedWrapper rawFeedWrapper : rawFeeds) {
            rawFeedWrapper.cleanup();
        }
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return null;
    }

    @Override
    public List getElementChain(int element) {
        return null;
    }

    @Override
    public int getNumberOfOutputs() {
        int number = 0;
        for (RawFeedWrapper wrapper : rawFeeds) {
            number += wrapper.getRawFeed().getNumberOfOutputs();
        }
        return number;
    }
}
