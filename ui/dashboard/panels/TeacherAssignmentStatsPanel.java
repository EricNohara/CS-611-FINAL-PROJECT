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

public class TeacherAssignmentStatsPanel extends JPanel implements Refreshable {
    private final Teacher teacher;
    private JComboBox<Course> courseComboBox;
    private JComboBox<Assignment> assignmentComboBox;
    private DefaultTableModel statsModel;
    private JTable statsTable;
    private JPanel chartPanel;
    private JLabel meanLabel, stdDevLabel, medianLabel;

    public TeacherAssignmentStatsPanel(Teacher teacher) {
        super(new BorderLayout(10, 10));
        this.teacher = teacher;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUI();
        loadCourses();
    }

    private void buildUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        courseComboBox = new JComboBox<>();
        assignmentComboBox = new JComboBox<>();
        assignmentComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            if (value != null) {
                label.setText(value.getName());
            }
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            return label;
        });

        JButton loadBtn = new JButton("Load Stats");
        JButton exportBtn = new JButton("Export to CSV");

        topPanel.add(new JLabel("Course:"));
        topPanel.add(courseComboBox);
        topPanel.add(new JLabel("Assignment:"));
        topPanel.add(assignmentComboBox);
        topPanel.add(loadBtn);
        topPanel.add(exportBtn);

        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Student ID", "Student Name", "Score", "Max Points"};
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

        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawHistogram((Graphics2D) g);
            }
        };
        chartPanel.setPreferredSize(new Dimension(600, 200));
        chartPanel.setBackground(Color.WHITE);
        add(chartPanel, BorderLayout.SOUTH);

        JPanel statsSummary = new JPanel(new GridLayout(1, 3));
        meanLabel = new JLabel("Mean: -");
        stdDevLabel = new JLabel("Std Dev: -");
        medianLabel = new JLabel("Median: -");
        statsSummary.add(meanLabel);
        statsSummary.add(stdDevLabel);
        statsSummary.add(medianLabel);
        add(statsSummary, BorderLayout.EAST);

        courseComboBox.addActionListener(e -> loadAssignments());
        loadBtn.addActionListener(e -> loadStats());
        exportBtn.addActionListener(e -> exportAssignmentStatsToCSV());
    }

    private void loadCourses() {
        courseComboBox.removeAllItems();
        List<Course> courses = CourseDAO.getInstance().getCoursesForTeacher(teacher.getId());
        for (Course course : courses) {
            courseComboBox.addItem(course);
        }
        loadAssignments();
    }

    private void loadAssignments() {
        assignmentComboBox.removeAllItems();
        Course selectedCourse = (Course) courseComboBox.getSelectedItem();
        if (selectedCourse == null) return;

        List<Assignment> assignments = AssignmentDAO.getInstance()
                .readAllCondition("course_id", selectedCourse.getId());
        for (Assignment a : assignments) {
            assignmentComboBox.addItem(a);
        }
    }

    private void loadStats() {
        statsModel.setRowCount(0);
        Course course = (Course) courseComboBox.getSelectedItem();
        Assignment assignment = (Assignment) assignmentComboBox.getSelectedItem();
        if (course == null || assignment == null) return;

        List<Submission> submissions = SubmissionDAO.getInstance()
                .readAllCondition("assignment_id", assignment.getId());

        Map<Integer, Double> studentScores = new HashMap<>();
        for (Submission s : submissions) {
            if (s.getStatus() == Submission.Status.GRADED && !s.getCollaboratorIds().isEmpty()) {
                studentScores.put(s.getCollaboratorIds().get(0), s.getPointsEarned());
            }
        }

        List<User> students = UserCourseDAO.getInstance()
                .getUsersInCourseByRole(course.getId(), User.Role.STUDENT);
        List<Double> allScores = new ArrayList<>();

        for (User student : students) {
            double score = studentScores.getOrDefault(student.getId(), 0.0);
            allScores.add(score);
            statsModel.addRow(new Object[] {
                    student.getId(),
                    student.getName(),
                    score,
                    assignment.getMaxPoints()
            });
        }

        updateStatsLabels(allScores);
        chartPanel.repaint();
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

        meanLabel.setText(String.format("Mean: %.2f", mean));
        stdDevLabel.setText(String.format("Std Dev: %.2f", stdDev));
        medianLabel.setText(String.format("Median: %.2f", median));
    }

    private void drawHistogram(Graphics2D g2) {
        Assignment assignment = (Assignment) assignmentComboBox.getSelectedItem();
        if (assignment == null) return;

        int w = chartPanel.getWidth();
        int h = chartPanel.getHeight();

        List<Double> scores = new ArrayList<>();
        for (int i = 0; i < statsModel.getRowCount(); i++) {
            Object val = statsModel.getValueAt(i, 2);
            if (val instanceof Number) scores.add(((Number) val).doubleValue());
        }
        if (scores.isEmpty()) return;

        int[] bins = new int[10];
        double max = assignment.getMaxPoints();
        for (double score : scores) {
            int bin = (int) ((score / max) * 10);
            if (bin == 10) bin = 9;
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

    private void exportAssignmentStatsToCSV() {
        if (statsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No data to export.",
                    "Export Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Assignment Statistics");
        fc.setSelectedFile(new File("assignment_stats.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

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
                    "Assignment statistics exported successfully!",
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

    public void setAssignmentChangeListener(java.util.function.Consumer<String> listener) {
        assignmentComboBox.addActionListener(e -> {
            Assignment selected = (Assignment) assignmentComboBox.getSelectedItem();
            if (selected != null) {
                listener.accept(selected.getName());
            }
        });
    }
}
