package ai.context.feed.row;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.StringUtils;
import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TimeZone;

public class CSVFeed extends RowFeed {

    private CSVReader reader;
    private String[] nextLine;
    private DataType[] types;
    private SimpleDateFormat format;
    private String fileName;
    private String timeStampRegex;

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
        try {
            nextLine = reader.readNext();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long timeStamp = 0;
        try {
            timeStamp = format.parse(nextLine[0]).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
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
}
