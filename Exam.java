import java.sql.Timestamp;

final class Exam extends Assignment {
    Exam(int id, String title, Timestamp dueDate,  double maxPoints, AssignmentTemplate template, int courseId) {
        super(id, title, dueDate, maxPoints, template, courseId);
    }
}