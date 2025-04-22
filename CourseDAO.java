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

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                courses.add(buildFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        String query = "INSERT INTO assignments (id, name, due_date, max_points, assignment_template_id, course_id, assignment_type) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, assignment.getId());
            stmt.setString(2, assignment.getName());
            stmt.setTimestamp(3, assignment.getDueDate());
            stmt.setDouble(4, assignment.getMaxPoints());
            
            // For now, we'll use a placeholder for the template ID
            // In a real implementation, you'd need to get the actual template ID 
            // or create a method in AbstractAssignment to access it
            stmt.setInt(5, 1); 
            
            stmt.setInt(6, course.getId());
            
            // Get assignment type based on class
            Assignment.Type type;
            if (assignment instanceof Homework) {
                type = Assignment.Type.HOMEWORK;
            } else if (assignment instanceof Quiz) {
                type = Assignment.Type.QUIZ;
            } else if (assignment instanceof Exam) {
                type = Assignment.Type.EXAM;
            } else if (assignment instanceof Project) {
                type = Assignment.Type.PROJECT;
            } else {
                throw new IllegalArgumentException("Unknown assignment type");
            }
            stmt.setInt(7, type.ordinal());

            stmt.executeUpdate();
            
            // Add assignment to course's list
            course.addAssignment(assignment);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<Assignment> getAssignmentsForCourse(int courseId) {
        List<Assignment> assignments = new ArrayList<>();
        
        String query = "SELECT a.*, at.weight, at.type as template_type FROM assignments a " +
                       "JOIN assignment_templates at ON a.assignment_template_id = at.id " +
                       "WHERE a.course_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                assignments.add(buildAssignmentFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return assignments;
    }
    
    // Helper method to build an assignment from a result set
    private Assignment buildAssignmentFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String title = rs.getString("name");
        Timestamp dueDate = rs.getTimestamp("due_date");
        double maxPoints = rs.getDouble("max_points");
        int templateId = rs.getInt("assignment_template_id");
        int assignmentTypeOrdinal = rs.getInt("assignment_type");
        
        // Get assignment type
        Assignment.Type type = Assignment.Type.values()[assignmentTypeOrdinal];
        
        // Create assignment template
        double weight = rs.getDouble("weight");
        Assignment.Type templateType = Assignment.Type.values()[rs.getInt("template_type")];
        
        // For simplicity, we'll create a minimal template
        List<String> submissionTypes = new ArrayList<>(); // In a real app, would fetch from DB
        AssignmentTemplate template = new AssignmentTemplate(rs.getInt("course_id"), weight, templateType, submissionTypes);
        template.setId(templateId);
        
        // Create appropriate assignment based on type
        return AssignmentFactory.create(type, id, title, dueDate, maxPoints, template);
    }
}