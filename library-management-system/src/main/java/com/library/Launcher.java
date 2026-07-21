package com.library;

/**
 * Separate non-Application main class, used only as the fat-jar's
 * Main-Class (see pom.xml shade plugin). Running `java -jar ...jar` with
 * a class that itself extends Application fails with a misleading
 * "JavaFX runtime components are missing" error on some JDK/OS
 * combinations; delegating through a plain class avoids that entirely.
 * `mvn javafx:run` (the recommended way to run this project) doesn't hit
 * this issue and calls Main directly.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
