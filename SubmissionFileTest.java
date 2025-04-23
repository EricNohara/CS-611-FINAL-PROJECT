import javax.swing.*;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SubmissionFileTest {
    public static void main(String[] args) {
        // Create dummy user
        UserDAO userDAO = UserDAO.getInstance();
        Student student = new Student("Eric N>L", "wefdsf@gmail.com", "asdfsfdsf");
        userDAO.create(student);

        // Create dummy course template
        CourseTemplateDAO courseTemplateDAO = CourseTemplateDAO.getInstance();
        CourseTemplate courseTemplate = new CourseTemplate("CS-611-template", new ArrayList<>());
        courseTemplateDAO.create(courseTemplate);

        // Create dummy course
        CourseDAO courseDAO = CourseDAO.getInstance();
        Course course = new Course(courseTemplate, "CS/611");
        courseDAO.create(course);

        // Create dummy assignment template
        AssignmentTemplateDAO assignmentTemplateDAO = AssignmentTemplateDAO.getInstance();
        List<String> extensions = new ArrayList<>();
        extensions.add("csv");
        extensions.add("txt");
        AssignmentTemplate assignmentTemplate = new AssignmentTemplate(courseTemplate.getId(), 0.4, Assignment.Type.HOMEWORK, extensions);
        assignmentTemplateDAO.create(assignmentTemplate);

        // Create dummy assignment
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        Assignment assignment = new Assignment("Order And Chaos", new Timestamp(System.currentTimeMillis()), 100, assignmentTemplate, course.getId());
        assignmentDAO.create(assignment);

        // Choose a file to upload
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // Simulate a submission object
            List<Integer> ids = new ArrayList<>();
            ids.add(student.getId());
            Submission submission = new Submission(assignment.getId(), null, Submission.Status.UNGRADED, ids);

            // Upload the file
            boolean uploadSuccess = SubmissionFileManager.uploadSubmission(selectedFile, submission);
            System.out.println("Upload successful: " + uploadSuccess);

            // Now try to delete the uploaded file
            if (uploadSuccess) {
                int confirm = JOptionPane.showConfirmDialog(null, "Delete uploaded file?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean deleteSuccess = SubmissionFileManager.deleteSubmission(submission);
                    System.out.println("Delete successful: " + deleteSuccess);
                }
            }
        } else {
            System.out.println("No file selected.");
        }
    }
}
