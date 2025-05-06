package ui.dashboard.panels;

import db.AssignmentDAO;
import db.CourseDAO;
import db.SubmissionDAO;
import db.UserCourseDAO;
import model.*;
import ui.utils.PaddedCellRenderer;

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
    private JComboBox<String> strategyComboBox;
    private JTextField thresholdField;
    private JLabel thresholdLabel;
    private DefaultTableModel statsModel;
    private JTable statsTable;
    private JLabel meanLabel, stdDevLabel, medianLabel;
    private JPanel chartPanel;

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
        strategyComboBox = new JComboBox<>(new String[] { "Proportional", "Pass/Fail (60%)" });
        thresholdLabel = new JLabel("Pass Threshold:");
        thresholdField = new JTextField("60", 5); // default 60%
        thresholdField.setEnabled(false);

        strategyComboBox.addActionListener(e -> {
            String selected = (String) strategyComboBox.getSelectedItem();
            thresholdField.setEnabled("Pass/Fail (60%)".equals(selected));
        });

        JButton loadBtn = new JButton("Load Overall Stats");
        JButton exportBtn = new JButton("Export to CSV");

        topPanel.add(new JLabel("Course:"));
        topPanel.add(courseComboBox);
        topPanel.add(new JLabel("Strategy:"));
        topPanel.add(strategyComboBox);
        topPanel.add(thresholdLabel);
        topPanel.add(thresholdField);
        topPanel.add(loadBtn);
        topPanel.add(exportBtn);
        add(topPanel, BorderLayout.NORTH);

        String[] columns = { "Student ID", "Student Name", "Overall Grade" };
        statsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        statsTable = new JTable(statsModel);
        add(new JScrollPane(statsTable), BorderLayout.CENTER);
        statsTable.getTableHeader().setFont(statsTable.getTableHeader().getFont().deriveFont(Font.BOLD));

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(statsTable);
        paddedRenderer.applyCellPadding(statsTable);

        JPanel statsSummary = new JPanel(new GridLayout(1, 3));
        meanLabel = new JLabel("Mean: -");
        stdDevLabel = new JLabel("Std Dev: -");
        medianLabel = new JLabel("Median: -");
        statsSummary.add(meanLabel);
        statsSummary.add(stdDevLabel);
        statsSummary.add(medianLabel);
        add(statsSummary, BorderLayout.EAST);

        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawOverallGradeHistogram((Graphics2D) g);
            }
        };
        chartPanel.setPreferredSize(new Dimension(600, 200));
        chartPanel.setBackground(Color.WHITE);
        add(chartPanel, BorderLayout.SOUTH);

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

    private GradingStrategy getSelectedStrategy(double threshold) {
        String selected = (String) strategyComboBox.getSelectedItem();
        if ("Pass/Fail (60%)".equals(selected)) {
            return new PassFailGradingStrategy(threshold / 100.0);
        } else {
            return new ProportionalGradingStrategy();
        }
    }

    private void loadStats() {
        statsModel.setRowCount(0);
        Course course = (Course) courseComboBox.getSelectedItem();
        if (course == null)
            return;

        double thresholdVal = 60.0;
        try {
            thresholdVal = Double.parseDouble(thresholdField.getText());
            if (thresholdVal < 0 || thresholdVal > 100)
                throw new NumberFormatException("Out of range");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid pass threshold. Please enter a value between 0 and 100.",
                    "Threshold Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        GradingStrategy strategy = getSelectedStrategy(thresholdVal);

        List<Assignment> assignments = AssignmentDAO.getInstance().readAllCondition("course_id", course.getId());
        List<User> students = UserCourseDAO.getInstance().getUsersInCourseByRole(course.getId(), User.Role.STUDENT);

        List<Double> rawScores = new ArrayList<>();

        for (User student : students) {
            double totalEarned = 0, totalMax = 0;
            for (Assignment a : assignments) {
                List<Submission> subs = SubmissionDAO.getInstance()
                        .readAllCondition("assignment_id", a.getId()).stream()
                        .filter(sub -> sub.getCollaboratorIds().contains(student.getId())
                                && sub.getStatus() == Submission.Status.GRADED)
                        .collect(Collectors.toList());
                if (!subs.isEmpty()) {
                    totalEarned += subs.get(0).getPointsEarned();
                    totalMax += a.getMaxPoints();
                }
            }

            if (totalMax > 0) {
                double grade = strategy.calculateGrade(totalEarned, totalMax);
                rawScores.add(grade);
                String displayGrade = (strategy instanceof ProportionalGradingStrategy)
                        ? String.format("%.2f%%", grade * 100)
                        : (grade == 1.0 ? "Pass" : "Fail");

                statsModel.addRow(new Object[] {
                        student.getId(),
                        student.getName(),
                        displayGrade
                });
            }
        }

        List<Double> percentages = rawScores.stream().map(g -> g * 100).collect(Collectors.toList());
        updateStatsLabels(percentages);
        chartPanel.repaint(); // Redraw histogram
    }

    private void drawOverallGradeHistogram(Graphics2D g2) {
        int w = chartPanel.getWidth();
        int h = chartPanel.getHeight();

        List<Double> scores = new ArrayList<>();
        for (int i = 0; i < statsModel.getRowCount(); i++) {
            Object val = statsModel.getValueAt(i, 2);
            if (val instanceof String && ((String) val).endsWith("%")) {
                try {
                    double percent = Double.parseDouble(((String) val).replace("%", ""));
                    scores.add(percent);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (scores.isEmpty())
            return;

        int[] bins = new int[10];
        for (double score : scores) {
            int bin = Math.min((int) (score / 10), 9);
            bins[bin]++;
        }

        int barWidth = w / bins.length;
        int maxCount = Arrays.stream(bins).max().orElse(1);

        g2.setColor(Color.BLUE);
        for (int i = 0; i < bins.length; i++) {
            int barHeight = (int) (((double) bins[i] / maxCount) * (h - 30));
            g2.fillRect(i * barWidth + 4, h - barHeight - 20, barWidth - 8, barHeight);
        }

        g2.setColor(Color.BLACK);
        for (int i = 0; i < bins.length; i++) {
            String label = String.format("%d-%d%%", i * 10, (i + 1) * 10 - 1);
            g2.drawString(label, i * barWidth + 4, h - 5);
        }
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
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
            for (int i = 0; i < statsModel.getColumnCount(); i++) {
                pw.print(statsModel.getColumnName(i));
                if (i < statsModel.getColumnCount() - 1)
                    pw.print(",");
            }
            pw.println();

            for (int r = 0; r < statsModel.getRowCount(); r++) {
                for (int c = 0; c < statsModel.getColumnCount(); c++) {
                    pw.print(statsModel.getValueAt(r, c));
                    if (c < statsModel.getColumnCount() - 1)
                        pw.print(",");
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
