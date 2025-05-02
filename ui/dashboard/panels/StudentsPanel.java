package ui.dashboard.panels;

import db.*;
import model.*;
import utils.CSVStudentManager;
import utils.Hasher;
import ui.utils.StudentGradeResult;
import ui.UIConstants;
import ui.utils.PaddedCellRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static ui.utils.StudentGradeResult.getLetterGrade;
import static ui.utils.StudentGradeResult.getStudentGradePercent;

// Students tab
public final class StudentsPanel extends JPanel implements Refreshable {

    private final Teacher teacher;
    private final JTabbedPane parentTabs;
    private final List<Course> teacherCourses;

    // UI widgets we reuse in helpers
    private DefaultTableModel studentModel;
    private JTable studentTable;
    private JComboBox<String> courseCombo;
    private JComboBox<String> statusCombo;

    public StudentsPanel(Teacher teacher, JTabbedPane parentTabs) {
        super(new BorderLayout(10, 10));
        this.teacher = teacher;
        this.parentTabs = parentTabs;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        teacherCourses = CourseDAO.getInstance()
                .getCoursesForTeacher(teacher.getId());

        buildUI();
        loadStudentGraderData(); // initial fill
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

        filter.add(Box.createHorizontalStrut(10));
        filter.add(new JLabel("Status:"));

        statusCombo = new JComboBox<>();
        statusCombo.addItem("All");
        statusCombo.addItem("Active");
        statusCombo.addItem("Inactive");
        filter.add(statusCombo);

        JButton applyBtn = new JButton("Apply Filter");
        JButton addStudentBtn = new JButton("Add Student");
        JButton importBtn = new JButton("Import Students");
        JButton addGraderBtn = new JButton("Add Grader");

        filter.add(applyBtn);
        filter.add(Box.createHorizontalStrut(20));
        filter.add(addStudentBtn);
        filter.add(importBtn);
        filter.add(addGraderBtn);

        add(filter, BorderLayout.NORTH);

        // Table
        String[] cols = { "ID", "Name", "Email", "Course", "CourseId", "Active", "Last Login", "Role", "Grade" };
        studentModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        studentTable = new JTable(studentModel);
        add(new JScrollPane(studentTable), BorderLayout.CENTER);

        TableCellRenderer paddedRenderer = new PaddedCellRenderer(UIConstants.TABLE_CELL_PAD);
        PaddedCellRenderer.setDefaultRowHeight(studentTable);
        for (int i = 0; i < studentTable.getColumnCount(); i++) {
            studentTable.getColumnModel().getColumn(i).setCellRenderer(paddedRenderer);
        }

        // Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton viewBtn = new JButton("View Profile");
        JButton removeBtn = new JButton("Remove from Course");
        JButton toggleStatusBtn = new JButton("Toggle Active/Inactive");
        JButton emailBtn = new JButton("Email Student");
        JButton gradesBtn = new JButton("View Grades");
        JButton exportBtn = new JButton("Export Grades");

        actions.add(viewBtn);
        actions.add(removeBtn);
        actions.add(toggleStatusBtn);
        actions.add(emailBtn);
        actions.add(gradesBtn);
        actions.add(exportBtn);
        add(actions, BorderLayout.SOUTH);

        // Listeners
        applyBtn.addActionListener(e -> loadStudentGraderData());
        addStudentBtn.addActionListener(e -> addStudent());
        importBtn.addActionListener(e -> importStudents());
        addGraderBtn.addActionListener(e -> addGrader());
        viewBtn.addActionListener(e -> viewStudentProfile());
        removeBtn.addActionListener(e -> removeStudentFromCourse());
        toggleStatusBtn.addActionListener(e -> toggleStudentStatus());
        emailBtn.addActionListener(e -> emailStudent());
        gradesBtn.addActionListener(e -> viewStudentGrades());
        exportBtn.addActionListener(e -> exportGrades());
    }

