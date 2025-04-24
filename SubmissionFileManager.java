import java.io.File;
import java.util.stream.Collectors;

import db.AssignmentDAO;
import db.CourseDAO;
import model.Assignment;
import model.Course;
import model.Submission;
import utils.FileManager;

public class SubmissionFileManager {
    public static final String BASE_PATH = "data/courses";

    public static boolean uploadSubmission(File upload, Submission submission) {
        String savePath = buildSavePath(submission);
        String submissionName = buildSubmissionName(submission) + getFileExtension(upload);

        // create a new File object for the final destination (save path + renamed file name)
        File destinationFile = new File(savePath, submissionName);

        // ensure that the directories exist before saving the file
        File directory = destinationFile.getParentFile();
        if (!directory.exists()) {
            directory.mkdirs(); // Create any necessary directories
        }

        // try to save the file using the FileManager
        if (FileManager.saveFile(upload, destinationFile.getAbsolutePath())) {
            submission.setFilepath(destinationFile.getAbsolutePath()); // save the file path
            return true;
        }

        return false; // Return false if the file saving fails
    }

    public static boolean deleteSubmission(Submission submission) {
        return FileManager.deleteFile(submission.getFilepath());
    }

    // STATIC HELPER METHODS
    private static String buildSavePath(Submission submission) {
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        Assignment assignment = assignmentDAO.read(submission.getAssignmentId());
        String assignmentDir = sanitizeForPath(assignment.getName()) + "-" + assignment.getId();

        CourseDAO courseDAO = CourseDAO.getInstance();
        Course course = courseDAO.read(assignment.getCourseId());
        String courseDir = sanitizeForPath(course.getName()) + "-" + course.getId();

        return BASE_PATH + "/" + courseDir + "/" + assignmentDir;
    }

    private static String buildSubmissionName(Submission submission) {
        String collaboratorsJoined = submission.getCollaboratorIds().stream()
                                               .map(String::valueOf)  // convert id to string
                                               .collect(Collectors.joining("-"));  // join with hyphen
        return collaboratorsJoined + "-" + submission.getSubmittedAt().getTime();
    }

    public static String sanitizeForPath(String input) {
        // replace whitespace with underscore
        String noWhitespace = input.replaceAll("\\s+", "_");
    
        // remove special characters
        // removes: \ / : * ? " < > | and control characters
        return noWhitespace.replaceAll("[\\\\/:*?\"<>|]", "");
    }

    private static String getFileExtension(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return fileName.substring(lastDotIndex); 
        }
        return ""; 
    }
}
