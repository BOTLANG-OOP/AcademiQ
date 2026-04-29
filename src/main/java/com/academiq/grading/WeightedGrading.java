package com.academiq.grading;

import com.academiq.model.Assessment;

import java.util.List;
import java.util.Map;

public class WeightedGrading implements GradingPolicy {

    private final Map<String, Double> categoryWeights;

    public WeightedGrading(Map<String, Double> categoryWeights) {
        this.categoryWeights = categoryWeights;
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
