package ai.context.mapping;

import java.util.HashMap;

public class FXStreetCountryMapping {

    private static HashMap<String, String> map = new HashMap<String, String>();

    static {
        map.put("UK","United Kingdom");
        map.put("US", "United States");
        map.put("EMU", "European Monetary Union");
        map.put("FR", "France");
        map.put("DE", "Germany");
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
