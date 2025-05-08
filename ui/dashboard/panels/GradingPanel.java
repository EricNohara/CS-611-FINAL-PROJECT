package ui.dashboard.panels;

import db.*;
import model.*;
import ui.utils.GradingUtils;
import ui.utils.PaddedCellRenderer;
import utils.EmailSender;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;


// Grading tab
public final class GradingPanel extends JPanel implements Refreshable {

    private final User grader;
    private final JTabbedPane parentTabs;
    private final List<Course> teacherCourses;

    /* UI widgets referenced by helpers */
    private DefaultTableModel submissionModel;
    private JTable submissionTable;
    private JComboBox<String> courseCombo;
    private JComboBox<String> assignmentCombo;
    private JComboBox<String> statusCombo;

    public GradingPanel(User teacher, JTabbedPane parentTabs) {
        super(new BorderLayout(10, 10));
        this.grader = teacher;
        this.parentTabs = parentTabs;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        teacherCourses = CourseDAO.getInstance()
                .getCoursesForTeacher(teacher.getId());

        buildUI();
        loadSubmissionsData(); // first fill
    }

    /* =============== UI construction =============== */
    private void buildUI() {

        // -------- filter bar --------
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filter.add(new JLabel("Course:"));
        courseCombo = new JComboBox<>();
        courseCombo.addItem("All Courses");
        teacherCourses.forEach(c -> courseCombo.addItem(c.getName()));
        filter.add(courseCombo);

        filter.add(new JLabel("Assignment:"));
        assignmentCombo = new JComboBox<>(new String[] { "All Assignments" });
        filter.add(assignmentCombo);

        // dynamic assignment dropdown
        courseCombo.addActionListener(e -> updateAssignmentDropdown());

        filter.add(new JLabel("Status:"));
        statusCombo = new JComboBox<>(new String[] {
                "All",
                Submission.Status.GRADED.toString(),
                Submission.Status.UNGRADED.toString(),
                Submission.Status.LATE.toString() });
        filter.add(statusCombo);

        JButton applyBtn = new JButton("Apply Filter");
        filter.add(applyBtn);

        add(filter, BorderLayout.NORTH);

        // -------- submissions table --------
        String[] cols = { "ID", "Student", "Course", "Assignment",
                "Submitted", "Status", "Grade" };
        submissionModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        submissionTable = new JTable(submissionModel);
        add(new JScrollPane(submissionTable), BorderLayout.CENTER);
        submissionTable.getTableHeader().setFont(submissionTable.getTableHeader().getFont().deriveFont(Font.BOLD));

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(submissionTable);
        paddedRenderer.applyCellPadding(submissionTable);

        // -------- action buttons --------
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton viewBtn = new JButton("View Submission");
        JButton gradeBtn = new JButton("Grade");
        JButton publishBtn = new JButton("Publish Grades");
        JButton exportBtn = new JButton("Export Grades");
        actions.add(viewBtn);
        actions.add(gradeBtn);
        actions.add(publishBtn);
        actions.add(exportBtn);
        add(actions, BorderLayout.SOUTH);

        /* ---- listeners ---- */
        applyBtn.addActionListener(e -> loadSubmissionsData());
        viewBtn.addActionListener(e -> viewSubmission());
        gradeBtn.addActionListener(e -> gradeSubmission());
        publishBtn.addActionListener(e -> publishGrades());
        exportBtn.addActionListener(e -> exportGrades());
    }

    // Helpers

    // Helper method to update assignment dropdown based on course selection
    private void updateAssignmentDropdown() {
        assignmentCombo.removeAllItems();
        assignmentCombo.addItem("All Assignments");

        String selectedCourse = (String) courseCombo.getSelectedItem();

        if (!"All Courses".equals(selectedCourse)) {
            // Find the selected course
            Course course = teacherCourses.stream()
                    .filter(c -> c.getName().equals(selectedCourse))
                    .findFirst()
                    .orElse(null);

            if (course != null) {
                // Get assignments for this course
                AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
                List<Assignment> assignments = assignmentDAO.readAllCondition("course_id", course.getId());

                if (assignments != null) {
                    for (Assignment assignment : assignments) {
                        assignmentCombo.addItem(assignment.getName());
                    }
                }
            }
        }
    }

