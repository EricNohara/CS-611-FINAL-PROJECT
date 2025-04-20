import java.sql.Timestamp;

final class Exam extends AbstractAssignment {
    Exam(String id, String title, Timestamp dueDate,  double maxPoints, AssignmentTemplate template) {
        super(id, title, dueDate, maxPoints, template);
    }
}