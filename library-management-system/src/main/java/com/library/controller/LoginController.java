package com.library.controller;

import com.library.model.Staff;
import com.library.service.AuthService;
import com.library.util.SceneManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberCheckBox;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Hyperlink forgotPasswordLink;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        authService.getRememberedUsername().ifPresent(name -> {
            usernameField.setText(name);
            rememberCheckBox.setSelected(true);
            passwordField.requestFocus();
        });
    }

    @FXML
    private void handleLogin() {
        hideError();
        try {
            Staff staff = authService.login(usernameField.getText(), passwordField.getText(),
                    rememberCheckBox.isSelected());
            openMainShell(staff);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        showForgotPasswordDialog();
    }

    private void openMainShell(Staff staff) {
        SceneManager.show("/fxml/main.fxml", "LibraSys — " + staff.getFullName(), 1280, 800);
        Stage stage = SceneManager.getPrimaryStage();
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.centerOnScreen();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * A compact two-step reset dialog (find account by username -> answer the
     * security question -> set a new password). Built in code rather than a
     * separate FXML file since it's a small, single-purpose utility form.
     */
    private void showForgotPasswordDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(SceneManager.getPrimaryStage());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Reset Password");
        dialog.setResizable(false);

        Label header = new Label("Reset your password");
        header.getStyleClass().add("login-title");
        header.setStyle("-fx-font-size: 17px;");

        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("form-label");
        TextField usernameInput = new TextField();
        usernameInput.getStyleClass().add("text-field");
        Button findButton = new Button("Find account");
        findButton.getStyleClass().add("btn-outline");

        Label questionLabel = new Label();
        questionLabel.setWrapText(true);
        questionLabel.getStyleClass().add("muted-text");
        questionLabel.setVisible(false);
        questionLabel.setManaged(false);

        Label answerLabel = new Label("Answer");
        answerLabel.getStyleClass().add("form-label");
        TextField answerInput = new TextField();
        answerInput.getStyleClass().add("text-field");

        Label newPasswordLabel = new Label("New password (min. 6 characters)");
        newPasswordLabel.getStyleClass().add("form-label");
        PasswordField newPasswordInput = new PasswordField();
        newPasswordInput.getStyleClass().add("text-field");

        VBox answerSection = new VBox(5, answerLabel, answerInput, newPasswordLabel, newPasswordInput);
        answerSection.setVisible(false);
        answerSection.setManaged(false);

        Button resetButton = new Button("Reset password");
        resetButton.getStyleClass().add("btn-primary");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setVisible(false);
        resetButton.setManaged(false);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);

        findButton.setOnAction(e -> {
            statusLabel.setText("");
            try {
                String question = authService.getSecurityQuestion(usernameInput.getText());
                questionLabel.setText("Security question: " + question);
                questionLabel.setVisible(true);
                questionLabel.setManaged(true);
                answerSection.setVisible(true);
                answerSection.setManaged(true);
                resetButton.setVisible(true);
                resetButton.setManaged(true);
            } catch (IllegalArgumentException ex) {
                statusLabel.getStyleClass().setAll("login-error-banner");
                statusLabel.setText(ex.getMessage());
            }
        });

        resetButton.setOnAction(e -> {
            try {
                authService.resetPassword(usernameInput.getText(), answerInput.getText(), newPasswordInput.getText());
                statusLabel.getStyleClass().setAll("muted-text");
                statusLabel.setStyle("-fx-text-fill: #2E7D5B; -fx-font-weight: bold;");
                statusLabel.setText("Password updated — you can sign in now.");
                resetButton.setDisable(true);
                findButton.setDisable(true);
            } catch (IllegalArgumentException ex) {
                statusLabel.getStyleClass().setAll("login-error-banner");
                statusLabel.setStyle("");
                statusLabel.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(10, header, usernameLabel, usernameInput, findButton,
                questionLabel, answerSection, resetButton, statusLabel);
        root.setPadding(new Insets(24));
        root.setPrefWidth(340);
        root.getStyleClass().add("card");

        Scene scene = new Scene(root);
        SceneManager.applyTheme(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
