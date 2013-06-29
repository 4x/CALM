package ai.context.util.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateUtils {

    public static SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    public static long getTimeFromString_YYYYMMddHHmmss(String dateTime){
        try {
            return format.parse(dateTime).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
