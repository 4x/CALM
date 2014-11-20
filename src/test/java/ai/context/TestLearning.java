package ai.context;

import ai.context.core.ai.LearnerService;
import ai.context.core.ai.LearningException;
import ai.context.util.analysis.SuccessMap;
import ai.context.util.measurement.LoggerTimer;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class TestLearning {

    private LearnerService learner = new LearnerService();

    private int numInputs = 8;
    private int degreesOfFreedom = 12;
    private int numPoints = 20000;

    private int numPredict = 1000;

    private double tolerance = 0.005;
    private double actionResolution = 1.0;
    private int maxPopulation = 1000;

    @Before
    public void setup() {
        LoggerTimer.turn(false);
        learner.setActionResolution(actionResolution);
        learner.setMaxPopulation(maxPopulation);
        learner.setTolerance(tolerance);

        long t = System.currentTimeMillis();
        int[] signal = null;
        for (int i = 0; i < numPoints; i++) {
            signal = getRandomSignal();

            double movement = getBlackBoxMovement(signal);
            try {
                learner.addStateAction(signal, movement);
            } catch (LearningException e) {
                e.printStackTrace();
            }
            System.out.println("Learned: point " + i + ": Signal: " + getStringFromSignal(signal) + " Movement: " + movement);
        }

        System.out.println(learner.getPopulation().size() + " Time: " + (System.currentTimeMillis() - t));
        //LearnerServiceBuilder.save(learner, "src/test/resources", 1);
    }

    @Test
    public void testLearning() {
        //learner = LearnerServiceBuilder.load("src/test/resources", 1);

        TreeMap<Integer, TreeMap<Integer, Double>> successMap = new TreeMap<Integer, TreeMap<Integer, Double>>();
        TreeSet<Integer> ySet = new TreeSet<Integer>();

        for (int iTest = 0; iTest < numPredict; iTest++) {
            int[] signal = getRandomSignal();
            TreeMap<Integer, Double> distribution = learner.getActionDistribution(signal);

            int observed = (int) getBlackBoxMovement(signal) / 10;

            if (!successMap.containsKey(observed)) {
                successMap.put(observed, new TreeMap<Integer, Double>());
            }

            Map<Integer, Double> map = successMap.get(observed);
            for (int i : distribution.keySet()) {
                int expected = i / 10;
                if (!map.containsKey(expected)) {
                    map.put(expected, 0.0);
                }

                map.put(expected, map.get(expected) + distribution.get(i));
                ySet.add(expected);
            }
        }
        new SuccessMap(ySet, successMap);
    }

    private int[] getRandomSignal() {
        int[] signal = new int[numInputs];

        for (int v = 0; v < numInputs; v++) {
            signal[v] = (int) (Math.random() * degreesOfFreedom);
        }
        return signal;
    }

    private double getBlackBoxMovement(int[] signal) {
        double movement = 0.0;

        boolean neg = false;

        int index = 0;

        for (int val : signal) {
            if (index % 2 == 0) {
                movement += val;
            } else {
                movement -= val;
            }

            /*if(index == 12 && val > 5){
                movement =  movement * Math.cos(movement);
            }
            else {
                movement =  movement * Math.sin(movement);
            }

            if(index == 10 && val > 5){
                movement =  Math.sqrt(Math.abs(movement));
            }

            if(index == 4 && val > 5){
                neg = true;
            }

            *//*if(index == 5 && movement < 0){
                movement = Math.pow(Math.sin(movement), 2);
            }
            else {
                movement =  Math.pow(Math.cos(movement), 3);
            }*//*

            if(index == 3 && val > 5){
                neg = !neg;
            }*/

            index++;
        }

        /*if(neg == true){
            movement = -Math.abs(movement);
        }
        else {
            movement = +Math.abs(movement);
        }*/

        movement = movement * (0.9 + (0.2) * Math.random());
        movement = movement * 100;
        return movement;
    }

    public static void main(String[] args) {
        TestLearning test = new TestLearning();
        test.setup();
        test.testLearning();
    }

    public String getStringFromSignal(int[] signal) {
        String sig = "[";

        for (int i : signal) {
            sig += i + ",";
        }

        if (sig.length() > 1) {
            sig = sig.substring(0, sig.length() - 1);
        }

        sig += "]";

        return sig;
    }
}
