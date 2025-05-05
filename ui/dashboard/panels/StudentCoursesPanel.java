package ui.dashboard.panels;

import db.CourseDAO;
import db.UserCourseDAO;
import model.Course;
import model.Student;
import model.UserCourse;
import ui.UIConstants;
import ui.dashboard.panels.Refreshable;
import ui.utils.PaddedCellRenderer;
import ui.utils.Padding;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class StudentCoursesPanel extends JPanel implements Refreshable {
    private final Student student;
    private DefaultTableModel courseModel;
    private JTable courseTable;

    public StudentCoursesPanel(Student student) {
        super(new BorderLayout(10, 10));
        this.student = student;
        Padding.addPanelPaddingDefault(this);
        buildUI();
        loadCourses();
    }

    private void buildUI() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton enrollButton = new JButton("Enroll in Course");
        JButton refreshButton = new JButton("Refresh");
        topPanel.add(enrollButton);
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);

        String[] columns = { "ID", "Name", "Status" };
        courseModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        courseTable = new JTable(courseModel);
        add(new JScrollPane(courseTable), BorderLayout.CENTER);
        courseTable.getTableHeader().setFont(courseTable.getTableHeader().getFont().deriveFont(Font.BOLD));

        PaddedCellRenderer paddedRenderer = new PaddedCellRenderer();
        PaddedCellRenderer.setDefaultRowHeight(courseTable);
        paddedRenderer.applyCellPadding(courseTable);

        enrollButton.addActionListener(e -> showEnrollDialog());
        refreshButton.addActionListener(e -> refresh());
    }

    private void loadCourses() {
        courseModel.setRowCount(0);
        CourseDAO courseDAO = CourseDAO.getInstance();
        UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();

        List<UserCourse> enrollments = userCourseDAO.readAll().stream()
                .filter(uc -> uc.getUserId() == student.getId())
                .collect(Collectors.toList());

        for (UserCourse uc : enrollments) {
            Course course = courseDAO.read(uc.getCourseId());
            if (course != null) {
                String status = uc.getStatus().toString();
                courseModel.addRow(new Object[] { course.getId(), course.getName(), status });
            }
        }
    }

    private void showEnrollDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Enroll in Course",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        Padding.addPanelPaddingDefault(panel);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(UIConstants.getBoldLabel("Course ID:"), gbc);

        gbc.gridx = 1;
        JTextField courseIdField = new JTextField(10);
        form.add(courseIdField, gbc);
        Padding.addInputPaddingDefault(courseIdField);

        panel.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton enroll = new JButton("Enroll");
        buttons.add(cancel);
        buttons.add(enroll);
        panel.add(buttons, BorderLayout.SOUTH);

        cancel.addActionListener(e -> dialog.dispose());
        enroll.addActionListener(e -> {
            try {
                int courseId = Integer.parseInt(courseIdField.getText().trim());
                CourseDAO courseDAO = CourseDAO.getInstance();
                Course course = courseDAO.read(courseId);
                if (course == null) {
                    JOptionPane.showMessageDialog(dialog, "Course not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();
                if (userCourseDAO.read(student.getId(), courseId) != null) {
                    JOptionPane.showMessageDialog(dialog, "Already enrolled in this course.", "Warning",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                UserCourse enrollment = new UserCourse(student.getId(), courseId, UserCourse.Status.ACTIVE,
                        student.getRole());
                userCourseDAO.create(enrollment);
                JOptionPane.showMessageDialog(dialog, "Successfully enrolled!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                refresh();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid course ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Enrollment failed: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(panel);
        dialog.setLocationRelativeTo(this);
        dialog.pack();
        dialog.setVisible(true);
    }

    @Override
    public void refresh() {
        loadCourses();
    }
}