    // Helper method to load students data
    private void loadStudentGraderData(DefaultTableModel model, String courseFilter, String statusFilter,
            List<Course> teacherCourses) {
        // Clear existing data
        model.setRowCount(0);

        // Get all students
        UserDAO userDAO = UserDAO.getInstance();
        List<User> allUsers = userDAO.readAll();

        // Filter students only
        List<User> users = allUsers.stream()
                .filter(u -> u.getRole() == User.Role.STUDENT || u.getRole() == User.Role.GRADER)
                .collect(Collectors.toList());

        // Get user-course relationships
        UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();
        List<UserCourse> allUserCourses = userCourseDAO.readAll();

        // For each student, check if they're in any of the teacher's courses
        for (User user : users) {
            for (Course course : teacherCourses) {
                boolean enrolled = allUserCourses.stream()
                        .anyMatch(uc -> uc.getUserId() == user.getId() && uc.getCourseId() == course.getId());

                if (!enrolled)
                    continue;

                if (courseFilter != null && !"All Courses".equals(courseFilter) &&
                        !course.getName().equals(courseFilter))
                    continue;

                UserCourse uc = userCourseDAO.read(user.getId(), course.getId());
                if (uc == null)
                    continue;

                if (!"All".equals(statusFilter)) {
                    boolean active = uc.getStatus() == UserCourse.Status.ACTIVE;
                    if ("Active".equals(statusFilter) && !active)
                        continue;
                    if ("Inactive".equals(statusFilter) && active)
                        continue;
                }
                double grade = getStudentGradePercent(user.getId(), course.getId());

                String status = uc.getStatus() == UserCourse.Status.ACTIVE ? "Active" : "Inactive";

                Object[] rowData = {
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        course.getName(),
                        course.getId(),
                        status,
                        user.getLastUpdated(),
                        user.getRole(),
                        grade
                };
                model.addRow(rowData);
            }
        }
    }

