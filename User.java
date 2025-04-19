import java.sql.Timestamp;

public abstract class User {
    public enum Role {
        STUDENT, GRADER, TEACHER, ADMIN
    }

    private final int id;
    private final Timestamp createdAt;

    private String name;
    private String email;
    private String passwordHash;
    private Timestamp lastUpdated;

    public User(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
    }

    // GETTERS
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getLastUpdated() { return lastUpdated; }

    // SETTERS
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }

    // ABSTRACT
    public abstract Role getRole();
}