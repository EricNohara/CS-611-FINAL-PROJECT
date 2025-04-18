import javax.swing.*;
import java.awt.*;

class LoginFrame extends JFrame {
    public LoginFrame() {
        setTitle("Grading System Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2));
        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField();
        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField();
        JButton loginButton = new JButton("Login");

        panel.add(userLabel);
        panel.add(userField);
        panel.add(passLabel);
        panel.add(passField);
        panel.add(new JLabel());
        panel.add(loginButton);

        add(panel);

        loginButton.addActionListener(e -> {
            // Role-based navigation (hardcoded teacher for now)
            String username = userField.getText();
            String password = new String(passField.getPassword());

            // CHANGE THIS LATER - HARDCODED RIGHT NOW
            if (username.equals("teacher") && password.equals("1234")) {
                dispose();
                new DashboardFrame("Teacher");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials");
            }
        });

        setVisible(true);
    }
}