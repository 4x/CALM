package ai.context.util.common;

public class LabelledTuple {
    public final String name;
    public final int key;
    public final double value;

    public LabelledTuple(String name, int key, double value) {
        this.name = name;
        this.key = key;
        this.value = value;
    }
}
