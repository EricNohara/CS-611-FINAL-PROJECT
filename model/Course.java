package model;
import java.util.*;
// import java.util.stream.Collectors;

public class Course {
    private int id;
    private int courseTemplateId;
    private String name;
    private boolean active;
    private List<Assignment> assignments;
    private CourseTemplate courseTemplate;

    // Constructors
    public Course() {
        this.id = -1;
        this.active = true;
        this.courseTemplate = null;
        this.courseTemplateId = -1;
        this.assignments = new ArrayList<>();
        this.name = "";
    }
    
    public Course(CourseTemplate courseTemplate, String name) {
        this.id = -1;
        this.courseTemplate = courseTemplate;
        this.courseTemplateId = courseTemplate != null ? courseTemplate.getId() : -1;
        this.name = name;
        this.active = true;
        this.assignments = new ArrayList<>();
    }

    public Course(int id, int courseTemplateId, String name, boolean active, List<Assignment> assignments, CourseTemplate courseTemplate) {
        this.id = id;
        this.courseTemplateId = courseTemplateId;
        this.name = name;
        this.active = active;
        this.assignments = assignments;
        this.courseTemplate = courseTemplate;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getCourseTemplateId() {
        return courseTemplateId;
    }
    public void setCourseTemplateId(int courseTemplateId) {this.courseTemplateId = courseTemplateId;}

    public CourseTemplate getCourseTemplate() { return courseTemplate; }
    public void setCourseTemplate(CourseTemplate courseTemplate) { this.courseTemplate = courseTemplate; }
    
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public List<Assignment> getAssignments() { return Collections.unmodifiableList(assignments); }
    public void setAssignments(List<Assignment> assignments) { this.assignments = assignments; }
    
    // Assignment methods
    public void addAssignment(Assignment a) { assignments.add(a); }
    
    public void removeAssignment(Assignment a) { assignments.remove(a); }

    public double finalAverage(Map<String, Double> earnedPointsById) {
        return assignments.stream()
                .mapToDouble(a -> a.contribution(earnedPointsById.getOrDefault(a.getId(), 0.0)))
                .sum();
    }

    // public Map<Assignment.Type, List<Assignment>> byType() {
    //     return assignments.stream().collect(Collectors.groupingBy(a -> a.getClass()
    //                                                           .getSimpleName()
    //                                                           .toUpperCase()
    //                                                           .contains("EXAM")
    //                                                           ? Assignment.Type.EXAM
    //                                                           : a instanceof Quiz ? Assignment.Type.QUIZ
    //                                                                               : a instanceof Project ? Assignment.Type.PROJECT
    //                                                                                                      : Assignment.Type.HOMEWORK));
    // }
}