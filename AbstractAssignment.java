import java.util.*;
import java.sql.Timestamp;

abstract class AbstractAssignment implements Assignment {
    private final String    id;
    private final String    title;
    private final Timestamp dueDate;
    private final double    maxPoints;
    private final AssignmentTemplate template;

    AbstractAssignment(String id, String title, Timestamp dueDate, double maxPoints, AssignmentTemplate template) {
        Objects.requireNonNull(id); Objects.requireNonNull(title);
        Objects.requireNonNull(dueDate);

        if (maxPoints <= 0) throw new IllegalArgumentException("maxPoints ≤ 0");

        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.maxPoints = maxPoints;
        this.template = template;
    }

    /* GETTERS */
    @Override public String getId() { return id; }
    @Override public String getTitle() { return title; }
    @Override public Timestamp getDueDate() { return dueDate; }
    @Override public double getMaxPoints() { return maxPoints; }
    @Override public double getWeight() { return template.getWeight(); }
    
    public AssignmentTemplate getTemplate() { return template; }
}