    // Helper method to load submissions data
    private void loadSubmissionsData(DefaultTableModel model, String courseFilter, String assignmentFilter,
                                 String statusFilter, List<Course> teacherCourses) {
        model.setRowCount(0);

        SubmissionDAO submissionDAO = SubmissionDAO.getInstance();
        List<Submission> allSubmissions = submissionDAO.readAll();
        AssignmentDAO assignmentDAO = AssignmentDAO.getInstance();
        UserDAO userDAO = UserDAO.getInstance();

        // Key: (studentId, assignmentId) -> latest Submission
        Map<String, Submission> latestSubmissions = new HashMap<>();

        for (Submission submission : allSubmissions) {
            if (submission.getCollaboratorIds().isEmpty()) continue;
            int studentId = submission.getCollaboratorIds().get(0);
            String key = studentId + "-" + submission.getAssignmentId();

            // Get the most recent submission for each student-assignment pair
            Submission existing = latestSubmissions.get(key);
            if (existing == null || submission.getSubmittedAt().after(existing.getSubmittedAt())) {
                latestSubmissions.put(key, submission);
            }
        }

        for (Submission submission : latestSubmissions.values()) {
            Assignment assignment = assignmentDAO.read(submission.getAssignmentId());
            if (assignment == null) continue;

            boolean isTeachersCourse = teacherCourses.stream()
                    .anyMatch(c -> c.getId() == assignment.getCourseId());
            if (!isTeachersCourse) continue;

            Course course = teacherCourses.stream()
                    .filter(c -> c.getId() == assignment.getCourseId())
                    .findFirst()
                    .orElse(null);
            if (course == null) continue;

            // Apply course filter
            if (courseFilter != null && !"All Courses".equals(courseFilter) &&
                    !course.getName().equals(courseFilter)) {
                continue;
            }

            // Apply assignment filter
            if (assignmentFilter != null && !"All Assignments".equals(assignmentFilter) &&
                    !assignment.getName().equals(assignmentFilter)) {
                continue;
            }

            // Apply status filter
            if (statusFilter != null && !"All".equals(statusFilter) &&
                    !submission.getStatus().toString().equals(statusFilter)) {
                continue;
            }

            int studentId = submission.getCollaboratorIds().get(0);
            User student = userDAO.read(studentId);
            if (student == null) continue;

            String gradeDisplay = "-";
            if (submission.getStatus() == Submission.Status.GRADED) {
                gradeDisplay = String.format("%.0f/%.0f",
                        submission.getPointsEarned(), assignment.getMaxPoints());
            }

            Object[] rowData = {
                    submission.getId(),
                    student.getName(),
                    course.getName(),
                    assignment.getName(),
                    submission.getSubmittedAt(),
                    submission.getStatus().toString(),
                    gradeDisplay
            };
            model.addRow(rowData);
        }
    }


    private void viewSubmission(Submission submission, Assignment assignment) {
        GradingUtils.showSubmissionDialog(this, submission, assignment);
    }

    // Helper method to grade a submission
    private void gradeSubmission(Submission submission, Assignment assignment) {
        GradingUtils.showGradingDialog(this, grader, submission, assignment, this::loadSubmissionsData);
    }

    // Reload the table using the current filter-combo selections.
    private void loadSubmissionsData() {
        loadSubmissionsData(
                submissionModel,
                (String) courseCombo.getSelectedItem(),
                (String) assignmentCombo.getSelectedItem(),
                (String) statusCombo.getSelectedItem(),
                teacherCourses);
    }

    // View the submission that is currently selected in the table.
    private void viewSubmission() {
        int row = submissionTable.getSelectedRow();
        if (row == -1)
            return; // nothing selected
        int modelRow = submissionTable.convertRowIndexToModel(row);
        int submissionId = (int) submissionModel.getValueAt(modelRow, 0);

        SubmissionDAO sDao = SubmissionDAO.getInstance();
        Submission sub = sDao.read(submissionId);
        Assignment asg = AssignmentDAO.getInstance().read(sub.getAssignmentId());

        viewSubmission(sub, asg); // call existing helper
    }

    // Grade the submission that is currently selected in the table.
    private void gradeSubmission() {
        int row = submissionTable.getSelectedRow();
        if (row == -1)
            return;
        int modelRow = submissionTable.convertRowIndexToModel(row);
        int submissionId = (int) submissionModel.getValueAt(modelRow, 0);

        SubmissionDAO sDao = SubmissionDAO.getInstance();
        Submission sub = sDao.read(submissionId);
        Assignment asg = AssignmentDAO.getInstance().read(sub.getAssignmentId());

        gradeSubmission(sub, asg); // call existing helper
    }

