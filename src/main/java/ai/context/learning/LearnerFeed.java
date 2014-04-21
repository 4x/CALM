package ai.context.learning;

public interface LearnerFeed {

    public boolean hasNext();

    public DataObject readNext();

    public String getDescription();
}
