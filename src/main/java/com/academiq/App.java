package com.academiq;

import com.academiq.grading.CurvedGrading;
import com.academiq.grading.GradingPolicy;
import com.academiq.grading.PointsBasedGrading;
import com.academiq.grading.WeightedGrading;
import com.academiq.model.Assessment;
import com.academiq.model.Course;
import com.academiq.model.Student;
import com.academiq.model.Term;
import com.academiq.model.TimeSlot;
import com.academiq.persistence.SqliteDataStore;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Optional;

public class App extends Application {

    private static final String DEFAULT_STUDENT_ID = "default-student";

    private SqliteDataStore store;
    private Student student;

    private StackPane contentArea;
    private VBox sidebar;
    private Pane courseListPane;
    private Pane gradeEntryPane;
    private Pane dashboardPane;
    private Pane schedulePane;
    private Button coursesButton;
    private Button gradesButton;
    private Button dashboardButton;
    private Button scheduleButton;
    private Button activeButton;

    @Override
    public void start(Stage stage) {
        store = new SqliteDataStore();
        Student loaded = store.loadStudent(DEFAULT_STUDENT_ID);
        if (loaded == null) {
            student = new Student("Student", DEFAULT_STUDENT_ID);
            store.save(student);
        } else {
            student = loaded;
        }

        student.cumulativeGPAProperty().addListener((obs, oldV, newV) -> store.scheduleSave(student));

        courseListPane = createCourseListPane();
        gradeEntryPane = createGradeEntryPane();
        dashboardPane = createDashboardPane();
        schedulePane = createSchedulePane();

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        sidebar = createSidebar();

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        navigateTo("courses");

        stage.setTitle("AcademiQ");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> store.close());
        stage.show();
    }

    private VBox createSidebar() {
        VBox box = new VBox();
        box.getStyleClass().add("sidebar");

        Label title = new Label("AcademiQ");
        title.getStyleClass().add("app-title");

        Separator separator = new Separator();

        coursesButton = createNavButton("📚  Courses", "courses");
        gradesButton = createNavButton("📝  Grade Entry", "grades");
        dashboardButton = createNavButton("📊  Dashboard", "dashboard");
        scheduleButton = createNavButton("📅  Schedule", "schedule");

        VBox navButtons = new VBox(coursesButton, gradesButton, dashboardButton, scheduleButton);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label version = new Label("v1.0-SNAPSHOT");
        version.getStyleClass().add("version-label");

        box.getChildren().addAll(title, separator, navButtons, spacer, version);
        return box;
    }

    private Button createNavButton(String text, String viewName) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> navigateTo(viewName));
        return button;
    }

    private void navigateTo(String viewName) {
        contentArea.getChildren().clear();
        Button next = switch (viewName) {
            case "courses" -> {
                contentArea.getChildren().add(courseListPane);
                yield coursesButton;
            }
            case "grades" -> {
                contentArea.getChildren().add(gradeEntryPane);
                yield gradesButton;
            }
            case "dashboard" -> {
                contentArea.getChildren().add(dashboardPane);
                yield dashboardButton;
            }
            case "schedule" -> {
                contentArea.getChildren().add(schedulePane);
                yield scheduleButton;
            }
            default -> null;
        };

        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-active");
        }
        if (next != null) {
            if (!next.getStyleClass().contains("nav-active")) {
                next.getStyleClass().add("nav-active");
            }
            activeButton = next;
        }
    }

    private VBox createCourseListPane() {
        Label header = new Label("Courses");
        header.getStyleClass().add("section-header");

        ComboBox<Term> termCombo = new ComboBox<>(student.getTerms());
        termCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Term term) {
                if (term == null) return "";
                return term.getName() + " (" + term.getSemester() + " " + term.getYear() + ")";
            }
            @Override
            public Term fromString(String s) {
                return null;
            }
        });
        termCombo.setPromptText("Select a term");
        termCombo.getStyleClass().add("term-combo");

        if (!student.getTerms().isEmpty()) {
            termCombo.getSelectionModel().select(0);
        }

        Button addTermButton = new Button("Add Term");
        addTermButton.getStyleClass().addAll("aq-button", "aq-button-primary");
        addTermButton.setOnAction(e -> {
            Optional<Term> created = showAddTermDialog();
            created.ifPresent(t -> {
                student.addTerm(t);
                termCombo.getSelectionModel().select(t);
                store.scheduleSave(student);
            });
        });

        Button removeTermButton = new Button("Remove Term");
        removeTermButton.getStyleClass().addAll("aq-button", "aq-button-danger");
        removeTermButton.disableProperty().bind(termCombo.getSelectionModel().selectedItemProperty().isNull());
        removeTermButton.setOnAction(e -> {
            Term selected = termCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete term '" + selected.getName() + "'? This removes all courses, "
                            + "grades, and schedule data in this term.",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText(null);
            confirm.setTitle("Delete term");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    student.removeTerm(selected);
                    store.scheduleSave(student);
                }
            });
        });

        HBox termControls = new HBox(12, new Label("Term:"), termCombo, addTermButton, removeTermButton);
        termControls.setAlignment(Pos.CENTER_LEFT);
        termControls.getStyleClass().add("term-controls");

        StackPane body = new StackPane();
        body.getStyleClass().add("courses-body");

        Label emptyPrompt = new Label("Add a term to get started");
        emptyPrompt.getStyleClass().add("empty-prompt");

        Label noTermSelected = new Label("Select a term to view courses");
        noTermSelected.getStyleClass().add("empty-prompt");

        TableView<Course> courseTable = new TableView<>();
        courseTable.getStyleClass().add("course-table");
        courseTable.setPlaceholder(new Label("No courses yet. Click 'Add Course' to begin"));

        TableColumn<Course, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));
        nameCol.setPrefWidth(220);

        TableColumn<Course, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getCode()));
        codeCol.setPrefWidth(120);

        TableColumn<Course, Number> unitsCol = new TableColumn<>("Units");
        unitsCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getUnits()));
        unitsCol.setPrefWidth(80);

        TableColumn<Course, Number> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(cd -> cd.getValue().finalGradeProperty());
        gradeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", value.doubleValue()));
                }
            }
        });
        gradeCol.setPrefWidth(100);

        courseTable.getColumns().addAll(nameCol, codeCol, unitsCol, gradeCol);

        Runnable rebindTable = () -> {
            Term sel = termCombo.getSelectionModel().getSelectedItem();
            if (sel == null) {
                courseTable.setItems(FXCollections.emptyObservableList());
            } else {
                courseTable.setItems(sel.getCourses());
            }
        };
        termCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) -> rebindTable.run());
        rebindTable.run();

        Button addCourseButton = new Button("Add Course");
        addCourseButton.getStyleClass().addAll("aq-button", "aq-button-primary");
        addCourseButton.disableProperty().bind(termCombo.getSelectionModel().selectedItemProperty().isNull());
        addCourseButton.setOnAction(e -> {
            Term term = termCombo.getSelectionModel().getSelectedItem();
            if (term == null) return;
            showCourseDialog(null).ifPresent(course -> {
                term.addCourse(course);
                courseTable.getSelectionModel().select(course);
                store.scheduleSave(student);
            });
        });

        Button editCourseButton = new Button("Edit Course");
        editCourseButton.getStyleClass().addAll("aq-button", "aq-button-secondary");
        editCourseButton.disableProperty().bind(courseTable.getSelectionModel().selectedItemProperty().isNull());
        editCourseButton.setOnAction(e -> {
            Course selected = courseTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            showCourseDialog(selected).ifPresent(c -> store.scheduleSave(student));
        });

        Button removeCourseButton = new Button("Remove Course");
        removeCourseButton.getStyleClass().addAll("aq-button", "aq-button-danger");
        removeCourseButton.disableProperty().bind(courseTable.getSelectionModel().selectedItemProperty().isNull());
        removeCourseButton.setOnAction(e -> {
            Course selected = courseTable.getSelectionModel().getSelectedItem();
            Term term = termCombo.getSelectionModel().getSelectedItem();
            if (selected == null || term == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Remove course '" + selected.getName() + "'? This removes its assessments and schedule.",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText(null);
            confirm.setTitle("Remove course");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    term.removeCourse(selected);
                    store.scheduleSave(student);
                }
            });
        });

        HBox courseButtonBar = new HBox(12, addCourseButton, editCourseButton, removeCourseButton);
        courseButtonBar.setAlignment(Pos.CENTER_LEFT);
        courseButtonBar.getStyleClass().add("course-button-bar");

        VBox tableSection = new VBox(12, courseTable, courseButtonBar);
        VBox.setVgrow(courseTable, Priority.ALWAYS);

        body.getChildren().addAll(emptyPrompt, noTermSelected, tableSection);

        emptyPrompt.visibleProperty().bind(Bindings.isEmpty(student.getTerms()));
        emptyPrompt.managedProperty().bind(emptyPrompt.visibleProperty());

        noTermSelected.visibleProperty().bind(
                Bindings.isNotEmpty(student.getTerms())
                        .and(termCombo.getSelectionModel().selectedItemProperty().isNull()));
        noTermSelected.managedProperty().bind(noTermSelected.visibleProperty());

        tableSection.visibleProperty().bind(termCombo.getSelectionModel().selectedItemProperty().isNotNull());
        tableSection.managedProperty().bind(tableSection.visibleProperty());

        student.getTerms().addListener((ListChangeListener<Term>) c -> {
            if (termCombo.getSelectionModel().isEmpty() && !student.getTerms().isEmpty()) {
                termCombo.getSelectionModel().select(0);
            }
        });

        VBox pane = new VBox(16, header, termControls, new Separator(), body);
        pane.setPadding(new Insets(16));
        VBox.setVgrow(body, javafx.scene.layout.Priority.ALWAYS);
        return pane;
    }

    private Optional<Term> showAddTermDialog() {
        Dialog<Term> dialog = new Dialog<>();
        dialog.setTitle("Add Term");
        dialog.setHeaderText("New term");

        ButtonType okType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. 1st Semester");

        int currentYear = Year.now().getValue();
        int initialYear = Math.min(2030, Math.max(2020, currentYear));
        Spinner<Integer> yearSpinner = new Spinner<>(2020, 2030, initialYear);
        yearSpinner.setEditable(true);

        ComboBox<String> semesterCombo = new ComboBox<>();
        semesterCombo.getItems().addAll("First", "Second", "Summer");
        semesterCombo.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8, 0, 8, 0));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Year:"), 0, 1);
        grid.add(yearSpinner, 1, 1);
        grid.add(new Label("Semester:"), 0, 2);
        grid.add(semesterCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStyleClass().add("aq-dialog");

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);
        okButton.disableProperty().bind(nameField.textProperty().isEmpty());

        dialog.setResultConverter(bt -> {
            if (bt == okType) {
                return new Term(nameField.getText().trim(), yearSpinner.getValue(), semesterCombo.getValue());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private Optional<Course> showCourseDialog(Course existing) {
        boolean editing = existing != null;

        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Edit Course" : "Add Course");
        dialog.setHeaderText(editing ? "Edit course grading policy" : "New course");

        ButtonType okType = new ButtonType(editing ? "Save" : "Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextField nameField = new TextField(editing ? existing.getName() : "");
        nameField.setPromptText("e.g. Data Structures");
        TextField codeField = new TextField(editing ? existing.getCode() : "");
        codeField.setPromptText("e.g. CS201");
        Spinner<Integer> unitsSpinner = new Spinner<>(1, 6, editing ? existing.getUnits() : 3);
        unitsSpinner.setEditable(true);

        if (editing) {
            nameField.setDisable(true);
            codeField.setDisable(true);
            unitsSpinner.setDisable(true);
        }

        ComboBox<String> policyTypeCombo = new ComboBox<>();
        policyTypeCombo.getItems().addAll("Weighted", "Points-Based", "Curved");

        VBox configArea = new VBox(8);
        configArea.getStyleClass().add("policy-config-area");

        PolicyEditor topEditor = new PolicyEditor(policyTypeCombo, configArea, existing == null ? null : existing.getGradingPolicy(), false);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8, 0, 8, 0));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Code:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("Units:"), 0, 2);
        grid.add(unitsSpinner, 1, 2);
        grid.add(new Label("Grading:"), 0, 3);
        grid.add(policyTypeCombo, 1, 3);
        grid.add(configArea, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStyleClass().add("aq-dialog");

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);
        Runnable updateOk = () -> {
            boolean fieldsValid = editing
                    || (!nameField.getText().trim().isEmpty() && !codeField.getText().trim().isEmpty());
            okButton.setDisable(!(fieldsValid && topEditor.isValid()));
        };
        nameField.textProperty().addListener((o, ov, nv) -> updateOk.run());
        codeField.textProperty().addListener((o, ov, nv) -> updateOk.run());
        topEditor.setOnValidityChanged(updateOk);
        updateOk.run();

        dialog.setResultConverter(bt -> {
            if (bt != okType) return null;
            GradingPolicy policy = topEditor.buildPolicy();
            if (policy == null) return null;
            if (editing) {
                existing.setGradingPolicy(policy);
                return existing;
            }
            return new Course(nameField.getText().trim(), codeField.getText().trim(),
                    unitsSpinner.getValue(), policy);
        });

        return dialog.showAndWait();
    }

    /**
     * Editor for a grading policy type. Manages the dynamic config area and can be
     * nested inside a CurvedGrading editor as the base policy.
     */
    private static final class PolicyEditor {
        private final ComboBox<String> typeCombo;
        private final VBox container;
        private final boolean nested;
        private Runnable onValidityChanged = () -> {};

        // Weighted state
        private final VBox weightedRows = new VBox(6);
        private final Label weightedTotal = new Label();

        // Points state
        private TextField pointsField;

        // Curved state
        private TextField curveField;
        private PolicyEditor baseEditor;

        PolicyEditor(ComboBox<String> typeCombo, VBox container, GradingPolicy initial, boolean nested) {
            this.typeCombo = typeCombo;
            this.container = container;
            this.nested = nested;

            typeCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> rebuildConfig());

            String initialType = "Weighted";
            if (initial instanceof PointsBasedGrading) initialType = "Points-Based";
            else if (initial instanceof CurvedGrading) initialType = "Curved";

            typeCombo.getSelectionModel().select(initialType);
            // selecting same value as default doesn't fire listener; rebuild explicitly
            rebuildConfigWithInitial(initial);
        }

        void setOnValidityChanged(Runnable r) {
            this.onValidityChanged = r;
        }

        private void rebuildConfig() {
            rebuildConfigWithInitial(null);
        }

        private void rebuildConfigWithInitial(GradingPolicy initial) {
            container.getChildren().clear();
            String type = typeCombo.getSelectionModel().getSelectedItem();
            if (type == null) type = "Weighted";

            switch (type) {
                case "Weighted" -> buildWeighted(initial instanceof WeightedGrading w ? w : null);
                case "Points-Based" -> buildPoints(initial instanceof PointsBasedGrading p ? p : null);
                case "Curved" -> buildCurved(initial instanceof CurvedGrading c ? c : null);
            }
            onValidityChanged.run();
        }

        private void buildWeighted(WeightedGrading initial) {
            weightedRows.getChildren().clear();
            addWeightedRow("Exams", "0.6");
            addWeightedRow("Homework", "0.4");

            Button addRow = new Button("Add Category");
            addRow.getStyleClass().addAll("aq-button", "aq-button-secondary");
            addRow.setOnAction(e -> addWeightedRow("", ""));

            recomputeWeightedTotal();
            container.getChildren().addAll(new Label("Category weights:"), weightedRows, addRow, weightedTotal);
        }

        private void addWeightedRow(String catName, String weight) {
            TextField nameF = new TextField(catName);
            nameF.setPromptText("Category");
            TextField weightF = new TextField(weight);
            weightF.setPromptText("0.5");
            weightF.setPrefWidth(80);
            Button remove = new Button("Remove");
            remove.getStyleClass().addAll("aq-button", "aq-button-secondary");
            HBox row = new HBox(8, nameF, weightF, remove);
            row.setAlignment(Pos.CENTER_LEFT);
            remove.setOnAction(e -> {
                weightedRows.getChildren().remove(row);
                recomputeWeightedTotal();
                onValidityChanged.run();
            });
            weightF.textProperty().addListener((o, ov, nv) -> {
                recomputeWeightedTotal();
                onValidityChanged.run();
            });
            nameF.textProperty().addListener((o, ov, nv) -> onValidityChanged.run());
            weightedRows.getChildren().add(row);
        }

        private void recomputeWeightedTotal() {
            double total = 0.0;
            for (var node : weightedRows.getChildren()) {
                if (node instanceof HBox row && row.getChildren().size() >= 2
                        && row.getChildren().get(1) instanceof TextField wf) {
                    try {
                        total += Double.parseDouble(wf.getText().trim());
                    } catch (NumberFormatException ignored) {
                        // skip
                    }
                }
            }
            weightedTotal.setText(String.format("Total: %.2f", total));
            if (Math.abs(total - 1.0) > 0.0001) {
                weightedTotal.setStyle("-fx-text-fill: #C62828;");
            } else {
                weightedTotal.setStyle("-fx-text-fill: #2E7D32;");
            }
        }

        private void buildPoints(PointsBasedGrading initial) {
            pointsField = new TextField("100.0");
            pointsField.textProperty().addListener((o, ov, nv) -> onValidityChanged.run());
            HBox row = new HBox(8, new Label("Total Possible Points:"), pointsField);
            row.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().add(row);
        }

        private void buildCurved(CurvedGrading initial) {
            if (nested) {
                // Should never happen — UI prevents nested Curved selection.
                container.getChildren().add(new Label("Nested curved policies are not supported."));
                return;
            }
            curveField = new TextField("5.0");
            curveField.textProperty().addListener((o, ov, nv) -> onValidityChanged.run());
            HBox curveRow = new HBox(8, new Label("Curve Amount:"), curveField);
            curveRow.setAlignment(Pos.CENTER_LEFT);

            ComboBox<String> baseCombo = new ComboBox<>();
            baseCombo.getItems().addAll("Weighted", "Points-Based");
            VBox baseConfig = new VBox(8);
            baseEditor = new PolicyEditor(baseCombo, baseConfig, null, true);
            baseEditor.setOnValidityChanged(onValidityChanged);

            HBox baseTypeRow = new HBox(8, new Label("Base policy:"), baseCombo);
            baseTypeRow.setAlignment(Pos.CENTER_LEFT);

            container.getChildren().addAll(curveRow, baseTypeRow, baseConfig);
        }

        boolean isValid() {
            String type = typeCombo.getSelectionModel().getSelectedItem();
            if (type == null) return false;
            return switch (type) {
                case "Weighted" -> isWeightedValid();
                case "Points-Based" -> parseDouble(pointsField) != null && parseDouble(pointsField) > 0;
                case "Curved" -> parseDouble(curveField) != null && baseEditor != null && baseEditor.isValid();
                default -> false;
            };
        }

        private boolean isWeightedValid() {
            if (weightedRows.getChildren().isEmpty()) return false;
            double total = 0.0;
            for (var node : weightedRows.getChildren()) {
                if (!(node instanceof HBox row) || row.getChildren().size() < 2) return false;
                if (!(row.getChildren().get(0) instanceof TextField nameF)) return false;
                if (!(row.getChildren().get(1) instanceof TextField weightF)) return false;
                if (nameF.getText().trim().isEmpty()) return false;
                Double w = parseDouble(weightF);
                if (w == null || w <= 0) return false;
                total += w;
            }
            return Math.abs(total - 1.0) <= 0.0001;
        }

        GradingPolicy buildPolicy() {
            if (!isValid()) return null;
            String type = typeCombo.getSelectionModel().getSelectedItem();
            return switch (type) {
                case "Weighted" -> {
                    java.util.LinkedHashMap<String, Double> map = new java.util.LinkedHashMap<>();
                    for (var node : weightedRows.getChildren()) {
                        HBox row = (HBox) node;
                        String name = ((TextField) row.getChildren().get(0)).getText().trim();
                        double w = Double.parseDouble(((TextField) row.getChildren().get(1)).getText().trim());
                        map.put(name, w);
                    }
                    yield new WeightedGrading(map);
                }
                case "Points-Based" -> new PointsBasedGrading(Double.parseDouble(pointsField.getText().trim()));
                case "Curved" -> new CurvedGrading(Double.parseDouble(curveField.getText().trim()),
                        baseEditor.buildPolicy());
                default -> null;
            };
        }

        private static Double parseDouble(TextField f) {
            if (f == null) return null;
            try {
                return Double.parseDouble(f.getText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private VBox createGradeEntryPane() {
        Label header = new Label("Grade Entry");
        header.getStyleClass().add("section-header");

        ComboBox<Term> termCombo = new ComboBox<>(student.getTerms());
        termCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Term term) {
                if (term == null) return "";
                return term.getName() + " (" + term.getSemester() + " " + term.getYear() + ")";
            }
            @Override
            public Term fromString(String s) {
                return null;
            }
        });
        termCombo.setPromptText("Select a term");
        termCombo.getStyleClass().add("term-combo");

        ComboBox<Course> courseCombo = new ComboBox<>();
        courseCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Course course) {
                if (course == null) return "";
                return course.getName() + " (" + course.getCode() + ")";
            }
            @Override
            public Course fromString(String s) {
                return null;
            }
        });
        courseCombo.setPromptText("Select a course");
        courseCombo.getStyleClass().add("term-combo");
        courseCombo.disableProperty().bind(termCombo.getSelectionModel().selectedItemProperty().isNull());

        termCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                courseCombo.setItems(FXCollections.emptyObservableList());
            } else {
                courseCombo.setItems(newT.getCourses());
                if (!newT.getCourses().isEmpty()) {
                    courseCombo.getSelectionModel().select(0);
                }
            }
        });
        if (!student.getTerms().isEmpty()) {
            termCombo.getSelectionModel().select(0);
        }

        HBox selectors = new HBox(12,
                new Label("Term:"), termCombo,
                new Label("Course:"), courseCombo);
        selectors.setAlignment(Pos.CENTER_LEFT);
        selectors.getStyleClass().add("term-controls");

        TableView<Assessment> table = new TableView<>();
        table.getStyleClass().add("course-table");
        table.setEditable(true);
        table.setPlaceholder(new Label("No assessments yet. Click 'Add Assessment' to begin"));

        TableColumn<Assessment, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getTitle()));
        titleCol.setPrefWidth(180);

        TableColumn<Assessment, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getCategory()));
        categoryCol.setPrefWidth(120);

        TableColumn<Assessment, Number> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(cd -> cd.getValue().scoreProperty());
        scoreCol.setEditable(true);
        scoreCol.setCellFactory(col -> new ScoreEditingCell());
        scoreCol.setPrefWidth(100);

        TableColumn<Assessment, String> maxCol = new TableColumn<>("Max Score");
        maxCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(formatNumber(cd.getValue().getMaxScore())));
        maxCol.setPrefWidth(100);

        TableColumn<Assessment, String> weightCol = new TableColumn<>("Weight");
        weightCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(formatNumber(cd.getValue().getWeight())));
        weightCol.setPrefWidth(80);

        TableColumn<Assessment, String> dateCol = new TableColumn<>("Date");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        dateCol.setCellValueFactory(cd -> {
            LocalDate d = cd.getValue().getDate();
            return new ReadOnlyStringWrapper(d == null ? "" : d.format(df));
        });
        dateCol.setPrefWidth(110);

        TableColumn<Assessment, Number> percentCol = new TableColumn<>("%");
        percentCol.setCellValueFactory(cd -> cd.getValue().scoreProperty());
        percentCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                Assessment a = (Assessment) getTableRow().getItem();
                if (!a.isGraded()) {
                    setText("—");
                } else {
                    setText(String.format("%.1f%%", a.getPercentage()));
                }
            }
        });
        percentCol.setPrefWidth(80);

        table.getColumns().addAll(titleCol, categoryCol, scoreCol, maxCol, weightCol, dateCol, percentCol);

        Runnable rebindTable = () -> {
            Course sel = courseCombo.getSelectionModel().getSelectedItem();
            if (sel == null) {
                table.setItems(FXCollections.emptyObservableList());
            } else {
                table.setItems(sel.getAssessments());
            }
        };
        courseCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldC, newC) -> rebindTable.run());
        rebindTable.run();

        Button addButton = new Button("Add Assessment");
        addButton.getStyleClass().addAll("aq-button", "aq-button-primary");
        addButton.disableProperty().bind(courseCombo.getSelectionModel().selectedItemProperty().isNull());
        addButton.setOnAction(e -> {
            Course course = courseCombo.getSelectionModel().getSelectedItem();
            if (course == null) return;
            showAssessmentDialog(course).ifPresent(a -> {
                course.addAssessment(a);
                table.getSelectionModel().select(a);
                store.scheduleSave(student);
            });
        });

        Button removeButton = new Button("Remove Assessment");
        removeButton.getStyleClass().addAll("aq-button", "aq-button-danger");
        removeButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        removeButton.setOnAction(e -> {
            Course course = courseCombo.getSelectionModel().getSelectedItem();
            Assessment selected = table.getSelectionModel().getSelectedItem();
            if (course == null || selected == null) return;
            course.removeAssessment(selected);
            store.scheduleSave(student);
        });

        HBox buttonBar = new HBox(12, addButton, removeButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.getStyleClass().add("course-button-bar");

        VBox breakdownPanel = createGradeBreakdownPanel(courseCombo.getSelectionModel().selectedItemProperty());

        HBox tableAndBreakdown = new HBox(16, table, breakdownPanel);
        HBox.setHgrow(table, Priority.ALWAYS);
        tableAndBreakdown.setAlignment(Pos.TOP_LEFT);

        VBox tableSection = new VBox(12, tableAndBreakdown, buttonBar);
        VBox.setVgrow(tableAndBreakdown, Priority.ALWAYS);

        Label noCourseSelected = new Label("Select a course to enter grades");
        noCourseSelected.getStyleClass().add("empty-prompt");

        StackPane body = new StackPane(noCourseSelected, tableSection);
        body.getStyleClass().add("courses-body");

        noCourseSelected.visibleProperty().bind(courseCombo.getSelectionModel().selectedItemProperty().isNull());
        noCourseSelected.managedProperty().bind(noCourseSelected.visibleProperty());
        tableSection.visibleProperty().bind(courseCombo.getSelectionModel().selectedItemProperty().isNotNull());
        tableSection.managedProperty().bind(tableSection.visibleProperty());

        VBox pane = new VBox(16, header, selectors, new Separator(), body);
        pane.setPadding(new Insets(16));
        VBox.setVgrow(body, Priority.ALWAYS);
        return pane;
    }

    private static String formatNumber(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.format("%.0f", v);
        }
        return String.format("%.2f", v);
    }

    private Optional<Assessment> showAssessmentDialog(Course course) {
        Dialog<Assessment> dialog = new Dialog<>();
        dialog.setTitle("Add Assessment");
        dialog.setHeaderText("New assessment for " + course.getName());

        ButtonType okType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Midterm Exam");

        LinkedHashSet<String> categorySuggestions = new LinkedHashSet<>();
        for (Assessment a : course.getAssessments()) {
            if (a.getCategory() != null && !a.getCategory().isBlank()) {
                categorySuggestions.add(a.getCategory());
            }
        }
        ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(categorySuggestions));
        categoryCombo.setEditable(true);
        categoryCombo.setPromptText("Category");

        TextField scoreField = new TextField("-1");
        TextField maxScoreField = new TextField("100");
        TextField weightField = new TextField("1.0");
        DatePicker datePicker = new DatePicker(LocalDate.now());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8, 0, 8, 0));
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryCombo, 1, 1);
        grid.add(new Label("Score:"), 0, 2);
        grid.add(scoreField, 1, 2);
        grid.add(new Label("Max Score:"), 0, 3);
        grid.add(maxScoreField, 1, 3);
        grid.add(new Label("Weight:"), 0, 4);
        grid.add(weightField, 1, 4);
        grid.add(new Label("Date:"), 0, 5);
        grid.add(datePicker, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStyleClass().add("aq-dialog");

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);
        Runnable updateOk = () -> {
            boolean valid = !titleField.getText().trim().isEmpty()
                    && categoryCombo.getEditor().getText() != null
                    && !categoryCombo.getEditor().getText().trim().isEmpty()
                    && parseDoubleOrNull(scoreField.getText()) != null
                    && parsePositiveDoubleOrNull(maxScoreField.getText()) != null
                    && parsePositiveDoubleOrNull(weightField.getText()) != null
                    && datePicker.getValue() != null;
            okButton.setDisable(!valid);
        };
        titleField.textProperty().addListener((o, ov, nv) -> updateOk.run());
        categoryCombo.getEditor().textProperty().addListener((o, ov, nv) -> updateOk.run());
        scoreField.textProperty().addListener((o, ov, nv) -> updateOk.run());
        maxScoreField.textProperty().addListener((o, ov, nv) -> updateOk.run());
        weightField.textProperty().addListener((o, ov, nv) -> updateOk.run());
        datePicker.valueProperty().addListener((o, ov, nv) -> updateOk.run());
        updateOk.run();

        dialog.setResultConverter(bt -> {
            if (bt != okType) return null;
            return new Assessment(
                    titleField.getText().trim(),
                    categoryCombo.getEditor().getText().trim(),
                    Double.parseDouble(scoreField.getText().trim()),
                    Double.parseDouble(maxScoreField.getText().trim()),
                    Double.parseDouble(weightField.getText().trim()),
                    datePicker.getValue());
        });

        return dialog.showAndWait();
    }

    private static Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parsePositiveDoubleOrNull(String s) {
        Double d = parseDoubleOrNull(s);
        return (d != null && d > 0) ? d : null;
    }

    /** Editable table cell for Assessment.score: shows "—" when ungraded, numeric otherwise. */
    private static final class ScoreEditingCell extends TableCell<Assessment, Number> {
        private TextField textField;

        @Override
        public void startEdit() {
            if (isEmpty()) return;
            super.startEdit();
            if (textField == null) {
                textField = new TextField();
                textField.setOnAction(e -> commitFromField());
                textField.focusedProperty().addListener((obs, was, isNow) -> {
                    if (!isNow) commitFromField();
                });
                textField.setOnKeyReleased(e -> {
                    if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });
            }
            Assessment a = getRowAssessment();
            textField.setText(a == null ? "" : String.valueOf(a.getScore()));
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }

        private void commitFromField() {
            Double parsed = parseDoubleOrNull(textField.getText());
            if (parsed == null) {
                cancelEdit();
                return;
            }
            commitEdit(parsed);
        }

        @Override
        public void commitEdit(Number newValue) {
            Assessment a = getRowAssessment();
            if (a != null && newValue != null) {
                a.setScore(newValue.doubleValue());
            }
            super.commitEdit(newValue);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setGraphic(null);
            updateItem(getItem(), isEmpty());
        }

        private Assessment getRowAssessment() {
            return getTableRow() == null ? null : (Assessment) getTableRow().getItem();
        }

        @Override
        protected void updateItem(Number value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(value == null ? "" : String.valueOf(value.doubleValue()));
                    setGraphic(textField);
                    setText(null);
                }
            } else {
                Assessment a = (Assessment) getTableRow().getItem();
                if (!a.isGraded()) {
                    setText("—");
                } else {
                    setText(formatNumber(a.getScore()));
                }
                setGraphic(null);
            }
        }
    }

    private VBox createGradeBreakdownPanel(ReadOnlyObjectProperty<Course> selectedCourse) {
        Label header = new Label("Grade Breakdown");
        header.getStyleClass().add("breakdown-header");

        Label policyTypeLabel = new Label();
        policyTypeLabel.getStyleClass().add("breakdown-policy-type");

        HBox headerRow = new HBox(12, header, policyTypeLabel);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        TextArea breakdownArea = new TextArea();
        breakdownArea.setEditable(false);
        breakdownArea.setWrapText(false);
        breakdownArea.getStyleClass().add("breakdown-text");
        breakdownArea.setPrefRowCount(10);

        Label finalGradeCaption = new Label("Final Grade");
        finalGradeCaption.getStyleClass().add("breakdown-caption");

        Label finalGradeLabel = new Label("—");
        finalGradeLabel.getStyleClass().add("breakdown-final-grade");

        Label letterGradeLabel = new Label("");
        letterGradeLabel.getStyleClass().add("breakdown-letter-grade");

        HBox gradeRow = new HBox(12, finalGradeLabel, letterGradeLabel);
        gradeRow.setAlignment(Pos.BASELINE_LEFT);

        VBox projectionSection = createGradeProjectionSection(selectedCourse);

        VBox panel = new VBox(12, headerRow, breakdownArea, finalGradeCaption, gradeRow,
                new Separator(), projectionSection);
        panel.getStyleClass().add("breakdown-card");
        panel.setPrefWidth(340);
        panel.setMinWidth(300);
        panel.setMaxWidth(380);

        ChangeListener<Number> gradeListener = (obs, ov, nv) -> {
            Course c = selectedCourse.get();
            if (c == null) return;
            breakdownArea.setText(c.getGradeBreakdown());
            double g = nv.doubleValue();
            finalGradeLabel.setText(String.format("%.2f", g));
            letterGradeLabel.setText(letterGradeFor(g));
        };

        final Course[] bound = { null };
        Runnable rebind = () -> {
            Course old = bound[0];
            Course cur = selectedCourse.get();
            if (old != null) {
                old.finalGradeProperty().removeListener(gradeListener);
            }
            bound[0] = cur;
            if (cur == null) {
                breakdownArea.clear();
                policyTypeLabel.setText("");
                finalGradeLabel.setText("—");
                letterGradeLabel.setText("");
                return;
            }
            cur.finalGradeProperty().addListener(gradeListener);
            policyTypeLabel.setText(policyTypeLabelFor(cur.getGradingPolicy()));
            breakdownArea.setText(cur.getGradeBreakdown());
            double g = cur.getFinalGrade();
            finalGradeLabel.setText(String.format("%.2f", g));
            letterGradeLabel.setText(letterGradeFor(g));
        };
        selectedCourse.addListener((o, ov, nv) -> rebind.run());
        rebind.run();

        panel.visibleProperty().bind(selectedCourse.isNotNull());
        panel.managedProperty().bind(panel.visibleProperty());

        return panel;
    }

    private VBox createGradeProjectionSection(ReadOnlyObjectProperty<Course> selectedCourse) {
        Label header = new Label("What Do I Need?");
        header.getStyleClass().add("projection-header");

        Label targetLabel = new Label("Target grade (0–100):");
        targetLabel.getStyleClass().add("projection-target-label");

        TextField targetField = new TextField("90");
        targetField.getStyleClass().add("projection-target-field");
        targetField.setPrefWidth(80);

        HBox inputRow = new HBox(8, targetLabel, targetField);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label resultLabel = new Label();
        resultLabel.getStyleClass().add("projection-result");
        resultLabel.setWrapText(true);
        resultLabel.setMaxWidth(Double.MAX_VALUE);

        VBox section = new VBox(8, header, inputRow, resultLabel);
        section.getStyleClass().add("projection-section");

        Runnable update = () -> {
            resultLabel.getStyleClass().removeAll(
                    "projection-good", "projection-warn", "projection-hard", "projection-impossible");
            Course c = selectedCourse.get();
            if (c == null) {
                resultLabel.setText("");
                return;
            }
            String raw = targetField.getText() == null ? "" : targetField.getText().trim();
            if (raw.isEmpty()) {
                resultLabel.setText("Enter a target grade");
                return;
            }
            double target;
            try {
                target = Double.parseDouble(raw);
            } catch (NumberFormatException ex) {
                resultLabel.setText("Enter a number between 0 and 100");
                resultLabel.getStyleClass().add("projection-warn");
                return;
            }
            if (target < 0 || target > 100) {
                resultLabel.setText("Enter a number between 0 and 100");
                resultLabel.getStyleClass().add("projection-warn");
                return;
            }
            double needed = c.whatDoINeed(target);
            if (needed == 0.0) {
                resultLabel.setText("You've already achieved this grade!");
                resultLabel.getStyleClass().add("projection-good");
            } else if (needed < 0) {
                resultLabel.setText("Impossible — would require more than 100%");
                resultLabel.getStyleClass().add("projection-impossible");
            } else {
                resultLabel.setText(String.format("You need %.1f%% on remaining assessments", needed));
                if (needed <= 80) resultLabel.getStyleClass().add("projection-good");
                else if (needed <= 95) resultLabel.getStyleClass().add("projection-warn");
                else resultLabel.getStyleClass().add("projection-hard");
            }
        };

        targetField.textProperty().addListener((o, ov, nv) -> update.run());

        ChangeListener<Number> gradeListener = (o, ov, nv) -> update.run();
        final Course[] bound = { null };
        Runnable rebind = () -> {
            Course old = bound[0];
            Course cur = selectedCourse.get();
            if (old != null) old.finalGradeProperty().removeListener(gradeListener);
            bound[0] = cur;
            if (cur != null) cur.finalGradeProperty().addListener(gradeListener);
            update.run();
        };
        selectedCourse.addListener((o, ov, nv) -> rebind.run());
        rebind.run();

        return section;
    }

    private static String policyTypeLabelFor(GradingPolicy p) {
        if (p instanceof WeightedGrading) return "Weighted";
        if (p instanceof PointsBasedGrading) return "Points-Based";
        if (p instanceof CurvedGrading) return "Curved";
        return "";
    }

    private static String letterGradeFor(double g) {
        if (g >= 97) return "1.00";
        if (g >= 94) return "1.25";
        if (g >= 91) return "1.50";
        if (g >= 88) return "1.75";
        if (g >= 85) return "2.00";
        if (g >= 82) return "2.25";
        if (g >= 79) return "2.50";
        if (g >= 76) return "2.75";
        if (g >= 75) return "3.00";
        return "5.00";
    }

    private VBox createDashboardPane() {
        return createPlaceholder("Dashboard", "GPA overview and statistics");
    }

    private VBox createSchedulePane() {
        Label header = new Label("Schedule");
        header.getStyleClass().add("section-header");

        ComboBox<Term> termCombo = new ComboBox<>(student.getTerms());
        termCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Term term) {
                if (term == null) return "";
                return term.getName() + " (" + term.getSemester() + " " + term.getYear() + ")";
            }
            @Override
            public Term fromString(String s) {
                return null;
            }
        });
        termCombo.setPromptText("Select a term");
        termCombo.getStyleClass().add("term-combo");
        if (!student.getTerms().isEmpty()) {
            termCombo.getSelectionModel().select(0);
        }

        HBox selectors = new HBox(12, new Label("Term:"), termCombo);
        selectors.setAlignment(Pos.CENTER_LEFT);
        selectors.getStyleClass().add("term-controls");

        Label noTermSelected = new Label("Select a term to manage schedules");
        noTermSelected.getStyleClass().add("empty-prompt");

        Label noCoursesLabel = new Label("No courses in this term");
        noCoursesLabel.getStyleClass().add("empty-prompt");

        VBox courseSections = new VBox(18);
        courseSections.getStyleClass().add("schedule-sections");

        ScrollPane scroll = new ScrollPane(courseSections);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("schedule-scroll");

        StackPane body = new StackPane(noTermSelected, noCoursesLabel, scroll);
        body.getStyleClass().add("courses-body");

        final ListChangeListener<Course>[] courseListener = new ListChangeListener[1];
        final Term[] boundTerm = { null };

        Runnable rebuildSections = () -> {
            courseSections.getChildren().clear();
            Term sel = termCombo.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            for (Course course : sel.getCourses()) {
                courseSections.getChildren().add(createCourseScheduleSection(course));
            }
        };

        Runnable rebind = () -> {
            Term old = boundTerm[0];
            Term cur = termCombo.getSelectionModel().getSelectedItem();
            if (old != null && courseListener[0] != null) {
                old.getCourses().removeListener(courseListener[0]);
            }
            boundTerm[0] = cur;
            if (cur != null) {
                courseListener[0] = c -> rebuildSections.run();
                cur.getCourses().addListener(courseListener[0]);
            }
            rebuildSections.run();
        };
        termCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> rebind.run());
        rebind.run();

        noTermSelected.visibleProperty().bind(termCombo.getSelectionModel().selectedItemProperty().isNull());
        noTermSelected.managedProperty().bind(noTermSelected.visibleProperty());

        noCoursesLabel.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            Term t = termCombo.getSelectionModel().getSelectedItem();
            return t != null && t.getCourses().isEmpty();
        }, termCombo.getSelectionModel().selectedItemProperty(), courseSections.getChildren()));
        noCoursesLabel.managedProperty().bind(noCoursesLabel.visibleProperty());

        scroll.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            Term t = termCombo.getSelectionModel().getSelectedItem();
            return t != null && !t.getCourses().isEmpty();
        }, termCombo.getSelectionModel().selectedItemProperty(), courseSections.getChildren()));
        scroll.managedProperty().bind(scroll.visibleProperty());

        VBox pane = new VBox(16, header, selectors, new Separator(), body);
        pane.setPadding(new Insets(16));
        VBox.setVgrow(body, Priority.ALWAYS);
        return pane;
    }

    private VBox createCourseScheduleSection(Course course) {
        Label title = new Label(course.getName() + " (" + course.getCode() + ")");
        title.getStyleClass().add("schedule-course-title");

        TableView<TimeSlot> table = new TableView<>(course.getTimeSlots());
        table.getStyleClass().add("course-table");
        table.setPlaceholder(new Label("No time slots yet"));
        table.setPrefHeight(180);

        TableColumn<TimeSlot, String> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getDayOfWeek().name()));
        dayCol.setPrefWidth(140);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        TableColumn<TimeSlot, String> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getStartTime().format(timeFmt)));
        startCol.setPrefWidth(100);

        TableColumn<TimeSlot, String> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getEndTime().format(timeFmt)));
        endCol.setPrefWidth(100);

        TableColumn<TimeSlot, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getRoom() == null ? "" : cd.getValue().getRoom()));
        roomCol.setPrefWidth(160);

        table.getColumns().addAll(dayCol, startCol, endCol, roomCol);

        Button addButton = new Button("Add Time Slot");
        addButton.getStyleClass().addAll("aq-button", "aq-button-primary");
        addButton.setOnAction(e -> showTimeSlotDialog().ifPresent(ts -> {
            course.addTimeSlot(ts);
            table.getSelectionModel().select(ts);
            store.scheduleSave(student);
        }));

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll("aq-button", "aq-button-danger");
        removeButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        removeButton.setOnAction(e -> {
            TimeSlot sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            course.getTimeSlots().remove(sel);
            store.scheduleSave(student);
        });

        HBox buttonBar = new HBox(12, addButton, removeButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        VBox section = new VBox(8, title, table, buttonBar);
        section.getStyleClass().add("schedule-course-section");
        return section;
    }

    private Optional<TimeSlot> showTimeSlotDialog() {
        Dialog<TimeSlot> dialog = new Dialog<>();
        dialog.setTitle("Add Time Slot");
        dialog.setHeaderText("New time slot");

        ButtonType okType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        ComboBox<DayOfWeek> dayCombo = new ComboBox<>(FXCollections.observableArrayList(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY));
        dayCombo.getSelectionModel().select(DayOfWeek.MONDAY);

        Spinner<Integer> startHour = new Spinner<>(7, 21, 8);
        startHour.setEditable(true);
        startHour.setPrefWidth(80);
        Spinner<Integer> startMin = new Spinner<>();
        startMin.setValueFactory(new SpinnerValueFactory.ListSpinnerValueFactory<>(
                FXCollections.observableArrayList(0, 15, 30, 45)));
        startMin.getValueFactory().setValue(30);
        startMin.setEditable(true);
        startMin.setPrefWidth(80);

        Spinner<Integer> endHour = new Spinner<>(7, 21, 10);
        endHour.setEditable(true);
        endHour.setPrefWidth(80);
        Spinner<Integer> endMin = new Spinner<>();
        endMin.setValueFactory(new SpinnerValueFactory.ListSpinnerValueFactory<>(
                FXCollections.observableArrayList(0, 15, 30, 45)));
        endMin.getValueFactory().setValue(0);
        endMin.setEditable(true);
        endMin.setPrefWidth(80);

        TextField roomField = new TextField();
        roomField.setPromptText("e.g. CL-301");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("timeslot-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        HBox startBox = new HBox(6, startHour, new Label(":"), startMin);
        startBox.setAlignment(Pos.CENTER_LEFT);
        HBox endBox = new HBox(6, endHour, new Label(":"), endMin);
        endBox.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8, 0, 8, 0));
        grid.add(new Label("Day:"), 0, 0);
        grid.add(dayCombo, 1, 0);
        grid.add(new Label("Start:"), 0, 1);
        grid.add(startBox, 1, 1);
        grid.add(new Label("End:"), 0, 2);
        grid.add(endBox, 1, 2);
        grid.add(new Label("Room:"), 0, 3);
        grid.add(roomField, 1, 3);
        grid.add(errorLabel, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStyleClass().add("aq-dialog");

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);

        Runnable validate = () -> {
            LocalTime start = LocalTime.of(startHour.getValue(), startMin.getValue());
            LocalTime end = LocalTime.of(endHour.getValue(), endMin.getValue());
            boolean valid = end.isAfter(start);
            if (!valid) {
                errorLabel.setText("End time must be after start time");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            } else {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            }
            okButton.setDisable(!valid);
        };
        startHour.valueProperty().addListener((o, ov, nv) -> validate.run());
        startMin.valueProperty().addListener((o, ov, nv) -> validate.run());
        endHour.valueProperty().addListener((o, ov, nv) -> validate.run());
        endMin.valueProperty().addListener((o, ov, nv) -> validate.run());
        validate.run();

        dialog.setResultConverter(bt -> {
            if (bt != okType) return null;
            LocalTime start = LocalTime.of(startHour.getValue(), startMin.getValue());
            LocalTime end = LocalTime.of(endHour.getValue(), endMin.getValue());
            return new TimeSlot(dayCombo.getValue(), start, end, roomField.getText().trim());
        });

        return dialog.showAndWait();
    }

    private VBox createPlaceholder(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("placeholder-title");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("placeholder-subtitle");

        VBox box = new VBox(title, subtitle);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
