package ui.dashboard.panels;

import db.*;
import model.*;
import ui.*;
import ui.utils.AssignmentTemplateItem;
import ui.utils.CourseItem;
import utils.SubmissionFileManager;

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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// Assignments Tab
public final class AssignmentsPanel extends JPanel {

    private final Teacher teacher;
    private final JTabbedPane parentTabs;
    private final List<Course> teacherCourses;

    // Widgets
    private DefaultTableModel assignmentModel;
    private JTable assignmentTable;
    private JComboBox<String> courseCombo;
    private JComboBox<String> typeCombo;

    public AssignmentsPanel(Teacher teacher,
            JTabbedPane parentTabs) {
        super(new BorderLayout(10, 10));
        this.teacher = teacher;
        this.parentTabs = parentTabs;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        /* preload courses once */
        teacherCourses = CourseDAO.getInstance()
                .getCoursesForTeacher(teacher.getId());

        buildUI();
        loadAssignments(null, null); // fill table first time
    }

    // UI construciont
    private void buildUI() {

        // Filter bar
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filter.add(new JLabel("Course:"));
        courseCombo = new JComboBox<>();
        courseCombo.addItem("All Courses");
        teacherCourses.forEach(c -> courseCombo.addItem(c.getName()));
        filter.add(courseCombo);

        filter.add(new JLabel("Type:"));
        typeCombo = new JComboBox<>(new String[] {
                "All Types",
                Assignment.Type.HOMEWORK.toString(),
                Assignment.Type.QUIZ.toString(),
                Assignment.Type.EXAM.toString(),
                Assignment.Type.PROJECT.toString() });
        filter.add(typeCombo);

        JButton applyBtn = new JButton("Apply Filter");
        JButton newBtn = new JButton("New Assignment");
        filter.add(applyBtn);
        filter.add(Box.createHorizontalStrut(20));
        filter.add(newBtn);

        add(filter, BorderLayout.NORTH);

        // Table in center
        String[] cols = { "ID", "Title", "Course", "Type",
                "Due Date", "Max Points", "Status" };
        assignmentModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        assignmentTable = new JTable(assignmentModel);
        add(new JScrollPane(assignmentTable), BorderLayout.CENTER);

        // Buttons at bottom
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editBtn = new JButton("Edit");
        JButton delBtn = new JButton("Delete");
        JButton subsBtn = new JButton("View Submissions");
        actions.add(editBtn);
        actions.add(delBtn);
        actions.add(subsBtn);
        add(actions, BorderLayout.SOUTH);

        // Listeners
        applyBtn.addActionListener(e -> loadAssignments(
                (String) courseCombo.getSelectedItem(),
                (String) typeCombo.getSelectedItem()));

        newBtn.addActionListener(e -> createNewAssignment());
        editBtn.addActionListener(e -> editSelectedAssignment());
        delBtn.addActionListener(e -> deleteSelectedAssignment());
        subsBtn.addActionListener(e -> viewSubmissionsForAssignment());
    }

    // Data loaders
    private void loadAssignments(String courseFilter, String typeFilter) {
        assignmentModel.setRowCount(0);

        AssignmentDAO adao = AssignmentDAO.getInstance();
        List<Assignment> all = new ArrayList<>();
        for (Course c : teacherCourses)
            all.addAll(adao.readAllCondition("course_id", c.getId()));

        for (Assignment a : all) {

            Course course = teacherCourses.stream()
                    .filter(c -> c.getId() == a.getCourseId()).findFirst().orElse(null);

            if (courseFilter != null && !"All Courses".equals(courseFilter)
                    && !course.getName().equals(courseFilter))
                continue;

            if (typeFilter != null && !"All Types".equals(typeFilter)
                    && !a.getType().toString().equals(typeFilter))
                continue;

            boolean open = a.getDueDate().after(new Timestamp(System.currentTimeMillis()));
            assignmentModel.addRow(new Object[] {
                    a.getId(), a.getName(), course.getName(),
                    a.getType(), a.getDueDate(),
                    a.getMaxPoints(),
                    open ? "Open" : "Closed" });
        }
    }

    // Helpers
    private void createNewAssignment() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Create New Assignment",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Title:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField titleField = new JTextField(30);
        formPanel.add(titleField, gbc);

        // Course
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Course:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;

        // Get courses for dropdown
        CourseDAO courseDAO = CourseDAO.getInstance();
        List<Course> teacherCourses = courseDAO.getCoursesForTeacher(teacher.getId());

