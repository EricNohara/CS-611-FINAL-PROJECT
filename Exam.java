import java.sql.Timestamp;

final class Exam extends AbstractAssignment {
    Exam(String id, String title, Timestamp dueDate,  double maxPoints, double weight) {
        super(id, title, dueDate, maxPoints, weight);
    }
}