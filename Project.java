import java.sql.Timestamp;

final class Project extends AbstractAssignment {
    Project(String id, String title, Timestamp dueDate, double maxPoints, double weight) {
        super(id, title, dueDate, maxPoints, weight);
    }
}
