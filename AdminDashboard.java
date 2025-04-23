import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.util.List;

public class AdminDashboard extends JFrame {
    private Admin admin;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTabbedPane tabbedPane;
    
    public AdminDashboard(Admin admin) {
        this.admin = admin;
        
        setTitle("Admin Dashboard - " + admin.getName());
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Welcome panel
        JPanel welcomePanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, " + admin.getName() + " (Administrator)");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        welcomePanel.add(welcomeLabel, BorderLayout.WEST);
        
        JButton logoutButton = new JButton("Logout");
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutPanel.add(logoutButton);
        welcomePanel.add(logoutPanel, BorderLayout.EAST);
        
        mainPanel.add(welcomePanel, BorderLayout.NORTH);
        
        // Create tabbed pane for different admin functions
        tabbedPane = new JTabbedPane();
        
        // User management panel
        JPanel userPanel = createUserManagementPanel();
        tabbedPane.addTab("User Management", userPanel);
        
        // Course management panel
        JPanel coursePanel = createCourseManagementPanel();
        tabbedPane.addTab("Course Management", coursePanel);
        
        // System settings panel
        JPanel settingsPanel = createSystemSettingsPanel();
        tabbedPane.addTab("System Settings", settingsPanel);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        // Add logout action
        logoutButton.addActionListener(e -> logout());
        
        // Add main panel to frame
        add(mainPanel);
    }
    
    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton addUserButton = new JButton("Add User");
        JButton editUserButton = new JButton("Edit User");
        JButton deleteUserButton = new JButton("Delete User");
        JButton refreshButton = new JButton("Refresh");
        
