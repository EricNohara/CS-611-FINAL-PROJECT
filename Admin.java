import java.sql.Timestamp;
import java.util.List;

public class Admin extends User implements AdminOperations {
    private static Admin instance;

    private Admin(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }

    private Admin(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    // SINGLETON ACCESSORS
    public static Admin getInstance(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        if (instance == null) instance = new Admin(id, name, email, passwordHash, createdAt, lastUpdated);
        return instance;
    }

    public static Admin getInstance(String name, String email, String passwordHash) {
        if (instance == null) instance = new Admin(name, email, passwordHash);
        return instance;
    }

    // Reset (for logout or testing)
    public static void resetInstance() { instance = null; }

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

    // Other specific methods...
}
