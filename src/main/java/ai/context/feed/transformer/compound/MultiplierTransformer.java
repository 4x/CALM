package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;

import java.util.List;

public class MultiplierTransformer extends CompoundedTransformer{

    private Feed fA;
    private Feed fB;
    public MultiplierTransformer(Feed fA, Feed fB) {
        super(new Feed[]{fA, fB});
        this.fA = fA;
        this.fB = fB;
    }

    @Override
    protected Object getOutput(Object input) {
        List<Object> data = (List<Object>)input;
        Double fA = (Double) data.get(0);
        Double fB = (Double) data.get(1);
        return (fA * fB);
    }

    @Override
    public Feed getCopy() {
        return new MultiplierTransformer(fA.getCopy(), fB.getCopy());
    }
}
