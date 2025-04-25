package ui.dashboard;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import model.Admin;

import ui.LoginFrame;
import ui.dashboard.panels.CourseManagementPanel;
import ui.dashboard.panels.SystemSettingsPanel;
import ui.dashboard.panels.UserManagementPanel;

import java.awt.*;

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
        tabbedPane.addTab("User Management", new UserManagementPanel(admin));

        // Course management panel
        tabbedPane.addTab("Course Management", new CourseManagementPanel(admin));

        // System settings panel
        tabbedPane.addTab("System Settings", new SystemSettingsPanel(admin));

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

    private void logout() {
        // Confirm logout
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Close current window and open login window
            dispose();
            SwingUtilities.invokeLater(() -> {
                new LoginFrame().setVisible(true);
            });
        }
    }
}
