package com.library.controller;

import com.library.model.Student;
import com.library.service.StudentService;
import com.library.util.AlertUtil;
import com.library.util.SceneManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class StudentManagementController {

    @FXML private TextField searchField;
    @FXML private Button addStudentButton;
    @FXML private Button clearFiltersButton;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> colStudentNumber;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colDepartment;
    @FXML private TableColumn<Student, Number> colSemester;
    @FXML private TableColumn<Student, String> colPhone;
    @FXML private TableColumn<Student, Student> colStatus;
    @FXML private TableColumn<Student, Student> colActions;
    @FXML private Label pageLabel;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;

    private final StudentService studentService = new StudentService();
    private List<Student> filteredStudents = List.of();
    private int currentPage = 0;
    private static final int PAGE_SIZE = 8;

    @FXML
    public void initialize() {
        colStudentNumber.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentNumber()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colDepartment.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDepartment()));
        colSemester.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSemester()));
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone()));

        colStatus.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Student student, boolean empty) {
                super.updateItem(student, empty);
                if (empty || student == null) {
                    setGraphic(null);
                    return;
                }
                boolean active = student.getStatus() == Student.Status.ACTIVE;
                Label badge = new Label(active ? "Active" : "Inactive");
                badge.getStyleClass().addAll("badge", active ? "badge-success" : "badge-neutral");
                setGraphic(badge);
            }
        });

        colActions.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(6, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-icon-small");
                deleteBtn.getStyleClass().add("btn-icon-small");
                editBtn.setOnAction(e -> openStudentForm(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Student student, boolean empty) {
                super.updateItem(student, empty);
                setGraphic(empty || student == null ? null : box);
            }
        });

        searchField.textProperty().addListener((obs, old, val) -> refresh());
        refresh();
    }

    @FXML
    private void handleAddStudent() {
        openStudentForm(null);
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        refresh();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleNextPage() {
        currentPage++;
        renderPage();
    }

    private void handleDelete(Student student) {
        boolean confirmed = AlertUtil.confirm("Delete Student",
                "Delete " + student.getFullName() + "? This can't be undone.");
        if (!confirmed) return;
        try {
            studentService.delete(student.getStudentId());
            AlertUtil.success("Student Removed", student.getFullName() + " was removed.");
            refresh();
        } catch (Exception e) {
            AlertUtil.error("Cannot Delete Student", e.getMessage());
        }
    }

    private void refresh() {
        String keyword = searchField.getText();
        filteredStudents = (keyword == null || keyword.isBlank())
                ? studentService.getAll()
                : studentService.search(keyword.trim());
        currentPage = 0;
        renderPage();
    }

    private void renderPage() {
        int totalPages = Math.max(1, (int) Math.ceil(filteredStudents.size() / (double) PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredStudents.size());

        studentsTable.setItems(FXCollections.observableArrayList(filteredStudents.subList(from, Math.max(from, to))));
        pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages + "  (" + filteredStudents.size() + " students)");
        prevPageButton.setDisable(currentPage == 0);
        nextPageButton.setDisable(currentPage >= totalPages - 1);
    }

    private void openStudentForm(Student existing) {
        SceneManager.LoadResult<StudentFormController> result = SceneManager.loadInto("/fxml/student_form.fxml");
        Stage dialog = new Stage();
        dialog.initOwner(SceneManager.getPrimaryStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(existing == null ? "Register Student" : "Edit Student");
        dialog.setResizable(false);

        Scene scene = new Scene(result.node());
        SceneManager.applyTheme(scene);
        dialog.setScene(scene);

        result.controller().init(studentService, existing, this::refresh);
        dialog.showAndWait();
    }
}
