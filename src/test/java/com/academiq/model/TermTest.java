package com.academiq.model;

import com.academiq.grading.PointsBasedGrading;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermTest {

    @Test
    void sameDayOverlapDetectedInTerm() {
        Term term = new Term("Spring", 2026, "First");
        Course courseA = new Course("Course A", "A101", 3, new PointsBasedGrading(100));
        Course courseB = new Course("Course B", "B101", 3, new PointsBasedGrading(100));

        courseA.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 30), "R101"));
        courseB.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "R102"));

        term.addCourse(courseA);
        term.addCourse(courseB);

        List<ConflictRecord> conflicts = term.detectConflicts();

        assertEquals(1, conflicts.size());
        assertTrue(conflicts.get(0).getSlotA().overlapsWith(conflicts.get(0).getSlotB()));
    }

    @Test
    void adjacentSameDayNoConflictInTerm() {
        Term term = new Term("Spring", 2026, "First");
        Course courseA = new Course("Course A", "A101", 3, new PointsBasedGrading(100));
        Course courseB = new Course("Course B", "B101", 3, new PointsBasedGrading(100));

        courseA.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), "R101"));
        courseB.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "R102"));

        term.addCourse(courseA);
        term.addCourse(courseB);

        List<ConflictRecord> conflicts = term.detectConflicts();

        assertEquals(0, conflicts.size());
    }

    @Test
    void differentDayNoConflictInTerm() {
        Term term = new Term("Spring", 2026, "First");
        Course courseA = new Course("Course A", "A101", 3, new PointsBasedGrading(100));
        Course courseB = new Course("Course B", "B101", 3, new PointsBasedGrading(100));

        courseA.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), "R101"));
        courseB.addTimeSlot(new TimeSlot(DayOfWeek.TUESDAY, LocalTime.of(8, 30), LocalTime.of(9, 30), "R102"));

        term.addCourse(courseA);
        term.addCourse(courseB);

        List<ConflictRecord> conflicts = term.detectConflicts();

        assertEquals(0, conflicts.size());
    }

    @Test
    void multipleConflictsReturnedInTerm() {
        Term term = new Term("Spring", 2026, "First");
        Course courseA = new Course("Course A", "A101", 3, new PointsBasedGrading(100));
        Course courseB = new Course("Course B", "B101", 3, new PointsBasedGrading(100));

        courseA.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 30), "R101"));
        courseA.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), "R101"));

        courseB.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 15), "R102"));
        courseB.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 30), "R102"));

        term.addCourse(courseA);
        term.addCourse(courseB);

        List<ConflictRecord> conflicts = term.detectConflicts();

        assertEquals(2, conflicts.size());
        assertTrue(conflicts.stream().allMatch(conflict -> conflict.getSlotA().overlapsWith(conflict.getSlotB())));
    }
}
