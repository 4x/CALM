package ai.context.util.mathematics.discretisation;

import ai.context.util.mathematics.Discretisation;

import java.util.Map;
import java.util.TreeMap;

public class AbsoluteMovementDiscretiser implements Discretisation{

    private double maxAmplitude = 0;
    private TreeMap<Double, Double> discretisationLayers = new TreeMap<>();

    public AbsoluteMovementDiscretiser(double maxAmplitude) {
        this.maxAmplitude = maxAmplitude;
    }

    public void clearLayers(){
        discretisationLayers.clear();
    }

    public void addLayer(double amplitude, double discretisation){
        discretisationLayers.put(amplitude, discretisation);
    }

    @Override
    public double process(double val) {
        double abs = Math.abs(val);
        double sign = Math.signum(val);
        if(maxAmplitude > 0){
            abs = Math.min(abs, maxAmplitude);
        }

        double discretisation = 0;
        for(Map.Entry<Double, Double> entry : discretisationLayers.entrySet()){
            if(abs < entry.getKey()){
                discretisation = entry.getValue();
            }
        }

        if(discretisation > 0){
            abs = Math.round(abs/discretisation) * discretisation;
        }

        return (abs * sign);
    }
}
