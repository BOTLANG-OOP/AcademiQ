# AcademiQ

A JavaFX desktop app for tracking courses, grades, and schedules across multiple academic terms — with pluggable grading policies, reactive GPA calculation, and grade projection.

# Features

- **Multi-term tracking** — organize courses by semester and year; per-term GPA and cumulative GPA are recomputed automatically as grades change.
- **Pluggable grading policies** — each course chooses its own grading system:
  - **Weighted** — category weights (e.g. Exams 60%, Homework 40%); weights must sum to 1.0.
  - **Points-based** — fixed total possible points; final grade = earned / total.
  - **Curved** — a decorator that adds a curve to any other policy and clamps to [0, 100].
- **Grade projection** — answers "what do I need on remaining assessments to hit a target?" using the course's actual grading policy (works through the curve decorator too).
- **Schedule conflict detection** — flags overlapping `TimeSlot`s between courses in the same term.
- **SQLite persistence with debounced auto-save** — every model mutation reschedules a single write on a background daemon thread, so rapid edits coalesce into one transaction.
- **Reactive UI** — JavaFX properties propagate score → course grade → term GPA → cumulative GPA without manual refresh.
- **Database recovery** — on startup, integrity is checked and the user is offered a "Start Fresh" option if the file is corrupted.

<div align="center">

### Use Case Diagram
<img
  src="https://github.com/user-attachments/assets/0b8b36ad-0694-4cc8-ae8b-cd92402fdfca"
  alt="AcademiQ Use Case Diagram"
  width="846"
/>

### Class Diagram
<img
  src="https://github.com/BOTLANG-OOP/AcademiQ/blob/main/AcademiQClassDiagram.png"
  alt="AcademiQ Class Diagram"
  width="846"
/>

</div>

# Design Patterns

- **Strategy** — `GradingPolicy` is an interface with `WeightedGrading`, `PointsBasedGrading`, and `CurvedGrading` implementations. `Course` delegates all grade math to its policy and the policy can be swapped at runtime.
- **Decorator** — `CurvedGrading` wraps another `GradingPolicy`, adds a curve to the computed grade, and adjusts the target downward when projecting needed scores.
- **Factory** — `GradingPolicyFactory` rebuilds the correct concrete policy from the `grading_type` + JSON `grading_config` columns when loading from SQLite.
- **Observable chain** — `Assessment.scoreProperty` → `Course.finalGradeProperty` → `Term.termGPAProperty` → `Student.cumulativeGPAProperty`. Each layer listens to the layer below and recomputes; the same chain is what triggers debounced saves.
- **Debounced writer** — `SqliteDataStore.scheduleSave(...)` cancels any pending write and reschedules a single flush on a daemon `ScheduledExecutorService`, so a burst of edits collapses to one transaction.

# Tech Stack

- **Java 17** (compiled with `--source 17 --target 17`)
- **JavaFX 21** (controls + fxml) via the `javafx-maven-plugin`
- **AtlantaFX 2.1.0** — PrimerLight theme and `Styles` utility classes
- **Ikonli 12.3.1** — Feather icon pack for the sidebar and empty states
- **SQLite** via `sqlite-jdbc` 3.45.1.0 (WAL mode, foreign keys enabled, `PRAGMA integrity_check` on startup)
- **Gson 2.10.1** — serializes the `grading_config` blob stored per course
- **JUnit Jupiter 5.10.2** — test framework

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

The first run will download JavaFX 21 native libraries for your platform; subsequent runs are cached. The app creates (or opens) `academiq.db` in the working directory.

# Project Layout

```
src/main/java/com/academiq/
├── App.java                            JavaFX entry point — sidebar, navigation, all dialogs and views
├── grading/
│   ├── GradingPolicy.java              Strategy interface (computeFinalGrade / getBreakdown / projectNeeded)
│   ├── WeightedGrading.java            Category-weighted policy
│   ├── PointsBasedGrading.java         Total-possible-points policy
│   ├── CurvedGrading.java              Decorator that adds a curve to any base policy
│   └── GradingPolicyFactory.java       Rebuilds the right policy from persisted JSON config
├── model/
│   ├── Student.java                    Owns Terms; cumulative GPA property
│   ├── Term.java                       Owns Courses; term GPA + schedule conflict detection
│   ├── Course.java                     Owns Assessments + TimeSlots; final grade property
│   ├── Assessment.java                 Title, category, score, max, weight, date
│   ├── TimeSlot.java                   Day + start/end; overlapsWith()
│   └── ConflictRecord.java             Two courses + the two overlapping slots
└── persistence/
    ├── SqliteDataStore.java            Schema bootstrap, save/load, debounced writer, integrity check
    └── SampleDataGenerator.java        Seed data for manual testing

src/main/resources/com/academiq/
└── styles.css                          AtlantaFX-aware custom styling

src/test/java/com/academiq/             JUnit 5 tests (see below)

docs/
├── AcademiQSQL.md                      Authoritative SQLite schema reference
└── design-tokens.md                    Color, spacing, and typography tokens used by styles.css
```

# Test Coverage Summary

~98 JUnit 5 tests across 10 files, exercising every layer except the JavaFX view code:

| Area | File | Tests | Focus |
| --- | --- | --: | --- |
| Grading — Weighted | `grading/WeightedGradingTest.java` | 13 | category math, weight validation, ungraded handling |
| Grading — Points | `grading/PointsBasedGradingTest.java` | 11 | earned/total, zero-max guards, projection |
| Grading — Curved | `grading/CurvedGradingEdgeCaseTest.java` | 12 | decorator clamping, negative curves, projection through curve |
| Grading — Edge cases | `grading/EdgeCaseHardeningTest.java` | 17 | nulls, empties, NaN, extreme values |
| Grading — Factory | `grading/GradingPolicyFactoryTest.java` | 4 | round-trip rebuild of each policy type |
| Grading — Projection | `grading/ProjectionIntegrationTest.java` | 7 | end-to-end "what do I need" across policies |
| Model — Observables | `model/ObservableGradeChainTest.java` | 1 | score → course → term → student propagation |
| Model — Term | `model/TermTest.java` | 4 | term GPA, total units, conflict aggregation |
| Model — TimeSlot | `model/TimeSlotTest.java` | 6 | overlap detection across day/time edge cases |
| Persistence | `persistence/SqliteDataStoreTest.java` | 23 | schema, CRUD, cascades, debounced save, integrity recovery |

Run them with `mvn test`.

# Troubleshooting

- **`UnsupportedClassVersionError`**: ensure `java -version` reports 17 or newer.
- **Window does not open on Linux**: install your distro's JavaFX system dependencies (e.g., `libgtk-3-0`, `libxtst6`).
- **"Database Error" dialog on startup**: the integrity check failed. Choose *Start Fresh* to wipe `academiq.db` and start over, or *Quit* and back up the file before investigating.
- **Stale build**: run `mvn clean` before rebuilding.