    private void loadStudentGraderData() {
        loadStudentGraderData(studentModel,
                (String) courseCombo.getSelectedItem(),
                (String) statusCombo.getSelectedItem(),
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
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Course:"), gbc);

        CourseDAO courseDAO = CourseDAO.getInstance();
        List<Course> teacherCourses = courseDAO.getCoursesForTeacher(teacher.getId());

        DefaultComboBoxModel<Course> model = new DefaultComboBoxModel<>();
        if (teacherCourses.isEmpty()) {
            model.addElement(new Course() {
                {
                    setId(-1);
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
            if (isSelected) {
                lbl.setOpaque(true);
                lbl.setBackground(list.getSelectionBackground());
            }
            return lbl;
        });

        gbc.gridx = 1;
        gbc.gridy = 0;
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
            Course course = (Course) courseComboBox.getSelectedItem();
            if (course == null || course.getId() <= 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Please choose a valid course.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            UserDAO uDao = UserDAO.getInstance();
            UserCourseDAO ucDao = UserCourseDAO.getInstance();

            try {
                if (existingRadio.isSelected()) {
                    String email = emailField.getText();
                    if (email.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog,
                                "Email is required",
                                "Validation Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    User student = uDao.readByEmail(email); // <- implement in DAO or find manually
                    if (student == null || student.getRole() != User.Role.STUDENT) {
                        JOptionPane.showMessageDialog(dialog,
                                "No student account found with that email.",
                                "Not Found", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    /* check if already enrolled */
                    UserCourse existing = ucDao.read(student.getId(), course.getId());
                    if (existing != null) {
                        JOptionPane.showMessageDialog(dialog,
                                "This student is already enrolled in the course.",
                                "Duplicate", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    UserCourse uc = new UserCourse(student.getId(), course.getId(),
                            UserCourse.Status.ACTIVE, User.Role.STUDENT);
                    ucDao.create(uc);

                    JOptionPane.showMessageDialog(dialog,
                            "Student added to course successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();

                } else {
                    String email = newEmailField.getText().trim();
                    String name = nameField.getText().trim();
                    String rawPassword = new String(passwordField.getPassword());
                    String hashedPassword = Hasher.hashPassword(rawPassword);
                    if (email.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog,
                                "Email is required",
                                "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (name.isEmpty() || email.isEmpty() || rawPassword.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "All fields are required", "Validation Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    User existing = uDao.readByEmail(email);
                    if (existing != null) {
                        JOptionPane.showMessageDialog(dialog,
                                "An account with that email already exists.\nUse 'Existing Student' instead.",
                                "Duplicate Email", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    Student newStudent = new Student(name, email, hashedPassword);
                    uDao.create(newStudent);

                    ucDao.create(new UserCourse(newStudent.getId(), course.getId(), UserCourse.Status.ACTIVE,
                            User.Role.STUDENT));

                    JOptionPane.showMessageDialog(dialog, "New student created and added to course successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                }

                /* refresh main table */
                refresh();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Database error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void addGrader() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Grader to Course",
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
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Course:"), gbc);

        CourseDAO courseDAO = CourseDAO.getInstance();
        List<Course> teacherCourses = courseDAO.getCoursesForTeacher(teacher.getId());

        DefaultComboBoxModel<Course> model = new DefaultComboBoxModel<>();
        if (teacherCourses.isEmpty()) {
            model.addElement(new Course() {
                {
                    setId(-1);
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
            if (isSelected) {
                lbl.setOpaque(true);
                lbl.setBackground(list.getSelectionBackground());
            }
            return lbl;
        });

        gbc.gridx = 1;
        gbc.gridy = 0;
        formPanel.add(courseComboBox, gbc);

        // Grader selection method
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JRadioButton existingRadio = new JRadioButton("Existing Grader");
        JRadioButton newRadio = new JRadioButton("New Grader");
        ButtonGroup group = new ButtonGroup();
        group.add(existingRadio);
        group.add(newRadio);
        existingRadio.setSelected(true);
        methodPanel.add(existingRadio);
        methodPanel.add(Box.createHorizontalStrut(10));
        methodPanel.add(newRadio);
        formPanel.add(methodPanel, gbc);

        // Grader fields panel (for both existing and new Grader)
        JPanel existingPanel = new JPanel(new GridBagLayout());
        JPanel newPanel = new JPanel(new GridBagLayout());

        // Existing Grader panel
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        gbc2.gridx = 0;
        gbc2.gridy = 0;
        existingPanel.add(new JLabel("Email:"), gbc2);

        gbc2.gridx = 1;
        JTextField emailField = new JTextField(20);
        existingPanel.add(emailField, gbc2);

        // New Grader panel
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
        JButton addButton = new JButton("Add Grader");
        buttonPanel.add(cancelButton);
        buttonPanel.add(addButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        addButton.addActionListener(e -> {
            Course course = (Course) courseComboBox.getSelectedItem();
            if (course == null || course.getId() <= 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Please choose a valid course.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            UserDAO uDao = UserDAO.getInstance();
            UserCourseDAO ucDao = UserCourseDAO.getInstance();

            try {
                if (existingRadio.isSelected()) {
                    String email = emailField.getText();
                    if (email.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog,
                                "Email is required",
                                "Validation Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    User grader = uDao.readByEmail(email); // <- implement in DAO or find manually
                    if (grader == null || grader.getRole() != User.Role.GRADER) {
                        JOptionPane.showMessageDialog(dialog,
                                "No grader account found with that email.",
                                "Not Found", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    /* check if already enrolled */
                    UserCourse existing = ucDao.read(grader.getId(), course.getId());
                    if (existing != null) {
                        JOptionPane.showMessageDialog(dialog,
                                "This grader is already enrolled in the course.",
                                "Duplicate", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    UserCourse uc = new UserCourse(grader.getId(), course.getId(),
                            UserCourse.Status.ACTIVE, User.Role.GRADER);
                    ucDao.create(uc);

                    JOptionPane.showMessageDialog(dialog,
                            "grader added to course successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();

                } else {
                    String email = newEmailField.getText().trim();
                    String name = nameField.getText().trim();
                    String password = new String(passwordField.getPassword());
                    if (email.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog,
                                "Email is required",
                                "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "All fields are required", "Validation Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    User existing = uDao.readByEmail(email);
                    if (existing != null) {
                        JOptionPane.showMessageDialog(dialog,
                                "An account with that email already exists.\nUse 'Existing Grader' instead.",
                                "Duplicate Email", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    Grader newGrader = new Grader(name, email, password);
                    uDao.create(newGrader);

                    ucDao.create(new UserCourse(newGrader.getId(), course.getId(), UserCourse.Status.ACTIVE,
                            User.Role.GRADER));

                    JOptionPane.showMessageDialog(dialog, "New grader created and added to course successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                }

                /* refresh main table */
                refresh();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Database error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // Import students from csv
    private void importStudents() {

        // Choose csv file
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Students");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
        fc.setFileFilter(filter);
        fc.setAcceptAllFileFilterUsed(false);

        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File csvFile = fc.getSelectedFile();

        // choose target course
        List<Course> courses = CourseDAO.getInstance()
                .getCoursesForTeacher(teacher.getId());
        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "You have no active courses.",
                    "Import Cancelled", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Course target = (Course) JOptionPane.showInputDialog(
                this, "Enroll these students in which course?",
                "Choose Course", JOptionPane.QUESTION_MESSAGE,
                null, courses.toArray(), courses.get(0));

        if (target == null) return; // cancelled

        CSVStudentManager manager = new CSVStudentManager();
        manager.handleStudentCSVSubmission(csvFile, target.getId());

        JOptionPane.showMessageDialog(this,
                String.format("Import finished from %s\n"
                        + "Students enrolled    : %d\n"
                        + "Already enrolled     : %d\n"
                        + "Created and enrolled      : %d\n"
                        + "Students removed     : %d\n"
                        + "Bad / skipped lines  : %d",
                        csvFile.getName(), 
                        manager.getNumEnrolled(), 
                        manager.getNumAlreadyEnrolled(), 
                        manager.getNumCreatedAndEnrolled(), 
                        manager.getNumRemoved(),
                        manager.getNumSkippedLines()),
                "Import Summary", JOptionPane.INFORMATION_MESSAGE);

        refresh();
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

    private void toggleStudentStatus() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a student to toggle status",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int userId = (Integer) studentTable.getValueAt(selectedRow, 0);
        int courseId = Integer.parseInt(studentTable.getValueAt(selectedRow, 4).toString());

        UserCourseDAO ucDao = UserCourseDAO.getInstance();
        UserCourse uc = ucDao.read(userId, courseId);
        if (uc == null) {
            JOptionPane.showMessageDialog(this,
                    "Enrollment not found for selected user.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Toggle status
        UserCourse.Status newStatus = (uc.getStatus() == UserCourse.Status.ACTIVE)
                ? UserCourse.Status.INACTIVE
                : UserCourse.Status.ACTIVE;

        uc.setStatus(newStatus);
        ucDao.update(uc); // make sure this method exists in DAO

        JOptionPane.showMessageDialog(this,
                "User status updated to: " + newStatus,
                "Success", JOptionPane.INFORMATION_MESSAGE);

        loadStudentGraderData();
    }

    private void removeStudentFromCourse() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user to remove from course",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get student info
        String userName = studentTable.getValueAt(selectedRow, 1).toString();

        // Show course selection dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Remove User from Course",
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
        formPanel.add(new JLabel("User:"), gbc);

        gbc.gridx = 1;
        JLabel nameLabel = new JLabel(userName);
        formPanel.add(nameLabel, gbc);

        // Course selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Course:"), gbc);

        gbc.gridx = 1;
        JComboBox<String> courseComboBox = new JComboBox<>();

        // Get courses from student's enrollment (from table data)
        String courses = studentTable.getValueAt(selectedRow, 4).toString();
        for (String course : courses.split(" ")) {
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
        String selectedCourse = (String) courseComboBox.getSelectedItem();

        if (selectedCourse == null) {
            JOptionPane.showMessageDialog(dialog,
                    "Please choose a valid course.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId = (Integer) studentTable.getValueAt(selectedRow, 0);
        System.out.println("Selected course raw: [" + selectedCourse + "]");

        int courseId = Integer.parseInt((String) selectedCourse);

        removeButton.addActionListener(e -> {
            // Confirm removal
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Are you sure you want to remove " + userName + " from " + selectedCourse + "?",
                    "Confirm Removal",
                    JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            UserCourseDAO enrollmentDAO = UserCourseDAO.getInstance();
            enrollmentDAO.delete(userId, courseId);
            JOptionPane.showMessageDialog(dialog,
                    "Student removed from course successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });

        refresh();

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

        int viewRow = studentTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a student to view grades",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // basic student info
        int studentId = (Integer) studentTable.getValueAt(viewRow, 0);
        String studentName = studentTable.getValueAt(viewRow, 1).toString();

        // Button dialog shell
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Student Grades – " + studentName,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel hdr = new JLabel("Student: " + studentName);
        hdr.setFont(hdr.getFont().deriveFont(Font.BOLD, 14f));
        root.add(hdr, BorderLayout.NORTH);

        // one tab per course that (a) belongs to the teacher and (b) the student is
        // enrolled in
        JTabbedPane courseTabs = new JTabbedPane();

        int courseId = Integer.parseInt(studentTable.getValueAt(viewRow, 4).toString());

        Course course = teacherCourses.stream()
                .filter(c -> c.getId() == courseId)
                .findFirst()
                .orElse(null);

        if (course != null) {
            addCourseGradeTab(courseTabs, course, studentId);
        } else {
            courseTabs.addTab("No Grades", new JLabel("This student is not in your course."));
        }

        root.add(courseTabs, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportBtn = new JButton("Export Grades");
        JButton closeBtn = new JButton("Close");
        btns.add(exportBtn);
        btns.add(closeBtn);
        root.add(btns, BorderLayout.SOUTH);

        closeBtn.addActionListener(e -> dialog.dispose());
        exportBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog,
                    "Grades exported successfully!",
                    "Export", JOptionPane.INFORMATION_MESSAGE);
        });

        dialog.add(root);
        dialog.setVisible(true);
    }

    private void addCourseGradeTab(JTabbedPane tabs,
            Course course,
            int studentId) {

        AssignmentDAO aDao = AssignmentDAO.getInstance();
        SubmissionDAO sDao = SubmissionDAO.getInstance();

        List<Assignment> assignments = aDao.readAllCondition("course_id", course.getId());

        // Get grade info
        double earnedSum = 0, maxSum = 0;
        int completed = 0;

        String[] cols = { "Assignment", "Type", "Due Date",
                "Grade", "Comments" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        Submission lastSub = null;

        for (Assignment a : assignments) {

            // pull the submission(s) for THIS student / assignment
            List<Submission> subs = sDao.readAllCondition("assignment_id", a.getId())
                    .stream()
                    .filter(s -> s.getCollaboratorIds().contains(studentId))
                    .collect(Collectors.toList());

            Submission sub = subs.stream()
                    .max(Comparator.comparing(Submission::getSubmittedAt))
                    .orElse(null);

            String gradeStr = "-";
            String comment = "";
            if (sub != null) {
                lastSub = (lastSub == null ||
                        sub.getSubmittedAt().after(lastSub.getSubmittedAt()))
                                ? sub
                                : lastSub;

                if (sub.getStatus() == Submission.Status.GRADED) {
                    gradeStr = String.format("%.0f/%.0f",
                            sub.getPointsEarned(),
                            a.getMaxPoints());
                    earnedSum += sub.getPointsEarned();
                    maxSum += a.getMaxPoints();
                    completed++;
                }
            }

            model.addRow(new Object[] {
                    a.getName(),
                    a.getType(),
                    a.getDueDate().toLocalDateTime().toLocalDate(),
                    gradeStr
            });
        }

        // Top summary panel
        double percent = (maxSum > 0) ? 100.0 * earnedSum / maxSum : 0.0;
        String letter = getLetterGrade(percent);

        JPanel summary = new JPanel(new GridLayout(3, 2, 10, 5));
        summary.setBorder(BorderFactory.createTitledBorder("Course Information"));

        summary.add(new JLabel("Current Grade:"));
        summary.add(new JLabel(String.format("%s (%.1f%%)", letter, percent)));

        summary.add(new JLabel("Assignments Completed:"));
        summary.add(new JLabel(completed + " / " + assignments.size()));

        summary.add(new JLabel("Last Submission:"));
        summary.add(new JLabel(lastSub == null ? "—"
                : lastSub.getSubmittedAt().toLocalDateTime().toLocalDate()
                        + " (" + aDao.read(lastSub.getAssignmentId()).getName() + ")"));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(summary, BorderLayout.NORTH);
        root.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

        tabs.addTab(course.getName(), root);
    }
    private void exportGrades() {
        if (studentModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "There are no grades to export with the current filter.",
                    "Nothing to Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Grades");
        fc.setSelectedFile(new File("grades.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File outFile = fc.getSelectedFile();

        UserDAO uDao = UserDAO.getInstance();

        try (PrintWriter pw = new PrintWriter(outFile)) {

            pw.println("Student ID,Student Name,Student Email,Course,CourseId,"
                    + "Grade");

            for (int r = 0; r < studentModel.getRowCount(); r++) {
                String studentId = studentModel.getValueAt(r, 0).toString();
                String studentName = studentModel.getValueAt(r, 1).toString();
                String studentEmail = studentModel.getValueAt(r, 2).toString();
                String courseName = studentModel.getValueAt(r, 3).toString();
                String courseId = studentModel.getValueAt(r, 4).toString();
                Object gradeObj = studentModel.getValueAt(r, 7);
                String gradeFormatted;

                try {
                    double gradeVal = Double.parseDouble(String.valueOf(gradeObj));
                    gradeFormatted = String.format("%.2f", gradeVal);
                } catch (Exception e) {
                    gradeFormatted = ""; // 空字符串表示无效分数
                }


                pw.println(String.join(",",
                        studentId,
                        studentName,
                        studentEmail,
                        courseName,
                        courseId,
                        gradeFormatted
                        )
                );
            }

            JOptionPane.showMessageDialog(this,
                    "Grades exported successfully to " + outFile.getName(),
                    "Export Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error exporting grades: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
  
    @Override
    public void refresh() {
        loadStudentGraderData();
    }
}
