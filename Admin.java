import java.sql.Timestamp;

public class Admin extends User implements AdminOperations {
    public Admin(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }

    public Admin(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    // ABSTRACT METHOD IMPLEMENTATIONS

    @Override
    public User.Role getRole() { return User.Role.ADMIN; }

    @Override
    public void addUser(User user) {}

    @Override
    public void deleteUser(User user) {}

    @Override
    public void editUser(User user) {}

    // Student specific methods...
}
