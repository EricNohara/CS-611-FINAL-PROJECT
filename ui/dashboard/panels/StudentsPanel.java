package ui.dashboard.panels;

import db.*;
import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

// Students tab
public final class StudentsPanel extends JPanel {

    private final Teacher teacher;
    private final JTabbedPane parentTabs;
    private final List<Course> teacherCourses;

    // UI widgets we reuse in helpers
    private DefaultTableModel studentModel;
    private JTable studentTable;
    private JComboBox<String> courseCombo;

    public StudentsPanel(Teacher teacher, JTabbedPane parentTabs) {
        super(new BorderLayout(10, 10));
        this.teacher = teacher;
        this.parentTabs = parentTabs;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        teacherCourses = CourseDAO.getInstance()
                .getCoursesForTeacher(teacher.getId());

        buildUI();
        loadStudentsData(); // initial fill
    }

    // UI construction
    private void buildUI() {

        // Filter bar
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filter.add(new JLabel("Course:"));

        courseCombo = new JComboBox<>();
        courseCombo.addItem("All Courses");
        teacherCourses.forEach(c -> courseCombo.addItem(c.getName()));
        filter.add(courseCombo);

        JButton applyBtn = new JButton("Apply Filter");
        JButton addBtn = new JButton("Add Student");
        JButton importBtn = new JButton("Import Students");
        filter.add(applyBtn);
        filter.add(Box.createHorizontalStrut(20));
        filter.add(addBtn);
        filter.add(importBtn);

        add(filter, BorderLayout.NORTH);

        // Table
        String[] cols = { "ID", "Name", "Email", "Courses", "Last Login" };
        studentModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        studentTable = new JTable(studentModel);
        add(new JScrollPane(studentTable), BorderLayout.CENTER);

        // Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton viewBtn = new JButton("View Profile");
        JButton removeBtn = new JButton("Remove from Course");
        JButton emailBtn = new JButton("Email Student");
        JButton gradesBtn = new JButton("View Grades");
        actions.add(viewBtn);
        actions.add(removeBtn);
        actions.add(emailBtn);
        actions.add(gradesBtn);
        add(actions, BorderLayout.SOUTH);

        // Listeners
        applyBtn.addActionListener(e -> loadStudentsData());
        addBtn.addActionListener(e -> addStudent());
        importBtn.addActionListener(e -> importStudents());
        viewBtn.addActionListener(e -> viewStudentProfile());
        removeBtn.addActionListener(e -> removeStudentFromCourse());
        emailBtn.addActionListener(e -> emailStudent());
        gradesBtn.addActionListener(e -> viewStudentGrades());
    }

    // Helpers

    // Helper method to load students data
    private void loadStudentsData(DefaultTableModel model, String courseFilter, List<Course> teacherCourses) {
        // Clear existing data
        model.setRowCount(0);

        // Get all students
        UserDAO userDAO = UserDAO.getInstance();
        List<User> allUsers = userDAO.readAll();

        // Filter students only
        List<User> students = allUsers.stream()
                .filter(u -> u.getRole() == User.Role.STUDENT)
                .collect(Collectors.toList());

        // Get user-course relationships
        UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();
        List<UserCourse> allUserCourses = userCourseDAO.readAll();

        // For each student, check if they're in any of the teacher's courses
        for (User student : students) {
            // Find courses for this student
            List<Course> studentCourses = teacherCourses.stream()
                    .filter(course -> allUserCourses.stream()
                            .anyMatch(uc -> uc.getUserId() == student.getId() && uc.getCourseId() == course.getId()))
                    .collect(Collectors.toList());

            // Apply course filter
            if (courseFilter != null && !"All Courses".equals(courseFilter)) {
                boolean inFilteredCourse = studentCourses.stream()
                        .anyMatch(course -> course.getName().equals(courseFilter));

                if (!inFilteredCourse)
                    continue;
            }

            // Skip if not in any of teacher's courses
            if (studentCourses.isEmpty())
                continue;

            // Format courses string
            String coursesStr = studentCourses.stream()
                    .map(Course::getName)
                    .collect(Collectors.joining(", "));

            // Add to table
            Object[] rowData = {
                    student.getId(),
                    student.getName(),
                    student.getEmail(),
                    coursesStr,
                    student.getLastUpdated() // Using last update timestamp as "last login"
            };
            model.addRow(rowData);
        }
    }

