package ai.context.runner;

import ai.context.feed.DataType;
import ai.context.feed.FeedObject;
import ai.context.feed.row.CSVFeed;
import ai.context.feed.surgical.ExtractOneFromListFeed;
import ai.context.feed.surgical.FXHLDiffFeed;
import ai.context.feed.surgical.FXModuloFeed;
import ai.context.feed.synchronised.MinMaxAggregatorDiscretiser;
import ai.context.feed.synchronised.SynchFeed;
import ai.context.feed.synchronised.SynchronisedFeed;
import ai.context.feed.transformer.compound.AmplitudeWavelengthTransformer;
import ai.context.feed.transformer.compound.SubtractTransformer;
import ai.context.feed.transformer.filtered.RowBasedTransformer;
import ai.context.feed.transformer.series.online.CrossingSeriesOnlineTransformer;
import ai.context.feed.transformer.series.online.MinMaxDistanceTransformer;
import ai.context.feed.transformer.series.online.RSIOnlineTransformer;
import ai.context.feed.transformer.series.online.StandardDeviationOnlineTransformer;
import ai.context.feed.transformer.single.TimeVariablesAppenderFeed;
import ai.context.feed.transformer.single.unpadded.LinearDiscretiser;
import ai.context.feed.transformer.single.unpadded.LogarithmicDiscretiser;
import ai.context.learning.Learner;
import ai.context.learning.neural.NeuralLearner;
import ai.context.learning.neural.NeuronCluster;
import scala.actors.threadpool.Arrays;

import java.util.*;

public class MainNeural {
    public static void main(String[] args) {
        MainNeural test = new MainNeural();
        String path = "/opt/dev/data/";
        if(!(args == null || args.length == 0)){
            path = args[0];
        }
        test.setup(path);
    }


    public void setup(String path){

        SynchFeed motherFeed = initFeed(path, null);
        NeuronCluster.getInstance().setMotherFeed(motherFeed);

        long[] horizonRange = new long[]{60 * 60 * 1000L, 24 * 60 * 60 * 1000L};
        Integer[] actionElements = new Integer[]{3, 1, 2, 0};
        long outputFutureOffset = 5 * 60 * 1000L;
        double resolution = 0.0005;
        Set<Integer> availableStimuli = new HashSet<>();
        for(int i = 0 ; i < motherFeed.getNumberOfOutputs(); i ++){
            availableStimuli.add(i);
        }
        availableStimuli.removeAll(Arrays.asList(actionElements));

        Set<NeuralLearner> seeds = new HashSet<>();
        for(int i = 0 ; i < 5; i++){
            Integer[] sigElements = new Integer[6];
            for(int sig = 0; sig < sigElements.length; sig++){
                List<Integer> available = new ArrayList<>(availableStimuli);
                int chosenSig = available.get((int) (Math.random() * available.size()));
                availableStimuli.remove(chosenSig);
                sigElements[sig] = chosenSig;
            }
            seeds.add(new NeuralLearner(horizonRange, motherFeed, actionElements, sigElements, outputFutureOffset, resolution));
        }
        for(NeuralLearner seed : seeds){
            NeuronCluster.getInstance().start(seed);
        }
        NeuronCluster.getInstance().startServer();
    }

    private SynchFeed initFeed(String path, Learner learner){
        DataType[] typesCalendar = new DataType[]{
                DataType.OTHER,
                DataType.OTHER,
                DataType.INTEGER,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE,
                DataType.EXTRACTABLE_DOUBLE};

        String dateFC = "20080101 00:00:00";


        long interval = 1*60000L;
        CSVFeed feedCalendar = new CSVFeed(path + "feeds/Calendar_2008.csv", "yyyyMMdd HH:mm:ss", typesCalendar, dateFC);
        feedCalendar.setPaddable(true);
        feedCalendar.setInterval(interval);
        RowBasedTransformer f1 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0}, new String[]{"Nonfarm Payrolls"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f1);

