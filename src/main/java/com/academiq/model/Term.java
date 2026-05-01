package com.academiq.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Term {

    private final String name;
    private final int year;
    private final String semester;
    private final List<Course> courses;

    public Term(String name, int year, String semester) {
        this.name = name;
        this.year = year;
        this.semester = semester;
        this.courses = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getYear() {
        return year;
    }

    public String getSemester() {
        return semester;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void addCourse(Course c) {
        courses.add(c);
    }

    public void removeCourse(Course c) {
        courses.remove(c);
    }

    public double getTermGPA() {
        if (courses.isEmpty()) {
            return 0.0;
        }
        double totalPoints = 0.0;
        int totalUnits = 0;
        for (Course course : courses) {
            double grade = course.getGradingPolicy().computeFinalGrade(course.getAssessments());
            int units = course.getUnits();
            totalPoints += grade * units;
            totalUnits += units;
        }
        if (totalUnits == 0) {
            return 0.0;
        }
        // Convert 0-100 scale to 0.0-5.0 scale
        return (totalPoints / totalUnits) / 100.0 * 5.0;
    }

    public List<ConflictRecord> detectConflicts() {
        // TODO: cross-compare all course time slots
        return Collections.emptyList();
    }
}
