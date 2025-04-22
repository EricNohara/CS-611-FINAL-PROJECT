import java.util.*;
import java.sql.Timestamp;

public class Assignment {
    enum Type { HOMEWORK, QUIZ, EXAM, PROJECT }

    private int    id;
    private String    name;
    private Timestamp dueDate;
    private double    maxPoints;
    private AssignmentTemplate template;
    private int courseId;

    public Assignment(int id, String name, Timestamp dueDate, double maxPoints, AssignmentTemplate template, int courseId) {
        Objects.requireNonNull(id); Objects.requireNonNull(name);
        Objects.requireNonNull(dueDate);

        if (maxPoints <= 0) throw new IllegalArgumentException("maxPoints ≤ 0");

        this.id = id;
        this.name = name;
        this.dueDate = dueDate;
        this.maxPoints = maxPoints;
        this.template = template;
        this.courseId = courseId;
    }

    /** Returns the contribution earnedPoints / maxPoints · weight */
    public double contribution(double earnedPoints) {
        if (earnedPoints < 0.0 || earnedPoints > this.maxPoints) {
            throw new IllegalArgumentException("earnedPoints out of range");
        }

        return earnedPoints / this.maxPoints * this.getWeight();
    }

    /* GETTERS */
    public int getId() { return id; }
    public String getName() { return name; }
    public Timestamp getDueDate() { return dueDate; }
    public double getMaxPoints() { return maxPoints; }
    public double getWeight() { return template.getWeight(); }
    public AssignmentTemplate getTemplate() { return template; }
    public int getCourseId() { return this.courseId; }

    /* SETTERS */
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public void setMaxPoints(double maxPoints) { this.maxPoints = maxPoints; }
    public void setTemplate(AssignmentTemplate template) { this.template = template; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
}