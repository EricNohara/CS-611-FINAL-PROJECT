import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO implements CrudDAO<Course> {
    // SINGLETON ACCESS
    private static final CourseDAO instance = new CourseDAO();

    private CourseDAO() {}

    public static CourseDAO getInstance() { return instance; }

    // ABSTRACT CRUD OPERATIONS
    @Override
    public void create(Course course) {
        String query = "INSERT INTO courses (course_template_id, name, active) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, course.getCourseTemplateId());
            stmt.setString(2, course.getName());
            stmt.setBoolean(3, course.isActive()); // Add active status

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creating course failed, no rows affected.");

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    course.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating course failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Course read(int id) {
        String query = "SELECT * FROM courses WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) {
                return buildFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Course> readAll() {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM courses";
        
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            connection = DBConnection.getConnection();
            stmt = connection.prepareStatement(query);
            rs = stmt.executeQuery();

            while (rs.next()) {
                // Store all data from ResultSet locally before processing
                int id = rs.getInt("id");
                int courseTemplateId = rs.getInt("course_template_id");
                String name = rs.getString("name");
                boolean active = false;
                
                // Check if the active column exists before trying to access it
                try {
                    active = rs.getBoolean("active");
                } catch (SQLException e) {
                    // Column doesn't exist, use default value
                    System.out.println("Active column not found, using default value false");
                }
                
                // Create a new Course object
                Course course = new Course();
                course.setId(id);
                course.setName(name);
                course.setCourseTemplateId(courseTemplateId);
                course.setActive(active);
                
                // Get course template separately
                if (courseTemplateId > 0) {
                    CourseTemplate template = getCourseTemplate(courseTemplateId);
                    course.setCourseTemplate(template);
                }
                
                // Add course to list
                courses.add(course);
            }
        } catch (SQLException e) {
            System.err.println("Error reading all courses: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close resources in reverse order
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                // Don't close connection as it's a singleton
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }

        return courses;
    }


    @Override
    public List<Course> readAllCondition(String columnName, Object value) {
        String query = "SELECT * FROM courses WHERE " + columnName.trim() + " = ?";
        List<Course> courses = new ArrayList<>();
        
        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                courses.add(buildFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error reading courses: " + e.getMessage());
        }
        
        return courses;
    }

    @Override
    public void update(Course course) {
        String query = "UPDATE courses SET course_template_id = ?, name = ?, active = ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, course.getCourseTemplateId());
            stmt.setString(2, course.getName());
            stmt.setBoolean(3, course.isActive());
            stmt.setInt(4, course.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating course failed, no rows affected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int courseId) {
        // First delete from user_courses (junction table)
        deleteUserCoursesRelations(courseId);
        
        // Delete assignments related to this course
        deleteAssignmentsForCourse(courseId);
        
        // Then delete the course
        String query = "DELETE FROM courses WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, courseId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting course failed, no rows affected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // IMPLEMENT THE REQUIRED ABSTRACT METHOD
    @Override
    public Course buildFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int templateId = rs.getInt("course_template_id");
        String name = rs.getString("name");
        boolean active = rs.getBoolean("active");
        
        Course course = new Course();
        course.setId(id);
        course.setName(name);
        course.setCourseTemplateId(templateId);
        course.setActive(active);
        
        // Set course template
        CourseTemplate template = getCourseTemplate(templateId);
        course.setCourseTemplate(template);
        
        // Load assignments for this course
        List<Assignment> assignments = getAssignmentsForCourse(id);
        for (Assignment assignment : assignments) {
            course.addAssignment(assignment);
        }
        
        return course;
    }

    // Helper method to delete user_courses relations
    private void deleteUserCoursesRelations(int courseId) {
        String query = "DELETE FROM user_courses WHERE course_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, courseId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Helper method to delete assignments for a course
    private void deleteAssignmentsForCourse(int courseId) {
        String query = "DELETE FROM assignments WHERE course_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, courseId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // USER-COURSE RELATIONSHIP METHODS
    public void addUserToCourse(int userId, int courseId) {
        String query = "INSERT INTO user_courses (user_id, course_id) VALUES (?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, courseId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeUserFromCourse(int userId, int courseId) {
        String query = "DELETE FROM user_courses WHERE user_id = ? AND course_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, courseId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // COURSE TEMPLATE METHODS
    public CourseTemplate getCourseTemplate(int templateId) {
        String query = "SELECT * FROM course_templates WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, templateId);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) {
                String name = rs.getString("name");
                
                // Get assignment templates for this course template
                List<AssignmentTemplate> assignmentTemplates = getAssignmentTemplatesForCourseTemplate(templateId);
                
                // Create and return course template
                CourseTemplate template = new CourseTemplate(name, assignmentTemplates);
                template.setId(templateId);
                
                return template;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private List<AssignmentTemplate> getAssignmentTemplatesForCourseTemplate(int courseTemplateId) {
        List<AssignmentTemplate> templates = new ArrayList<>();
        
        String query = "SELECT * FROM assignment_templates WHERE course_template_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, courseTemplateId);
            ResultSet rs = stmt.executeQuery();
    
            while (rs.next()) {
                int id = rs.getInt("id");
                double weight = rs.getDouble("weight");
                int typeOrdinal = rs.getInt("type");
                Assignment.Type type = Assignment.Type.values()[typeOrdinal];
                
                // Parse submission types (stored as comma-separated string in DB)
                String submissionTypesStr = rs.getString("submission_types");
                List<String> submissionTypes = new ArrayList<>();
                if (submissionTypesStr != null && !submissionTypesStr.isEmpty()) {
                    String[] types = submissionTypesStr.split(",");
                    for (String t : types) {
                        submissionTypes.add(t.trim());
                    }
                }
                
                AssignmentTemplate template = new AssignmentTemplate(courseTemplateId, weight, type, submissionTypes);
                template.setId(id);
                
                templates.add(template);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return templates;
    }
    
    public void createCourseTemplate(CourseTemplate template) {
        String query = "INSERT INTO course_templates (name) VALUES (?)";
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, template.getName());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creating course template failed, no rows affected.");
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    template.setId(generatedKeys.getInt(1));
                    
                    // Now save assignment templates
                    saveAssignmentTemplates(template);
                } else {
                    throw new SQLException("Creating course template failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void saveAssignmentTemplates(CourseTemplate template) {
        String query = "INSERT INTO assignment_templates (course_template_id, weight, type, submission_types) VALUES (?, ?, ?, ?)";
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            for (AssignmentTemplate at : template.getAssignmentTemplates()) {
                stmt.setInt(1, template.getId());
                stmt.setDouble(2, at.getWeight());
                stmt.setInt(3, at.getType().ordinal());
                
                // Convert submission types list to comma-separated string
                StringBuilder sb = new StringBuilder();
                List<String> types = at.getSubmissionTypes();
                for (int i = 0; i < types.size(); i++) {
                    sb.append(types.get(i));
                    if (i < types.size() - 1) {
                        sb.append(",");
                    }
                }
                stmt.setString(4, sb.toString());
                
                stmt.executeUpdate();
                
                // Get generated key
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        at.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DASHBOARD DATA METHODS
    public List<Course> getCoursesForTeacher(int teacherId) {
        List<Course> courses = new ArrayList<>();
        
        String query = "SELECT c.* FROM courses c " +
                       "JOIN user_courses uc ON c.id = uc.course_id " +
                       "WHERE uc.user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, teacherId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                courses.add(buildFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return courses;
    }
    
    public List<Course> getCoursesForStudent(int studentId) {
        List<Course> courses = new ArrayList<>();
        
        String query = "SELECT c.* FROM courses c " +
                       "JOIN user_courses uc ON c.id = uc.course_id " +
                       "WHERE uc.user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                courses.add(buildFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return courses;
    }
    
    public int getActiveCoursesCount(int teacherId) {
        String query = "SELECT COUNT(*) FROM courses c " +
                       "JOIN user_courses uc ON c.id = uc.course_id " +
                       "WHERE uc.user_id = ? AND c.active = 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, teacherId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
    
    public int getTotalStudentsCount(int teacherId) {
        String query = "SELECT COUNT(DISTINCT u.id) FROM users u " +
                       "JOIN user_courses uc ON u.id = uc.user_id " +
                       "JOIN courses c ON uc.course_id = c.id " +
                       "JOIN user_courses tc ON c.id = tc.course_id " +
                       "WHERE tc.user_id = ? AND u.role = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, teacherId);
            stmt.setInt(2, User.Role.STUDENT.ordinal());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
    
    public int getStudentCountForCourse(int courseId) {
        String query = "SELECT COUNT(*) FROM user_courses uc " +
                       "JOIN users u ON uc.user_id = u.id " +
                       "WHERE uc.course_id = ? AND u.role = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, courseId);
            stmt.setInt(2, User.Role.STUDENT.ordinal());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
    
    public List<User> getStudentsInCourse(int courseId) {
        List<User> students = new ArrayList<>();
        
        String query = "SELECT u.* FROM users u " +
                       "JOIN user_courses uc ON u.id = uc.user_id " +
                       "WHERE uc.course_id = ? AND u.role = ?";
    
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, courseId);
            stmt.setInt(2, User.Role.STUDENT.ordinal());
            ResultSet rs = stmt.executeQuery();
    
            UserDAO userDAO = UserDAO.getInstance();
            while (rs.next()) {
                // Get the user ID and use UserDAO's read method instead
                int userId = rs.getInt("id");
                User user = userDAO.read(userId);
                if (user != null) {
                    students.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return students;
    }
    
    // ASSIGNMENT MANAGEMENT METHODS
    public void addAssignmentToCourse(Course course, Assignment assignment) {
        String query = "INSERT INTO assignments (name, due_date, max_points, assignment_template_id, course_id) " +
                      "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, assignment.getName());
            stmt.setTimestamp(2, assignment.getDueDate());
            stmt.setDouble(3, assignment.getMaxPoints());
            stmt.setInt(4, assignment.getTemplate().getId());
            stmt.setInt(5, course.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creating assignment failed, no rows affected.");
            
            // Get the generated ID
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    assignment.setId(id);
                } else {
                    throw new SQLException("Creating assignment failed, no ID obtained.");
                }
            }
            
            // Add assignment to course's list
            course.addAssignment(assignment);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<Assignment> getAssignmentsForCourse(int courseId) {
        List<Assignment> assignments = new ArrayList<>();
        
        String query = "SELECT a.*, at.type as template_type FROM assignments a " +
                       "JOIN assignment_templates at ON a.assignment_template_id = at.id " +
                       "WHERE a.course_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                Timestamp dueDate = rs.getTimestamp("due_date");
                double maxPoints = rs.getDouble("max_points");
                int templateId = rs.getInt("assignment_template_id");
                int typeOrdinal = rs.getInt("template_type");
                Assignment.Type type = Assignment.Type.values()[typeOrdinal];
                
                // Get the assignment template
                AssignmentTemplateDAO templateDAO = AssignmentTemplateDAO.getInstance();
                AssignmentTemplate template = templateDAO.read(templateId);
                
                // Create appropriate assignment using the factory
                Assignment assignment = AssignmentFactory.create(type, id, name, dueDate, maxPoints, template, courseId);
                assignments.add(assignment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return assignments;
    }
}