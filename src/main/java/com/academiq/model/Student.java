package com.academiq.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.Collections;
import java.util.List;

public class Student {

    private final String name;
    private final String id;
    private final ObservableList<Term> terms;
    private final DoubleProperty cumulativeGPA;
    private final ChangeListener<Number> termGpaListener;

    public Student(String name, String id) {
        this.name = name;
        this.id = id;
        this.terms = FXCollections.observableArrayList();
        this.cumulativeGPA = new SimpleDoubleProperty();
        this.termGpaListener = (observable, oldValue, newValue) -> recomputeCumulativeGPA();

        this.terms.addListener((ListChangeListener<Term>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::attachTermListener);
                }
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::detachTermListener);
                }
            }
            recomputeCumulativeGPA();
        });

        recomputeCumulativeGPA();
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public ObservableList<Term> getTerms() {
        return terms;
    }

    public void addTerm(Term t) {
        terms.add(t);
    }

    public void removeTerm(Term t) {
        terms.remove(t);
    }

    public double getCumulativeGPA() {
        return cumulativeGPA.get();
    }

    public DoubleProperty cumulativeGPAProperty() {
        return cumulativeGPA;
    }

    private void recomputeCumulativeGPA() {
        cumulativeGPA.set(calculateCumulativeGPA());
    }

    private double calculateCumulativeGPA() {
        double weightedSum = 0.0;
        int totalUnits = 0;
        for (Term term : terms) {
            int termUnits = term.getTotalUnits();
            weightedSum += term.getTermGPA() * termUnits;
            totalUnits += termUnits;
        }
        if (totalUnits == 0) {
            return 0.0;
        }
        return weightedSum / totalUnits;
    }

    private void attachTermListener(Term term) {
        term.termGPAProperty().addListener(termGpaListener);
    }

    private void detachTermListener(Term term) {
        term.termGPAProperty().removeListener(termGpaListener);
    }

    public List<ConflictRecord> getAllConflicts() {
        // TODO: aggregate from all terms
        return Collections.emptyList();
    }
}
