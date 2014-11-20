package ai.context.util.feeding.stimuli;

import ai.context.feed.synchronised.ISynchFeed;
import ai.context.util.feeding.StateToAction;
import scala.actors.threadpool.Arrays;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StimuliGenerator {

    private int candidatesPerPass = 10000;
    private int threads = 8;
    private ExecutorService pool = Executors.newFixedThreadPool(threads);
    private List<StimuliHolder>[] holdersArr = new ArrayList[threads];
    private TreeMap<Double, StimuliInformation> ranked = new TreeMap<>();

    public void process(final List<StateToAction> series, final ISynchFeed motherFeed, final Long[] horizons, int passes, final int coreStimuli){
        for(int pass = 0; pass < passes; pass++) {
            final CountDownLatch latch = new CountDownLatch(threads);
            for(int t = 0; t < threads; t++) {
                final int finalT = t;
                final int finalPass = pass;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        List<StimuliHolder> holders = holdersArr[finalT] = new ArrayList<StimuliHolder>();
                        Integer[] actionElements = new Integer[]{3, 1, 2, 0};
                        Set<Integer> availableStimuli = new HashSet<>();
                        for (int i = 0; i < motherFeed.getNumberOfOutputs(); i++) {
                            availableStimuli.add(i);
                        }
                        availableStimuli.removeAll(Arrays.asList(actionElements));
                        for (int i = 0; i < 10000/threads; i++) {
                            Integer[] sigElements = new Integer[coreStimuli];
                            for (int sig = 0; sig < sigElements.length; sig++) {
                                if (availableStimuli.isEmpty()) {
                                    for (int index = 0; index < motherFeed.getNumberOfOutputs(); index++) {
                                        availableStimuli.add(index);
                                    }
                                    availableStimuli.removeAll(Arrays.asList(actionElements));
                                }
                                List<Integer> available = new ArrayList<>(availableStimuli);
                                int chosenSig = available.get((int) (Math.random() * available.size()));
                                availableStimuli.remove(chosenSig);
                                sigElements[sig] = chosenSig;
                            }

                            holders.add(new StimuliHolder(sigElements, horizons[((int) Math.min((Math.random() * horizons.length), horizons.length - 1))]));
                        }

                        int i = 0;
                        for (StateToAction stateToAction : series) {
                            if (Math.random() > 0.75) {
                                for (StimuliHolder holder : holders) {
                                    holder.feed(stateToAction);
                                }
                            }
                            i++;

                            if (i % 100 == 0) {
                                System.out.println("[" + finalPass + "," + finalT + "]" + "StimuliGen P1: " + i + "(" + series.size() + ")");
                            }
                        }

                        i = 0;
                        for (StateToAction stateToAction : series) {
                            if (Math.random() > 0.75) {
                                for (StimuliHolder holder : holders) {
                                    holder.reFeed(stateToAction);
                                }
                            }
                            i++;
                            if (i % 100 == 0) {
                                System.out.println("[" + finalPass + "," + finalT + "]" + "StimuliGen P2: " + i + "(" + series.size() + ")");
                            }
                        }

                        for (StimuliHolder holder : holders) {
                            StimuliInformation information = new StimuliInformation(holder.getSignalsSources(), holder.getHorizon(), holder.getScore());
                            ranked.put(holder.getScore(), information);
                        }
                        latch.countDown();
                    }
                };
                pool.execute(r);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter("Signals_" + coreStimuli + "_CoreStimuli");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for(StimuliInformation holder : ranked.descendingMap().values()){
            System.out.println(Arrays.toString(holder.getSignalsSources()) + " -> " + holder.getHorizon());

            String toPrint = "" + holder.getHorizon();
            for(int sig : holder.getSignalsSources()){
                toPrint += "," + sig;
            }
            writer.println(toPrint);
        }
        writer.close();
    }

    public List<StimuliInformation> getTop(int n){
        List<StimuliInformation> top = new ArrayList<>();
        int i = 0;
        for(StimuliInformation holder : ranked.descendingMap().values()){
            if(i > n){
                break;
            }

            top.add(holder);
            i++;
        }
        return top;
    }

    public void reset(){
        ranked.clear();
    }
}


