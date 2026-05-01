package com.academiq.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Student {

    private final String name;
    private final String id;
    private final List<Term> terms;

    public Student(String name, String id) {
        this.name = name;
        this.id = id;
        this.terms = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public void addTerm(Term t) {
        terms.add(t);
    }

    public void removeTerm(Term t) {
        terms.remove(t);
    }

    public double getCumulativeGPA() {
        if (terms.isEmpty()) {
            return 0.0;
        }
        double totalPoints = 0.0;
        int totalUnits = 0;
        for (Term term : terms) {
            for (Course course : term.getCourses()) {
                double grade = course.getGradingPolicy().computeFinalGrade(course.getAssessments());
                int units = course.getUnits();
                totalPoints += grade * units;
                totalUnits += units;
            }
        }
        if (totalUnits == 0) {
            return 0.0;
        }
        // Convert 0-100 scale to 0.0-5.0 scale
        return (totalPoints / totalUnits) / 100.0 * 5.0;
    }

    public List<ConflictRecord> getAllConflicts() {
        // TODO: aggregate from all terms
        return Collections.emptyList();
    }
}
