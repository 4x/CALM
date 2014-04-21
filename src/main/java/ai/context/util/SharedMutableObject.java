package ai.context.util;

public class SharedMutableObject<T> {

    private T value;

    public SharedMutableObject(T initialValue) {
        value = initialValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
