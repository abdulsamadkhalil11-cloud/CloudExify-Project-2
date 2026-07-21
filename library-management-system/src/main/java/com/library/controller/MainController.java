package com.library.controller;

import com.library.model.Staff;
import com.library.service.AuthService;
import com.library.service.SettingsService;
import com.library.util.AlertUtil;
import com.library.util.SceneManager;
import com.library.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * The post-login shell: sidebar + top bar + a content area that swaps
 * between Dashboard/Books/Students/Issue-Return/Settings without ever
 * reloading the shell itself.
 */
public class MainController {

    @FXML private VBox sidebar;
    @FXML private Button navDashboard;
    @FXML private Button navBooks;
    @FXML private Button navStudents;
    @FXML private Button navIssueReturn;
    @FXML private Button navSettings;
    @FXML private Button navLogout;
    @FXML private Button themeToggleButton;
    @FXML private Label pageHeaderLabel;
    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private StackPane contentArea;

    private final SettingsService settingsService = new SettingsService();
    private final AuthService authService = new AuthService();
    private List<Button> navButtons;

    @FXML
    public void initialize() {
        navButtons = List.of(navDashboard, navBooks, navStudents, navIssueReturn, navSettings);

        Staff staff = SessionManager.getCurrentStaff();
        if (staff != null) {
            userNameLabel.setText(staff.getFullName());
            userRoleLabel.setText(staff.getRole() == Staff.Role.ADMIN ? "Administrator" : "Librarian");
            userInitialsLabel.setText(initialsOf(staff.getFullName()));
        }

        String theme = settingsService.getTheme();
        SceneManager.setTheme(theme);
        updateThemeButtonGlyph(theme);

        showDashboard();

        sidebar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                registerShortcuts(newScene);
            }
        });
    }

    @FXML
    private void showDashboard() {
        setActive(navDashboard);
        pageHeaderLabel.setText("Dashboard");
        loadContent("/fxml/dashboard.fxml");
    }

    @FXML
    private void showBooks() {
        setActive(navBooks);
        pageHeaderLabel.setText("Book Management");
        loadContent("/fxml/books.fxml");
    }

    @FXML
    private void showStudents() {
        setActive(navStudents);
        pageHeaderLabel.setText("Student Management");
        loadContent("/fxml/students.fxml");
    }

    @FXML
    private void showIssueReturn() {
        setActive(navIssueReturn);
        pageHeaderLabel.setText("Issue / Return");
        loadContent("/fxml/issue_return.fxml");
    }

    @FXML
    private void showSettings() {
        setActive(navSettings);
        pageHeaderLabel.setText("Settings");
        loadContent("/fxml/settings.fxml");
    }

    @FXML
    private void handleThemeToggle() {
        String newTheme = "light".equals(settingsService.getTheme()) ? "dark" : "light";
        settingsService.setTheme(newTheme);
        SceneManager.setTheme(newTheme);
        updateThemeButtonGlyph(newTheme);
    }

    @FXML
    private void handleLogout() {
        if (AlertUtil.confirm("Log out", "Are you sure you want to log out?")) {
            authService.logout();
            SceneManager.show("/fxml/login.fxml", "LibraSys — Sign In", 900, 560);
            SceneManager.getPrimaryStage().centerOnScreen();
        }
    }

    private void loadContent(String fxmlPath) {
        SceneManager.LoadResult<Object> result = SceneManager.loadInto(fxmlPath);
        contentArea.getChildren().setAll(result.node());
    }

    private void setActive(Button active) {
        for (Button b : navButtons) {
            b.getStyleClass().remove("sidebar-nav-item-active");
        }
        if (!active.getStyleClass().contains("sidebar-nav-item-active")) {
            active.getStyleClass().add("sidebar-nav-item-active");
        }
    }

    private void updateThemeButtonGlyph(String theme) {
        themeToggleButton.setText("light".equals(theme) ? "\u2600" : "\u263E");
    }

    private String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String first = String.valueOf(parts[0].charAt(0));
        return parts.length > 1 ? first + parts[parts.length - 1].charAt(0) : first;
    }

    private void registerShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN), this::showDashboard);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), this::showBooks);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN), this::showStudents);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN), this::showIssueReturn);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN), this::showSettings);
    }
}
