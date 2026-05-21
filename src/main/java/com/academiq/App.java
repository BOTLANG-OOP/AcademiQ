package com.academiq;

import com.academiq.grading.CurvedGrading;
import com.academiq.grading.GradingPolicy;
import com.academiq.grading.PointsBasedGrading;
import com.academiq.grading.WeightedGrading;
import com.academiq.model.Assessment;
import com.academiq.model.ConflictRecord;
import com.academiq.model.Course;
import com.academiq.model.Student;
import com.academiq.model.Term;
import com.academiq.model.TimeSlot;
import com.academiq.persistence.SqliteDataStore;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
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
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
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

    private Button sidebarToggleButton;
    private boolean sidebarExpanded = true;
    private static final double SIDEBAR_EXPANDED_WIDTH = 220;
    private static final double SIDEBAR_COLLAPSED_WIDTH = 56;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        store = new SqliteDataStore();
        if (store.hasConnectionError() || !store.isDatabaseHealthy()) {
            if (!handleCorruptedDatabase()) {
                return;
            }
        }
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

    private boolean handleCorruptedDatabase() {
        ButtonType startFresh = new ButtonType("Start Fresh", ButtonBar.ButtonData.OK_DONE);
        ButtonType quit = new ButtonType("Quit", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.setHeaderText("AcademiQ could not read its data file.");
        alert.setContentText("The database may be corrupted. Would you like to start fresh?\n"
                + "(This will delete all saved data.)");
        alert.getButtonTypes().setAll(startFresh, quit);
        alert.getDialogPane().getStyleClass().add("aq-dialog");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == startFresh) {
            store.close();
            SqliteDataStore.deleteDatabase("academiq.db");
            store = new SqliteDataStore();
            return true;
        }
        Platform.exit();
        return false;
    }

    private VBox createSidebar() {
        VBox box = new VBox();
        box.getStyleClass().addAll("sidebar", "sidebar-expanded");

        Label title = new Label("AcademiQ");
        title.getStyleClass().add("app-title");

        sidebarToggleButton = new Button("☰");
        sidebarToggleButton.getStyleClass().add("sidebar-inline-toggle");
        sidebarToggleButton.setFocusTraversable(false);
        sidebarToggleButton.setOnAction(e -> toggleSidebar());

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox titleRow = new HBox(title, titleSpacer, sidebarToggleButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getStyleClass().add("sidebar-title-row");

        Label studentName = new Label(student.getName());
        studentName.getStyleClass().add("sidebar-student-name");

        Label studentId = new Label(student.getId());
        studentId.getStyleClass().add("sidebar-student-id");

        Separator separator = new Separator();

        coursesButton = createNavButton("📚", "Courses", "courses");
        gradesButton = createNavButton("📝", "Grade Entry", "grades");
        dashboardButton = createNavButton("📊", "Dashboard", "dashboard");
        scheduleButton = createNavButton("📅", "Schedule", "schedule");

        VBox navButtons = new VBox(4, coursesButton, gradesButton, dashboardButton, scheduleButton);
        navButtons.setPadding(new Insets(0, 6, 0, 6));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label version = new Label("v1.0");
        version.getStyleClass().add("version-label");

        box.getChildren().addAll(titleRow, studentName, studentId, separator,
                navButtons, spacer, version);
        box.setPrefWidth(SIDEBAR_EXPANDED_WIDTH);
        box.setMinWidth(SIDEBAR_EXPANDED_WIDTH);
        box.setMaxWidth(SIDEBAR_EXPANDED_WIDTH);
        return box;
    }

    private Button createNavButton(String emoji, String label, String viewName) {
        Button button = new Button(emoji + "  " + label);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setUserData(label);
        button.setOnAction(e -> navigateTo(viewName));
        return button;
    }

    private void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        double targetWidth = sidebarExpanded ? SIDEBAR_EXPANDED_WIDTH : SIDEBAR_COLLAPSED_WIDTH;

        Timeline timeline = new Timeline(
            new KeyFrame(
                javafx.util.Duration.millis(200),
                new KeyValue(sidebar.prefWidthProperty(), targetWidth),
                new KeyValue(sidebar.minWidthProperty(), targetWidth),
                new KeyValue(sidebar.maxWidthProperty(), targetWidth)
            )
        );
        timeline.play();

        for (Node child : sidebar.getChildren()) {
            if (child.getStyleClass().contains("sidebar-title-row") && child instanceof HBox row) {
                row.setAlignment(sidebarExpanded ? Pos.CENTER_LEFT : Pos.CENTER);
                for (Node sub : row.getChildren()) {
                    if (sub.getStyleClass().contains("app-title")) {
                        sub.setVisible(sidebarExpanded);
                        sub.setManaged(sidebarExpanded);
                    }
                }
            }
            if (child.getStyleClass().contains("sidebar-student-name")
                || child.getStyleClass().contains("sidebar-student-id")
                || child.getStyleClass().contains("version-label")) {
                child.setVisible(sidebarExpanded);
                child.setManaged(sidebarExpanded);
            }
        }


        for (Node btn : new Node[]{coursesButton, gradesButton, dashboardButton, scheduleButton}) {
            if (btn instanceof Button b) {
                String label = (String) b.getUserData();
                String currentText = b.getText();
                int spaceIdx = currentText.indexOf(" ");
                String emoji = spaceIdx > 0 ? currentText.substring(0, spaceIdx).trim() : currentText.trim();
                if (sidebarExpanded) {
                    b.setText(emoji + "  " + label);
                    b.setAlignment(Pos.CENTER_LEFT);
                } else {
                    b.setText(emoji);
                    b.setAlignment(Pos.CENTER);
                }
            }
        }

        sidebar.getStyleClass().removeAll("sidebar-expanded", "sidebar-collapsed");
        sidebar.getStyleClass().add(sidebarExpanded ? "sidebar-expanded" : "sidebar-collapsed");
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
        addTermButton.getStyleClass().addAll(Styles.ACCENT);
        addTermButton.setOnAction(e -> {
            Optional<Term> created = showAddTermDialog();
            created.ifPresent(t -> {
                student.addTerm(t);
                termCombo.getSelectionModel().select(t);
                store.scheduleSave(student);
            });
        });

        Button removeTermButton = new Button("Remove Term");
        removeTermButton.getStyleClass().addAll(Styles.DANGER);
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
        courseTable.getStyleClass().addAll(Styles.BORDERED, Styles.STRIPED);
        courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
        addCourseButton.getStyleClass().addAll(Styles.ACCENT);
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
        editCourseButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        editCourseButton.disableProperty().bind(courseTable.getSelectionModel().selectedItemProperty().isNull());
        editCourseButton.setOnAction(e -> {
            Course selected = courseTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            showCourseDialog(selected).ifPresent(c -> store.scheduleSave(student));
        });

        Button removeCourseButton = new Button("Remove Course");
        removeCourseButton.getStyleClass().addAll(Styles.DANGER);
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
        clampIntegerSpinner(yearSpinner, 2020, 2030);

        ComboBox<String> semesterCombo = new ComboBox<>();
        semesterCombo.getItems().addAll("First", "Second", "Summer");
        semesterCombo.getSelectionModel().selectFirst();

        Label nameError = makeErrorLabel("Name is required");
        Label yearError = makeErrorLabel("Year must be between 2020 and 2030");
        Label semError = makeErrorLabel("Semester is required");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(8, 0, 8, 0));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(nameError, 1, 1);
        grid.add(new Label("Year:"), 0, 2);
        grid.add(yearSpinner, 1, 2);
        grid.add(yearError, 1, 3);
        grid.add(new Label("Semester:"), 0, 4);
        grid.add(semesterCombo, 1, 4);
        grid.add(semError, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStyleClass().add("aq-dialog");

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);
        Runnable validate = () -> {
            boolean nameOk = !nameField.getText().trim().isEmpty();
            Integer y = yearSpinner.getValue();
            boolean yearOk = y != null && y >= 2020 && y <= 2030;
            boolean semOk = semesterCombo.getValue() != null;
            setFieldError(nameField, !nameOk);
            setFieldError(yearSpinner, !yearOk);
            setFieldError(semesterCombo, !semOk);
            showError(nameError, !nameOk);
            showError(yearError, !yearOk);
            showError(semError, !semOk);
            okButton.setDisable(!(nameOk && yearOk && semOk));
        };
        nameField.textProperty().addListener((o, ov, nv) -> validate.run());
        yearSpinner.valueProperty().addListener((o, ov, nv) -> validate.run());
        yearSpinner.getEditor().textProperty().addListener((o, ov, nv) -> validate.run());
        semesterCombo.valueProperty().addListener((o, ov, nv) -> validate.run());
        validate.run();

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
        clampIntegerSpinner(unitsSpinner, 1, 6);

        if (editing) {
            nameField.setDisable(true);
            codeField.setDisable(true);
            unitsSpinner.setDisable(true);
        }

        Label nameError = makeErrorLabel("Name is required");
        Label codeError = makeErrorLabel("Code is required");
        Label unitsError = makeErrorLabel("Units must be between 1 and 6");

        ComboBox<String> policyTypeCombo = new ComboBox<>();
        policyTypeCombo.getItems().addAll("Weighted", "Points-Based", "Curved");

        VBox configArea = new VBox(8);
        configArea.getStyleClass().add("policy-config-area");

        PolicyEditor topEditor = new PolicyEditor(policyTypeCombo, configArea, existing == null ? null : existing.getGradingPolicy(), false);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(8, 0, 8, 0));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(nameError, 1, 1);
        grid.add(new Label("Code:"), 0, 2);
        grid.add(codeField, 1, 2);
        grid.add(codeError, 1, 3);
        grid.add(new Label("Units:"), 0, 4);
        grid.add(unitsSpinner, 1, 4);
        grid.add(unitsError, 1, 5);
        grid.add(new Label("Grading:"), 0, 6);
        grid.add(policyTypeCombo, 1, 6);
        grid.add(configArea, 0, 7, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStyleClass().add("aq-dialog");

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);
        Runnable updateOk = () -> {
            boolean nameOk = editing || !nameField.getText().trim().isEmpty();
            boolean codeOk = editing || !codeField.getText().trim().isEmpty();
            Integer u = unitsSpinner.getValue();
            boolean unitsOk = editing || (u != null && u >= 1 && u <= 6);
            if (!editing) {
                setFieldError(nameField, !nameOk);
                setFieldError(codeField, !codeOk);
                setFieldError(unitsSpinner, !unitsOk);
                showError(nameError, !nameOk);
                showError(codeError, !codeOk);
                showError(unitsError, !unitsOk);
            }
            okButton.setDisable(!(nameOk && codeOk && unitsOk && topEditor.isValid()));
        };
        nameField.textProperty().addListener((o, ov, nv) -> updateOk.run());
        codeField.textProperty().addListener((o, ov, nv) -> updateOk.run());
        unitsSpinner.valueProperty().addListener((o, ov, nv) -> updateOk.run());
        unitsSpinner.getEditor().textProperty().addListener((o, ov, nv) -> updateOk.run());
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
            addRow.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
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
            remove.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
            HBox row = new HBox(8, nameF, weightF, remove);
            row.setAlignment(Pos.CENTER_LEFT);
            remove.setOnAction(e -> {
                weightedRows.getChildren().remove(row);
                recomputeWeightedTotal();
                onValidityChanged.run();
            });
            Runnable revalidateRow = () -> {
                setFieldError(nameF, nameF.getText().trim().isEmpty());
                Double w = parseDouble(weightF);
                setFieldError(weightF, w == null || w <= 0);
            };
            weightF.textProperty().addListener((o, ov, nv) -> {
                revalidateRow.run();
                recomputeWeightedTotal();
                onValidityChanged.run();
            });
            nameF.textProperty().addListener((o, ov, nv) -> {
                revalidateRow.run();
                onValidityChanged.run();
            });
            weightedRows.getChildren().add(row);
            revalidateRow.run();
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
            weightedTotal.setText(String.format("Weights sum: %.2f", total));
            weightedTotal.getStyleClass().removeAll("weights-sum-error", "weights-sum-ok");
            if (Math.abs(total - 1.0) > 0.01) {
                weightedTotal.getStyleClass().add("weights-sum-error");
            } else {
                weightedTotal.getStyleClass().add("weights-sum-ok");
            }
        }

        private void buildPoints(PointsBasedGrading initial) {
            pointsField = new TextField("100.0");
            pointsField.textProperty().addListener((o, ov, nv) -> {
                Double v = parseDouble(pointsField);
                setFieldError(pointsField, v == null || v <= 0);
                onValidityChanged.run();
            });
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
            curveField.textProperty().addListener((o, ov, nv) -> {
                Double v = parseDouble(curveField);
                setFieldError(curveField, v == null || v < 0);
                onValidityChanged.run();
            });
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
                case "Curved" -> {
                    Double cv = parseDouble(curveField);
                    yield cv != null && cv >= 0 && baseEditor != null && baseEditor.isValid();
                }
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
            return Math.abs(total - 1.0) <= 0.01;
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
        table.getStyleClass().addAll(Styles.BORDERED, Styles.STRIPED);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
        addButton.getStyleClass().addAll(Styles.ACCENT);
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
        removeButton.getStyleClass().addAll(Styles.DANGER);
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

    private static Label makeErrorLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("error-label");
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    private static void showError(Label l, boolean show) {
        l.setVisible(show);
        l.setManaged(show);
    }

    private static void setFieldError(Control c, boolean error) {
        if (error) {
            if (!c.getStyleClass().contains("field-error")) {
                c.getStyleClass().add("field-error");
            }
        } else {
            c.getStyleClass().removeAll("field-error");
        }
    }

    private static void setFieldError(Node n, boolean error) {
        if (error) {
            if (!n.getStyleClass().contains("field-error")) {
                n.getStyleClass().add("field-error");
            }
        } else {
            n.getStyleClass().removeAll("field-error");
        }
    }

    private static void clampIntegerSpinner(Spinner<Integer> spinner, int min, int max) {
        spinner.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) {
                String txt = spinner.getEditor().getText();
                try {
                    int v = Integer.parseInt(txt.trim());
                    if (v < min) v = min;
                    if (v > max) v = max;
                    spinner.getValueFactory().setValue(v);
                } catch (NumberFormatException ex) {
                    Integer cur = spinner.getValue();
                    spinner.getEditor().setText(cur == null ? String.valueOf(min) : String.valueOf(cur));
                }
            }
        });
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

        Label titleError = makeErrorLabel("Title is required");
        Label categoryError = makeErrorLabel("Category is required");
        Label scoreError = makeErrorLabel("Score must be a number (-1 = ungraded)");
        Label scoreWarn = makeErrorLabel("Score exceeds max score");
        Label maxError = makeErrorLabel("Max score must be greater than 0");
        Label weightError = makeErrorLabel("Weight must be greater than 0");
        Label dateError = makeErrorLabel("Date is required");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(8, 0, 8, 0));
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(titleError, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(categoryError, 1, 3);
        grid.add(new Label("Score:"), 0, 4);
        grid.add(scoreField, 1, 4);
        grid.add(scoreError, 1, 5);
        grid.add(scoreWarn, 1, 6);
        grid.add(new Label("Max Score:"), 0, 7);
        grid.add(maxScoreField, 1, 7);
        grid.add(maxError, 1, 8);
        grid.add(new Label("Weight:"), 0, 9);
        grid.add(weightField, 1, 9);
        grid.add(weightError, 1, 10);
        grid.add(new Label("Date:"), 0, 11);
        grid.add(datePicker, 1, 11);
        grid.add(dateError, 1, 12);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStyleClass().add("aq-dialog");

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);
        Runnable updateOk = () -> {
            boolean titleOk = !titleField.getText().trim().isEmpty();
            String catText = categoryCombo.getEditor().getText();
            boolean catOk = catText != null && !catText.trim().isEmpty();
            Double scoreVal = parseDoubleOrNull(scoreField.getText());
            boolean scoreOk = scoreVal != null && scoreVal >= -1;
            Double maxVal = parsePositiveDoubleOrNull(maxScoreField.getText());
            boolean maxOk = maxVal != null;
            Double weightVal = parsePositiveDoubleOrNull(weightField.getText());
            boolean weightOk = weightVal != null;
            boolean dateOk = datePicker.getValue() != null;
            boolean scoreExceeds = scoreOk && maxOk && scoreVal > maxVal;

            setFieldError(titleField, !titleOk);
            setFieldError(categoryCombo, !catOk);
            setFieldError(scoreField, !scoreOk || scoreExceeds);
            setFieldError(maxScoreField, !maxOk);
            setFieldError(weightField, !weightOk);
            setFieldError(datePicker, !dateOk);

            showError(titleError, !titleOk);
            showError(categoryError, !catOk);
            showError(scoreError, !scoreOk);
            showError(scoreWarn, scoreExceeds);
            showError(maxError, !maxOk);
            showError(weightError, !weightOk);
            showError(dateError, !dateOk);

            okButton.setDisable(!(titleOk && catOk && scoreOk && maxOk && weightOk && dateOk));
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
            if (parsed == null || parsed < -1) {
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

        private void applyExceedsStyle(Assessment a) {
            getStyleClass().removeAll("score-exceeds-max");
            setTooltip(null);
            if (a != null && a.isGraded() && a.getScore() > a.getMaxScore()) {
                getStyleClass().add("score-exceeds-max");
                setTooltip(new Tooltip("Score exceeds max score (" + formatNumber(a.getMaxScore()) + ")"));
            }
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
                applyExceedsStyle(a);
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
        Label header = new Label("Dashboard");
        header.getStyleClass().add("section-header");

        // --- Section 1: Cumulative GPA ---
        Label cumulativeCaption = new Label("Cumulative GPA");
        cumulativeCaption.getStyleClass().add("dashboard-caption");

        Label cumulativeValue = new Label();
        cumulativeValue.getStyleClass().add("dashboard-cumulative-value");
        cumulativeValue.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("%.2f", student.getCumulativeGPA()),
                student.cumulativeGPAProperty()));

        ProgressBar cumulativeBar = new ProgressBar(0);
        cumulativeBar.getStyleClass().add("dashboard-cumulative-bar");
        cumulativeBar.setMaxWidth(Double.MAX_VALUE);
        cumulativeBar.progressProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0.0, Math.min(1.0, student.getCumulativeGPA() / 5.0)),
                student.cumulativeGPAProperty()));

        Runnable applyCumulativeColor = () -> {
            cumulativeBar.getStyleClass().removeAll(
                    "gpa-excellent", "gpa-good", "gpa-fair", "gpa-poor");
            cumulativeValue.getStyleClass().removeAll(
                    "gpa-text-excellent", "gpa-text-good", "gpa-text-fair", "gpa-text-poor");
            double g = student.getCumulativeGPA();
            String barClass = gpaColorClass(g);
            cumulativeBar.getStyleClass().add(barClass);
            cumulativeValue.getStyleClass().add("gpa-text-" + barClass.substring("gpa-".length()));
        };
        student.cumulativeGPAProperty().addListener((o, ov, nv) -> applyCumulativeColor.run());
        applyCumulativeColor.run();

        VBox cumulativeCard = new VBox(8, cumulativeCaption, cumulativeValue, cumulativeBar);
        cumulativeCard.getStyleClass().add("dashboard-cumulative-card");

        // --- Section 2: Per-term breakdown ---
        Label termsHeader = new Label("Terms");
        termsHeader.getStyleClass().add("dashboard-subheader");

        Label noTermsLabel = new Label("No terms yet. Add a term in Courses to see GPA breakdown.");
        noTermsLabel.getStyleClass().add("empty-prompt");
        noTermsLabel.visibleProperty().bind(Bindings.isEmpty(student.getTerms()));
        noTermsLabel.managedProperty().bind(noTermsLabel.visibleProperty());

        ObjectProperty<Term> selectedTerm = new SimpleObjectProperty<>();

        VBox termCards = new VBox(12);
        termCards.getStyleClass().add("dashboard-term-cards");

        Runnable rebuildTermCards = () -> {
            termCards.getChildren().clear();
            for (Term t : student.getTerms()) {
                termCards.getChildren().add(createTermCard(t, selectedTerm));
            }
            Term cur = selectedTerm.get();
            if (cur != null && !student.getTerms().contains(cur)) {
                selectedTerm.set(null);
            }
        };
        student.getTerms().addListener((ListChangeListener<Term>) c -> rebuildTermCards.run());
        rebuildTermCards.run();

        ScrollPane termScroll = new ScrollPane(termCards);
        termScroll.setFitToWidth(true);
        termScroll.getStyleClass().add("dashboard-scroll");
        termScroll.setPrefHeight(280);

        VBox termsSection = new VBox(8, termsHeader, noTermsLabel, termScroll);

        // --- Section 3: Course-level detail (visible when a term is selected) ---
        Label coursesHeader = new Label();
        coursesHeader.getStyleClass().add("dashboard-subheader");
        coursesHeader.textProperty().bind(Bindings.createStringBinding(() -> {
            Term t = selectedTerm.get();
            return t == null ? "Courses" : "Courses — " + t.getName();
        }, selectedTerm));

        VBox courseCards = new VBox(8);
        courseCards.getStyleClass().add("dashboard-course-cards");

        Label pickTermHint = new Label("Select a term above to view its courses");
        pickTermHint.getStyleClass().add("empty-prompt");

        ScrollPane courseScroll = new ScrollPane(courseCards);
        courseScroll.setFitToWidth(true);
        courseScroll.getStyleClass().add("dashboard-scroll");
        courseScroll.setPrefHeight(220);

        final ListChangeListener<Course> coursesListener = c -> rebuildCourseCards(selectedTerm.get(), courseCards);
        final Term[] boundCoursesTerm = { null };
        Runnable rebindCourses = () -> {
            Term old = boundCoursesTerm[0];
            Term cur = selectedTerm.get();
            if (old != null) old.getCourses().removeListener(coursesListener);
            boundCoursesTerm[0] = cur;
            if (cur != null) cur.getCourses().addListener(coursesListener);
            rebuildCourseCards(cur, courseCards);
        };
        selectedTerm.addListener((o, ov, nv) -> rebindCourses.run());
        rebindCourses.run();

        pickTermHint.visibleProperty().bind(selectedTerm.isNull());
        pickTermHint.managedProperty().bind(pickTermHint.visibleProperty());
        courseScroll.visibleProperty().bind(selectedTerm.isNotNull());
        courseScroll.managedProperty().bind(courseScroll.visibleProperty());

        VBox coursesSection = new VBox(8, coursesHeader, pickTermHint, courseScroll);

        VBox pane = new VBox(16, header, cumulativeCard, termsSection, coursesSection);
        pane.setPadding(new Insets(16));
        pane.getStyleClass().add("dashboard-pane");
        VBox.setVgrow(termsSection, Priority.SOMETIMES);
        VBox.setVgrow(coursesSection, Priority.SOMETIMES);
        return pane;
    }

    private VBox createTermCard(Term term, ObjectProperty<Term> selectedTerm) {
        Label name = new Label(term.getName() + " — " + term.getSemester() + " " + term.getYear());
        name.getStyleClass().add("dashboard-term-name");

        Label gpaLabel = new Label();
        gpaLabel.getStyleClass().add("dashboard-term-gpa");
        gpaLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("GPA %.2f", term.getTermGPA()),
                term.termGPAProperty()));

        Label unitsLabel = new Label();
        unitsLabel.getStyleClass().add("dashboard-term-units");
        unitsLabel.textProperty().bind(Bindings.createStringBinding(
                () -> term.getTotalUnits() + " units",
                term.getCourses(), term.termGPAProperty()));

        HBox topRow = new HBox(12, name, new Region(), gpaLabel, unitsLabel);
        HBox.setHgrow(topRow.getChildren().get(1), Priority.ALWAYS);
        topRow.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar(0);
        bar.getStyleClass().add("dashboard-term-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.progressProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0.0, Math.min(1.0, term.getTermGPA() / 5.0)),
                term.termGPAProperty()));

        Runnable applyColor = () -> {
            bar.getStyleClass().removeAll("gpa-excellent", "gpa-good", "gpa-fair", "gpa-poor");
            bar.getStyleClass().add(gpaColorClass(term.getTermGPA()));
        };
        term.termGPAProperty().addListener((o, ov, nv) -> applyColor.run());
        applyColor.run();

        Label codes = new Label();
        codes.getStyleClass().add("dashboard-term-codes");
        codes.setWrapText(true);
        codes.textProperty().bind(Bindings.createStringBinding(() -> {
            if (term.getCourses().isEmpty()) return "No courses";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < term.getCourses().size(); i++) {
                if (i > 0) sb.append(" · ");
                sb.append(term.getCourses().get(i).getCode());
            }
            return sb.toString();
        }, term.getCourses()));

        VBox card = new VBox(6, topRow, bar, codes);
        card.getStyleClass().add("dashboard-term-card");
        card.setOnMouseClicked(e -> selectedTerm.set(term));

        Runnable applySelectionStyle = () -> {
            boolean isSelected = selectedTerm.get() == term;
            if (isSelected && !card.getStyleClass().contains("dashboard-term-card-selected")) {
                card.getStyleClass().add("dashboard-term-card-selected");
            } else if (!isSelected) {
                card.getStyleClass().remove("dashboard-term-card-selected");
            }
        };
        selectedTerm.addListener((o, ov, nv) -> applySelectionStyle.run());
        applySelectionStyle.run();

        return card;
    }

    private static void rebuildCourseCards(Term term, VBox container) {
        container.getChildren().clear();
        if (term == null) return;
        for (Course course : term.getCourses()) {
            container.getChildren().add(createCourseDetailCard(course));
        }
    }

    private static VBox createCourseDetailCard(Course course) {
        Label title = new Label(course.getName() + " (" + course.getCode() + ")");
        title.getStyleClass().add("dashboard-course-title");

        Label units = new Label(course.getUnits() + " units");
        units.getStyleClass().add("dashboard-course-units");

        HBox topRow = new HBox(12, title, new Region(), units);
        HBox.setHgrow(topRow.getChildren().get(1), Priority.ALWAYS);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label grade = new Label();
        grade.getStyleClass().add("dashboard-course-grade");
        grade.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("%.2f", course.getFinalGrade()),
                course.finalGradeProperty()));

        Label letter = new Label();
        letter.getStyleClass().add("dashboard-course-letter");
        letter.textProperty().bind(Bindings.createStringBinding(
                () -> letterGradeFor(course.getFinalGrade()),
                course.finalGradeProperty()));

        HBox gradeRow = new HBox(10, grade, letter);
        gradeRow.setAlignment(Pos.BASELINE_LEFT);

        VBox card = new VBox(4, topRow, gradeRow);
        card.getStyleClass().add("dashboard-course-card");
        return card;
    }

    private static String gpaColorClass(double gpa) {
        if (gpa >= 4.0) return "gpa-excellent";
        if (gpa >= 3.0) return "gpa-good";
        if (gpa >= 2.0) return "gpa-fair";
        return "gpa-poor";
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

        VBox conflictPanel = new VBox(6);
        conflictPanel.getStyleClass().add("conflict-panel");

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

        DateTimeFormatter conflictTimeFmt = DateTimeFormatter.ofPattern("HH:mm");
        Runnable refreshConflicts = () -> {
            conflictPanel.getChildren().clear();
            conflictPanel.getStyleClass().removeAll("conflict-panel-ok", "conflict-panel-danger");
            Term t = termCombo.getSelectionModel().getSelectedItem();
            if (t == null) return;
            List<ConflictRecord> conflicts = t.detectConflicts();
            if (conflicts.isEmpty()) {
                conflictPanel.getStyleClass().add("conflict-panel-ok");
                Label ok = new Label("✓ No schedule conflicts");
                ok.getStyleClass().add("conflict-ok-text");
                conflictPanel.getChildren().add(ok);
            } else {
                conflictPanel.getStyleClass().add("conflict-panel-danger");
                Label heading = new Label(conflicts.size() + " schedule conflict"
                        + (conflicts.size() == 1 ? "" : "s") + " detected");
                heading.getStyleClass().add("conflict-heading");
                conflictPanel.getChildren().add(heading);
                for (ConflictRecord cr : conflicts) {
                    String line = String.format(
                            "⚠ CONFLICT: %s (%s %s–%s) overlaps with %s (%s %s–%s)",
                            cr.getCourseA().getCode(),
                            cr.getSlotA().getDayOfWeek().name(),
                            cr.getSlotA().getStartTime().format(conflictTimeFmt),
                            cr.getSlotA().getEndTime().format(conflictTimeFmt),
                            cr.getCourseB().getCode(),
                            cr.getSlotB().getDayOfWeek().name(),
                            cr.getSlotB().getStartTime().format(conflictTimeFmt),
                            cr.getSlotB().getEndTime().format(conflictTimeFmt));
                    Label l = new Label(line);
                    l.getStyleClass().add("conflict-line");
                    l.setWrapText(true);
                    conflictPanel.getChildren().add(l);
                }
            }
        };

        ListChangeListener<TimeSlot> slotListener = c -> refreshConflicts.run();

        Runnable rebind = () -> {
            Term old = boundTerm[0];
            Term cur = termCombo.getSelectionModel().getSelectedItem();
            if (old != null) {
                if (courseListener[0] != null) old.getCourses().removeListener(courseListener[0]);
                for (Course c : old.getCourses()) c.getTimeSlots().removeListener(slotListener);
            }
            boundTerm[0] = cur;
            if (cur != null) {
                courseListener[0] = (ListChangeListener<Course>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            for (Course added : change.getAddedSubList()) {
                                added.getTimeSlots().addListener(slotListener);
                            }
                        }
                        if (change.wasRemoved()) {
                            for (Course removed : change.getRemoved()) {
                                removed.getTimeSlots().removeListener(slotListener);
                            }
                        }
                    }
                    rebuildSections.run();
                    refreshConflicts.run();
                };
                cur.getCourses().addListener(courseListener[0]);
                for (Course c : cur.getCourses()) c.getTimeSlots().addListener(slotListener);
            }
            rebuildSections.run();
            refreshConflicts.run();
        };
        termCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> rebind.run());
        rebind.run();

        conflictPanel.visibleProperty().bind(termCombo.getSelectionModel().selectedItemProperty().isNotNull());
        conflictPanel.managedProperty().bind(conflictPanel.visibleProperty());

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

        VBox pane = new VBox(16, header, selectors, conflictPanel, new Separator(), body);
        pane.setPadding(new Insets(16));
        VBox.setVgrow(body, Priority.ALWAYS);
        return pane;
    }

    private VBox createCourseScheduleSection(Course course) {
        Label title = new Label(course.getName() + " (" + course.getCode() + ")");
        title.getStyleClass().add("schedule-course-title");

        TableView<TimeSlot> table = new TableView<>(course.getTimeSlots());
        table.getStyleClass().addAll(Styles.BORDERED, Styles.STRIPED);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
        addButton.getStyleClass().addAll(Styles.ACCENT);
        addButton.setOnAction(e -> showTimeSlotDialog().ifPresent(ts -> {
            course.addTimeSlot(ts);
            table.getSelectionModel().select(ts);
            store.scheduleSave(student);
        }));

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll(Styles.DANGER);
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
        clampIntegerSpinner(startHour, 7, 21);
        Spinner<Integer> startMin = new Spinner<>();
        startMin.setValueFactory(new SpinnerValueFactory.ListSpinnerValueFactory<>(
                FXCollections.observableArrayList(0, 15, 30, 45)));
        startMin.getValueFactory().setValue(30);
        startMin.setEditable(true);
        startMin.setPrefWidth(80);

        Spinner<Integer> endHour = new Spinner<>(7, 21, 10);
        endHour.setEditable(true);
        endHour.setPrefWidth(80);
        clampIntegerSpinner(endHour, 7, 21);
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

        Label dayError = makeErrorLabel("Day is required");
        Label roomError = makeErrorLabel("Room is required");

        HBox startBox = new HBox(6, startHour, new Label(":"), startMin);
        startBox.setAlignment(Pos.CENTER_LEFT);
        HBox endBox = new HBox(6, endHour, new Label(":"), endMin);
        endBox.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(8, 0, 8, 0));
        grid.add(new Label("Day:"), 0, 0);
        grid.add(dayCombo, 1, 0);
        grid.add(dayError, 1, 1);
        grid.add(new Label("Start:"), 0, 2);
        grid.add(startBox, 1, 2);
        grid.add(new Label("End:"), 0, 3);
        grid.add(endBox, 1, 3);
        grid.add(errorLabel, 0, 4, 2, 1);
        grid.add(new Label("Room:"), 0, 5);
        grid.add(roomField, 1, 5);
        grid.add(roomError, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStyleClass().add("aq-dialog");

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);

        Runnable validate = () -> {
            boolean dayOk = dayCombo.getValue() != null;
            boolean roomOk = !roomField.getText().trim().isEmpty();
            LocalTime start = LocalTime.of(startHour.getValue(), startMin.getValue());
            LocalTime end = LocalTime.of(endHour.getValue(), endMin.getValue());
            boolean timeOk = end.isAfter(start);
            if (!timeOk) {
                errorLabel.setText("End time must be after start time");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                setFieldError(endHour, true);
                setFieldError(endMin, true);
            } else {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
                setFieldError(endHour, false);
                setFieldError(endMin, false);
            }
            setFieldError(dayCombo, !dayOk);
            setFieldError(roomField, !roomOk);
            showError(dayError, !dayOk);
            showError(roomError, !roomOk);
            okButton.setDisable(!(dayOk && roomOk && timeOk));
        };
        dayCombo.valueProperty().addListener((o, ov, nv) -> validate.run());
        roomField.textProperty().addListener((o, ov, nv) -> validate.run());
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
