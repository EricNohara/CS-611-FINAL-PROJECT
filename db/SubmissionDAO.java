package db;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import model.Submission;

public class SubmissionDAO implements CrudDAO<Submission> {
    // SINGLETON ACCESS
    private static final SubmissionDAO instance = new SubmissionDAO();

    private SubmissionDAO() {}

    public static SubmissionDAO getInstance() { return instance; }

    // ABSTRACT CRUD OPERATIONS
    @Override
    public void create(Submission submission) {
        String submissionQuery = "INSERT INTO submissions (assignment_id, grader_id, filepath, submitted_at, points_earned, grade, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String userSubmissionsQuery = "INSERT INTO user_submissions (user_id, submission_id) VALUES (?, ?)";

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false); // BEGIN TRANSACTION

            try (PreparedStatement stmt = connection.prepareStatement(submissionQuery, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, submission.getAssignmentId());
                stmt.setInt(2, submission.getGraderId());
                stmt.setString(3, submission.getFilepath());
                stmt.setTimestamp(4, submission.getSubmittedAt());
                stmt.setDouble(5, submission.getPointsEarned());
                stmt.setDouble(6, submission.getGrade());
                stmt.setInt(7, submission.getStatus().ordinal());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Creating submission failed, no rows affected.");

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int submissionId = generatedKeys.getInt(1);
                        submission.setId(submissionId);

                        try (PreparedStatement userSubStmt = connection.prepareStatement(userSubmissionsQuery)) {
                            for (Integer collaborator_id : submission.getCollaboratorIds()) {
                                userSubStmt.setInt(1, collaborator_id);
                                userSubStmt.setInt(2, submissionId);
                                userSubStmt.addBatch();
                            }
                            userSubStmt.executeBatch();
                        }
                    } else {
                        throw new SQLException("Creating submission failed, no ID obtained.");
                    }
                }

                connection.commit(); // COMMIT if everything succeeds
            } catch (SQLException e) {
                connection.rollback(); // ROLLBACK on any error
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error creating submission: " + e.getMessage());
        }
    }
    
    @Override
    public Submission read(int id) {
        String query = "SELECT * FROM submissions WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) {
                return buildFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading submission: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Submission> readAllCondition(String columnName, Object value) {
        String query = "SELECT * FROM submissions WHERE " + columnName.trim() + " = ?";
        //System.out.println("SQL = " + query + ", param = " + value);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
    
            stmt.setObject(1, value);
            ResultSet rs = stmt.executeQuery();
            List<Submission> submissions = new ArrayList<>();
    
            while (rs.next()) submissions.add(buildFromResultSet(rs));
            return submissions;
        } catch (SQLException e) {
            System.err.println("Error reading submissions: " + e.getMessage());
        }
        return null;
    }
//    private List<Integer> loadCollaboratorsForSubmission(int submissionId) {
//        List<Integer> collaboratorIds = new ArrayList<>();
//        String collaboratorQuery = "SELECT user_id FROM user_submissions WHERE submission_id = ?";
//
//        try (Connection connection = DBConnection.getConnection();
//             PreparedStatement stmt = connection.prepareStatement(collaboratorQuery)) {
//            stmt.setInt(1, submissionId);
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    collaboratorIds.add(rs.getInt("user_id"));
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println("Error loading collaborators: " + e.getMessage());
//        }
//
//        return collaboratorIds;
//    }
//

    @Override
    public List<Submission> readAll() {
        List<Submission> submissions = new ArrayList<>();
        String query = "SELECT * FROM submissions";

        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) submissions.add(buildFromResultSet(rs));           
        } catch (SQLException e) {
            System.err.println("Error reading all submissions: " + e.getMessage());
        }

        return submissions;
    }

    @Override
    public void update(Submission submission) {
        String updateSubmissionQuery = "UPDATE submissions SET grader_id = ?, filepath = ?, submitted_at = ?, points_earned = ?, grade = ?, status = ? WHERE id = ?";
        String deleteUserSubmissionsQuery = "DELETE FROM user_submissions WHERE submission_id = ?";
        String addUserSubmissionsQuery = "INSERT INTO user_submissions (user_id, submission_id) VALUES (?, ?)";

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false); // start transaction

            try {
                // update submission record
                try (PreparedStatement stmt = connection.prepareStatement(updateSubmissionQuery)) {
                    stmt.setInt(1, submission.getGraderId());
                    stmt.setString(2, submission.getFilepath());
                    stmt.setTimestamp(3, submission.getSubmittedAt());
                    stmt.setDouble(4, submission.getPointsEarned());
                    stmt.setDouble(5, submission.getGrade());
                    stmt.setInt(6, submission.getStatus().ordinal());
                    stmt.setInt(7, submission.getId());

                    stmt.executeUpdate();
                }

                // delete old collaborators
                try (PreparedStatement stmt = connection.prepareStatement(deleteUserSubmissionsQuery)) {
                    stmt.setInt(1, submission.getId());
                    stmt.executeUpdate();
                }

                // insert new collaborators
                try (PreparedStatement stmt = connection.prepareStatement(addUserSubmissionsQuery)) {
                    for (Integer collaboratorId : submission.getCollaboratorIds()) {
                        stmt.setInt(1, collaboratorId);
                        stmt.setInt(2, submission.getId());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                connection.commit(); // commit transaction
            } catch (SQLException e) {
                connection.rollback(); // rollback on any half deleted states
                throw e; // rethrow to trigger the outer catch block
            }
        } catch (SQLException e) {
            System.err.println("Error updating submission: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM submissions WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting submission failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting submission: " + e.getMessage());
        }
    }

    @Override
    public Submission buildFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int assignmentId = rs.getInt("assignment_id");
        int graderId = rs.getInt("grader_id");
        String filepath = rs.getString("filepath");
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        double pointsEarned = rs.getDouble("points_earned");
        double grade = rs.getDouble("grade");
        Submission.Status status = Submission.Status.values()[rs.getInt("status")];

        // do query of all users in user_submissions for this submission id
        List<Integer> collaboratorIds = new ArrayList<>();
        String collaboratorQuery = "SELECT user_id FROM user_submissions WHERE submission_id = ?";

        try (Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(collaboratorQuery)) {
            stmt.setInt(1, id);
            try (ResultSet collabRs = stmt.executeQuery()) {
                while (collabRs.next()) {
                    int userId = collabRs.getInt("user_id");
                    if (userId > -1) collaboratorIds.add(userId);
                }
            }
        }

        return new Submission(id, assignmentId, graderId, filepath, submittedAt, pointsEarned, grade, status, collaboratorIds);
    }
//    public Submission buildFromResultSet(ResultSet rs) throws SQLException {
//        int id = rs.getInt("id");
//        int assignmentId = rs.getInt("assignment_id");
//        int graderId = rs.getInt("grader_id");
//        String filepath = rs.getString("filepath");
//        Timestamp submittedAt = rs.getTimestamp("submitted_at");
//        double pointsEarned = rs.getDouble("points_earned");
//        double grade = rs.getDouble("grade");
//        Submission.Status status = Submission.Status.values()[rs.getInt("status")];
//
//        return new Submission(id, assignmentId, graderId, filepath, submittedAt, pointsEarned, grade, status, new ArrayList<>());
//    }
//

}
