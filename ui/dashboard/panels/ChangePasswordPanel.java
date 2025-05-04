package ui.dashboard.panels;

import model.User;
import utils.Hasher;

import javax.swing.*;

import db.UserDAO;

import java.awt.*;

public class ChangePasswordPanel extends JPanel {
    private final User user;

    public ChangePasswordPanel(User user) {
        super(new BorderLayout(10, 10));
        this.user = user;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    private void buildUI() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel currentPassLabel = new JLabel("Current Password:");
        JLabel newPassLabel = new JLabel("New Password:");
        JLabel confirmPassLabel = new JLabel("Confirm New Password:");

        JPasswordField currentPassField = new JPasswordField(20);
        JPasswordField newPassField = new JPasswordField(20);
        JPasswordField confirmPassField = new JPasswordField(20);

        JButton changeBtn = new JButton("Change Password");

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(currentPassLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(currentPassField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(newPassLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(newPassField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(confirmPassLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(confirmPassField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        formPanel.add(changeBtn, gbc);

        add(formPanel, BorderLayout.CENTER);

        changeBtn.addActionListener(e -> {
            String currentPass = new String(currentPassField.getPassword());
            String newPass = new String(newPassField.getPassword());
            String confirmPass = new String(confirmPassField.getPassword());

            if (!Hasher.checkPassword(currentPass, user.getPasswordHash())) {
                JOptionPane.showMessageDialog(this, "Incorrect current password.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (newPass.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            user.setPasswordHash(Hasher.hashPassword(newPass));
            UserDAO.getInstance().update(user);
            JOptionPane.showMessageDialog(this, "Password successfully changed!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            currentPassField.setText("");
            newPassField.setText("");
            confirmPassField.setText("");
        });
    }
}
