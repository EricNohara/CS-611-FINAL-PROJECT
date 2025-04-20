import java.sql.Timestamp;

public interface Assignment {
    enum Type { HOMEWORK, QUIZ, EXAM, PROJECT }

    String getId();          // unique per course‑instance
    String getTitle();
    Timestamp getDueDate();
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