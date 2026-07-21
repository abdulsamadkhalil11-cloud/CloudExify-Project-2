package com.library.util;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Owns the single primary Stage. Splash, Login and the post-login shell are
 * three Scenes swapped onto the same Stage (not three separate windows), each
 * with a short fade-in — that's the "smooth transitions" requirement, done
 * with a real FadeTransition rather than a canned animation library.
 */
public final class SceneManager {

    private static Stage primaryStage;
    private static String currentTheme = "light";

    private SceneManager() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /** Loads an FXML as a brand new Scene on the primary Stage, fading it in. */
    public static <T> T show(String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(requireResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            applyTheme(scene);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            fadeIn(root);
            return loader.getController();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load " + fxmlPath, e);
        }
    }

    /** Loads an FXML as a standalone node + controller pair, for content dropped into the shell. */
    public static <T> LoadResult<T> loadInto(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(requireResource(fxmlPath));
            Parent node = loader.load();
            return new LoadResult<>(node, loader.getController());
        } catch (IOException e) {
            throw new IllegalStateException("Could not load " + fxmlPath, e);
        }
    }

    public record LoadResult<T>(Parent node, T controller) {
    }

    public static void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(requireResource("/css/common.css").toExternalForm());
        scene.getStylesheets().add(requireResource("/css/" + currentTheme + "-theme.css").toExternalForm());
    }

    /** Switches the light/dark stylesheet on whatever Scene is currently showing. */
    public static void setTheme(String theme) {
        currentTheme = theme;
        if (primaryStage != null && primaryStage.getScene() != null) {
            applyTheme(primaryStage.getScene());
        }
    }

    public static String getTheme() {
        return currentTheme;
    }

    private static void fadeIn(Parent root) {
        FadeTransition fade = new FadeTransition(Duration.millis(280), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private static URL requireResource(String path) {
        return Objects.requireNonNull(SceneManager.class.getResource(path), "Missing resource: " + path);
    }
}
