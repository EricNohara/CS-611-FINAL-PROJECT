package ui.dashboard.panels;

import db.*;
import model.*;
import ui.LoginFrame; // if the panel needs it for actions
import ui.utils.TemplateItem; // remove if unused
import utils.Hasher;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.util.List;

// User management panel
public final class UserManagementPanel extends JPanel {

    private final Admin admin;
    private final DefaultTableModel tableModel;
    private final JTable userTable;

    public UserManagementPanel(Admin admin) {
        super(new BorderLayout(10, 10));
        this.admin = admin;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top row buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add User");
        JButton editBtn = new JButton("Edit User");
        JButton delBtn = new JButton("Delete User");
        JButton refreshBtn = new JButton("Refresh");
        buttonRow.add(addBtn);
        buttonRow.add(editBtn);
        buttonRow.add(delBtn);
        buttonRow.add(refreshBtn);
        add(buttonRow, BorderLayout.NORTH);

        // Filter and search
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Role:"));

        String[] roles = { "All Roles", "STUDENT", "GRADER", "TEACHER", "ADMIN" };
        JComboBox<String> roleFilter = new JComboBox<>(roles);
        filterPanel.add(roleFilter);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Search:"));

        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

        /* combine the two small panels */
        JPanel filterSearch = new JPanel(new BorderLayout());
        filterSearch.add(filterPanel, BorderLayout.WEST);
        filterSearch.add(searchPanel, BorderLayout.EAST);

        add(filterSearch, BorderLayout.SOUTH);

        roleFilter.addActionListener(e -> {
            String selectedRole = (String) roleFilter.getSelectedItem();
            filterUsersByRole(selectedRole);
        });

        searchBtn.addActionListener(e -> {
            String term = searchField.getText().trim();
            if (term.isEmpty())
                loadUserData();
            else
                searchUsers(term);
        });

        // Table
        String[] cols = { "ID", "Name", "Email", "Role", "Created", "Last Updated" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return (c == 0) ? Integer.class
                        : (c == 4 || c == 5) ? Timestamp.class
                                : String.class;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setAutoCreateRowSorter(true);
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Listeners
        addBtn.addActionListener(e -> showAddUserDialog());
        editBtn.addActionListener(e -> editSelectedUser());
        delBtn.addActionListener(e -> deleteSelectedUser());
        refreshBtn.addActionListener(e -> loadUserData());

        loadUserData();
    }

    // Helpers

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
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add New User",
                Dialog.ModalityType.APPLICATION_MODAL);
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
        String[] roles = { "STUDENT", "GRADER", "TEACHER" };
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
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit User",
                Dialog.ModalityType.APPLICATION_MODAL);
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
}
