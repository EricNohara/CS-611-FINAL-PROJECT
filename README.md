# CS611 Final Project

---

- **Name:** Eric Nohara-LeClair
- **Email**: ernohara@bu.edu
- **BUID:** U90387562

- **Name:** Kelvin Kuang
- **Email:** kkuang@bu.edu
- **BUID:** U264180009

- **Name:** Juling Fan
- **Email:** juling7@bu.edu
- **BUID:** ...

---

# File Structure

```
.
├── data
│   ├── backups
│   ├── courses
│   └── database.db
├── db
│   ├── AssignmentDAO.java
│   ├── AssignmentTemplateDAO.java
│   ├── CourseDAO.java
│   ├── CourseTemplateDAO.java
│   ├── CrudDAO.java
│   ├── DBConnection.java
│   ├── DBSetup.java
│   ├── SubmissionDAO.java
│   ├── UserCourseDAO.java
│   └── UserDAO.java
├── doc
│   ├── design_doc.md
│   ├── schema.dbml
│   ├── schema.pdf
│   ├── uml.png
│   └── uml.puml
├── lib
│   └── sqlite-jdbc-3.49.1.0.jar
├── model
│   ├── Admin.java
│   ├── AdminOperations.java
│   ├── Assignment.java
│   ├── AssignmentTemplate.java
│   ├── Course.java
│   ├── CourseManager.java
│   ├── CourseTemplate.java
│   ├── Grader.java
│   ├── GradingStrategy.java
│   ├── PassFailGradingStrategy.java
│   ├── ProportionalGradingStrategy.java
│   ├── Student.java
│   ├── Submission.java
│   ├── SubmissionGrader.java
│   ├── SubmissionUploader.java
│   ├── Teacher.java
│   ├── User.java
│   └── UserCourse.java
├── out
│   ├── db
│   ├── model
│   ├── ui
│   │   ├── dashboard
│   │   │   ├── components
│   │   │   └── panels
│   │   └── utils
│   └── utils
│       └── model
├── ui
│   ├── dashboard
│   │   ├── components
│   │   │   └── DashboardCard.java
│   │   ├── panels
│   │   │   ├── AssignmentsPanel.java
│   │   │   ├── ChangePasswordPanel.java
│   │   │   ├── CourseManagementPanel.java
│   │   │   ├── CoursesPanel.java
│   │   │   ├── GradingPanel.java
│   │   │   ├── OverviewPanel.java
│   │   │   ├── Refreshable.java
│   │   │   ├── StudentAssignmentsPanel.java
│   │   │   ├── StudentCoursesPanel.java
│   │   │   ├── StudentsPanel.java
│   │   │   ├── SystemSettingsPanel.java
│   │   │   ├── TeacherAssignmentStatsPanel.java
│   │   │   ├── TeacherOverallStatsPanel.java
│   │   │   ├── TemplatesPanel.java
│   │   │   └── UserManagementPanel.java
│   │   ├── AdminDashboard.java
│   │   ├── GraderDashboard.java
│   │   ├── StudentDashboard.java
│   │   └── TeacherDashboard.java
│   ├── utils
│   │   ├── AssignmentTemplateItem.java
│   │   ├── CourseItem.java
│   │   ├── GradingUtils.java
│   │   ├── PaddedCellRenderer.java
│   │   ├── Padding.java
│   │   ├── Refreshable.java
│   │   ├── StudentGradeResult.java
│   │   └── TemplateItem.java
│   ├── LoginFrame.java
│   └── UIConstants.java
├── utils
│   ├── CSVParser.java
│   ├── CSVStudentManager.java
│   ├── DBUtils.java
│   ├── FileExtensionValidator.java
│   ├── FileManager.java
│   ├── Hasher.java
│   └── SubmissionFileManager.java
├── CS611_Final_Project_Presentation.pptx
├── GradingSystemApp.java
├── Main.java
└── README.md
```

---

## Notes from Q&A

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
  - Similarity checker if we have time

---

## How to Compile + Run

Ensure you have cloned the repository and have all required files, including the sqlite-jdbc-3.49.1.0.jar file. Make sure you have the specified directory structure.

### Windows

```
javac -cp ".;lib/sqlite-jdbc-3.49.1.0.jar" -d out *.java db/*.java model/*.java utils/*.java ui/*.java    // compile
java -cp "out;lib/sqlite-jdbc-3.49.1.0.jar" Main                                                          // run
del /s /q out\*.class                                                                                     // clean
```

### Linux/Mac

```
javac -cp ".:lib/sqlite-jdbc-3.49.1.0.jar" -d out *.java db/*.java model/*.java utils/*.java ui/*.java    // compile
java -cp "out:lib/sqlite-jdbc-3.49.1.0.jar" Main                                                          // run
find out -type f -name "*.class" -delete                                                                  // clean
```

---

## Dependencies and Requirements

- Correct file structure
- Java 8
- sqlite-jdbc-3.49.1.0.jar

---

## Known Bugs or Issues

- No known bugs.

---

## Testing Strategy

- **Testing DB Operations:**

  - Used test scripts using the database access objects we created
  - Checked the database manually using the SQLite DB browser
  - Added edge cases in our testing scripts

- **Testing UI:**

  - Ran the main file and made sure everything on the UI was how we wanted it to look
  - Made sure everything worked as intended on the UI

- **Testing UI and DB Integration:**

  - Used queries in the UI to display information about users, courses, assignments, submissions, etc.
  - Checked manually to ensure that these values were the expected values
  - Cross validated the values appearing in our UI with the values in the DB using the SQLite DB browser

- **Testing File Storage:**

  - Created a test script to upload files under a fake submission to make sure that the file paths were generated correctly
  - Stored files locally within the ./data/courses directory, where each submission is located within the ./data/courses/course_name/assignment_name/ directory
  - Once the UI was integrated with the DB operations, used the FileChooser to upload files through the UI dashboards and made sure everything was working properly

- **Testing DB Backups:**

  - Generated a DB state, backed up the DB, reset the DB
  - Made sure that the DB was indeed reset with no information other than the admin
  - Loaded the previous backed up DB
  - Made sure that the loaded state was identical to the initial state of the DB

- **Testing Utils:**
  - Did seperate testing on our utility classes to make sure they functioned correctly in a vaccum
  - e.g. made sure the Hasher function hashed and compared passwords correctly using test scripts

---

## References

- [Java SQL Integration](https://docs.oracle.com/javase/8/docs/api/java/sql/package-summary.html)
- [Try with Resources Statement](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)
- [SQLite Error Codes](https://sqlite.org/rescode.html)
- [SHA-256 Hashing with Salt](https://www.baeldung.com/java-password-hashing)
- [Java Swing](https://docs.oracle.com/javase/tutorial/uiswing/index.html)
- [Java Regex](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html)
- [Java File Storage](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)
- [Schema Generator](https://dbdiagram.io/home)
