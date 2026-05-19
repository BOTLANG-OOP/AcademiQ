package com.academiq.grading;

import com.academiq.model.Assessment;
import com.academiq.model.Course;
import com.academiq.model.Term;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeCaseHardeningTest {

    private static final LocalDate D = LocalDate.of(2026, 1, 1);
    private static final double DELTA = 0.01;

    private static Assessment graded(String title, String category, double score, double maxScore, double weight) {
        return new Assessment(title, category, score, maxScore, weight, D);
    }

    private static Assessment ungraded(String title, String category, double maxScore, double weight) {
        return new Assessment(title, category, -1, maxScore, weight, D);
    }

    private static Map<String, Double> weights(Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Double) kv[i + 1]);
        }
        return m;
    }

    @Nested
    class NullInputs {

        @Test
        void weightedHandlesNullAssessments() {
            WeightedGrading p = new WeightedGrading(weights("Exams", 0.5, "Homework", 0.5));
            assertDoesNotThrow(() -> p.computeFinalGrade(null));
            assertDoesNotThrow(() -> p.getBreakdown(null));
            assertDoesNotThrow(() -> p.projectNeeded(null, 85.0));

            assertEquals(0.0, p.computeFinalGrade(null), DELTA);
            String breakdown = p.getBreakdown(null);
            assertNotNull(breakdown);
            assertEquals(-1.0, p.projectNeeded(null, 85.0), DELTA);
        }

        @Test
        void pointsBasedHandlesNullAssessments() {
            PointsBasedGrading p = new PointsBasedGrading(100);
            assertDoesNotThrow(() -> p.computeFinalGrade(null));
            assertDoesNotThrow(() -> p.getBreakdown(null));
            assertDoesNotThrow(() -> p.projectNeeded(null, 85.0));

            assertEquals(0.0, p.computeFinalGrade(null), DELTA);
            assertNotNull(p.getBreakdown(null));
            assertEquals(-1.0, p.projectNeeded(null, 85.0), DELTA);
        }

        @Test
        void curvedHandlesNullAssessments() {
            CurvedGrading p = new CurvedGrading(5.0, new PointsBasedGrading(100));
            assertDoesNotThrow(() -> p.computeFinalGrade(null));
            assertDoesNotThrow(() -> p.getBreakdown(null));
            assertDoesNotThrow(() -> p.projectNeeded(null, 85.0));

            assertEquals(0.0, p.computeFinalGrade(null), DELTA);
            assertNotNull(p.getBreakdown(null));
            assertEquals(-1.0, p.projectNeeded(null, 85.0), DELTA);
        }

        @Test
        void emptyListHandledByAllPolicies() {
            List<Assessment> empty = Collections.emptyList();

            WeightedGrading w = new WeightedGrading(weights("Exams", 0.5, "Homework", 0.5));
            assertEquals(0.0, w.computeFinalGrade(empty), DELTA);
            assertNotNull(w.getBreakdown(empty));
            assertEquals(-1.0, w.projectNeeded(empty, 85.0), DELTA);

            PointsBasedGrading p = new PointsBasedGrading(100);
            assertEquals(0.0, p.computeFinalGrade(empty), DELTA);
            assertNotNull(p.getBreakdown(empty));
            assertEquals(-1.0, p.projectNeeded(empty, 85.0), DELTA);

            CurvedGrading c = new CurvedGrading(5.0, p);
            assertEquals(0.0, c.computeFinalGrade(empty), DELTA);
            assertNotNull(c.getBreakdown(empty));
            assertEquals(-1.0, c.projectNeeded(empty, 85.0), DELTA);
        }
    }

    @Nested
    class ZeroWeights {

        @Test
        void zeroWeightCategoryExcludedFromFinal() {
            WeightedGrading p = new WeightedGrading(weights("Exams", 0.0, "Homework", 1.0));
            List<Assessment> as = Arrays.asList(
                    graded("E1", "Exams", 50, 100, 1.0),
                    graded("H1", "Homework", 90, 100, 1.0)
            );
            assertEquals(90.0, p.computeFinalGrade(as), DELTA);
        }

        @Test
        void allCategoriesZeroWeightReturnsZero() {
            WeightedGrading p = new WeightedGrading(weights("Exams", 0.0, "Homework", 0.0));
            List<Assessment> as = Arrays.asList(
                    graded("E1", "Exams", 80, 100, 1.0),
                    graded("H1", "Homework", 90, 100, 1.0)
            );
            assertDoesNotThrow(() -> p.computeFinalGrade(as));
            assertEquals(0.0, p.computeFinalGrade(as), DELTA);
        }

        @Test
        void zeroWeightCategoryDoesNotDivideByZero() {
            WeightedGrading p = new WeightedGrading(weights("Exams", 0.0));
            List<Assessment> as = Collections.singletonList(graded("E1", "Exams", 80, 100, 1.0));
            assertDoesNotThrow(() -> p.computeFinalGrade(as));
            assertEquals(0.0, p.computeFinalGrade(as), DELTA);
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void allScoresZeroReturnsZero() {
            List<Assessment> as = Arrays.asList(
                    graded("E1", "Exams", 0, 100, 1.0),
                    graded("H1", "Homework", 0, 100, 1.0)
            );

            WeightedGrading w = new WeightedGrading(weights("Exams", 0.5, "Homework", 0.5));
            assertEquals(0.0, w.computeFinalGrade(as), DELTA);

            PointsBasedGrading p = new PointsBasedGrading(200);
            assertEquals(0.0, p.computeFinalGrade(as), DELTA);
        }

        @Test
        void singleAssessmentHandledByAllPolicies() {
            List<Assessment> single = Collections.singletonList(graded("E1", "Exams", 75, 100, 1.0));

            WeightedGrading w = new WeightedGrading(weights("Exams", 1.0));
            assertEquals(75.0, w.computeFinalGrade(single), DELTA);

            PointsBasedGrading p = new PointsBasedGrading(100);
            assertEquals(75.0, p.computeFinalGrade(single), DELTA);

            CurvedGrading c = new CurvedGrading(5.0, p);
            assertEquals(80.0, c.computeFinalGrade(single), DELTA);
        }

        @Test
        void maxScoreZeroMixedDoesNotBreakPolicies() {
            List<Assessment> as = Arrays.asList(
                    graded("Zero", "Exams", 0, 0, 1.0),
                    graded("Normal", "Exams", 80, 100, 1.0)
            );

            WeightedGrading w = new WeightedGrading(weights("Exams", 1.0));
            assertDoesNotThrow(() -> w.computeFinalGrade(as));
            assertDoesNotThrow(() -> w.getBreakdown(as));

            PointsBasedGrading p = new PointsBasedGrading(100);
            assertDoesNotThrow(() -> p.computeFinalGrade(as));
            assertDoesNotThrow(() -> p.getBreakdown(as));
            assertEquals(80.0, p.computeFinalGrade(as), DELTA);
        }

        @Test
        void veryLargeNumbersComputeCleanly() {
            List<Assessment> as = Collections.singletonList(
                    graded("Big", "Exams", 999999, 999999, 1.0)
            );

            WeightedGrading w = new WeightedGrading(weights("Exams", 1.0));
            assertEquals(100.0, w.computeFinalGrade(as), DELTA);

            PointsBasedGrading p = new PointsBasedGrading(999999);
            assertEquals(100.0, p.computeFinalGrade(as), DELTA);
        }

        @Test
        void negativeTargetReturnsZero() {
            List<Assessment> as = Arrays.asList(
                    graded("E1", "Exams", 80, 100, 1.0),
                    ungraded("Final", "Exams", 100, 1.0)
            );

            WeightedGrading w = new WeightedGrading(weights("Exams", 1.0));
            assertEquals(0.0, w.projectNeeded(as, -10.0), DELTA);

            PointsBasedGrading p = new PointsBasedGrading(200);
            assertEquals(0.0, p.projectNeeded(as, -10.0), DELTA);

            CurvedGrading c = new CurvedGrading(5.0, p);
            assertEquals(0.0, c.projectNeeded(as, -10.0), DELTA);
        }

        @Test
        void targetAboveHundredReturnsNegativeOne() {
            List<Assessment> as = Arrays.asList(
                    graded("E1", "Exams", 80, 100, 1.0),
                    ungraded("Final", "Exams", 100, 1.0)
            );

            WeightedGrading w = new WeightedGrading(weights("Exams", 1.0));
            assertEquals(-1.0, w.projectNeeded(as, 150.0), DELTA);

            PointsBasedGrading p = new PointsBasedGrading(200);
            assertEquals(-1.0, p.projectNeeded(as, 150.0), DELTA);

            CurvedGrading c = new CurvedGrading(5.0, p);
            assertEquals(-1.0, c.projectNeeded(as, 150.0), DELTA);
        }
    }

    @Nested
    class CourseEdgeCases {

        @Test
        void courseWithNoAssessmentsReturnsZeroGrade() {
            Course course = new Course("Empty", "E101", 3, new PointsBasedGrading(100));
            assertDoesNotThrow(course::getFinalGrade);
            assertEquals(0.0, course.getFinalGrade(), DELTA);
            assertDoesNotThrow(course::getGradeBreakdown);
        }

        @Test
        void courseConstructorRejectsNullGradingPolicy() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Course("X", "X101", 3, null)
            );
            assertTrue(ex.getMessage().contains("GradingPolicy"));
        }
    }

    @Nested
    class TermEdgeCases {

        @Test
        void termWithNoCoursesHasZeroGPA() {
            Term term = new Term("Empty", 2026, "First");
            assertEquals(0.0, term.getTermGPA(), DELTA);
        }

        @Test
        void termWithAllZeroUnitCoursesHasZeroGPA() {
            Term term = new Term("Zero", 2026, "First");
            Course a = new Course("A", "A1", 0, new PointsBasedGrading(100));
            Course b = new Course("B", "B1", 0, new PointsBasedGrading(100));
            term.addCourse(a);
            term.addCourse(b);
            assertDoesNotThrow(term::getTermGPA);
            assertEquals(0.0, term.getTermGPA(), DELTA);
        }
    }
}
