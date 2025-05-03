package ui.dashboard;

import model.Student;
import ui.LoginFrame;
import ui.UIConstants;
import ui.dashboard.panels.ChangePasswordPanel;
import ui.dashboard.panels.Refreshable;
import ui.dashboard.panels.StudentAssignmentsPanel;
import ui.dashboard.panels.StudentCoursesPanel;
import ui.utils.Padding;

import javax.swing.*;
import java.awt.*;

public class StudentDashboard extends JFrame {
    private final Student student;
    private final JTabbedPane tabbedPane;

    public StudentDashboard(Student student) {
        this.student = student;

        setTitle("Student Dashboard - " + student.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        Padding.addPanelPaddingDefault(mainPanel);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, " + student.getName() + "!");
        welcomeLabel.setFont(UIConstants.LARGE_FONT_BOLD);
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        JButton refreshButton = new JButton("Refresh");
        JButton logoutButton = new JButton("Logout");
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.add(refreshButton);
        topRightPanel.add(logoutButton);
        headerPanel.add(topRightPanel, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();

        // Enroll and view courses
        tabbedPane.addTab("Courses", new StudentCoursesPanel(student));

        // View and submit assignments
        tabbedPane.addTab("Assignments", new StudentAssignmentsPanel(student));

        tabbedPane.addTab("Change Password", new ChangePasswordPanel(student));

        tabbedPane.addChangeListener(e -> {
            Component selected = tabbedPane.getSelectedComponent();
            if (selected instanceof Refreshable) {
                ((Refreshable) selected).refresh();
            }
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Ready");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        logoutButton.addActionListener(e -> logout());
        refreshButton.addActionListener(e -> refresh());

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
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
