package ai.context.util.analysis.feed;

import java.util.HashMap;

public class ObjectHolder {

    private static HashMap<String, Object> objects = new HashMap<>();

    public static void save(String id, Object object) {
        objects.put(id, object);
    }

    public static Object get(String id) {
        return objects.get(id);
    }

    public void wipe() {
        objects.clear();
    }
}
