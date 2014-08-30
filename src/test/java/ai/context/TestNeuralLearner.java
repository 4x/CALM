package ai.context;

import ai.context.core.ai.LearningException;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.ISynchFeed;
import ai.context.learning.neural.NeuralLearner;
import ai.context.runner.feeding.MotherFeedCreator;
import ai.context.util.configuration.PropertiesHolder;

public class TestNeuralLearner {
    public static void main(String[] args){
        ISynchFeed motherFeed = MotherFeedCreator.getMotherFeed("/opt/dev/data/");

        PropertiesHolder.parentsPerNeuron = 0;
        PropertiesHolder.addtionalStimuliPerNeuron = 0;
        NeuralLearner neuralLearner = new NeuralLearner(new long[]{2 * 3600 * 1000L, 4 * 3600 * 1000L}, motherFeed, new Integer[]{3, 1, 2, 0}, new Integer[]{12, 24, 36, 48}, null, null, 1800000, 0.0001);

        for(int i = 0; i < 2000; i++){
            try {
                neuralLearner.step();
                FeedObject f = neuralLearner.readNext(null);

                System.out.println(f);
            } catch (LearningException e) {
                e.printStackTrace();
            }
        }
    }
}
