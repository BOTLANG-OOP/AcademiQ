package com.academiq;

import com.academiq.grading.PointsBasedGrading;
import com.academiq.grading.WeightedGrading;
import com.academiq.model.Assessment;
import com.academiq.model.Course;
import com.academiq.model.Student;
import com.academiq.model.Term;
import com.academiq.model.TimeSlot;
import com.academiq.persistence.SqliteDataStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EndToEndIntegrationTest {

    @Test
    void createPopulateGradeProjectScheduleConflictRestartVerify() throws Exception {
        Student original = new Student("E2E Student", "stu-e2e");
        Term term = new Term("Fall 2026", 2026, "Fall");

        Course pointsCourse = new Course("History", "HIST101", 3, new PointsBasedGrading(200.0));
        pointsCourse.addAssessment(new Assessment("Quiz 1", "Quiz", 45.0, 50.0, 1.0, LocalDate.of(2026, 9, 1)));
        pointsCourse.addAssessment(new Assessment("Quiz 2", "Quiz", 40.0, 50.0, 1.0, LocalDate.of(2026, 9, 8)));
        pointsCourse.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), "R101"));

        Course weightedCourse = new Course("Physics", "PHYS101", 4,
                new WeightedGrading(Map.of("Exams", 0.7, "Labs", 0.3)));
        weightedCourse.addAssessment(new Assessment("Midterm", "Exams", 75.0, 100.0, 1.0, LocalDate.of(2026, 9, 2)));
        weightedCourse.addAssessment(new Assessment("Lab 1", "Labs", 18.0, 20.0, 1.0, LocalDate.of(2026, 9, 3)));
        Assessment finalExam = new Assessment("Final", "Exams", -1.0, 100.0, 1.0, LocalDate.of(2026, 12, 1));
        weightedCourse.addAssessment(finalExam);
        weightedCourse.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 30), "R102"));

        term.addCourse(pointsCourse);
        term.addCourse(weightedCourse);
        original.addTerm(term);

        // grade test
        assertEquals(85.0, pointsCourse.getFinalGrade(), 1e-9);
        assertEquals(79.5, weightedCourse.getFinalGrade(), 1e-9);

        double initialExpectedTermGpa = ((85.0 * 3) + (79.5 * 4)) / 7.0 / 100.0 * 5.0;
        assertEquals(initialExpectedTermGpa, term.getTermGPA(), 1e-9);
        assertEquals(initialExpectedTermGpa, original.getCumulativeGPA(), 1e-9);


        double pointsNeeded = pointsCourse.whatDoINeed(90.0);
        assertEquals(95.0, pointsNeeded, 1e-9);

        double weightedNeeded = weightedCourse.whatDoINeed(80.0);
        assertEquals(76.42857142857143, weightedNeeded, 1e-9);
        finalExam.setScore(weightedNeeded);
        assertEquals(80.0, weightedCourse.getFinalGrade(), 1e-9);

        double finalExpectedTermGpa = ((85.0 * 3) + (80.0 * 4)) / 7.0 / 100.0 * 5.0;
        assertEquals(finalExpectedTermGpa, term.getTermGPA(), 1e-9);
        assertEquals(finalExpectedTermGpa, original.getCumulativeGPA(), 1e-9);

        // schedule & conflict end to end test
        assertEquals(1, term.detectConflicts().size());
        assertTrue(term.detectConflicts().get(0).getSlotA().overlapsWith(term.detectConflicts().get(0).getSlotB()));

        Path tempDb = Files.createTempFile("academiq-e2e", ".db");
        tempDb.toFile().deleteOnExit();

        try (SqliteDataStore store = new SqliteDataStore(tempDb.toString())) {
            store.save(original);
        }

        try (SqliteDataStore store = new SqliteDataStore(tempDb.toString())) {
            Student loaded = store.loadStudent(original.getId());
            assertNotNull(loaded);
            assertEquals(original.getName(), loaded.getName());
            assertEquals(original.getTerms().size(), loaded.getTerms().size());

            Term loadedTerm = loaded.getTerms().get(0);
            assertEquals(term.getName(), loadedTerm.getName());
            assertEquals(term.getYear(), loadedTerm.getYear());
            assertEquals(term.getSemester(), loadedTerm.getSemester());
            assertEquals(2, loadedTerm.getCourses().size());

            Course loadedPoints = loadedTerm.getCourses().stream()
                    .filter(c -> "HIST101".equals(c.getCode()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(2, loadedPoints.getAssessments().size());
            assertEquals(1, loadedPoints.getTimeSlots().size());
            assertEquals(85.0, loadedPoints.getFinalGrade(), 1e-9);

            Course loadedWeighted = loadedTerm.getCourses().stream()
                    .filter(c -> "PHYS101".equals(c.getCode()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(3, loadedWeighted.getAssessments().size());
            assertEquals(1, loadedWeighted.getTimeSlots().size());
            assertEquals(80.0, loadedWeighted.getFinalGrade(), 1e-9);

            assertEquals(finalExpectedTermGpa, loadedTerm.getTermGPA(), 1e-9);
            assertEquals(finalExpectedTermGpa, loaded.getCumulativeGPA(), 1e-9);
            assertEquals(1, loadedTerm.detectConflicts().size());
        }
    }
}
