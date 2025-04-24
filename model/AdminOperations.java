package model;
import java.util.List;

public interface AdminOperations {
    // system wide user handling
    User getUser(int id);
    List<User> getAllUsers();
    void addUser(User user);
    void deleteUser(User user);
    void editUser(User user);

    // whatever other operations an admin needs to perform
}
