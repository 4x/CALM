package ai.context.util;

import java.util.HashSet;
import java.util.Set;

public class StringUtils {

    public static Set<Character> numberChars = new HashSet<Character>();

    static {
        numberChars.add('0');
        numberChars.add('1');
        numberChars.add('2');
        numberChars.add('3');
        numberChars.add('4');
        numberChars.add('5');
        numberChars.add('6');
        numberChars.add('7');
        numberChars.add('8');
        numberChars.add('9');
        numberChars.add('.');
    }

    public static Double extractDouble(String data)
    {
        String number = "";
        boolean extracting = false;
        for(char c : data.toCharArray())
        {
            if(numberChars.contains(c))
            {
                extracting = true;
                number += c;
            }
            else if(extracting)
            {
                break;
            }
        }
        if(number.length() > 0 && number.matches("[0-9]*[\\.[0-9]*]*"))
        {
            return Double.parseDouble(number);
        }
        return 0.0;
    }
}
