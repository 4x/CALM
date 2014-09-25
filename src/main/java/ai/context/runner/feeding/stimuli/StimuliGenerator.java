package ai.context.runner.feeding.stimuli;

import ai.context.feed.synchronised.ISynchFeed;
import ai.context.runner.feeding.StateToAction;
import ai.context.util.configuration.PropertiesHolder;
import scala.actors.threadpool.Arrays;

import java.util.*;

public class StimuliGenerator {

    public List<StimuliHolder> holders = new ArrayList<>();
    private TreeMap<Double, StimuliHolder> ranked = new TreeMap<>();

    public void process(List<StateToAction> series, ISynchFeed motherFeed, Long[] horizons){
        Integer[] actionElements = new Integer[]{3, 1, 2, 0};
        Set<Integer> availableStimuli = new HashSet<>();
        for (int i = 0; i < motherFeed.getNumberOfOutputs(); i++) {
            availableStimuli.add(i);
        }
        availableStimuli.removeAll(Arrays.asList(actionElements));
        for(int i = 0; i < 20000; i++){
            Integer[] sigElements = new Integer[PropertiesHolder.coreStimuliPerNeuron];
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
        for(StateToAction stateToAction : series){
            if(Math.random() > 0.75) {
                for (StimuliHolder holder : holders) {
                    holder.feed(stateToAction);
                }
            }
            i++;

            if(i % 100 == 0){
                System.out.println("StimuliGen P1: " + i + "("+series.size()+")");
            }
        }

        i = 0;
        for(StateToAction stateToAction : series){
            if(Math.random() > 0.75) {
                for (StimuliHolder holder : holders) {
                    holder.reFeed(stateToAction);
                }
            }
            i++;
            if(i % 100 == 0){
                System.out.println("StimuliGen P2: " + i + "("+series.size()+")");
            }
        }

        for(StimuliHolder holder : holders){
            ranked.put(holder.getScore(), holder);
        }

        for(StimuliHolder holder : ranked.descendingMap().values()){
            System.out.println(Arrays.toString(holder.getSignalsSources()) + " -> " + holder.getHorizon());
        }
    }

    public List<StimuliHolder> getTop(int n){
        List<StimuliHolder> top = new ArrayList<>();
        int i = 0;
        for(StimuliHolder holder : ranked.descendingMap().values()){
            if(i > n){
                break;
            }

            top.add(holder);
            i++;
        }
        return top;
    }

    public void reset(){
        holders.clear();
        ranked.clear();
    }
}


