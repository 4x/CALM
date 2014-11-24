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
        int i = 0;
        for(Feed f : motherFeed.rawFeeds()){
            System.out.println(i + " " + f.getDescription(0,"") + " " + (f.getLatestTime() - tX));
            i++;
        }
        FeedObject dY = motherFeed.getNextComposite(y);
        long tY = dY.getTimeStamp();
        i = 0;
        for(Feed f : motherFeed.rawFeeds()){
            System.out.println(i + " " + f + " " + (f.getLatestTime() - tY));
            i++;
        }
        FeedObject dZ = motherFeed.getNextComposite(z);
        long tZ = dZ.getTimeStamp();
        i = 0;
        for(Feed f : motherFeed.rawFeeds()){
            System.out.println(i + " " + f + " " + (f.getLatestTime() - tZ));
            i++;
        }

        System.out.print((tX - tY) + " " + (tY - tZ));
    }
}
