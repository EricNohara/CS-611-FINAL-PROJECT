import java.sql.Timestamp;
import java.util.List;

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
    public User getUser(int id) { return UserDAO.getUserById(id); }

    @Override
    public List<User> getAllUsers() { return UserDAO.getAllUsers(); }

    @Override
    public void addUser(User user) { UserDAO.addUser(user); }

    @Override
    public void deleteUser(User user) { UserDAO.deleteUser(user.getId()); }

    @Override
    public void editUser(User user) { UserDAO.editUser(user); }

    // Student specific methods...
}
