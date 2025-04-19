import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String DATABASE_URL = "jdbc:sqlite:./data/database.db";
    private static Connection connection;

    // Singleton connection to the DB
    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DATABASE_URL);  // SQLite will create the DB if it doesn't exist
        }
        return connection;
    }
}