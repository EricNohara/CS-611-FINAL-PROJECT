import java.util.List;

import db.SubmissionDAO;
import model.Submission;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        // Test Data Setup
        SubmissionDAO submissionDAO = SubmissionDAO.getInstance();

        // Create a submission
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(3);
        Submission submission = new Submission(1, "path/to/file.pdf", Submission.Status.UNGRADED, ids);

        // CREATE: Insert the new submission
        submissionDAO.create(submission);
        System.out.println("Created submission with ID: " + submission.getId());

        // READ: Read the submission we just created
        Submission retrievedSubmission = submissionDAO.read(submission.getId());
        if (retrievedSubmission != null) {
            System.out.println("Retrieved submission: " + retrievedSubmission);
        } else {
            System.out.println("Submission not found.");
        }

        // UPDATE: Update the submission (e.g., changing the grade)
        retrievedSubmission.setGrade(90.0);
        submissionDAO.update(retrievedSubmission);
        System.out.println("Updated submission with new grade: " + retrievedSubmission.getGrade());

        // READ: Verify update by reading again
        Submission updatedSubmission = submissionDAO.read(retrievedSubmission.getId());
        if (updatedSubmission != null) {
            System.out.println("Updated submission: " + updatedSubmission);
        }

        // DELETE: Delete the submission
        submissionDAO.delete(submission.getId());
        System.out.println("Deleted submission with ID: " + submission.getId());

        // READ: Verify deletion
        Submission deletedSubmission = submissionDAO.read(submission.getId());
        if (deletedSubmission == null) {
            System.out.println("Submission successfully deleted.");
        } else {
            System.out.println("Failed to delete submission.");
        }
    }
}
