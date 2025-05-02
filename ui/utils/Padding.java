package ui.utils;

import javax.swing.*;
import javax.swing.border.Border;

import ui.UIConstants;

public class Padding {
    public static void addInputPadding(JComponent component, int padY, int padX) {
        Border original = component.getBorder();
        Border padding = BorderFactory.createEmptyBorder(padY, padX, padY, padX);
        component.setBorder(BorderFactory.createCompoundBorder(original, padding));
    }

    public static void addInputPaddingDefault(JComponent component) {
        Border original = component.getBorder();
        int padX = UIConstants.TXT_INPUT_PAD_X, padY = UIConstants.TXT_INPUT_PAD_Y;
        Border padding = BorderFactory.createEmptyBorder(padY, padX, padY, padX);
        component.setBorder(BorderFactory.createCompoundBorder(original, padding));
    }

    public static void addPanelPadding(JPanel panel, int padTop, int padLeft, int padBottom, int padRight) {
        panel.setBorder(BorderFactory.createEmptyBorder(padTop, padLeft, padBottom, padRight));
    }

    public static void addPanelPaddingDefault(JPanel panel) {
        int pad = UIConstants.PANEL_PAD;
        panel.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
    }
}
