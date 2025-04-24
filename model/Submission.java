package model;
import java.sql.Timestamp;
import java.util.List;

public class Submission {
    public enum Status {
        UNGRADED, GRADED, LATE
    }

    private int id;
    private int assignment_id;
    private int grader_id;
    private String filepath;
    private Timestamp submitted_at;
    private double grade;
    private Status status;
    private List<Integer> collaboratorIds;

    public Submission(int assignment_id, String filepath, Status status, List<Integer> collaboratorIds) {
        this.id = -1;
        this.assignment_id = assignment_id;
        this.grader_id = -1;
        this.filepath = filepath;
        this.submitted_at = new Timestamp(System.currentTimeMillis());
        this.grade = -1.0;
        this.status = status;
        this.collaboratorIds = collaboratorIds;
    }

    public Submission(int id, int assignment_id, int grader_id, String filepath, Timestamp submittedAt, double grade, Status status, List<Integer> collaboratorIds) {
        this.id = id;
        this.assignment_id = assignment_id;
        this.grader_id = grader_id;
        this.filepath = filepath;
        this.submitted_at = submittedAt;
        this.grade = grade;
        this.status = status;
        this.collaboratorIds = collaboratorIds;
    }

    // GETTERS
    public int getId() { return id; }
    public int getAssignmentId() { return assignment_id; }
    public int getGraderId() { return grader_id; }
    public String getFilepath() { return filepath; }
    public Timestamp getSubmittedAt() { return submitted_at; }
    public double getGrade() { return grade; }
    public Status getStatus() { return status; }
    public List<Integer> getCollaboratorIds() { return collaboratorIds; }

    // SETTERS
    public void setId(int id) { this.id = id; }
    public void setAssignmentId(int assignment_id) { this.assignment_id = assignment_id; }
    public void setGraderId(int grader_id) { this.grader_id = grader_id; }
    public void setFilepath(String filepath) { this.filepath = filepath; }
    public void setSubmittedAt(Timestamp submitted_at) { this.submitted_at = submitted_at; }
    public void setGrade(double grade) { this.grade = grade; }
    public void setStatus(Status status) { this.status = status; }
    public void setCollaborators(List<Integer> collaboratorIds) { this.collaboratorIds = collaboratorIds; }

}
