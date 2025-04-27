package ui.dashboard.panels;

import ui.dashboard.components.DashboardCard;
import db.*;
import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.util.List;

public final class OverviewPanel extends JPanel implements Refreshable{
    private User teacher;

    public OverviewPanel(User teacher) {
        super(new BorderLayout(10, 10));
        this.teacher = teacher; // 需要把 teacher 保存成成员变量
        buildUI();
    }
    private void buildUI() {
        removeAll();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel cards = new JPanel(new GridLayout(2, 2, 15, 15));

        CourseDAO courseDAO = CourseDAO.getInstance();
        AssignmentDAO assignDAO = AssignmentDAO.getInstance();
        SubmissionDAO submDAO = SubmissionDAO.getInstance();

        int activeCourses = courseDAO.getActiveCoursesCount(teacher.getId());
        //System.out.println(activeCourses);
        int totalStudents = courseDAO.getTotalStudentsCount(teacher.getId());

        List<Course> courses = courseDAO.getCoursesForTeacher(teacher.getId());
        int pendingSubmissions = 0;
        int upcomingDeadlines = 0;

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp inOneWeek = new Timestamp(now.getTime() + 7L * 24 * 60 * 60 * 1000);

        for (Course c : courses) {
            List<Assignment> assigns = assignDAO.readAllCondition("course_id", c.getId());
            if (assigns == null)
                continue;

            for (Assignment a : assigns) {
                List<Submission> subs = submDAO.readAllCondition("assignment_id", a.getId());
                if (subs != null)
                    pendingSubmissions += subs.stream()
                            .filter(s -> s.getStatus() == Submission.Status.UNGRADED)
                            .count();

                if (a.getDueDate().after(now) && a.getDueDate().before(inOneWeek))
                    upcomingDeadlines++;
            }
        }

        cards.add(new DashboardCard("Active Courses", String.valueOf(activeCourses),
                "You have " + activeCourses + " active courses this semester."));
        cards.add(new DashboardCard("Pending Submissions", String.valueOf(pendingSubmissions),
                "You have " + pendingSubmissions + " submissions waiting to be graded."));
        cards.add(new DashboardCard("Upcoming Deadlines", String.valueOf(upcomingDeadlines),
                "Deadlines in the next 7 days."));
        cards.add(new DashboardCard("Total Students", String.valueOf(totalStudents),
                "Across all your courses."));

        add(cards, BorderLayout.NORTH);

        JPanel activityPanel = new JPanel(new BorderLayout(5, 5));
        activityPanel.setBorder(BorderFactory.createTitledBorder("Recent Activity"));

        String[] cols = { "Time", "Activity", "Course", "Details" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        List<Submission> subs = submDAO.readAll();
        subs.sort((s1, s2) -> s2.getSubmittedAt().compareTo(s1.getSubmittedAt()));

        int added = 0;
        for (Submission s : subs) {
            if (added == 5)
                break;
            Assignment a = assignDAO.read(s.getAssignmentId());
            if (a == null)
                continue;
            Course c = courses.stream().filter(x -> x.getId() == a.getCourseId()).findFirst().orElse(null);
            if (c == null)
                continue;

            String act = (s.getStatus() == Submission.Status.GRADED) ? "Grades Published" : "Submission Received";
            String det = (s.getStatus() == Submission.Status.GRADED) ? a.getName() + " grades released"
                    : "New submission for " + a.getName();

            model.addRow(new Object[]{s.getSubmittedAt(), act, c.getName(), det});
            added++;
        }

        JTable tbl = new JTable(model);
        activityPanel.add(new JScrollPane(tbl), BorderLayout.CENTER);

        add(activityPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }
    @Override
    public void refresh() {
        buildUI();
    }
}
