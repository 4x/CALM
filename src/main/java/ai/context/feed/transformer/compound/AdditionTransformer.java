package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;

import java.util.List;

public class AdditionTransformer extends CompoundedTransformer{

    private Feed addTo;
    private Feed addThis;
    public AdditionTransformer(Feed addTo, Feed addThis) {
        super(new Feed[]{addTo, addThis});
        this.addThis = addThis;
        this.addTo = addTo;
    }

    @Override
    protected Object getOutput(Object input) {
        List<Object> data = (List<Object>)input;
        Double addTo = (Double) data.get(0);
        Double addThis = (Double) data.get(1);
        return (addTo + addThis);
    }

    @Override
    public Feed getCopy() {
        return new AdditionTransformer(addTo.getCopy(), addThis.getCopy());
    }

    @Override
    public String getDescription(int startIndex, String padding) {
        return padding + "["+startIndex+"] Addition of feed " + addThis + " to feed " + addTo;
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Feed A",
                "Feed B"
        };
    }
}