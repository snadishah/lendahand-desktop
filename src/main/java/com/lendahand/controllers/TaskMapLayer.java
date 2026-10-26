package com.lendahand.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.lendahand.models.Task;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

/**
 * Custom Gluon MapLayer that places task pins and a user location dot
 * directly onto the map using real JavaFX nodes — no HTML, no WebView.
 */
public class TaskMapLayer extends MapLayer {

    private record PinEntry(MapPoint point, StackPane node, Task task) {}

    private final List<PinEntry>  pins       = new ArrayList<>();
    private StackPane              userPin   = null;
    private MapPoint               userPoint = null;
    private Consumer<Task>         onTaskClick;

    public void setOnTaskClick(Consumer<Task> handler) {
        this.onTaskClick = handler;
    }

    /** Add all tasks as pins to the layer. */
    public void loadTasks(List<Task> tasks) {
        // Remove old pin nodes from the layer
        pins.forEach(p -> getChildren().remove(p.node()));
        pins.clear();

        for (Task t : tasks) {
            if (t.getLatitude() == 0 && t.getLongitude() == 0) continue;

            StackPane pin = buildTaskPin(t);
            MapPoint  mp  = new MapPoint(t.getLatitude(), t.getLongitude());

            pin.setOnMouseClicked(e -> {
                if (onTaskClick != null) onTaskClick.accept(t);
                e.consume();
            });
            pin.setCursor(Cursor.HAND);

            pins.add(new PinEntry(mp, pin, t));
            getChildren().add(pin);
        }
        markDirty();
    }

    /** Place a red pulsing dot at the user's location. */
    public void setUserLocation(double lat, double lng, String name) {
        if (userPin != null) getChildren().remove(userPin);

        Circle outer = new Circle(14, Color.rgb(255, 59, 92, 0.20));
        Circle inner = new Circle(6,  Color.rgb(255, 59, 92, 1.0));
        inner.setStroke(Color.WHITE);
        inner.setStrokeWidth(2);

        userPin   = new StackPane(outer, inner);
        userPoint = new MapPoint(lat, lng);
        getChildren().add(userPin);
        markDirty();
    }

    /**
     * Called by Gluon Maps every time the map is panned or zoomed.
     * We simply recompute the screen position of every pin.
     */
    @Override
    protected void layoutLayer() {
        // Layout task pins
        for (PinEntry entry : pins) {
            Point2D screen = getMapPoint(
                entry.point().getLatitude(),
                entry.point().getLongitude());
            entry.node().setVisible(true);
            entry.node().setTranslateX(screen.getX());
            entry.node().setTranslateY(screen.getY());
        }

        // Layout user pin
        if (userPin != null && userPoint != null) {
            Point2D screen = getMapPoint(
                userPoint.getLatitude(),
                userPoint.getLongitude());
            userPin.setVisible(true);
            userPin.setTranslateX(screen.getX());
            userPin.setTranslateY(screen.getY());
        }
    }

    // ── Pin builder ───────────────────────────────────────────────────────────

    private StackPane buildTaskPin(Task task) {
        Color pinColor = switch (task.getStatus()) {
            case OPEN        -> Color.rgb(0, 200, 150);   // emerald
            case IN_PROGRESS -> Color.rgb(245, 166, 35);  // amber
            default          -> Color.rgb(58, 58, 78);    // grey
        };

        // Teardrop SVG path (centred at 0,0 pointing down)
        SVGPath drop = new SVGPath();
        drop.setContent("M0,-20 C-9,-28 -11,-10 0,4 C11,-10 9,-28 0,-20 Z");
        drop.setFill(pinColor);
        drop.setStroke(Color.rgb(0, 0, 0, 0.35));
        drop.setStrokeWidth(1);

        // White inner dot
        Circle dot = new Circle(3.5, Color.rgb(255, 255, 255, 0.85));
        dot.setTranslateY(-14);

        // Stack them — anchor point is bottom centre (0, 4)
        StackPane pin = new StackPane(drop, dot);
        pin.setPickOnBounds(false);

        // Hover scale effect
        pin.setOnMouseEntered(e -> pin.setScaleX(1.3));
        pin.setOnMouseExited(e  -> pin.setScaleX(1.0));
        pin.setOnMouseEntered(e -> { pin.setScaleX(1.3); pin.setScaleY(1.3); });
        pin.setOnMouseExited(e  -> { pin.setScaleX(1.0); pin.setScaleY(1.0); });

        return pin;
    }
}