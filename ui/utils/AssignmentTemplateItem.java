package ui.utils;
import java.util.List;

import model.Assignment;
import model.AssignmentTemplate;

public class AssignmentTemplateItem {
    private int id;
    private Assignment.Type type;
    private double weight;
    private List<String> submissionTypes;
    
    public AssignmentTemplateItem(AssignmentTemplate template) {
        this.id = template.getId();
        this.type = template.getType();
        this.weight = template.getWeight();
        this.submissionTypes = template.getSubmissionTypes();
    }
    
    public int getId() {
        return id;
    }
    
    public Assignment.Type getType() {
        return type;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public List<String> getSubmissionTypes() {
        return submissionTypes;
    }
    
    @Override
    public String toString() {
        return type.toString() + " (" + (weight * 100) + "%)";
    }
}