package ui.utils;

import db.AssignmentDAO;
import db.SubmissionDAO;
import db.UserCourseDAO;
import db.UserDAO;
import model.*;
import ui.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class GradingUtils {

    public static void showSubmissionDialog(Component parent, Submission submission, Assignment assignment) {
        buildDialog(parent, submission, assignment, null, false);
    }

    public static void showGradingDialog(Component parent, User teacher, Submission submission, Assignment assignment, Runnable refreshCallback) {
        buildDialog(parent, submission, assignment, new GradingContext(teacher, refreshCallback), true);
    }

    private static void buildDialog(Component parent, Submission submission, Assignment assignment, GradingContext gradingContext, boolean enableGrading) {
        List<String> studentNames = loadStudentNames(submission);
        System.out.println("check student name" + studentNames.size());
    
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), 
            enableGrading ? "Grade Submission" : "View Submission", 
            Dialog.ModalityType.APPLICATION_MODAL);
    
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        Padding.addPanelPaddingDefault(panel);
    
        JPanel headerPanel = buildSubmissionInfoPanel(submission, assignment, studentNames);
        panel.add(headerPanel, BorderLayout.NORTH);
    
        JPanel gradingPanel = null;
    
        if (enableGrading) {
            JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            mainSplitPane.setDividerLocation(450);
    
            JPanel submissionPanel = buildSubmissionPanel(submission);
            gradingPanel = buildGradingPanel(assignment); 
    
            mainSplitPane.setLeftComponent(submissionPanel);
            mainSplitPane.setRightComponent(gradingPanel);
    
            panel.add(mainSplitPane, BorderLayout.CENTER);
        } else {
            JPanel submissionPanel = buildSubmissionPanel(submission);
            panel.add(submissionPanel, BorderLayout.CENTER);
        }
    
        JPanel buttonPanel = buildButtonPanel(dialog, submission, gradingContext, assignment, gradingPanel);
        panel.add(buttonPanel, BorderLayout.SOUTH);
    
        dialog.add(panel);
        dialog.setLocationRelativeTo(parent);
        dialog.pack();
        dialog.setVisible(true);
    }
    

    private static List<String> loadStudentNames(Submission submission) {
        UserDAO userDAO = UserDAO.getInstance();
        List<String> names = new ArrayList<>();
        for (int id : submission.getCollaboratorIds()) {
            User student = userDAO.read(id);
            if (student != null) names.add(student.getName());
            else names.add("Unknown Student (ID: " + id + ")");
        }
        return names;
    }

    private static JPanel buildSubmissionInfoPanel(Submission submission, Assignment assignment, List<String> studentNames) {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Submission Info"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        panel.add(new JLabel("Assignment:"));
        panel.add(new JLabel(assignment.getName()));

        panel.add(new JLabel("Student(s):"));
        panel.add(new JLabel(String.join(", ", studentNames)));

        panel.add(new JLabel("Submitted Date:"));
        panel.add(new JLabel(submission.getSubmittedAt().toString()));

        panel.add(new JLabel("Status:"));
        panel.add(new JLabel(submission.getStatus().toString()));

        return panel;
    }

    private static JPanel buildSubmissionPanel(Submission submission) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Submission Content"));

        JTextArea contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true); // wrap at word boundaries, not mid-word

        loadFileContent(submission, contentArea);

        panel.add(new JScrollPane(contentArea), BorderLayout.CENTER);
        return panel;
    }

    private static void loadFileContent(Submission submission, JTextArea area) {
        if (submission.getFilepath() == null || submission.getFilepath().isEmpty()) {
            area.setText("No file available.");
            return;
        }

        File file = new File(submission.getFilepath());
        if (!file.exists()) {
            area.setText("File not found.");
            return;
        }

        try {
            String text = new String(Files.readAllBytes(file.toPath()), "UTF-8");
            if (text.length() > 10000) text = text.substring(0, 10000) + "\n... (truncated)";
            area.setText(text);
        } catch (IOException e) {
            area.setText("Error reading file: " + e.getMessage());
        }
    }

    private static JPanel buildGradingPanel(Assignment assignment) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Grading"));

        String[] columns = {"Criteria", "Points", "Max Points"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        // EDIT THIS LATER
        switch (assignment.getType()) {
            case HOMEWORK:
                model.addRow(new Object[]{"Correctness", 0, assignment.getMaxPoints() * 0.7});
                model.addRow(new Object[]{"Code Style/Documentation", 0, assignment.getMaxPoints() * 0.2});
                model.addRow(new Object[]{"Efficiency", 0, assignment.getMaxPoints() * 0.1});
                break;
            case PROJECT:
                model.addRow(new Object[]{"Functionality", 0, assignment.getMaxPoints() * 0.4});
                model.addRow(new Object[]{"Design/Architecture", 0, assignment.getMaxPoints() * 0.3});
                model.addRow(new Object[]{"Code Quality", 0, assignment.getMaxPoints() * 0.2});
                model.addRow(new Object[]{"Documentation", 0, assignment.getMaxPoints() * 0.1});
                break;
            default:
                model.addRow(new Object[]{"Overall Score", 0, assignment.getMaxPoints()});
                break;
        }

        JTable table = new JTable(model) {
            public boolean isCellEditable(int row, int col) {
                return col == 1;
            }
        };

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(table);
        paddedRenderer.applyCellPadding(table);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JTextArea feedbackArea = new JTextArea(6, 30);
        feedbackArea.setBorder(BorderFactory.createTitledBorder("Feedback"));
        feedbackArea.setFont(UIConstants.DEFAULT_FONT);
        panel.add(new JScrollPane(feedbackArea), BorderLayout.SOUTH);

        panel.putClientProperty("rubricModel", model);
        panel.putClientProperty("rubricTable", table);
        panel.putClientProperty("feedbackArea", feedbackArea);

        return panel;
    }

    private static JPanel buildButtonPanel(JDialog dialog, Submission submission, GradingContext gradingContext, Assignment assignment, JPanel gradingPanel) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton downloadButton = new JButton("Download File");
        downloadButton.addActionListener(e -> downloadFile(dialog, submission));
        panel.add(downloadButton);

        if (gradingContext != null) {
            JButton saveButton = new JButton("Save Grade");
            saveButton.addActionListener(e -> saveGrade(gradingContext.teacher, submission, assignment, gradingPanel, dialog, gradingContext.refreshCallback));
            panel.add(saveButton);
        }

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        panel.add(closeButton);

        return panel;
    }

    private static void downloadFile(Component parent, Submission submission) {
        if (submission.getFilepath() == null || submission.getFilepath().isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No file to download", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(new File(submission.getFilepath()).getName()));

        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                Files.copy(new File(submission.getFilepath()).toPath(),
                        fileChooser.getSelectedFile().toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(parent, "Downloaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "Download failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void saveGrade(User teacher, Submission submission, Assignment assignment, JPanel gradingPanel, JDialog dialog, Runnable refreshCallback) {
        if(!(teacher instanceof SubmissionGrader)) {
            return;
        }
        SubmissionGrader grader = (SubmissionGrader) teacher;
        JTable rubricTable = (JTable) gradingPanel.getClientProperty("rubricTable");
        if (rubricTable != null && rubricTable.isEditing()) {
            rubricTable.getCellEditor().stopCellEditing();
        }

        try {
            DefaultTableModel model = (DefaultTableModel) gradingPanel.getClientProperty("rubricModel");
            //JTextArea feedbackArea = (JTextArea) gradingPanel.getClientProperty("feedbackArea");

            double totalPoints = 0;
            for (int i = 0; i < model.getRowCount(); i++) {
                Object val = model.getValueAt(i, 1);
                if (val != null) {
                    try {
                        totalPoints += Double.parseDouble(val.toString());
                    } catch (NumberFormatException ex) {
                        System.out.println("Invalid number at row " + i + ": " + val);
                    }
                }
            }

            if (totalPoints > assignment.getMaxPoints()) {
                JOptionPane.showMessageDialog(dialog, "Total points cannot exceed max points", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (submission.getStatus() != Submission.Status.GRADED) {
                submission.setStatus(Submission.Status.GRADED);
            }

            submission.setGraderId(teacher.getId());
            //submission.setFeedback(feedbackArea.getText());


            grader.gradeSubmission(submission, totalPoints);

            JOptionPane.showMessageDialog(dialog, "Graded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();

            if (refreshCallback != null) {
                refreshCallback.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class GradingContext {
        User teacher;
        Runnable refreshCallback;

        GradingContext(User teacher, Runnable refreshCallback) {
            this.teacher = teacher;
            this.refreshCallback = refreshCallback;
        }
    }

//    private double GetStudentGrade(int studentId, int courseId){
//        AssignmentDAO aDao = AssignmentDAO.getInstance();
//        SubmissionDAO sDao = SubmissionDAO.getInstance();
//
//        List<Assignment> assignments = aDao.readAllCondition("course_id", courseId);
//
//        // Get grade info
//        double earnedSum = 0, maxSum = 0;
//        int completed = 0;
//
//        String[] cols = { "Assignment", "Type", "Due Date",
//                "Grade", "Comments" };
//        DefaultTableModel model = new DefaultTableModel(cols, 0) {
//            @Override
//            public boolean isCellEditable(int r, int c) {
//                return false;
//            }
//        };
//
//        Submission lastSub = null;
//
//        for (Assignment a : assignments) {
//
//            // pull the submission(s) for THIS student / assignment
//            List<Submission> subs = sDao.readAllCondition("assignment_id", a.getId())
//                    .stream()
//                    .filter(s -> s.getCollaboratorIds().contains(studentId))
//                    .collect(Collectors.toList());
//
//            Submission sub = subs.stream()
//                    .max(Comparator.comparing(Submission::getSubmittedAt))
//                    .orElse(null);
//
//            String gradeStr = "-";
//            String comment = "";
//            if (sub != null) {
//                lastSub = (lastSub == null ||
//                        sub.getSubmittedAt().after(lastSub.getSubmittedAt()))
//                        ? sub
//                        : lastSub;
//
//                if (sub.getStatus() == Submission.Status.GRADED) {
//                    gradeStr = String.format("%.0f/%.0f",
//                            sub.getPointsEarned(),
//                            a.getMaxPoints());
//                    earnedSum += sub.getPointsEarned();
//                    maxSum += a.getMaxPoints();
//                    completed++;
//                }
//            }
//
//
//        }
//
//        // Top summary panel
//        double percent = (maxSum > 0) ? 100.0 * earnedSum / maxSum : 0.0;
//        String letter = getLetterGrade(percent);
//
//        JPanel summary = new JPanel(new GridLayout(3, 2, 10, 5));
//        summary.setBorder(BorderFactory.createTitledBorder("Course Information"));
//
//        summary.add(new JLabel("Current Grade:"));
//        summary.add(new JLabel(String.format("%s (%.1f%%)", letter, percent)));
//
//        summary.add(new JLabel("Assignments Completed:"));
//        summary.add(new JLabel(completed + " / " + assignments.size()));
//
//        summary.add(new JLabel("Last Submission:"));
//        summary.add(new JLabel(lastSub == null ? "—"
//                : lastSub.getSubmittedAt().toLocalDateTime().toLocalDate()
//                + " (" + aDao.read(lastSub.getAssignmentId()).getName() + ")"));
//
//        JPanel root = new JPanel(new BorderLayout(10, 10));
//        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//        root.add(summary, BorderLayout.NORTH);
//        root.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
//
//
//    }
}