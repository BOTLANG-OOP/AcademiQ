package com.academiq.persistence;

import com.academiq.model.Student;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Concrete SQLite persistence — no interface by design.
 * GradingPolicy already demonstrates Strategy pattern;
 * a DataStore interface with one implementation would be a code smell.
 */
public class SqliteDataStore {

    private static final String DB_FILE = "academiq.db";
    private Connection connection;

    public SqliteDataStore() {
        // TODO: initialize connection, call createTables()
    }

    public SqliteDataStore(String dbPath) {
        // TODO: initialize connection to custom path, call createTables()
    }

    private void createTables() {
        // TODO: CREATE TABLE IF NOT EXISTS for:
        // students, terms, courses, assessments, time_slots
        // Use ON DELETE CASCADE on all foreign keys
        // courses table needs grading_type TEXT and grading_config TEXT columns
    }

    public void save(Student student) {
        // TODO: upsert full student object tree
    }

    public Student load() {
        // TODO: reconstruct Student → Term → Course → Assessment from DB
        // Use GradingPolicyFactory to rebuild polymorphic GradingPolicy from
        // grading_type + grading_config columns
        return null;
    }

    public void close() {
        // TODO: close connection safely
    }
}
