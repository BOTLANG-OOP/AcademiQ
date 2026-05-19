package com.academiq.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class Term {

    private final String name;
    private final int year;
    private final String semester;
    private final ObservableList<Course> courses;
    private final DoubleProperty termGPA;
    private final ChangeListener<Number> courseGradeListener;

    public Term(String name, int year, String semester) {
        this.name = name;
        this.year = year;
        this.semester = semester;
        this.courses = FXCollections.observableArrayList();
        this.termGPA = new SimpleDoubleProperty();
        this.courseGradeListener = (observable, oldValue, newValue) -> recomputeTermGPA();

        this.courses.addListener((ListChangeListener<Course>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::attachCourseListener);
                }
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::detachCourseListener);
                }
            }
            recomputeTermGPA();
        });

        recomputeTermGPA();
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

    public ObservableList<Course> getCourses() {
        return courses;
    }

    public void addCourse(Course c) {
        courses.add(c);
    }

    public void removeCourse(Course c) {
        courses.remove(c);
    }

    public int getTotalUnits() {
        int total = 0;
        for (Course course : courses) {
            total += course.getUnits();
        }
        return total;
    }

    public double getTermGPA() {
        return termGPA.get();
    }

    public DoubleProperty termGPAProperty() {
        return termGPA;
    }

    private void recomputeTermGPA() {
        termGPA.set(calculateTermGPA());
    }

    private double calculateTermGPA() {
        double totalPoints = 0.0;
        int totalUnits = 0;
        for (Course course : courses) {
            int units = course.getUnits();
            totalPoints += course.getFinalGrade() * units;
            totalUnits += units;
        }
        if (totalUnits == 0) {
            return 0.0;
        }
        return (totalPoints / totalUnits) / 100.0 * 5.0;
    }

    private void attachCourseListener(Course course) {
        course.finalGradeProperty().addListener(courseGradeListener);
    }

    private void detachCourseListener(Course course) {
        course.finalGradeProperty().removeListener(courseGradeListener);
    }

    public List<ConflictRecord> detectConflicts() {
        List<ConflictRecord> conflicts = new ArrayList<>();
        for (int i = 0; i < courses.size(); i++) {
            Course courseA = courses.get(i);
            for (int j = i + 1; j < courses.size(); j++) {
                Course courseB = courses.get(j);
                for (TimeSlot slotA : courseA.getTimeSlots()) {
                    for (TimeSlot slotB : courseB.getTimeSlots()) {
                        if (slotA.overlapsWith(slotB)) {
                            conflicts.add(new ConflictRecord(courseA, courseB, slotA, slotB));
                        }
                    }
                }
            }
        }
        return conflicts;
    }
}
