package com.lendahand;

import com.lendahand.utils.DatabaseConnection;
import com.lendahand.utils.SceneManager;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        DatabaseConnection.initializeDatabase();
        SceneManager.setStage(primaryStage);

        // Always maximized — no fixed width/height
        primaryStage.setMaximized(true);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);
        primaryStage.setTitle("LendAHand");

        SceneManager.switchScene("login.fxml", "Login");
    }

    public static void main(String[] args) {
        launch(args);
    }
}