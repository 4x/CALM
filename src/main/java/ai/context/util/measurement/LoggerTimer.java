package ai.context.util.measurement;

import java.util.HashMap;

public class LoggerTimer {

    private static HashMap<Object, Long> callers = new HashMap<>();
    private static boolean on = true;

    public static void printTimeDelta(String message, Object caller) {
        if (!on) {
            return;
        }
        Long t = System.currentTimeMillis();
        if (!callers.containsKey(caller)) {
            callers.put(caller, t);
        }

        System.err.println(message + ": " + (t - callers.get(caller)) + "ms");
        callers.put(caller, t);
    }

    public static void turn(boolean onOff) {
        on = onOff;
    }
}
