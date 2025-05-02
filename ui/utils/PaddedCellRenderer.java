package ui.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import ui.UIConstants;

import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PaddedCellRenderer extends DefaultTableCellRenderer {
    private static final int padding = UIConstants.TABLE_CELL_PAD;

    public PaddedCellRenderer() {
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

    public void applyCellPadding(JTable table) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(this);
        }
    }
}
