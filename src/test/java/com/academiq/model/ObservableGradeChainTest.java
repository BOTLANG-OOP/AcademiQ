package com.academiq.model;

import com.academiq.grading.PointsBasedGrading;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservableGradeChainTest {

    private static final LocalDate DATE = LocalDate.of(2026, 1, 1);

    @Test
    void scoreChangePropagatesToCumulativeGPA() {
        Student student = new Student("S", "1");
        Term term = new Term("T1", 2026, "First");
        Course course = new Course("Course", "C1", 3, new PointsBasedGrading(100));
        Assessment assessment = new Assessment("A1", "General", 50, 100, 1.0, DATE);

        course.addAssessment(assessment);
        term.addCourse(course);
        student.addTerm(term);

        AtomicInteger cumulativeFires = new AtomicInteger();
        student.cumulativeGPAProperty().addListener((obs, oldV, newV) -> cumulativeFires.incrementAndGet());

        double before = student.getCumulativeGPA();
        assessment.setScore(100);

        assertTrue(cumulativeFires.get() > 0, "cumulative GPA listener should fire on score change");
        assertEquals(course.getFinalGrade(), 100.0, 1e-9);
        assertTrue(student.getCumulativeGPA() > before);
    }
}
