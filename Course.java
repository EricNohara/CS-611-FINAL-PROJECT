import java.util.*;
import java.util.stream.Collectors;

public class Course {
    private final List<Assignment> assignments = new ArrayList<>();

    public void addAssignment(Assignment a) { assignments.add(a); }

    public double finalAverage(Map<String, Double> earnedPointsById) {
        return assignments.stream()
                .mapToDouble(a -> a.contribution(earnedPointsById.getOrDefault(a.getId(), 0.0)))
                .sum();
    }

    public Map<Assignment.Type, List<Assignment>> byType() {
        return assignments.stream().collect(Collectors.groupingBy(a -> a.getClass()
                                                              .getSimpleName()
                                                              .toUpperCase()
                                                              .contains("EXAM")
                                                              ? Assignment.Type.EXAM
                                                              : a instanceof Quiz ? Assignment.Type.QUIZ
                                                                                  : a instanceof Project ? Assignment.Type.PROJECT
                                                                                                         : Assignment.Type.HOMEWORK));
    }
}