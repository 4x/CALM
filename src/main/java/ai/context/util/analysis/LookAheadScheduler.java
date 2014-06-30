package ai.context.util.analysis;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;

import java.util.ArrayList;
import java.util.List;

public class LookAheadScheduler {
    private Feed feed;
    private int[] keywordIndices;
    private List<FeedObject> buffer = new ArrayList<>();

    public LookAheadScheduler(Feed feed, int ... keywordIndices) {
        this.feed = feed;
        this.keywordIndices = keywordIndices;
    }

    public long getTimeToNext(long time, String[] keywords){

        List<FeedObject> toRemove = new ArrayList<>();
        for(FeedObject dataInBuffer : buffer){
            if(dataInBuffer.getTimeStamp() > time){
                Object[] contents = (Object[]) dataInBuffer.getData();
                if(isInterested(contents, keywords)){
                    return dataInBuffer.getTimeStamp() - time;
                }
            }
            else {
                toRemove.add(dataInBuffer);
            }
        }
        buffer.removeAll(toRemove);

        FeedObject data = null;
        while((data = feed.readNext(this)) != null){
            if(data.getTimeStamp() > time){
                buffer.add(data);
                Object[] contents = (Object[]) data.getData();
                if(isInterested(contents, keywords)){
                    return data.getTimeStamp() - time;
                }
            }
        }
        return 0;
    }

    private boolean isInterested(Object[] contents, String[] keywords){
        for(int i = 0; i < keywords.length; i++){
            if(!contents[keywordIndices[i]].equals(keywords[i])){
                return false;
            }
        }
        return true;
    }
}
