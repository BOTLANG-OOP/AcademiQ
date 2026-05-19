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
        if (assessments == null || assessments.isEmpty()) {
            return 0.0;
        }
        double base = basePolicy.computeFinalGrade(assessments);
        double curved = base + curveAmount;
        if (curved > 100.0) return 100.0;
        if (curved < 0.0) return 0.0;
        return curved;
    }

    @Override
    public String getBreakdown(List<Assessment> assessments) {
        String baseBreakdown = basePolicy.getBreakdown(assessments);
        double finalGrade = computeFinalGrade(assessments);
        String sign = curveAmount >= 0 ? "+" : "-";
        double magnitude = Math.abs(curveAmount);
        return String.format(
            "%s%n---%nCurve applied: %s%.2f%nFinal grade (after curve): %.2f",
            baseBreakdown, sign, magnitude, finalGrade
        );
    }

    @Override
    public double projectNeeded(List<Assessment> assessments, double targetGrade) {
        if (assessments == null || assessments.isEmpty()) {
            return -1.0;
        }
        if (targetGrade > 100.0) {
            return -1.0;
        }
        if (targetGrade < 0.0) {
            return 0.0;
        }
        double adjustedTarget = targetGrade - curveAmount;
        if (adjustedTarget < 0.0) adjustedTarget = 0.0;
        return basePolicy.projectNeeded(assessments, adjustedTarget);
    }
}
