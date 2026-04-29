package com.academiq.model;

public class ConflictRecord {

    private final Course courseA;
    private final Course courseB;
    private final TimeSlot slotA;
    private final TimeSlot slotB;

    public ConflictRecord(Course courseA, Course courseB, TimeSlot slotA, TimeSlot slotB) {
        this.courseA = courseA;
        this.courseB = courseB;
        this.slotA = slotA;
        this.slotB = slotB;
    }

    public Course getCourseA() {
        return courseA;
    }

    public Course getCourseB() {
        return courseB;
    }

    public TimeSlot getSlotA() {
        return slotA;
    }

    public TimeSlot getSlotB() {
        return slotB;
    }
}
