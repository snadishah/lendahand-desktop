package com.lendahand.controllers;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import com.lendahand.dao.TaskDAO;
import com.lendahand.models.Task;
import com.lendahand.services.LocationService;
import com.lendahand.utils.DistanceCalculator;
import com.lendahand.utils.SceneManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class TaskFeedController implements Initializable {

    @FXML private ListView<Task> taskListView;
    @FXML private TextField      locationField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Label          statusLabel;
    @FXML private Button         sortBtn;

    private List<Task> allTasks;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categoryFilter.getItems().addAll(
            "All Categories",
            "Heavy Lifting", "Tutoring", "Cleaning", "Need a Ride",
            "Computer Help", "Delivery / Errands", "Pet Care", "Other");
        categoryFilter.getSelectionModel().selectFirst();

        taskListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Task t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setText(null); return; }
                setText(buildRow(t));
            }
        });

        loadTasks();
    }

    private void loadTasks() {
        allTasks = TaskDAO.getOpenTasks();
        applyFilterAndRefresh();
        statusLabel.setText(allTasks.size() + " open tasks found.");
    }

    @FXML
    private void handleSortByDistance() {
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            statusLabel.setText("⚠  Enter your location first.");
            statusLabel.setStyle("-fx-text-fill: #ff4757;");
            return;
        }
        sortBtn.setDisable(true);
        sortBtn.setText("⏳  Locating…");
        statusLabel.setText("Geocoding your location…");

        javafx.concurrent.Task<double[]> bgTask = new javafx.concurrent.Task<>() {
            @Override
            protected double[] call() {
                return LocationService.getCoordinates(location);
            }
        };

        bgTask.setOnSucceeded(e -> Platform.runLater(() -> {
            double[] coords = bgTask.getValue();
            sortBtn.setDisable(false);
            sortBtn.setText("📍  Sort by Distance");

            if (coords == null) {
                statusLabel.setText("❌  Could not geocode location. Try a more specific address.");
                statusLabel.setStyle("-fx-text-fill: #ff4757;");
                return;
            }

            double myLat = coords[0], myLon = coords[1];

            // Attach distance to each task then sort with Collections.sort
            for (Task t : allTasks) {
                if (t.getLatitude() != 0 || t.getLongitude() != 0) {
                    double dist = DistanceCalculator.calculateDistance(
                        myLat, myLon, t.getLatitude(), t.getLongitude());
                    t.setDistanceKm(dist);
                } else {
                    t.setDistanceKm(Double.MAX_VALUE); // no coords → push to bottom
                }
            }

            // Pure Java sort (Haversine-based)
            allTasks.sort(Comparator.comparingDouble(Task::getDistanceKm));

            applyFilterAndRefresh();
            statusLabel.setText("✅  Tasks sorted by distance from \"" + location + "\"");
            statusLabel.setStyle("-fx-text-fill: #00d4aa;");
        }));

        new Thread(bgTask).start();
    }

    @FXML
    private void handleFilterChanged() {
        applyFilterAndRefresh();
    }

    @FXML
    private void handleViewDetails() {
        Task selected = taskListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a task first.");
            return;
        }
        TaskDetailsController ctrl =
            SceneManager.switchSceneGetController("task-details.fxml", "Task Details");
        if (ctrl != null) ctrl.setTask(selected);
    }

    @FXML
    private void handleRefresh() {
        loadTasks();
        statusLabel.setStyle("-fx-text-fill: #8b90a0;");
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("dashboard.fxml", "Dashboard");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void applyFilterAndRefresh() {
        String cat = categoryFilter.getValue();
        List<Task> filtered = "All Categories".equals(cat)
            ? allTasks
            : allTasks.stream()
                .filter(t -> cat.equals(t.getCategory()))
                .toList();

        taskListView.setItems(FXCollections.observableArrayList(filtered));
    }

    private String buildRow(Task t) {
        String dist = t.getDistanceKm() > 0 && t.getDistanceKm() < Double.MAX_VALUE
            ? String.format("  •  %.1f km", t.getDistanceKm())
            : "";
        return String.format("[%s]  %s  —  %.0f Rs.%s",
            t.getCategory(), t.getTitle(), t.getBudget(), dist);
    }
}
