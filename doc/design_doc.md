# CS-611 Final Project – Design Document

## Design Patterns

### Strategy Pattern

We use the Strategy pattern to encapsulate different grading policies. This allows us to define interchangeable grading behaviors that can be selected at runtime, without changing the objects that use them.

```java
public interface GradingStrategy {
    double calculateGrade(double pointsEarned, double maxPoints);
}

public class ProportionalGradingStrategy implements GradingStrategy {
    public double calculateGrade(double pointsEarned, double maxPoints) {
        return pointsEarned / maxPoints;
    }
}

public class PassFailGradingStrategy implements GradingStrategy {
    private double passThreshold;
    public PassFailGradingStrategy(double passThreshold) {
        this.passThreshold = passThreshold;
    }
    public double calculateGrade(double pointsEarned, double maxPoints) {
        return pointsEarned >= passThreshold * maxPoints ? 1.0 : 0.0;
    }
}
```

### Singleton (Modified) Pattern

We use **Singleton pattern** in `DBConnection.java`. The constructor is private to prevent instantiation, but the `getConnection()` method returns a new connection instance every time it is called.

```java
public class DBConnection {
    private static final String DATABASE_URL = "jdbc:sqlite:./data/database.db";

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DATABASE_URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON;");
        return conn;
    }
}
```

**Design rationale**:
- Prevents accidental instantiation (`private constructor`)
- Keeps connection management simple by avoiding global connection state
- Foreign key enforcement is enabled for every connection


### Factory Pattern

The `CourseManager` interface and its expected implementations reflect a Factory pattern usage, where higher-level logic can rely on abstracted interfaces for creating, editing, and deleting entities like courses, assignments, and templates.

```java
public interface CourseManager {
    void addUserToCourse(User user, Course course);
    void createCourse(Course course);
    void createAssignment(Assignment assignment);
    void createCourseTemplate(CourseTemplate template);
}
```

## Database Schema

The database schema is defined in `schema.dbml` and follows normalized relational design. It is implemented using SQLite and includes support for templates, grading, and submission metadata.

### users

Stores account and role information.

| Column         | Type      | Notes                |
|----------------|-----------|----------------------|
| id             | int       | Primary key          |
| name           | text      |                      |
| email          | text      | Unique email         |
| password_hash  | text      | Hashed password      |
| role           | int       | Enum (student, etc.) |
| created_at     | timestamp | Defaults to now      |
| last_updated   | timestamp | Timestamp on update  |

---

### courses

Represents real courses instantiated from templates.

| Column             | Type | Notes                          |
|--------------------|------|--------------------------------|
| id                 | int  | Primary key                   |
| course_template_id | int  | FK → `course_templates.id`    |
| name               | text | Course title                  |
| active             | bool | Whether course is live/hidden |

---

### course_templates

Reusable course skeletons to replicate structures across semesters.

| Column | Type | Notes        |
|--------|------|--------------|
| id     | int  | Primary key  |
| name   | text | Template name|

---

### assignment_templates

Reusable templates for assignments.

| Column             | Type  | Notes                                        |
|--------------------|-------|----------------------------------------------|
| id                 | int   | Primary key                                  |
| course_template_id | int   | FK → `course_templates.id`                   |
| weight             | real  | Contribution to final grade                 |
| type               | int   | Enum (homework, quiz, text, ...)            |
| submission_types   | text  | Allowed file extensions (e.g. `.pdf,.zip`)  |

---

### assignments

Assignments tied to specific courses.

| Column           | Type      | Notes                                |
|------------------|-----------|--------------------------------------|
| id               | int       | Primary key                          |
| name             | text      | Assignment title                     |
| due_date         | timestamp | Deadline                             |
| max_points       | real      | Full score                           |
| course_id        | int       | FK → `courses.id`                    |
| weight           | real      | Contribution to course grade         |
| type             | int       | Matches type used in templates       |
| submission_types | text      | File formats allowed                 |

---

### user_courses

Many-to-many relationship between users and courses, includes role and enrollment status.

| Column    | Type | Notes                           |
|-----------|------|---------------------------------|
| user_id   | int  | FK → `users.id`                |
| course_id | int  | FK → `courses.id`              |
| status    | int  | Enrollment status              |
| role      | int  | Role in this course            |

Primary key: `(user_id, course_id)`

---

### submissions

Stores submitted files and grading status.

| Column        | Type      | Notes                               |
|---------------|-----------|-------------------------------------|
| id            | int       | Primary key                         |
| assignment_id | int       | FK → `assignments.id`               |
| grader_id     | int       | FK → `users.id`                     |
| filepath      | text      | Path to file                        |
| submitted_at  | timestamp | Default = now                       |
| points_earned | real      | Raw points awarded                  |
| grade         | real      | Normalized or letter grade          |
| status        | int       | Enum: ungraded, graded, late, etc.  |

---

### user_submissions

Supports group submissions: maps multiple users to one submission.

| Column         | Type | Notes                       |
|----------------|------|-----------------------------|
| user_id        | int  | FK → `users.id`             |
| submission_id  | int  | FK → `submissions.id`       |

#### course_templates / assignment_templates

These allow instructors to predefine reusable course or assignment configurations, making it easier to replicate course content across terms.

---

### Schema Design Decisions

- We used **normalized tables** to avoid redundancy and allow efficient querying.
- Many-to-many relationships are modeled with junction tables (e.g., `user_courses`).
- Timestamps and status fields enable deadline tracking and grading lifecycle support.
- Templates are separated from live courses to facilitate clean reuse.

This schema supports extension with minimal changes — e.g., group submissions, rubrics, or peer reviews.

## External Library

We use the SQLite JDBC driver (`sqlite-jdbc-[version].jar`) in the `lib/` folder.

- **Why SQLite?**
    - Zero setup
    - File-based, lightweight, easy to distribute
    - Perfect for desktop apps and academic demos

- **Why not PostgreSQL or MySQL?**
    - Those require server configuration and more operational overhead
    - SQLite is sufficient for the scale and scope of this project

## Conclusion

This project demonstrates a modular design using standard design patterns, cleanly separated responsibilities, and a normalized relational schema. It is lightweight yet extensible, and well-suited for future expansion such as web interfaces or analytics.
