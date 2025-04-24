package model;
public interface CourseManager {
    // adding + removing users to courses
    void addUserToCourse(User user, Course course);
    void setUserStatus(User user, Course course, UserCourse.Status status);
    void removeUserFromCourse(User user, Course course);

    // create + edit + delete course templates
    void createCourseTemplate(CourseTemplate template);
    void editCourseTemplate(CourseTemplate template);
    void deleteCourseTemplate(CourseTemplate template);

    // create + edit + delete actual courses
    void createCourse(Course course);
    void editCourse(Course course);
    void deleteCourse(Course course);

    // create + edit + delete assignments
    void createAssignment(Assignment assignment);
    void editAssignment(Assignment assignment);
    void deleteAssignment(Assignment assignment);
}
