import java.util.*;
import javax.swing.*;

import db.DBSetup;
import db.UserDAO;
import model.Admin;
import model.User;
import ui.LoginFrame;
import utils.Hasher;

// Main application class
public class GradingSystemApp {
    public void run() {
        // Initialize database
        DBSetup.createTables();
        
        // Check if any users exist, if not create default admin
        UserDAO userDAO = UserDAO.getInstance();
        List<User> users = userDAO.readAll();
        
        if (users.isEmpty()) {
            // Create default admin user
            Admin admin = Admin.getInstance("Administrator", "admin@bu.edu", Hasher.hashPassword("admin123"));
            userDAO.create(admin);
            
            // Notify about default credentials
            JOptionPane.showMessageDialog(null, 
                "Default admin account created:\nEmail: admin@bu.edu\nPassword: admin123\n\nPlease change this password after logging in.", 
                "First Run Setup", 
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        // Start the login window
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new LoginFrame().setVisible(true);
        });
    }
}