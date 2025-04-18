import java.time.LocalDate;
import java.time.LocalTime;

final class Quiz extends AbstractAssignment {
    Quiz(String id, String title, LocalDate dueDate, LocalTime dueTime, double maxPoints, double weight) {
        super(id, title, dueDate, dueTime, maxPoints, weight);
    }
}