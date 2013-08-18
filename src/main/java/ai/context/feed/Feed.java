package ai.context.feed;

import java.util.List;

public interface Feed<T> {
    public boolean hasNext();
    public FeedObject<T> readNext(Object caller);
    public Feed getCopy();

    public long getLatestTime();
    public void addChild(Feed feed);

    public String getDescription(int startIndex, String padding);

    //public String[] getConstructorArguments();

    public List getElementChain(int element);
    public int getNumberOfOutputs();
}
