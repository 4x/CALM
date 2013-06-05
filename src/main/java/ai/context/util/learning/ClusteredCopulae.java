package ai.context.util.learning;

import ai.context.util.mathematics.CorrelationCalculator;

import java.util.HashMap;
import java.util.TreeMap;

public class ClusteredCopulae {

    private HashMap<Integer, HashMap<Integer, TreeMap<Integer, CorrelationCalculator>>> variableClusteredCorrelations = new HashMap<Integer, HashMap<Integer, TreeMap<Integer, CorrelationCalculator>>>();

    public synchronized void addObservation(int state[], double value)
    {
        for(int i = 0; i < state.length; i++)
        {
            if((!variableClusteredCorrelations.containsKey(i)))
            {
                variableClusteredCorrelations.put(i, new HashMap<Integer, TreeMap<Integer, CorrelationCalculator>>());
            }

            HashMap<Integer, TreeMap<Integer, CorrelationCalculator>> m1 = variableClusteredCorrelations.get(i);
            if(!m1.containsKey(state[i]))
            {
                m1.put(state[i], new TreeMap<Integer, CorrelationCalculator>());
            }

            TreeMap<Integer, CorrelationCalculator> m2 = m1.get(state[i]);
            for(int index = 0; index < state.length; index++)
            {
                if(index != i)
                {
                    if(!m2.containsKey(index))
                    {
                        m2.put(index, new CorrelationCalculator());
                    }

                    m2.get(index).getCorrelationCoefficient(state[index], value);
                }
            }
        }
    }

    public synchronized double[] getCorrelationWeights(int[] state)
    {
        double[] weights = new double[state.length];
        for (int i = 0; i < state.length; i++)
        {
            weights[i] = 0.0;
        }

        for (int i = 0; i < state.length; i++)
        {
            if(variableClusteredCorrelations.containsKey(i))
            {
                HashMap<Integer, TreeMap<Integer, CorrelationCalculator>> m1 = variableClusteredCorrelations.get(i);
                if(m1.containsKey(state[i]))
                {
                    TreeMap<Integer, CorrelationCalculator> m2 = m1.get(state[i]);
                    for(int index = 0; index < state.length; index++)
                    {
                        double increment = 0.0;
                        if(m2.containsKey(index))
                        {
                            double correlation = Math.abs(m2.get(index).getCurrentCorrelation());
                            if(correlation >= 0 && correlation <= 2)
                            {
                                increment = correlation;
                            }
                        }
                        else{
                            double upper = Math.random()/5;
                            double lower = Math.random()/5;

                            Integer u = m2.floorKey(index);
                            Integer l = m2.ceilingKey(index);

                            if(u != null)
                            {
                                upper = m2.get(u).getCurrentCorrelation();
                            }
                            if(l != null)
                            {
                                lower = m2.get(l).getCurrentCorrelation();
                            }

                            if(u != null && l != null)
                            {
                                double correlation = Math.abs(((index - l) * (upper - lower) / (u - l) + lower));
                                if(correlation >= 0 && correlation <= 2)
                                {
                                    increment = correlation;
                                }
                            }
                            else{
                                double correlation = Math.abs(lower + upper);
                                if(correlation >= 0 && correlation <= 2)
                                {
                                    increment = correlation;
                                }
                            }
                        }

                        weights[index] += increment;
                    }
                }
            }
        }

        for (int i = 0; i < state.length; i++)
        {
            weights[i] = Math.abs(weights[i]/state.length);
        }
        return weights;
    }
}