    private void loadStudentsData() {
        loadStudentsData(studentModel,
                (String) courseCombo.getSelectedItem(),
                teacherCourses);
    }

    private void addStudent() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Student to Course",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Course selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Course:"), gbc);

        CourseDAO courseDAO = CourseDAO.getInstance();
        List<Course> teacherCourses = courseDAO.getCoursesForTeacher(teacher.getId());

        DefaultComboBoxModel<Course> model = new DefaultComboBoxModel<>();
        if (teacherCourses.isEmpty()) {
            model.addElement(new Course() {
                {
                    setName("-- No active courses --");
                }
            });
        } else {
            teacherCourses.forEach(model::addElement);
        }

        // show course names via renderer
        JComboBox<Course> courseComboBox = new JComboBox<>(model);
        courseComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel(value.getName());
            if (isSelected)
                lbl.setBackground(list.getSelectionBackground());
            return lbl;
        });

        formPanel.add(courseComboBox, gbc);

        // Student selection method
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JRadioButton existingRadio = new JRadioButton("Existing Student");
        JRadioButton newRadio = new JRadioButton("New Student");
        ButtonGroup group = new ButtonGroup();
        group.add(existingRadio);
        group.add(newRadio);
        existingRadio.setSelected(true);
        methodPanel.add(existingRadio);
        methodPanel.add(Box.createHorizontalStrut(10));
        methodPanel.add(newRadio);
        formPanel.add(methodPanel, gbc);

        // Student fields panel (for both existing and new student)
        JPanel existingPanel = new JPanel(new GridBagLayout());
        JPanel newPanel = new JPanel(new GridBagLayout());

        // Existing student panel
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        gbc2.gridx = 0;
        gbc2.gridy = 0;
        existingPanel.add(new JLabel("Email:"), gbc2);

        gbc2.gridx = 1;
        JTextField emailField = new JTextField(20);
        existingPanel.add(emailField, gbc2);

        // New student panel
        GridBagConstraints gbc3 = new GridBagConstraints();
        gbc3.insets = new Insets(5, 5, 5, 5);
        gbc3.fill = GridBagConstraints.HORIZONTAL;

        gbc3.gridx = 0;
        gbc3.gridy = 0;
        newPanel.add(new JLabel("Name:"), gbc3);

        gbc3.gridx = 1;
        JTextField nameField = new JTextField(20);
        newPanel.add(nameField, gbc3);

        gbc3.gridx = 0;
        gbc3.gridy = 1;
        newPanel.add(new JLabel("Email:"), gbc3);

        gbc3.gridx = 1;
        JTextField newEmailField = new JTextField(20);
        newPanel.add(newEmailField, gbc3);

        gbc3.gridx = 0;
        gbc3.gridy = 2;
        newPanel.add(new JLabel("Password:"), gbc3);

        gbc3.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        newPanel.add(passwordField, gbc3);

        // Add student panels to a card layout
        JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.add(existingPanel, "existing");
        cardPanel.add(newPanel, "new");

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(cardPanel, gbc);

        // Set initial state
        CardLayout cl = (CardLayout) cardPanel.getLayout();
        cl.show(cardPanel, "existing");

        // Add radio button listeners to switch between panels
        existingRadio.addActionListener(e -> cl.show(cardPanel, "existing"));
        newRadio.addActionListener(e -> cl.show(cardPanel, "new"));

        panel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton addButton = new JButton("Add Student");
        buttonPanel.add(cancelButton);
        buttonPanel.add(addButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        addButton.addActionListener(e -> {
            String course = (String) courseComboBox.getSelectedItem();

            if (existingRadio.isSelected()) {
                String email = emailField.getText();
                if (email.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Email is required",
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // In a real app, this would look up the student and add them to the course
                JOptionPane.showMessageDialog(dialog,
                        "Student added to course successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } else { // New student
                String name = nameField.getText();
                String email = newEmailField.getText();
                String password = new String(passwordField.getPassword());

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                            "All fields are required",
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // In a real app, this would create a new student and add them to the course
                JOptionPane.showMessageDialog(dialog,
                        "New student created and added to course successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void importStudents() {
        // In a real app, this would open a file chooser to import a CSV of students
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Students");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            // Import students from the selected file
            JOptionPane.showMessageDialog(this,
                    "Students imported successfully from " + fileChooser.getSelectedFile().getName(),
                    "Import Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void viewStudentProfile() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a student to view profile",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get student info
        String studentId = studentTable.getValueAt(selectedRow, 0).toString();
        String studentName = studentTable.getValueAt(selectedRow, 1).toString();
        String studentEmail = studentTable.getValueAt(selectedRow, 2).toString();

        // In a real app, this would open a dialog showing the student profile
        JOptionPane.showMessageDialog(this,
                "Viewing profile for " + studentName + " (ID: " + studentId + ")",
                "Student Profile",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void removeStudentFromCourse() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a student to remove from course",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get student info
        String studentName = studentTable.getValueAt(selectedRow, 1).toString();

        // Show course selection dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Remove Student from Course",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Student name
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Student:"), gbc);

        gbc.gridx = 1;
        JLabel nameLabel = new JLabel(studentName);
        formPanel.add(nameLabel, gbc);

        // Course selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Course:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> courseComboBox = new JComboBox<>();

        // Get courses from student's enrollment (from table data)
        String courses = studentTable.getValueAt(selectedRow, 3).toString();
        for (String course : courses.split(", ")) {
            courseComboBox.addItem(course);
        }

        formPanel.add(courseComboBox, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton removeButton = new JButton("Remove");
        buttonPanel.add(cancelButton);
        buttonPanel.add(removeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        removeButton.addActionListener(e -> {
            String course = (String) courseComboBox.getSelectedItem();

            // Confirm removal
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Are you sure you want to remove " + studentName + " from " + course + "?",
                    "Confirm Removal",
                    JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            // In a real app, this would remove the student from the course in the database
            JOptionPane.showMessageDialog(dialog,
                    "Student removed from course successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void emailStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a student to email",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get student info
        String studentName = studentTable.getValueAt(selectedRow, 1).toString();
        String studentEmail = studentTable.getValueAt(selectedRow, 2).toString();

        // Open email dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Email Student",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // To field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("To:"), gbc);

        gbc.gridx = 1;
        JLabel toLabel = new JLabel(studentName + " <" + studentEmail + ">");
        formPanel.add(toLabel, gbc);

        // Subject field
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Subject:"), gbc);

        gbc.gridx = 1;
        JTextField subjectField = new JTextField(30);
        formPanel.add(subjectField, gbc);

        // Message field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        JTextArea messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        formPanel.add(messageScrollPane, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton sendButton = new JButton("Send");
        buttonPanel.add(cancelButton);
        buttonPanel.add(sendButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        sendButton.addActionListener(e -> {
            String subject = subjectField.getText();
            String message = messageArea.getText();

            if (subject.isEmpty() || message.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Subject and message are required",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // In a real app, this would send an email
            JOptionPane.showMessageDialog(dialog,
                    "Email sent successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void viewStudentGrades() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a student to view grades",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get student info
        String studentName = studentTable.getValueAt(selectedRow, 1).toString();

        // Open grades dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Student Grades - ",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Student info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel nameLabel = new JLabel("Student: " + studentName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoPanel.add(nameLabel);
        panel.add(infoPanel, BorderLayout.NORTH);

        // Create tabbed pane for different courses
        JTabbedPane courseTabs = new JTabbedPane();

        // Add tabs for each course
        addCourseGradeTab(courseTabs, "CS101", studentName);
        addCourseGradeTab(courseTabs, "CS202", studentName);

        panel.add(courseTabs, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        JButton exportButton = new JButton("Export Grades");
        buttonPanel.add(exportButton);
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        closeButton.addActionListener(e -> dialog.dispose());
        exportButton.addActionListener(e -> {
            // In a real app, this would export the grades to a file
            JOptionPane.showMessageDialog(dialog,
                    "Grades exported successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void addCourseGradeTab(JTabbedPane tabbedPane, String courseName, String studentName) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Course info panel
        JPanel courseInfoPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        courseInfoPanel.setBorder(BorderFactory.createTitledBorder("Course Information"));

        courseInfoPanel.add(new JLabel("Current Grade:"));
        courseInfoPanel.add(new JLabel("A- (91.5%)"));

        courseInfoPanel.add(new JLabel("Assignments Completed:"));
        courseInfoPanel.add(new JLabel("8/12"));

        courseInfoPanel.add(new JLabel("Last Submission:"));
        courseInfoPanel.add(new JLabel("2025-04-18 (Homework 3)"));

        panel.add(courseInfoPanel, BorderLayout.NORTH);

        // Grades table
        String[] gradeColumns = { "Assignment", "Type", "Due Date", "Grade", "Comments" };
        DefaultTableModel gradeModel = new DefaultTableModel(gradeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        if ("CS101".equals(courseName)) {
            // Sample data for CS101
            gradeModel.addRow(new Object[] { "Homework 1", "HOMEWORK", "2025-02-10", "95/100", "Excellent work!" });
            gradeModel.addRow(
                    new Object[] { "Quiz 1", "QUIZ", "2025-02-15", "48/50", "Great understanding of concepts." });
            gradeModel.addRow(new Object[] { "Homework 2", "HOMEWORK", "2025-02-24", "89/100",
                    "Good effort, some errors in logic." });
            gradeModel.addRow(new Object[] { "Homework 3", "HOMEWORK", "2025-03-10", "92/100", "Well done." });
            gradeModel
                    .addRow(new Object[] { "Midterm", "EXAM", "2025-03-15", "85/100", "Strong performance overall." });
            gradeModel.addRow(new Object[] { "Homework 4", "HOMEWORK", "2025-03-24", "90/100", "Excellent solution." });
            gradeModel.addRow(new Object[] { "Quiz 2", "QUIZ", "2025-04-01", "45/50", "Very good." });
            gradeModel.addRow(new Object[] { "Homework 5", "HOMEWORK", "2025-04-10", "94/100", "Outstanding work." });
        } else if ("CS202".equals(courseName)) {
            // Sample data for CS202
            gradeModel.addRow(new Object[] { "Homework 1", "HOMEWORK", "2025-02-12", "88/100",
                    "Good work, some improvements needed." });
            gradeModel.addRow(
                    new Object[] { "Project 1", "PROJECT", "2025-03-01", "185/200", "Excellent implementation." });
            gradeModel.addRow(new Object[] { "Homework 2", "HOMEWORK", "2025-03-15", "92/100", "Very good solution." });
            gradeModel.addRow(
                    new Object[] { "Quiz 1", "QUIZ", "2025-03-20", "43/50", "Strong understanding of concepts." });
            gradeModel.addRow(new Object[] { "Homework 3", "HOMEWORK", "2025-04-05", "86/100", "Good effort." });
        }

        JTable gradeTable = new JTable(gradeModel);
        JScrollPane gradeScrollPane = new JScrollPane(gradeTable);
        panel.add(gradeScrollPane, BorderLayout.CENTER);

        // Add to tabbed pane
        tabbedPane.addTab(courseName, panel);
    }
}
