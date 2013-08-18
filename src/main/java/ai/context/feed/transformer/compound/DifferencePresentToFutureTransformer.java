package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;
import ai.context.feed.transformer.single.FutureOffsetTransformer;

import java.util.ArrayList;
import java.util.List;

public class DifferencePresentToFutureTransformer extends CompoundedTransformer{

    private Feed present;
    private FutureOffsetTransformer future;
    public DifferencePresentToFutureTransformer(Feed presentFeed, FutureOffsetTransformer future) {
        super(new Feed[]{presentFeed, future});
        this.present = presentFeed;
        this.future = future;
    }

    @Override
    protected Object getOutput(Object input) {
        List<Object> data = (List<Object>)input;
        Double present = (Double) data.get(0);
        Object[] output = new Object[future.getNumberOfOutputs()];

        for(int i = 1; i < data.size(); i++){
            output[i - 1] =  (Double) data.get(i) - present;
        }
        return output;
    }

    @Override
    public Feed getCopy() {
        return new AdditionTransformer(present.getCopy(), future.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "["+startIndex+"] Future Movement of feed " + present + " using feed " + future;
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Present",
                "Future"
        };
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(present.getElementChain(0));
        for(int i = 0; i < future.getNumberOfOutputs(); i++){
            list.add(future.getElementChain(i));
        }
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return future.getNumberOfOutputs();
    }
}