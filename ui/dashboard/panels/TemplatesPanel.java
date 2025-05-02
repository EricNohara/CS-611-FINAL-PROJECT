package ui.dashboard.panels;

import db.*;
import model.*;
import ui.UIConstants;
import ui.utils.PaddedCellRenderer;
import ui.utils.Padding;
import ui.utils.TemplateItem;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// Courses templates
public final class TemplatesPanel extends JPanel implements Refreshable {

    private final User teacher;
    private final JTabbedPane parentTabs;

    // widgets we need to touch from helpers
    private DefaultListModel<String> templateListModel;
    private JList<String> templateJList;

    public TemplatesPanel(User teacher, JTabbedPane parentTabs) {
        super(new BorderLayout(10, 10));
        this.teacher = teacher;
        this.parentTabs = parentTabs;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUI();
    }

    // UI construction 
    private void buildUI() {
        // --- Top button row ---
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newBtn = new JButton("New Template");
        JButton edtBtn = new JButton("Edit Template");
        JButton delBtn = new JButton("Delete Template");
        btns.add(newBtn);
        btns.add(edtBtn);
        btns.add(delBtn);
        add(btns, BorderLayout.NORTH);

        // --- Split-pane: list (left) & detail (right) ---
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(300);

        // left list
        templateListModel = new DefaultListModel<>();
        templateJList = new JList<>(templateListModel);
        templateJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        split.setLeftComponent(new JScrollPane(templateJList));

        // right detail panel will be rebuilt in updateTemplateDetails()
        setPlaceholderTemplateDetails(split);

        add(split, BorderLayout.CENTER);

        loadTemplateNames(); // fill list once

        /* ----- wire button + list listeners ----- */
        newBtn.addActionListener(e -> createNewTemplate());
        edtBtn.addActionListener(e -> editSelectedTemplate());
        delBtn.addActionListener(e -> deleteSelectedTemplate(split));

        templateJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && templateJList.getSelectedIndex() != -1)
                showTemplateDetails(templateJList.getSelectedValue(), split);
        });
    }

    // Helpers
    private void setPlaceholderTemplateDetails(JSplitPane split) {
        JPanel placeholder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        placeholder.add(new JLabel("Select a template to view details"));
        split.setRightComponent(placeholder);
    }

    // table/list reload helpers
    private void loadTemplateNames() {
        templateListModel.clear();
        CourseTemplateDAO dao = CourseTemplateDAO.getInstance();
        dao.readAll().forEach(t -> templateListModel.addElement(t.getName()));
    }

    private void createNewTemplate() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Create New Template",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        Padding.addPanelPaddingDefault(panel);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Template name
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(UIConstants.getBoldLabel("Template Name:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField templateNameField = new JTextField(30);
        formPanel.add(templateNameField, gbc);
        Padding.addInputPaddingDefault(templateNameField);

        // Assignment templates
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        formPanel.add(UIConstants.getBoldLabel("Assignment Types:"), gbc);

        // Assignment types table
        String[] assignmentColumns = { "Type", "Weight", "Count", "Submission Types" };
        DefaultTableModel assignmentModel = new DefaultTableModel(assignmentColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // Make "Type" column (index 0) non-editable
            }
        };

        // Add default rows
        assignmentModel.addRow(new Object[] { "HOMEWORK", "40", "10", "pdf, docx, java" });
        assignmentModel.addRow(new Object[] { "QUIZ", "20", "5", "txt, pdf" });
        assignmentModel.addRow(new Object[] { "PROJECT", "20", "1", "zip, jar" });
        assignmentModel.addRow(new Object[] { "EXAM", "20", "1", "py, c" });

        JTable assignmentTable = new JTable(assignmentModel);
        JScrollPane assignmentScrollPane = new JScrollPane(assignmentTable);

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(assignmentTable);
        paddedRenderer.applyCellPadding(assignmentTable);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(assignmentScrollPane, gbc);

        // Buttons for assignment types
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel typeButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addTypeButton = new JButton("Add Type");
        JButton editTypeButton = new JButton("Edit Type");
        JButton deleteTypeButton = new JButton("Delete Type");
        typeButtonPanel.add(addTypeButton);
        typeButtonPanel.add(editTypeButton);
        typeButtonPanel.add(deleteTypeButton);

        formPanel.add(typeButtonPanel, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton createButton = new JButton("Create Template");
        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        addTypeButton.addActionListener(e -> addAssignmentType(assignmentModel));
        editTypeButton.addActionListener(e -> editAssignmentType(assignmentTable, assignmentModel));
        deleteTypeButton.addActionListener(e -> deleteAssignmentType(assignmentTable, assignmentModel));

        createButton.addActionListener(e -> {
            String templateName = templateNameField.getText();
            if (templateName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Template name is required",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate total weight is 100%
            double totalWeight = 0;
            for (int i = 0; i < assignmentModel.getRowCount(); i++) {
                totalWeight += Double.parseDouble(assignmentModel.getValueAt(i, 1).toString());
            }

            if (Math.abs(totalWeight - 100) > 0.01) {
                JOptionPane.showMessageDialog(dialog,
                        "Total weight must equal 100%",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Create a new course template
                CourseTemplate template = new CourseTemplate(templateName, new ArrayList<>());

                // Get assignment templates from the table
                List<AssignmentTemplate> assignmentTemplates = new ArrayList<>();

                for (int i = 0; i < assignmentModel.getRowCount(); i++) {
                    String typeStr = (String) assignmentModel.getValueAt(i, 0);
                    String weightStr = assignmentModel.getValueAt(i, 1).toString();
                    String countStr = assignmentModel.getValueAt(i, 2).toString();
                    String submissionTypesStr = (String) assignmentModel.getValueAt(i, 3);

                    Assignment.Type type = Assignment.Type.valueOf(typeStr);
                    double weight = Double.parseDouble(weightStr) / 100.0; // Convert from percentage to decimal
                    int count = Integer.parseInt(countStr);

                    // Parse submission types
                    List<String> submissionTypes = new ArrayList<>();
                    if (submissionTypesStr != null && !submissionTypesStr.isEmpty()) {
                        String[] types = submissionTypesStr.split(",");
                        for (String t : types) {
                            submissionTypes.add(t.trim());
                        }
                    }

                    // Create assignment template
                    // Note: Course template ID will be set after the course template is created
                    // We'll use -1 as a placeholder
                    AssignmentTemplate assignTemplate = new AssignmentTemplate(
                            -1, weight / count, type, submissionTypes);

                    // Create multiple templates based on count if needed
                    for (int j = 0; j < count; j++) {
                        assignmentTemplates.add(assignTemplate);
                    }
                }

                // Set assignment templates
                template.setAssignmentTemplates(assignmentTemplates);

                // Save to database using the CourseTemplateDAO
                CourseTemplateDAO templateDAO = CourseTemplateDAO.getInstance();
                templateDAO.create(template);

                // Show success message
                JOptionPane.showMessageDialog(dialog,
                        "Template created successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                // Close dialog
                dialog.dispose();

                // Refresh templates panel
                // For now, switch to another tab and back
                int currentIndex = parentTabs.getSelectedIndex();
                parentTabs.setSelectedIndex(currentIndex == 0 ? 1 : 0);
                parentTabs.setSelectedIndex(2); // Switch to Templates tab

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error creating template: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void editSelectedTemplate() {
        if (!(teacher instanceof Teacher)) {
            JOptionPane.showMessageDialog(this,
                    "You do not have permission to edit template.",
                    "Permission denied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Teacher tc = (Teacher) teacher;
        String selectedTemplateName = templateJList.getSelectedValue();
        if (selectedTemplateName == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a template to edit",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get the selected template from database
        CourseTemplateDAO templateDAO = CourseTemplateDAO.getInstance();
        List<CourseTemplate> allTemplates = templateDAO.readAll();
        CourseTemplate selectedTemplate = allTemplates.stream()
                .filter(t -> t.getName().equals(selectedTemplateName))
                .findFirst()
                .orElse(null);

        if (selectedTemplate == null) {
            JOptionPane.showMessageDialog(this,
                    "Template not found in database",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create edit dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Template",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        Padding.addPanelPaddingDefault(panel);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Template name
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(UIConstants.getBoldLabel("Template Name:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JTextField templateNameField = new JTextField(selectedTemplate.getName(), 30);
        formPanel.add(templateNameField, gbc);
        Padding.addInputPaddingDefault(templateNameField);

        // Assignment templates table
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        formPanel.add(UIConstants.getBoldLabel("Assignment Types:"), gbc);

        String[] assignmentColumns = { "Type", "Weight", "Count", "Submission Types" };
        DefaultTableModel assignmentModel = new DefaultTableModel(assignmentColumns, 0);

        // Group assignment templates by type for display
        Map<Assignment.Type, List<AssignmentTemplate>> typeGroups = selectedTemplate.getAssignmentTemplates().stream()
                .collect(Collectors.groupingBy(AssignmentTemplate::getType));

        for (Map.Entry<Assignment.Type, List<AssignmentTemplate>> entry : typeGroups.entrySet()) {
            Assignment.Type type = entry.getKey();
            List<AssignmentTemplate> assignTemplates = entry.getValue();

            // Calculate total weight for this type
            double totalWeight = assignTemplates.stream().mapToDouble(AssignmentTemplate::getWeight).sum();
            String weightStr = String.format("%.0f", totalWeight * 100); // Convert to percentage

            // Get submission types (assume all templates of same type have same submission
            // types)
            String submissionTypes = String.join(", ", assignTemplates.get(0).getSubmissionTypes());

            assignmentModel.addRow(new Object[] {
                    type.toString(),
                    weightStr,
                    assignTemplates.size(),
                    submissionTypes
            });
        }

        JTable assignmentTable = new JTable(assignmentModel);
        JScrollPane assignmentScrollPane = new JScrollPane(assignmentTable);

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(assignmentTable);
        paddedRenderer.applyCellPadding(assignmentTable);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(assignmentScrollPane, gbc);

        // Buttons for assignment types
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel typeButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addTypeButton = new JButton("Add Type");
        JButton editTypeButton = new JButton("Edit Type");
        JButton deleteTypeButton = new JButton("Delete Type");
        typeButtonPanel.add(addTypeButton);
        typeButtonPanel.add(editTypeButton);
        typeButtonPanel.add(deleteTypeButton);

        formPanel.add(typeButtonPanel, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton saveButton = new JButton("Save Changes");
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());

        // Same handlers as in createNewTemplate
        addTypeButton.addActionListener(e -> addAssignmentType(assignmentModel));
        editTypeButton.addActionListener(e -> editAssignmentType(assignmentTable, assignmentModel));
        deleteTypeButton.addActionListener(e -> deleteAssignmentType(assignmentTable, assignmentModel));

        saveButton.addActionListener(e -> {
            String templateName = templateNameField.getText();
            if (templateName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Template name is required",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validate total weight is 100%
            double totalWeight = 0;
            for (int i = 0; i < assignmentModel.getRowCount(); i++) {
                // Convert from percentage string to decimal
                String weightStr = ((String) assignmentModel.getValueAt(i, 1)).replace("%", "");
                totalWeight += Double.parseDouble(weightStr);
            }

            if (Math.abs(totalWeight - 100) > 0.01) {
                JOptionPane.showMessageDialog(dialog,
                        "Total weight must equal 100%",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Create updated assignment templates
                List<AssignmentTemplate> newAssignmentTemplates = new ArrayList<>();
                for (int i = 0; i < assignmentModel.getRowCount(); i++) {
                    String typeStr = (String) assignmentModel.getValueAt(i, 0);
                    Assignment.Type type = Assignment.Type.valueOf(typeStr);

                    // Convert percentage string to decimal weight
                    String weightStr = ((String) assignmentModel.getValueAt(i, 1)).replace("%", "");
                    double weight = Double.parseDouble(weightStr) / 100.0;

                    int count = Integer.parseInt(assignmentModel.getValueAt(i, 2).toString());

                    // Parse submission types
                    String submissionTypesStr = (String) assignmentModel.getValueAt(i, 3);
                    List<String> submissionTypes = Arrays.asList(submissionTypesStr.split(",\\s*"));

                    // Create a template for each count
                    for (int j = 0; j < count; j++) {
                        AssignmentTemplate template = new AssignmentTemplate(
                                selectedTemplate.getId(),
                                weight / count, // Divide weight by count
                                type,
                                submissionTypes);
                        newAssignmentTemplates.add(template);
                    }
                }

                // Update template in database
                selectedTemplate.setName(templateName);
                selectedTemplate.setAssignmentTemplates(newAssignmentTemplates);

                // Use teacher to edit the template
                tc.editCourseTemplate(selectedTemplate);

                // Update UI
                DefaultListModel<String> model = (DefaultListModel<String>) templateJList.getModel();
                int index = templateJList.getSelectedIndex();
                if (index != -1) {
                    model.set(index, templateName);
                }

                JOptionPane.showMessageDialog(dialog,
                        "Template updated successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error updating template: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void deleteSelectedTemplate(JSplitPane split) {
        if (!(teacher instanceof Teacher)) {
            JOptionPane.showMessageDialog(this,
                    "You do not have permission to delete template.",
                    "Permission denied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Teacher tc = (Teacher) teacher;
        String selectedTemplateName = templateJList.getSelectedValue();
        if (selectedTemplateName == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a template to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete template '" + selectedTemplateName + "'?\n" +
                        "This action cannot be undone and will affect any courses using this template.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Get the selected template from database
        CourseTemplateDAO templateDAO = CourseTemplateDAO.getInstance();
        List<CourseTemplate> allTemplates = templateDAO.readAll();
        CourseTemplate selectedTemplate = allTemplates.stream()
                .filter(t -> t.getName().equals(selectedTemplateName))
                .findFirst()
                .orElse(null);

        if (selectedTemplate == null) {
            JOptionPane.showMessageDialog(this,
                    "Template not found in database",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Use teacher to delete the template
            tc.deleteCourseTemplate(selectedTemplate);

            // Update UI
            DefaultListModel<String> model = (DefaultListModel<String>) templateJList.getModel();
            model.removeElement(selectedTemplateName);

            JOptionPane.showMessageDialog(this,
                    "Template deleted successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            setPlaceholderTemplateDetails(split);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error deleting template: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTemplateDetails(CourseTemplate template,
            DefaultTableModel model,
            JLabel headerLabel) {
        // 1. header
        headerLabel.setText("Template Name: " + template.getName());

        // 2. table rows
        model.setRowCount(0); // clear
        Map<Assignment.Type, List<AssignmentTemplate>> byType = template.getAssignmentTemplates().stream()
                .collect(Collectors.groupingBy(AssignmentTemplate::getType));

        for (Map.Entry<Assignment.Type, List<AssignmentTemplate>> e : byType.entrySet()) {
            Assignment.Type type = e.getKey();
            List<AssignmentTemplate> list = e.getValue();

            double weightSum = list.stream().mapToDouble(AssignmentTemplate::getWeight).sum();
            String weightStr = String.format("%.0f%%", weightSum * 100);

            String submissionTypes = String.join(", ", list.get(0).getSubmissionTypes());

            model.addRow(new Object[] { type, weightStr, list.size(), submissionTypes });
        }
    }

    private void showTemplateDetails(String templateName, JSplitPane split) {
        CourseTemplateDAO dao = CourseTemplateDAO.getInstance();
        CourseTemplate tpl = dao.readAll().stream()
                .filter(t -> t.getName().equals(templateName))
                .findFirst().orElse(null);
        if (tpl == null) return;

        // --- right panel: header + table ---
        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.setBorder(BorderFactory.createTitledBorder("Template Details"));

        JLabel header = new JLabel("Template Name: " + tpl.getName());
        right.add(header, BorderLayout.NORTH);

        String[] cols = { "Type", "Weight", "Count", "Submission Types" };
        DefaultTableModel mdl = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tbl = new JTable(mdl);
        right.add(new JScrollPane(tbl), BorderLayout.CENTER);

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(tbl);
        paddedRenderer.applyCellPadding(tbl);


        // use the detailed method to fill table + header
        showTemplateDetails(tpl, mdl, header);

        split.setRightComponent(right);
    }

    private void addAssignmentType(DefaultTableModel model) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Assignment Type",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new GridBagLayout());
        Padding.addPanelPaddingDefault(panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Type
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(UIConstants.getBoldLabel("Type:"), gbc);

        gbc.gridx = 1;
        String[] types = { "HOMEWORK", "QUIZ", "EXAM", "PROJECT" };
        JComboBox<String> typeComboBox = new JComboBox<>(types);
        panel.add(typeComboBox, gbc);
        Padding.addInputPaddingDefault(typeComboBox);

        // Weight
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(UIConstants.getBoldLabel("Weight (%):"), gbc);

        gbc.gridx = 1;
        JTextField weightField = new JTextField("10");
        panel.add(weightField, gbc);
        Padding.addInputPaddingDefault(weightField);

        // Count
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(UIConstants.getBoldLabel("Count:"), gbc);

        gbc.gridx = 1;
        JTextField countField = new JTextField("1");
        panel.add(countField, gbc);
        Padding.addInputPaddingDefault(countField);

        // Submission types
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(UIConstants.getBoldLabel("Submission Types:"), gbc);

        gbc.gridx = 1;
        JTextField submissionTypesField = new JTextField("pdf, docx");
        panel.add(submissionTypesField, gbc);
        Padding.addInputPaddingDefault(submissionTypesField);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancelButton = new JButton("Cancel");
        JButton addButton = new JButton("Add");
        buttonPanel.add(cancelButton);
        buttonPanel.add(addButton);
        panel.add(buttonPanel, gbc);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        addButton.addActionListener(e -> {
            try {
                String type = (String) typeComboBox.getSelectedItem();
                String weight = weightField.getText();
                String count = countField.getText();
                String submissionTypes = submissionTypesField.getText();

                // Validate inputs
                double weightValue = Double.parseDouble(weight);
                int countValue = Integer.parseInt(count);

                if (weightValue <= 0 || countValue <= 0) {
                    throw new NumberFormatException();
                }

                // Add to model
                model.addRow(new Object[] { type, weight, count, submissionTypes });
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter valid numbers for weight and count",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void editAssignmentType(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an assignment type to edit",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Assignment Type",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new GridBagLayout());
        Padding.addPanelPaddingDefault(panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Type
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(UIConstants.getBoldLabel("Type:"), gbc);

        gbc.gridx = 1;
        String[] types = { "HOMEWORK", "QUIZ", "EXAM", "PROJECT" };
        JComboBox<String> typeComboBox = new JComboBox<>(types);
        typeComboBox.setSelectedItem(model.getValueAt(selectedRow, 0));
        panel.add(typeComboBox, gbc);
        Padding.addInputPaddingDefault(typeComboBox);

        // Weight
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(UIConstants.getBoldLabel("Weight (%):"), gbc);

        gbc.gridx = 1;
        JTextField weightField = new JTextField(model.getValueAt(selectedRow, 1).toString());
        panel.add(weightField, gbc);
        Padding.addInputPaddingDefault(weightField);

        // Count
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(UIConstants.getBoldLabel("Count:"), gbc);

        gbc.gridx = 1;
        JTextField countField = new JTextField(model.getValueAt(selectedRow, 2).toString());
        panel.add(countField, gbc);
        Padding.addInputPaddingDefault(countField);

        // Submission types
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(UIConstants.getBoldLabel("Submission Types:"), gbc);

        gbc.gridx = 1;
        JTextField submissionTypesField = new JTextField(model.getValueAt(selectedRow, 3).toString());
        panel.add(submissionTypesField, gbc);
        Padding.addInputPaddingDefault(submissionTypesField);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancelButton = new JButton("Cancel");
        JButton saveButton = new JButton("Save");
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        panel.add(buttonPanel, gbc);

        // Add button actions
        cancelButton.addActionListener(e -> dialog.dispose());
        saveButton.addActionListener(e -> {
            try {
                String type = (String) typeComboBox.getSelectedItem();
                String weight = weightField.getText();
                String count = countField.getText();
                String submissionTypes = submissionTypesField.getText();

                // Validate inputs
                double weightValue = Double.parseDouble(weight);
                int countValue = Integer.parseInt(count);

                if (weightValue <= 0 || countValue <= 0) {
                    throw new NumberFormatException();
                }

                // Update model
                model.setValueAt(type, selectedRow, 0);
                model.setValueAt(weight, selectedRow, 1);
                model.setValueAt(count, selectedRow, 2);
                model.setValueAt(submissionTypes, selectedRow, 3);

                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter valid numbers for weight and count",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.setLocationRelativeTo(this);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void deleteAssignmentType(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select an assignment type to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this assignment type?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        model.removeRow(selectedRow);
    }
    @Override
    public void refresh() {
        loadTemplateNames();
    }
}
