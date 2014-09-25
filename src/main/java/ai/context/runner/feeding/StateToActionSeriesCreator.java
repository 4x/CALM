package ai.context.runner.feeding;

import ai.context.feed.DataType;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.util.DataSetUtils;
import ai.context.util.configuration.PropertiesHolder;

import java.util.*;

public class StateToActionSeriesCreator {

    public static Feed priceFeed;
    public static double ask;
    public static double bid;

    public static Long[] horizons;

    public static List<StateToAction> createSeries(String path, long start, long end, int timeFrames){
        return StateToActionSeriesCreator.createSeries(null, path, start, end, timeFrames);
    }

    public static List<StateToAction> createSeries(ISynchFeed motherFeed, String path, long start, long end, int timeFrames){
        ArrayList<StateToAction> stateToActions = new ArrayList<>();
        Set<Watcher> watchers = new HashSet<>();

        if(motherFeed == null){
            motherFeed = MotherFeedCreator.getMotherFeed(path);
        }

        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        priceFeed = new CSVFeed(path + "feeds/" + PropertiesHolder.ticksFile, "yyyy.MM.dd HH:mm:ss.SSS", typesPrice, start - PropertiesHolder.timeQuantum);
        FeedObject pricePoint = null;

        TreeSet<Long> horizons = new TreeSet<>();

        while (true){
            FeedObject data = motherFeed.getNextComposite(null);
            long tStart = data.getTimeStamp() + PropertiesHolder.timeQuantum;
            long tEnd = tStart + PropertiesHolder.timeQuantum;

            if(tStart > end){
                break;
            }

            if(tStart >= start){
                List<Object> sig = new ArrayList<>();
                DataSetUtils.add(data.getData(), sig);
                int[] signal = new int[sig.size()];
                for(int i = 0; i < signal.length; i++){
                    int num = 0;
                    Object raw = sig.get(i);
                    if(raw instanceof Integer){
                        num = (Integer) raw;
                    }
                    signal[i] = num;
                }
                //System.out.println("Reached: " + new Date(tStart) + ": " + sig);
                if(ask != 0 && bid != 0){
                    StateToAction stateToAction = new StateToAction(tStart, signal);
                    stateToActions.add(stateToAction);
                    for(int i = 0; i < timeFrames; i++){
                        long horizon = (i + 1)*PropertiesHolder.timeQuantum;
                        horizons.add(horizon);
                        long tStop = tStart + horizon;
                        Watcher watcher = new Watcher(horizon, tStop, stateToAction, ask, bid);
                        watchers.add(watcher);
                    }
                }
                while(true){
                    if(pricePoint != null){
                        long t = pricePoint.getTimeStamp();
                        ask = (double) ((Object[])pricePoint.getData())[0];
                        bid = (double) ((Object[])pricePoint.getData())[1];
                        if(t > tEnd){
                            break;
                        }

                        if(t >= tStart){
                            HashSet<Watcher> toRemove = new HashSet<>();
                            for(Watcher watcher : watchers){
                                if(watcher.addPoint(t, ask, bid)){
                                    toRemove.add(watcher);
                                }
                            }
                            watchers.removeAll(toRemove);
                        }
                    }
                    pricePoint = priceFeed.readNext(null);
                }
            }
        }
        StateToActionSeriesCreator.horizons = new Long[horizons.size()];
        horizons.toArray(StateToActionSeriesCreator.horizons);
        return stateToActions;
    }
}

