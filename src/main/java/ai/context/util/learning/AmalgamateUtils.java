package ai.context.util.learning;

public class AmalgamateUtils {

    public static String getAmalgamateString(int[] amalgamate)
    {
        String data = "[";

        for(int i : amalgamate)
        {
            data += i + ",";
        }

        data = data.substring(0, data.length() - 1) + "]";

        return data;
    }
}
