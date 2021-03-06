package ai.context.feed.synchronised;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.List;

public class SynchronisedFeed extends SynchronisableFeed {

    private Feed rawFeed;

    public SynchronisedFeed(){
        super(null);
    }

    public SynchronisedFeed(Feed rawFeed, SynchronisableFeed sibling) {
        super(sibling);
        this.rawFeed = rawFeed;
        rawFeed.addChild(this);
    }

    @Override
    public boolean hasNext() {
        return rawFeed.hasNext();
    }

    @Override
    public FeedObject readNext(Object caller) {
        return rawFeed.readNext(this);
    }

    @Override
    public Feed getCopy() {
        SynchronisedFeed copy = null;

        for (Feed feed : feeds) {
            copy = new SynchronisedFeed(feed.getCopy(), copy);
        }
        return copy;
    }

    public Feed getRawFeed() {
        return rawFeed;
    }

    public void addRawFeed(Feed feed){
        if(rawFeed == null){
            rawFeed = feed;
        }
        else {
            new SynchronisedFeed(feed, this);
        }
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        String description = "";
        for (SynchronisableFeed feed : feeds) {
            description += ((SynchronisedFeed) feed).getRawFeed().getDescription(startIndex, padding + " ") + "\n";
            String[] lines = description.split("\n");
            startIndex = Integer.parseInt(lines[lines.length - 1].trim().split("]")[0].substring(1));
            startIndex++;
        }
        return description;
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Primary Feed",
                "A Sibling Feed <Optional>"
        };
    }

    @Override
    public List getElementChain(int element) {
        int remaining = element;
        Feed feed = null;
        for (Feed candidate : feeds) {
            feed = ((SynchronisedFeed) candidate).getRawFeed();
            if (feed.getNumberOfOutputs() > remaining) {
                break;
            }

            remaining -= feed.getNumberOfOutputs();
        }

        List list = new ArrayList<>();

        list.add(this);
        list.add(feed.getElementChain(remaining));
        return list;
    }



    @Override
    public int getNumberOfOutputs() {
        int number = 0;
        for (Feed feed : feeds) {
            number += ((SynchronisedFeed) feed).getRawFeed().getNumberOfOutputs();
        }
        return number;
    }


}
