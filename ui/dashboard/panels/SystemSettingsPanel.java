package ui.dashboard.panels;

import model.Admin;
import javax.swing.*;
import java.awt.*;

// System settings panel
public final class SystemSettingsPanel extends JPanel {

    private final Admin admin;

    public SystemSettingsPanel(Admin admin) {
        super(new BorderLayout(10, 10));
        this.admin = admin;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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

        add(settingsTabs, BorderLayout.CENTER);
    }
}