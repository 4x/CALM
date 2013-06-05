package ai.context;

import ai.context.core.LearnerService;
import ai.context.util.analysis.SuccessMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class TestLearning {

    private LearnerService learner = new LearnerService();

    private int numInputs = 20;
    private int degreesOfFreedom = 5;
    private int numPoints = 10000;

    private int numPredict = 10000;

    private int tolerance = 5;
    private double actionResolution = 1.0;
    private int maxPopulation = 200;

    @Before
    public void setup()
    {
        learner.setActionResolution(actionResolution);
        learner.setMaxPopulation(maxPopulation);
        learner.setTolerance(tolerance);

        for(int i = 0; i < numPoints; i++)
        {
            int[] signal = getRandomSignal();

            double movement = getBlackBoxMovement(signal);
            learner.addStateAction(signal, movement);
            System.out.println("Learned: point " + i + ": " + movement);
        }
    }

    @Test
    public void testLearning()
    {
        TreeMap<Integer, TreeMap<Integer, Double>> successMap = new TreeMap<Integer, TreeMap<Integer, Double>>();
        TreeSet<Integer> ySet = new TreeSet<Integer>();

        for(int iTest = 0; iTest < numPredict; iTest++)
        {
            int[] signal = getRandomSignal();
            TreeMap<Integer, Double> distribution =learner.getActionDistribution(signal);

            int observed = (int) getBlackBoxMovement(signal) / 10;

            if(!successMap.containsKey(observed))
            {
                successMap.put(observed, new TreeMap<Integer, Double>());
            }

            Map<Integer, Double> map = successMap.get(observed);
            for(int i : distribution.keySet())
            {
                int expected = i/10;
                if(!map.containsKey(expected))
                {
                    map.put(expected, 0.0);
                }

                map.put(expected, map.get(expected) + distribution.get(i));
                ySet.add(expected);
            }
        }
        new SuccessMap(ySet, successMap);
    }

    private int[] getRandomSignal()
    {
        int[] signal = new int[numInputs];

        for(int v = 0; v < numInputs; v++)
        {
            signal[v] = (int) (Math.random() * degreesOfFreedom);
        }
        return  signal;
    }

    private double getBlackBoxMovement(int[] signal)
    {
        double movement = 0.0;

        int index = 0;
        for(int val : signal)
        {
            if(index % 2 == 0)
            {
                movement += val;
            }
            else {
                movement -= val;
            }

            index ++;
            if(index > 10)
            {
                break;
            }
        }

        movement = movement * (0.9 + (0.2) * Math.random());
        movement = movement * 100;
        return movement;
    }

    public static void main(String[] args)
    {
        TestLearning test = new TestLearning();
        test.setup();
        test.testLearning();
    }
}
