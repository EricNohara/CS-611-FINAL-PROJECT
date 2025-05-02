package ui;

import java.awt.*;

import javax.swing.JLabel;

public class UIConstants {
    public static final int SMALL_FONT_SIZE = 18;
    public static final int FONT_SIZE = 24;
    public static final int LARGE_FONT_SIZE = 30;
    public static final int TXT_INPUT_PAD_X = 10;
    public static final int TXT_INPUT_PAD_Y = 6;
    public static final int PANEL_PAD = 20;
    public static final int TABLE_CELL_PAD = 8;

    public static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, FONT_SIZE);
    public static final Font DEFAULT_FONT_BOLD = new Font("SansSerif", Font.BOLD, FONT_SIZE);
    public static final Font LARGE_FONT_BOLD = new Font("SansSerif", Font.BOLD, LARGE_FONT_SIZE);
    public static final Font SMALL_FONT_ITALIC = new Font("SansSerif", Font.ITALIC, SMALL_FONT_SIZE);

    public static JLabel getBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConstants.DEFAULT_FONT_BOLD);
        return label;
    }
}
