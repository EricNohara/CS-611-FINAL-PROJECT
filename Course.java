import java.util.*;
import java.util.stream.Collectors;

public class Course {
    private int id;
    private int courseTemplateId;
    private String name;
    private boolean active;
    private final List<Assignment> assignments = new ArrayList<>();

    // Constructors
    public Course() {
        this.id = -1;
        this.active = true;
    }
    
    public Course(int courseTemplateId, String name) {
        this.id = -1;
        this.courseTemplateId = courseTemplateId;
        this.name = name;
        this.active = true;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getCourseTemplateId() { return courseTemplateId; }
    public void setCourseTemplateId(int courseTemplateId) { this.courseTemplateId = courseTemplateId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public List<Assignment> getAssignments() { return Collections.unmodifiableList(assignments); }
    
    // Assignment methods
    public void addAssignment(Assignment a) { assignments.add(a); }
    
    public void removeAssignment(Assignment a) { assignments.remove(a); }

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