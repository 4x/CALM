package ai.context.feed.stitchable;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.communication.Notifiable;

import java.io.*;

public abstract class StitchableFeed implements Feed {

    private Feed liveFeed;
    private boolean caughtUp = false;
    private String liveFileName;
    protected Notifiable notifiable;
    protected BufferedWriter writer;

    public StitchableFeed(String liveFileName, Feed liveFeed){
        this.liveFeed = liveFeed;
        this.liveFileName = liveFileName;
        try {
            FileOutputStream fos = new FileOutputStream(liveFileName);
            writer = new BufferedWriter(new OutputStreamWriter(fos));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startPadding(){
        while (!caughtUp){
            readNext(this);
        }
    }

    protected abstract FeedObject formatForCSVFeed(FeedObject data);

    @Override
    public boolean hasNext() {
        return liveFeed.hasNext();
    }

    @Override
    public FeedObject readNext(Object caller) {
        FeedObject data = liveFeed.readNext(this);

        return formatForCSVFeed(data);
    }

    @Override
    public void addChild(Feed feed) {
        // DO Nothing: this should always have only one child
    }

    @Override
    public Feed getCopy() {
        return null;
    }

    public void catchUp() {
        notifiable.notifyFor("CATCH UP");
        this.caughtUp = true;
    }

    public boolean isCaughtUp(){
        return caughtUp;
    }

    public String getLiveFileName() {
        return liveFileName;
    }

    public void setNotifiable(Notifiable notifiable) {
        this.notifiable = notifiable;
    }
}
