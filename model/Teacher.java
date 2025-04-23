import java.sql.Timestamp;

public class Teacher extends User implements SubmissionGrader, CourseManager {
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
    public void gradeSubmission(Submission submission) {}

    @Override
    public void addUser(User user, Course course) {}

    @Override
    public void removeUser(User user, Course course) {}

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
    public Assignment createAssignment(Course course) { return null; }

    @Override
    public Assignment editAddAssignment(Assignment assignment) { return null; }

    @Override
    public void deleteAssignment(Assignment assignment) {}
}
