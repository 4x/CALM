package ai.context;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchronisableFeed;
import org.junit.Before;
import org.junit.Test;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class TestSynchFeeds {

    private TestFeed[] f = new TestFeed[3];

    @Before
    public void setup()
    {
        f[0] = new TestFeed(null);
        f[1] = new TestFeed(f[0]);
        f[2] = new TestFeed(f[1]);
        
        
        f[1].add(new FeedObject<Integer>(0,1));
        f[1].add(new FeedObject<Integer>(10,2));
        f[1].add(new FeedObject<Integer>(20,3));
        f[1].add(new FeedObject<Integer>(30,4));
        f[1].add(new FeedObject<Integer>(40,5));

        f[0].add(new FeedObject<Integer>(3,1));
        f[0].add(new FeedObject<Integer>(9,2));
        f[0].add(new FeedObject<Integer>(20,3));
        f[0].add(new FeedObject<Integer>(25,4));
        f[0].add(new FeedObject<Integer>(45,5));

        f[2].add(new FeedObject<Integer>(5, 1));
        f[2].add(new FeedObject<Integer>(15, 2));
        f[2].add(new FeedObject<Integer>(25, 3));
        f[2].add(new FeedObject<Integer>(35, 4));
        f[2].add(new FeedObject<Integer>(45, 5));

        f[0].init();
    }

    @Test
    public void testFeeds()
    {
        /*for(int i = 0; i < 20; i++)
        {
            for(int iF = 0; iF < 3; iF++)
            {
                FeedObject data = f[iF].getNext();
                System.out.println(i + " " + iF + " " + data.getTimeStamp() + " " + data.getData());
            }
        }
        */
        for(int i = 0; i < 10; i++)
        {
            FeedObject data = f[0].getNextComposite(this);
            System.out.println(i + " " + data.getTimeStamp() + " " + data.getData());
        }


    }
}

class TestFeed extends SynchronisableFeed{

    private Queue<FeedObject<Integer>> feed = new ArrayBlockingQueue<FeedObject<Integer>>(100);

    public TestFeed(SynchronisableFeed sibling)
    {
        super(sibling);
    }

    @Override
    public boolean hasNext() {
        return !feed.isEmpty();
    }

    @Override
    public FeedObject readNext(Object caller) {
        return feed.poll();
    }

    public void add(FeedObject<Integer> data)
    {
        feed.add(data);
    }

    @Override
    public Feed getCopy() {
        return null;
    }
}
