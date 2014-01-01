package ai.context.learning;

import ai.context.builder.LearnerServiceBuilder;
import ai.context.container.TimedContainer;
import ai.context.core.ai.LearnerService;
import ai.context.core.ai.StateActionPair;
import ai.context.util.learning.ClusteredCopulae;
import ai.context.util.mathematics.CorrelationCalculator;
import ai.context.util.mathematics.MinMaxAggregator;
import ai.context.util.measurement.OpenPosition;
import ai.context.util.trading.BlackBox;
import ai.context.util.trading.PositionFactory;
import com.dukascopy.api.JFException;

import java.io.*;
import java.util.*;

import static ai.context.util.common.DateUtils.getTimeFromString_YYYYMMddHHmmss;

public class Learner implements Runnable, TimedContainer{

    private LearnerService learner = new LearnerService();

    private TreeMap<Long, DataObject> recentData = new TreeMap<Long, DataObject>();
    private TreeMap<Long, MinMaxAggregator> recentAggregators = new TreeMap<>();
    //private TreeMap<Long, Integer> recentPos = new TreeMap<>();

    private LinkedList<TreeMap<Double, Double>> recentPredictions = new LinkedList<TreeMap<Double, Double>>();
    private TreeMap<Double, Double> prediction = new TreeMap<Double, Double>();

    private HashSet<OpenPosition> positions = new HashSet<OpenPosition>();
    private TreeMap<Integer, TreeMap<Integer, Double>> successMap = new TreeMap<Integer, TreeMap<Integer, Double>>();

    private long tNow = 0;

    private LearnerFeed trainingLearnerFeed;

    private double accruedPnL = 0;

    private BufferedWriter fileOutputStream;

    private boolean inLiveTrading = false;
    private BlackBox blackBox;

    private boolean adapting;


    private boolean alive = true;

    private boolean saved = false;
    private long timeToSave = getTimeFromString_YYYYMMddHHmmss("20130201000000");

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

    @Override
    public void run() {

        while (alive)
        {
            DataObject data = trainingLearnerFeed.readNext();
            tNow = data.getTimeStamp();

            if(data == null)
            {
                break;
            }

            if(adapting && tNow > timeToSave){
                break;
            }

            if(!learner.isMerging() && !saved && tNow > timeToSave){
                System.out.println("Saving at " + new Date(tNow) + " " + new Date(timeToSave));
                LearnerServiceBuilder.save(learner, "./memories", timeToSave);
                saved = true;
            }

            if(tNow % (6 * 3600 * 1000L) == 0)
            {
                System.out.println("It is now: " + new Date(tNow));
            }


            for(DataObject cursor : recentData.values()){

                recentAggregators.get(cursor.getTimeStamp()).addValue(data.getValue()[1] - cursor.getValue()[0]);
                recentAggregators.get(cursor.getTimeStamp()).addValue(data.getValue()[2] - cursor.getValue()[0]);
            }

            while (!recentData.isEmpty() && recentData.firstEntry().getValue().getTimeStamp() < (tNow - PositionFactory.getTimeSpan()))
            {
                int[] s = recentData.firstEntry().getValue().getSignal();
                long t =  recentData.firstEntry().getValue().getTimeStamp();

                if(recentAggregators.get(t).getMax() != null){

                    learner.addStateAction(s, recentAggregators.get(t).getMax());
                    learner.addStateAction(s, recentAggregators.get(t).getMin());
                    //learner.addStateAction(s, data.getValue()[3] - recentData.firstEntry().getValue().getValue()[3]);

                    recentAggregators.remove(t);
                    recentData.remove(t);
                    if(!recentPredictions.isEmpty()) {
                        removeFromPrediction(recentPredictions.removeFirst());
                    }
                }
                else {
                    break;
                }
            }

            recentData.put(data.getTimeStamp(), data);
            recentAggregators.put(data.getTimeStamp(), new MinMaxAggregator());

            if(!adapting){
                int[] signal = data.getSignal();
                TreeMap<Integer, Double> distribution = learner.getActionDistribution(signal);
                TreeMap<Double, Double> prediction = new TreeMap<Double, Double>();
                for(Map.Entry<Integer, Double> entry : distribution.entrySet())
                {
                    prediction.put(data.getValue()[0] + entry.getKey() * learner.getActionResolution(), entry.getValue());
                }
                recentPredictions.add(prediction);

                updateOverallPrediction(prediction);

                HashSet<OpenPosition> closed = new HashSet<OpenPosition>();
                for(OpenPosition position : positions)
                {
                    if(position.canCloseOnBar_Pessimistic(tNow, data.getValue()[1], data.getValue()[2], data.getValue()[0]))
                    {
                        closed.add(position);
                        int x = (int) (position.getTarget()/learner.getActionResolution());
                        int y = (int) (position.getPnL()/learner.getActionResolution());
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

                    int dir = -1;
                    if(position.isLong()){
                        dir = 1;
                    }
                    //recentPos.put(data.getTimeStamp(), dir);
                }
            }
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
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean loadMemories(String folder, long time){
        if(new File(folder + "/LearnerService_" + time).exists()){
            learner = LearnerServiceBuilder.load(folder, time);
            saved = true;
            return true;
        }
        return false;
    }

    public LearnerService getLearner() {
        return learner;
    }

    public void setTolerance(double tolerance) {
        learner.setTolerance(tolerance);
    }

    public void setActionResolution(double actionResolution) {
        learner.setActionResolution(actionResolution);
    }

    public void setMaxPopulation(int maxPopulation) {
        learner.setMaxPopulation(maxPopulation);
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
        PositionFactory.setLive(true);
    }

    public void setBlackBox(BlackBox blackBox) {
        this.blackBox = blackBox;
    }

    public boolean isLive() {
        return inLiveTrading;
    }

    public void kill(){
        alive = false;
    }

    public TreeMap<Double, StateActionPair> getAlphas(){
        return learner.getAlphaStates();
    }

    public double[] getVarCorrelations(int[] state){
        return learner.updateAndGetCorrelationWeights(state);
    }

    public void setAdapting(boolean adapting) {
        this.adapting = adapting;
        learner.setCorrelating(adapting);
    }

    public TreeMap<Integer, CorrelationCalculator> getCorrelationCalculators(){
        return learner.getCorrelationCalculators();
    }

    public ClusteredCopulae getCopulae(){
        return learner.getCopulae();
    }

    public void setCorrelationTools(TreeMap<Integer, CorrelationCalculator> calculators, ClusteredCopulae copulae){
        learner.setCopulae(copulae);
        learner.setCorrelationCalculators(calculators);
    }
}
