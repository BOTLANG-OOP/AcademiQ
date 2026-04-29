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
        // TODO: weighted average by units, 5.0 = highest
        return 0.0;
    }

    public List<ConflictRecord> detectConflicts() {
        // TODO: cross-compare all course time slots
        return Collections.emptyList();
    }
}
