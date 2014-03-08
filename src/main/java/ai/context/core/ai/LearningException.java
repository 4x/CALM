package ai.context.core.ai;

public class LearningException extends Throwable {
    private final String reason;

    public LearningException(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
