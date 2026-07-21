package com.library;

import com.library.controller.SplashController;
import com.library.util.AlertUtil;
import com.library.util.SceneManager;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Entry point. Splash runs real startup work (DB connect + schema init),
 * then hands off to Login. Run via {@code mvn javafx:run} — see README.
 *
 * <p>Extends Application directly, which is why {@link Launcher} exists
 * separately: a plain {@code java -jar} launch requires a main class that
 * does NOT extend Application, or the JVM refuses to start with
 * "JavaFX runtime components are missing" even though they aren't.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app-icon.png")));
        primaryStage.setResizable(false);
        primaryStage.show();

        SplashController splash = SceneManager.show("/fxml/splash.fxml", "LibraSys", 520, 360);
        primaryStage.centerOnScreen();

        splash.startLoading(() -> {
            primaryStage.setResizable(true);
            try {
                SceneManager.show("/fxml/login.fxml", "LibraSys — Sign In", 900, 560);
                primaryStage.centerOnScreen();
            } catch (Exception e) {
                AlertUtil.error("Startup Error", "Could not open the login screen: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}