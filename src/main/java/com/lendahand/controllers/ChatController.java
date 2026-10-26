package com.lendahand.controllers;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.lendahand.services.GeminiService;
import com.lendahand.utils.SceneManager;
import com.lendahand.utils.SessionManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ChatController implements Initializable {

    @FXML private VBox       chatBox;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField  inputField;
    @FXML private Button     sendBtn;

    private final List<Map<String, String>> history = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String name = SessionManager.getInstance().getCurrentUser() != null
            ? SessionManager.getInstance().getCurrentUser().getName() : "there";

        appendBotMessage("👋  Hi " + name + "! I'm your LendAHand assistant.\n"
            + "Ask me anything — how to post a task, bid, use the wallet, and more!");

        chatBox.heightProperty().addListener((obs, old, nv) ->
            chatScrollPane.setVvalue(1.0));

        // Enter key to send
        inputField.setOnAction(e -> handleSend());
    }

    @FXML
    private void handleSend(ActionEvent event) {
        if (event != null && event.getSource() instanceof Button btn) {
            Object ud = btn.getUserData();
            if (ud != null && !ud.toString().isBlank()) {
                inputField.setText(ud.toString());
            }
        }
        handleSend();
    }

    @FXML
    private void handleSend() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        appendUserMessage(msg);
        inputField.clear();
        sendBtn.setDisable(true);

        Map<String, String> userTurn = new HashMap<>();
        userTurn.put("role", "user");
        userTurn.put("text", msg);
        history.add(userTurn);

        javafx.concurrent.Task<String> bgTask = new javafx.concurrent.Task<>() {
            @Override
            protected String call() {
                return GeminiService.chat(msg, history.subList(0, history.size() - 1));
            }
        };

        bgTask.setOnSucceeded(e -> Platform.runLater(() -> {
            String reply = bgTask.getValue();
            appendBotMessage(reply);

            Map<String, String> modelTurn = new HashMap<>();
            modelTurn.put("role", "model");
            modelTurn.put("text", reply);
            history.add(modelTurn);

            sendBtn.setDisable(false);
        }));

        bgTask.setOnFailed(e -> Platform.runLater(() -> {
            appendBotMessage("⚠️  Sorry, I couldn't reach the AI. Check your connection and API key.");
            sendBtn.setDisable(false);
        }));

        new Thread(bgTask).start();
    }

    @FXML
    private void handleClearChat() {
        chatBox.getChildren().clear();
        history.clear();
        appendBotMessage("Chat cleared! How can I help you? 😊");
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("dashboard.fxml", "Dashboard");
    }

    // ── Message bubble builders ───────────────────────────────────────────────

    private void appendUserMessage(String text) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(480);
        bubble.setPadding(new Insets(11, 16, 11, 16));
        bubble.setStyle(
            "-fx-background-color:linear-gradient(to right,#FF6B6B,#FF8E53);" +
            "-fx-text-fill:#FFFFFF;" +
            "-fx-background-radius:18 18 4 18;" +
            "-fx-font-size:13px;" +
            "-fx-effect:dropshadow(gaussian,rgba(255,107,107,0.25),8,0,0,2);");

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(2, 16, 2, 60));
        chatBox.getChildren().add(row);
    }

    private void appendBotMessage(String text) {
        // Bot avatar
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size:20px;");
        avatar.setMinWidth(36);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(460);
        bubble.setPadding(new Insets(11, 16, 11, 16));
        bubble.setStyle(
            "-fx-background-color:#FFFFFF;" +
            "-fx-text-fill:#1E293B;" +
            "-fx-background-radius:18 18 18 4;" +
            "-fx-font-size:13px;" +
            "-fx-border-color:#E8E8E8;" +
            "-fx-border-radius:18 18 18 4;" +
            "-fx-border-width:1.5;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");

        HBox row = new HBox(10, avatar, bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 60, 2, 16));
        chatBox.getChildren().add(row);
    }
}