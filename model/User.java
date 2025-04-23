import java.sql.Timestamp;

public abstract class User {
    public enum Role {
        STUDENT, GRADER, TEACHER, ADMIN
    }

    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private Timestamp createdAt;
    private Timestamp lastUpdated;

    public User(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
    }

    public User(String name, String email, String passwordHash) {
        this.id = -1;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = null;
        this.lastUpdated = null;
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
    public void setId(int id) { this.id = id; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // METHODS
    public boolean login(String password) {
        return Hasher.checkPassword(password, passwordHash);
    }

    // ABSTRACT
    public abstract Role getRole();
}