package ui.dashboard.panels;

import db.AssignmentDAO;
import db.CourseDAO;
import db.SubmissionDAO;
import db.UserCourseDAO;
import model.Assignment;
import model.Course;
import model.Submission;
import model.Teacher;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TeacherOverallStatsPanel extends JPanel implements Refreshable {
    private final Teacher teacher;
    private JComboBox<Course> courseComboBox;
    private DefaultTableModel statsModel;
    private JTable statsTable;
    private JLabel meanLabel, stdDevLabel, medianLabel;

    public TeacherOverallStatsPanel(Teacher teacher) {
        super(new BorderLayout(10, 10));
        this.teacher = teacher;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUI();
        loadCourses();
    }

    private void buildUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        courseComboBox = new JComboBox<>();
        JButton loadBtn = new JButton("Load Overall Stats");
        JButton exportBtn = new JButton("Export to CSV");

        topPanel.add(new JLabel("Course:"));
        topPanel.add(courseComboBox);
        topPanel.add(loadBtn);
        topPanel.add(exportBtn);
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Student ID", "Student Name", "Overall Grade %"};
        statsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        statsTable = new JTable(statsModel);
        add(new JScrollPane(statsTable), BorderLayout.CENTER);

        JPanel statsSummary = new JPanel(new GridLayout(1, 3));
        meanLabel = new JLabel("Mean: -");
        stdDevLabel = new JLabel("Std Dev: -");
        medianLabel = new JLabel("Median: -");
        statsSummary.add(meanLabel);
        statsSummary.add(stdDevLabel);
        statsSummary.add(medianLabel);
        add(statsSummary, BorderLayout.SOUTH);

        loadBtn.addActionListener(e -> loadStats());
        exportBtn.addActionListener(e -> exportStatsToCSV());
    }

    private void loadCourses() {
        courseComboBox.removeAllItems();
        List<Course> courses = CourseDAO.getInstance().getCoursesForTeacher(teacher.getId());
        for (Course course : courses) {
            courseComboBox.addItem(course);
        }
    }

    private void loadStats() {
        statsModel.setRowCount(0);
        Course course = (Course) courseComboBox.getSelectedItem();
        if (course == null) return;

        List<Assignment> assignments = AssignmentDAO.getInstance()
                .readAllCondition("course_id", course.getId());
        List<User> students = UserCourseDAO.getInstance()
                .getUsersInCourseByRole(course.getId(), User.Role.STUDENT);

        List<Double> percentages = new ArrayList<>();

        for (User student : students) {
            double totalEarned = 0, totalMax = 0;
            for (Assignment a : assignments) {
                List<Submission> subs = SubmissionDAO.getInstance()
                        .readAllCondition("assignment_id", a.getId()).stream()
                        .filter(sub -> sub.getCollaboratorIds().contains(student.getId()) && sub.getStatus() == Submission.Status.GRADED)
                        .collect(Collectors.toList());
                if (!subs.isEmpty()) {
                    totalEarned += subs.get(0).getPointsEarned();
                    totalMax += a.getMaxPoints();
                }
            }
            if (totalMax > 0) {
                double percent = totalEarned / totalMax * 100;
                percentages.add(percent);
                statsModel.addRow(new Object[]{
                        student.getId(),
                        student.getName(),
                        String.format("%.2f%%", percent)
                });
            }
        }

        updateStatsLabels(percentages);
    }

    private void updateStatsLabels(List<Double> scores) {
        if (scores.isEmpty()) {
            meanLabel.setText("Mean: -");
            stdDevLabel.setText("Std Dev: -");
            medianLabel.setText("Median: -");
            return;
        }

        double sum = scores.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / scores.size();
        double variance = scores.stream().mapToDouble(s -> Math.pow(s - mean, 2)).sum() / scores.size();
        double stdDev = Math.sqrt(variance);

        Collections.sort(scores);
        double median = scores.size() % 2 == 0
                ? (scores.get(scores.size() / 2 - 1) + scores.get(scores.size() / 2)) / 2
                : scores.get(scores.size() / 2);

        meanLabel.setText(String.format("Mean: %.2f%%", mean));
        stdDevLabel.setText(String.format("Std Dev: %.2f%%", stdDev));
        medianLabel.setText(String.format("Median: %.2f%%", median));
    }

    private void exportStatsToCSV() {
        if (statsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export.", "Export Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Course Grade Stats");
        fc.setSelectedFile(new File("course_overall_stats.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
            for (int i = 0; i < statsModel.getColumnCount(); i++) {
                pw.print(statsModel.getColumnName(i));
                if (i < statsModel.getColumnCount() - 1) pw.print(",");
            }
            pw.println();

            for (int r = 0; r < statsModel.getRowCount(); r++) {
                for (int c = 0; c < statsModel.getColumnCount(); c++) {
                    pw.print(statsModel.getValueAt(r, c));
                    if (c < statsModel.getColumnCount() - 1) pw.print(",");
                }
                pw.println();
            }

            JOptionPane.showMessageDialog(this,
                    "Course grade statistics exported successfully!",
                    "Export Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error exporting data: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        loadCourses();
    }
}
