package model;
import java.util.*;
import java.sql.Timestamp;

public class Assignment {
    public enum Type {
        HOMEWORK, QUIZ, EXAM, PROJECT;

        public int toInt() {
            return this.ordinal() + 1;
        }

        public static Type fromInt(int i) {
            return Type.values()[i - 1];
        }
    }

    private int    id;
    private String    name;
    private Timestamp dueDate;
    private double    maxPoints;
    private int courseId;
    private List<String> submission_path;
    private double weight;
    private Assignment.Type type;
    private List<String> submissionTypes;

    public Assignment(String name, Timestamp dueDate, double maxPoints, AssignmentTemplate template, int courseId) {
        Objects.requireNonNull(id); Objects.requireNonNull(name);
        Objects.requireNonNull(dueDate);

        if (maxPoints <= 0) throw new IllegalArgumentException("maxPoints ≤ 0");

        this.id = -1;
        this.name = name;
        this.dueDate = dueDate;
        this.maxPoints = maxPoints;
        this.courseId = courseId;
        if(template != null){
            this.weight = template.getWeight();
            this.type = template.getType();
            this.submissionTypes = template.getSubmissionTypes();
        }
    }

    public Assignment(int id, String name, Timestamp dueDate, int maxPoints, int courseId,
                      List<String> submissionPath, double weight, Type type, List<String> submissionTypes) {
        this.id = id;
        this.name = name;
        this.dueDate = dueDate;
        this.maxPoints = maxPoints;
        this.courseId = courseId;
        this.submission_path = submissionPath;
        this.weight = weight;
        this.type = type;
        this.submissionTypes = submissionTypes;
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
    public double getWeight() { return this.weight; }
    public List<String> getSubmission_path() {return submission_path;}
    public int getCourseId() { return this.courseId; }
    public Assignment.Type getType() { return this.type; }
    public List<String> getSubmissionTypes() { return this.submissionTypes; }


    /* SETTERS */
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public void setMaxPoints(double maxPoints) { this.maxPoints = maxPoints; }
    public void setSubmission_path(List<String> submission_path) {this.submission_path = submission_path;}
    public void setWeight(double weight) {this.weight = weight;}
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public void setType(Assignment.Type type) { this.type = type; }
    public void setSubmissionTypes(List<String> submissionTypes) { this.submissionTypes = submissionTypes; }
}