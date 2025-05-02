package ui.dashboard.panels;

import db.*;
import model.*;
import ui.UIConstants;
import ui.utils.PaddedCellRenderer;
import ui.utils.Padding;
import ui.utils.TemplateItem;
import utils.Hasher; // if used in helper dialogs
import utils.SubmissionFileManager; // if used later

import javax.naming.ldap.UnsolicitedNotification;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.List;

// Courses tab
public final class CoursesPanel extends JPanel implements Refreshable{
    private final User teacher;
    private final JTabbedPane parentTabs;

    // UI elements we need to refresh
    private DefaultTableModel courseModel;
    private JTable courseTable;

    public CoursesPanel(User teacher, JTabbedPane parentTabs) {
        super(new BorderLayout(10, 10));
        this.teacher = teacher;
        this.parentTabs = parentTabs;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    /* ====== UI construction ====== */
    private void buildUI() {
        // ---------- top button row ----------
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newBtn = new JButton("New Course");
        JButton editBtn = new JButton("Edit Course");
        JButton delBtn = new JButton("Delete Course");
        buttons.add(newBtn);
        buttons.add(editBtn);
        buttons.add(delBtn);
        add(buttons, BorderLayout.NORTH);

        // ---------- table ----------
        String[] cols = { "ID", "Course Name", "Template", "Students", "Status" };
        courseModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        courseTable = new JTable(courseModel);
        add(new JScrollPane(courseTable), BorderLayout.CENTER);

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(courseTable);
        paddedRenderer.applyCellPadding(courseTable);


        loadCourseRows(); // fills the table once

        /* wire buttons */
        newBtn.addActionListener(e -> createNewCourse());
        editBtn.addActionListener(e -> editSelectedCourse());
        delBtn.addActionListener(e -> deleteSelectedCourse());
    }

    /* ====== table refresh ====== */
    private void loadCourseRows() {
        courseModel.setRowCount(0); // clear
        CourseDAO dao = CourseDAO.getInstance();
        List<Course> courses = dao.getCoursesForTeacher(teacher.getId());

        for (Course c : courses) {
            String tpl = (c.getCourseTemplate() == null) ? "None"
                    : c.getCourseTemplate().getName();
            int students = dao.getStudentCountForCourse(c.getId());

            courseModel.addRow(new Object[] {
                    c.getId(), c.getName(), tpl,
                    students, c.isActive() ? "Active" : "Inactive"
            });
        }
    }

    // private helpers
    private void createNewCourse() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Create New Course",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        Padding.addPanelPaddingDefault(panel);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Course name
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(UIConstants.getBoldLabel("Course Name:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField courseNameField = new JTextField(30);
        formPanel.add(courseNameField, gbc);
        Padding.addInputPaddingDefault(courseNameField);

        // Template selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(UIConstants.getBoldLabel("Course Template:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;

        // Get available templates from database
        CourseTemplateDAO templateDAO = CourseTemplateDAO.getInstance();
        List<CourseTemplate> templates = templateDAO.readAll();

        // Create a model for the combo box
        DefaultComboBoxModel<TemplateItem> templateModel = new DefaultComboBoxModel<>();
        templateModel.addElement(new TemplateItem(-1, "None")); // Option for no template

        for (CourseTemplate template : templates) {
            templateModel.addElement(new TemplateItem(template.getId(), template.getName()));
        }

        JComboBox<TemplateItem> templateComboBox = new JComboBox<>(templateModel);
        formPanel.add(templateComboBox, gbc);
        Padding.addInputPaddingDefault(templateComboBox);

        // Active checkbox
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        formPanel.add(UIConstants.getBoldLabel("Active:"), gbc);

        gbc.gridx = 1;
        JCheckBox activeCheckBox = new JCheckBox();
        activeCheckBox.setSelected(true);
        formPanel.add(activeCheckBox, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        formPanel.add(UIConstants.getBoldLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        JTextArea descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(UIConstants.DEFAULT_FONT); // assuming you have one
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        formPanel.add(descScrollPane, gbc);
        Padding.addInputPaddingDefault(descriptionArea);

        panel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton createButton = new JButton("Create Course");
        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        createButton.addActionListener(e -> {
            String courseName = courseNameField.getText();
            if (courseName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Course name is required",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create course in database
            try {
                // Get selected template
                TemplateItem selectedItem = (TemplateItem) templateComboBox.getSelectedItem();
                int templateId = selectedItem.getId();

                // Create course object
                Course course = new Course();
                course.setName(courseName);
                course.setActive(activeCheckBox.isSelected());

                if (templateId != -1) {
                    // Get the template from database
                    CourseTemplate template = templateDAO.read(templateId);
                    course.setCourseTemplateId(templateId);
                    course.setCourseTemplate(template);
                }

                // Save course to database
                CourseDAO courseDAO = CourseDAO.getInstance();
                courseDAO.create(course);

                // Add the teacher to the course with TEACHER role
                UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();
                UserCourse userCourse = new UserCourse(
                        teacher.getId(),
                        course.getId(),
                        UserCourse.Status.ACTIVE,
                        User.Role.TEACHER);
                userCourseDAO.create(userCourse);

                // Show success message
                JOptionPane.showMessageDialog(dialog,
                        "Course created successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                // Close dialog
                dialog.dispose();

                // Refresh the courses panel
                refresh();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error creating course: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void editSelectedCourse() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a course to edit",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert row index from view to model in case of sorting
        int modelRow = courseTable.convertRowIndexToModel(selectedRow);

        // Get course ID from the table
        int courseId = (int) courseTable.getModel().getValueAt(modelRow, 0);

        // Get course from database
        CourseDAO courseDAO = CourseDAO.getInstance();
        Course course = courseDAO.read(courseId);

        if (course == null) {
            JOptionPane.showMessageDialog(this,
                    "Course not found in database",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if this teacher is authorized to edit this course
        UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();
        UserCourse userCourse = userCourseDAO.read(teacher.getId(), courseId);

        if (userCourse == null || userCourse.getRole() != User.Role.TEACHER) {
            JOptionPane.showMessageDialog(this,
                    "You do not have permission to edit this course",
                    "Access Denied",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create edit dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Course",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        Padding.addPanelPaddingDefault(panel);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Course ID field (display only)
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(UIConstants.getBoldLabel("Course ID:"), gbc);

        gbc.gridx = 1;
        JLabel idLabel = new JLabel(String.valueOf(course.getId()));
        formPanel.add(idLabel, gbc);

        // Course name field
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(UIConstants.getBoldLabel("Course Name:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(course.getName(), 30);
        formPanel.add(nameField, gbc);
        Padding.addInputPaddingDefault(nameField);

        // Template selection
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(UIConstants.getBoldLabel("Course Templates:"), gbc);

        gbc.gridx = 1;

        // Get available templates from database
        CourseTemplateDAO templateDAO = CourseTemplateDAO.getInstance();
        List<CourseTemplate> templates = templateDAO.readAll();

        // Create a model for the combo box
        DefaultComboBoxModel<TemplateItem> templateModel = new DefaultComboBoxModel<>();
        templateModel.addElement(new TemplateItem(-1, "None")); // Option for no template

        int selectedIndex = 0; // Default to "None"

        for (int i = 0; i < templates.size(); i++) {
            CourseTemplate template = templates.get(i);
            templateModel.addElement(new TemplateItem(template.getId(), template.getName()));

            // Check if this is the current template
            if (course.getCourseTemplate() != null &&
                    course.getCourseTemplate().getId() == template.getId()) {
                selectedIndex = i + 1; // +1 because of the "None" option
            }
        }

        JComboBox<TemplateItem> templateComboBox = new JComboBox<>(templateModel);
        Padding.addInputPaddingDefault(templateComboBox);
        templateComboBox.setSelectedIndex(selectedIndex);
        formPanel.add(templateComboBox, gbc);

        // Active checkbox
        gbc.gridx = 0;
        gbc.gridy = 3;
        
        formPanel.add(UIConstants.getBoldLabel("Active:"), gbc);

        gbc.gridx = 1;
        JCheckBox activeCheckBox = new JCheckBox();
        activeCheckBox.setSelected(course.isActive());
        formPanel.add(activeCheckBox, gbc);

        // Description (optional in this implementation)
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(UIConstants.getBoldLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        JTextArea descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(UIConstants.DEFAULT_FONT);
        Padding.addInputPaddingDefault(descriptionArea);
        // If there's a description field in Course class, set the text here
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
            String courseName = nameField.getText().trim();
            if (courseName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Course name is required",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Get selected template
                TemplateItem selectedItem = (TemplateItem) templateComboBox.getSelectedItem();
                int templateId = selectedItem.getId();

                // Update course object
                course.setName(courseName);
                course.setActive(activeCheckBox.isSelected());

                if (templateId != -1) {
                    // Get the template from database
                    CourseTemplate template = templateDAO.read(templateId);
                    course.setCourseTemplateId(templateId);
                    course.setCourseTemplate(template);
                } else {
                    // No template selected
                    course.setCourseTemplateId(-1);
                    course.setCourseTemplate(null);
                }

                // Update course in database
                courseDAO.update(course);

                // Show success message
                JOptionPane.showMessageDialog(dialog,
                        "Course updated successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                // Close dialog
                dialog.dispose();

                // Refresh the courses panel
               refresh();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error updating course: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void deleteSelectedCourse() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a course to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert row index from view to model in case of sorting
        int modelRow = courseTable.convertRowIndexToModel(selectedRow);

        // Get course ID and name from the table
        int courseId = (int) courseTable.getModel().getValueAt(modelRow, 0);
        String courseName = (String) courseTable.getModel().getValueAt(modelRow, 1);

        // Get course from database
        CourseDAO courseDAO = CourseDAO.getInstance();
        Course course = courseDAO.read(courseId);

        if (course == null) {
            JOptionPane.showMessageDialog(this,
                    "Course not found in database",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if this teacher is authorized to delete this course
        UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();
        UserCourse userCourse = userCourseDAO.read(teacher.getId(), courseId);

        if (userCourse == null || userCourse.getRole() != User.Role.TEACHER) {
            JOptionPane.showMessageDialog(this,
                    "You do not have permission to delete this course",
                    "Access Denied",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if there are submissions for this course
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        List<Assignment> assignments = assignmentDAO.readAllCondition("course_id", courseId);

        boolean hasSubmissions = false;
        if (assignments != null && !assignments.isEmpty()) {
            SubmissionDAO submissionDAO = SubmissionDAO.getInstance();
            for (Assignment assignment : assignments) {
                List<Submission> submissions = submissionDAO.readAllCondition("assignment_id", assignment.getId());
                if (submissions != null && !submissions.isEmpty()) {
                    hasSubmissions = true;
                    break;
                }
            }
        }

        // Calculate number of students enrolled
        int studentCount = courseDAO.getStudentCountForCourse(courseId);

        // Create a warning message with details
        StringBuilder warningMessage = new StringBuilder();
        warningMessage.append("Are you sure you want to delete the course '").append(courseName).append("'?\n\n");

        if (studentCount > 0) {
            warningMessage.append("This course has ").append(studentCount).append(" student");
            if (studentCount > 1)
                warningMessage.append("s");
            warningMessage.append(" enrolled.\n");
        }

        if (assignments != null && !assignments.isEmpty()) {
            warningMessage.append("This course has ").append(assignments.size()).append(" assignment");
            if (assignments.size() > 1)
                warningMessage.append("s");
            warningMessage.append(".\n");
        }

        if (hasSubmissions) {
            warningMessage.append("There are student submissions for this course.\n");
        }

        warningMessage.append("\nDeleting this course will:\n");
        warningMessage.append("- Remove all student enrollments\n");
        warningMessage.append("- Delete all assignments\n");
        if (hasSubmissions) {
            warningMessage.append("- Delete all submissions and grades\n");
        }
        warningMessage.append("\nThis action cannot be undone.");

        // Confirm deletion with detailed warning
        int confirm = JOptionPane.showConfirmDialog(this,
                warningMessage.toString(),
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // Delete all enrollments for this course
            // This should be handled by ON DELETE CASCADE in the database,
            // but we'll do it explicitly to be safe
            List<UserCourse> enrollments = userCourseDAO.readAllCondition("course_id", courseId);
            if (enrollments != null) {
                for (UserCourse enrollment : enrollments) {
                    userCourseDAO.delete(enrollment.getUserId(), enrollment.getCourseId());
                }
            }

            // Delete all assignments for this course
            // Submissions should be deleted by ON DELETE CASCADE
            if (assignments != null) {
                for (Assignment assignment : assignments) {
                    assignmentDAO.delete(assignment.getId());
                }
            }

            // Finally, delete the course
            courseDAO.delete(courseId);

            // Show success message
            JOptionPane.showMessageDialog(this,
                    "Course deleted successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // Refresh the courses panel
            refresh();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error deleting course: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        loadCourseRows();
    }
}
