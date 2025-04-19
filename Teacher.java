import java.sql.Timestamp;

public class Teacher extends User {
    public Teacher(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }
    
    public Teacher(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    @Override
    public User.Role getRole() { return User.Role.TEACHER; }

    // Student specific methods...
}
