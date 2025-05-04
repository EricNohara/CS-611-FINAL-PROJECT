package ui.dashboard.panels;

import db.*;
import model.*;
import ui.UIConstants;
import ui.utils.PaddedCellRenderer;
import ui.utils.Padding;
import ui.utils.TemplateItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

// Course management panel
public final class CourseManagementPanel extends JPanel {

    private final Admin admin;
    private final DefaultTableModel courseModel;
    private final JTable courseTable;

    public CourseManagementPanel(Admin admin) {
        super(new BorderLayout(10, 10));
        this.admin = admin;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top row buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Course");
        JButton editBtn = new JButton("Edit Course");
        JButton delBtn = new JButton("Delete Course");
        JButton viewBtn = new JButton("View Course Details");
        JButton refreshBtn = new JButton("Refresh");
        btnRow.add(addBtn);
        btnRow.add(editBtn);
        btnRow.add(delBtn);
        btnRow.add(viewBtn);
        btnRow.add(refreshBtn);
        add(btnRow, BorderLayout.NORTH);

        // Table
        String[] cols = { "ID", "Course Name", "Template", "Active", "Students" };
        courseModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 4) ? Integer.class
                        : (c == 3) ? Boolean.class
                                : String.class;
            }
        };
        courseTable = new JTable(courseModel);
        courseTable.setAutoCreateRowSorter(true);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(courseTable), BorderLayout.CENTER);
        courseTable.getTableHeader().setFont(courseTable.getTableHeader().getFont().deriveFont(Font.BOLD));

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(courseTable);
        paddedRenderer.applyCellPadding(courseTable);

        // Bottom filter
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox activeOnly = new JCheckBox("Show Active Courses Only");
        filterPanel.add(activeOnly);
        add(filterPanel, BorderLayout.SOUTH);

        // Listeners
        addBtn.addActionListener(e -> showAddCourseDialog());
        editBtn.addActionListener(e -> editSelectedCourse());
        delBtn.addActionListener(e -> deleteSelectedCourse());
        viewBtn.addActionListener(e -> viewCourseDetails());
        refreshBtn.addActionListener(e -> loadCourseData());
        activeOnly.addActionListener(e -> {
            if (activeOnly.isSelected())
                filterActiveCourses();
            else
                loadCourseData();
        });

        loadCourseData();
    }

    /* ===== helper methods – paste bodies verbatim ===== */
    private void loadCourseData() {
        // Clear existing data
        courseModel.setRowCount(0);

        try {
            // Get course DAO instance
            CourseDAO courseDAO = CourseDAO.getInstance();

            // Get all courses from the database
            List<Course> courses = courseDAO.readAll();

            // Add courses to table
            for (Course course : courses) {
                // Get the template name
                String templateName = "None";
                if (course.getCourseTemplate() != null) {
                    templateName = course.getCourseTemplate().getName();
                }

                // Get the number of students
                int studentCount = courseDAO.getStudentCountForCourse(course.getId());

                Object[] rowData = {
                        course.getId(),
                        course.getName(),
                        templateName,
                        course.isActive(),
                        studentCount
                };
                courseModel.addRow(rowData);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading courses: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterActiveCourses() {
        // Clear existing data
        courseModel.setRowCount(0);

        try {
            // Get course DAO instance
            CourseDAO courseDAO = CourseDAO.getInstance();

            // Get all courses from the database
            List<Course> courses = courseDAO.readAll();

            // Add only active courses to table
            for (Course course : courses) {
                if (course.isActive()) {
                    // Get the template name
                    String templateName = "None";
                    if (course.getCourseTemplate() != null) {
                        templateName = course.getCourseTemplate().getName();
                    }

                    // Get the number of students
                    int studentCount = courseDAO.getStudentCountForCourse(course.getId());

                    Object[] rowData = {
                            course.getId(),
                            course.getName(),
                            templateName,
                            true, // Active is true for filtered courses
                            studentCount
                    };
                    courseModel.addRow(rowData);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error filtering courses: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddCourseDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add New Course",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new GridBagLayout());
        Padding.addPanelPaddingDefault(panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Course name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(UIConstants.getBoldLabel("Course Name:"), gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        panel.add(nameField, gbc);
        Padding.addInputPaddingDefault(nameField);

        // Course template selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(UIConstants.getBoldLabel("Course Template:"), gbc);

        gbc.gridx = 1;
        JComboBox<TemplateItem> templateComboBox = new JComboBox<>();
        // Add a "None" option
        templateComboBox.addItem(new TemplateItem(-1, "None"));
        Padding.addInputPaddingDefault(templateComboBox);

        // Get course templates from database
        try {
            CourseTemplateDAO templateDAO = CourseTemplateDAO.getInstance();
            List<CourseTemplate> templates = templateDAO.readAll();
            for (CourseTemplate template : templates) {
                templateComboBox.addItem(new TemplateItem(template.getId(), template.getName()));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(dialog,
                    "Error loading course templates: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        panel.add(templateComboBox, gbc);

        // Active checkbox
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(UIConstants.getBoldLabel("Active:"), gbc);

        gbc.gridx = 1;
        JCheckBox activeCheckBox = new JCheckBox();
        activeCheckBox.setSelected(true);
        panel.add(activeCheckBox, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);

        // Save button action
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Course name is required",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Get selected template
                TemplateItem selectedTemplate = (TemplateItem) templateComboBox.getSelectedItem();
                int templateId = selectedTemplate.getId();

                // Create course object
                Course course = new Course();
                course.setName(name);
                course.setActive(activeCheckBox.isSelected());

                if (templateId != -1) {
                    // Get the template from database
                    CourseTemplateDAO templateDAO = CourseTemplateDAO.getInstance();
                    CourseTemplate template = templateDAO.read(templateId);
                    course.setCourseTemplate(template);
                    course.setCourseTemplateId(templateId);
                }

                // Save course to database
                CourseDAO courseDAO = CourseDAO.getInstance();
                courseDAO.create(course);

                // Reload course data
                loadCourseData();

                // Close dialog
                dialog.dispose();

                JOptionPane.showMessageDialog(this,
                        "Course added successfully",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error adding course: " + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Cancel button action
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(panel);
        dialog.setLocationRelativeTo(this);
        dialog.pack();
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
        int courseId = (int) courseModel.getValueAt(modelRow, 0);

        try {
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

            // Create edit dialog
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Course",
                    Dialog.ModalityType.APPLICATION_MODAL);

            JPanel panel = new JPanel(new GridBagLayout());
            Padding.addPanelPaddingDefault(panel);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Course ID field (display only)
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(UIConstants.getBoldLabel("Course ID:"), gbc);

            gbc.gridx = 1;
            JLabel idLabel = new JLabel(String.valueOf(course.getId()));
            panel.add(idLabel, gbc);

            // Course name field
            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(UIConstants.getBoldLabel("Course Name:"), gbc);

            gbc.gridx = 1;
            JTextField nameField = new JTextField(course.getName(), 20);
            panel.add(nameField, gbc);
            Padding.addInputPaddingDefault(nameField);

            // Course template selection
            gbc.gridx = 0;
            gbc.gridy = 2;
            panel.add(UIConstants.getBoldLabel("Course Template:"), gbc);

            gbc.gridx = 1;
            JComboBox<TemplateItem> templateComboBox = new JComboBox<>();
            Padding.addInputPaddingDefault(templateComboBox);
            // Add a "None" option
            templateComboBox.addItem(new TemplateItem(-1, "None"));

            // Get course templates from database
            CourseTemplateDAO templateDAO = CourseTemplateDAO.getInstance();
            List<CourseTemplate> templates = templateDAO.readAll();
            int selectedIndex = 0;

            for (int i = 0; i < templates.size(); i++) {
                CourseTemplate template = templates.get(i);
                templateComboBox.addItem(new TemplateItem(template.getId(), template.getName()));

                if (course.getCourseTemplateId() == template.getId()) {
                    selectedIndex = i + 1; // +1 because of the "None" option
                }
            }

            templateComboBox.setSelectedIndex(selectedIndex);
            panel.add(templateComboBox, gbc);

            // Active checkbox
            gbc.gridx = 0;
            gbc.gridy = 3;
            panel.add(UIConstants.getBoldLabel("Active:"), gbc);

            gbc.gridx = 1;
            JCheckBox activeCheckBox = new JCheckBox();
            activeCheckBox.setSelected(course.isActive());
            panel.add(activeCheckBox, gbc);

            // Buttons
            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton saveButton = new JButton("Save Changes");
            JButton cancelButton = new JButton("Cancel");
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            panel.add(buttonPanel, gbc);

            // Save button action
            saveButton.addActionListener(e -> {
                String name = nameField.getText().trim();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Course name is required",
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    // Get selected template
                    TemplateItem selectedTemplate = (TemplateItem) templateComboBox.getSelectedItem();
                    int templateId = selectedTemplate.getId();

                    // Update course object
                    course.setName(name);
                    course.setActive(activeCheckBox.isSelected());

                    if (templateId != -1) {
                        // Get the template from database
                        CourseTemplate template = templateDAO.read(templateId);
                        course.setCourseTemplate(template);
                        course.setCourseTemplateId(templateId);
                    } else {
                        course.setCourseTemplate(null);
                        course.setCourseTemplateId(-1);
                    }

                    // Update course in database
                    courseDAO.update(course);

                    // Reload course data
                    loadCourseData();

                    // Close dialog
                    dialog.dispose();

                    JOptionPane.showMessageDialog(this,
                            "Course updated successfully",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Error updating course: " + ex.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });

            // Cancel button action
            cancelButton.addActionListener(e -> dialog.dispose());

            dialog.add(panel);
            dialog.setLocationRelativeTo(this);
            dialog.pack();
            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error editing course: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
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
        int courseId = (int) courseModel.getValueAt(modelRow, 0);
        String courseName = (String) courseModel.getValueAt(modelRow, 1);

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the course '" + courseName + "'?\n" +
                        "This will also delete all assignments and submissions for this course.\n" +
                        "This action cannot be undone.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // Delete course from database
            CourseDAO courseDAO = CourseDAO.getInstance();
            courseDAO.delete(courseId);

            // Reload course data
            loadCourseData();

            JOptionPane.showMessageDialog(this,
                    "Course deleted successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error deleting course: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewCourseDetails() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a course to view details",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert row index from view to model in case of sorting
        int modelRow = courseTable.convertRowIndexToModel(selectedRow);

        // Get course ID from the table
        int courseId = (int) courseTable.getModel().getValueAt(modelRow, 0);

        try {
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

            // Create detail dialog
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Course Details - " + course.getName(),
                    Dialog.ModalityType.APPLICATION_MODAL);

            // Create tabbed pane for different details
            JTabbedPane detailsTabs = new JTabbedPane();

            // Course info panel
            JPanel infoPanel = new JPanel(new BorderLayout(10, 10));
            Padding.addPanelPaddingDefault(infoPanel);

            // Basic info section
            JPanel basicInfoPanel = new JPanel(new GridLayout(4, 2, 10, 10));
            basicInfoPanel.setBorder(BorderFactory.createTitledBorder("Basic Information"));

            basicInfoPanel.add(new JLabel("Course ID:"));
            basicInfoPanel.add(new JLabel(String.valueOf(course.getId())));

            basicInfoPanel.add(new JLabel("Course Name:"));
            basicInfoPanel.add(new JLabel(course.getName()));

            basicInfoPanel.add(new JLabel("Template:"));
            String templateName = course.getCourseTemplate() != null ? course.getCourseTemplate().getName() : "None";
            basicInfoPanel.add(new JLabel(templateName));

            basicInfoPanel.add(new JLabel("Status:"));
            JLabel statusLabel = new JLabel(course.isActive() ? "Active" : "Inactive");
            statusLabel.setForeground(course.isActive() ? new Color(0, 128, 0) : Color.RED);
            basicInfoPanel.add(statusLabel);

            infoPanel.add(basicInfoPanel, BorderLayout.NORTH);

            // Students list
            JPanel studentsPanel = new JPanel(new BorderLayout(10, 10));
            studentsPanel.setBorder(BorderFactory.createTitledBorder("Enrolled Students"));

            // Get students enrolled in this course
            List<User> students = courseDAO.getStudentsInCourse(course.getId());

            // Create table for students
            String[] studentColumns = { "ID", "Name", "Email" };
            DefaultTableModel studentModel = new DefaultTableModel(studentColumns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            for (User student : students) {
                studentModel.addRow(new Object[] {
                        student.getId(),
                        student.getName(),
                        student.getEmail()
                });
            }

            JTable studentTable = new JTable(studentModel);
            JScrollPane studentScrollPane = new JScrollPane(studentTable);
            studentsPanel.add(studentScrollPane, BorderLayout.CENTER);

            studentTable.getTableHeader().setFont(studentTable.getTableHeader().getFont().deriveFont(Font.BOLD));
            PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
            PaddedCellRenderer.setDefaultRowHeight(studentTable);
            paddedRenderer.applyCellPadding(studentTable);

            infoPanel.add(studentsPanel, BorderLayout.CENTER);

            // Add info panel to tabs
            detailsTabs.addTab("Information", infoPanel);

            // Assignments panel
            JPanel assignmentsPanel = new JPanel(new BorderLayout(10, 10));
            Padding.addPanelPaddingDefault(assignmentsPanel);

            // Get assignments for this course
            AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
            List<Assignment> assignments = assignmentDAO.readAllCondition("course_id", course.getId());

            // Create table for assignments
            String[] assignmentColumns = { "ID", "Name", "Type", "Due Date", "Max Points" };
            DefaultTableModel assignmentModel = new DefaultTableModel(assignmentColumns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            for (Assignment assignment : assignments) {
                assignmentModel.addRow(new Object[] {
                        assignment.getId(),
                        assignment.getName(),
                        assignment.getType(),
                        assignment.getDueDate(),
                        assignment.getMaxPoints()
                });
            }

            JTable assignmentTable = new JTable(assignmentModel);
            JScrollPane assignmentScrollPane = new JScrollPane(assignmentTable);
            assignmentsPanel.add(assignmentScrollPane, BorderLayout.CENTER);
            assignmentTable.getTableHeader().setFont(assignmentTable.getTableHeader().getFont().deriveFont(Font.BOLD));

            PaddedCellRenderer.setDefaultRowHeight(assignmentTable);
            paddedRenderer.applyCellPadding(assignmentTable);


            // Add assignments panel to tabs
            detailsTabs.addTab("Assignments", assignmentsPanel);

            // Add tabbedPane to dialog
            dialog.add(detailsTabs);

            // Add close button
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dialog.dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(closeButton);

            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setLocationRelativeTo(this);
            dialog.pack();
            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error viewing course details: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
