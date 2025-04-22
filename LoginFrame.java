import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    private UserDAO userDAO;
    
    public LoginFrame() {
        // Initialize UserDAO
        userDAO = UserDAO.getInstance();
        
        // Set up the frame
        setTitle("Login");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Create components
        JLabel emailLabel = new JLabel("Email:");
        JLabel passwordLabel = new JLabel("Password:");
        
        emailField = new JTextField(20);
        passwordField = new JPasswordField(20);
        
        loginButton = new JButton("Login");
        cancelButton = new JButton("Cancel");
        
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