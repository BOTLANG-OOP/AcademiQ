package com.academiq.grading;

import com.academiq.model.Assessment;

import java.util.List;

public class CurvedGrading implements GradingPolicy {

    private final double curveAmount;
    private final GradingPolicy basePolicy;

    public CurvedGrading(double curveAmount, GradingPolicy basePolicy) {
        this.curveAmount = curveAmount;
        this.basePolicy = basePolicy;
    }

    @Override
    public double computeFinalGrade(List<Assessment> assessments) {
        // TODO: delegate to basePolicy then apply curve
        return 0.0;
    }

    @Override
    public String getBreakdown(List<Assessment> assessments) {
        // TODO: delegate to basePolicy then apply curve
        return "";
    }

    @Override
    public double projectNeeded(List<Assessment> assessments, double targetGrade) {
        // TODO: delegate to basePolicy then apply curve
        return 0.0;
    }
}
