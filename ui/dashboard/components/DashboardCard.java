package ui.dashboard.components;

import javax.swing.*;
import java.awt.*;

// Reusable statistic card (title, big value, short description) used on the Overview tab.
public final class DashboardCard extends JPanel {

    public DashboardCard(String title, String value, String description) {
        super(new BorderLayout(5, 5));

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel valueLbl = new JLabel(value, SwingConstants.CENTER);
        valueLbl.setFont(new Font("Arial", Font.BOLD, 36));

        JLabel descLbl  = new JLabel(description);
        descLbl.setFont(new Font("Arial", Font.PLAIN, 12));

        add(titleLbl , BorderLayout.NORTH);
        add(valueLbl , BorderLayout.CENTER);
        add(descLbl  , BorderLayout.SOUTH);
    }
}
