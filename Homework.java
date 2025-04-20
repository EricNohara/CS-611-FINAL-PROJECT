import java.sql.Timestamp;

final class Homework extends AbstractAssignment {
    Homework(String id, String title, Timestamp dueDate, double maxPoints, double weight) {
        super(id, title, dueDate, maxPoints, weight);
    }
}