package ai.context.feed.stitchable;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.communication.Notifiable;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class StitchableFeed implements Feed {

    private Feed liveFeed;
    private boolean caughtUp = false;
    private String liveFileName;
    protected Notifiable notifiable;
    protected BufferedWriter writer;
    private long timeStamp;

    private LinkedList<FeedObject> queue = new LinkedList<>();

    public StitchableFeed(String liveFileName, Feed liveFeed) {
        this.liveFeed = liveFeed;
        this.liveFileName = liveFileName;

        try {
            File  file = new File(liveFileName);
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(liveFileName);
            writer = new BufferedWriter(new OutputStreamWriter(fos));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startPadding() {
        while (!caughtUp) {
            queue.add(readNext(this));
        }
    }

    protected abstract FeedObject formatForCSVFeed(FeedObject data);

    @Override
    public boolean hasNext() {
        return liveFeed.hasNext();
    }

    @Override
    public synchronized FeedObject readNext(Object caller) {
        FeedObject data = liveFeed.readNext(this);
        timeStamp = data.getTimeStamp();

        return formatForCSVFeed(data);
    }

    @Override
    public void addChild(Feed feed) {
        // DO Nothing: this should always have only one child
    }

    @Override
    public void removeChild(Feed feed) {
    }

    @Override
    public Feed getCopy() {
        return null;
    }

    public void catchUp() {
        if (notifiable != null) {
            notifiable.notifyFor("CATCH UP");
        }
        System.out.println("CAUGHT UP");
        this.caughtUp = true;
    }

    public boolean isCaughtUp() {
        return caughtUp;
    }

    public String getLiveFileName() {
        return liveFileName;
    }

    public void setNotifiable(Notifiable notifiable) {
        this.notifiable = notifiable;
    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Spliced live feed: " + liveFeed.getDescription(startIndex, padding);
    }

    @Override
    public List getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(liveFeed.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return liveFeed.getNumberOfOutputs();
    }
}
