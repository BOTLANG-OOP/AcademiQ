package com.academiq.grading;

import com.academiq.model.Assessment;

import java.util.List;

public class PointsBasedGrading implements GradingPolicy {

    private final double totalPossible;

    public PointsBasedGrading(double totalPossible) {
        this.totalPossible = totalPossible;
    }

    @Override
    public double computeFinalGrade(List<Assessment> assessments) {
        // TODO
        return 0.0;
    }

    @Override
    public String getBreakdown(List<Assessment> assessments) {
        // TODO
        return "";
    }

    @Override
    public double projectNeeded(List<Assessment> assessments, double targetGrade) {
        // TODO
        return 0.0;
    }
}
