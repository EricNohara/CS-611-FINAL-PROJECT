package ui.dashboard.panels;

import db.*;
import model.*;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

// Grading tab
public final class GradingPanel extends JPanel implements Refreshable {

    private final Teacher teacher;
    private final JTabbedPane parentTabs;
    private final List<Course> teacherCourses;

    /* UI widgets referenced by helpers */
    private DefaultTableModel submissionModel;
    private JTable submissionTable;
    private JComboBox<String> courseCombo;
    private JComboBox<String> assignmentCombo;
    private JComboBox<String> statusCombo;

    public GradingPanel(Teacher teacher, JTabbedPane parentTabs) {
        super(new BorderLayout(10, 10));
        this.teacher = teacher;
        this.parentTabs = parentTabs;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        teacherCourses = CourseDAO.getInstance()
                .getCoursesForTeacher(teacher.getId());

        buildUI();
        loadSubmissionsData(); // first fill
    }

    /* =============== UI construction =============== */
    private void buildUI() {

        // -------- filter bar --------
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filter.add(new JLabel("Course:"));
        courseCombo = new JComboBox<>();
        courseCombo.addItem("All Courses");
        teacherCourses.forEach(c -> courseCombo.addItem(c.getName()));
        filter.add(courseCombo);

        filter.add(new JLabel("Assignment:"));
        assignmentCombo = new JComboBox<>(new String[] { "All Assignments" });
        filter.add(assignmentCombo);

        // dynamic assignment dropdown
        courseCombo.addActionListener(e -> updateAssignmentDropdown());

        filter.add(new JLabel("Status:"));
        statusCombo = new JComboBox<>(new String[] {
                "All",
                Submission.Status.GRADED.toString(),
                Submission.Status.UNGRADED.toString(),
                Submission.Status.LATE.toString() });
        filter.add(statusCombo);

        JButton applyBtn = new JButton("Apply Filter");
        filter.add(applyBtn);

        add(filter, BorderLayout.NORTH);

        // -------- submissions table --------
        String[] cols = { "ID", "Student", "Course", "Assignment",
                "Submitted", "Status", "Grade" };
        submissionModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        submissionTable = new JTable(submissionModel);
        add(new JScrollPane(submissionTable), BorderLayout.CENTER);

        // -------- action buttons --------
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton viewBtn = new JButton("View Submission");
        JButton gradeBtn = new JButton("Grade");
        JButton publishBtn = new JButton("Publish Grades");
        JButton exportBtn = new JButton("Export Grades");
        actions.add(viewBtn);
        actions.add(gradeBtn);
        actions.add(publishBtn);
        actions.add(exportBtn);
        add(actions, BorderLayout.SOUTH);

        /* ---- listeners ---- */
        applyBtn.addActionListener(e -> loadSubmissionsData());
        viewBtn.addActionListener(e -> viewSubmission());
        gradeBtn.addActionListener(e -> gradeSubmission());
        publishBtn.addActionListener(e -> publishGrades());
        exportBtn.addActionListener(e -> exportGrades());
    }

    // Helpers

    // Helper method to update assignment dropdown based on course selection
    private void updateAssignmentDropdown() {
        assignmentCombo.removeAllItems();
        assignmentCombo.addItem("All Assignments");

        String selectedCourse = (String) courseCombo.getSelectedItem();

        if (!"All Courses".equals(selectedCourse)) {
            // Find the selected course
            Course course = teacherCourses.stream()
                    .filter(c -> c.getName().equals(selectedCourse))
                    .findFirst()
                    .orElse(null);

            if (course != null) {
                // Get assignments for this course
                AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
                List<Assignment> assignments = assignmentDAO.readAllCondition("course_id", course.getId());

                if (assignments != null) {
                    for (Assignment assignment : assignments) {
                        assignmentCombo.addItem(assignment.getName());
                    }
                }
            }
        }
    }

