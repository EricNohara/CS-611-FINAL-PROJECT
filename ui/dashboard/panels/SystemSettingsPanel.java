package ui.dashboard.panels;

import model.Admin;
import ui.utils.Padding;
import utils.DBUtils;

import javax.swing.*;

import org.omg.PortableInterceptor.SUCCESSFUL;

import java.awt.*;
import java.io.File;

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
        JButton loadBackupButton = new JButton("Load Backup");

        dbActionsPanel.add(backupButton);
        dbActionsPanel.add(resetButton);
        dbActionsPanel.add(loadBackupButton);

        dbPanel.add(dbActionsPanel, BorderLayout.CENTER);

        // App settings
        JPanel appPanel = new JPanel(new BorderLayout(10, 10));

        JPanel appSettingsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        appSettingsPanel.setBorder(BorderFactory.createTitledBorder("Application Settings"));

        appSettingsPanel.add(new JLabel("Default File Upload Path:"));
        JTextField uploadPathField = new JTextField("./uploads/");
        appSettingsPanel.add(uploadPathField);
        Padding.addInputPaddingDefault(uploadPathField);

        appSettingsPanel.add(new JLabel("Max Upload Size (MB):"));
        JTextField maxUploadField = new JTextField("50");
        appSettingsPanel.add(maxUploadField);
        Padding.addInputPaddingDefault(maxUploadField);

        appSettingsPanel.add(new JLabel("Session Timeout (minutes):"));
        JTextField timeoutField = new JTextField("30");
        appSettingsPanel.add(timeoutField);
        Padding.addInputPaddingDefault(timeoutField);

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

        resetButton.addActionListener(e -> handleResetDB());
        backupButton.addActionListener(e -> handleBackupDB());
        loadBackupButton.addActionListener(e -> handleLoadBackup());

        add(settingsTabs, BorderLayout.CENTER);
    }

    private void handleResetDB() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to reset the database? This action cannot be undone.",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            performDatabaseReset();
        }
    }

    private void performDatabaseReset() {
        boolean resetSuccessful = DBUtils.clearAllTables();

        if (resetSuccessful) {
            JOptionPane.showMessageDialog(
                this,
                "Database has been reset successfully.",
                "Reset Complete",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Failed to reset database",
                "Reset Failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
        
    }

    private void handleBackupDB() {
        boolean backupSuccessful = DBUtils.backupDatabase();

        if (backupSuccessful) {
            JOptionPane.showMessageDialog(
                this,
                "Backup completed successfully.",
                "Backup Successful",
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Failed to backup database",
                "Backup Failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void handleLoadBackup() {
        // Open file chooser to select a backup file
        JFileChooser fileChooser = new JFileChooser("./data/backups/");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Database files", "db"));

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile != null) {
                // Try loading the backup
                boolean loadSuccessful = DBUtils.loadBackup(selectedFile);

                if (loadSuccessful) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Backup loaded successfully. System will now exit to apply changes. Please restart application.",
                        "Load Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    System.exit(0);
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to load the selected backup.",
                        "Load Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
    }
}