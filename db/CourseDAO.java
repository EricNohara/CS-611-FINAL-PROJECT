package db;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import model.Assignment;
import model.Course;
import model.CourseTemplate;
import model.User;

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

            if (course.getCourseTemplateId() == -1) stmt.setNull(1, Types.INTEGER);
            else stmt.setInt(1, course.getCourseTemplateId());

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
                    CourseTemplateDAO courseTemplateDAO = CourseTemplateDAO.getInstance();
                    CourseTemplate template = courseTemplateDAO.read(courseTemplateId);
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

            if (course.getCourseTemplateId() == -1) stmt.setNull(1, Types.INTEGER);
            else stmt.setInt(1, course.getCourseTemplateId());

            stmt.setString(2, course.getName() == null ? "" : course.getName());
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

    @Override
    public Course buildFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int templateId = rs.getInt("course_template_id");
        String name = rs.getString("name");
        boolean active = rs.getBoolean("active");
        
        // Set course template
        CourseTemplateDAO courseTemplateDAO = CourseTemplateDAO.getInstance();
        CourseTemplate template = courseTemplateDAO.read(templateId);
        
        // Load assignments for this course
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        List<Assignment> assignments = assignmentDAO.readAllCondition("course_id", id);
        
        return new Course(id, templateId, name, active, assignments, template);
    }
    
    // DASHBOARD DATA METHODS
    public List<Course> getCoursesForTeacher(int teacherId) {
        List<Course> courses = new ArrayList<>();
        
        String query = "SELECT id, course_template_id, name, active FROM courses c "
               + "JOIN user_courses uc ON c.id = uc.course_id "
               + "WHERE uc.user_id = ?";

               try (Connection con = DBConnection.getConnection();
               PreparedStatement ps = con.prepareStatement(query)) {
      
              ps.setInt(1, teacherId);
              try (ResultSet rs = ps.executeQuery()) {
                  while (rs.next()) {
                      // Grab primitive columns only
                      Course c = new Course();
                      c.setId(rs.getInt("id"));
                      c.setCourseTemplateId(rs.getInt("course_template_id"));
                      c.setName(rs.getString("name"));
                      c.setActive(rs.getBoolean("active"));
                      courses.add(c);
                  }
              }
          } catch (SQLException ex) { ex.printStackTrace(); }
      
          CourseTemplateDAO tplDAO = CourseTemplateDAO.getInstance();
          AssignmentDAO     asnDAO = AssignmentDAO.getInstance();
      
          for (Course c : courses) {
              if (c.getCourseTemplateId() > 0)
                  c.setCourseTemplate(tplDAO.read(c.getCourseTemplateId()));
              c.setAssignments(asnDAO.readAllCondition("course_id", c.getId()));
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
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(buildFromResultSet(rs));  
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getCoursesForStudent: " + e.getMessage());
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
        String query = "SELECT COUNT(u.id) FROM users u " +
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
            stmt.setInt(4, assignment.getId());
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
}