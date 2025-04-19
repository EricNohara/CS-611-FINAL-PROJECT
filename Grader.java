import java.sql.Timestamp;

public class Grader extends User {
    public Grader(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }

    @Override
    public User.Role getRole() { return User.Role.GRADER; }

    // Student specific methods...
}
