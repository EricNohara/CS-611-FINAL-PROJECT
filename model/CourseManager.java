package model;
public interface CourseManager {
    // adding + removing users to courses
    void addUser(User user, Course course);
    void removeUser(User user, Course course);

    // create + edit + delete course templates
    CourseTemplate createCourseTemplate();
    CourseTemplate editCourseTemplate(CourseTemplate template);
    void deleteCourseTemplate(CourseTemplate template);

    // create + edit + delete actual courses
    Course createCourse(CourseTemplate template);
    Course editCourse(Course course);
    void deleteCourse(Course course);

    // create + edit + delete assignments
    Assignment createAssignment(Course course);
    Assignment editAddAssignment(Assignment assignment);
    void deleteAssignment(Assignment assignment);
}
