package com.academiq.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSlotTest {

    @Test
    void sameDayOverlapDetected() {
        TimeSlot first = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 30), "R101");
        TimeSlot second = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "R102");

        assertTrue(first.overlapsWith(second));
        assertTrue(second.overlapsWith(first));
    }

    @Test
    void adjacentSameDayDoesNotOverlap() {
        TimeSlot first = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), "R101");
        TimeSlot second = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), "R102");

        assertFalse(first.overlapsWith(second));
        assertFalse(second.overlapsWith(first));
    }

    @Test
    void differentDayDoesNotOverlap() {
        TimeSlot first = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), "R101");
        TimeSlot second = new TimeSlot(DayOfWeek.TUESDAY, LocalTime.of(8, 30), LocalTime.of(9, 30), "R102");

        assertFalse(first.overlapsWith(second));
        assertFalse(second.overlapsWith(first));
    }
}
