package ai.context.util.learning;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AmalgamateUtils {

    public static String getAmalgamateString(int[] amalgamate)
    {
        String data = "";

        for(int i : amalgamate)
        {
            data += i + "|";
        }

        if(data.length() > 1){
            data = data.substring(0, data.length() - 1) ;
        }

        return data;
    }

    public static String getArrayString(double[] array)
    {
        String data = "";

        for(double i : array)
        {
            data += i + ":";
        }

        if(data.length() > 1){
            data = data.substring(0, data.length() - 1) ;
        }

        return data;
    }

    public static String getArrayString(int[] array)
    {
        String data = "";

        for(int i : array)
        {
            data += i + ":";
        }

        if(data.length() > 1){
            data = data.substring(0, data.length() - 1) ;
        }

        return data;
    }

    public static String getArrayString(Object[] array)
    {
        String data = "";

        for(Object i : array)
        {
            data += i + ":";
        }

        if(data.length() > 1){
            data = data.substring(0, data.length() - 1) ;
        }

        return data;
    }

    public static String getMapString(Map map)
    {
        String data = "";

        for(Object o : map.entrySet())
        {
            Map.Entry entry = (Map.Entry)o;

            if(entry.getValue() instanceof Map){
                String tmp = getMapString((Map)entry.getValue());
                for(String part : tmp.split(";")){
                    data += entry.getKey() + ":" + part + ";";
                }
            }
            else if(entry.getValue() instanceof Set){
                for(Object cO : (Set) entry.getValue()){
                    data += entry.getKey() + ":" + getStringFor(cO) + ";";
                }
            }
            else if(entry.getValue() instanceof List){
                for(Object cO : (List) entry.getValue()){
                    data += entry.getKey() + ":" + getStringFor(cO) + ";";
                }
            }
            else{
                data += entry.getKey() + ":" + getStringFor(entry.getValue()) + ";";
            }

        }

        if(data.length() > 1){
            data = data.substring(0, data.length() - 1) ;
        }

        return data;
    }

    public static String getStringFor(Object object){
        if(object.getClass().isPrimitive() || object instanceof String || object instanceof Number){
            return object.toString();
        }
        else{
            return System.identityHashCode(object) + "";
        }
    }
}