    private void exportGrades() {
        if (submissionModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "There are no submissions to export with the current filter.",
                    "Nothing to Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Grades");
        fc.setSelectedFile(new File("grades.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File outFile = fc.getSelectedFile();

        SubmissionDAO sDao = SubmissionDAO.getInstance();
        AssignmentDAO aDao = AssignmentDAO.getInstance();
        UserDAO uDao = UserDAO.getInstance();

        try (PrintWriter pw = new PrintWriter(outFile)) {

            pw.println("Submission ID,Student ID,Student Name,Course,Assignment,"
                    + "Submitted,Status,Points,Max Points,Percentage");

            for (int r = 0; r < submissionModel.getRowCount(); r++) {

                int subId = (Integer) submissionModel.getValueAt(r, 0);
                Submission sub = sDao.read(subId);
                if (sub == null)
                    continue;

                Assignment asg = aDao.read(sub.getAssignmentId());
                Course crs = teacherCourses.stream()
                        .filter(c -> c.getId() == asg.getCourseId())
                        .findFirst().orElse(null);

                int stuId = sub.getCollaboratorIds().isEmpty() ? -1
                        : sub.getCollaboratorIds().get(0);
                String stuName = (stuId == -1) ? "Unknown"
                        : uDao.read(stuId).getName();

                String points = (sub.getStatus() == Submission.Status.GRADED)
                        ? String.format("%.1f", sub.getPointsEarned())
                        : "";
                String pct = (sub.getStatus() == Submission.Status.GRADED)
                        ? String.format("%.1f", 100.0 * sub.getPointsEarned() / asg.getMaxPoints())
                        : "";

                // escape commas in free-text fields
                String escStu = "\"" + stuName.replace("\"", "\"\"") + "\"";
                String escCrs = "\"" + crs.getName().replace("\"", "\"\"") + "\"";
                String escAsg = "\"" + asg.getName().replace("\"", "\"\"") + "\"";

                pw.println(String.join(",",
                        String.valueOf(sub.getId()),
                        String.valueOf(stuId),
                        escStu,
                        escCrs,
                        escAsg,
                        sub.getSubmittedAt().toString(),
                        sub.getStatus().toString(),
                        points,
                        String.format("%.1f", asg.getMaxPoints()),
                        pct));
            }

            JOptionPane.showMessageDialog(this,
                    "Grades exported successfully to " + outFile.getName(),
                    "Export Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error exporting grades: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void publishGrades() {
        // Build a list of assignments the teacher owns
        List<Assignment> allAsg = new ArrayList<>();
        AssignmentDAO aDao = AssignmentDAO.getInstance();
        for (Course c : teacherCourses)
            allAsg.addAll(aDao.readAllCondition("course_id", c.getId()));

        if (allAsg.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "You have no assignments to publish.",
                    "Nothing to Publish", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show selection dialog
        DefaultListModel<Assignment> lm = new DefaultListModel<>();
        allAsg.forEach(lm::addElement);

        JList<Assignment> list = new JList<>(lm);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
        
                Assignment asg = (Assignment) value;
                Course crs = teacherCourses.stream()
                        .filter(c -> c.getId() == asg.getCourseId())
                        .findFirst().orElse(null);
                String labelText = (crs != null ? crs.getName() : "Unknown") + ": " + asg.getName();
        
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, labelText, index, isSelected, cellHasFocus);
                return label;
            }
        });
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JCheckBox notifyChk = new JCheckBox("Notify students via email", true);

        int opt = JOptionPane.showConfirmDialog(
                this,
                new Object[] { "Select assignment(s) to publish:",
                        new JScrollPane(list),
                        notifyChk },
                "Publish Grades",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (opt != JOptionPane.OK_OPTION || list.getSelectedValuesList().isEmpty())
            return;

        // Update submissions
        SubmissionDAO sDao = SubmissionDAO.getInstance();
        int released = 0;

        for (Assignment asg : list.getSelectedValuesList()) {
            List<Submission> subs = sDao.readAllCondition("assignment_id", asg.getId());

            for (Submission sub : subs) {
                if (sub.getStatus() != Submission.Status.GRADED)
                    continue; // only graded ones

                if (sub.getGrade() >= 0) { // not yet published
                    double percent = 100.0 * sub.getPointsEarned() / asg.getMaxPoints();
                    sub.setGrade(percent); // store percentage as released grade
                    sDao.update(sub);
                    released++;

                    if (notifyChk.isSelected()) {
                        // EmailService.notifyStudent(sub.getCollaboratorIds(), asg, percent);
                        List<Integer> collaborators = sub.getCollaboratorIds();
                        List<String> emails = new ArrayList<>();
                        UserDAO userDAO = UserDAO.getInstance();

                        for (Integer id : collaborators) {
                            User user = userDAO.read(id);
                            if (user != null) emails.add(user.getEmail());
                        }

                        EmailSender.sendEmail(emails, asg.getName() + " Has Been Graded!", "You scored a " + percent + "%");
                    }
                }
            }
        }

        JOptionPane.showMessageDialog(this,
                String.format("Grades published for %d submission(s).", released),
                "Publish Complete", JOptionPane.INFORMATION_MESSAGE);

        loadSubmissionsData(); // refresh table
    }


    @Override
    public void refresh() {
        loadSubmissionsData();
    }
}
