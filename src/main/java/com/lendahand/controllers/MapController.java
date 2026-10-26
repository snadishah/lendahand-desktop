package com.lendahand.controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import com.lendahand.dao.TaskDAO;
import com.lendahand.models.Task;
import com.lendahand.services.LocationService;
import com.lendahand.utils.SceneManager;
import com.lendahand.utils.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class MapController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private TextField  locationField;
    @FXML private Label      statusLabel;
    @FXML private Button     locateBtn;

    private MapView      mapView;
    private TaskMapLayer taskLayer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Build MapView programmatically (Gluon Maps requires this)
        mapView   = new MapView();
        taskLayer = new TaskMapLayer();

        mapView.addLayer(taskLayer);
        mapView.setZoom(5);
        mapView.flyTo(0, new MapPoint(30.3753, 69.3451), 0.1);

        // Style the map container
        mapView.setStyle("-fx-background-color:#0c0c0e;");

        // Inject into the centre of the BorderPane
        rootPane.setCenter(mapView);

        // When user clicks a task pin → navigate to task details
        taskLayer.setOnTaskClick(task -> Platform.runLater(() -> {
            TaskDetailsController ctrl =
                SceneManager.switchSceneGetController("task-details.fxml", "Task Details");
            if (ctrl != null) ctrl.setTask(task);
        }));

        // Load all tasks
        loadTasksOnMap();
        setStatus("Map loaded. Enter your city and click Find Me.", true);
    }

    @FXML
    private void handleLocateMe() {
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            setStatus("Enter your city or address first.", false);
            return;
        }

        locateBtn.setDisable(true);
        locateBtn.setText("Locating...");
        setStatus("Finding your location...", true);

        javafx.concurrent.Task<double[]> bg = new javafx.concurrent.Task<>() {
            @Override protected double[] call() {
                return LocationService.getCoordinates(location);
            }
        };

        bg.setOnSucceeded(e -> Platform.runLater(() -> {
            locateBtn.setDisable(false);
            locateBtn.setText("Find Me");
            double[] coords = bg.getValue();
            if (coords == null) {
                setStatus("Could not find that location. Try city + country.", false);
                return;
            }
            String name = SessionManager.getInstance().getCurrentUser().getName();
            taskLayer.setUserLocation(coords[0], coords[1], name);
            mapView.flyTo(0, new MapPoint(coords[0], coords[1]), 0.1);
            mapView.setZoom(11);
            setStatus("Showing tasks near " + location, true);
        }));

        bg.setOnFailed(e -> Platform.runLater(() -> {
            locateBtn.setDisable(false);
            locateBtn.setText("Find Me");
            setStatus("Location lookup failed. Try again.", false);
        }));

        new Thread(bg).start();
    }

    @FXML
    private void handleRefreshTasks() {
        loadTasksOnMap();
        setStatus("Tasks refreshed.", true);
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("dashboard.fxml", "Dashboard");
    }

    private void loadTasksOnMap() {
        List<Task> tasks = TaskDAO.getAllTasksForMap();
        taskLayer.loadTasks(tasks);
        setStatus(tasks.size() + " tasks loaded on map.", true);
    }

    private void setStatus(String msg, boolean good) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(good
            ? "-fx-text-fill:#00c896; -fx-font-size:12px;"
            : "-fx-text-fill:#ff3b5c; -fx-font-size:12px;");
        statusLabel.setVisible(true);
    }
}