package model;

import db.UserDAO;

public class UserCourse {
    public enum Status {
        ACTIVE, INACTIVE
    }

    private int userId;
    private int courseId;
    private Status status;
    private User.Role role;

    public UserCourse(int userId, int courseId, Status status, User.Role role) {
        this.userId = userId;
        this.courseId = courseId;
        this.status = status;
        this.role = role;
    }

    public UserCourse(int userId, int courseId) {
        this.userId = userId;
        this.courseId = courseId;
        this.status = Status.ACTIVE;

        UserDAO userDAO = UserDAO.getInstance();
        User user = userDAO.read(userId);
        this.role = user.getRole();
    }

    // GETTERS
    public int getUserId() { return this.userId; }
    public int getCourseId() { return this.courseId; }
    public Status getStatus() { return this.status; }
    public User.Role getRole() { return this.role; }

    // SETTERS
    public void setUserId(int userId) { this.userId = userId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public void setStatus(Status status) { this.status = status; }
    public void setRole(User.Role role) { this.role = role; }
}
