package ai.context.util.analysis.feed;

public class ConstructorArgument {

    enum TYPE{
        VALUE,
        REFERENCE
    }

    private TYPE type;
    private Object value;

    public ConstructorArgument(TYPE type, Object value) {
        this.type = type;
        this.value = value;
    }

    public TYPE getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
