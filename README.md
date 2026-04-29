# AcademiQ

A JavaFX desktop app for tracking courses, grades, and schedules across multiple academic terms — with pluggable grading policies and real grade projection.

# Features
- Multi-term tracking — organize courses by semester, compute per-term and cumulative GPA
- Pluggable grading — each course uses its own system (weighted, points-based, or curved)
- Grade projection — "what do I need on the final?" using the course's actual grading policy
- Schedule conflicts — detect and flag overlapping time slots within a term
- Swappable persistence — save/load via JSON or CSV without changing application code


  USE CASE DIAGRAM
  <img width="846" height="768" alt="AcademyQUseCaseDiagram" src="https://github.com/user-attachments/assets/0b8b36ad-0694-4cc8-ae8b-cd92402fdfca" />

  <div align = 'center'>
    CLASS DIAGRAM
    <img src="https://github.com/BOTLANG-OOP/AcademiQ/blob/main/AcademiQClassDiagram.png" alt="Class Diagram of AcademiQ" width="846" height="768"/>
  </div>

# Build & Run

## Prerequisites
- **JDK 17+** (tested with Eclipse Temurin 21)
- **Maven 3.9+**
- A desktop environment capable of running JavaFX (Windows/macOS/Linux with a display)

Verify your toolchain:
```bash
java -version
mvn -v
```

## Compile
Compile sources without running tests:
```bash
mvn clean compile
```

## Run tests
```bash
mvn test
```

## Build a JAR
Produces `target/academiq-1.0-SNAPSHOT.jar` and installs it to your local Maven repository:
```bash
mvn clean install
```

## Run the app
Launches the JavaFX window via the `javafx-maven-plugin`:
```bash
mvn javafx:run
```

The first run will download JavaFX 21 native libraries for your platform; subsequent runs are cached.

## Project layout
- `src/main/java/com/academiq/` — application code (`App.java` is the JavaFX entry point)
  - `model/` — domain classes (Student, Term, Course, Assessment, TimeSlot, ConflictRecord)
  - `grading/` — `GradingPolicy` strategy interface and implementations
  - `persistence/` — `SqliteDataStore` for SQLite persistence
- `src/main/resources/com/academiq/` — CSS and (eventually) FXML
- `src/test/java/com/academiq/` — JUnit 5 tests

## Troubleshooting
- **`UnsupportedClassVersionError`**: ensure `java -version` reports 17 or newer.
- **Window does not open on Linux**: install your distro's JavaFX system dependencies (e.g., `libgtk-3-0`, `libxtst6`).
- **Stale build**: run `mvn clean` before rebuilding.
