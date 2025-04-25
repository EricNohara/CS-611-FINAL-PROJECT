package ui.dashboard;

import javax.swing.*;

import java.awt.*;



import model.Teacher;

import ui.dashboard.panels.OverviewPanel;
import ui.dashboard.panels.StudentsPanel;
import ui.dashboard.panels.CoursesPanel;
import ui.dashboard.panels.TemplatesPanel;
import ui.LoginFrame;
import ui.dashboard.panels.AssignmentsPanel;
import ui.dashboard.panels.GradingPanel;


public class TeacherDashboard extends JFrame {
    private Teacher teacher;
    private JTabbedPane tabbedPane;

    public TeacherDashboard(Teacher teacher) {
        this.teacher = teacher;

        setTitle("Teacher Dashboard - " + teacher.getName());
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create header panel with welcome message and logout button
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, " + teacher.getName() + "!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        JButton logoutButton = new JButton("Logout");
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutPanel.add(logoutButton);
        headerPanel.add(logoutPanel, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Create tabbed pane for different teacher functions
        tabbedPane = new JTabbedPane();

        // Dashboard overview panel
        tabbedPane.addTab("Overview", new OverviewPanel(teacher));

        // Courses panel
        tabbedPane.addTab("Courses", new CoursesPanel(teacher, tabbedPane));

        // Course templates panel
        tabbedPane.addTab("Course Templates", new TemplatesPanel(teacher, tabbedPane));

        // Assignments panel
        tabbedPane.addTab("Assignments", new AssignmentsPanel(teacher, tabbedPane));

        // Grading panel
        tabbedPane.addTab("Grading", new GradingPanel(teacher, tabbedPane));

        // Students panel
        tabbedPane.addTab("Students", new StudentsPanel(teacher, tabbedPane));

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Ready");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

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