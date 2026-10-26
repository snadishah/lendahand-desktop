package com.lendahand.controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import com.lendahand.dao.TaskDAO;
import com.lendahand.dao.UserDAO;
import com.lendahand.models.Task;
import com.lendahand.models.User;
import com.lendahand.services.LocationService;
import com.lendahand.utils.SceneManager;
import com.lendahand.utils.SessionManager;
import com.lendahand.utils.ThemeManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class DashboardController implements Initializable {

    @FXML private Label  welcomeLabel;
    @FXML private Label  roleLabel;
    @FXML private Label  balanceLabel;
    @FXML private Label  statsLabel;
    @FXML private Button primaryActionBtn;
    @FXML private Button themeToggleBtn;
    @FXML private Button openTasksTab;
    @FXML private Button myTasksTab;
    @FXML private Label          taskCountLabel;
    @FXML private ListView<Task> taskListView;
    @FXML private VBox    mapContainer;
    @FXML private TextField mapLocationField;
    @FXML private Label   mapStatusLabel;

    private User      currentUser;
    private boolean   showingOpen = true;

    // Map — try Gluon first, fall back to WebView SVG map
    private MapView      gluonMap;
    private TaskMapLayer taskLayer;
    private WebEngine    webEngine;
    private boolean      usingGluon = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            SceneManager.switchScene("login.fxml", "Login");
            return;
        }

        // Update theme toggle button label
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️  Light Mode" : "🌙  Dark Mode");

        initMap();
        refreshDashboard();
        handleShowOpenTasks();
    }

    // ── Map init ──────────────────────────────────────────────────────────────

    private void initMap() {
        try {
            gluonMap   = new MapView();
            taskLayer  = new TaskMapLayer();
            gluonMap.addLayer(taskLayer);
            gluonMap.setZoom(5);
            gluonMap.flyTo(0, new MapPoint(30.3753, 69.3451), 0.1);
            gluonMap.setStyle("-fx-background-color:#F7F6F2;");
            VBox.setVgrow(gluonMap, Priority.ALWAYS);
            mapContainer.getChildren().add(1, gluonMap);

            taskLayer.setOnTaskClick(task -> Platform.runLater(() -> {
                TaskDetailsController ctrl =
                    SceneManager.switchSceneGetController("task-details.fxml", "Task Details");
                if (ctrl != null) ctrl.setTask(task);
            }));

            List<Task> tasks = TaskDAO.getAllTasksForMap();
            taskLayer.loadTasks(tasks);
            mapStatusLabel.setText(tasks.size() + " tasks on map. Enter city → Go.");
            usingGluon = true;

        } catch (Exception e) {
            // Gluon not available — fall back to SVG WebView map
            System.out.println("[Map] Gluon unavailable, using WebView map: " + e.getMessage());
            initWebViewMap();
        }
    }

    private void initWebViewMap() {
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        VBox.setVgrow(webView, Priority.ALWAYS);
        mapContainer.getChildren().add(1, webView);

        webEngine.setOnAlert(event -> {
            String data = event.getData();
            if (data != null && data.startsWith("openTask:")) {
                try {
                    int taskId = Integer.parseInt(data.replace("openTask:", "").trim());
                    Platform.runLater(() -> {
                        Task task = TaskDAO.getTaskById(taskId);
                        if (task != null) {
                            TaskDetailsController ctrl =
                                SceneManager.switchSceneGetController("task-details.fxml", "Task Details");
                            if (ctrl != null) ctrl.setTask(task);
                        }
                    });
                } catch (NumberFormatException ignored) {}
            }
        });

        URL mapUrl = getClass().getResource("/com/lendahand/map.html");
        if (mapUrl != null) webEngine.load(mapUrl.toExternalForm());

        webEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                Platform.runLater(this::pushTasksToWebMap);
                mapStatusLabel.setText("Map ready. Enter your city and click Go.");
            }
        });
    }

    private void pushTasksToWebMap() {
        if (webEngine == null) return;
        List<Task> tasks = TaskDAO.getAllTasksForMap();
        JSONArray arr = new JSONArray();
        for (Task t : tasks) {
            if (t.getLatitude() == 0 && t.getLongitude() == 0) continue;
            JSONObject obj = new JSONObject();
            obj.put("id",       t.getTaskID());
            obj.put("title",    t.getTitle());
            obj.put("category", t.getCategory() != null ? t.getCategory() : "Other");
            obj.put("budget",   t.getBudget());
            obj.put("address",  t.getAddress()    != null ? t.getAddress()    : "");
            obj.put("poster",   t.getPosterName() != null ? t.getPosterName() : "");
            obj.put("status",   t.getStatus().name());
            obj.put("lat",      t.getLatitude());
            obj.put("lng",      t.getLongitude());
            obj.put("distKm",   0);
            arr.put(obj);
        }
        String json = arr.toString().replace("\\","\\\\").replace("'","\\'");
        webEngine.executeScript("loadTasks('" + json + "');");
        mapStatusLabel.setText(tasks.size() + " tasks on map.");
    }

    @FXML
    private void handleMapLocate() {
        String loc = mapLocationField.getText().trim();
        if (loc.isEmpty()) return;
        mapStatusLabel.setText("Locating " + loc + "...");

        javafx.concurrent.Task<double[]> bg = new javafx.concurrent.Task<>() {
            @Override protected double[] call() {
                return LocationService.getCoordinates(loc);
            }
        };
        bg.setOnSucceeded(e -> Platform.runLater(() -> {
            double[] c = bg.getValue();
            if (c == null) { mapStatusLabel.setText("Location not found."); return; }
            String name = currentUser.getName();

            if (usingGluon && taskLayer != null) {
                taskLayer.setUserLocation(c[0], c[1], name);
                gluonMap.flyTo(0, new MapPoint(c[0], c[1]), 0.1);
                gluonMap.setZoom(11);
            } else if (webEngine != null) {
                String safe = name.replace("\\","\\\\").replace("'","\\'");
                webEngine.executeScript(
                    String.format("setUserLocation(%f,%f,'%s');", c[0], c[1], safe));
            }
            mapStatusLabel.setText("Showing tasks near " + loc);
        }));
        new Thread(bg).start();
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    @FXML
    public void handleShowOpenTasks() {
        showingOpen = true;
        setActiveTab(openTasksTab, myTasksTab);
        List<Task> tasks = TaskDAO.getOpenTasks();
        taskListView.setItems(FXCollections.observableArrayList(tasks));
        taskCountLabel.setText(tasks.size() + " open tasks available");
        applyCells();
    }

    @FXML
    public void handleShowMyTasks() {
        showingOpen = false;
        setActiveTab(myTasksTab, openTasksTab);
        List<Task> tasks = SessionManager.getInstance().isPoster()
            ? TaskDAO.getTasksByPoster(currentUser.getUserID())
            : TaskDAO.getTasksByHelper(currentUser.getUserID());
        taskListView.setItems(FXCollections.observableArrayList(tasks));
        taskCountLabel.setText("Your tasks (" + tasks.size() + ")");
        applyCells();
    }

    private void setActiveTab(Button on, Button off) {
        on.setStyle("-fx-background-color:linear-gradient(to right,#FF6B6B,#FF8E53);" +
            "-fx-text-fill:#FFFFFF;-fx-font-weight:bold;-fx-font-size:12px;" +
            "-fx-background-radius:10;-fx-padding:9 20;-fx-cursor:hand;");
        off.setStyle("-fx-background-color:transparent;-fx-text-fill:#94A3B8;" +
            "-fx-font-size:12px;-fx-background-radius:10;-fx-padding:9 20;-fx-cursor:hand;");
    }

    private void applyCells() {
        taskListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Task t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setText(null); return; }
                String icon = switch (t.getStatus()) {
                    case OPEN        -> "🟢";
                    case IN_PROGRESS -> "🟡";
                    case DONE        -> "✅";
                    case CANCELLED   -> "❌";
                };
                setText(icon + "  " + t.getTitle()
                    + "  —  Rs. " + String.format("%,.0f", t.getBudget())
                    + (t.getPosterName() != null ? "  |  " + t.getPosterName() : ""));
            }
        });
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    public void refreshDashboard() {
        currentUser = UserDAO.getUserById(currentUser.getUserID());
        if (currentUser == null) return;
        SessionManager.getInstance().setCurrentUser(currentUser);

        welcomeLabel.setText("Welcome, " + currentUser.getName() + "!");
        roleLabel.setText(currentUser.getRole());
        balanceLabel.setText(String.format("Rs. %,.2f", currentUser.getWalletBalance()));

        if (SessionManager.getInstance().isPoster()) {
            primaryActionBtn.setText("Post a New Task");
            statsLabel.setText(
                TaskDAO.getTasksByPoster(currentUser.getUserID()).size() + " task(s) posted.");
        } else {
            primaryActionBtn.setText("Browse Open Tasks");
            statsLabel.setText(
                TaskDAO.getTasksByHelper(currentUser.getUserID()).size() + " task(s) active.");
        }
    }

    @FXML private void handleRefresh() {
        refreshDashboard();
        if (showingOpen) handleShowOpenTasks(); else handleShowMyTasks();
        if (!usingGluon) pushTasksToWebMap();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void handlePrimaryAction() {
        if (SessionManager.getInstance().isPoster())
            SceneManager.switchScene("post-task.fxml", "Post a Task");
        else
            SceneManager.switchScene("task-feed.fxml", "Browse Tasks");
    }

    @FXML private void handleOpenBids() {
        SceneManager.switchScene("bids-view.fxml", "My Bids");
    }

    @FXML private void handleToggleTheme() {
        ThemeManager.toggle();
        // Reload dashboard — SceneManager applies theme class
        SceneManager.switchScene("dashboard.fxml", "Dashboard");
    }

    @FXML private void handleOpenWallet()      { SceneManager.switchScene("wallet.fxml",    "My Wallet");    }
    @FXML private void handleSecondaryAction() { SceneManager.switchScene("chat.fxml",       "AI Assistant"); }
    @FXML private void handleOpenMap()         { SceneManager.switchScene("map.fxml",        "Task Map");     }

    @FXML private void handleViewTask() {
        Task sel = taskListView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TaskDetailsController ctrl =
            SceneManager.switchSceneGetController("task-details.fxml", "Task Details");
        if (ctrl != null) ctrl.setTask(sel);
    }

    @FXML private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.switchScene("login.fxml", "Login");
    }
}