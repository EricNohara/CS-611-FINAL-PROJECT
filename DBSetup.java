import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DBSetup {
    private static final String createUsersQuery = "CREATE TABLE IF NOT EXISTS users ("
                                                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                                    + "name TEXT NOT NULL,"
                                                    + "email TEXT UNIQUE NOT NULL,"
                                                    + "password_hash TEXT NOT NULL,"
                                                    + "role INTEGER NOT NULL,"
                                                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                                                    + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";

    private static final String createCourseTemplatesQuery = "CREATE TABLE IF NOT EXISTS course_templates (" +
                                                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                            "name TEXT NOT NULL);";

    private static final String createAssignmentTemplatesQuery = "CREATE TABLE IF NOT EXISTS assignment_templates (" +
                                                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                                "course_template_id INTEGER," +
                                                                "count INTEGER," +
                                                                "weight REAL," +
                                                                "type INTEGER," +
                                                                "submission_types TEXT," +
                                                                "FOREIGN KEY (course_template_id) REFERENCES course_templates(id));";

    private static final String createCoursesQuery = "CREATE TABLE IF NOT EXISTS courses (" +
                                                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                    "course_template_id INTEGER," +
                                                    "name TEXT," +
                                                    "FOREIGN KEY (course_template_id) REFERENCES course_templates(id));";

    private static final String createUserCoursesQuery = "CREATE TABLE IF NOT EXISTS user_courses (" +
                                                        "user_id INTEGER," +
                                                        "course_id INTEGER," +
                                                        "PRIMARY KEY (user_id, course_id)," +
                                                        "FOREIGN KEY (user_id) REFERENCES users(id)," +
                                                        "FOREIGN KEY (course_id) REFERENCES courses(id));";

    private static final String createSubmissionsQuery = "CREATE TABLE IF NOT EXISTS submissions (" + 
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," + 
                                                        "assignment_id INTEGER," + 
                                                        "grader_id INTEGER," + 
                                                        "filepath TEXT," + 
                                                        "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," + 
                                                        "grade REAL," + 
                                                        "status INTEGER," + 
                                                        "FOREIGN KEY (grader_id) REFERENCES users(id)," + 
                                                        "FOREIGN KEY (assignment_id) REFERENCES assignments(id));";

    public static final String createUserSubmissionsQuery = "CREATE TABLE IF NOT EXISTS user_submissions (" +
                                                            "user_id INTEGER," +
                                                            "submission_id INTEGER," +
                                                            "PRIMARY KEY (user_id, submission_id)," +
                                                            "FOREIGN KEY (user_id) REFERENCES users(id)," +
                                                            "FOREIGN KEY (submission_id) REFERENCES submissions(id));";

    private static final String[] createTableQueries = {
        createUsersQuery, 
        createCourseTemplatesQuery, 
        createAssignmentTemplatesQuery, 
        createCoursesQuery, 
        createUserCoursesQuery,
        createSubmissionsQuery,
        createUserSubmissionsQuery
    };

    public static void createTables() {
        try (Connection connection = DBConnection.getConnection();
            Statement stmt = connection.createStatement()) {
            for (String query : createTableQueries) stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
