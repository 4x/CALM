package ai.context.util.score;

public class NeuronScoreHelper {
    public double getScoreForNeuronId(int id){
        return NeuronScoreKeeper.getWeightFor(id);
    }

    public String getInfoForNeuronId(int id){
        return NeuronScoreKeeper.getInfoFor(id);
    }
}
