package db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import model.Admin;
import model.Grader;
import model.Student;
import model.Teacher;
import model.User;

public class UserDAO implements CrudDAO<User> {
    // SINGLETON ACCESS
    private static final UserDAO instance = new UserDAO();

    private UserDAO() {}

    public static UserDAO getInstance() { return instance; }

    // ABSTRACT CRUD OPERATIONS
    @Override
    public void create(User user) {
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
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 && e.getMessage().contains("UNIQUE")) {
                System.err.println("Email already exists");
            } else {
                System.err.println("Error adding user");
            }
        }
    }

    @Override
    public User read(int id) {
        String query = "SELECT * FROM users WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) return buildFromResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<User> readAllCondition(String columnName, Object value) {
        String query = "SELECT * FROM assignment_templates WHERE " + columnName.trim() + " = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();
            List<User> users = new ArrayList<>();
    
            while (rs.next()) users.add(buildFromResultSet(rs));
            return users;
        } catch (SQLException e) {
            System.err.println("Error reading users: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<User> readAll() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";

        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(buildFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    @Override
    public void update(User user) {
        String query = "UPDATE users SET name = ?, email = ?, password_hash = ?, role = ?, last_updated = ? WHERE id = ?";
        Timestamp current = new Timestamp(System.currentTimeMillis());

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setInt(4, user.getRole().ordinal());
            stmt.setTimestamp(5, current);
            stmt.setInt(6, user.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, no rows affected.");
            }

            // Update the lastUpdated field in the User object
            user.setLastUpdated(current);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int userId) {
        String query = "DELETE FROM users WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, userId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting user failed, no rows affected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public User buildFromResultSet(ResultSet rs) throws SQLException {
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
                return Admin.getInstance(id, name, email, passwordHash, createdAt, lastUpdated); // return singleton Admin
            default:
                throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    // retrieves user data from table and returns a new User object
    public User readByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) {
                return buildFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
