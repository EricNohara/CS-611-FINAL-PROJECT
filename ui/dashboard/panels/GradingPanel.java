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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

// Grading tab
public final class GradingPanel extends JPanel {

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
        // Get student info
        UserDAO userDAO = UserDAO.getInstance();
        List<String> studentNames = new ArrayList<>();

        for (int studentId : submission.getCollaboratorIds()) {
            User student = userDAO.read(studentId);
            if (student != null) {
                studentNames.add(student.getName());
            } else {
                studentNames.add("Unknown Student (ID: " + studentId + ")");
            }
        }

        // Create viewer dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "View Submission",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header panel
        JPanel headerPanel = new JPanel(new GridLayout(4, 2, 10, 5));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Submission Information"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        headerPanel.add(new JLabel("Submission ID:"));
        headerPanel.add(new JLabel(Integer.toString(submission.getId())));

        headerPanel.add(new JLabel("Student(s):"));
        headerPanel.add(new JLabel(String.join(", ", studentNames)));

        headerPanel.add(new JLabel("Submitted Date:"));
        headerPanel.add(new JLabel(submission.getSubmittedAt().toString()));

        headerPanel.add(new JLabel("Status:"));
        JLabel statusLabel = new JLabel(submission.getStatus().toString());

        // Set color based on status
        if (submission.getStatus() == Submission.Status.GRADED) {
            statusLabel.setForeground(Color.GREEN.darker());
        } else if (submission.getStatus() == Submission.Status.LATE) {
            statusLabel.setForeground(Color.ORANGE.darker());
        }

        headerPanel.add(statusLabel);

        panel.add(headerPanel, BorderLayout.NORTH);

        // File details panel
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("File Details"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JPanel fileDetailsPanel = new JPanel(new GridLayout(2, 2, 10, 5));

        fileDetailsPanel.add(new JLabel("File Path:"));
        JTextField filePathField = new JTextField(submission.getFilepath());
        filePathField.setEditable(false);
        fileDetailsPanel.add(filePathField);

        fileDetailsPanel.add(new JLabel("File Type:"));

        // Determine file type from path
        String fileType = "Unknown";
        if (submission.getFilepath() != null && !submission.getFilepath().isEmpty()) {
            int dotIndex = submission.getFilepath().lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < submission.getFilepath().length() - 1) {
                fileType = submission.getFilepath().substring(dotIndex + 1).toUpperCase();
            }
        }

        fileDetailsPanel.add(new JLabel(fileType));

        filePanel.add(fileDetailsPanel, BorderLayout.NORTH);

