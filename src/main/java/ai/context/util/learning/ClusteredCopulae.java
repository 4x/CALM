package ai.context.util.learning;

import ai.context.util.mathematics.CorrelationCalculator;

import java.util.Arrays;

public class ClusteredCopulae {

    private int base = 20;

    private CorrelationCalculator[][][] variableClusteredCorrelations;

    public synchronized void addObservation(int state[], double value) {
        if(variableClusteredCorrelations == null){
            variableClusteredCorrelations = new CorrelationCalculator[state.length][state.length][base * 2];
        }
        for (int i = 0; i < state.length; i++) {
            int index = state[i] + base;
            if(index >= base * 2){
                index = base * 2 - 1;
            }
            else if(index < 0){
                index = 0;
            }

            for (int j = 0; j < state.length; j++) {
                if (j != i) {
                    if(variableClusteredCorrelations[i][j][index] == null){
                        variableClusteredCorrelations[i][j][index] = new CorrelationCalculator();
                    }
                    variableClusteredCorrelations[i][j][index].getCorrelationCoefficient(state[j], value);
                }
            }
        }
    }

    public double[] getCorrelationWeights(int[] state) {
        double[] weights = new double[state.length];

        if(variableClusteredCorrelations == null){
            return weights;
        }

        for (int i = 0; i < state.length; i++) {
            int index = state[i] + base;
            if(index >= base * 2){
                index = base * 2 - 1;
            }
            else if(index < 0){
                index = 0;
            }
            for (int j = 0; j < state.length; j++) {
                if (j != i) {
                    CorrelationCalculator calc = variableClusteredCorrelations[i][j][index];
                    double correlation = 0;

                    if(calc != null){
                        correlation = calc.getCurrentCorrelation() * calc.getCurrentCorrelation();
                    }
                    else{
                        CorrelationCalculator[] calcArr = variableClusteredCorrelations[i][j];
                        double upper = 0;
                        double lower = 0;

                        Integer u = null;
                        Integer l = null;
                        for(int indexSearch = 0; indexSearch < calcArr.length; indexSearch++){
                            if(indexSearch < index && calcArr[indexSearch] != null){
                                l = indexSearch;
                            }
                            else if(indexSearch > index && calcArr[indexSearch] != null){
                                u = indexSearch;
                                break;
                            }
                        }

                        if (u != null) {
                            upper = calcArr[u].getCurrentCorrelation();
                        }
                        if (l != null) {
                            lower = calcArr[l].getCurrentCorrelation();
                        }

                        if (u != null && l != null) {
                            correlation = ((state[i] - l) * (upper - lower) / (u - l) + lower);
                            correlation *= correlation;
                        } else {
                            correlation = lower + upper;
                            correlation *= correlation;
                        }
                    }
                    if(correlation > 0) {
                        weights[j] += correlation;
                    }
                }
            }
        }

        int div = state.length - 1;
        for (int i = 0; i < state.length; i++) {
            weights[i] = Math.sqrt(weights[i] / div);
        }
        return weights;
    }
}
