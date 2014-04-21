package ai.context;

import ai.context.util.StringUtils;
import org.junit.Test;

public class TestExtractDouble {

    @Test
    public void testExtraction() {
        String test = "$700.3";
        System.out.println(StringUtils.extractDouble(test));

        test = "700";
        System.out.println(StringUtils.extractDouble(test));

        test = "";
        System.out.println(StringUtils.extractDouble(test));
    }
}
