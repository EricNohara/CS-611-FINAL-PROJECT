import java.sql.Timestamp;

public class Grader extends User {
    public Grader(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }

    public Grader(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    @Override
    public User.Role getRole() { return User.Role.GRADER; }

    // Student specific methods...
}
