package model;

import java.io.File;

public interface SubmissionUploader {
    boolean uploadSubmission(File file, Submission submission);
}
