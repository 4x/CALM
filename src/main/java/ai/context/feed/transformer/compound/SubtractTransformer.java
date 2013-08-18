package ai.context.feed.transformer.compound;

import ai.context.feed.Feed;

import java.util.ArrayList;
import java.util.List;

public class SubtractTransformer extends CompoundedTransformer{

    private Feed subtractFrom;
    private Feed subtractThis;
    public SubtractTransformer(Feed subtractFrom, Feed subtractThis) {
        super(new Feed[]{subtractFrom, subtractThis});
        this.subtractFrom = subtractFrom;
        this.subtractThis = subtractThis;
    }

    @Override
    protected Object getOutput(Object input) {
        List<Object> data = (List<Object>)input;
        Double subtractFrom = (Double) data.get(0);
        Double subtractThis = (Double) data.get(1);
        if(subtractFrom == null || subtractThis == null){
            return null;
        }
        return (subtractFrom - subtractThis);
    }

    @Override
    public Feed getCopy() {
        return new SubtractTransformer(subtractFrom.getCopy(), subtractThis.getCopy());
    }

    public String getDescription(int startIndex, String padding) {
        return padding + "["+startIndex+"] Subtractor of feed " + subtractThis.getDescription(startIndex, padding) + " from feed " + subtractFrom.getDescription(startIndex, padding);
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "Feed SubtractFrom",
                "Feed SubtractThis"
        };
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();

        list.add(this);
        list.add(subtractFrom.getElementChain(0));
        list.add(subtractThis.getElementChain(0));
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return 1;
    }
}
