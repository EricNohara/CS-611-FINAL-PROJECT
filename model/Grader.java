package model;
import java.nio.file.Files;
import java.sql.Timestamp;

import db.AssignmentDAO;
import db.SubmissionDAO;
import utils.FileSimilarityChecker;
import utils.SimilarityStrategy;
import utils.TfIdfCosineSimilarity;

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
    public void gradeSubmission(Submission submission, double pointsEarned) {
        // calculate grade - earned / max
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        Assignment assignment = assignmentDAO.read(submission.getAssignmentId());
        double maxPoints = assignment.getMaxPoints();
        double grade = pointsEarned / maxPoints;

        submission.setPointsEarned(pointsEarned);
        submission.setGrade(grade);

        // update the submission in the table
        SubmissionDAO submissionDAO = SubmissionDAO.getInstance();
        submissionDAO.update(submission);
    }

    @Override
    public double getMaxSimilarity(Submission submission) {
        try {
            return FileSimilarityChecker.getMaxSimilarity(submission.getFilepath());
        } catch (Exception e) {
            System.err.println("Error calculating submission similarity: " + e.getMessage());
        }

        return 0.0; // default
    }

    // Student specific methods...
}
