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
                return buildCourseFromResultSet(rs);
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
                courses.add(buildCourseFromResultSet(rs));
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
                courses.add(buildCourseFromResultSet(rs));
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
                courses.add(buildCourseFromResultSet(rs));
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

            while (rs.next()) {
                // Use UserDAO to build User objects
                students.add(UserDAO.buildUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return students;
    }
    
    // Helper method to build a course from a result set
    private Course buildCourseFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int templateId = rs.getInt("course_template_id");
        String name = rs.getString("name");
        boolean active = rs.getBoolean("active");
        
        Course course = new Course();
        course.setId(id);
        course.setName(name);
        course.setCourseTemplateId(templateId);
        course.setActive(active);
        
        return course;
    }
}