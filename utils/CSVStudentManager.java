package utils;

import java.io.*;
import java.util.*;

import db.UserCourseDAO;
import db.UserDAO;
import model.Grader;
import model.Student;
import model.Teacher;
import model.User;
import model.UserCourse;

public class CSVStudentManager {
    private static UserDAO userDAO = UserDAO.getInstance();
    private static UserCourseDAO userCourseDAO = UserCourseDAO.getInstance();

    private static List<String> expectedHeader = Arrays.asList("name", "email", "role");
    private static int nameIdx = 0;
    private static int emailIdx = 1;
    private static int roleIdx = 2;
    private static String defaultPassword = "default";
    private static String defaultPasswordHash = Hasher.hashPassword(defaultPassword);

    // reads in files of the form: id, name, email, role
    public static void handleStudentCSVSubmission(File file, int courseId) {
        List<List<String>> rows;
        List<Integer> activeUserIds = new ArrayList<>();
        UserCourse.Status active = UserCourse.Status.ACTIVE;

        try {
            rows = CSVParser.parse(file);  
        } catch (Exception e) {
            System.err.println("Error processing inputted CSV file: " + e.getMessage());
            return;
        }

        if (rows.isEmpty()) {
            System.err.println("CSV file is empty.");
            return;
        }

        if (!validateHeader(rows.get(0))) {
            System.err.println("CSV header must be: name,email,role");
            return;
        }

        rows.remove(0); // remove the header

        // add/update all users in the csv file
        for (List<String> row : rows) {
            if (!isValidRow(row)) continue; // only operate on valid rows

            // if user already exists, add them to the course or set them to active
            User user = getUserFromRow(row);
            if (user != null) {
                // if user already in the course, update their status, else create new relationship
                UserCourse userCourse = userCourseDAO.read(user.getId(), courseId);
                if (userCourse != null) {
                    userCourse.setStatus(active);
                    userCourseDAO.update(userCourse);
                } else {
                    userCourse = new UserCourse(user.getId(), courseId, active, user.getRole());
                    userCourseDAO.create(userCourse);
                } 
            } else {
                // otherwise need to create user and add them to user course table
                user = buildUserFromRow(row);
                userDAO.create(user);

                UserCourse userCourse = new UserCourse(user.getId(), courseId, active, user.getRole());
                userCourseDAO.create(userCourse);
            }

            activeUserIds.add(user.getId()); // keep track of the active users in the course
        }

        // update all users in the course but NOT in the csv file
        List<UserCourse> userCourses = userCourseDAO.readAllCondition("course_id", courseId);
        for (UserCourse uc : userCourses) {
            if (!activeUserIds.contains(uc.getUserId())) {
                uc.setStatus(UserCourse.Status.INACTIVE);
                userCourseDAO.update(uc);
            }
        }
    }

    // validate the format of the header of the csv file
    private static boolean validateHeader(List<String> firstRow) {
        return firstRow.equals(expectedHeader);
    }

    // validate that they types in the header are correct
    private static boolean isValidRow(List<String> row) {
        if (row.size() != expectedHeader.size()) {
            System.err.println("Invalid row (wrong number of columns): " + row);
            return false;
        }

        int role;

        try {
            role = Integer.parseInt(row.get(roleIdx).trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid number in row: " + row);
            return false;
        }

        if (role < 0 || role > User.Role.values().length - 1) {
            System.err.println("Invalid number in row: " + row);
            return false;
        }

        if (role == User.Role.ADMIN.ordinal()) {
            System.err.println("Admin cannot be added to a course in row: " + row);
            return false;
        }

        String name = row.get(nameIdx).trim();
        String email = row.get(emailIdx).trim();

        if (name.isEmpty() || email.isEmpty()) {
            System.err.println("Missing name or email in row: " + row);
            return false;
        }

        return true;
    }

    // parse user info and create new user with default password "default"
    private static User buildUserFromRow(List<String> row) {
        String name = row.get(nameIdx);
        String email = row.get(emailIdx);
        User.Role role = User.Role.values()[Integer.parseInt(row.get(roleIdx))];

        switch (role) {
            case STUDENT:
                return new Student(name, email, defaultPasswordHash);
            case GRADER:
                return new Grader(name, email, defaultPasswordHash);
            case TEACHER:
                return new Teacher(name, email, defaultPasswordHash);
            default:
                return null;
        }
    }

    // get a user from the parsed row
    private static User getUserFromRow(List<String> row) {
        return userDAO.readByEmail(row.get(emailIdx));
    }

    // public static void main(String[] args) {
    //     File testFile = new File("test.csv");
    //     int courseId = 2;

    //     CSVStudentManager.handleStudentCSVSubmission(testFile, courseId);
    // }
}
