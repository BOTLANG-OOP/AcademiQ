package com.academiq.grading;

import com.academiq.model.Assessment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionIntegrationTest {

    private static final LocalDate D = LocalDate.of(2026, 1, 1);
    private static final double DELTA = 0.01;

    private static Assessment graded(String title, String category, double score, double maxScore, double weight) {
        return new Assessment(title, category, score, maxScore, weight, D);
    }

    private static Assessment ungraded(String title, String category, double maxScore, double weight) {
        return new Assessment(title, category, -1, maxScore, weight, D);
    }

    private static void fillUngraded(List<Assessment> assessments, double percentage) {
        for (Assessment a : assessments) {
            if (!a.isGraded()) {
                a.setScore(percentage / 100.0 * a.getMaxScore());
            }
        }
    }

    private static Map<String, Double> weights(String k1, double v1, String k2, double v2) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    @Test
    void weightedProjectionRoundTrip() {
        WeightedGrading policy = new WeightedGrading(weights("Exams", 0.60, "Homework", 0.40));
        List<Assessment> assessments = new ArrayList<>(Arrays.asList(
                graded("Midterm", "Exams", 78, 100, 1.0),
                ungraded("Final", "Exams", 100, 1.0),
                graded("HW1", "Homework", 95, 100, 1.0),
                ungraded("HW2", "Homework", 100, 1.0)
        ));

        double target = 85.0;
        double required = policy.projectNeeded(assessments, target);
        assertTrue(required >= 0.0 && required <= 100.0, "expected achievable, got " + required);

        fillUngraded(assessments, required);
        assertEquals(target, policy.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void pointsBasedProjectionRoundTrip() {
        PointsBasedGrading policy = new PointsBasedGrading(500);
        List<Assessment> assessments = new ArrayList<>(Arrays.asList(
                graded("Quiz 1", "Quizzes", 85, 100, 1.0),
                graded("Quiz 2", "Quizzes", 92, 100, 1.0),
                ungraded("Midterm", "Exams", 150, 1.0),
                ungraded("Final", "Exams", 150, 1.0)
        ));

        double target = 90.0;
        double required = policy.projectNeeded(assessments, target);
        assertTrue(required >= 0.0 && required <= 100.0, "expected achievable, got " + required);

        fillUngraded(assessments, required);
        assertEquals(target, policy.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void curvedWeightedProjectionRoundTrip() {
        WeightedGrading base = new WeightedGrading(weights("Exams", 0.50, "Homework", 0.50));
        CurvedGrading policy = new CurvedGrading(5.0, base);

        List<Assessment> assessments = new ArrayList<>(Arrays.asList(
                graded("Midterm", "Exams", 80, 100, 1.0),
                ungraded("Final", "Exams", 100, 1.0),
                graded("HW1", "Homework", 88, 100, 1.0),
                ungraded("HW2", "Homework", 100, 1.0)
        ));

        double target = 90.0;
        double required = policy.projectNeeded(assessments, target);
        assertTrue(required >= 0.0 && required <= 100.0, "expected achievable, got " + required);

        fillUngraded(assessments, required);
        assertEquals(target, policy.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void curvedPointsBasedProjectionRoundTrip() {
        PointsBasedGrading base = new PointsBasedGrading(400);
        CurvedGrading policy = new CurvedGrading(8.0, base);

        List<Assessment> assessments = new ArrayList<>(Arrays.asList(
                graded("HW1", "Homework", 80, 100, 1.0),
                graded("HW2", "Homework", 85, 100, 1.0),
                ungraded("Midterm", "Exams", 100, 1.0),
                ungraded("Final", "Exams", 100, 1.0)
        ));

        double target = 95.0;
        double required = policy.projectNeeded(assessments, target);
        assertTrue(required >= 0.0 && required <= 100.0, "expected achievable, got " + required);

        fillUngraded(assessments, required);
        assertEquals(target, policy.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void doubleCurvedProjectionRoundTrip() {
        WeightedGrading base = new WeightedGrading(weights("Exams", 0.60, "Homework", 0.40));
        CurvedGrading inner = new CurvedGrading(2.0, base);
        CurvedGrading policy = new CurvedGrading(3.0, inner);

        List<Assessment> assessments = new ArrayList<>(Arrays.asList(
                graded("Midterm", "Exams", 75, 100, 1.0),
                ungraded("Final", "Exams", 100, 1.0),
                graded("HW1", "Homework", 90, 100, 1.0),
                ungraded("HW2", "Homework", 100, 1.0)
        ));

        double target = 88.0;
        double required = policy.projectNeeded(assessments, target);
        assertTrue(required >= 0.0 && required <= 100.0, "expected achievable, got " + required);

        fillUngraded(assessments, required);
        assertEquals(target, policy.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void impossibleAcrossAllPolicies() {
        WeightedGrading weighted = new WeightedGrading(weights("Exams", 0.50, "Homework", 0.50));
        List<Assessment> wAssessments = Arrays.asList(
                graded("Midterm", "Exams", 10, 100, 1.0),
                ungraded("Final", "Exams", 100, 1.0),
                graded("HW1", "Homework", 20, 100, 1.0),
                ungraded("HW2", "Homework", 100, 1.0)
        );
        assertEquals(-1.0, weighted.projectNeeded(wAssessments, 95.0), DELTA);

        PointsBasedGrading points = new PointsBasedGrading(500);
        List<Assessment> pAssessments = Arrays.asList(
                graded("Quiz 1", "Q", 5, 100, 1.0),
                graded("Quiz 2", "Q", 10, 100, 1.0),
                ungraded("Midterm", "E", 150, 1.0),
                ungraded("Final", "E", 150, 1.0)
        );
        assertEquals(-1.0, points.projectNeeded(pAssessments, 95.0), DELTA);

        CurvedGrading curved = new CurvedGrading(2.0, new WeightedGrading(weights("Exams", 0.50, "Homework", 0.50)));
        List<Assessment> cAssessments = Arrays.asList(
                graded("Midterm", "Exams", 10, 100, 1.0),
                ungraded("Final", "Exams", 100, 1.0),
                graded("HW1", "Homework", 20, 100, 1.0),
                ungraded("HW2", "Homework", 100, 1.0)
        );
        assertEquals(-1.0, curved.projectNeeded(cAssessments, 99.0), DELTA);
    }

    @Test
    void alreadyAchievedAcrossAllPolicies() {
        WeightedGrading weighted = new WeightedGrading(weights("Exams", 0.50, "Homework", 0.50));
        List<Assessment> wAssessments = Arrays.asList(
                graded("Midterm", "Exams", 95, 100, 1.0),
                graded("Final", "Exams", 92, 100, 1.0),
                graded("HW1", "Homework", 98, 100, 1.0),
                graded("HW2", "Homework", 96, 100, 1.0)
        );
        assertEquals(0.0, weighted.projectNeeded(wAssessments, 80.0), DELTA);

        PointsBasedGrading points = new PointsBasedGrading(500);
        List<Assessment> pAssessments = Arrays.asList(
                graded("Quiz 1", "Q", 95, 100, 1.0),
                graded("Quiz 2", "Q", 100, 100, 1.0),
                graded("Midterm", "E", 140, 150, 1.0),
                graded("Final", "E", 145, 150, 1.0)
        );
        assertEquals(0.0, points.projectNeeded(pAssessments, 80.0), DELTA);

        CurvedGrading curved = new CurvedGrading(5.0, new WeightedGrading(weights("Exams", 0.50, "Homework", 0.50)));
        List<Assessment> cAssessments = Arrays.asList(
                graded("Midterm", "Exams", 90, 100, 1.0),
                graded("Final", "Exams", 88, 100, 1.0),
                graded("HW1", "Homework", 94, 100, 1.0),
                graded("HW2", "Homework", 92, 100, 1.0)
        );
        assertEquals(0.0, curved.projectNeeded(cAssessments, 80.0), DELTA);
    }
}
