AcademiQ
SQLite Schema Design

## Table Definitions

## 1. `students` Table

Stores student profile information.

| Column         | Type    | Constraint                | Description                  |
| -------------- | ------- | ------------------------- | ---------------------------- |
| student_id     | VARCHAR | PRIMARY KEY AUTOINCREMENT | Unique student ID            |
| student_number | TEXT    | UNIQUE NOT NULL           | School-issued student number |
| full_name      | TEXT    | NOT NULL                  | Student full name            |
| email          | TEXT    | UNIQUE                    | Student email                |
| created_at     | TEXT    | DEFAULT CURRENT_TIMESTAMP | Date created                 |

---

## 2. `terms` Table

Stores school terms or semesters.

| Column      | Type    | Constraint                | Description           |
| ----------- | ------- | ------------------------- | --------------------- |
| term_id     | VARCHAR | PRIMARY KEY AUTOINCREMENT | Unique term ID        |
| term_name   | TEXT    | NOT NULL                  | Example: 1st Semester |
| school_year | TEXT    | NOT NULL                  | Example: 2025–2026    |
| start_date  | TEXT    | NOT NULL                  | Term start date       |
| end_date    | TEXT    | NOT NULL                  | Term end date         |

---

## 3. `courses` Table

Stores course or subject information.

| Column         | Type    | Constraint                | Description                |
| -------------- | ------- | ------------------------- | -------------------------- |
| course_id      | VARCHAR | PRIMARY KEY AUTOINCREMENT | Unique course ID           |
| term_id        | VARCHAR | FOREIGN KEY               | References `terms.term_id` |
| student_id     | VARCHAR | FOREIGN KEY               | References `students.student_id` |
| course_code    | TEXT    | NOT NULL                  | Example: IT101             |
| course_name    | TEXT    | NOT NULL                  | Course title               |
| grading_type   | TEXT    | NOT NULL                  | Type of grading used       |
| grading_config | TEXT    | NOT NULL                  | JSON-style grading rules   |

**Foreign Key Rule:**

```sql
    FOREIGN KEY (term_id) REFERENCES terms(term_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE

    FOREIGN KEY (student_id) REFERENCES students(student_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
```

Meaning: if a term is deleted, its courses are also deleted.

---

## 4. `assessments` Table

Stores quizzes, exams, projects, and other graded activities.

| Column          | Type    | Constraint                | Description                    |
| --------------- | ------- | ------------------------- | ------------------------------ |
| assessment_id   | VARCHAR | PRIMARY KEY AUTOINCREMENT | Unique assessment ID           |
| course_id       | VARCHAR | FOREIGN KEY               | References `courses.course_id` |
| assessment_name | TEXT    | NOT NULL                  | Example: Midterm Exam          |
| assessment_type | TEXT    | NOT NULL                  | Quiz, Exam, Project, etc.      |
| total_score     | REAL    | NOT NULL                  | Highest possible score         |
| weight          | REAL    | NOT NULL                  | Percentage weight              |
| due_date        | TEXT    |                           | Optional deadline              |

**Foreign Key Rule:**

```sql
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
```

Meaning: if a course is deleted, its assessments are also deleted.

---

## 5. `time_slots` Table

Stores class schedules.

| Column       | Type    | Constraint                | Description                    |
| ------------ | ------- | ------------------------- | ------------------------------ |
| time_slot_id | VARCHAR | PRIMARY KEY AUTOINCREMENT | Unique schedule ID             |
| course_id    | VARCHAR | FOREIGN KEY               | References `courses.course_id` |
| day_of_week  | TEXT    | NOT NULL                  | Monday, Tuesday, etc.          |
| start_time   | TEXT    | NOT NULL                  | Class start time               |
| end_time     | TEXT    | NOT NULL                  | Class end time                 |
| room         | TEXT    |                           | Classroom or location          |

```sql
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
```

**Cascade Rule Summary**
| Relationship              | Cascade Rule                               |
| ------------------------- | ------------------------------------------ |
| `terms` → `courses`       | Delete term = delete related courses       |
| `courses` → `assessments` | Delete course = delete related assessments |
| `courses` → `time_slots`  | Delete course = delete related schedules   |


