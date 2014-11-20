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
import java.util.concurrent.ArrayBlockingQueue;

public class CSVFeed extends RowFeed {

    private CSVReader reader;
    private BufferedWriter writer;

    private String[] nextLine;
    private DataType[] types;
    private SimpleDateFormat format;
    private String fileName;
    private String timeStampRegex;

    private StitchableFeed stitchableFeed;
    private boolean stitching = false;
    private long timeStamp = 0;
    private int lastLineHash = 0;
    private String startDateTime;

    private long interval = 500;

    protected boolean paddable = false;

    private Object[] paddingData;

    private boolean testing = false;

    private boolean skipWeekends = false;

    private Object previousData;
    private FeedObject previousLive;
    private ArrayBlockingQueue<FeedObject> liveData = new ArrayBlockingQueue<FeedObject>(1000);

    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    public CSVFeed(String fileName, String timeStampRegex, DataType[] types, long startDateTime) {
        this(fileName, timeStampRegex,types, new SimpleDateFormat(timeStampRegex).format(new Date(startDateTime)));
    }

    public CSVFeed(String fileName, String timeStampRegex, DataType[] types, String startDateTime) {
        this.format = new SimpleDateFormat(timeStampRegex);
        this.fileName = fileName;
        this.timeStampRegex = timeStampRegex;
        this.startDateTime = startDateTime;
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (startDateTime != null) {
            try {
                this.timeStamp = format.parse(startDateTime).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        this.types = types;
        paddingData = new Object[types.length];
        int i = 0;
        for (DataType type : types) {
            switch (type) {
                case DOUBLE:
                    paddingData[i] = 0.0;
                    break;
                case EXTRACTABLE_DOUBLE:
                    paddingData[i] = 0.0;
                    break;
                case INTEGER:
                    paddingData[i] = 0;
                    break;
                case LONG:
                    paddingData[i] = 0L;
                    break;
                case OTHER:
                    paddingData[i] = "PADDING";
                    break;
                default:
                    paddingData[i] = "PADDING";
            }
            i++;
        }
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
        if(stitching){
            return stitchableFeed.hasNext();
        }

        try {
            nextLine = reader.readNext();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (nextLine != null);
    }

    @Override
    public synchronized FeedObject readNext(Object caller) {

        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }
        long timeStamp = 0;
        while (true) {
            try {
                if (!stitching) {
                    nextLine = reader.readNext();
                    if (nextLine == null) {
                        if (stitchableFeed == null) {
                            return null;
                        } else {
                            stitching = true;
                            System.out.println("Switched to stitching " + fileName);
                            startCollectingLive();

                            return returnNextLive(caller);
                        }
                    }
                } else {
                    return returnNextLive(caller);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                timeStamp = format.parse(nextLine[0]).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (timeStamp > this.timeStamp || (timeStamp == this.timeStamp && nextLine.hashCode() != lastLineHash)) {
                if(skipWeekends){
                    Date d = new Date(timeStamp);
                    if(!(d.getDay() == 6 || d.getDay() == 0)){
                        break;
                    }
                }
                else {
                    break;
                }
            }
        }
        lastLineHash = nextLine.hashCode();

        try {
            Object[] data = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                switch (types[i]) {
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
            previousData = data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        FeedObject feedObject = new FeedObject(timeStamp, previousData);
        List<Feed> toRemove = new ArrayList<>();
        for (Feed listener : buffers.keySet()) {
            if (listener != caller) {
                List<FeedObject> list = buffers.get(listener);
                list.add(feedObject);
                if(list.size() > 2000){
                    toRemove.add(listener);
                }
            }
        }
        for(Feed remove : toRemove){
            buffers.remove(remove);
        }

        this.timeStamp = feedObject.getTimeStamp();
        previousLive = feedObject;
        return feedObject;
    }

    private FeedObject returnNextLive(Object caller){
        FeedObject toReturn;
        while (true){
            toReturn = getNextLive();

            if(skipWeekends){
                Date d = new Date(toReturn.getTimeStamp());
                if(!(d.getDay() == 6 || d.getDay() == 0)){
                    break;
                }
            }
            else {
                break;
            }
        }
        append(toReturn);
        this.timeStamp = toReturn.getTimeStamp();
        for (Feed listener : buffers.keySet()) {
            if (listener != caller) {
                buffers.get(listener).add(toReturn);
            }
        }
        return toReturn;
    }

    public CSVFeed getCopy() {
        return new CSVFeed(fileName, timeStampRegex, types, startDateTime);
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }

    @Override
    public void removeChild(Feed feed) {
        buffers.remove(feed);
    }

    public void setStitchableFeed(StitchableFeed stitchableFeed) {
        this.stitchableFeed = stitchableFeed;
    }

    public boolean isStitching() {
        return stitching;
    }

    private void append(FeedObject feedObject) {
        if (!testing) {
            List list = new ArrayList<>();
            DataSetUtils.add(feedObject.getData(), list);

            String toAppend = format.format(new Date(feedObject.getTimeStamp()));
            for (Object column : list) {
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

    private void startCollectingLive() {
        Runnable collector = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    previousLive = stitchableFeed.readNext(this);
                    liveData.add(previousLive);
                }
            }
        };
        new Thread(collector).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private FeedObject getNextLive() {

        FeedObject data = null;
        while (true) {
            data = liveData.poll();
            if (data == null) {
                if (paddable) {
                    long t = System.currentTimeMillis();
                    t = t - (t % interval) + interval;

                    if(skipWeekends){
                        Date d = new Date(t);
                        if(!(d.getDay() == 6 || d.getDay() == 0)){
                            return new FeedObject(t, paddingData);
                        }
                    }
                    else {
                        return new FeedObject(t, paddingData);
                    }
                } else {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if (data.getTimeStamp() >= timeStamp) {
                if(skipWeekends){
                    Date d = new Date(data.getTimeStamp());
                    if(!(d.getDay() == 6 || d.getDay() == 0)){
                        break;
                    }
                }
                else {
                    break;
                }
            }
        }
        return data;
    }

    public boolean isSkipWeekends() {
        return skipWeekends;
    }

    public void setSkipWeekends(boolean skipWeekends) {
        this.skipWeekends = skipWeekends;
    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void setPaddable(boolean paddable) {
        this.paddable = paddable;
    }

    public void close(){
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] CSV File: " + fileName;
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return types.length;
    }

    @Override
    public String toString() {
        return getDescription(0, "");
    }
}
