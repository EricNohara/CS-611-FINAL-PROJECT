import java.sql.Timestamp;

final class Homework extends Assignment {
    Homework(int id, String title, Timestamp dueDate, double maxPoints, AssignmentTemplate template, int courseId) {
        super(id, title, dueDate, maxPoints, template, courseId);
    }
}