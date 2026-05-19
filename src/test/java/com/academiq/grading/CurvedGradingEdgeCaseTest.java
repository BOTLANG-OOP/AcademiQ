package com.academiq.grading;

import com.academiq.model.Assessment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurvedGradingEdgeCaseTest {

    private static final LocalDate D = LocalDate.of(2026, 1, 1);
    private static final double DELTA = 0.01;

    private static Assessment graded(String title, String category, double score, double maxScore, double weight) {
        return new Assessment(title, category, score, maxScore, weight, D);
    }

    private static Assessment ungraded(String title, String category, double maxScore, double weight) {
        return new Assessment(title, category, -1, maxScore, weight, D);
    }

    private static Map<String, Double> twoCategoryWeights() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("Exams", 0.50);
        m.put("Homework", 0.50);
        return m;
    }

    /** Two-category WeightedGrading: Exams 50%, Homework 50%. */
    private static WeightedGrading standardWeighted() {
        return new WeightedGrading(twoCategoryWeights());
    }

    /**
     * Standard two-category assessment list: one graded and one ungraded per category.
     * Midterm 80/100, Final ungraded; HW1 90/100, HW2 ungraded. All weight 1.
     */
    private static List<Assessment> standardWeightedAssessments() {
        return new ArrayList<>(Arrays.asList(
                graded("Midterm", "Exams", 80, 100, 1.0),
                ungraded("Final", "Exams", 100, 1.0),
                graded("HW1", "Homework", 90, 100, 1.0),
                ungraded("HW2", "Homework", 100, 1.0)
        ));
    }

    /** PointsBasedGrading with totalPossible=200. */
    private static PointsBasedGrading standardPoints() {
        return new PointsBasedGrading(200);
    }

    /**
     * Standard PointsBased assessment list: one graded (75/100) and one ungraded (max 100).
     */
    private static List<Assessment> standardPointsAssessments() {
        return new ArrayList<>(Arrays.asList(
                graded("Quiz", "Quizzes", 75, 100, 1.0),
                ungraded("Final", "Exams", 100, 1.0)
        ));
    }

    private static void fillUngraded(List<Assessment> assessments, double percentage) {
        for (Assessment a : assessments) {
            if (!a.isGraded()) {
                a.setScore(percentage / 100.0 * a.getMaxScore());
            }
        }
    }

    @Test
    void curveCapAt100() {
        // Base PointsBasedGrading returns 97.0 (97/100).
        PointsBasedGrading base = new PointsBasedGrading(100);
        List<Assessment> assessments = Collections.singletonList(
                graded("Only", "X", 97, 100, 1.0)
        );
        CurvedGrading curved = new CurvedGrading(5.0, base);

        assertEquals(100.0, curved.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void curveFloorAt0() {
        // Base PointsBasedGrading returns 3.0 (3/100).
        PointsBasedGrading base = new PointsBasedGrading(100);
        List<Assessment> assessments = Collections.singletonList(
                graded("Only", "X", 3, 100, 1.0)
        );
        CurvedGrading curved = new CurvedGrading(-10.0, base);

        assertEquals(0.0, curved.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void zeroCurveIsPassthrough() {
        WeightedGrading base = standardWeighted();
        CurvedGrading curved = new CurvedGrading(0.0, base);
        List<Assessment> assessments = standardWeightedAssessments();

        assertEquals(base.computeFinalGrade(assessments),
                curved.computeFinalGrade(assessments), DELTA);
        assertEquals(base.projectNeeded(assessments, 85.0),
                curved.projectNeeded(assessments, 85.0), DELTA);
        assertEquals(base.projectNeeded(assessments, 75.0),
                curved.projectNeeded(assessments, 75.0), DELTA);
    }

    @Test
    void negativeCurveProjection() {
        PointsBasedGrading base = standardPoints();
        CurvedGrading curved = new CurvedGrading(-5.0, base);
        List<Assessment> assessments = standardPointsAssessments();

        double target = 80.0;
        double curvedRequired = curved.projectNeeded(assessments, target);
        double baseRequired = base.projectNeeded(assessments, 85.0);

        // CurvedGrading delegates with target = 85.0
        assertEquals(baseRequired, curvedRequired, DELTA);

        // Specifically: required = (85/100 * 200 - 75) / (200-100) * 100 = 95
        assertEquals(95.0, curvedRequired, DELTA);

        // Round-trip: filling in the returned score should yield target through curved policy.
        fillUngraded(assessments, curvedRequired);
        assertEquals(target, curved.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void stackedCurvesAddUp_base80() {
        // Base = 80.0
        PointsBasedGrading base = new PointsBasedGrading(100);
        List<Assessment> assessments = Collections.singletonList(
                graded("Only", "X", 80, 100, 1.0)
        );
        CurvedGrading stacked = new CurvedGrading(3.0, new CurvedGrading(4.0, base));

        assertEquals(87.0, stacked.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void stackedCurvesAddUp_base95Capped() {
        // Base = 95.0; 95 + 4 + 3 = 102 → cap to 100.
        PointsBasedGrading base = new PointsBasedGrading(100);
        List<Assessment> assessments = Collections.singletonList(
                graded("Only", "X", 95, 100, 1.0)
        );
        CurvedGrading stacked = new CurvedGrading(3.0, new CurvedGrading(4.0, base));

        assertEquals(100.0, stacked.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void stackedCurvesProjection() {
        WeightedGrading base = standardWeighted();
        CurvedGrading inner = new CurvedGrading(4.0, base);
        CurvedGrading outer = new CurvedGrading(3.0, inner);

        List<Assessment> assessments = standardWeightedAssessments();
        double target = 90.0;

        double outerRequired = outer.projectNeeded(assessments, target);
        double innerRequired = inner.projectNeeded(assessments, 87.0);
        double baseRequired = base.projectNeeded(assessments, 83.0);

        // Outer delegates target 87 to inner, which delegates 83 to base.
        assertEquals(innerRequired, outerRequired, DELTA);
        assertEquals(baseRequired, outerRequired, DELTA);
        assertTrue(outerRequired >= 0.0 && outerRequired <= 100.0,
                "expected achievable, got " + outerRequired);

        // Round-trip: fill ungraded with required score, verify outer returns target.
        fillUngraded(assessments, outerRequired);
        assertEquals(target, outer.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void curvedWithEmptyAssessments() {
        WeightedGrading base = standardWeighted();
        CurvedGrading curved = new CurvedGrading(10.0, base);
        List<Assessment> empty = new ArrayList<>();

        // base returns 0.0 + 10.0 curve = 10.0 (in 0-100 range)
        assertEquals(10.0, curved.computeFinalGrade(empty), DELTA);

        // projectNeeded must not throw.
        assertDoesNotThrow(() -> curved.projectNeeded(empty, 80.0));
    }

    @Test
    void curvedWithAllUngradedAssessments() {
        WeightedGrading base = standardWeighted();
        double curveAmount = 5.0;
        CurvedGrading curved = new CurvedGrading(curveAmount, base);

        List<Assessment> assessments = new ArrayList<>(Arrays.asList(
                ungraded("Midterm", "Exams", 100, 1.0),
                ungraded("Final", "Exams", 100, 1.0),
                ungraded("HW1", "Homework", 100, 1.0),
                ungraded("HW2", "Homework", 100, 1.0)
        ));

        // No graded → base = 0.0, + curve = 5.0.
        assertEquals(curveAmount, curved.computeFinalGrade(assessments), DELTA);

        // projectNeeded(target=80): delegates with target = 75 to base.
        // With all ungraded and equal weights, base returns adjusted target = 75.
        double target = 80.0;
        double required = curved.projectNeeded(assessments, target);
        assertEquals(target - curveAmount, required, DELTA);
    }

    @Test
    void projectionNeverReturnsNegativeScore() {
        // Current grade = 50 (all graded), target = 10, curve = 20.
        // Adjusted target = -10 → clamped to 0; current already exceeds 0 → returns 0.0.
        WeightedGrading base = standardWeighted();
        CurvedGrading curved = new CurvedGrading(20.0, base);

        List<Assessment> assessments = Arrays.asList(
                graded("Midterm", "Exams", 50, 100, 1.0),
                graded("Final", "Exams", 50, 100, 1.0),
                graded("HW1", "Homework", 50, 100, 1.0),
                graded("HW2", "Homework", 50, 100, 1.0)
        );

        double required = curved.projectNeeded(assessments, 10.0);
        assertTrue(required >= 0.0, "projection must not be negative, got " + required);
        assertEquals(0.0, required, DELTA);
    }

    @Test
    void breakdownShowsCurveInfo() {
        WeightedGrading base = standardWeighted();
        double curveAmount = 5.0;
        CurvedGrading curved = new CurvedGrading(curveAmount, base);
        List<Assessment> assessments = standardWeightedAssessments();

        String breakdown = curved.getBreakdown(assessments);
        double finalGrade = curved.computeFinalGrade(assessments);
        String baseBreakdown = base.getBreakdown(assessments);

        assertTrue(breakdown.contains(String.format("%.2f", curveAmount)),
                "breakdown should mention curve amount: " + breakdown);
        assertTrue(breakdown.toLowerCase().contains("curve"),
                "breakdown should mention 'curve': " + breakdown);
        assertTrue(breakdown.contains(String.format("%.2f", finalGrade)),
                "breakdown should mention adjusted final grade: " + breakdown);
        assertTrue(breakdown.contains(baseBreakdown),
                "breakdown should include base policy breakdown: " + breakdown);
    }
}
