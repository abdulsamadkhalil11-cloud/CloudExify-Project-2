package com.library.controller;

import com.library.model.Staff;
import com.library.service.AuthService;
import com.library.service.SettingsService;
import com.library.util.AlertUtil;
import com.library.util.DatabaseConnection;
import com.library.util.SceneManager;
import com.library.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SettingsController {

    @FXML private ToggleButton lightModeToggle;
    @FXML private ToggleButton darkModeToggle;

    @FXML private Label profileNameLabel;
    @FXML private Label profileUsernameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label passwordStatusLabel;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button changePasswordButton;

    @FXML private VBox configCard;
    @FXML private Label configStatusLabel;
    @FXML private TextField libraryNameField;
    @FXML private TextField currencyField;
    @FXML private TextField finePerDayField;
    @FXML private TextField loanPeriodField;
    @FXML private TextField maxRenewalsField;
    @FXML private Button saveConfigButton;

    @FXML private VBox dataCard;
    @FXML private Label dataStatusLabel;
    @FXML private Button backupButton;
    @FXML private Button restoreButton;

    private final SettingsService settingsService = new SettingsService();
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        Staff staff = SessionManager.getCurrentStaff();
        boolean isAdmin = staff != null && staff.isAdmin();

        if (staff != null) {
            profileNameLabel.setText("Name: " + staff.getFullName());
            profileUsernameLabel.setText("Username: " + staff.getUsername());
            profileRoleLabel.setText("Role: " + staff.getRole());
        }

        configCard.setVisible(isAdmin);
        configCard.setManaged(isAdmin);
        dataCard.setVisible(isAdmin);
        dataCard.setManaged(isAdmin);

        if ("dark".equals(settingsService.getTheme())) {
            darkModeToggle.setSelected(true);
        } else {
            lightModeToggle.setSelected(true);
        }

        if (isAdmin) {
            libraryNameField.setText(settingsService.getLibraryName());
            currencyField.setText(settingsService.getCurrencySymbol());
            finePerDayField.setText(String.valueOf(settingsService.getFinePerDay()));
            loanPeriodField.setText(String.valueOf(settingsService.getLoanPeriodDays()));
            maxRenewalsField.setText(String.valueOf(settingsService.getMaxRenewals()));

            boolean sqlite = DatabaseConnection.getInstance().isSqlite();
            backupButton.setDisable(!sqlite);
            restoreButton.setDisable(!sqlite);
        }
    }

    @FXML
    private void handleThemeChange() {
        String theme = darkModeToggle.isSelected() ? "dark" : "light";
        settingsService.setTheme(theme);
        SceneManager.setTheme(theme);
    }

    @FXML
    private void handleChangePassword() {
        hidePasswordStatus();
        Staff staff = SessionManager.getCurrentStaff();
        if (staff == null) return;

        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            showStatus(passwordStatusLabel, "New password and confirmation don't match.", false);
            return;
        }
        try {
            authService.changePassword(staff.getStaffId(), currentPasswordField.getText(), newPasswordField.getText());
            showStatus(passwordStatusLabel, "Password updated.", true);
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } catch (IllegalArgumentException e) {
            showStatus(passwordStatusLabel, e.getMessage(), false);
        }
    }

    @FXML
    private void handleSaveConfig() {
        hideStatus(configStatusLabel);
        try {
            double finePerDay = parseDouble(finePerDayField.getText(), "Fine per day");
            int loanPeriod = parseInt(loanPeriodField.getText(), "Loan period");
            int maxRenewals = parseInt(maxRenewalsField.getText(), "Max renewals");

            if (finePerDay < 0) throw new IllegalArgumentException("Fine per day can't be negative.");
            if (loanPeriod <= 0) throw new IllegalArgumentException("Loan period must be at least 1 day.");
            if (maxRenewals < 0) throw new IllegalArgumentException("Max renewals can't be negative.");

            settingsService.setFinePerDay(finePerDay);
            settingsService.setLoanPeriodDays(loanPeriod);
            settingsService.setMaxRenewals(maxRenewals);
            if (!libraryNameField.getText().isBlank()) {
                settingsService.setLibraryName(libraryNameField.getText().trim());
            }
            if (!currencyField.getText().isBlank()) {
                settingsService.setCurrencySymbol(currencyField.getText().trim());
            }

            showStatus(configStatusLabel, "Configuration saved.", true);
        } catch (IllegalArgumentException e) {
            showStatus(configStatusLabel, e.getMessage(), false);
        }
    }

    @FXML
    private void handleBackup() {
        String sourcePath = DatabaseConnection.getInstance().getSqliteFilePath();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Database Backup");
        chooser.setInitialFileName("library-backup-" + java.time.LocalDate.now() + ".db");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        var file = chooser.showSaveDialog(backupButton.getScene().getWindow());
        if (file == null) return;

        try {
            Files.copy(Path.of(sourcePath), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showStatus(dataStatusLabel, "Backup saved to " + file.getAbsolutePath(), true);
        } catch (IOException e) {
            showStatus(dataStatusLabel, "Backup failed: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleRestore() {
        boolean confirmed = AlertUtil.confirm("Restore Database",
                "This replaces all current data with the chosen backup file. This can't be undone. Continue?");
        if (!confirmed) return;

        String targetPath = DatabaseConnection.getInstance().getSqliteFilePath();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Backup File to Restore");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        var file = chooser.showOpenDialog(restoreButton.getScene().getWindow());
        if (file == null) return;

        try {
            Files.copy(file.toPath(), Path.of(targetPath), StandardCopyOption.REPLACE_EXISTING);
            AlertUtil.warning("Restore Complete", "Database restored. Please restart LibraSys for the changes to take effect.");
        } catch (IOException e) {
            showStatus(dataStatusLabel, "Restore failed: " + e.getMessage(), false);
        }
    }

    private double parseDouble(String text, String fieldName) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private int parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    private void showStatus(Label label, String message, boolean success) {
        label.setText(message);
        label.getStyleClass().setAll(success ? "badge-success" : "login-error-banner");
        label.setStyle(success ? "-fx-padding: 8 12 8 12; -fx-background-radius: 8; -fx-font-weight: bold;" : "");
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideStatus(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private void hidePasswordStatus() {
        hideStatus(passwordStatusLabel);
    }
}
