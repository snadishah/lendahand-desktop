package com.lendahand.controllers;
import java.net.URL;
import java.util.ResourceBundle;

import com.lendahand.dao.TaskDAO;
import com.lendahand.services.GeminiService;
import com.lendahand.services.LocationService;
import com.lendahand.utils.SceneManager;
import com.lendahand.utils.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class PostTaskController implements Initializable {

    // ── FXML fields ───────────────────────────────────────────────────────────
    @FXML private TextField  titleField;
    @FXML private TextArea   descriptionArea;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField  addressField;
    @FXML private TextField  budgetField;
    @FXML private TextArea   aiEstimateArea;
    @FXML private Button     estimateBtn;
    @FXML private Button     postBtn;
    @FXML private Label      statusLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categoryCombo.getItems().addAll(
            "Heavy Lifting",
            "Tutoring",
            "Cleaning",
            "Need a Ride",
            "Computer Help",
            "Delivery / Errands",
            "Pet Care",
            "Other"
        );
        categoryCombo.getSelectionModel().selectFirst();
        aiEstimateArea.setWrapText(true);
        aiEstimateArea.setEditable(false);
        aiEstimateArea.setPromptText("Fill in the task description then click 'Get AI Estimate'…");
    }

    /** Calls Gemini in a background thread so the UI doesn't freeze. */
    @FXML
    private void handleGetEstimate() {
        String desc = descriptionArea.getText().trim();
        if (desc.isEmpty()) {
            showStatus("Please enter a task description first.", false);
            return;
        }
        estimateBtn.setDisable(true);
        estimateBtn.setText("⏳  Asking AI…");
        aiEstimateArea.setText("Contacting Gemini AI, please wait…");

        javafx.concurrent.Task<String> bgTask = new javafx.concurrent.Task<>() {
            @Override
            protected String call() {
                return GeminiService.getPriceEstimate(desc);
            }
        };

        bgTask.setOnSucceeded(e -> Platform.runLater(() -> {
            aiEstimateArea.setText(bgTask.getValue());
            estimateBtn.setDisable(false);
            estimateBtn.setText("✨  Get AI Estimate");
        }));
        bgTask.setOnFailed(e -> Platform.runLater(() -> {
            aiEstimateArea.setText("Failed to get estimate. Check your internet connection.");
            estimateBtn.setDisable(false);
            estimateBtn.setText("✨  Get AI Estimate");
        }));

        new Thread(bgTask).start();
    }

    @FXML
    private void handlePostTask() {
        String title       = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        String category    = categoryCombo.getValue();
        String address     = addressField.getText().trim();
        String budgetStr   = budgetField.getText().trim();

        if (title.isEmpty() || description.isEmpty() || address.isEmpty() || budgetStr.isEmpty()) {
            showStatus("Please fill in all required fields.", false);
            return;
        }

        double budget;
        try {
            budget = Double.parseDouble(budgetStr);
            if (budget <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showStatus("Budget must be a positive number.", false);
            return;
        }

        double userBalance = SessionManager.getInstance().getCurrentUser().getWalletBalance();
        if (budget > userBalance) {
            showStatus(String.format("Insufficient balance. You have %.2f LHT.", userBalance), false);
            return;
        }

        postBtn.setDisable(true);
        postBtn.setText("⏳  Posting…");
        showStatus("Geocoding your address…", true);

        int posterID = SessionManager.getInstance().getCurrentUser().getUserID();
        double finalBudget = budget;

        // Geocode + DB insert on a background thread
        javafx.concurrent.Task<Boolean> bgTask = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                double lat = 0, lon = 0;
                double[] coords = LocationService.getCoordinates(address);
                if (coords != null) { lat = coords[0]; lon = coords[1]; }

                com.lendahand.models.Task task = new com.lendahand.models.Task(
                    posterID, title, description, category, address, lat, lon, finalBudget);
                return TaskDAO.createTask(task);
            }
        };

        bgTask.setOnSucceeded(e -> Platform.runLater(() -> {
            if (bgTask.getValue()) {
                showStatus("✅  Task posted successfully!", true);
                clearForm();
            } else {
                showStatus("Failed to post task. Please try again.", false);
            }
            postBtn.setDisable(false);
            postBtn.setText("📋  Post Task");
        }));

        new Thread(bgTask).start();
    }

    @FXML
    private void handleBackToDashboard() {
        SceneManager.switchScene("dashboard.fxml", "Dashboard");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showStatus(String msg, boolean good) {
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setStyle(good
            ? "-fx-text-fill: #00d4aa;"
            : "-fx-text-fill: #ff4757;");
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        addressField.clear();
        budgetField.clear();
        aiEstimateArea.clear();
        categoryCombo.getSelectionModel().selectFirst();
    }
}
