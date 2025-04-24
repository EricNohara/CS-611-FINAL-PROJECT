package model;
import java.sql.Timestamp;

public class Grader extends User implements SubmissionGrader {
    public Grader(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }

    public Grader(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    @Override
    public User.Role getRole() { return User.Role.GRADER; }

    @Override
    public void gradeSubmission(Submission submission) {}

    // Student specific methods...
}