        // File preview/content area
        JTextArea contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // If the file exists and is a text file, try to load its content
        if (submission.getFilepath() != null && !submission.getFilepath().isEmpty()) {
            File file = new File(submission.getFilepath());
            if (file.exists() && isTextFile(file)) {
                try {
                    // Load file content (first 10KB to avoid memory issues with large files)
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

        JScrollPane contentScrollPane = new JScrollPane(contentArea);
        filePanel.add(contentScrollPane, BorderLayout.CENTER);

        panel.add(filePanel, BorderLayout.CENTER);

        // Grade details panel (if graded)
        if (submission.getStatus() == Submission.Status.GRADED) {
            JPanel gradePanel = new JPanel(new GridLayout(2, 2, 10, 5));
            gradePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Grade Information"),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));

            gradePanel.add(new JLabel("Score:"));
            gradePanel.add(new JLabel(String.format("%.1f/%.1f",
                    submission.getPointsEarned(), assignment.getMaxPoints())));

            gradePanel.add(new JLabel("Percentage:"));
            double percentage = (submission.getPointsEarned() / assignment.getMaxPoints()) * 100;
            gradePanel.add(new JLabel(String.format("%.1f%%", percentage)));

            panel.add(gradePanel, BorderLayout.SOUTH);
        }

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton downloadButton = new JButton("Download File");
        downloadButton.addActionListener(e -> {
            if (submission.getFilepath() == null || submission.getFilepath().isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "No file available for download",
                        "Download Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Show file chooser for download
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Submission File");

            // Suggest filename based on original filename
            File originalFile = new File(submission.getFilepath());
            fileChooser.setSelectedFile(new File(originalFile.getName()));

            if (fileChooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File destFile = fileChooser.getSelectedFile();

                try {
                    // Copy the file
                    Files.copy(originalFile.toPath(), destFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);

                    JOptionPane.showMessageDialog(dialog,
                            "File downloaded successfully to " + destFile.getAbsolutePath(),
                            "Download Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Error downloading file: " + ex.getMessage(),
                            "Download Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton gradeButton = new JButton("Grade Submission");
        gradeButton.addActionListener(e -> {
            dialog.dispose();
            gradeSubmission(submission, assignment);
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(downloadButton);

        if (submission.getStatus() != Submission.Status.GRADED) {
            buttonPanel.add(gradeButton);
        }

        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    // Helper method to grade a submission
    private void gradeSubmission(Submission submission, Assignment assignment) {
        // Get student info
        UserDAO userDAO = UserDAO.getInstance();
        List<String> studentNames = new ArrayList<>();

        for (int studentId : submission.getCollaboratorIds()) {
            User student = userDAO.read(studentId);
            if (student != null) {
                studentNames.add(student.getName());
            } else {
                studentNames.add("Unknown Student (ID: " + studentId + ")");
            }
        }

        // Create grading dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Grade Submission",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header panel
        JPanel headerPanel = new JPanel(new GridLayout(4, 2, 10, 5));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Submission Information"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        headerPanel.add(new JLabel("Assignment:"));
        headerPanel.add(new JLabel(assignment.getName()));

        headerPanel.add(new JLabel("Student(s):"));
        headerPanel.add(new JLabel(String.join(", ", studentNames)));

        headerPanel.add(new JLabel("Submitted Date:"));
        headerPanel.add(new JLabel(submission.getSubmittedAt().toString()));

        headerPanel.add(new JLabel("Due Date:"));
        headerPanel.add(new JLabel(assignment.getDueDate().toString()));

        panel.add(headerPanel, BorderLayout.NORTH);

        // Create main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(450);

        // Left panel - Submission view
        JPanel submissionPanel = new JPanel(new BorderLayout(5, 5));
        submissionPanel.setBorder(BorderFactory.createTitledBorder("Submission"));

        // File details
        JPanel fileDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileDetailsPanel.add(new JLabel("File: "));

        JTextField filePathField = new JTextField(submission.getFilepath(), 30);
        filePathField.setEditable(false);
        fileDetailsPanel.add(filePathField);

        JButton openFileButton = new JButton("Open File");
        openFileButton.addActionListener(e -> {
            if (submission.getFilepath() == null || submission.getFilepath().isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "No file available",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Try to open file with default application
                Desktop.getDesktop().open(new File(submission.getFilepath()));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error opening file: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        fileDetailsPanel.add(openFileButton);

        submissionPanel.add(fileDetailsPanel, BorderLayout.NORTH);

        // File preview/content area
        JTextArea contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // If the file exists and is a text file, try to load its content
        if (submission.getFilepath() != null && !submission.getFilepath().isEmpty()) {
            File file = new File(submission.getFilepath());
            if (file.exists() && isTextFile(file)) {
                try {
                    // Load file content (first 10KB to avoid memory issues with large files)
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

        JScrollPane contentScrollPane = new JScrollPane(contentArea);
        submissionPanel.add(contentScrollPane, BorderLayout.CENTER);

        mainSplitPane.setLeftComponent(submissionPanel);

        // Right panel - Grading
        JPanel gradingPanel = new JPanel(new BorderLayout(5, 5));
        gradingPanel.setBorder(BorderFactory.createTitledBorder("Grading"));

        // Grading rubric
        JPanel rubricPanel = new JPanel(new BorderLayout(5, 5));
        rubricPanel.setBorder(BorderFactory.createTitledBorder("Rubric"));

        // Create a simple rubric table
        String[] rubricColumns = { "Criteria", "Points", "Max Points" };
        DefaultTableModel rubricModel = new DefaultTableModel(rubricColumns, 0);

        // Add some default rubric items based on assignment type
        switch (assignment.getType()) {
            case HOMEWORK:
                rubricModel.addRow(new Object[] { "Correctness", 0, assignment.getMaxPoints() * 0.7 });
                rubricModel.addRow(new Object[] { "Code Style/Documentation", 0, assignment.getMaxPoints() * 0.2 });
                rubricModel.addRow(new Object[] { "Efficiency", 0, assignment.getMaxPoints() * 0.1 });
                break;
            case PROJECT:
                rubricModel.addRow(new Object[] { "Functionality", 0, assignment.getMaxPoints() * 0.4 });
                rubricModel.addRow(new Object[] { "Design/Architecture", 0, assignment.getMaxPoints() * 0.3 });
                rubricModel.addRow(new Object[] { "Code Quality", 0, assignment.getMaxPoints() * 0.2 });
                rubricModel.addRow(new Object[] { "Documentation", 0, assignment.getMaxPoints() * 0.1 });
                break;
            case EXAM:
            case QUIZ:
            default:
                rubricModel.addRow(new Object[] { "Overall Score", 0, assignment.getMaxPoints() });
                break;
        }

        // If already graded, pre-fill with equal distribution
        if (submission.getStatus() == Submission.Status.GRADED) {
            double pointsPerCategory = submission.getPointsEarned() / rubricModel.getRowCount();

            for (int i = 0; i < rubricModel.getRowCount(); i++) {
                double maxForCategory = (double) rubricModel.getValueAt(i, 2);
                double pointsForCategory = Math.min(pointsPerCategory, maxForCategory);
                rubricModel.setValueAt(pointsForCategory, i, 1);
            }
        }

        JTable rubricTable = new JTable(rubricModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only allow editing points column
            }
        };

        // Add a table model listener to update total
        final JLabel totalLabel = new JLabel("Total: 0.0/" + assignment.getMaxPoints());

        rubricModel.addTableModelListener(e -> {
            if (e.getColumn() == 1) { // Points column changed
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
                        // Ignore parsing errors
                    }
                }
                totalLabel.setText(String.format("Total: %.1f/%.1f", total, assignment.getMaxPoints()));
            }
        });

        JScrollPane rubricScrollPane = new JScrollPane(rubricTable);
        rubricPanel.add(rubricScrollPane, BorderLayout.CENTER);

        // Total row
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.add(totalLabel);
        rubricPanel.add(totalPanel, BorderLayout.SOUTH);

        gradingPanel.add(rubricPanel, BorderLayout.NORTH);

        // Feedback area
        JPanel feedbackPanel = new JPanel(new BorderLayout(5, 5));
        feedbackPanel.setBorder(BorderFactory.createTitledBorder("Feedback"));

        JTextArea feedbackArea = new JTextArea(8, 30);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);

        JScrollPane feedbackScrollPane = new JScrollPane(feedbackArea);
        feedbackPanel.add(feedbackScrollPane, BorderLayout.CENTER);

        gradingPanel.add(feedbackPanel, BorderLayout.CENTER);

        mainSplitPane.setRightComponent(gradingPanel);

        panel.add(mainSplitPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        JButton saveButton = new JButton("Save Grade");
        saveButton.addActionListener(e -> {
            try {
                // Calculate total points
                double totalPoints = 0;
                for (int i = 0; i < rubricModel.getRowCount(); i++) {
                    Object value = rubricModel.getValueAt(i, 1);
                    if (value instanceof Number) {
                        totalPoints += ((Number) value).doubleValue();
                    } else if (value instanceof String) {
                        totalPoints += Double.parseDouble((String) value);
                    }
                }

                // Ensure points are not more than max
                if (totalPoints > assignment.getMaxPoints()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Total points cannot exceed max points for assignment",
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // If this is the first time grading, update status
                if (submission.getStatus() != Submission.Status.GRADED) {
                    submission.setStatus(Submission.Status.GRADED);
                }

                // Update submission with new grade
                submission.setGraderId(teacher.getId());

                // Use the teacher's gradeSubmission method
                teacher.gradeSubmission(submission, totalPoints);

                // Show success message
                JOptionPane.showMessageDialog(dialog,
                        "Submission graded successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter valid numbers for points",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error saving grade: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
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
        if (submissionModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "There are no submissions to export with the current filter.",
                    "Nothing to Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Grades");
        fc.setSelectedFile(new File("grades.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File outFile = fc.getSelectedFile();

        SubmissionDAO sDao = SubmissionDAO.getInstance();
        AssignmentDAO aDao = AssignmentDAO.getInstance();
        UserDAO uDao = UserDAO.getInstance();

        try (PrintWriter pw = new PrintWriter(outFile)) {

            pw.println("Submission ID,Student ID,Student Name,Course,Assignment,"
                    + "Submitted,Status,Points,Max Points,Percentage");

            for (int r = 0; r < submissionModel.getRowCount(); r++) {

                int subId = (Integer) submissionModel.getValueAt(r, 0);
                Submission sub = sDao.read(subId);
                if (sub == null)
                    continue;

                Assignment asg = aDao.read(sub.getAssignmentId());
                Course crs = teacherCourses.stream()
                        .filter(c -> c.getId() == asg.getCourseId())
                        .findFirst().orElse(null);

                int stuId = sub.getCollaboratorIds().isEmpty() ? -1
                        : sub.getCollaboratorIds().get(0);
                String stuName = (stuId == -1) ? "Unknown"
                        : uDao.read(stuId).getName();

                String points = (sub.getStatus() == Submission.Status.GRADED)
                        ? String.format("%.1f", sub.getPointsEarned())
                        : "";
                String pct = (sub.getStatus() == Submission.Status.GRADED)
                        ? String.format("%.1f", 100.0 * sub.getPointsEarned() / asg.getMaxPoints())
                        : "";

                // escape commas in free-text fields
                String escStu = "\"" + stuName.replace("\"", "\"\"") + "\"";
                String escCrs = "\"" + crs.getName().replace("\"", "\"\"") + "\"";
                String escAsg = "\"" + asg.getName().replace("\"", "\"\"") + "\"";

                pw.println(String.join(",",
                        String.valueOf(sub.getId()),
                        String.valueOf(stuId),
                        escStu,
                        escCrs,
                        escAsg,
                        sub.getSubmittedAt().toString(),
                        sub.getStatus().toString(),
                        points,
                        String.format("%.1f", asg.getMaxPoints()),
                        pct));
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

    private void publishGrades() {
        // Build a list of assignments the teacher owns
        List<Assignment> allAsg = new ArrayList<>();
        AssignmentDAO aDao = AssignmentDAO.getInstance();
        for (Course c : teacherCourses)
            allAsg.addAll(aDao.readAllCondition("course_id", c.getId()));

        if (allAsg.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "You have no assignments to publish.",
                    "Nothing to Publish", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show selection dialog
        DefaultListModel<Assignment> lm = new DefaultListModel<>();
        allAsg.forEach(lm::addElement);

        JList<Assignment> list = new JList<>(lm);
        list.setCellRenderer((lst, val, idx, s, fs) -> {
            Course crs = teacherCourses.stream()
                    .filter(c -> c.getId() == val.getCourseId())
                    .findFirst().orElse(null);
            return new JLabel(crs.getName() + ": " + val.getName());
        });
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JCheckBox notifyChk = new JCheckBox("Notify students via email", true);

        int opt = JOptionPane.showConfirmDialog(
                this,
                new Object[] { "Select assignment(s) to publish:",
                        new JScrollPane(list),
                        notifyChk },
                "Publish Grades",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (opt != JOptionPane.OK_OPTION || list.getSelectedValuesList().isEmpty())
            return;

        // Update submissions
        SubmissionDAO sDao = SubmissionDAO.getInstance();
        int released = 0;

        for (Assignment asg : list.getSelectedValuesList()) {
            List<Submission> subs = sDao.readAllCondition("assignment_id", asg.getId());

            for (Submission sub : subs) {
                if (sub.getStatus() != Submission.Status.GRADED)
                    continue; // only graded ones

                if (sub.getGrade() < 0) { // not yet published
                    double percent = 100.0 * sub.getPointsEarned() / asg.getMaxPoints();
                    sub.setGrade(percent); // store percentage as “released grade”
                    sDao.update(sub);
                    released++;

                    if (notifyChk.isSelected()) {
                        // placeholder: integrate your email service here
                        // EmailService.notifyStudent(sub.getCollaboratorIds(), asg, percent);
                    }
                }
            }
        }

        JOptionPane.showMessageDialog(this,
                String.format("Grades published for %d submission(s).", released),
                "Publish Complete", JOptionPane.INFORMATION_MESSAGE);

        loadSubmissionsData(); // refresh table
    }
}
