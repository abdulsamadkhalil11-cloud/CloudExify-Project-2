package com.library.controller;

import com.library.util.DatabaseConnection;
import com.library.util.SchemaInitializer;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * Drives the splash screen. The progress bar is bound to a real background
 * Task (schema creation + first-run seeding) rather than a fake timer, so
 * "loading" always reflects actual work — instant on run #2+, briefly
 * visible on a genuinely fresh database.
 */
public class SplashController {

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label statusLabel;

    public void startLoading(Runnable onComplete) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Connecting to database...");
                updateProgress(0.15, 1);
                DatabaseConnection db = DatabaseConnection.getInstance();

                updateMessage("Preparing schema...");
                updateProgress(0.45, 1);
                SchemaInitializer.initialize(db);

                updateMessage("Ready");
                updateProgress(1, 1);
                Thread.sleep(300);
                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> onComplete.run());
        task.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            Throwable ex = task.getException();
            statusLabel.setText("Startup failed: " + (ex != null ? ex.getMessage() : "unknown error"));
        });

        Thread thread = new Thread(task, "startup-loader");
        thread.setDaemon(true);
        thread.start();
    }
}
