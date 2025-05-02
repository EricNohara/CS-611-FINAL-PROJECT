package ui.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import ui.UIConstants;

import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PaddedCellRenderer extends DefaultTableCellRenderer {
    private final int padding;

    public PaddedCellRenderer(int padding) {
        this.padding = padding;
        setOpaque(true); // Required for background color to show
        setVerticalAlignment(SwingConstants.CENTER); // Vertically center the text
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        label.setBorder(new EmptyBorder(padding, padding, padding, padding));
        label.setVerticalAlignment(SwingConstants.CENTER); // Ensure vertical centering each time
        return label;
    }

    public static void setDefaultRowHeight (JTable table) {
        table.setRowHeight(table.getRowHeight() + 2 * UIConstants.TABLE_CELL_PAD);
    }
}
