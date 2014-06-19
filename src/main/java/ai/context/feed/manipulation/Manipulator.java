package ai.context.feed.manipulation;

import ai.context.feed.FeedObject;

import java.util.Set;
import java.util.TreeMap;

public interface Manipulator {
    public FeedObject<Integer[]> manipulate(long t, TreeMap<Long, Set<FeedObject>> history);

    public int getNumberOfOutputs();
}
