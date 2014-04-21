package ai.context.feed.predictor;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.util.analysis.feed.Learner;

import java.util.*;

public class PredictionExtractionFeed implements Feed {

    private LinkedList<FeedObject<TreeMap<Integer, Double>>> inputFeed = new LinkedList<>();
    private HashMap<Feed, LinkedList<FeedObject>> buffers = new HashMap<>();

    private Learner learner;

    private long timeStamp = 0;

    public PredictionExtractionFeed(Learner learner) {
        this.learner = learner;
        learner.addExtractor(this);
    }

    public synchronized void addData(FeedObject<TreeMap<Integer, Double>> data) {
        inputFeed.add(data);
        notify();
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public synchronized FeedObject readNext(Object caller) {
        if (buffers.containsKey(caller) && buffers.get(caller).size() > 0) {
            return buffers.get(caller).pollFirst();
        }

        while (inputFeed.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        FeedObject<TreeMap<Integer, Double>> data = inputFeed.pollFirst();
        timeStamp = data.getTimeStamp();

        double count = 0;
        double sum = 0;
        double sum2 = 0;

        for (Map.Entry<Integer, Double> entry : data.getData().entrySet()) {
            count += entry.getValue();
            sum += entry.getKey() * entry.getValue();
            sum2 += entry.getKey() * entry.getKey() * entry.getValue();
        }

        int sectionIndex = 0;
        double[] sections = new double[5];
        double count2 = 0;
        for (Map.Entry<Integer, Double> entry : data.getData().entrySet()) {
            count2 += entry.getValue();
            if (count2 >= sectionIndex * count / (sections.length - 1)) {
                sections[sectionIndex] = entry.getKey();
                sectionIndex++;
            }
        }
        double mean = sum / count;
        double stddev = Math.sqrt(sum2 / count - Math.pow(sum / count, 2));

        double[] output = new double[sections.length + 2];
        output[0] = mean;
        output[1] = stddev;
        for (int i = 2; i < output.length; i++) {
            output[i] = sections[i - 2];
        }
        FeedObject feedObject = new FeedObject(timeStamp, output);

        for (Feed listener : buffers.keySet()) {
            if (listener != caller) {
                buffers.get(listener).add(feedObject);
            }
        }

        return feedObject;
    }

    @Override
    public Feed getCopy() {
        return new PredictionExtractionFeed(learner);
    }

    @Override
    public void removeChild(Feed feed) {
        buffers.remove(feed);
    }

    @Override
    public void addChild(Feed feed) {
        buffers.put(feed, new LinkedList<FeedObject>());
    }

    @Override
    public long getLatestTime() {
        return timeStamp;
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Prediction Extractor:\n"
                + learner.getSignalFeed().getDescription(startIndex++, padding + " ")
                + learner.getDataFeed().getDescription(startIndex++, padding + " ");
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        list.add(this);
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 7;
    }
}
