import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CourseTemplateDAO implements CrudDAO<CourseTemplate> {
    // SINGLETON ACCESS
    private static final CourseTemplateDAO instance = new CourseTemplateDAO();

    private CourseTemplateDAO() {}

    public static CourseTemplateDAO getInstance() { return instance; }

    // ABSTRACT CRUD OPERATION IMPLEMENTATIONS
    @Override
    public void create(CourseTemplate template) {
        String courseTemplateQuery = "INSERT INTO course_templates (name) VALUES (?)";
        String assignmentTemplatesQuery = "INSERT INTO assignment_templates (course_template_id, weight, type, submission_types) VALUES (?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false); // BEGIN TRANSACTION

            try (PreparedStatement stmt = connection.prepareStatement(courseTemplateQuery, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, template.getName());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Creating course template failed, no rows affected.");

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int templateId = generatedKeys.getInt(1);
                        template.setId(templateId);

                        // now insert all assignment templates
                        try (PreparedStatement assignTempStmt = connection.prepareStatement(assignmentTemplatesQuery, Statement.RETURN_GENERATED_KEYS)) {
                            for (AssignmentTemplate assignTemp : template.getAssignmentTemplates()) {
                                assignTempStmt.setInt(1, templateId);
                                assignTempStmt.setDouble(2, assignTemp.getWeight());
                                assignTempStmt.setInt(3, assignTemp.getType().ordinal());
                                assignTempStmt.setString(4, assignTemp.getSubmissionTypes().stream().collect(Collectors.joining(","))); // csv of file extensions
                                assignTempStmt.addBatch();
                            }
                            assignTempStmt.executeBatch();

                            // populate each assignmentTemplate with its generated ID
                            try (ResultSet assignKeys = assignTempStmt.getGeneratedKeys()) {
                                int index = 0;
                                while (assignKeys.next() && index < template.getAssignmentTemplates().size()) {
                                    int assignId = assignKeys.getInt(1);
                                    template.getAssignmentTemplates().get(index).setId(assignId);
                                    index++;
                                }
                            }
                        }
                    } else {
                        throw new SQLException("Creating submission failed, no ID obtained.");
                    }
                }

                connection.commit(); // COMMIT if everything succeeds
            } catch (SQLException e) {
                connection.rollback(); // ROLLBACK on any error
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error creating submission: " + e.getMessage());
        }
    }

    @Override
    public CourseTemplate read(int id) {
        String query = "SELECT * FROM course_templates WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) return buildFromResultSet(rs);
        } catch (SQLException e) {
            System.err.println("Error reading course template: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<CourseTemplate> readAllCondition(String columnName, Object value) {
        String query = "SELECT * FROM course_templates WHERE " + columnName.trim() + " = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();
            List<CourseTemplate> templates = new ArrayList<>();
    
            while (rs.next()) templates.add(buildFromResultSet(rs));
            return templates;
        } catch (SQLException e) {
            System.err.println("Error reading course template: " + e.getMessage());
        }
        return null;
    }
    
    @Override
    public List<CourseTemplate> readAll() {
        List<CourseTemplate> templates = new ArrayList<>();
        String query = "SELECT * FROM course_templates";

        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) templates.add(buildFromResultSet(rs));
        } catch (SQLException e) {
            System.err.println("Error reading all course templates: " + e.getMessage());
        }

        return templates;
    }
    
    @Override
    public void update(CourseTemplate template) {
        String updateCourseTemplatesQuery = "UPDATE course_templates SET name = ? WHERE id = ?";
        String deleteAssignmentTemplatesQuery = "DELETE FROM assignment_templates WHERE course_template_id = ?";
        String addAssignmentTemplatesQuery = "INSERT INTO assignment_templates (course_template_id, weight, type, submission_types) VALUES (?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false); // start transaction

            try {
                // update course template record
                try (PreparedStatement stmt = connection.prepareStatement(updateCourseTemplatesQuery)) {
                    stmt.setString(1, template.getName());
                    stmt.setInt(2, template.getId());
                    stmt.executeUpdate();
                }

                // delete old assignment templates
                try (PreparedStatement stmt = connection.prepareStatement(deleteAssignmentTemplatesQuery)) {
                    stmt.setInt(1, template.getId());
                    stmt.executeUpdate();
                }

                // insert new assignment templates
                try (PreparedStatement stmt = connection.prepareStatement(addAssignmentTemplatesQuery, Statement.RETURN_GENERATED_KEYS)) {
                    for (AssignmentTemplate assignTemp : template.getAssignmentTemplates()) {
                        stmt.setInt(1, assignTemp.getCourseTemplateId());
                        stmt.setDouble(2, assignTemp.getWeight());
                        stmt.setInt(3, assignTemp.getType().ordinal());
                        stmt.setString(4, assignTemp.getSubmissionTypes().stream().collect(Collectors.joining(","))); // csv of file extensions
                        stmt.addBatch();
                    }
                    stmt.executeBatch();

                    // populate each assignmentTemplate with its generated ID
                    try (ResultSet assignKeys = stmt.getGeneratedKeys()) {
                        int index = 0;
                        while (assignKeys.next() && index < template.getAssignmentTemplates().size()) {
                            int assignId = assignKeys.getInt(1);
                            template.getAssignmentTemplates().get(index).setId(assignId);
                            index++;
                        }
                    }
                }

                connection.commit(); // commit transaction
            } catch (SQLException e) {
                connection.rollback(); // rollback on any half deleted states
                throw e; // rethrow to trigger the outer catch block
            }
        } catch (SQLException e) {
            System.err.println("Error updating submission: " + e.getMessage());
        }
    }
    
    @Override
    public void delete(int id) {
        String query = "DELETE FROM course_templates WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting course template failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting course template: " + e.getMessage());
        }
    }
    
    @Override
    public CourseTemplate buildFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");

        AssignmentTemplateDAO templateDAO = AssignmentTemplateDAO.getInstance();
        return new CourseTemplate(id, name, templateDAO.readAllCondition("course_template_id", id));
    }
}
