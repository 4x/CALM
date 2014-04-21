package ai.context.core.neural.messaging.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class RecentUniqueStrings {
    private Set<String> set = new HashSet<>();
    private LinkedList<String> list = new LinkedList<>();
    private int maxMemory;

    public RecentUniqueStrings(int maxMemory) {
        this.maxMemory = maxMemory;
    }

    public boolean has(String id) {
        return set.contains(id);
    }

    public boolean add(String id) {
        if (has(id)) {
            return false;
        }
        if (list.size() > maxMemory) {
            set.remove(list.pollFirst());
        }
        list.add(id);
        set.add(id);
        return true;
    }
}
