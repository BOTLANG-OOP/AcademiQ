package com.academiq.model;

import com.academiq.grading.GradingPolicy;

import java.util.ArrayList;
import java.util.List;

public class Course {

    private final String name;
    private final String code;
    private final int units;
    private final List<Assessment> assessments;
    private final List<TimeSlot> timeSlots;
    private GradingPolicy gradingPolicy;

    public Course(String name, String code, int units, GradingPolicy gradingPolicy) {
        this.name = name;
        this.code = code;
        this.units = units;
        this.gradingPolicy = gradingPolicy;
        this.assessments = new ArrayList<>();
        this.timeSlots = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public int getUnits() {
        return units;
    }

    public List<Assessment> getAssessments() {
        return assessments;
    }

    public List<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public GradingPolicy getGradingPolicy() {
        return gradingPolicy;
    }

    public void setGradingPolicy(GradingPolicy policy) {
        this.gradingPolicy = policy;
    }

    public void addAssessment(Assessment a) {
        assessments.add(a);
    }

    public void removeAssessment(Assessment a) {
        assessments.remove(a);
    }

    public void addTimeSlot(TimeSlot ts) {
        timeSlots.add(ts);
    }

    public double getFinalGrade() {
        return gradingPolicy.computeFinalGrade(assessments);
    }

    public String getGradeBreakdown() {
        return gradingPolicy.getBreakdown(assessments);
    }

    public double whatDoINeed(double targetGrade) {
        return gradingPolicy.projectNeeded(assessments, targetGrade);
    }
}
