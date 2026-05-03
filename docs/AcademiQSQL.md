# AcademiQ

## SQLite Schema Design

## Table Definitions

## 1. `students` Table

Stores basic student information.

| Column     | Type | Constraint  | Description                                       |
| ---------- | ---- | ----------- | ------------------------------------------------- |
| student_id | TEXT | PRIMARY KEY | Student ID from `Student(String name, String id)` |
| name       | TEXT | NOT NULL    | Student full name                                 |

---

## 2. `terms` Table

Stores the student’s academic terms.

| Column     | Type    | Constraint           | Description                      |
| ---------- | ------- | -------------------- | -------------------------------- |
| term_id    | TEXT    | PRIMARY KEY          | Unique term ID                   |
| student_id | TEXT    | FOREIGN KEY NOT NULL | References `students.student_id` |
| name       | TEXT    | NOT NULL             | Term name                        |
| year       | INTEGER | NOT NULL             | Academic year                    |
| semester   | TEXT    | NOT NULL             | Semester name                    |

**Foreign Key Rule:**

```sql
FOREIGN KEY (student_id) REFERENCES students(student_id)
ON DELETE CASCADE
ON UPDATE CASCADE
```

Meaning: if a student is deleted, the student’s related terms are also deleted.

---

## 3. `courses` Table

Stores course or subject information.

| Column         | Type    | Constraint           | Description                         |
| -------------- | ------- | -------------------- | ----------------------------------- |
| course_id      | TEXT    | PRIMARY KEY          | Unique course ID                    |
| term_id        | TEXT    | FOREIGN KEY NOT NULL | References `terms.term_id`          |
| name           | TEXT    | NOT NULL             | Course name                         |
| code           | TEXT    | NOT NULL             | Course code                         |
| units          | INTEGER | NOT NULL             | Number of units                     |
| grading_type   | TEXT    | NOT NULL             | Type of grading policy used         |
| grading_config | TEXT    | NOT NULL             | Stored grading policy configuration |

**Foreign Key Rule:**

```sql
FOREIGN KEY (term_id) REFERENCES terms(term_id)
ON DELETE CASCADE
ON UPDATE CASCADE
```

Meaning: if a term is deleted, its related courses are also deleted.

---

## 4. `assessments` Table

Stores quizzes, exams, projects, and other graded activities.

| Column        | Type | Constraint           | Description                         |
| ------------- | ---- | -------------------- | ----------------------------------- |
| assessment_id | TEXT | PRIMARY KEY          | Unique assessment ID                |
| course_id     | TEXT | FOREIGN KEY NOT NULL | References `courses.course_id`      |
| title         | TEXT | NOT NULL             | Assessment title                    |
| category      | TEXT | NOT NULL             | Quiz, exam, project, activity, etc. |
| score         | REAL | NOT NULL             | Student’s earned score              |
| max_score     | REAL | NOT NULL             | Highest possible score              |
| weight        | REAL | NOT NULL             | Assessment weight                   |
| date          | TEXT | NOT NULL             | Assessment date                     |

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

| Column       | Type | Constraint           | Description                    |
| ------------ | ---- | -------------------- | ------------------------------ |
| time_slot_id | TEXT | PRIMARY KEY          | Unique time slot ID            |
| course_id    | TEXT | FOREIGN KEY NOT NULL | References `courses.course_id` |
| day_of_week  | TEXT | NOT NULL             | Day of the week                |
| start_time   | TEXT | NOT NULL             | Class start time               |
| end_time     | TEXT | NOT NULL             | Class end time                 |
| room         | TEXT | NOT NULL             | Classroom or location          |

**Foreign Key Rule:**

```sql
FOREIGN KEY (course_id) REFERENCES courses(course_id)
ON DELETE CASCADE
ON UPDATE CASCADE
```

Meaning: if a course is deleted, its schedules are also deleted.

---

## Cascade Rule Summary

| Relationship              | Cascade Rule                               |
| ------------------------- | ------------------------------------------ |
| `students` → `terms`      | Delete student = delete related terms      |
| `terms` → `courses`       | Delete term = delete related courses       |
| `courses` → `assessments` | Delete course = delete related assessments |
| `courses` → `time_slots`  | Delete course = delete related schedules   |

---
