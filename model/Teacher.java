package model;
import java.sql.Timestamp;

import db.AssignmentDAO;
import db.SubmissionDAO;
import db.UserCourseDAO;

public class Teacher extends User implements SubmissionGrader, CourseManager {
    private static UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();

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
    public CourseTemplate createCourseTemplate() { return null; }

    @Override
    public CourseTemplate editCourseTemplate(CourseTemplate template) { return null; }

    @Override
    public void deleteCourseTemplate(CourseTemplate template) {}

    @Override
    public Course createCourse(CourseTemplate template) { return null; }

    @Override
    public Course editCourse(Course course) { return null; }

    @Override
    public void deleteCourse(Course course) {}

    @Override
    public Assignment createAssignment(Course course, AssignmentTemplate template) { return null; }

    @Override
    public Assignment editAssignment(Assignment assignment) { return null; }

    @Override
    public void deleteAssignment(Assignment assignment) {}
}
