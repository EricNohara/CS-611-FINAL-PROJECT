import java.sql.Timestamp;


public final class AssignmentFactory {
    private AssignmentFactory() {}         

    // public static Assignment create(Assignment.Type type, int id, String name, Timestamp dueDate, double maxPoints, AssignmentTemplate template, int courseId) {
    //     switch (type) {
    //         case HOMEWORK: return new Homework(id, name, dueDate, maxPoints, template, courseId);
    //         case QUIZ: return new Quiz(id, name, dueDate, maxPoints, template, courseId);
    //         case EXAM: return new Exam(id, name, dueDate, maxPoints, template, courseId);
    //         case PROJECT: return new Project(id, name, dueDate, maxPoints, template, courseId);
    //         default: throw new UnsupportedOperationException("Unknown Type: " + type);
    //     }
    // }
}