package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.List;

public class MultiplierTransformer extends CompoundedTransformer {

    private Feed fA;
    private Feed fB;

    public MultiplierTransformer(Feed fA, Feed fB) {
        super(new Feed[]{fA, fB});
        this.fA = fA;
        this.fB = fB;
    }

    @Override
    protected Object getOutput(Object input) {
        List<Object> data = (List<Object>) input;
        Double fA = (Double) data.get(0);
        Double fB = (Double) data.get(1);
        return (fA * fB);
    }

    @Override
    public Feed getCopy() {
        return new MultiplierTransformer(fA.getCopy(), fB.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Multiplier of feed " + fA + " by feed " + fB;
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Feed A",
                "Feed B"
        };
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(fA.getElementChain(0));
        list.add(fB.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
