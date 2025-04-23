import java.util.List;

public class AssignmentTemplate {
    private int id;
    private int courseTemplateId;
    private double weight;
    private Assignment.Type type;
    private List<String> submissionTypes; // list of allowed file extensions

    public AssignmentTemplate(int courseTemplateId, double weight, Assignment.Type type, List<String> submissionTypes) {
        if (weight < 0 || weight > 1) throw new IllegalArgumentException("weight must be in [0,1]");

        this.id = -1; // set this value after inserting into DB
        this.courseTemplateId = courseTemplateId;
        this.weight = weight;
        this.type = type;
        this.submissionTypes = submissionTypes;
    }

    public AssignmentTemplate(int id, int courseTemplateId, double weight, Assignment.Type type, List<String> submissionTypes) {
        this.id = id;
        this.courseTemplateId = courseTemplateId;
        this.weight = weight;
        this.type = type;
        this.submissionTypes = submissionTypes;
    }

    // GETTERS
    public int getId() { return id; }
    public int getCourseTemplateId() { return courseTemplateId; }
    public double getWeight() { return weight; }
    public Assignment.Type getType() { return type; }
    public List<String> getSubmissionTypes() { return submissionTypes; }

    // SETTERS
    public void setId(int id) { this.id = id; }
    public void setCourseTemplateId(int courseTemplateId) { this.courseTemplateId = courseTemplateId; }
    public void setWeight(double weight) { this.weight = weight; }
    public void setType(Assignment.Type type) { this.type = type; }
    public void setSubmissionTypes(List<String> submissionTypes) { this.submissionTypes = submissionTypes; }

}
