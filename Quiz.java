import java.sql.Timestamp;

final class Quiz extends AbstractAssignment {
    Quiz(String id, String title, Timestamp dueDate, double maxPoints, double weight) {
        super(id, title, dueDate, maxPoints, weight);
    }
}