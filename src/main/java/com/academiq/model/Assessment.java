package com.academiq.model;

import java.time.LocalDate;
import java.util.Objects;

public class Assessment {

    private final String title;
    private final String category;
    private double score;
    private final double maxScore;
    private final double weight;
    private final LocalDate date;

    public Assessment(String title, String category, double score, double maxScore, double weight, LocalDate date) {
        this.title = title;
        this.category = category;
        this.score = score;
        this.maxScore = maxScore;
        this.weight = weight;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public double getScore() {
        return score;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public double getWeight() {
        return weight;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getPercentage() {
        if (maxScore == 0) {
            return 0.0;
        }
        return score / maxScore * 100;
    }

    public double getWeightedScore() {
        return getPercentage() * weight;
    }

    public boolean isGraded() {
        return score >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Assessment)) return false;
        Assessment that = (Assessment) o;
        return Objects.equals(title, that.title)
                && Objects.equals(category, that.category)
                && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, category, date);
    }
}
