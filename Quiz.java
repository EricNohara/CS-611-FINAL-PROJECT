import java.sql.Timestamp;

final class Quiz extends Assignment {
    Quiz(int id, String title, Timestamp dueDate, double maxPoints, AssignmentTemplate template, int courseId) {
        super(id, title, dueDate, maxPoints, template, courseId);
    }
}