package db;
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
                                                                "course_template_id INTEGER NOT NULL," +
                                                                "weight REAL," +
                                                                "type INTEGER," +
                                                                "submission_types TEXT," +
                                                                "FOREIGN KEY(course_template_id) REFERENCES course_templates(id) ON DELETE CASCADE);";

    private static final String createCoursesQuery = "CREATE TABLE IF NOT EXISTS courses (" +
                                                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                    "course_template_id INTEGER," +
                                                    "name TEXT NOT NULL," +
                                                    "active INTEGER NOT NULL DEFAULT 1," +
                                                    "FOREIGN KEY (course_template_id) REFERENCES course_templates(id) ON DELETE SET NULL);";

    private static final String createUserCoursesQuery = "CREATE TABLE IF NOT EXISTS user_courses (" +
                                                        "user_id INTEGER," +
                                                        "course_id INTEGER," +
                                                        "status INTEGER NOT NULL," +
                                                        "role INTEGER NOT NULL," +
                                                        "PRIMARY KEY (user_id, course_id)," +
                                                        "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                                                        "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE);";

    private static final String createSubmissionsQuery = "CREATE TABLE IF NOT EXISTS submissions (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "assignment_id INTEGER NOT NULL," +
                                                        "grader_id INTEGER," +
                                                        "filepath TEXT NOT NULL," +
                                                        "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL," +
                                                        "points_earned REAL," +
                                                        "grade REAL," +
                                                        "status INTEGER NOT NULL," +
                                                        "FOREIGN KEY (grader_id) REFERENCES users(id) ON DELETE SET NULL," +
                                                        "FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE);";

    private static final String createUserSubmissionsQuery = "CREATE TABLE IF NOT EXISTS user_submissions (" +
                                                            "user_id INTEGER," +
                                                            "submission_id INTEGER," +
                                                            "PRIMARY KEY (user_id, submission_id)," +
                                                            "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                                                            "FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE);";

    private static final String createAssignmentsQuery = "CREATE TABLE IF NOT EXISTS assignments (" +
                                                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "name TEXT NOT NULL," +
                                                        "due_date TIMESTAMP NOT NULL," +
                                                        "max_points REAL NOT NULL," +
                                                        "course_id INTEGER NOT NULL," +
                                                        "weight REAL NOT NULL," +
                                                        "type INTEGER NOT NULL," +
                                                        "submission_types TEXT," +
                                                        "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE);";

    private static final String clearAllTablesQuery = "DELETE FROM user_submissions;" +
                                                     "DELETE FROM user_courses;" +
                                                     "DELETE FROM users;" +
                                                     "DELETE FROM course_templates;" +
                                                     "DELETE FROM assignment_templates;" +
                                                     "DELETE FROM courses;" +
                                                     "DELETE FROM assignments;" +
                                                     "DELETE FROM submissions;" +
                                                     "DELETE FROM sqlite_sequence;";

    private static final String[] createTableQueries = {
        createUsersQuery, 
        createCourseTemplatesQuery, 
        createAssignmentTemplatesQuery, 
        createCoursesQuery, 
        createUserCoursesQuery,
        createSubmissionsQuery,
        createUserSubmissionsQuery,
        createAssignmentsQuery
    };

    public static void createTables() {
        try (Connection connection = DBConnection.getConnection();
            Statement stmt = connection.createStatement()) {
            for (String query : createTableQueries) stmt.execute(query);
        } catch (SQLException e) {
            System.err.println("Error creating all tables: " + e.getMessage());
        }
    }

    public static void clearAllTables() {
        try (Connection connection = DBConnection.getConnection();
            Statement stmt = connection.createStatement()) {
            stmt.execute(clearAllTablesQuery);
        } catch (SQLException e) {
            System.err.println("Error clearing all tables: " + e.getMessage());
        }
    }
}
