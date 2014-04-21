package ai.context.feed.transformer.series.learning;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import com.tictactec.ta.lib.CoreAnnotated;
import com.tictactec.ta.lib.MInteger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TALibTransformer extends BufferedTransformer {

    public static String TYPE_MACD = "MACD";
    public static String TYPE_ADX = "ADX";
    public static String TYPE_FTT = "FTT";

    private int span;
    private CoreAnnotated taLib = new CoreAnnotated();
    private String analyticsType;
    private Map<String, Object> parameters;
    private Feed[] taLibFeeds;

    public TALibTransformer(int span, String type, Map<String, Object> parameters, Feed[] feeds) {
        super((10 * span), feeds);

        this.span = span;
        this.parameters = parameters;
        this.taLibFeeds = feeds;
        this.analyticsType = type;

        pushBackOutput(span);
    }

    @Override
    protected FeedObject[] getOutput(FeedObject[] input) {
        FeedObject[] output = new FeedObject[input.length];
        Object[][] allInputs = null;

        for (int i = 0; i < input.length; i++) {
            Object[] inputArray = ((Object[]) input[i].getData());
            if (allInputs == null) {
                allInputs = new Object[inputArray.length][input.length];
            }
            for (int x = 0; x < inputArray.length; x++) {
                allInputs[x][i] = inputArray[x];
            }
        }

        if (analyticsType.equals(TYPE_ADX)) {
            double[] outputArray = new double[input.length];
            //TODO prepare input/parameters
            double[] high = new double[input.length];
            double[] low = new double[input.length];
            double[] close = new double[input.length];

            for (int i = 0; i < high.length; i++) {
                high[i] = (double) allInputs[0][i];
                low[i] = (double) allInputs[1][i];
                close[i] = (double) allInputs[2][i];
            }

            taLib.adx(0, input.length - 1, high, low, close, ((Integer) parameters.get("period")).intValue() / 2, new MInteger(), new MInteger(), outputArray);
            //TODO prepare output
            //System.out.println("ADX");
            for (int i = 0; i < input.length; i++) {
                output[i] = new FeedObject(input[i].getTimeStamp(), outputArray[i]);
                //System.out.println(i + " " + outputArray[i]);
            }

        } else if (analyticsType.equals(TYPE_FTT)) {
        } else if (analyticsType.equals(TYPE_MACD)) {
        } else if (analyticsType.equals(TYPE_ADX)) {

        } else if (analyticsType.equals(TYPE_ADX)) {

        } else if (analyticsType.equals(TYPE_ADX)) {

        } else if (analyticsType.equals(TYPE_ADX)) {

        } else if (analyticsType.equals(TYPE_ADX)) {

        } else if (analyticsType.equals(TYPE_ADX)) {

        }

        return output;
    }

    @Override
    public Feed getCopy() {
        // Don't think we will use this a lot but we can implement if needs be
        return null;
    }

    public void goLive() {
        goLive(span);
    }

    @Override
    public String getDescription(int startIndex, String padding) {

        String data = "";
        for (Feed feed : taLibFeeds) {
            data += "\n" + feed.getDescription(startIndex, padding + "   ");
        }
        return padding + "[" + startIndex + "] TALib Transformer of type: " + analyticsType + " with span: " + span + ", and parameters: " + parameters + " and feeds: " + data;
    }

    @Override
    public List<Feed> getElementChain(int element) {
        List list = new ArrayList<>();
        return list;
    }

    @Override
    public int getNumberOfOutputs() {
        return -1;
    }
}
