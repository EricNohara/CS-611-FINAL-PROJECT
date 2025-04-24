package model;
public interface CourseManager {
    // adding + removing users to courses
    void addUserToCourse(User user, Course course);
    void removeUserFromCourse(User user, Course course);

    // create + edit + delete course templates
    CourseTemplate createCourseTemplate();
    CourseTemplate editCourseTemplate(CourseTemplate template);
    void deleteCourseTemplate(CourseTemplate template);

    // create + edit + delete actual courses
    Course createCourse(CourseTemplate template);
    Course editCourse(Course course);
    void deleteCourse(Course course);

    // create + edit + delete assignments
    Assignment createAssignment(Course course, AssignmentTemplate template);
    Assignment editAssignment(Assignment assignment);
    void deleteAssignment(Assignment assignment);
}
