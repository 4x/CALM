package ai.context.feed.row;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.stitchable.StitchableFeed;
import ai.context.util.DataSetUtils;
import ai.context.util.StringUtils;
import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CSVFeed extends RowFeed {

    private CSVReader reader;
    private BufferedWriter writer;

    private String[] nextLine;
    private DataType[] types;
    private SimpleDateFormat format;
    private String fileName;
    private String timeStampRegex;

    private StitchableFeed stitchableFeed;
    private CSVReader stitchReader;
    private boolean stitching = false;
    private long timeStamp;
    private int lastLineHash = 0;

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    public CSVFeed(String fileName, String timeStampRegex, DataType[] types)
    {
        this.format = new SimpleDateFormat(timeStampRegex);
        this.fileName = fileName;
        this.timeStampRegex = timeStampRegex;
        format.setTimeZone(TimeZone.getTimeZone("GMT"));

        this.types = types;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileName);
            FileOutputStream fos = new FileOutputStream(fileName, true);
            writer = new BufferedWriter(new OutputStreamWriter(fos));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        reader = new CSVReader(new InputStreamReader(inputStream), ',', '"', 1);
    }

    @Override
    public boolean hasNext() {
        try {
            nextLine = reader.readNext();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (nextLine != null);
    }

    @Override
    public FeedObject readNext(Object caller){

        if(buffers.containsKey(caller) && buffers.get(caller).size() > 0)
        {
            return buffers.get(caller).pollFirst();
        }
        long timeStamp = 0;
        while (true){
            try {
                if(!stitching){
                    nextLine = reader.readNext();
                    if(nextLine == null){
                        if(stitchableFeed == null){
                            return null;
                        }
                        else {
                            stitching = true;
                            System.out.println("Switched to stitching");
                            InputStream inputStream = null;
                            try {
                                inputStream = new FileInputStream(stitchableFeed.getLiveFileName());
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            stitchReader = new CSVReader(new InputStreamReader(inputStream), ',', '"', 1);
                            nextLine = stitchReader.readNext();
                            if(nextLine == null){
                                stitchableFeed.catchUp();
                                System.out.println("Caught up");
                                FeedObject toReturn = stitchableFeed.readNext(this);
                                append(toReturn);
                                return toReturn;
                            }
                        }
                    }
                }
                else {
                    if(stitchableFeed.isCaughtUp()){
                        FeedObject toReturn = stitchableFeed.readNext(this);
                        append(toReturn);
                        return toReturn;
                    }
                    else{
                        nextLine = stitchReader.readNext();
                        if(nextLine == null){
                            stitchableFeed.catchUp();
                            System.out.println("Caught up");
                            FeedObject toReturn = stitchableFeed.readNext(this);
                            append(toReturn);
                            return toReturn;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            try {
                timeStamp = format.parse(nextLine[0]).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if(!stitching || timeStamp > this.timeStamp || (timeStamp == this.timeStamp && nextLine.hashCode() == lastLineHash)){
                break;
            }
        }
        lastLineHash = nextLine.hashCode();

        Object[] data = new Object[types.length];
        for (int i = 0; i < types.length; i++)
        {
            switch (types[i])
            {
                case DOUBLE:
                    data[i] = Double.parseDouble(nextLine[i + 1]);
                    break;
                case INTEGER:
                    data[i] = Integer.parseInt(nextLine[i + 1]);
                    break;
                case LONG:
                    data[i] = Long.parseLong(nextLine[i + 1]);
                    break;
                case EXTRACTABLE_DOUBLE:
                    data[i] = StringUtils.extractDouble(nextLine[i + 1]);
                    break;
                default:
                    data[i] = nextLine[i + 1];
            }
        }
        FeedObject feedObject = new FeedObject(timeStamp, data);
        for(Feed listener : buffers.keySet()){
            if(listener != caller){
                buffers.get(listener).add(feedObject);
            }
        }

        this.timeStamp = feedObject.getTimeStamp();
        if(stitching){
            append(feedObject);
        }
        return feedObject;
    }

    public CSVFeed getCopy()
    {
        return new CSVFeed(fileName, timeStampRegex, types);
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }

    public void setStitchableFeed(StitchableFeed stitchableFeed) {
        this.stitchableFeed = stitchableFeed;
    }

    public boolean isStitching() {
        return stitching;
    }

    private void append(FeedObject feedObject){
        List list = new ArrayList<>();
        DataSetUtils.add(feedObject.getData(), list);

        String toAppend = format.format(new Date(feedObject.getTimeStamp()));
        for(Object column : list){
            toAppend += "," + column;
        }
        try {
            writer.write(toAppend + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
