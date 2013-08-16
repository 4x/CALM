package ai.context.feed.transformer.filtered;

import ai.context.container.TimedContainer;
import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.row.RowFeed;

import java.util.ArrayList;
import java.util.List;

public class RowBasedTransformer extends FilteredEventDecayFeed{

    private int[] filterColumn;
    private String[] regexMatch;
    private int[] interestedColumns;
    private TimedContainer container;
    protected RowFeed rawFeed;

    public RowBasedTransformer(RowFeed rawFeed, double halfLife, int[] filterColumn, String[] regexMatch, int[] interestedColumns, TimedContainer container) {
        super(rawFeed, halfLife, container);

        this.rawFeed = rawFeed;
        this.filterColumn = filterColumn;
        this.regexMatch = regexMatch;
        this.interestedColumns = interestedColumns;
        this.container = container;
    }

    @Override
    protected boolean pass(FeedObject raw) {

        Object[] data = (Object[]) raw.getData();
        for(int i = 0; i < filterColumn.length; i++){
            if(!data[filterColumn[i]].toString().matches(regexMatch[i]))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Object process(Object rawData) {

        Object[] data = (Object[]) rawData;
        List<Object> list = new ArrayList<Object>();
        for(int index : interestedColumns)
        {
            list.add(data[index]);
        }

        return list;
    }

    @Override
    public Feed getCopy() {
        return new RowBasedTransformer(rawFeed.getCopy(), halfLife, filterColumn, regexMatch, interestedColumns, container);
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        String data = "";
        for(int i = 0; i < filterColumn.length; i++){
            data += filterColumn[i] + " -> " + regexMatch[i] + "; ";
        }

        return padding + "["+startIndex+"] Row based filter with half-life: "+halfLife+", filtering: "+data+" on feed: " + rawFeed.getDescription(startIndex, padding);
    }

    //@Override
    public String[] getConstructorArguments() {
        return new String[]{
                "RawFeed",
                "HalfLife",
                "int[] filterColumn",
                "String[] regexMatch - to keep just rows that have the right values in the filterColumns",
                "int[] interestedColumns - the columns whose values we are interested in",
                "TimedContainer"
        };
    }
}
