package ai.context.mapping;

import java.util.HashMap;

public class FXStreetCountryMapping {

    private static HashMap<String, String> map = new HashMap<String, String>();

    static {
        map.put("United Kingdom", "UK");
        map.put("United States", "US");
        map.put("France", "FR");
        map.put("Germany", "DE");
    }

    public static String getMapping(String country)
    {
        if(map.containsKey(country))
        {
            return map.get(country);
        }
        return country;
    }
}
