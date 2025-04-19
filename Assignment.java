import java.time.LocalDate;
import java.time.LocalTime;

public interface Assignment {
    enum Type { HOMEWORK, QUIZ, EXAM, PROJECT }

    String getId();          // unique per course‑instance
    String getTitle();
    LocalDate getDueDate();
    LocalTime getDueTime();
    double getMaxPoints();   // denominator
    double getWeight();      // used in weighted average

    /** Returns the contribution earnedPoints / maxPoints · weight */
    default double contribution(double earnedPoints) {
        if (earnedPoints < 0.0 || earnedPoints > getMaxPoints()) {
            throw new IllegalArgumentException("earnedPoints out of range");
        }

        return earnedPoints / getMaxPoints() * getWeight();
    }
}