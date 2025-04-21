import java.sql.Timestamp;


public final class AssignmentFactory {
    private AssignmentFactory() {}         

    public static Assignment create(Assignment.Type type, String id, String title, Timestamp dueDate, double maxPoints, AssignmentTemplate template) {
        switch (type) {
            case HOMEWORK: return new Homework(id, title, dueDate, maxPoints, template);
            case QUIZ: return new Quiz(id, title, dueDate, maxPoints, template);
            case EXAM: return new Exam(id, title, dueDate, maxPoints, template);
            case PROJECT: return new Project(id, title, dueDate, maxPoints, template);
            default: throw new UnsupportedOperationException("Unknown Type: " + type);
        }
    }
}