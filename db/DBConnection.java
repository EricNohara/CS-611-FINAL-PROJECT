package db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String DATABASE_URL = "jdbc:sqlite:./data/database.db";

    // Singleton connection to the DB
    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DATABASE_URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON;");
        return conn;
    }
}