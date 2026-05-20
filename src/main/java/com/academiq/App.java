package com.academiq;

import com.academiq.grading.CurvedGrading;
import com.academiq.grading.GradingPolicy;
import com.academiq.grading.PointsBasedGrading;
import com.academiq.grading.WeightedGrading;
import com.academiq.model.Course;
import com.academiq.model.Student;
import com.academiq.model.Term;
import com.academiq.persistence.SqliteDataStore;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

import java.time.Year;
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
        return createPlaceholder("Grade Entry", "Select a course to enter grades");
    }

    private VBox createDashboardPane() {
        return createPlaceholder("Dashboard", "GPA overview and statistics");
    }

    private VBox createSchedulePane() {
        return createPlaceholder("Schedule", "Weekly class schedule and conflicts");
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
