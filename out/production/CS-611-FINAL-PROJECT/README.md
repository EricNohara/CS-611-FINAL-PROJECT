# CS611 Final Project

- Grading system
- Support for file upload, or just text boxes
- Support for autogravder?
- Users and accounts
  - Student vs Teacher vs Admin
- Have different courses
  - Create, enroll, invite to course
- Create assignments
  - Deadlines
  - Scoring
- Way to view submissions
- Group submissions
  - Attaching more than one user to submission
- Sorting courses/displaying only active courses
- Categorize assignments/submissions
- Assign pages for submissions
- Creating rubric
- Grade statistics
- Interface to grade from rubric
- Publish grades
- Different interfaces for different users

# Notes from Q&A

- Teacher view of grading system

  - Teachers usually teach same course over different semesters
  - Content of class might change, but breakdown of assignments and exams remain the same
    - Every single semester/class has to be built from the beginning
    - Create template for classes?
    - Ability to copy from previous classes?
  - Gradescope does not automatically calculate grades
    - Compute weighted average of assignments
    - Maybe compute grade statistics to figure out curves?
  - Submit any type of file, based on assignment
  - Keep grade statistics
  - Ease of changing assignments/grades
  - Can assign different roles to different users
  - Similarity checker

---

## Compile + Run

#### Windows

```
javac -cp ".;lib/sqlite-jdbc-3.49.1.0.jar" -d out *.java db/*.java model/*.java utils/*.java
java -cp "out;lib/sqlite-jdbc-3.49.1.0.jar" Main
del /s /q out\*.class
```

---

## References

- [Java SQL Integration](https://docs.oracle.com/javase/8/docs/api/java/sql/package-summary.html)
- [Try with Resources Statement](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)
- [SQLite Error Codes](https://sqlite.org/rescode.html)
- [SHA-256 Hashing with Salt](https://www.baeldung.com/java-password-hashing)
