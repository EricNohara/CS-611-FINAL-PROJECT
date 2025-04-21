import java.util.List;

public class Main {
    public static void main(String[] args) {
        Admin admin = Admin.getInstance("ADMIN", "admin@gmail.com", "sdfdsfsdfs");
        UserDAO.addUser(admin);

        List<User> users = admin.getAllUsers();
        for (User user : users) System.out.println(user.getName());

        admin.deleteUser(users.get(0));
        for (User user : users) System.out.println(user.getName());
    }
}