        RowBasedTransformer f2 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Consumer Price Index \\(MoM\\)", "United Kingdom"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f2);
        RowBasedTransformer f3 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Consumer Price Index \\(MoM\\)", "United States"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f3);
        RowBasedTransformer f4 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Consumer Price Index \\(MoM\\)", "Germany"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f4);

        RowBasedTransformer f5 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Producer Price Index \\(MoM\\)", "European Monetary Union"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f5);
        RowBasedTransformer f6 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Producer Price Index \\(MoM\\)", "United States"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f6);
        RowBasedTransformer f7 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Retail Price Index \\(MoM\\)", "United Kingdom"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f7);
        RowBasedTransformer f8 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Manufacturing Production \\(MoM\\)", "United Kingdom"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f8);
        RowBasedTransformer f9 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Producer Price Index \\(MoM\\)", "Germany"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f9);
        RowBasedTransformer f10 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"BoE Interest Rate Decision", "United Kingdom"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f10);
        RowBasedTransformer f11 = new RowBasedTransformer(feedCalendar, 4L*60L*60L*1000L,  new int[]{0, 1}, new String[]{"Fed Interest Rate Decision", "United States"}, new int[]{3, 4, 5}, learner);
        feedCalendar.addChild(f11);


        DataType[] typesPrice = new DataType[]{
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE,
                DataType.DOUBLE};

        String dateFP = "2008.01.01 00:00:00";

        CSVFeed feedPriceEUR = new CSVFeed(path + "feeds/EURUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  dateFP);
        CSVFeed feedPriceGBP = new CSVFeed(path + "feeds/GBPUSD.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  dateFP);
        CSVFeed feedPriceCHF = new CSVFeed(path + "feeds/USDCHF.csv", "yyyy.MM.dd HH:mm:ss", typesPrice,  dateFP);


        SynchFeed feed = buildSynchFeed(null, feedPriceEUR);
        feed = buildSynchFeed(feed, feedPriceGBP);
        feed = buildSynchFeed(feed, feedPriceCHF);

        //SmartDiscretiserOnSynchronisedFeed sFeed = new SmartDiscretiserOnSynchronisedFeed(feed, 5000, 5);
        MinMaxAggregatorDiscretiser sFeed = new MinMaxAggregatorDiscretiser(feed, 5000, 10);
        sFeed.lock();

        feed.addChild(sFeed);
        TimeVariablesAppenderFeed tFeed = new TimeVariablesAppenderFeed(sFeed);
        sFeed.addChild(tFeed);

        /*feed = addToSynchFeed(feed, f1, 25, 100);
        feed = addToSynchFeed(feed, f2, 0.1, 0);
        feed = addToSynchFeed(feed, f3, 0.1, 0);
        feed = addToSynchFeed(feed, f4, 0.1, 0);
        feed = addToSynchFeed(feed, f5, 0.1, 0);
        feed = addToSynchFeed(feed, f6, 0.1, 0);
        feed = addToSynchFeed(feed, f7, 0.1, 0);
        feed = addToSynchFeed(feed, f8, 0.1, 0);
        feed = addToSynchFeed(feed, f9, 0.1, 0);
        feed = addToSynchFeed(feed, f10, 0.1, 0);
        feed = addToSynchFeed(feed, f11, 0.1, 0);*/
        SynchFeed synchFeed = new SynchFeed();
        synchFeed.addRawFeed(feedPriceEUR);
        synchFeed.addRawFeed(tFeed);

        int i = 0;
        while (true){
            FeedObject data = synchFeed.getNextComposite(this);
            i++;

            if(i  == 5000) {
                break;
            }

            System.out.println(new Date(data.getTimeStamp()) + " " + data);
        }
        return synchFeed;
    }

    private SynchFeed buildSynchFeed(SynchFeed synch, CSVFeed ... feeds) {
        if(synch == null){
            synch = new SynchFeed();
        }
        for(CSVFeed feed : feeds){
            ExtractOneFromListFeed feedH = new ExtractOneFromListFeed(feed, 1);
            feed.addChild(feedH);
            ExtractOneFromListFeed feedL = new ExtractOneFromListFeed(feed, 2);
            feed.addChild(feedL);
            ExtractOneFromListFeed feedC = new ExtractOneFromListFeed(feed, 3);
            feed.addChild(feedC);
            ExtractOneFromListFeed feedV = new ExtractOneFromListFeed(feed, 4);
            feed.addChild(feedV);

            /*DeltaOnlineTransformer dH = new DeltaOnlineTransformer(1, feedH);
            DeltaOnlineTransformer dL = new DeltaOnlineTransformer(1, feedH);
            DeltaOnlineTransformer dC = new DeltaOnlineTransformer(1, feedH);
            DeltaOnlineTransformer dV = new DeltaOnlineTransformer(1, feedH);

            DeltaOnlineTransformer dH1 = new DeltaOnlineTransformer(2, feedH);
            DeltaOnlineTransformer dL1 = new DeltaOnlineTransformer(2, feedH);
            DeltaOnlineTransformer dC1 = new DeltaOnlineTransformer(2, feedH);
            DeltaOnlineTransformer dV1 = new DeltaOnlineTransformer(2, feedH);

            DeltaOnlineTransformer dH2 = new DeltaOnlineTransformer(4, feedH);
            DeltaOnlineTransformer dL2 = new DeltaOnlineTransformer(4, feedH);
            DeltaOnlineTransformer dC2 = new DeltaOnlineTransformer(4, feedH);
            DeltaOnlineTransformer dV2 = new DeltaOnlineTransformer(4, feedH);*/

            //DeltaOnlineTransformer dH3 = new DeltaOnlineTransformer(8, feedH);
            //DeltaOnlineTransformer dL3 = new DeltaOnlineTransformer(8, feedH);
            //DeltaOnlineTransformer dC3 = new DeltaOnlineTransformer(8, feedH);
            //DeltaOnlineTransformer dV3 = new DeltaOnlineTransformer(8, feedH);

            //DeltaOnlineTransformer dH4 = new DeltaOnlineTransformer(16, feedH);
            //DeltaOnlineTransformer dL4 = new DeltaOnlineTransformer(16, feedH);
            //DeltaOnlineTransformer dC4 = new DeltaOnlineTransformer(16, feedH);
            //DeltaOnlineTransformer dV4 = new DeltaOnlineTransformer(16, feedH);

            //DeltaOnlineTransformer dH5 = new DeltaOnlineTransformer(32, feedH);
            //DeltaOnlineTransformer dL5 = new DeltaOnlineTransformer(32, feedH);
            //DeltaOnlineTransformer dC5 = new DeltaOnlineTransformer(32, feedH);
            //DeltaOnlineTransformer dV5 = new DeltaOnlineTransformer(32, feedH);

            //DeltaOnlineTransformer dH6 = new DeltaOnlineTransformer(64, feedH);
            //DeltaOnlineTransformer dL6 = new DeltaOnlineTransformer(64, feedH);
            //DeltaOnlineTransformer dC6 = new DeltaOnlineTransformer(64, feedH);
            //DeltaOnlineTransformer dV6 = new DeltaOnlineTransformer(64, feedH);

            //DeltaOnlineTransformer dH7 = new DeltaOnlineTransformer(128, feedH);
            //DeltaOnlineTransformer dL7 = new DeltaOnlineTransformer(128, feedH);

            //DeltaOnlineTransformer dH8 = new DeltaOnlineTransformer(256, feedH);
            //DeltaOnlineTransformer dL8 = new DeltaOnlineTransformer(256, feedH);

            FXHLDiffFeed feedDiff = new FXHLDiffFeed(feed, 0.0001);
            feed.addChild(feedDiff);

            FXModuloFeed feedModulo = new FXModuloFeed(feed, 0.0001, 100);
            feed.addChild(feedModulo);

            StandardDeviationOnlineTransformer stdFeedH = new StandardDeviationOnlineTransformer(12, feedH);
            StandardDeviationOnlineTransformer stdFeedL = new StandardDeviationOnlineTransformer(12, feedL);
            StandardDeviationOnlineTransformer stdFeedV = new StandardDeviationOnlineTransformer(12, feedV);

            LogarithmicDiscretiser stdLH1 = new LogarithmicDiscretiser(0.00001, 0, stdFeedH, -1);
            LogarithmicDiscretiser stdLL1 = new LogarithmicDiscretiser(0.00001, 0, stdFeedL, -1);
            LogarithmicDiscretiser stdLV1 = new LogarithmicDiscretiser(0.00001, 0, stdFeedV, -1);


            AmplitudeWavelengthTransformer awFeedH = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedV = new AmplitudeWavelengthTransformer(feedV, stdFeedV, 2, 0.5);
            /*AmplitudeWavelengthTransformer awFeedH2 = new AmplitudeWavelengthTransformer(feedH, stdFeedH, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2 = new AmplitudeWavelengthTransformer(feedL, stdFeedL, 4, 0.5);*/

            StandardDeviationOnlineTransformer stdFeedH2 = new StandardDeviationOnlineTransformer(50, feedH);
            StandardDeviationOnlineTransformer stdFeedL2 = new StandardDeviationOnlineTransformer(50, feedL);
            //StandardDeviationOnlineTransformer stdFeedV2 = new StandardDeviationOnlineTransformer(50, feedV);

            AmplitudeWavelengthTransformer awFeedH50 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL50 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 2, 0.5);
            //AmplitudeWavelengthTransformer awFeedV50 = new AmplitudeWavelengthTransformer(feedV, stdFeedV2, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedH2_50 = new AmplitudeWavelengthTransformer(feedH, stdFeedH2, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2_50 = new AmplitudeWavelengthTransformer(feedL, stdFeedL2, 4, 0.5);

            StandardDeviationOnlineTransformer stdFeedH3 = new StandardDeviationOnlineTransformer(400, feedH);
            StandardDeviationOnlineTransformer stdFeedL3 = new StandardDeviationOnlineTransformer(400, feedL);

            LogarithmicDiscretiser stdLH3 = new LogarithmicDiscretiser(0.00001, 0, stdFeedH3, -1);
            LogarithmicDiscretiser stdLL3 = new LogarithmicDiscretiser(0.00001, 0, stdFeedL3, -1);
            //StandardDeviationOnlineTransformer stdFeedV3 = new StandardDeviationOnlineTransformer(100, feedV);

            AmplitudeWavelengthTransformer awFeedH100 = new AmplitudeWavelengthTransformer(feedH, stdFeedH3, 2, 0.5);
            AmplitudeWavelengthTransformer awFeedL100 = new AmplitudeWavelengthTransformer(feedL, stdFeedL3, 2, 0.5);
            //AmplitudeWavelengthTransformer awFeedV100 = new AmplitudeWavelengthTransformer(feedV, stdFeedV3, 2, 0.5);
            /*AmplitudeWavelengthTransformer awFeedH2_100 = new AmplitudeWavelengthTransformer(feedH, stdFeedH3, 4, 0.5);
            AmplitudeWavelengthTransformer awFeedL2_100 = new AmplitudeWavelengthTransformer(feedL, stdFeedL3, 4, 0.5);*/


            /*RSIOnlineTransformer rsiH = new RSIOnlineTransformer(feedH, 5, 5, 0.5);
            RSIOnlineTransformer rsiL = new RSIOnlineTransformer(feedL, 5, 5, 0.5);

            ExtractOneFromListFeed kRSIH1 = new ExtractOneFromListFeed(rsiH, 1);
            rsiH.addChild(kRSIH1);
            ExtractOneFromListFeed dRSIH1 = new ExtractOneFromListFeed(rsiH, 2);
            rsiH.addChild(dRSIH1);
            ExtractOneFromListFeed kRSIL1 = new ExtractOneFromListFeed(rsiL, 1);
            rsiL.addChild(kRSIL1);
            ExtractOneFromListFeed dRSIL1 = new ExtractOneFromListFeed(rsiL, 2);
            rsiL.addChild(dRSIL1);
            CrossingSeriesOnlineTransformer crossH1 = new CrossingSeriesOnlineTransformer(kRSIH1, dRSIH1, 10);
            CrossingSeriesOnlineTransformer crossL1 = new CrossingSeriesOnlineTransformer(kRSIL1, dRSIL1, 10);*/

            RSIOnlineTransformer rsiH3 = new RSIOnlineTransformer(feedH, 5, 20, 0.1);
            RSIOnlineTransformer rsiL3 = new RSIOnlineTransformer(feedL, 5, 20, 0.1);

            ExtractOneFromListFeed kRSIH3 = new ExtractOneFromListFeed(rsiH3, 1);
            rsiH3.addChild(kRSIH3);
            ExtractOneFromListFeed dRSIH3 = new ExtractOneFromListFeed(rsiH3, 2);
            rsiH3.addChild(dRSIH3);
            ExtractOneFromListFeed kRSIL3 = new ExtractOneFromListFeed(rsiL3, 1);
            rsiL3.addChild(kRSIL3);
            ExtractOneFromListFeed dRSIL3 = new ExtractOneFromListFeed(rsiL3, 2);
            rsiL3.addChild(dRSIL3);
            CrossingSeriesOnlineTransformer crossH3 = new CrossingSeriesOnlineTransformer(kRSIH3, dRSIH3, 10);
            CrossingSeriesOnlineTransformer crossL3 = new CrossingSeriesOnlineTransformer(kRSIL3, dRSIL3, 10);

            /*RSIOnlineTransformer rsiH2 = new RSIOnlineTransformer(feedH, 5, 40, 0.05);
            RSIOnlineTransformer rsiL2 = new RSIOnlineTransformer(feedL, 5, 40, 0.05);

            ExtractOneFromListFeed kRSIH2 = new ExtractOneFromListFeed(rsiH2, 1);
            rsiH2.addChild(kRSIH2);
            ExtractOneFromListFeed dRSIH2 = new ExtractOneFromListFeed(rsiH2, 2);
            rsiH2.addChild(dRSIH2);
            ExtractOneFromListFeed kRSIL2 = new ExtractOneFromListFeed(rsiL2, 1);
            rsiL2.addChild(kRSIL2);
            ExtractOneFromListFeed dRSIL2 = new ExtractOneFromListFeed(rsiL2, 2);
            rsiL2.addChild(dRSIL2);
            CrossingSeriesOnlineTransformer crossH2 = new CrossingSeriesOnlineTransformer(kRSIH2, dRSIH2, 10);
            CrossingSeriesOnlineTransformer crossL2 = new CrossingSeriesOnlineTransformer(kRSIL2, dRSIL2, 10);*/

            RSIOnlineTransformer rsiH5 = new RSIOnlineTransformer(feedH, 5, 10, 0.01);
            RSIOnlineTransformer rsiL5 = new RSIOnlineTransformer(feedL, 5, 10, 0.01);

            ExtractOneFromListFeed kRSIH5 = new ExtractOneFromListFeed(rsiH5, 1);
            rsiH5.addChild(kRSIH5);
            ExtractOneFromListFeed dRSIH5 = new ExtractOneFromListFeed(rsiH5, 2);
            rsiH5.addChild(dRSIH5);
            ExtractOneFromListFeed kRSIL5 = new ExtractOneFromListFeed(rsiL5, 1);
            rsiL5.addChild(kRSIL5);
            ExtractOneFromListFeed dRSIL5 = new ExtractOneFromListFeed(rsiL5, 2);
            rsiL5.addChild(dRSIL5);
            CrossingSeriesOnlineTransformer crossH5 = new CrossingSeriesOnlineTransformer(kRSIH5, dRSIH5, 10);
            CrossingSeriesOnlineTransformer crossL5 = new CrossingSeriesOnlineTransformer(kRSIL5, dRSIL5, 10);

            /*RSIOnlineTransformer rsiH4 = new RSIOnlineTransformer(feedH, 5, 10, 0.2);
            RSIOnlineTransformer rsiL4 = new RSIOnlineTransformer(feedL, 5, 10, 0.2);

            ExtractOneFromListFeed kRSIH4 = new ExtractOneFromListFeed(rsiH4, 1);
            rsiH4.addChild(kRSIH4);
            ExtractOneFromListFeed dRSIH4 = new ExtractOneFromListFeed(rsiH4, 2);
            rsiH4.addChild(dRSIH4);
            ExtractOneFromListFeed kRSIL4 = new ExtractOneFromListFeed(rsiL4, 1);
            rsiL4.addChild(kRSIL4);
            ExtractOneFromListFeed dRSIL4 = new ExtractOneFromListFeed(rsiL4, 2);
            rsiL4.addChild(dRSIL4);
            CrossingSeriesOnlineTransformer crossH4 = new CrossingSeriesOnlineTransformer(kRSIH4, dRSIH4, 10);
            CrossingSeriesOnlineTransformer crossL4 = new CrossingSeriesOnlineTransformer(kRSIL4, dRSIL4, 10);*/

            MinMaxDistanceTransformer mmdT1 = new MinMaxDistanceTransformer(50, feedL, feedH, feedC);
            MinMaxDistanceTransformer mmdT2 = new MinMaxDistanceTransformer(100, feedL, feedH, feedC);
            MinMaxDistanceTransformer mmdT3 = new MinMaxDistanceTransformer(200, feedL, feedH, feedC);
            MinMaxDistanceTransformer mmdT4 = new MinMaxDistanceTransformer(400, feedL, feedH, feedC);
            MinMaxDistanceTransformer mmdT5 = new MinMaxDistanceTransformer(800, feedL, feedH, feedC);

            /*synch = new SynchronisedFeed(feedH, synch);
            synch = new SynchronisedFeed(feedL, synch);
            synch = new SynchronisedFeed(feedC, synch);*/
            synch.addRawFeed(feedV);

            synch.addRawFeed(mmdT1);
            synch.addRawFeed(mmdT2);
            synch.addRawFeed(mmdT3);
            synch.addRawFeed(mmdT4);
            synch.addRawFeed(mmdT5);

            /*RadarOnlineTransformer r1 = new RadarOnlineTransformer(100, feedL, feedH, feedC, 0.0001);
            RadarOnlineTransformer r2 = new RadarOnlineTransformer(200, feedL, feedH, feedC, 0.0001);
            RadarOnlineTransformer r3 = new RadarOnlineTransformer(400, feedL, feedH, feedC, 0.0001);
            RadarOnlineTransformer r4 = new RadarOnlineTransformer(800, feedL, feedH, feedC, 0.0001);

            synch.addRawFeed(r1);
            synch.addRawFeed(r2);
            synch.addRawFeed(r3);
            synch.addRawFeed(r4);*/

            /*synch.addRawFeed(dH);
            synch.addRawFeed(dL);
            synch.addRawFeed(dC);
            synch.addRawFeed(dV);

            synch.addRawFeed(dH1);
            synch.addRawFeed(dL1);
            synch.addRawFeed(dC1);
            synch.addRawFeed(dV1);

            synch.addRawFeed(dH2);
            synch.addRawFeed(dL2);
            synch.addRawFeed(dC2);
            synch.addRawFeed(dV2);*/

            //synch.addRawFeed(dH3);
            //synch.addRawFeed(dL3);
            //synch.addRawFeed(dC3);
            //synch.addRawFeed(dV3);

            //synch.addRawFeed(dH4);
            //synch.addRawFeed(dL4);
            //synch.addRawFeed(dC4);
            //synch.addRawFeed(dV4);

            //synch.addRawFeed(dH5);
            //synch.addRawFeed(dL5);
            //synch.addRawFeed(dC5);
            //synch.addRawFeed(dV5);

            //synch.addRawFeed(dH6);
            //synch.addRawFeed(dL6);
            //synch.addRawFeed(dC6);
            //synch.addRawFeed(dV6);

            synch.addRawFeed(feedDiff);
            synch.addRawFeed(feedModulo);

            synch.addRawFeed(stdLH1);
            synch.addRawFeed(stdLL1);
            synch.addRawFeed(stdLV1);

            /*synch.addRawFeed(stdFeedH2);
            synch.addRawFeed(stdFeedL2);
            synch.addRawFeed(stdFeedV2);*/

            synch.addRawFeed(stdLH3);
            synch.addRawFeed(stdLL3);
            //synch.addRawFeed(stdFeedV3);

            synch.addRawFeed(awFeedH);
            synch.addRawFeed(awFeedL);
            synch.addRawFeed(awFeedV);
            //synch.addRawFeed(awFeedH2);
            //synch.addRawFeed(awFeedL2);

            synch.addRawFeed(awFeedH50);
            synch.addRawFeed(awFeedL50);
            //synch.addRawFeed(awFeedV50);
            synch.addRawFeed(awFeedH2_50);
            synch.addRawFeed(awFeedL2_50);

            synch.addRawFeed(awFeedH100);
            synch.addRawFeed(awFeedL100);
            //synch.addRawFeed(awFeedV100);
            //synch.addRawFeed(awFeedH2_100);
            //synch.addRawFeed(awFeedL2_100);

            /*synch.addRawFeed(rsiH);
            synch.addRawFeed(rsiL);
            synch.addRawFeed(crossH1);
            synch.addRawFeed(crossL1);*/
            /*synch.addRawFeed(rsiH2);
            synch.addRawFeed(rsiL2);
            synch.addRawFeed(crossH2);
            synch.addRawFeed(crossL2);*/
            synch.addRawFeed(rsiH3);
            synch.addRawFeed(rsiL3);
            synch.addRawFeed(crossH3);
            synch.addRawFeed(crossL3);
            /*synch.addRawFeed(rsiH4);
            synch.addRawFeed(rsiL4);
            synch.addRawFeed(crossH4);
            synch.addRawFeed(crossL4);*/
            synch.addRawFeed(rsiH5);
            synch.addRawFeed(rsiL5);
            synch.addRawFeed(crossH5);
            synch.addRawFeed(crossL5);
        }
        return synch;
    }

    private SynchronisedFeed addToSynchFeed(SynchronisedFeed feed, RowBasedTransformer raw, double resolution, double benchmark){

        LinearDiscretiser l0 = new LinearDiscretiser(resolution, benchmark, raw, 0);
        raw.addChild(l0);
        feed = new SynchronisedFeed(l0, feed);

        ExtractOneFromListFeed e1 = new ExtractOneFromListFeed(raw, 0);
        raw.addChild(e1);
        ExtractOneFromListFeed e2 = new ExtractOneFromListFeed(raw, 1);
        raw.addChild(e2);
        ExtractOneFromListFeed e3 = new ExtractOneFromListFeed(raw, 2);
        raw.addChild(e3);

        SubtractTransformer s1 = new SubtractTransformer(e1, e2);
        e1.addChild(s1);
        e2.addChild(s1);

        SubtractTransformer s2 = new SubtractTransformer(e1, e3);
        e1.addChild(s2);
        e3.addChild(s2);

        LinearDiscretiser l1 = new LinearDiscretiser(resolution, benchmark, s1, 0);
        s1.addChild(l1);
        feed = new SynchronisedFeed(l1, feed);

        LinearDiscretiser l2 = new LinearDiscretiser(resolution, benchmark, s2, 0);
        s2.addChild(l2);
        feed = new SynchronisedFeed(l2, feed);

        LinearDiscretiser l3 = new LinearDiscretiser(0.1, 0, raw, 3);
        raw.addChild(l3);
        feed = new SynchronisedFeed(l3, feed);

        return feed;
    }
}
