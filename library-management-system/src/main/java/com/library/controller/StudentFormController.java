package com.library.controller;

import com.library.model.Student;
import com.library.service.StudentService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class StudentFormController {

    @FXML private Label formTitleLabel;
    @FXML private Label formErrorLabel;
    @FXML private ImageView photoPreview;
    @FXML private TextField photoPathField;
    @FXML private Button browsePhotoButton;
    @FXML private TextField fullNameField;
    @FXML private TextField studentNumberField;
    @FXML private TextField departmentField;
    @FXML private Spinner<Integer> semesterSpinner;
    @FXML private ComboBox<Student.Status> statusCombo;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private StudentService studentService;
    private Student editingStudent;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        semesterSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 1));
        statusCombo.setItems(FXCollections.observableArrayList(Student.Status.values()));
        statusCombo.setValue(Student.Status.ACTIVE);
        loadDefaultPhotoPreview();
    }

    public void init(StudentService studentService, Student existing, Runnable onSaved) {
        this.studentService = studentService;
        this.editingStudent = existing;
        this.onSaved = onSaved;

        if (existing != null) {
            formTitleLabel.setText("Edit Student");
            saveButton.setText("Save Changes");
            fullNameField.setText(existing.getFullName());
            studentNumberField.setText(existing.getStudentNumber());
            departmentField.setText(existing.getDepartment());
            semesterSpinner.getValueFactory().setValue(existing.getSemester());
            statusCombo.setValue(existing.getStatus());
            phoneField.setText(existing.getPhone());
            emailField.setText(existing.getEmail());
            photoPathField.setText(existing.getPhotoPath());
            loadPhotoPreview(existing.getPhotoPath());
        }
    }

    @FXML
    private void handleBrowsePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Student Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(browsePhotoButton.getScene().getWindow());
        if (file != null) {
            photoPathField.setText(file.getAbsolutePath());
            loadPhotoPreview(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleSave() {
        hideError();
        try {
            Student student = editingStudent != null ? editingStudent : new Student();
            student.setFullName(trimOrEmpty(fullNameField.getText()));
            student.setStudentNumber(trimOrEmpty(studentNumberField.getText()));
            student.setDepartment(trimOrEmpty(departmentField.getText()));
            student.setSemester(semesterSpinner.getValue());
            student.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : Student.Status.ACTIVE);
            student.setPhone(emptyToNull(phoneField.getText()));
            student.setEmail(emptyToNull(emailField.getText()));
            student.setPhotoPath(emptyToNull(photoPathField.getText()));

            if (editingStudent != null) {
                studentService.update(student);
            } else {
                studentService.register(student);
            }

            onSaved.run();
            closeDialog();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void loadPhotoPreview(String path) {
        if (path != null && !path.isBlank() && new File(path).exists()) {
            photoPreview.setImage(new Image(new File(path).toURI().toString(), 72, 72, true, true));
        } else {
            loadDefaultPhotoPreview();
        }
    }

    private void loadDefaultPhotoPreview() {
        photoPreview.setImage(new Image(getClass().getResourceAsStream("/images/default-avatar.png")));
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private void showError(String message) {
        formErrorLabel.setText(message);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }

    private void hideError() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);
    }

    private void closeDialog() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
