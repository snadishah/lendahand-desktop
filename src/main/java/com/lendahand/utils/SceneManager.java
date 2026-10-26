package com.lendahand.utils;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage primaryStage;

    public static void setStage(Stage stage) { primaryStage = stage; }
    public static Stage getStage()           { return primaryStage; }

    public static void switchScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/lendahand/" + fxmlFile));
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root);
            } else {
                scene.setRoot(root);
            }

            String css = SceneManager.class.getResource("/com/lendahand/styles.css").toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }

            // Apply the magic dark mode override instantly when the scene loads!
            if (ThemeManager.isDark()) {
                applyDarkThemeRecursively(root);
            }

            if (primaryStage.getScene() == null) {
                primaryStage.setScene(scene);
            }
            primaryStage.setTitle("LendAHand — " + title);
            primaryStage.setMaximized(true);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("[Scene] Failed to load " + fxmlFile + ": " + e.getMessage());
        }
    }

    public static <T> T switchSceneGetController(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/lendahand/" + fxmlFile));
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root);
            } else {
                scene.setRoot(root);
            }

            String css = SceneManager.class.getResource("/com/lendahand/styles.css").toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }

            // Apply the magic dark mode override instantly when the scene loads!
            if (ThemeManager.isDark()) {
                applyDarkThemeRecursively(root);
            }

            if (primaryStage.getScene() == null) {
                primaryStage.setScene(scene);
            }
            primaryStage.setTitle("LendAHand — " + title);
            primaryStage.setMaximized(true);
            primaryStage.show();

            return loader.getController();

        } catch (IOException e) {
            System.err.println("[Scene] Failed to load " + fxmlFile + ": " + e.getMessage());
            return null;
        }
    }

    // --- THE MAGIC DARK MODE OVERRIDE ---
    // This scans the UI at runtime and forces JavaFX to replace the hardcoded FXML colors!
    private static void applyDarkThemeRecursively(Node node) {
        if (node.getStyle() != null) {
            String style = node.getStyle();
            
            // Swap light backgrounds for dark navy
            style = style.replace("-fx-background-color:#FFFFFF", "-fx-background-color:#1E293B");
            style = style.replace("-fx-background-color: #FFFFFF", "-fx-background-color:#1E293B");
            style = style.replace("-fx-background-color:#F7F6F2", "-fx-background-color:#0F172A");
            style = style.replace("-fx-background-color: #F7F6F2", "-fx-background-color:#0F172A");
            
            // Swap dark text for white text
            style = style.replace("-fx-text-fill:#1E293B", "-fx-text-fill:#F8FAFC");
            style = style.replace("-fx-text-fill: #1E293B", "-fx-text-fill:#F8FAFC");
            style = style.replace("-fx-text-fill:#64748B", "-fx-text-fill:#94A3B8");
            style = style.replace("-fx-text-fill: #64748B", "-fx-text-fill:#94A3B8");
            
            // Swap light borders for dark borders
            style = style.replace("-fx-border-color:#E8E8E8", "-fx-border-color:#334155");
            style = style.replace("-fx-border-color:#E2E8F0", "-fx-border-color:#334155");

            node.setStyle(style);
        }

        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                applyDarkThemeRecursively(child);
            }
        }
    }
}