package ui.dashboard.components;

import javax.swing.*;

import ui.UIConstants;

import java.awt.*;

// Reusable statistic card (title, big value, short description) used on the Overview tab.
public final class DashboardCard extends JPanel {

    public DashboardCard(String title, String value, String description) {
        super(new BorderLayout(5, 5));

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UIConstants.DEFAULT_FONT_BOLD);

        JLabel valueLbl = new JLabel(value, SwingConstants.CENTER);
        valueLbl.setFont(UIConstants.LARGE_FONT_BOLD);

        JLabel descLbl  = new JLabel(description);
        descLbl.setFont(UIConstants.SMALL_FONT_ITALIC);

        add(titleLbl, BorderLayout.NORTH);
        add(valueLbl, BorderLayout.CENTER);
        add(descLbl, BorderLayout.SOUTH);
    }
}
