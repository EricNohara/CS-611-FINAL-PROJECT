import java.sql.Timestamp;

final class Project extends AbstractAssignment {
    Project(String id, String title, Timestamp dueDate, double maxPoints, AssignmentTemplate template) {
        super(id, title, dueDate, maxPoints, template);
    }
}
