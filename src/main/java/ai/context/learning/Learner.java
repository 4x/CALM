package ai.context.learning;

import ai.context.container.TimedContainer;
import ai.context.core.LearnerService;
import ai.context.util.mathematics.MinMaxAggregator;
import ai.context.util.measurement.OpenPosition;
import ai.context.util.measurement.LoggerTimer;
import ai.context.util.trading.BlackBox;
import ai.context.util.trading.PositionFactory;
import com.dukascopy.api.JFException;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Learner implements Runnable, TimedContainer{

    private LearnerService learner = new LearnerService();

    private TreeMap<Long, DataObject> recentData = new TreeMap<Long, DataObject>();
    private TreeMap<Long, MinMaxAggregator> recentAggregators = new TreeMap<>();

    private LinkedList<TreeMap<Double, Double>> recentPredictions = new LinkedList<TreeMap<Double, Double>>();
    private TreeMap<Double, Double> prediction = new TreeMap<Double, Double>();

    private HashSet<OpenPosition> positions = new HashSet<OpenPosition>();
    private TreeMap<Integer, TreeMap<Integer, Double>> successMap = new TreeMap<Integer, TreeMap<Integer, Double>>();

    private int tolerance = 2;
    private double actionResolution = 1.0;
    private int maxPopulation = 2000;

    private long timeShift = 0;
    private long tNow = 0;

    private LearnerFeed trainingLearnerFeed;
    private LearnerFeed liveLearnerFeed;

    private double accruedPnL = 0;

    private BufferedWriter fileOutputStream;

    private boolean inLiveTrading = false;
    private BlackBox blackBox;

    public Learner(String outputDir){
        try {
            fileOutputStream = new BufferedWriter(new FileWriter(outputDir + "PNL_"+ System.currentTimeMillis() + ".csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTrainingLearnerFeed(LearnerFeed trainingLearnerFeed) {
        this.trainingLearnerFeed = trainingLearnerFeed;
    }

    public void setLiveLearnerFeed(LearnerFeed liveLearnerFeed) {
        this.liveLearnerFeed = liveLearnerFeed;
    }

    @Override
    public void run() {

        learner.setActionResolution(actionResolution);
        learner.setMaxPopulation(maxPopulation);
        learner.setTolerance(tolerance);

        while (true)
        {
            LoggerTimer.printTimeDelta("1", this);

            DataObject data = trainingLearnerFeed.readNext();
            if(data == null)
            {
                break;
            }
            LoggerTimer.printTimeDelta("2", this);

            tNow = data.getTimeStamp();
            if(tNow % (6 * 3600 * 1000L) == 0)
            {
                System.out.println("It is now: " + new Date(tNow));
            }

            LoggerTimer.printTimeDelta("3", this);

            int[] signal = data.getSignal();
            LoggerTimer.printTimeDelta("4", this);

            TreeMap<Integer, Double> distribution = learner.getActionDistribution(signal);
            TreeMap<Double, Double> prediction = new TreeMap<Double, Double>();
            for(Map.Entry<Integer, Double> entry : distribution.entrySet())
            {
                prediction.put(data.getValue()[0] + entry.getKey() * actionResolution, entry.getValue());
            }
            LoggerTimer.printTimeDelta("5", this);

            recentPredictions.add(prediction);
            while (!recentData.isEmpty() && recentData.firstEntry().getValue().getTimeStamp() < (tNow - timeShift))
            {
                int[] s = recentData.firstEntry().getValue().getSignal();
                long t =  recentData.firstEntry().getValue().getTimeStamp();
                learner.addStateAction(s, recentAggregators.get(t).getMax());
                learner.addStateAction(s, recentAggregators.get(t).getMin());

                recentAggregators.remove(t);
                recentData.remove(t);
                if(!recentPredictions.isEmpty()) {
                    removeFromPrediction(recentPredictions.removeFirst());
                }
            }
            LoggerTimer.printTimeDelta("6", this);


            for(DataObject cursor : recentData.values()){

                recentAggregators.get(cursor.getTimeStamp()).addValue(data.getValue()[1] - cursor.getValue()[0]);
                recentAggregators.get(cursor.getTimeStamp()).addValue(data.getValue()[2] - cursor.getValue()[0]);
            }

            LoggerTimer.printTimeDelta("7", this);

            updateOverallPrediction(prediction);
            recentData.put(data.getTimeStamp(), data);
            recentAggregators.put(data.getTimeStamp(), new MinMaxAggregator());

            LoggerTimer.printTimeDelta("8", this);

            HashSet<OpenPosition> closed = new HashSet<OpenPosition>();
            for(OpenPosition position : positions)
            {
                if(position.canCloseOnBar_Pessimistic(tNow, data.getValue()[1], data.getValue()[2]))
                {
                    closed.add(position);
                    int x = (int) (position.getTarget()/actionResolution);
                    int y = (int) (position.getPnL()/actionResolution);
                    accruedPnL += position.getPnL();
                    PositionFactory.positionClosed(position);

                    System.out.println(position.getClosingMessage() + " PNL: " + accruedPnL + " CHANGE: " + position.getPnL() + " CAPITAL: " + PositionFactory.getAmount());
                    appendToFile(position.getClosingMessage() + " PNL: " + accruedPnL + " CHANGE: " + position.getPnL() + " CAPITAL: " + PositionFactory.getAmount());

                    double count = 0.0;
                    if(!successMap.containsKey(x))
                    {
                        successMap.put(x, new TreeMap<Integer, Double>());
                    }
                    if(!successMap.get(x).containsKey(y))
                    {
                        successMap.get(x).put(y, 0.0);
                    }
                    else {
                        count = successMap.get(x).get(y);
                    }
                    successMap.get(x).put(y, 1.0 + count);
                }
            }
            LoggerTimer.printTimeDelta("9", this);

            positions.removeAll(closed);

            OpenPosition position = PositionFactory.getPosition(tNow, data.getValue()[0], prediction);
            if(position != null)
            {
                if(inLiveTrading){
                    try {
                        blackBox.onDecision(position);
                    } catch (JFException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    positions.add(position);
                }
            }
            LoggerTimer.printTimeDelta("10", this);

        }
    }

    private void updateOverallPrediction(TreeMap<Double, Double> newPrediction)
    {
        for(Map.Entry<Double, Double> entry : newPrediction.entrySet())
        {
            if(!prediction.containsKey(entry.getKey()))
            {
                prediction.put(entry.getKey(), 0.0);
            }
            prediction.put(entry.getKey(), entry.getValue() + prediction.get(entry.getKey()));
        }
    }

    private void removeFromPrediction(TreeMap<Double, Double> oldPrediction)
    {
        for(Map.Entry<Double, Double> entry : oldPrediction.entrySet())
        {
            prediction.put(entry.getKey(), prediction.get(entry.getKey()) - entry.getValue());
        }
    }

    private void appendToFile(String data){
        try {
            fileOutputStream.write(data + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LearnerService getLearner() {
        return learner;
    }

    public int getTolerance() {
        return tolerance;
    }

    public void setTolerance(int tolerance) {
        this.tolerance = tolerance;
    }

    public double getActionResolution() {
        return actionResolution;
    }

    public void setActionResolution(double actionResolution) {
        this.actionResolution = actionResolution;
    }

    public int getMaxPopulation() {
        return maxPopulation;
    }

    public void setMaxPopulation(int maxPopulation) {
        this.maxPopulation = maxPopulation;
    }

    public long getTimeShift() {
        return timeShift;
    }

    public void setTimeShift(long timeShift) {
        this.timeShift = timeShift;
    }

    public void setCurrentTime(long time){
        tNow = time;
    }

    public long getTime()
    {
        return tNow;
    }

    public void setLive() {
        inLiveTrading = true;
    }

    public void setBlackBox(BlackBox blackBox) {
        this.blackBox = blackBox;
    }
}
