package db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import model.User;
import model.UserCourse;

public class UserCourseDAO implements CrudDAO<UserCourse> {
    // SINGLETON ACCESS
    private static final UserCourseDAO instance = new UserCourseDAO();

    private UserCourseDAO() {}

    public static UserCourseDAO getInstance() { return instance; }

    // ABSTRACT CRUD OPERATION IMPLEMENTATIONS
    @Override
    public void create(UserCourse userCourse) {
        String query = "INSERT INTO user_courses (user_id, course_id, status, role) VALUES (?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, userCourse.getUserId());
            stmt.setInt(2, userCourse.getCourseId());
            stmt.setInt(3, userCourse.getStatus().ordinal());
            stmt.setInt(4, userCourse.getRole().ordinal());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creating user course relationship failed, no rows affected.");
        } catch (SQLException e) {
            System.err.println("Error creating user course relationship: " + e.getMessage());
        }
    }
    
    @Override
    public UserCourse read(int id) { return null; }

    public UserCourse read(int userId, int courseId) {
        String query = "SELECT * FROM user_courses WHERE user_id = ? AND course_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, userId);
            stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) return buildFromResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public List<UserCourse> readAllCondition(String columnName, Object value) {
        String query = "SELECT * FROM user_courses WHERE " + columnName.trim() + " = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();
            List<UserCourse> userCourses = new ArrayList<>();
    
            while (rs.next()) userCourses.add(buildFromResultSet(rs));
            return userCourses;
        } catch (SQLException e) {
            System.err.println("Error reading user course relationships: " + e.getMessage());
        }
        return null;
    }
    
    @Override
    public List<UserCourse> readAll() {
        List<UserCourse> userCourses = new ArrayList<>();
        String query = "SELECT * FROM user_courses";

        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) userCourses.add(buildFromResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Error reading all user course relationships: " + e.getMessage());
        }

        return userCourses;
    }
    
    @Override
    public void update(UserCourse userCourse) {
        String query = "UPDATE user_courses SET status = ?, role = ? WHERE user_id = ? AND course_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, userCourse.getStatus().ordinal());
            stmt.setInt(2, userCourse.getRole().ordinal());
            stmt.setInt(3, userCourse.getUserId());
            stmt.setInt(4, userCourse.getCourseId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Updating user course relationship failed, no rows affected.");
        } catch (SQLException e) {
            System.err.println("Error updating user course relationship: " + e.getMessage());
        }
    }
    
    @Override
    public void delete(int id) {}

    public void delete(int userId, int courseId) {
        String query = "DELETE FROM user_courses WHERE user_id = ? AND course_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, courseId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting user course relationship failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user course relationship: " + e.getMessage());
        }
    }
    
    @Override
    public UserCourse buildFromResultSet(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        int courseId = rs.getInt("course_id");
        UserCourse.Status status = UserCourse.Status.values()[rs.getInt("status")];
        User.Role role = User.Role.values()[rs.getInt("role")];

        return new UserCourse(userId, courseId, status, role);
    }
}
