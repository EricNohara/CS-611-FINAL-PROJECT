package model;
import java.sql.Timestamp;

import utils.SubmissionFileManager;

import java.io.File;

public class Student extends User implements SubmissionUploader {
    public Student(int id, String name, String email, String passwordHash, Timestamp createdAt, Timestamp lastUpdated) {
        super(id, name, email, passwordHash, createdAt, lastUpdated);
    }

    public Student(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    @Override
    public User.Role getRole() { return User.Role.STUDENT; }

    @Override
    public boolean uploadSubmission(File file, Submission submission) {
        return SubmissionFileManager.uploadSubmission(file, submission);
    }

    // Student specific methods...
}
