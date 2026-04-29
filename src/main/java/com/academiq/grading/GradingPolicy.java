package com.academiq.grading;

import com.academiq.model.Assessment;
import java.util.List;

/**
 * Strategy interface for grade computation.
 * Each Course delegates all grade math to its GradingPolicy.
 *
 * Implementations: WeightedGrading, PointsBasedGrading, CurvedGrading (decorator).
 */
public interface GradingPolicy {

    /**
     * Compute the final grade (0-100) from the given assessments.
     */
    double computeFinalGrade(List<Assessment> assessments);

    /**
     * Return a human-readable breakdown of how the grade was computed.
     * Format is up to the implementation.
     */
    String getBreakdown(List<Assessment> assessments);

    /**
     * Given a target final grade, compute the score needed on remaining
     * (ungraded) assessments to achieve it.
     *
     * @param assessments   all assessments, including future ones with score = -1
     * @param targetGrade   desired final grade (0-100)
     * @return required score (0-100), or -1.0 if impossible
     */
    double projectNeeded(List<Assessment> assessments, double targetGrade);
}
