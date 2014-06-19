package ai.context.feed.manipulation;

public class WrapperManipulatorPair {
    private final int wrapperId;
    private final String manipulatorId;

    public WrapperManipulatorPair(int wrapperId, String manipulatorId) {
        this.wrapperId = wrapperId;
        this.manipulatorId = manipulatorId;
    }

    public int getWrapperId() {
        return wrapperId;
    }

    public String getManipulatorId() {
        return manipulatorId;
    }

    @Override
    public String toString() {
        return wrapperId + ":" + manipulatorId;
    }
}
