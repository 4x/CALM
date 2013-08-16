package ai.context.util.analysis.feed;

public class FeedTemplate {

    private String className;
    private String name;
    private String[] argments;

    public FeedTemplate(String className, String name, String[] argments) {
        this.className = className;
        this.name = name;
        this.argments = argments;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public String[] getArgments() {
        return argments;
    }
}
