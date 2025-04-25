package ui.utils;

// Helper class for course items in combo box
public class CourseItem {
    private int id;
    private String name;
    
    public CourseItem(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}