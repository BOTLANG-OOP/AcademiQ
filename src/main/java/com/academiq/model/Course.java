package com.academiq.model;

import com.academiq.grading.GradingPolicy;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.Objects;

public class Course {

    private final String name;
    private final String code;
    private final int units;
    private final ObservableList<Assessment> assessments;
    private final ObservableList<TimeSlot> timeSlots;
    private final ObjectProperty<GradingPolicy> gradingPolicy;
    private final DoubleProperty finalGrade;
    private final ChangeListener<Number> assessmentScoreListener;

    public Course(String name, String code, int units, GradingPolicy gradingPolicy) {
        this.name = name;
        this.code = code;
        this.units = units;
        this.gradingPolicy = new SimpleObjectProperty<>(Objects.requireNonNull(gradingPolicy, "gradingPolicy"));
        this.assessments = FXCollections.observableArrayList();
        this.timeSlots = FXCollections.observableArrayList();
        this.finalGrade = new SimpleDoubleProperty();
        this.assessmentScoreListener = (observable, oldValue, newValue) -> recomputeFinalGrade();

        this.assessments.addListener((ListChangeListener<Assessment>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::attachAssessmentListener);
                }
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::detachAssessmentListener);
                }
            }
            recomputeFinalGrade();
        });

        this.gradingPolicy.addListener((observable, oldPolicy, newPolicy) -> recomputeFinalGrade());
        recomputeFinalGrade();
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

    public ObservableList<Assessment> getAssessments() {
        return assessments;
    }

    public ObservableList<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public GradingPolicy getGradingPolicy() {
        return gradingPolicy.get();
    }

    public void setGradingPolicy(GradingPolicy policy) {
        this.gradingPolicy.set(Objects.requireNonNull(policy, "policy"));
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
        return finalGrade.get();
    }

    public DoubleProperty finalGradeProperty() {
        return finalGrade;
    }

    public String getGradeBreakdown() {
        return gradingPolicy.get().getBreakdown(assessments);
    }

    public double whatDoINeed(double targetGrade) {
        return gradingPolicy.get().projectNeeded(assessments, targetGrade);
    }

    private void recomputeFinalGrade() {
        finalGrade.set(gradingPolicy.get().computeFinalGrade(assessments));
    }

    private void attachAssessmentListener(Assessment assessment) {
        assessment.scoreProperty().addListener(assessmentScoreListener);
    }

    private void detachAssessmentListener(Assessment assessment) {
        assessment.scoreProperty().removeListener(assessmentScoreListener);
    }
}
