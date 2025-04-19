import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DBSetup {
    private static final String createUserTableQuery = "CREATE TABLE IF NOT EXISTS users ("
                                                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                                    + "name TEXT NOT NULL,"
                                                    + "email TEXT UNIQUE NOT NULL,"
                                                    + "password_hash TEXT NOT NULL,"
                                                    + "role INTEGER NOT NULL,"
                                                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                                                    + "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";

    // SQL queries for other tables...

    public static void createTables() {
        try (Connection connection = DBConnection.getConnection();
            Statement stmt = connection.createStatement()) {
            stmt.execute(createUserTableQuery);  // create the user table if it doesnt already exist

            // create other tables...
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