        buttonPanel.add(addUserButton);
        buttonPanel.add(editUserButton);
        buttonPanel.add(deleteUserButton);
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Create table
        String[] columnNames = {"ID", "Name", "Email", "Role", "Created Date", "Last Updated"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
            
            // Override getColumnClass to enable proper sorting
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Integer.class;
                if (column == 4 || column == 5) return Timestamp.class;
                return String.class;
            }
        };
        
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Role:"));
        
        String[] roles = {"All Roles", "STUDENT", "GRADER", "TEACHER", "ADMIN"};
        JComboBox<String> roleFilter = new JComboBox<>(roles);
        roleFilter.addActionListener(e -> {
            String selectedRole = (String) roleFilter.getSelectedItem();
            filterUsersByRole(selectedRole);
        });
        
        filterPanel.add(roleFilter);
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Search:"));
        
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                searchUsers(searchTerm);
            } else {
                loadUserData(); // Reset to show all users
            }
        });
        
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(filterPanel, BorderLayout.WEST);
        topPanel.add(searchPanel, BorderLayout.EAST);
        
        panel.add(topPanel, BorderLayout.SOUTH);
        
        // Load user data
        loadUserData();
        
        // Add button listeners
        addUserButton.addActionListener(e -> showAddUserDialog());
        editUserButton.addActionListener(e -> editSelectedUser());
        deleteUserButton.addActionListener(e -> deleteSelectedUser());
        refreshButton.addActionListener(e -> loadUserData());
        
        return panel;
    }
    
    private JPanel createCourseManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton addCourseButton = new JButton("Add Course");
        JButton editCourseButton = new JButton("Edit Course");
        JButton deleteCourseButton = new JButton("Delete Course");
        JButton viewCourseButton = new JButton("View Course Details");
        JButton refreshButton = new JButton("Refresh");
        
        buttonPanel.add(addCourseButton);
        buttonPanel.add(editCourseButton);
        buttonPanel.add(deleteCourseButton);
        buttonPanel.add(viewCourseButton);
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        
        // Create table
        String[] columnNames = {"ID", "Course Name", "Template", "Active", "Students"};
        DefaultTableModel courseModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
            
            // Override getColumnClass for proper sorting
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Integer.class;
                if (column == 3) return Boolean.class;
                if (column == 4) return Integer.class;
                return String.class;
            }
        };
        
        JTable courseTable = new JTable(courseModel);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.setAutoCreateRowSorter(true);
        
        JScrollPane scrollPane = new JScrollPane(courseTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Load course data from database
        loadCourseData(courseModel);
        
        // Add filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox showActiveOnly = new JCheckBox("Show Active Courses Only");
        showActiveOnly.addActionListener(e -> {
            if (showActiveOnly.isSelected()) {
                filterActiveCourses(courseModel);
            } else {
                loadCourseData(courseModel);
            }
        });
        filterPanel.add(showActiveOnly);
        
        panel.add(filterPanel, BorderLayout.SOUTH);
        
        // Add button listeners
        addCourseButton.addActionListener(e -> showAddCourseDialog(courseModel));
        editCourseButton.addActionListener(e -> editSelectedCourse(courseTable, courseModel));
        deleteCourseButton.addActionListener(e -> deleteSelectedCourse(courseTable, courseModel));
        viewCourseButton.addActionListener(e -> viewCourseDetails(courseTable));
        refreshButton.addActionListener(e -> loadCourseData(courseModel));
        
        return panel;
    }

    private void loadCourseData(DefaultTableModel courseModel) {
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

    private void filterActiveCourses(DefaultTableModel courseModel) {
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

    private void showAddCourseDialog(DefaultTableModel courseModel) {
        JDialog dialog = new JDialog(this, "Add New Course", true);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Course name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Course Name:"), gbc);
        
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        panel.add(nameField, gbc);
        
        // Course template selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Course Template:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<CourseTemplateItem> templateComboBox = new JComboBox<>();
        // Add a "None" option
        templateComboBox.addItem(new CourseTemplateItem(-1, "None"));
        
        // Get course templates from database
        try {
            CourseTemplateDAO templateDAO = new CourseTemplateDAO();
            List<CourseTemplate> templates = templateDAO.readAll();
            for (CourseTemplate template : templates) {
                templateComboBox.addItem(new CourseTemplateItem(template.getId(), template.getName()));
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
        panel.add(new JLabel("Active:"), gbc);
        
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
                CourseTemplateItem selectedTemplate = (CourseTemplateItem) templateComboBox.getSelectedItem();
                int templateId = selectedTemplate.getId();
                
                // Create course object
                Course course = new Course();
                course.setName(name);
                course.setActive(activeCheckBox.isSelected());
                
                if (templateId != -1) {
                    // Get the template from database
                    CourseTemplateDAO templateDAO = new CourseTemplateDAO();
                    CourseTemplate template = templateDAO.read(templateId);
                    course.setCourseTemplate(template);
                    course.setCourseTemplateId(templateId);
                }
                
                // Save course to database
                CourseDAO courseDAO = CourseDAO.getInstance();
                courseDAO.create(course);
                
                // Reload course data
                loadCourseData(courseModel);
                
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
        dialog.setVisible(true);
    }

    private void editSelectedCourse(JTable courseTable, DefaultTableModel courseModel) {
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
            JDialog dialog = new JDialog(this, "Edit Course", true);
            dialog.setSize(450, 300);
            dialog.setLocationRelativeTo(this);
            
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);
            
            // Course ID field (display only)
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Course ID:"), gbc);
            
            gbc.gridx = 1;
            JLabel idLabel = new JLabel(String.valueOf(course.getId()));
            panel.add(idLabel, gbc);
            
            // Course name field
            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(new JLabel("Course Name:"), gbc);
            
            gbc.gridx = 1;
            JTextField nameField = new JTextField(course.getName(), 20);
            panel.add(nameField, gbc);
            
            // Course template selection
            gbc.gridx = 0;
            gbc.gridy = 2;
            panel.add(new JLabel("Course Template:"), gbc);
            
            gbc.gridx = 1;
            JComboBox<CourseTemplateItem> templateComboBox = new JComboBox<>();
            // Add a "None" option
            templateComboBox.addItem(new CourseTemplateItem(-1, "None"));
            
            // Get course templates from database
            CourseTemplateDAO templateDAO = new CourseTemplateDAO();
            List<CourseTemplate> templates = templateDAO.readAll();
            int selectedIndex = 0;
            
            for (int i = 0; i < templates.size(); i++) {
                CourseTemplate template = templates.get(i);
                templateComboBox.addItem(new CourseTemplateItem(template.getId(), template.getName()));
                
                if (course.getCourseTemplateId() == template.getId()) {
                    selectedIndex = i + 1; // +1 because of the "None" option
                }
            }
            
            templateComboBox.setSelectedIndex(selectedIndex);
            panel.add(templateComboBox, gbc);
            
            // Active checkbox
            gbc.gridx = 0;
            gbc.gridy = 3;
            panel.add(new JLabel("Active:"), gbc);
            
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
                    CourseTemplateItem selectedTemplate = (CourseTemplateItem) templateComboBox.getSelectedItem();
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
                    loadCourseData(courseModel);
                    
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
            dialog.setVisible(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error editing course: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteSelectedCourse(JTable courseTable, DefaultTableModel courseModel) {
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
            loadCourseData(courseModel);
            
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
    
    private void viewCourseDetails(JTable courseTable) {
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
            JDialog dialog = new JDialog(this, "Course Details - " + course.getName(), true);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(this);
            
            // Create tabbed pane for different details
            JTabbedPane detailsTabs = new JTabbedPane();
            
            // Course info panel
            JPanel infoPanel = new JPanel(new BorderLayout(10, 10));
            infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Basic info section
            JPanel basicInfoPanel = new JPanel(new GridLayout(4, 2, 10, 10));
            basicInfoPanel.setBorder(BorderFactory.createTitledBorder("Basic Information"));
            
            basicInfoPanel.add(new JLabel("Course ID:"));
            basicInfoPanel.add(new JLabel(String.valueOf(course.getId())));
            
            basicInfoPanel.add(new JLabel("Course Name:"));
            basicInfoPanel.add(new JLabel(course.getName()));
            
            basicInfoPanel.add(new JLabel("Template:"));
            String templateName = course.getCourseTemplate() != null ? 
                                  course.getCourseTemplate().getName() : "None";
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
            String[] studentColumns = {"ID", "Name", "Email"};
            DefaultTableModel studentModel = new DefaultTableModel(studentColumns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            
            for (User student : students) {
                studentModel.addRow(new Object[]{
                    student.getId(),
                    student.getName(),
                    student.getEmail()
                });
            }
            
            JTable studentTable = new JTable(studentModel);
            JScrollPane studentScrollPane = new JScrollPane(studentTable);
            studentsPanel.add(studentScrollPane, BorderLayout.CENTER);
            
            infoPanel.add(studentsPanel, BorderLayout.CENTER);
            
            // Add info panel to tabs
            detailsTabs.addTab("Information", infoPanel);
            
            // Assignments panel
            JPanel assignmentsPanel = new JPanel(new BorderLayout(10, 10));
            assignmentsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Get assignments for this course
            List<Assignment> assignments = courseDAO.getAssignmentsForCourse(course.getId());
            
            // Create table for assignments
            String[] assignmentColumns = {"ID", "Name", "Type", "Due Date", "Max Points"};
            DefaultTableModel assignmentModel = new DefaultTableModel(assignmentColumns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            
            for (Assignment assignment : assignments) {
                assignmentModel.addRow(new Object[]{
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
            
            dialog.setVisible(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error viewing course details: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createSystemSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create tabbed pane for different settings
        JTabbedPane settingsTabs = new JTabbedPane();
        
        // Database settings
        JPanel dbPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel dbInfoPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        dbInfoPanel.setBorder(BorderFactory.createTitledBorder("Database Information"));
        
        dbInfoPanel.add(new JLabel("Database Location:"));
        dbInfoPanel.add(new JLabel("./data/database.db"));
        
        dbInfoPanel.add(new JLabel("Connection Status:"));
        JLabel connectionLabel = new JLabel("Connected");
        connectionLabel.setForeground(Color.GREEN.darker());
        dbInfoPanel.add(connectionLabel);
        
        dbInfoPanel.add(new JLabel("Schema Version:"));
        dbInfoPanel.add(new JLabel("1.0"));
        
        dbPanel.add(dbInfoPanel, BorderLayout.NORTH);
        
        JPanel dbActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backupButton = new JButton("Backup Database");
        JButton resetButton = new JButton("Reset Database");
        
        dbActionsPanel.add(backupButton);
        dbActionsPanel.add(resetButton);
        
        dbPanel.add(dbActionsPanel, BorderLayout.CENTER);
        
        // App settings
        JPanel appPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel appSettingsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        appSettingsPanel.setBorder(BorderFactory.createTitledBorder("Application Settings"));
        
        appSettingsPanel.add(new JLabel("Default File Upload Path:"));
        JTextField uploadPathField = new JTextField("./uploads/");
        appSettingsPanel.add(uploadPathField);
        
        appSettingsPanel.add(new JLabel("Max Upload Size (MB):"));
        JTextField maxUploadField = new JTextField("50");
        appSettingsPanel.add(maxUploadField);
        
        appSettingsPanel.add(new JLabel("Session Timeout (minutes):"));
        JTextField timeoutField = new JTextField("30");
        appSettingsPanel.add(timeoutField);
        
        appSettingsPanel.add(new JLabel("Enable Debug Logging:"));
        JCheckBox debugCheckbox = new JCheckBox();
        appSettingsPanel.add(debugCheckbox);
        
        appPanel.add(appSettingsPanel, BorderLayout.NORTH);
        
        JButton saveSettingsButton = new JButton("Save Settings");
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        savePanel.add(saveSettingsButton);
        appPanel.add(savePanel, BorderLayout.SOUTH);
        
        // Add panels to tabs
        settingsTabs.addTab("Database", dbPanel);
        settingsTabs.addTab("Application", appPanel);
        
        panel.add(settingsTabs, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadUserData() {
        // Clear existing data
        tableModel.setRowCount(0);
        
        // Get all users from the database
        List<User> users = admin.getAllUsers();
        
        // Add users to table
        for (User user : users) {
            Object[] rowData = {
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().toString(),
                user.getCreatedAt(),
                user.getLastUpdated()
            };
            tableModel.addRow(rowData);
        }
    }
    
    private void filterUsersByRole(String selectedRole) {
        if (selectedRole.equals("All Roles")) {
            loadUserData();
            return;
        }
        
        // Clear existing data
        tableModel.setRowCount(0);
        
        // Get all users from the database
        List<User> users = admin.getAllUsers();
        
        // Filter and add users to table
        for (User user : users) {
            if (user.getRole().toString().equals(selectedRole)) {
                Object[] rowData = {
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().toString(),
                    user.getCreatedAt(),
                    user.getLastUpdated()
                };
                tableModel.addRow(rowData);
            }
        }
    }
    
    private void searchUsers(String searchTerm) {
        // Clear existing data
        tableModel.setRowCount(0);
        
        // Get all users from the database
        List<User> users = admin.getAllUsers();
        
        // Filter and add users to table
        searchTerm = searchTerm.toLowerCase();
        for (User user : users) {
            if (user.getName().toLowerCase().contains(searchTerm) || 
                user.getEmail().toLowerCase().contains(searchTerm)) {
                Object[] rowData = {
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().toString(),
                    user.getCreatedAt(),
                    user.getLastUpdated()
                };
                tableModel.addRow(rowData);
            }
        }
    }
    
    private void showAddUserDialog() {
        // Show dialog to add a new user
        JDialog dialog = new JDialog(this, "Add New User", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        panel.add(nameField, gbc);
        
        // Email field
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);
        
        gbc.gridx = 1;
        JTextField emailField = new JTextField(20);
        panel.add(emailField, gbc);
        
        // Password field
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);
        
        // Role dropdown
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Role:"), gbc);
        
        gbc.gridx = 1;
        String[] roles = {"STUDENT", "GRADER", "TEACHER", "ADMIN"};
        JComboBox<String> roleComboBox = new JComboBox<>(roles);
        panel.add(roleComboBox, gbc);
        
        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 4;
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
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            String roleStr = (String) roleComboBox.getSelectedItem();
            
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                    "All fields are required", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Hash the password
            String passwordHash = Hasher.hashPassword(password);
            
            // Create appropriate user based on role
            User newUser = null;
            User.Role role = User.Role.valueOf(roleStr);
            
            try {
                switch (role) {
                    case STUDENT:
                        newUser = new Student(name, email, passwordHash);
                        break;
                    case GRADER:
                        newUser = new Grader(name, email, passwordHash);
                        break;
                    case TEACHER:
                        newUser = new Teacher(name, email, passwordHash);
                        break;
                    case ADMIN:
                        newUser = Admin.getInstance(name, email, passwordHash);
                        break;
                }
                
                // Add the user to the database
                admin.addUser(newUser);
                
                // Refresh table
                loadUserData();
                
                // Close dialog
                dialog.dispose();
                
                JOptionPane.showMessageDialog(this, 
                    "User added successfully", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Error adding user: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Cancel button action
        cancelButton.addActionListener(e -> dialog.dispose());
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void editSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a user to edit", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Convert row index from view to model in case of sorting
        int modelRow = userTable.convertRowIndexToModel(selectedRow);
        
        // Get user ID from the table
        int userId = (int) tableModel.getValueAt(modelRow, 0);
        
        // Get user from database
        User user = admin.getUser(userId);
        if (user == null) {
            JOptionPane.showMessageDialog(this, 
                "User not found in database", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create edit dialog
        JDialog dialog = new JDialog(this, "Edit User", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // ID field (display only)
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("ID:"), gbc);
        
        gbc.gridx = 1;
        JLabel idLabel = new JLabel(String.valueOf(user.getId()));
        panel.add(idLabel, gbc);
        
        // Name field
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Name:"), gbc);
        
        gbc.gridx = 1;
        JTextField nameField = new JTextField(user.getName(), 20);
        panel.add(nameField, gbc);
        
        // Email field
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Email:"), gbc);
        
        gbc.gridx = 1;
        JTextField emailField = new JTextField(user.getEmail(), 20);
        panel.add(emailField, gbc);
        
        // Role field (display only)
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Role:"), gbc);
        
        gbc.gridx = 1;
        JLabel roleLabel = new JLabel(user.getRole().toString());
        panel.add(roleLabel, gbc);
        
        // Change password checkbox
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JCheckBox changePasswordCheckbox = new JCheckBox("Change Password");
        panel.add(changePasswordCheckbox, gbc);
        
        // Password fields (initially invisible)
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        JLabel passwordLabel = new JLabel("New Password:");
        passwordLabel.setVisible(false);
        panel.add(passwordLabel, gbc);
        
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setVisible(false);
        panel.add(passwordField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setVisible(false);
        panel.add(confirmLabel, gbc);
        
        gbc.gridx = 1;
        JPasswordField confirmField = new JPasswordField(20);
        confirmField.setVisible(false);
        panel.add(confirmField, gbc);
        
        // Created date (display only)
        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(new JLabel("Created:"), gbc);
        
        gbc.gridx = 1;
        JLabel createdLabel = new JLabel(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "N/A");
        panel.add(createdLabel, gbc);
        
        // Last updated (display only)
        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(new JLabel("Last Updated:"), gbc);
        
        gbc.gridx = 1;
        JLabel updatedLabel = new JLabel(user.getLastUpdated() != null ? user.getLastUpdated().toString() : "N/A");
        panel.add(updatedLabel, gbc);
        
        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancelButton = new JButton("Cancel");
        JButton saveButton = new JButton("Save Changes");
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        panel.add(buttonPanel, gbc);
        
        // Add change password checkbox listener
        changePasswordCheckbox.addActionListener(e -> {
            boolean selected = changePasswordCheckbox.isSelected();
            passwordLabel.setVisible(selected);
            passwordField.setVisible(selected);
            confirmLabel.setVisible(selected);
            confirmField.setVisible(selected);
            
            // Resize the dialog to ensure all components are visible
            dialog.pack();
        });
        
        // Save button action
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            
            if (name.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                    "Name and email are required", 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // Check if password is being changed
                if (changePasswordCheckbox.isSelected()) {
                    String password = new String(passwordField.getPassword());
                    String confirm = new String(confirmField.getPassword());
                    
                    if (password.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, 
                            "Password cannot be empty", 
                            "Validation Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    if (!password.equals(confirm)) {
                        JOptionPane.showMessageDialog(dialog, 
                            "Passwords do not match", 
                            "Validation Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // Hash the password
                    user.setPasswordHash(Hasher.hashPassword(password));
                }
                
                // Update user data
                user.setName(name);
                user.setEmail(email);
                
                // Update user in database
                admin.editUser(user);
                
                // Refresh table
                loadUserData();
                
                JOptionPane.showMessageDialog(dialog, 
                    "User updated successfully", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Error updating user: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Cancel button action
        cancelButton.addActionListener(e -> dialog.dispose());
        
        dialog.add(panel);
        dialog.pack(); // Adjust size to content
        dialog.setVisible(true);
    }
    
    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a user to delete", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Convert row index from view to model in case of sorting
        int modelRow = userTable.convertRowIndexToModel(selectedRow);
        
        // Get user ID from the table
        int userId = (int) tableModel.getValueAt(modelRow, 0);
        
        // Prevent deleting the currently logged-in admin
        if (userId == admin.getId()) {
            JOptionPane.showMessageDialog(this, 
                "You cannot delete your own account while logged in", 
                "Operation Not Allowed", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get user from database
        User user = admin.getUser(userId);
        if (user == null) {
            JOptionPane.showMessageDialog(this, 
                "User not found in database", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete user '" + user.getName() + "' (" + user.getEmail() + ")?\n" +
            "This action cannot be undone.", 
            "Confirm Deletion", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        try {
            // Delete user from database
            admin.deleteUser(user);
            
            // Refresh table
            loadUserData();
            
            JOptionPane.showMessageDialog(this, 
                "User deleted successfully", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error deleting user: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void logout() {
        // Confirm logout
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to logout?", 
            "Confirm Logout", 
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            // Reset the Admin singleton instance
            Admin.resetInstance();
            
            // Close current window and open login window
            dispose();
            SwingUtilities.invokeLater(() -> {
                new LoginFrame().setVisible(true);
            });
        }
    }

    private class CourseTemplateItem {
        private int id;
        private String name;
        
        public CourseTemplateItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}
