import java.util.*;
import java.sql.Timestamp;

public abstract class Assignment {
    enum Type { HOMEWORK, QUIZ, EXAM, PROJECT }

    private String    id;
    private String    title;
    private Timestamp dueDate;
    private double    maxPoints;
    private AssignmentTemplate template;

    Assignment(String id, String title, Timestamp dueDate, double maxPoints, AssignmentTemplate template) {
        Objects.requireNonNull(id); Objects.requireNonNull(title);
        Objects.requireNonNull(dueDate);

        if (maxPoints <= 0) throw new IllegalArgumentException("maxPoints ≤ 0");

        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.maxPoints = maxPoints;
        this.template = template;
    }

    /** Returns the contribution earnedPoints / maxPoints · weight */
    public double contribution(double earnedPoints) {
        if (earnedPoints < 0.0 || earnedPoints > this.maxPoints) {
            throw new IllegalArgumentException("earnedPoints out of range");
        }

        return earnedPoints / this.maxPoints * this.getWeight();
    }

    /* GETTERS */
    public String getId() { return id; }
    public String getTitle() { return title; }
    public Timestamp getDueDate() { return dueDate; }
    public double getMaxPoints() { return maxPoints; }
    public double getWeight() { return template.getWeight(); }
    public AssignmentTemplate getTemplate() { return template; }

    /* SETTERS */
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public void setMaxPoints(double maxPoints) { this.maxPoints = maxPoints; }
    public void setTemplate(AssignmentTemplate template) { this.template = template; }
}