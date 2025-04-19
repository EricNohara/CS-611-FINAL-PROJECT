import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;

abstract class AbstractAssignment implements Assignment {
    private final String    id;
    private final String    title;
    private final LocalDate dueDate;
    private final LocalTime dueTime;
    private final double    maxPoints;
    private final double    weight;

    AbstractAssignment(String id, String title, LocalDate dueDate, LocalTime dueTime, double maxPoints, double weight) {
        Objects.requireNonNull(id); Objects.requireNonNull(title);
        Objects.requireNonNull(dueDate);

        if (maxPoints <= 0) throw new IllegalArgumentException("maxPoints ≤ 0");
        if (weight < 0 || weight > 1) throw new IllegalArgumentException("weight must be in [0,1]");

        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.maxPoints = maxPoints;
        this.weight = weight;
    }

    /* GETTERS */
    @Override public String getId() { return id; }
    @Override public String getTitle() { return title; }
    @Override public LocalDate getDueDate() { return dueDate; }
    @Override public LocalTime getDueTime() { return dueTime; }
    @Override public double getMaxPoints() { return maxPoints; }
    @Override public double getWeight() { return weight; }
}