        DefaultComboBoxModel<CourseItem> courseModel = new DefaultComboBoxModel<>();
        for (Course course : teacherCourses) {
            courseModel.addElement(new CourseItem(course.getId(), course.getName()));
        }

        JComboBox<CourseItem> courseComboBox = new JComboBox<>(courseModel);
        formPanel.add(courseComboBox, gbc);

        // Type
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Type:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        String[] assignmentTypes = { "HOMEWORK", "QUIZ", "EXAM", "PROJECT" };
        JComboBox<String> typeComboBox = new JComboBox<>(assignmentTypes);
        formPanel.add(typeComboBox, gbc);

        // Assignment Template section (optional)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        JCheckBox useTemplateCheckBox = new JCheckBox("Use Assignment Template");
        formPanel.add(useTemplateCheckBox, gbc);

        // Assignment Template dropdown (initially hidden)
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        JLabel templateLabel = new JLabel("Template:");
        templateLabel.setVisible(false);
        formPanel.add(templateLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JComboBox<AssignmentTemplateItem> templateComboBox = new JComboBox<>();
        templateComboBox.setVisible(false);
        formPanel.add(templateComboBox, gbc);

        // Due date
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Due Date:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;

        // Use a JSpinner with a date model for date selection
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 7); // Default due date is one week from now
        Date initialDate = calendar.getTime();
        calendar.add(Calendar.YEAR, 1); // Max date is one year from now
        Date maxDate = calendar.getTime();
        calendar.add(Calendar.YEAR, -2); // Min date is one year ago
        Date minDate = calendar.getTime();

