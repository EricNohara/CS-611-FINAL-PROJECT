package utils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import db.DBConnection;
import java.io.*;

public class DBUtils {
    private static final String clearAllUserSubmissionsQuery = "DELETE FROM user_submissions;";
    private static final String clearAllUserCoursesQuery = "DELETE FROM user_courses;";
    private static final String clearAllUsersQuery = "DELETE FROM users WHERE role != 3;"; // all but the admin
    private static final String clearAllCourseTempQuery = "DELETE FROM course_templates;";
    private static final String clearAllAssignmentTempQuery = "DELETE FROM assignment_templates;";
    private static final String clearAllCoursesQuery = "DELETE FROM courses;";
    private static final String clearAllAssignmentsQuery = "DELETE FROM assignments;";
    private static final String clearAllSubmissionsQuery = "DELETE FROM submissions;";
    private static final String clearAllSqliteSeqQuery = "DELETE FROM sqlite_sequence;";

    private static final String[] clearAllTablesQuery = {
        clearAllUserSubmissionsQuery,
        clearAllUserCoursesQuery,
        clearAllUsersQuery,
        clearAllCourseTempQuery,
        clearAllAssignmentTempQuery,
        clearAllCoursesQuery,
        clearAllAssignmentsQuery,
        clearAllSubmissionsQuery,
        clearAllSqliteSeqQuery
    };

    public static boolean clearAllTables() {
        try (Connection connection = DBConnection.getConnection();
            Statement stmt = connection.createStatement()) {
            for (String query : clearAllTablesQuery) stmt.execute(query);
        } catch (SQLException e) {
            System.err.println("Error clearing all tables: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean backupDatabase() {
        File dbFile = new File("./data/database.db");
        if (!dbFile.exists()) {
            System.err.println("Database file not found.");
            return false;
        }
    
        File backupDir = new File("./data/backups/");
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            System.err.println("Failed to create backup directory.");
            return false;
        }
    
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new java.util.Date());
        File backupFile = new File(backupDir, "backup_" + timestamp + ".db");
    
        try {
            java.nio.file.Files.copy(
                dbFile.toPath(),
                backupFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            return true;
        } catch (IOException e) {
            System.err.println("Failed to backup database: " + e.getMessage());
            return false;
        }
    }

    public static boolean isValidBackup(File file) {
        try (Connection testConn = DriverManager.getConnection("jdbc:sqlite:" + file.getPath())) {
            return true; // connection succeeded
        } catch (SQLException e) {
            return false;
        }
    }

    public static void checkConnectionCount() {
        try (Connection connection = DBConnection.getConnection()) {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("PRAGMA database_list;");
            
            while (rs.next()) {
                System.out.println("Database: " + rs.getString("file"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean loadBackup(File backup) {
        if (!backup.exists()) {
            System.err.println("Selected backup file does not exist.");
            return false;
        }
    
        if (!isValidBackup(backup)) {
            System.err.println("Selected file is not a valid backup.");
            return false;
        }

        if (!backupDatabase()) {
            System.err.println("Error creating backup of current database.");
            return false;
        }
    
        File pendingRestore = new File("./data/database_restore_pending.db");
    
        try {   
            // Copy the selected backup to the pending restore path
            java.nio.file.Files.copy(
                backup.toPath(),
                pendingRestore.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            System.out.println("Backup marked for restore on next startup.");
            return true;
        } catch (IOException e) {
            System.err.println("Failed to mark backup for restore: " + e.getMessage());
            return false;
        }
    }
}
