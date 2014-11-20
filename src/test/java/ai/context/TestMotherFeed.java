package ai.context;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.util.feeding.MotherFeedCreator;

public class TestMotherFeed {
    public static void main(String[] args){
        ISynchFeed motherFeed = MotherFeedCreator.getMotherFeed("/opt/dev/data/");


        Object x = new Object();
        Object y = new Object();
        Object z = new Object();
        FeedObject dX = motherFeed.getNextComposite(x);
        long tX = dX.getTimeStamp();
        for(Feed f : motherFeed.rawFeeds()){
            System.out.println(f + " " + (f.getLatestTime() - tX));
        }
        FeedObject dY = motherFeed.getNextComposite(y);
        long tY = dY.getTimeStamp();
        for(Feed f : motherFeed.rawFeeds()){
            System.out.println(f + " " + (f.getLatestTime() - tY));
        }
        FeedObject dZ = motherFeed.getNextComposite(z);
        long tZ = dZ.getTimeStamp();
        for(Feed f : motherFeed.rawFeeds()){
            System.out.println(f + " " + (f.getLatestTime() - tZ));
        }

        System.out.print((tX - tY) + " " + (tY - tZ));
    }
}
