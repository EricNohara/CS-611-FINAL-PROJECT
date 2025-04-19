import java.time.LocalDate;
import java.time.LocalTime;

final class Project extends AbstractAssignment {
    Project(String id, String title, LocalDate dueDate, LocalTime dueTime, double maxPoints, double weight) {
        super(id, title, dueDate, dueTime, maxPoints, weight);
    }
}
