import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AssignmentTemplateDAO implements CrudDAO<AssignmentTemplate> {
    // SINGLETON ACCESS
    private static final AssignmentTemplateDAO instance = new AssignmentTemplateDAO();

    private AssignmentTemplateDAO() {}

    public static AssignmentTemplateDAO getInstance() { return instance; }

    // ABSTRACT CRUD OPERATION IMPLEMENTATIONS
    @Override
    public void create(AssignmentTemplate template) {
        String query = "INSERT INTO assignment_templates (course_template_id, weight, type, submission_types) VALUES (?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, template.getCourseTemplateId());
            stmt.setDouble(2, template.getWeight());
            stmt.setInt(3, template.getType().ordinal());
            stmt.setString(4, template.getSubmissionTypes().stream().collect(Collectors.joining(","))); // csv of file extensions

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creating assignment template failed, no rows affected.");

            // update relevant fields in User object
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int templateId = generatedKeys.getInt(1);
                    template.setId(templateId); // set the generated id here
                } else {
                    throw new SQLException("Creating assignment template failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating assignment template: " + e.getMessage());
        }
    }

    @Override
    public AssignmentTemplate read(int id) {
        String query = "SELECT * FROM assignment_templates WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) return buildFromResultSet(rs);
        } catch (SQLException e) {
            System.err.println("Error reading assignment template: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<AssignmentTemplate> readAllCondition(String columnName, Object value) {
        String query = "SELECT * FROM assignment_templates WHERE " + columnName.trim() + " = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();
            List<AssignmentTemplate> templates = new ArrayList<>();
    
            while (rs.next()) templates.add(buildFromResultSet(rs));
            return templates;
        } catch (SQLException e) {
            System.err.println("Error reading assignment template: " + e.getMessage());
        }
        return null;
    }
    
    @Override
    public List<AssignmentTemplate> readAll() {
        List<AssignmentTemplate> templates = new ArrayList<>();
        String query = "SELECT * FROM assignment_templates";

        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                templates.add(buildFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error reading all assignment templates: " + e.getMessage());
        }

        return templates;
    }
    
    @Override
    public void update(AssignmentTemplate template) {
        String query = "UPDATE assignment_templates SET course_template_id = ?, weight = ?, type = ?, submission_types = ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, template.getCourseTemplateId());
            stmt.setDouble(2, template.getWeight());
            stmt.setInt(3, template.getType().ordinal());
            stmt.setString(4, template.getSubmissionTypes().stream().collect(Collectors.joining(",")));
            stmt.setInt(5, template.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating assignment template failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating assignment template: " + e.getMessage());
        }
    }
    
    @Override
    public void delete(int id) {
        String query = "DELETE FROM assignment_templates WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting assignment template failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting assignment template: " + e.getMessage());
        }
    }
    
    @Override
    public AssignmentTemplate buildFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int courseTemplateId = rs.getInt("course_template_id");
        double weight = rs.getDouble("weight");
        Assignment.Type type = Assignment.Type.values()[rs.getInt("type")];
        String submissionTypesCsv = rs.getString("submission_types");
        List<String> submissionTypes = new ArrayList<>();

        // convert CSV in table to list of valid file extensions
        if (submissionTypesCsv != null && !submissionTypesCsv.isEmpty()) {
            String[] typesArray = submissionTypesCsv.split(",");
            for (String t : typesArray) {
                submissionTypes.add(t.trim());
            }
        }
        
        return new AssignmentTemplate(id, courseTemplateId, weight, type, submissionTypes);
    }
}
