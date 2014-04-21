package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.List;

public class DividerTransformer extends CompoundedTransformer {

    private Feed numerator;
    private Feed denominator;

    public DividerTransformer(Feed numerator, Feed denominator) {
        super(new Feed[]{numerator, denominator});
        this.denominator = denominator;
        this.numerator = numerator;
    }

    @Override
    protected Object getOutput(Object input) {
        List<Object> data = (List<Object>) input;
        Double numerator = (Double) data.get(0);
        Double denominator = (Double) data.get(1);
        return (numerator / denominator);
    }

    @Override
    public Feed getCopy() {
        return new DividerTransformer(numerator.getCopy(), denominator.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "[" + startIndex + "] Division of feed " + numerator + " by feed " + denominator;
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Numerator feed",
                "Denominator feed"
        };
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(numerator.getElementChain(0));
        list.add(denominator.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}

