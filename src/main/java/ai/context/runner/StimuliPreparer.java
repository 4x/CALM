package ai.context.runner;

import ai.context.feed.synchronised.ISynchFeed;
import ai.context.util.configuration.DynamicPropertiesLoader;
import ai.context.util.configuration.PropertiesHolder;
import ai.context.util.feeding.MotherFeedCreator;
import ai.context.util.feeding.StateToAction;
import ai.context.util.feeding.StateToActionSeriesCreator;
import ai.context.util.feeding.stimuli.StimuliGenerator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class StimuliPreparer {

    public static void main(String[] args){
        DynamicPropertiesLoader.start("");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String path = "/opt/dev/data/";
        int coreStimuli = 16;
        if (!(args == null || args.length == 0)) {
            path = args[0];
            if(args.length >= 2){
                coreStimuli = Integer.parseInt(args[1]);
            }
        }
        ISynchFeed motherFeed = MotherFeedCreator.getMotherFeed(path);

        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        long start = 0;
        try {
            start = format.parse(PropertiesHolder.startDateTime).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long end = start + (7L*365L*86400000L);
        List<StateToAction> series = StateToActionSeriesCreator.createSeries(motherFeed, path, start, end, 16);
        System.out.println("STA series created from: " + PropertiesHolder.startDateTime + " to: " + format.format(new Date(end)));
        StimuliGenerator stimuliGenerator = new StimuliGenerator();
        stimuliGenerator.process(series, motherFeed, StateToActionSeriesCreator.horizons, 50, coreStimuli);
        System.out.println("Top stimuli generated");
    }
}
