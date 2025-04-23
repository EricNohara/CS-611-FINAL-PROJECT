import java.util.List;

public class CourseTemplate {
    private int id;
    private String name;
    private List<AssignmentTemplate> assignmentTemplates;

    public CourseTemplate(String name, List<AssignmentTemplate> assignmentTemplates) {
        this.id = -1;
        this.name = name;
        this.assignmentTemplates = assignmentTemplates;
    }

    public CourseTemplate(int id, String name, List<AssignmentTemplate> assignmentTemplates) {
        this.id = id;
        this.name = name;
        this.assignmentTemplates = assignmentTemplates;
    }

    // GETTERS
    public int getId() { return id; } 
    public String getName() { return name; }
    public List<AssignmentTemplate> getAssignmentTemplates() { return assignmentTemplates; }
    
    // SETTERS
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAssignmentTemplates(List<AssignmentTemplate> assignmentTemplates) { this.assignmentTemplates = assignmentTemplates; }
}
