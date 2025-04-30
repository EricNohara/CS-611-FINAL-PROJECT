package ui.utils;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.Comparator;

import java.util.List;
import model.Course;
import model.Assignment;
import model.Submission;
import db.AssignmentDAO;
import db.SubmissionDAO;



public class StudentGradeResult {
    public double earnedSum;
    public double maxSum;
    public int completedCount;
    public int totalAssignments;
    public String lastSubmissionInfo;
    public String letterGrade;


    public StudentGradeResult(double earnedSum, double maxSum, int completedCount,
                              int totalAssignments, String lastSubmissionInfo,
                              String letterGrade) {
        this.earnedSum = earnedSum;
        this.maxSum = maxSum;
        this.completedCount = completedCount;
        this.totalAssignments = totalAssignments;
        this.lastSubmissionInfo = lastSubmissionInfo;
        this.letterGrade = letterGrade;
    }
    public static double getStudentGradePercent(int studentId, int courseId) {
        AssignmentDAO aDao = AssignmentDAO.getInstance();
        SubmissionDAO sDao = SubmissionDAO.getInstance();

        List<Assignment> assignments = aDao.readAllCondition("course_id", courseId);

        double earnedSum = 0, maxSum = 0;

        for (Assignment a : assignments) {
            List<Submission> subs = sDao.readAllCondition("assignment_id", a.getId())
                    .stream()
                    .filter(s -> s.getCollaboratorIds().contains(studentId))
                    .collect(Collectors.toList());

            Submission sub = subs.stream()
                    .max(Comparator.comparing(Submission::getSubmittedAt))
                    .orElse(null);

            if (sub != null && sub.getStatus() == Submission.Status.GRADED) {
                earnedSum += sub.getPointsEarned();
                maxSum += a.getMaxPoints();
            }
        }

        return 100.0 * earnedSum / maxSum;
    }

    public static String getLetterGrade(double pct) {
        return (pct >= 93) ? "A"
                : (pct >= 90) ? "A-"
                : (pct >= 87) ? "B+"
                : (pct >= 83) ? "B"
                : (pct >= 80) ? "B-"
                : (pct >= 77) ? "C+"
                : (pct >= 73) ? "C"
                : (pct >= 70) ? "C-"
                : (pct >= 67) ? "D+"
                : (pct >= 63) ? "D"
                : (pct >= 60) ? "D-" : "F";
    }
}
