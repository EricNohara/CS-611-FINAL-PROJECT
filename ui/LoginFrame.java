package ui;
import javax.swing.*;

import db.DBSetup;
import db.UserDAO;
import model.Admin;
import model.Grader;
import model.Student;
import model.Teacher;
import model.User;
import ui.dashboard.TeacherDashboard;
import ui.dashboard.AdminDashboard;
import ui.dashboard.GraderDashboard;
import ui.dashboard.StudentDashboard;

import java.awt.*;
import java.awt.event.*;

import utils.Hasher;

public class LoginFrame extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    private JButton signUpButton;
    private UserDAO userDAO;
    
    public LoginFrame() {
        // Initialize UserDAO
        userDAO = UserDAO.getInstance();
        
        // Set up the frame
        setTitle("Login");
        //setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Create components
        JLabel emailLabel = new JLabel("Email:");
        JLabel passwordLabel = new JLabel("Password:");
        
        emailField = new JTextField(20);
        passwordField = new JPasswordField(20);
        
        loginButton = new JButton("Login");
        cancelButton = new JButton("Cancel");
        signUpButton = new JButton("Sign Up");
        
        // Set up layout
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Email row
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(emailLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(emailField, gbc);
        
        // Password row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(passwordLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(passwordField, gbc);
        
        // Buttons row
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(signUpButton);
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);
        
        // Add listeners
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        signUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSignUpDialog();
            }
        });
        
        // Enter key for login
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        });
        
        // Add panel to frame
        add(panel);

        pack();

        setLocationRelativeTo(null);
        
        // Initialize database tables if needed
        DBSetup.createTables();
    }
    
    private void performLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both email and password", 
                "Login Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Attempt to retrieve user by email
        User user = userDAO.readByEmail(email);
        
        if (user != null && user.login(password)) {
            JOptionPane.showMessageDialog(this, 
                "Login successful! Welcome " + user.getName(), 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            
            // Open appropriate window based on user role
            openUserDashboard(user);
            
            // Close login window
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Invalid email or password", 
                "Login Failed", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSignUpDialog() {
        // Create the dialog
        JDialog signUpDialog = new JDialog(this, "Sign Up", true);
        //signUpDialog.setSize(400, 300);
        signUpDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
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
        JTextField newEmailField = new JTextField(20);
        panel.add(newEmailField, gbc);
        
        // Password field
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        JPasswordField newPasswordField = new JPasswordField(20);
        panel.add(newPasswordField, gbc);
        
        // Confirm password field
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Confirm Password:"), gbc);
        
        gbc.gridx = 1;
        JPasswordField confirmPasswordField = new JPasswordField(20);
        panel.add(confirmPasswordField, gbc);
        
        // Role selection
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Role:"), gbc);
        
        gbc.gridx = 1;
        String[] roles = {"Student", "Grader", "Teacher"};
        JComboBox<String> roleComboBox = new JComboBox<>(roles);
        panel.add(roleComboBox, gbc);
        
        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton createButton = new JButton("Create Account");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, gbc);
        
        // Create button action
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = nameField.getText().trim();
                String email = newEmailField.getText().trim();
                String password = new String(newPasswordField.getPassword());
                String confirmPassword = new String(confirmPasswordField.getPassword());
                String selectedRole = (String) roleComboBox.getSelectedItem();
                
                // Validate input
                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(signUpDialog, 
                        "All fields are required", 
                        "Validation Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (!password.equals(confirmPassword)) {
                    JOptionPane.showMessageDialog(signUpDialog, 
                        "Passwords do not match", 
                        "Validation Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Check if email already exists
                if (userDAO.readByEmail(email) != null) {
                    JOptionPane.showMessageDialog(signUpDialog, 
                        "Email already exists", 
                        "Validation Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Create user based on selected role
                User newUser = null;
                String hashedPassword = Hasher.hashPassword(password);
                
                switch (selectedRole) {
                    case "Student":
                        newUser = new Student(name, email, hashedPassword);
                        break;
                    case "Grader":
                        newUser = new Grader(name, email, hashedPassword);
                        break;
                    case "Teacher":
                        newUser = new Teacher(name, email, hashedPassword);
                        break;
                }
                
                // Save user to database
                if (newUser != null) {
                    userDAO.create(newUser);
                    
                    JOptionPane.showMessageDialog(signUpDialog, 
                        "Account created successfully. You can now log in.", 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    signUpDialog.dispose();
                    
                    // Pre-fill the login form with the new email
                    emailField.setText(email);
                    passwordField.setText("");
                }
            }
        });
        
        // Cancel button action
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                signUpDialog.dispose();
            }
        });
        
        signUpDialog.add(panel);
        signUpDialog.setVisible(true);
    }

    
    private void openUserDashboard(User user) {
        // Open appropriate window based on user role
        switch (user.getRole()) {
            case STUDENT:
                // Open student dashboard
                SwingUtilities.invokeLater(() -> {
                    StudentDashboard dashboard = new StudentDashboard((Student) user);
                    dashboard.setVisible(true);
                });
                break;
            case GRADER:
                // Open grader dashboard
                SwingUtilities.invokeLater(() -> {
                    GraderDashboard dashboard = new GraderDashboard((Grader) user);
                    dashboard.setVisible(true);
                });
                break;

            case TEACHER:
                // Open teacher dashboard
                SwingUtilities.invokeLater(() -> {
                    TeacherDashboard dashboard = new TeacherDashboard((Teacher) user);
                    dashboard.setVisible(true);
                });
                break;
                
            case ADMIN:
                // Open admin dashboard
                SwingUtilities.invokeLater(() -> {
                    AdminDashboard dashboard = new AdminDashboard((Admin) user);
                    dashboard.setVisible(true);
                });
                break;
        }
    }
    
    public static void main(String[] args) {
        // Run login window on EDT
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginWindow = new LoginFrame();
            loginWindow.setVisible(true);
        });
    }
}