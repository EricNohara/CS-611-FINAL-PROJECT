import java.sql.Timestamp;

public class Student extends User {
    public Student(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }

    @Override
    public User.Role getRole() { return User.Role.STUDENT; }

    // Student specific methods...
}
