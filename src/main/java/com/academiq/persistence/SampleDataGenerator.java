package com.academiq.persistence;

import com.academiq.grading.CurvedGrading;
import com.academiq.grading.PointsBasedGrading;
import com.academiq.grading.WeightedGrading;
import com.academiq.model.Assessment;
import com.academiq.model.Course;
import com.academiq.model.Student;
import com.academiq.model.Term;
import com.academiq.model.TimeSlot;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class SampleDataGenerator {

    private static final String DB_FILE = "academiq-demo.db";

    public static void main(String[] args) {
        File existing = new File(DB_FILE);
        if (existing.exists() && !existing.delete()) {
            System.err.println("Failed to delete existing " + DB_FILE);
            return;
        }

        Student student = new Student("Student", "default-student");

        student.addTerm(buildTerm1());
        student.addTerm(buildTerm2());

        try (SqliteDataStore store = new SqliteDataStore(DB_FILE)) {
            store.save(student);
        }

        System.out.println("Sample database created: academiq-demo.db");
        System.out.println("Student: Student (default-student)");
        System.out.println("Term 1: 1st Semester 2025 — 3 courses, all fully graded");
        System.out.println("Term 2: 2nd Semester 2026 — 3 courses, some ungraded (projection demo)");
        System.out.println("Schedule conflict: IT302 vs IT303 on Friday 08:00-09:30");
    }

    private static Term buildTerm1() {
        Term term = new Term("1st Semester", 2025, "First");

        Map<String, Double> dsWeights = new LinkedHashMap<>();
        dsWeights.put("Exams", 0.40);
        dsWeights.put("Quizzes", 0.25);
        dsWeights.put("Projects", 0.20);
        dsWeights.put("Homework", 0.15);
        Course ds = new Course("Data Structures", "IT201", 3, new WeightedGrading(dsWeights));
        ds.addAssessment(new Assessment("Midterm Exam", "Exams", 82, 100, 0.4, LocalDate.of(2025, 10, 15)));
        ds.addAssessment(new Assessment("Final Exam", "Exams", 88, 100, 0.6, LocalDate.of(2025, 12, 18)));
        ds.addAssessment(new Assessment("Quiz 1", "Quizzes", 9, 10, 1.0, LocalDate.of(2025, 9, 20)));
        ds.addAssessment(new Assessment("Quiz 2", "Quizzes", 7, 10, 1.0, LocalDate.of(2025, 10, 25)));
        ds.addAssessment(new Assessment("Quiz 3", "Quizzes", 10, 10, 1.0, LocalDate.of(2025, 11, 15)));
        ds.addAssessment(new Assessment("Linked List Project", "Projects", 92, 100, 1.0, LocalDate.of(2025, 11, 1)));
        ds.addAssessment(new Assessment("HW1", "Homework", 48, 50, 1.0, LocalDate.of(2025, 9, 10)));
        ds.addAssessment(new Assessment("HW2", "Homework", 45, 50, 1.0, LocalDate.of(2025, 10, 5)));
        ds.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 30), "CL-301"));
        ds.addTimeSlot(new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(9, 30), "CL-301"));
        term.addCourse(ds);

        Course dm = new Course("Discrete Mathematics", "MATH202", 3, new PointsBasedGrading(500));
        dm.addAssessment(new Assessment("Exam 1", "Exams", 120, 150, 1.0, LocalDate.of(2025, 10, 10)));
        dm.addAssessment(new Assessment("Exam 2", "Exams", 135, 150, 1.0, LocalDate.of(2025, 12, 15)));
        dm.addAssessment(new Assessment("Problem Set 1", "Problem Sets", 45, 50, 1.0, LocalDate.of(2025, 9, 15)));
        dm.addAssessment(new Assessment("Problem Set 2", "Problem Sets", 42, 50, 1.0, LocalDate.of(2025, 10, 20)));
        dm.addAssessment(new Assessment("Problem Set 3", "Problem Sets", 48, 50, 1.0, LocalDate.of(2025, 11, 10)));
        dm.addAssessment(new Assessment("Problem Set 4", "Problem Sets", 40, 50, 1.0, LocalDate.of(2025, 12, 1)));
        dm.addTimeSlot(new TimeSlot(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 30), "MH-205"));
        dm.addTimeSlot(new TimeSlot(DayOfWeek.THURSDAY, LocalTime.of(10, 0), LocalTime.of(11, 30), "MH-205"));
        term.addCourse(dm);

        Map<String, Double> oopWeights = new LinkedHashMap<>();
        oopWeights.put("Exams", 0.50);
        oopWeights.put("Labs", 0.30);
        oopWeights.put("Participation", 0.20);
        Course oop = new Course("Object-Oriented Programming", "IT202", 3,
                new CurvedGrading(3.0, new WeightedGrading(oopWeights)));
        oop.addAssessment(new Assessment("Midterm", "Exams", 75, 100, 0.4, LocalDate.of(2025, 10, 12)));
        oop.addAssessment(new Assessment("Final", "Exams", 83, 100, 0.6, LocalDate.of(2025, 12, 20)));
        oop.addAssessment(new Assessment("Lab 1", "Labs", 95, 100, 1.0, LocalDate.of(2025, 9, 25)));
        oop.addAssessment(new Assessment("Lab 2", "Labs", 88, 100, 1.0, LocalDate.of(2025, 10, 30)));
        oop.addAssessment(new Assessment("Lab 3", "Labs", 90, 100, 1.0, LocalDate.of(2025, 11, 20)));
        oop.addAssessment(new Assessment("Attendance", "Participation", 85, 100, 1.0, LocalDate.of(2025, 12, 20)));
        oop.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 30), "CL-302"));
        oop.addTimeSlot(new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(11, 30), "CL-302"));
        term.addCourse(oop);

        return term;
    }

    private static Term buildTerm2() {
        Term term = new Term("2nd Semester", 2026, "Second");

        Map<String, Double> dbWeights = new LinkedHashMap<>();
        dbWeights.put("Exams", 0.35);
        dbWeights.put("Projects", 0.40);
        dbWeights.put("Quizzes", 0.25);
        Course db = new Course("Database Systems", "IT301", 3, new WeightedGrading(dbWeights));
        db.addAssessment(new Assessment("Midterm", "Exams", 78, 100, 0.5, LocalDate.of(2026, 3, 15)));
        db.addAssessment(new Assessment("Final", "Exams", -1, 100, 0.5, LocalDate.of(2026, 6, 10)));
        db.addAssessment(new Assessment("ER Diagram Project", "Projects", 90, 100, 0.5, LocalDate.of(2026, 3, 1)));
        db.addAssessment(new Assessment("SQL Queries Project", "Projects", -1, 100, 0.5, LocalDate.of(2026, 5, 20)));
        db.addAssessment(new Assessment("Quiz 1", "Quizzes", 8, 10, 1.0, LocalDate.of(2026, 2, 20)));
        db.addAssessment(new Assessment("Quiz 2", "Quizzes", 9, 10, 1.0, LocalDate.of(2026, 4, 1)));
        db.addTimeSlot(new TimeSlot(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(9, 30), "CL-401"));
        db.addTimeSlot(new TimeSlot(DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(9, 30), "CL-401"));
        term.addCourse(db);

        Course web = new Course("Web Development", "IT302", 3, new PointsBasedGrading(400));
        web.addAssessment(new Assessment("HTML/CSS Project", "Projects", 95, 100, 1.0, LocalDate.of(2026, 2, 28)));
        web.addAssessment(new Assessment("JavaScript Exam", "Exams", 82, 100, 1.0, LocalDate.of(2026, 3, 20)));
        web.addAssessment(new Assessment("React Project", "Projects", -1, 100, 1.0, LocalDate.of(2026, 5, 15)));
        web.addAssessment(new Assessment("Final Exam", "Exams", -1, 100, 1.0, LocalDate.of(2026, 6, 12)));
        web.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(14, 30), "CL-301"));
        web.addTimeSlot(new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(14, 30), "CL-301"));
        web.addTimeSlot(new TimeSlot(DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(9, 30), "CL-303"));
        term.addCourse(web);

        Map<String, Double> seWeights = new LinkedHashMap<>();
        seWeights.put("Exams", 0.30);
        seWeights.put("Group Project", 0.50);
        seWeights.put("Quizzes", 0.20);
        Course se = new Course("Software Engineering", "IT303", 3, new WeightedGrading(seWeights));
        se.addAssessment(new Assessment("Midterm", "Exams", 85, 100, 1.0, LocalDate.of(2026, 3, 18)));
        se.addAssessment(new Assessment("Sprint 1 Deliverable", "Group Project", 88, 100, 0.3, LocalDate.of(2026, 3, 5)));
        se.addAssessment(new Assessment("Sprint 2 Deliverable", "Group Project", 92, 100, 0.3, LocalDate.of(2026, 4, 10)));
        se.addAssessment(new Assessment("Final Deliverable", "Group Project", -1, 100, 0.4, LocalDate.of(2026, 6, 1)));
        se.addAssessment(new Assessment("Quiz 1", "Quizzes", 7, 10, 1.0, LocalDate.of(2026, 2, 25)));
        se.addTimeSlot(new TimeSlot(DayOfWeek.TUESDAY, LocalTime.of(13, 0), LocalTime.of(14, 30), "CL-305"));
        se.addTimeSlot(new TimeSlot(DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(9, 30), "CL-305"));
        term.addCourse(se);

        return term;
    }
}
