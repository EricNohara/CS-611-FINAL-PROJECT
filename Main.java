import java.util.List;

public class Main {
    public static void main(String[] args) {
        Admin admin = Admin.getInstance("ADMIN", "admin@gmail.com", "sdfdsfsdfs");
        Student student = new Student("Student 1", "wefsdfsf@gmail.com", "dsfsfs");

        UserDAO userDAO = UserDAO.getInstance();
        userDAO.create(admin);
        admin.addUser(student);

        List<User> users = admin.getAllUsers();
        for (User user : users) System.out.println(user.getName());

        admin.deleteUser(users.get(0));
        users = admin.getAllUsers();
        for (User user : users) System.out.println(user.getName());
    }
}
