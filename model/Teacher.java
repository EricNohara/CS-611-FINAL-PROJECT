package model;
import java.sql.Timestamp;
import java.util.Collections;

import db.AssignmentDAO;
import db.CourseDAO;
import db.CourseTemplateDAO;
import db.SubmissionDAO;
import db.UserCourseDAO;
import db.UserDAO;
import ui.dashboard.TeacherDashboard;
import utils.FileSimilarityChecker;
import utils.SubmissionFileManager;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public class Teacher extends User implements SubmissionGrader, CourseManager {
    private static UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();
    private static CourseTemplateDAO courseTemplateDAO = CourseTemplateDAO.getInstance();
    private static CourseDAO courseDAO = CourseDAO.getInstance();
    private static AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();

    public Teacher(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }
    
    public Teacher(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    // ABSTRACT METHOD IMPLEMENTATIONS

    @Override
    public User.Role getRole() { return User.Role.TEACHER; }

    @Override
    public void gradeSubmission(Submission submission, double pointsEarned) {
        // calculate grade - earned / max
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        Assignment assignment = assignmentDAO.read(submission.getAssignmentId());
        double maxPoints = assignment.getMaxPoints();
        double grade = pointsEarned / maxPoints;

        submission.setPointsEarned(pointsEarned);
        submission.setGrade(grade);
        System.out.println("get points:"+pointsEarned+" grade:"+grade);

        // update the submission in the table
        SubmissionDAO submissionDAO = SubmissionDAO.getInstance();
        submissionDAO.update(submission);
    }

    @Override
    public void addUserToCourse(User user, Course course) {
        UserCourse userCourse = new UserCourse(user.getId(), course.getId());
        userCourseDAO.create(userCourse);
    }

    @Override
    public void setUserStatus(User user, Course course, UserCourse.Status status) {
        UserCourse userCourse = userCourseDAO.read(user.getId(), course.getId());
        userCourse.setStatus(status);
        userCourseDAO.update(userCourse);
    }

    @Override
    public void removeUserFromCourse(User user, Course course) {
        userCourseDAO.delete(user.getId(), course.getId());
    }

    @Override
    public void createCourseTemplate(CourseTemplate template) {
        courseTemplateDAO.create(template);
    }

    @Override
    public void editCourseTemplate(CourseTemplate template) {
        courseTemplateDAO.update(template);
    }

    @Override
    public void deleteCourseTemplate(CourseTemplate template) {
        courseTemplateDAO.delete(template.getId());
    }

    @Override
    public void createCourse(Course course) {
        courseDAO.create(course);
    }

    @Override
    public void editCourse(Course course) {
        courseDAO.update(course);
    }

    @Override
    public void deleteCourse(Course course) {
        courseDAO.delete(course.getId());
    }

    @Override
    public void createAssignment(Assignment assignment) {
        assignmentDAO.create(assignment);
    }

    @Override
    public void editAssignment(Assignment assignment) {
        assignmentDAO.update(assignment);
    }

    @Override
    public void deleteAssignment(Assignment assignment) {
        assignmentDAO.delete(assignment.getId());
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

    public static void main (String[] args) {
        // Student student1 = new Student("Eric1", "ernohara1@bu.edu", "sdfsdfs");
        // Student student2 = new Student("Eric2", "ernohara2@bu.edu", "sdfsdfs");
        // Student student3 = new Student("Eric3", "ernohara3@bu.edu", "sdfsdfs");
        // UserDAO.getInstance().create(student1);
        // UserDAO.getInstance().create(student2);
        // UserDAO.getInstance().create(student3);

        Student student1 = (Student) UserDAO.getInstance().readByEmail("ernohara1@bu.edu");
        Student student2 = (Student) UserDAO.getInstance().readByEmail("ernohara2@bu.edu");
        Student student3 = (Student) UserDAO.getInstance().readByEmail("ernohara3@bu.edu");

        Assignment assignment = AssignmentDAO.getInstance().readAll().get(0);

        // Submission submission1 = new Submission(assignment.getId(), "", Submission.Status.UNGRADED, Collections.singletonList(student1.getId()));
        // Submission submission2 = new Submission(assignment.getId(), "", Submission.Status.UNGRADED, Collections.singletonList(student2.getId()));
        // Submission submission3 = new Submission(assignment.getId(), "", Submission.Status.UNGRADED, Collections.singletonList(student3.getId()));
        // SubmissionDAO.getInstance().create(submission1);
        // SubmissionDAO.getInstance().create(submission2);
        // SubmissionDAO.getInstance().create(submission3);

        Submission submission1 = SubmissionDAO.getInstance().read(1);
        submission1.setCollaborators(Collections.singletonList(student1.getId()));
        Submission submission2 = SubmissionDAO.getInstance().read(2);
        submission2.setCollaborators(Collections.singletonList(student2.getId()));
        Submission submission3 = SubmissionDAO.getInstance().read(3);
        submission3.setCollaborators(Collections.singletonList(student3.getId()));

        // Create 3 text files with different content
        try {
            // Create temporary directory for files
            Path tempDir = Files.createTempDirectory("submissions");
            tempDir.toFile().deleteOnExit(); // Clean up on exit

            Path file1 = tempDir.resolve("file1.txt");
            Files.write(file1, "First test document.".getBytes());
        
            Path file2 = tempDir.resolve("file2.txt");
            Files.write(file2, "asdfsdafdsafdsfdsfsdfs".getBytes());
        
            Path file3 = tempDir.resolve("file3.txt");
            Files.write(file3, "This is the first test document.\nAnd here is an addition to throw it off!".getBytes());

            SubmissionFileManager.uploadSubmission(file1.toFile(), submission1);
            SubmissionFileManager.uploadSubmission(file2.toFile(), submission2);
            SubmissionFileManager.uploadSubmission(file3.toFile(), submission3);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Teacher teacher = (Teacher) UserDAO.getInstance().read(3);

        System.out.println(teacher.getMaxSimilarity(submission1));
        System.out.println(teacher.getMaxSimilarity(submission2));
        System.out.println(teacher.getMaxSimilarity(submission3));
    }
}
