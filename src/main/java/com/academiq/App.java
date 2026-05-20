package com.academiq;

import com.academiq.model.Student;
import com.academiq.model.Term;
import com.academiq.persistence.SqliteDataStore;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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

        Label coursesPlaceholder = new Label("Select a course in this term to enter grades");
        coursesPlaceholder.getStyleClass().add("placeholder-subtitle");

        body.getChildren().addAll(emptyPrompt, coursesPlaceholder);
        emptyPrompt.visibleProperty().bind(Bindings.isEmpty(student.getTerms()));
        emptyPrompt.managedProperty().bind(emptyPrompt.visibleProperty());
        coursesPlaceholder.visibleProperty().bind(Bindings.isNotEmpty(student.getTerms()));
        coursesPlaceholder.managedProperty().bind(coursesPlaceholder.visibleProperty());

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
