import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDAO implements CrudDAO<Assignment> {
    // ABSTRACT CRUD OPERATIONS IMPLEMENTATIONS
    @Override
    public void create(Assignment assignment) {
        String query = "INSERT INTO assignments (name, due_date, max_points, assignment_template_id, course_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, assignment.getName());
            stmt.setTimestamp(2, assignment.getDueDate());
            stmt.setDouble(3, assignment.getMaxPoints());
            stmt.setInt(4, assignment.getTemplate().getId());
            stmt.setInt(5, assignment.getCourseId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creating assignment failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    assignment.setId(id); // set the generated id here
                } else {
                    throw new SQLException("Creating assignment failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating assignment: " + e.getMessage());
        }
    }
    
    @Override
    public Assignment read(int id) {
        String query = "SELECT * FROM assignments WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) return buildFromResultSet(rs);
        } catch (SQLException e) {
            System.err.println("Error reading assignment: " + e.getMessage());
        }
        return null;
    }
    
    @Override
    public List<Assignment> readAll() {
        List<Assignment> assignments = new ArrayList<>();
        String query = "SELECT * FROM assignments";

        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) assignments.add(buildFromResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Error reading all assignment: " + e.getMessage());
        }

        return assignments;
    }
    
    @Override
    public void update(Assignment assignment) {
        String query = "UPDATE assignments SET name = ?, due_date = ?, max_points = ?, assignment_template_id = ?, course_id = ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, assignment.getName());
            stmt.setTimestamp(2, assignment.getDueDate());
            stmt.setDouble(3, assignment.getMaxPoints());
            stmt.setInt(4, assignment.getTemplate().getId());
            stmt.setInt(5, assignment.getCourseId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating assignment failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating assignment: " + e.getMessage());
        }
    }
    
    @Override
    public void delete(int id) {
        String query = "DELETE FROM assignments WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting assignment failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting assignment: " + e.getMessage());
        }
    }
    
    @Override
    public Assignment buildFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        Timestamp dueDate = rs.getTimestamp("due_date");
        int maxPoints = rs.getInt("max_points");
        int assignmentTemplateId = rs.getInt("assignment_template_id");
        int courseId = rs.getInt("course_id");

        AssignmentTemplateDAO templateDAO = AssignmentTemplateDAO.getInstance();
        
        return new Assignment(id, name, dueDate, maxPoints, templateDAO.read(assignmentTemplateId), courseId);
    }
}
