import java.sql.Timestamp;

public class Admin extends User {
    public Admin(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }

    public Admin(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    @Override
    public User.Role getRole() { return User.Role.ADMIN; }

    // Student specific methods...
}
