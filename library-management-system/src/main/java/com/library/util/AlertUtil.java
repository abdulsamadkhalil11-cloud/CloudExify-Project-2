package com.library.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Every dialog in the app goes through here so success/warning/error/confirm
 * all share one styled look (see css/common.css .alert-pane rules) instead
 * of the unstyled OS-default Alert box.
 */
public final class AlertUtil {

    private AlertUtil() {
    }

    public static void success(String title, String message) {
        show(AlertType.INFORMATION, title, message, "alert-success");
    }

    public static void warning(String title, String message) {
        show(AlertType.WARNING, title, message, "alert-warning");
    }

    public static void error(String title, String message) {
        show(AlertType.ERROR, title, message, "alert-error");
    }

    /** True if the user picked Yes. */
    public static boolean confirm(String title, String message) {
        Alert alert = build(AlertType.CONFIRMATION, title, message, "alert-confirm");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private static void show(AlertType type, String title, String message, String styleClass) {
        build(type, title, message, styleClass).showAndWait();
    }

    private static Alert build(AlertType type, String title, String message, String styleClass) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(AlertUtil.class.getResource("/css/common.css").toExternalForm());
        pane.getStyleClass().add(styleClass);

        if (SceneManager.getPrimaryStage() != null) {
            Stage dialogStage = (Stage) pane.getScene().getWindow();
            dialogStage.getIcons().setAll(SceneManager.getPrimaryStage().getIcons());
        }
        return alert;
    }
}