        SpinnerDateModel dateModel = new SpinnerDateModel(initialDate, minDate, maxDate, Calendar.DAY_OF_MONTH);
        JSpinner dueDateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dueDateSpinner, "yyyy-MM-dd HH:mm");
        dueDateSpinner.setEditor(dateEditor);
        formPanel.add(dueDateSpinner, gbc);

        // Max points
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Max Points:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField pointsField = new JTextField("100", 5);
        formPanel.add(pointsField, gbc);

        // Weight
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Weight (%):"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField weightField = new JTextField("10", 5);
        formPanel.add(weightField, gbc);

        // Submission types
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Submission Types:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField submissionTypesField = new JTextField("pdf, docx, java", 20);
        formPanel.add(submissionTypesField, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        JTextArea descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        formPanel.add(descScrollPane, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton createButton = new JButton("Create Assignment");
        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add listeners for course and template selection
        courseComboBox.addActionListener(e1 -> {
            if (useTemplateCheckBox.isSelected()) {
                updateTemplateDropdown(templateComboBox);
            }
        });

        useTemplateCheckBox.addActionListener(e2 -> {
            boolean useTemplate = useTemplateCheckBox.isSelected();
            templateLabel.setVisible(useTemplate);
            templateComboBox.setVisible(useTemplate);

            if (useTemplate) {
                updateTemplateDropdown(templateComboBox);
            }
        });

        templateComboBox.addActionListener(e3 -> {
            if (templateComboBox.getSelectedItem() != null) {
                AssignmentTemplateItem selectedTemplate = (AssignmentTemplateItem) templateComboBox.getSelectedItem();

                // Update fields based on selected template
                if (selectedTemplate != null) {
                    typeComboBox.setSelectedItem(selectedTemplate.getType().toString());
                    weightField.setText(String.format("%.1f", selectedTemplate.getWeight() * 100));
                    submissionTypesField.setText(String.join(", ", selectedTemplate.getSubmissionTypes()));
                }
            }
        });

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        createButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Title is required",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (courseComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(dialog,
                        "Please select a course",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Get selected course
                CourseItem selectedCourse = (CourseItem) courseComboBox.getSelectedItem();
                int courseId = selectedCourse.getId();

                // Get selected assignment type
                String typeStr = (String) typeComboBox.getSelectedItem();
                Assignment.Type type = Assignment.Type.valueOf(typeStr);

                // Get due date
                Date dueDate = (Date) dueDateSpinner.getValue();
                Timestamp dueDateTimestamp = new Timestamp(dueDate.getTime());

                // Get max points
                double maxPoints = Double.parseDouble(pointsField.getText());
                if (maxPoints <= 0) {
                    throw new NumberFormatException("Max points must be positive");
                }

                // Get weight
                double weight = Double.parseDouble(weightField.getText()) / 100.0; // Convert to decimal
                if (weight <= 0) {
                    throw new NumberFormatException("Weight must be positive");
                }

                // Get submission types
                String submissionTypesText = submissionTypesField.getText().trim();
                List<String> submissionTypes = new ArrayList<>();
                if (!submissionTypesText.isEmpty()) {
                    String[] types = submissionTypesText.split(",");
                    for (String t : types) {
                        submissionTypes.add(t.trim());
                    }
                }

                // Get description (this would be stored in a field in Assignment if available)
                String description = descriptionArea.getText().trim();

                // Create assignment object - using the correct constructor signature
                // Assignment(int id, String name, Timestamp dueDate, double maxPoints, int
                // courseId, double weight, Type type, List<String> submissionTypes)
                // Note: We use -1 for ID since it will be assigned by the database
                Assignment assignment = new Assignment(
                        -1, title, dueDateTimestamp, maxPoints, courseId, weight, type, submissionTypes);

                // Save to database
                AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
                assignmentDAO.create(assignment);

                // Show success message
                JOptionPane.showMessageDialog(dialog,
                        "Assignment created successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                // Close dialog
                dialog.dispose();

                // Refresh assignments panel
                // For now, switch to another tab and back
                int currentIndex = parentTabs.getSelectedIndex();
                parentTabs.setSelectedIndex(currentIndex == 0 ? 1 : 0);
                parentTabs.setSelectedIndex(3); // Switch to Assignments tab

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter valid numbers for max points and weight: " + ex.getMessage(),
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error creating assignment: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void editSelectedAssignment() {
        int selectedRow = assignmentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an assignment to edit",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert row index from view to model in case of sorting
        int modelRow = assignmentTable.convertRowIndexToModel(selectedRow);

        // Get assignment ID from the table
        int assignmentId = (int) assignmentTable.getModel().getValueAt(modelRow, 0);

        // Get assignment from database
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        Assignment assignment = assignmentDAO.read(assignmentId);

        if (assignment == null) {
            JOptionPane.showMessageDialog(this,
                    "Assignment not found in database",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create edit dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Assignment",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Assignment ID (read-only)
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Assignment ID:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JLabel idLabel = new JLabel(String.valueOf(assignment.getId()));
        formPanel.add(idLabel, gbc);

        // Title
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Title:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField titleField = new JTextField(assignment.getName(), 30);
        formPanel.add(titleField, gbc);

        // Course (read-only)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Course:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;

        // Get course name
        CourseDAO courseDAO = CourseDAO.getInstance();
        Course course = courseDAO.read(assignment.getCourseId());
        String courseName = course != null ? course.getName() : "Unknown";

        JLabel courseLabel = new JLabel(courseName);
        formPanel.add(courseLabel, gbc);

        // Type
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Type:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        String[] assignmentTypes = { "HOMEWORK", "QUIZ", "EXAM", "PROJECT" };
        JComboBox<String> typeComboBox = new JComboBox<>(assignmentTypes);
        typeComboBox.setSelectedItem(assignment.getType().toString());
        formPanel.add(typeComboBox, gbc);

        // Due date
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Due Date:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;

        // Use a JSpinner with a date model for date selection
        Date dueDate = new Date(assignment.getDueDate().getTime());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1); // Max date is one year from now
        Date maxDate = calendar.getTime();
        calendar.add(Calendar.YEAR, -2); // Min date is one year ago
        Date minDate = calendar.getTime();

        SpinnerDateModel dateModel = new SpinnerDateModel(dueDate, minDate, maxDate, Calendar.DAY_OF_MONTH);
        JSpinner dueDateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dueDateSpinner, "yyyy-MM-dd HH:mm");
        dueDateSpinner.setEditor(dateEditor);
        formPanel.add(dueDateSpinner, gbc);

        // Max points
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Max Points:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField pointsField = new JTextField(String.valueOf(assignment.getMaxPoints()), 5);
        formPanel.add(pointsField, gbc);

        // Weight
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Weight (%):"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField weightField = new JTextField(String.format("%.1f", assignment.getWeight() * 100), 5);
        formPanel.add(weightField, gbc);

        // Submission types
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Submission Types:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField submissionTypesField = new JTextField(String.join(", ", assignment.getSubmissionTypes()), 20);
        formPanel.add(submissionTypesField, gbc);

        // Description (this would be implemented if Assignment had a description field)
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        JTextArea descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        // If Assignment had a description field, we would set it here
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        formPanel.add(descScrollPane, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton saveButton = new JButton("Save Changes");
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        saveButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Title is required",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Get assignment type
                String typeStr = (String) typeComboBox.getSelectedItem();
                Assignment.Type type = Assignment.Type.valueOf(typeStr);

                // Get due date
                Date dueDate1 = (Date) dueDateSpinner.getValue();
                Timestamp dueDateTimestamp = new Timestamp(dueDate1.getTime());

                // Get max points
                double maxPoints = Double.parseDouble(pointsField.getText());
                if (maxPoints <= 0) {
                    throw new NumberFormatException("Max points must be positive");
                }

                // Get weight
                double weight = Double.parseDouble(weightField.getText()) / 100.0; // Convert to decimal
                if (weight <= 0) {
                    throw new NumberFormatException("Weight must be positive");
                }

                // Get submission types
                String submissionTypesText = submissionTypesField.getText().trim();
                List<String> submissionTypes = new ArrayList<>();
                if (!submissionTypesText.isEmpty()) {
                    String[] types = submissionTypesText.split(",");
                    for (String t : types) {
                        submissionTypes.add(t.trim());
                    }
                }

                // Get description (would be implemented if Assignment had a description field)
                String description = descriptionArea.getText().trim();

                // Update assignment object
                assignment.setName(title);
                assignment.setType(type);
                assignment.setDueDate(dueDateTimestamp);
                assignment.setMaxPoints(maxPoints);
                assignment.setWeight(weight);
                assignment.setSubmissionTypes(submissionTypes);
                // If Assignment had a description field, we would set it here

                // Save to database
                teacher.editAssignment(assignment);

                // Show success message
                JOptionPane.showMessageDialog(dialog,
                        "Assignment updated successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                // Close dialog
                dialog.dispose();

                // Refresh assignments panel
                // For now, switch to another tab and back
                int currentIndex = parentTabs.getSelectedIndex();
                parentTabs.setSelectedIndex(currentIndex == 0 ? 1 : 0);
                parentTabs.setSelectedIndex(3); // Switch to Assignments tab

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter valid numbers for max points and weight: " + ex.getMessage(),
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error updating assignment: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void deleteSelectedAssignment() {
        int selectedRow = assignmentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an assignment to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert row index from view to model in case of sorting
        int modelRow = assignmentTable.convertRowIndexToModel(selectedRow);

        // Get assignment ID and name from the table
        int assignmentId = (int) assignmentTable.getModel().getValueAt(modelRow, 0);
        String assignmentName = (String) assignmentTable.getModel().getValueAt(modelRow, 1);

        // Get assignment from database
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        Assignment assignment = assignmentDAO.read(assignmentId);

        if (assignment == null) {
            JOptionPane.showMessageDialog(this,
                    "Assignment not found in database",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check for submissions
        SubmissionDAO submissionDAO = SubmissionDAO.getInstance();
        List<Submission> submissions = submissionDAO.readAllCondition("assignment_id", assignmentId);
        boolean hasSubmissions = submissions != null && !submissions.isEmpty();

        // Build confirmation message
        StringBuilder message = new StringBuilder();
        message.append("Are you sure you want to delete the assignment '").append(assignmentName).append("'?\n\n");

        if (hasSubmissions) {
            int submissionCount = submissions.size();
            message.append("This assignment has ").append(submissionCount).append(" submission");
            if (submissionCount > 1)
                message.append("s");
            message.append(".\n\n");

            // Count graded submissions
            long gradedCount = submissions.stream()
                    .filter(s -> s.getStatus() == Submission.Status.GRADED)
                    .count();

            if (gradedCount > 0) {
                message.append("Including ").append(gradedCount).append(" graded submission");
                if (gradedCount > 1)
                    message.append("s");
                message.append(".\n\n");
            }
        }

        message.append("Deleting this assignment will:\n");
        if (hasSubmissions) {
            message.append("• Delete all student submissions for this assignment\n");
            message.append("• Remove all grades associated with this assignment\n");
        }
        message.append("• Remove the assignment from the course\n\n");
        message.append("This action cannot be undone. Do you want to continue?");

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(this,
                message.toString(),
                "Confirm Assignment Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // Delete all submissions first (if any)
            if (hasSubmissions) {
                for (Submission submission : submissions) {
                    // Delete the submission file if it exists
                    if (submission.getFilepath() != null && !submission.getFilepath().isEmpty()) {
                        SubmissionFileManager.deleteSubmission(submission);
                    }

                    // Delete the submission from database
                    submissionDAO.delete(submission.getId());
                }
            }

            // Delete the assignment
            teacher.deleteAssignment(assignment);

            // Update the table model
            DefaultTableModel model = (DefaultTableModel) assignmentTable.getModel();
            model.removeRow(modelRow);

            JOptionPane.showMessageDialog(this,
                    "Assignment deleted successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error deleting assignment: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewSubmissionsForAssignment() {
        int selectedRow = assignmentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an assignment to view submissions",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert row index from view to model in case of sorting
        int modelRow = assignmentTable.convertRowIndexToModel(selectedRow);

        // Get assignment ID and name from the table
        int assignmentId = (int) assignmentTable.getModel().getValueAt(modelRow, 0);
        String assignmentName = (String) assignmentTable.getModel().getValueAt(modelRow, 1);
        String courseName = (String) assignmentTable.getModel().getValueAt(modelRow, 2);

        // Get assignment from database
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        Assignment assignment = assignmentDAO.read(assignmentId);

        if (assignment == null) {
            JOptionPane.showMessageDialog(this,
                    "Assignment not found in database",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create submissions dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Submissions for ",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header panel
        JPanel headerPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Assignment Information"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        headerPanel.add(new JLabel("Assignment:"));
        headerPanel.add(new JLabel(assignmentName));

        headerPanel.add(new JLabel("Course:"));
        headerPanel.add(new JLabel(courseName));

        headerPanel.add(new JLabel("Due Date:"));
        headerPanel.add(new JLabel(assignment.getDueDate().toString()));

        panel.add(headerPanel, BorderLayout.NORTH);

        // Get submissions for this assignment
        SubmissionDAO submissionDAO = SubmissionDAO.getInstance();
        List<Submission> submissions = submissionDAO.readAllCondition("assignment_id", assignmentId);

        // Submissions table
        String[] submissionColumns = { "ID", "Student", "Submitted Date", "Status", "Grade", "Feedback" };
        DefaultTableModel submissionModel = new DefaultTableModel(submissionColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0)
                    return Integer.class;
                if (column == 2)
                    return Timestamp.class;
                return String.class;
            }
        };

        // Get user DAO for student information
        UserDAO userDAO = UserDAO.getInstance();

        // Add submissions to table
        if (submissions != null && !submissions.isEmpty()) {
            for (Submission submission : submissions) {
                // Get student info
                String studentName = "Unknown";
                if (!submission.getCollaboratorIds().isEmpty()) {
                    int studentId = submission.getCollaboratorIds().get(0);
                    User student = userDAO.read(studentId);
                    if (student != null) {
                        studentName = student.getName();

                        // If there are multiple collaborators, show as "Student Name + (X others)"
                        if (submission.getCollaboratorIds().size() > 1) {
                            studentName += " (+" + (submission.getCollaboratorIds().size() - 1) + " others)";
                        }
                    }
                }

                // Format grade
                String gradeDisplay = "-";
                if (submission.getStatus() == Submission.Status.GRADED) {
                    gradeDisplay = String.format("%.1f/%.1f",
                            submission.getPointsEarned(), assignment.getMaxPoints());
                }

                // Placeholder for feedback (would be implemented if Submission had a feedback
                // field)
                String feedback = "";

                // Add to table
                submissionModel.addRow(new Object[] {
                        submission.getId(),
                        studentName,
                        submission.getSubmittedAt(),
                        submission.getStatus().toString(),
                        gradeDisplay,
                        feedback
                });
            }
        }

        JTable submissionTable = new JTable(submissionModel);
        submissionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        submissionTable.setAutoCreateRowSorter(true);

        // Set preferred column widths
        submissionTable.getColumnModel().getColumn(0).setPreferredWidth(50); // ID
        submissionTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Student
        submissionTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Submitted Date
        submissionTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Status
        submissionTable.getColumnModel().getColumn(4).setPreferredWidth(80); // Grade
        submissionTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Feedback

        JScrollPane scrollPane = new JScrollPane(submissionTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Statistics panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Statistics"));

        int totalSubmissions = submissions != null ? submissions.size() : 0;

        // Count submissions by status
        int gradedCount = 0;
        int ungradedCount = 0;
        int lateCount = 0;

        if (submissions != null) {
            gradedCount = (int) submissions.stream()
                    .filter(s -> s.getStatus() == Submission.Status.GRADED)
                    .count();

            ungradedCount = (int) submissions.stream()
                    .filter(s -> s.getStatus() == Submission.Status.UNGRADED)
                    .count();

            lateCount = (int) submissions.stream()
                    .filter(s -> s.getStatus() == Submission.Status.LATE)
                    .count();
        }

        // Calculate average grade for graded submissions
        double totalPoints = 0;
        double avgGrade = 0;

        if (gradedCount > 0) {
            totalPoints = submissions.stream()
                    .filter(s -> s.getStatus() == Submission.Status.GRADED)
                    .mapToDouble(Submission::getPointsEarned)
                    .sum();

            avgGrade = totalPoints / gradedCount;
        }

        // Add statistics labels
        statsPanel.add(new JLabel("Total Submissions: " + totalSubmissions));
        statsPanel.add(new JLabel("Graded: " + gradedCount));
        statsPanel.add(new JLabel("Ungraded: " + ungradedCount));
        statsPanel.add(new JLabel("Late: " + lateCount));

        if (gradedCount > 0) {
            statsPanel.add(new JLabel(String.format("Average Grade: %.1f/%.1f", avgGrade, assignment.getMaxPoints())));
        }

        panel.add(statsPanel, BorderLayout.SOUTH);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton viewButton = new JButton("View Submission");
        viewButton.addActionListener(e -> {
            int selectedSubmissionRow = submissionTable.getSelectedRow();
            if (selectedSubmissionRow == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Please select a submission to view",
                        "No Selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Get submission ID
            int submissionId = (int) submissionTable.getValueAt(
                    submissionTable.convertRowIndexToModel(selectedSubmissionRow), 0);

            // Get submission from database
            Submission submission = submissionDAO.read(submissionId);

            if (submission != null) {
                // Open submission viewer
                viewSubmission(submission, assignment);
            }
        });

        JButton gradeButton = new JButton("Grade Submission");
        gradeButton.addActionListener(e -> {
            int selectedSubmissionRow = submissionTable.getSelectedRow();
            if (selectedSubmissionRow == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Please select a submission to grade",
                        "No Selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Get submission ID
            int submissionId = (int) submissionTable.getValueAt(
                    submissionTable.convertRowIndexToModel(selectedSubmissionRow), 0);

            // Get submission from database
            Submission submission = submissionDAO.read(submissionId);

            if (submission != null) {
                // Open grading dialog
                gradeSubmission(submission, assignment);

                // Refresh submission table after grading
                int rowIdx = submissionTable.convertRowIndexToModel(selectedSubmissionRow);

                if (submission.getStatus() == Submission.Status.GRADED) {
                    submissionModel.setValueAt(submission.getStatus().toString(), rowIdx, 3);
                    submissionModel.setValueAt(String.format("%.1f/%.1f",
                            submission.getPointsEarned(), assignment.getMaxPoints()), rowIdx, 4);
                }
            }
        });

        JButton exportButton = new JButton("Export Grades");
        exportButton.addActionListener(e -> {
            if (submissions == null || submissions.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "No submissions to export",
                        "Export Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Show file chooser for export
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Grades");
            fileChooser.setSelectedFile(new File(assignmentName.replaceAll("\\W+", "_") + "_grades.csv"));

            if (fileChooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // Export CSV of grades
                exportSubmissionGrades(file, submissions, assignment);
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(viewButton);
        buttonPanel.add(gradeButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.NORTH);

        dialog.add(panel);
        dialog.setVisible(true);
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

    // Helper method to update the template dropdown based on selected course
    private void updateTemplateDropdown(JComboBox<AssignmentTemplateItem> templateComboBox) {
        templateComboBox.removeAllItems();

        // Get the selected course name
        String selected = (String) courseCombo.getSelectedItem();
        if (selected == null || "All Courses".equals(selected)) {
            return; // nothing selected or sentinel value
        }

        // Look-up the Course object
        Course course = teacherCourses.stream()
                .filter(c -> c.getName().equals(selected))
                .findFirst().orElse(null);
        if (course == null || course.getCourseTemplate() == null)
            return;

        CourseTemplate tpl = course.getCourseTemplate();
        for (AssignmentTemplate at : tpl.getAssignmentTemplates()) {
            templateComboBox.addItem(new AssignmentTemplateItem(at));
        }
    }
}
