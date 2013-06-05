package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;

import java.util.List;

public class DividerTransformer extends CompoundedTransformer{

    private Feed numerator;
    private Feed denominator;

    public DividerTransformer(Feed numerator, Feed denominator) {
        super(new Feed[]{numerator, denominator});
        this.denominator = denominator;
        this.numerator = numerator;
    }

    @Override
    protected Object getOutput(Object input) {
        List<Object> data = (List<Object>)input;
        Double numerator = (Double) data.get(0);
        Double denominator = (Double) data.get(1);
        return (numerator / denominator);
    }

    @Override
    public Feed getCopy() {
        return new DividerTransformer(numerator.getCopy(), denominator.getCopy());
    }
}

