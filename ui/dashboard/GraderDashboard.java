package ui.dashboard;

import model.Grader;
import ui.LoginFrame;
import ui.dashboard.panels.*;

import javax.swing.*;
import java.awt.*;


public class GraderDashboard extends JFrame {
    private Grader grader;
    private JTabbedPane tabbedPane;

    public GraderDashboard(Grader grader) {
        this.grader = grader;

        setTitle("Grader Dashboard - " + grader.getName());
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create header panel with welcome message and logout button
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, " + grader.getName() + "!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        //refresh
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());




        JButton logoutButton = new JButton("Logout");

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.add(refreshButton);
        topRightPanel.add(logoutButton);
        headerPanel.add(topRightPanel, BorderLayout.EAST);
        // Create tabbed pane for different teacher functions
        tabbedPane = new JTabbedPane();

        // Dashboard overview panel
        tabbedPane.addTab("Overview", new OverviewPanel(grader));

        // Courses panel
        tabbedPane.addTab("Courses", new CoursesPanel(grader, tabbedPane));

        // Assignments panel
        tabbedPane.addTab("Assignments", new AssignmentsPanel(grader, tabbedPane));

        // Grading panel
        tabbedPane.addTab("Grading", new GradingPanel(grader, tabbedPane));


        //refresh
        tabbedPane.addChangeListener(e -> {
            Component selected = tabbedPane.getSelectedComponent();
            if (selected != null && selected instanceof Refreshable) {
                ((Refreshable) selected).refresh();
            }
        });

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
    private void refresh() {
        int tabCount = tabbedPane.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            Component tab = tabbedPane.getComponentAt(i);
            if (tab instanceof Refreshable) {
                ((Refreshable) tab).refresh();
            }
        }
    }

}