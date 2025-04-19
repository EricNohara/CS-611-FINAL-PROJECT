public class UserDAOTest {
    public static void main(String[] args) {
        DBSetup.createTables();

        // Create a Student user
        Student testStudent = new Student(
            "Alice3 Test",
            "alice3@example.com",
            "hashedpassword1233"
        );

        // Insert into DB
        UserDAO.createUser(testStudent);
        System.out.println("Created user with ID: " + testStudent.getId());

        // Fetch from DB by email
        User fetchedUser = UserDAO.getUserByEmail("alice3@example.com");
        if (fetchedUser != null) {
            System.out.println("Fetched user:");
            System.out.println("ID: " + fetchedUser.getId());
            System.out.println("Name: " + fetchedUser.getName());
            System.out.println("Email: " + fetchedUser.getEmail());
            System.out.println("Role: " + fetchedUser.getRole());
            System.out.println("Created At: " + fetchedUser.getCreatedAt());
            System.out.println("Last Updated: " + fetchedUser.getLastUpdated());
        } else {
            System.out.println("No user found with that email.");
        }
    }
}
