import java.sql.*;

public class UserDAO {
    public static void createUser(User user) {
        String query = "INSERT INTO users (name, email, password_hash, role, created_at, last_updated) VALUES (?, ?, ?, ?, ?, ?)";
        Timestamp current = new Timestamp(System.currentTimeMillis());

        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setInt(4, user.getRole().ordinal()); // store role enum value
            stmt.setTimestamp(5, current);
            stmt.setTimestamp(6, current);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creating user failed, no rows affected.");

            // update relevant fields in User object
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1)); // set the generated id here
                    user.setCreatedAt(current);
                    user.setLastUpdated(current);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // retrieves user data from table and returns a new User object
    public static User getUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) {
                return buildUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // helper method to build a user from a generic DB row
    private static User buildUserFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        User.Role role = User.Role.values()[rs.getInt("role")];
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp lastUpdated = rs.getTimestamp("last_updated");
    
        switch (role) {
            case STUDENT:
                return new Student(id, name, email, passwordHash, createdAt, lastUpdated);
            case GRADER:
                return new Grader(id, name, email, passwordHash, createdAt, lastUpdated);
            case TEACHER:
                return new Teacher(id, name, email, passwordHash, createdAt, lastUpdated);
            case ADMIN:
                return new Admin(id, name, email, passwordHash, createdAt, lastUpdated);
            default:
                throw new IllegalArgumentException("Unknown role: " + role);
        }
    }
}
