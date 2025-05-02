package ui.utils;

import javax.swing.*;
import javax.swing.border.Border;

import ui.UIConstants;

public class InputPadding {
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
}
