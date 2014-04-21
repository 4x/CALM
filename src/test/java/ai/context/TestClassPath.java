package ai.context;

import ai.context.util.io.Channel;

public class TestClassPath {

    public static void main(String[] args) {
        System.out.println(Channel.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    }
}