    // Helper method to load submissions data
    private void loadSubmissionsData(DefaultTableModel model, String courseFilter, String assignmentFilter,
            String statusFilter, List<Course> teacherCourses) {
        // Clear existing data
        model.setRowCount(0);

        // Get submissions from database
        SubmissionDAO submissionDAO = SubmissionDAO.getInstance();
        List<Submission> allSubmissions = submissionDAO.readAll();

        // Get assignments
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();

        // Get users
        UserDAO userDAO = UserDAO.getInstance();

        // Filter and add submissions to table
        for (Submission submission : allSubmissions) {
            // Get assignment
            Assignment assignment = assignmentDAO.read(submission.getAssignmentId());
            if (assignment == null)
                continue;

            // Check if assignment belongs to one of teacher's courses
            boolean isTeachersCourse = teacherCourses.stream()
                    .anyMatch(c -> c.getId() == assignment.getCourseId());

            if (!isTeachersCourse)
                continue;

            // Find course
            Course course = teacherCourses.stream()
                    .filter(c -> c.getId() == assignment.getCourseId())
                    .findFirst()
                    .orElse(null);

            if (course == null)
                continue;

            // Apply course filter
            if (courseFilter != null && !"All Courses".equals(courseFilter) &&
                    !course.getName().equals(courseFilter)) {
                continue;
            }

            // Apply assignment filter
            if (assignmentFilter != null && !"All Assignments".equals(assignmentFilter) &&
                    !assignment.getName().equals(assignmentFilter)) {
                continue;
            }

            // Apply status filter
            if (statusFilter != null && !"All".equals(statusFilter) &&
                    !submission.getStatus().toString().equals(statusFilter)) {
                continue;
            }

            // Get student info
            if (submission.getCollaboratorIds().isEmpty())
                continue;

            int firstStudentId = submission.getCollaboratorIds().get(0);
            User student = userDAO.read(firstStudentId);

            if (student == null)
                continue;

            // Format grade
            String gradeDisplay = "-";
            if (submission.getStatus() == Submission.Status.GRADED) {
                gradeDisplay = String.format("%.0f/%.0f",
                        submission.getPointsEarned(), assignment.getMaxPoints());
            }

            Object[] rowData = {
                    submission.getId(),
                    student.getName(),
                    course.getName(),
                    assignment.getName(),
                    submission.getSubmittedAt(),
                    submission.getStatus().toString(),
                    gradeDisplay
            };
            model.addRow(rowData);
        }
    }

    private void viewSubmission(Submission submission, Assignment assignment) {
        GradingUtils.showSubmissionDialog(this, submission, assignment);
    }

    // Helper method to grade a submission
    private void gradeSubmission(Submission submission, Assignment assignment) {
        GradingUtils.showGradingDialog(this, teacher, submission, assignment, this::loadSubmissionsData);
    }


