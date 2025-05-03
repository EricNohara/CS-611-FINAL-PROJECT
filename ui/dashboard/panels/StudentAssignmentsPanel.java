package ui.dashboard.panels;

import db.AssignmentDAO;
import db.CourseDAO;
import db.SubmissionDAO;
import db.UserCourseDAO;
import model.Assignment;
import model.Course;
import model.Student;
import model.Submission;
import model.UserCourse;
import ui.dashboard.panels.Refreshable;
import ui.utils.PaddedCellRenderer;
import ui.utils.Padding;
import utils.SubmissionFileManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class StudentAssignmentsPanel extends JPanel implements Refreshable {
    private final Student student;
    private DefaultTableModel assignmentModel;
    private JTable assignmentTable;

    public StudentAssignmentsPanel(Student student) {
        super(new BorderLayout(10, 10));
        this.student = student;
        Padding.addPanelPaddingDefault(this);
        buildUI();
        loadAssignments();
    }

    private void buildUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton submitButton = new JButton("Submit Assignment");
        JButton refreshButton = new JButton("Refresh");
        topPanel.add(submitButton);
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);

        String[] columns = { "ID", "Title", "Course", "Type", "Due Date", "Max Points", "Status" };
        assignmentModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        assignmentTable = new JTable(assignmentModel);
        add(new JScrollPane(assignmentTable), BorderLayout.CENTER);

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(assignmentTable);
        paddedRenderer.applyCellPadding(assignmentTable);

        submitButton.addActionListener(e -> submitSelectedAssignment());
        refreshButton.addActionListener(e -> refresh());
    }

    private void loadAssignments() {
        assignmentModel.setRowCount(0);
        CourseDAO courseDAO = CourseDAO.getInstance();
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();

        List<UserCourse> enrollments = userCourseDAO.readAll().stream()
                .filter(uc -> uc.getUserId() == student.getId())
                .collect(Collectors.toList());

        for (UserCourse uc : enrollments) {
            Course course = courseDAO.read(uc.getCourseId());
            if (course == null)
                continue;

            List<Assignment> assignments = assignmentDAO.readAllCondition("course_id", course.getId());
            for (Assignment a : assignments) {
                boolean open = a.getDueDate().after(new Timestamp(System.currentTimeMillis()));
                assignmentModel.addRow(new Object[] {
                        a.getId(), a.getName(), course.getName(), a.getType().toString(),
                        a.getDueDate(), a.getMaxPoints(), open ? "Open" : "Closed"
                });
            }
        }
    }

    private void submitSelectedAssignment() {
        int selectedRow = assignmentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an assignment.", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int assignmentId = (int) assignmentModel.getValueAt(selectedRow, 0);
        String assignmentTitle = (String) assignmentModel.getValueAt(selectedRow, 1);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select file to submit for: " + assignmentTitle);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                Submission submission = new Submission(
                        assignmentId,
                        "",
                        Submission.Status.UNGRADED,
                        Collections.singletonList(student.getId()));

                boolean success = SubmissionFileManager.uploadSubmission(selectedFile, submission);
                if (!success) {
                    throw new RuntimeException("File upload failed");
                }

                SubmissionDAO.getInstance().create(submission);

                JOptionPane.showMessageDialog(this, "File submitted successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to submit file: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void refresh() {
        loadAssignments();
    }
}
