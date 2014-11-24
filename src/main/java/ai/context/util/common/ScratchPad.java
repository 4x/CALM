package ai.context.util.common;

import java.util.TreeMap;

public class ScratchPad {
    public static final String NEURON_RANKING_NAN = "NEURON_RANKING_NAN";
    public static final String MEAN_SKEW_NAN = "MEAN_SKEW_NAN";
    public static final String CURRENT_CORRELATION_NAN = "CURRENT_CORRELATION_NAN";
    public static final String NEURON_SCORE_NAN = "NEURON_SCORE_NAN";
    public static final String STIMULI_HOLDER_NAN = "STIMULI_HOLDER_NAN";

    public static TreeMap<String, Object> memory = new TreeMap<>();

    public static void incrementCountFor(String id){
        if(!ScratchPad.memory.containsKey(id)){
            ScratchPad.memory.put(id, new Count());
        }
        Count c = (Count) ScratchPad.memory.get(id);
        c.val++;
    }
}
