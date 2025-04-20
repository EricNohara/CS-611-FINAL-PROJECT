public class UserDAOTest {
    public static void main(String[] args) {
        DBSetup.createTables();

        // === CREATE ===
        Student testStudent = new Student(
            "Alice3 Test",
            "alice3@example.com",
            "hashedpassword1233"
        );

        UserDAO.createUser(testStudent);
        System.out.println("Created user with ID: " + testStudent.getId());

        // === READ ===
        User fetchedUser = UserDAO.getUserByEmail("alice3@example.com");
        if (fetchedUser != null) {
            System.out.println("\nFetched user:");
            printUser(fetchedUser);
        } else {
            System.out.println("No user found with that email.");
        }

        // === UPDATE ===
        if (fetchedUser != null) {
            fetchedUser.setName("Alice Updated");
            fetchedUser.setPasswordHash("newhashedpassword");
            UserDAO.updateUser(fetchedUser);

            User updatedUser = UserDAO.getUserByEmail("alice3@example.com");
            System.out.println("\nUpdated user:");
            printUser(updatedUser);
        }

        // === DELETE ===
        if (fetchedUser != null) {
            UserDAO.deleteUser(fetchedUser.getId());

            User deletedUser = UserDAO.getUserByEmail("alice3@example.com");
            System.out.println("\nAfter deletion:");
            if (deletedUser == null) {
                System.out.println("User successfully deleted.");
            } else {
                System.out.println("User still exists:");
                printUser(deletedUser);
            }
        }
    }

    private static void printUser(User user) {
        System.out.println("ID: " + user.getId());
        System.out.println("Name: " + user.getName());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Role: " + user.getRole());
        System.out.println("Created At: " + user.getCreatedAt());
        System.out.println("Last Updated: " + user.getLastUpdated());
    }
}