    private void openSubmissionFile(Submission submission, Component parent) {
        if (submission.getFilepath() == null || submission.getFilepath().isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No file available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Desktop.getDesktop().open(new File(submission.getFilepath()));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Error opening file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSubmissionContent(Submission submission, JTextArea contentArea) {
        if (submission.getFilepath() != null && !submission.getFilepath().isEmpty()) {
            File file = new File(submission.getFilepath());
            if (file.exists() && isTextFile(file)) {
                try {
                    contentArea.setText(loadFileContent(file, 10 * 1024));
                } catch (IOException e) {
                    contentArea.setText("Error loading file content: " + e.getMessage());
                }
            } else {
                contentArea.setText("Binary file - preview not available");
            }
        } else {
            contentArea.setText("No file available");
        }
    }

    private JPanel buildGradingPanel(Assignment assignment, Submission submission) {
        JPanel gradingPanel = new JPanel(new BorderLayout(5, 5));
        gradingPanel.setBorder(BorderFactory.createTitledBorder("Grading"));

        JPanel rubricPanel = new JPanel(new BorderLayout(5, 5));
        rubricPanel.setBorder(BorderFactory.createTitledBorder("Rubric"));

        String[] rubricColumns = {"Criteria", "Points", "Max Points"};
        DefaultTableModel rubricModel = new DefaultTableModel(rubricColumns, 0);
        fillRubricModel(rubricModel, assignment);

        JTable rubricTable = new JTable(rubricModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };

        JLabel totalLabel = new JLabel("Total: 0.0/" + assignment.getMaxPoints());
        rubricModel.addTableModelListener(e -> updateTotalLabel(rubricModel, totalLabel, assignment));

        rubricPanel.add(new JScrollPane(rubricTable), BorderLayout.CENTER);

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.add(totalLabel);
        rubricPanel.add(totalPanel, BorderLayout.SOUTH);

        gradingPanel.add(rubricPanel, BorderLayout.NORTH);

        JPanel feedbackPanel = new JPanel(new BorderLayout(5, 5));
        feedbackPanel.setBorder(BorderFactory.createTitledBorder("Feedback"));
        feedbackPanel.add(new JScrollPane(new JTextArea(8, 30)), BorderLayout.CENTER);

        gradingPanel.add(feedbackPanel, BorderLayout.CENTER);

        return gradingPanel;
    }

    private void fillRubricModel(DefaultTableModel rubricModel, Assignment assignment) {
        switch (assignment.getType()) {
            case HOMEWORK:
                rubricModel.addRow(new Object[]{"Correctness", 0, assignment.getMaxPoints() * 0.7});
                rubricModel.addRow(new Object[]{"Code Style/Documentation", 0, assignment.getMaxPoints() * 0.2});
                rubricModel.addRow(new Object[]{"Efficiency", 0, assignment.getMaxPoints() * 0.1});
                break;
            case PROJECT:
                rubricModel.addRow(new Object[]{"Functionality", 0, assignment.getMaxPoints() * 0.4});
                rubricModel.addRow(new Object[]{"Design/Architecture", 0, assignment.getMaxPoints() * 0.3});
                rubricModel.addRow(new Object[]{"Code Quality", 0, assignment.getMaxPoints() * 0.2});
                rubricModel.addRow(new Object[]{"Documentation", 0, assignment.getMaxPoints() * 0.1});
                break;
            case EXAM:
            case QUIZ:
            default:
                rubricModel.addRow(new Object[]{"Overall Score", 0, assignment.getMaxPoints()});
                break;
        }
    }

    private void updateTotalLabel(DefaultTableModel rubricModel, JLabel totalLabel, Assignment assignment) {
        double total = 0;
        for (int i = 0; i < rubricModel.getRowCount(); i++) {
            try {
                Object value = rubricModel.getValueAt(i, 1);
                if (value instanceof Number) {
                    total += ((Number) value).doubleValue();
                } else if (value instanceof String) {
                    total += Double.parseDouble((String) value);
                }
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
        totalLabel.setText(String.format("Total: %.1f/%.1f", total, assignment.getMaxPoints()));
    }

    private JPanel buildButtonPanel(Submission submission, Assignment assignment, JPanel gradingPanel, JDialog dialog) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton saveButton = new JButton("Save Grade");
        saveButton.addActionListener(e -> saveGrade(submission, assignment, gradingPanel, dialog));

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private void saveGrade(Submission submission, Assignment assignment, JPanel gradingPanel, JDialog dialog) {
        try {
            JScrollPane rubricScrollPane = (JScrollPane) ((JPanel) gradingPanel.getComponent(0)).getComponent(0);
            JTable rubricTable = (JTable) rubricScrollPane.getViewport().getView();
            DefaultTableModel rubricModel = (DefaultTableModel) rubricTable.getModel();

            double totalPoints = 0;
            for (int i = 0; i < rubricModel.getRowCount(); i++) {
                Object value = rubricModel.getValueAt(i, 1);
                if (value instanceof Number) {
                    totalPoints += ((Number) value).doubleValue();
                    System.out.println("Total Points added: " + totalPoints);
                } else if (value instanceof String) {
                    //totalPoints += Double.parseDouble((String) value);
                    System.out.println("value is not Number");
                }
            }
            System.out.println("line 655"+totalPoints);
            if (totalPoints > assignment.getMaxPoints()) {
                JOptionPane.showMessageDialog(dialog, "Total points cannot exceed max points for assignment", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (submission.getStatus() != Submission.Status.GRADED) {
                System.out.println(submission.getStatus());
                submission.setStatus(Submission.Status.GRADED);
            }

            submission.setGraderId(teacher.getId());
            teacher.gradeSubmission(submission, totalPoints);

            JOptionPane.showMessageDialog(dialog, "Submission graded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, "Error saving grade: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper method to check if a file is a text file
    private boolean isTextFile(File file) {
        // Check file extension first
        String name = file.getName().toLowerCase();
        if (name.endsWith(".txt") || name.endsWith(".java") || name.endsWith(".py") ||
                name.endsWith(".c") || name.endsWith(".cpp") || name.endsWith(".h") ||
                name.endsWith(".cs") || name.endsWith(".js") || name.endsWith(".html") ||
                name.endsWith(".css") || name.endsWith(".xml") || name.endsWith(".json") ||
                name.endsWith(".md") || name.endsWith(".csv")) {
            return true;
        }

        // If extension check doesn't help, try to read the first few bytes
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[1024]; // Read first 1KB
            int bytesRead = bis.read(buffer);

            if (bytesRead == -1) {
                return false; // Empty file
            }

            // Check for binary content (non-text characters)
            for (int i = 0; i < bytesRead; i++) {
                byte b = buffer[i];
                // If we find a non-text character (outside of normal ASCII and common control
                // chars)
                if (b < 0x08 && b != 0x09 && b != 0x0A && b != 0x0D) {
                    return false;
                }
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Helper method to load file content with a size limit
    private String loadFileContent(File file, int maxBytes) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            char[] buffer = new char[1024];
            int bytesRead = 0;
            int charsRead;

            while ((charsRead = reader.read(buffer)) != -1 && bytesRead < maxBytes) {
                content.append(buffer, 0, charsRead);
                bytesRead += charsRead * 2; // Approximate bytes (char is 2 bytes)
            }

            if (bytesRead >= maxBytes) {
                content.append("\n\n... (file too large to display completely) ...");
            }
        }

        return content.toString();
    }

    // Helper method to export grades to CSV
    private void exportSubmissionGrades(File file, List<Submission> submissions, Assignment assignment) {
        try (PrintWriter writer = new PrintWriter(file)) {
            // Write header
            writer.println("Submission ID,Student ID,Student Name,Submitted Date,Status,Points,Max Points,Percentage");

            // Get user DAO for student information
            UserDAO userDAO = UserDAO.getInstance();

            // Write submission data
            for (Submission submission : submissions) {
                // Get primary student (first collaborator)
                int studentId = -1;
                String studentName = "Unknown";

                if (!submission.getCollaboratorIds().isEmpty()) {
                    studentId = submission.getCollaboratorIds().get(0);
                    User student = userDAO.read(studentId);
                    if (student != null) {
                        studentName = student.getName();
                    }
                }

                // Format for CSV (escape commas in strings)
                String submissionId = Integer.toString(submission.getId());
                String studentIdStr = Integer.toString(studentId);
                studentName = "\"" + studentName.replace("\"", "\"\"") + "\"";
                String submittedDate = submission.getSubmittedAt().toString();
                String status = submission.getStatus().toString();

                String points = "-";
                String percentage = "-";

                if (submission.getStatus() == Submission.Status.GRADED) {
                    points = String.format("%.1f", submission.getPointsEarned());
                    percentage = String.format("%.1f",
                            (submission.getPointsEarned() / assignment.getMaxPoints()) * 100);
                }

                String maxPoints = String.format("%.1f", assignment.getMaxPoints());

                // Write the CSV line
                writer.println(String.join(",", submissionId, studentIdStr, studentName,
                        submittedDate, status, points, maxPoints, percentage));
            }

            JOptionPane.showMessageDialog(null,
                    "Grades exported successfully to " + file.getName(),
                    "Export Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Error exporting grades: " + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Reload the table using the current filter-combo selections.
    private void loadSubmissionsData() {
        loadSubmissionsData(
                submissionModel,
                (String) courseCombo.getSelectedItem(),
                (String) assignmentCombo.getSelectedItem(),
                (String) statusCombo.getSelectedItem(),
                teacherCourses);
    }

    // View the submission that is currently selected in the table.
    private void viewSubmission() {
        int row = submissionTable.getSelectedRow();
        if (row == -1)
            return; // nothing selected
        int modelRow = submissionTable.convertRowIndexToModel(row);
        int submissionId = (int) submissionModel.getValueAt(modelRow, 0);

        SubmissionDAO sDao = SubmissionDAO.getInstance();
        Submission sub = sDao.read(submissionId);
        Assignment asg = AssignmentDAO.getInstance().read(sub.getAssignmentId());

        viewSubmission(sub, asg); // call existing helper
    }

    // Grade the submission that is currently selected in the table.
    private void gradeSubmission() {
        int row = submissionTable.getSelectedRow();
        if (row == -1)
            return;
        int modelRow = submissionTable.convertRowIndexToModel(row);
        int submissionId = (int) submissionModel.getValueAt(modelRow, 0);

        SubmissionDAO sDao = SubmissionDAO.getInstance();
        Submission sub = sDao.read(submissionId);
        Assignment asg = AssignmentDAO.getInstance().read(sub.getAssignmentId());

        gradeSubmission(sub, asg); // call existing helper
    }

    private void exportGrades() {
        // In a real app, this would export grades to a file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Grades");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            // Export grades to the selected file
            JOptionPane.showMessageDialog(this,
                    "Grades exported successfully to " + fileChooser.getSelectedFile().getName(),
                    "Export Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void publishGrades() {
        // In a real app, this would publish grades for selected assignments
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Publish Grade",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Assignment selection
        JPanel selectionPanel = new JPanel(new BorderLayout(5, 5));
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Select Assignments"));

        DefaultListModel<String> assignmentListModel = new DefaultListModel<>();
        assignmentListModel.addElement("CS101: Homework 1");
        assignmentListModel.addElement("CS101: Quiz 1");
        assignmentListModel.addElement("CS202: Project 1");
        assignmentListModel.addElement("CS303: Midterm Exam");

        JList<String> assignmentList = new JList<>(assignmentListModel);
        assignmentList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane listScrollPane = new JScrollPane(assignmentList);
        selectionPanel.add(listScrollPane, BorderLayout.CENTER);

        panel.add(selectionPanel, BorderLayout.CENTER);

        // Options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox notifyCheckBox = new JCheckBox("Notify students via email");
        notifyCheckBox.setSelected(true);
        optionsPanel.add(notifyCheckBox);
        panel.add(optionsPanel, BorderLayout.SOUTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton publishButton = new JButton("Publish");
        buttonPanel.add(cancelButton);
        buttonPanel.add(publishButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        publishButton.addActionListener(e -> {
            // Get selected assignments
            List<String> selectedAssignments = assignmentList.getSelectedValuesList();
            if (selectedAssignments.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please select at least one assignment",
                        "No Selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // In a real app, this would publish the grades in the database
            JOptionPane.showMessageDialog(dialog,
                    "Grades published successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    @Override
    public void refresh() {
        loadSubmissionsData();
    }
}
