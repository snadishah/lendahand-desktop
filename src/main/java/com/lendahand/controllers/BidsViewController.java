package com.lendahand.controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.lendahand.dao.BidDAO;
import com.lendahand.dao.ReviewDAO;
import com.lendahand.dao.TaskDAO;
import com.lendahand.models.Bid;
import com.lendahand.models.Task;
import com.lendahand.services.EscrowManager;
import com.lendahand.utils.SceneManager;
import com.lendahand.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class BidsViewController implements Initializable {

    @FXML private Label       titleLabel;
    @FXML private Label       subtitleLabel;
    @FXML private VBox        contentBox;
    @FXML private Label       feedbackLabel;
    @FXML private ScrollPane  scrollPane;

    private boolean isHelper;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        isHelper = SessionManager.getInstance().isHelper();
        feedbackLabel.setVisible(false);

        if (isHelper) {
            titleLabel.setText("My Bids");
            subtitleLabel.setText("Tasks you have offered to help with.");
            loadHelperBids();
        } else {
            titleLabel.setText("Manage Bids");
            subtitleLabel.setText("Review offers from helpers and accept the best one.");
            loadPosterBids();
        }
    }

    private void loadHelperBids() {
        contentBox.getChildren().clear();
        int helperId = SessionManager.getInstance().getCurrentUser().getUserID();
        List<Bid> myBids = BidDAO.getBidsForHelper(helperId);

        if (myBids.isEmpty()) {
            contentBox.getChildren().add(new Label("You haven't placed any bids yet."));
            return;
        }

        for (Bid b : myBids) {
            Task t = TaskDAO.getTaskById(b.getTaskID());
            if (t == null) continue;

            VBox card = new VBox(8);
            card.setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:12; -fx-padding:16;" +
                          "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);");

            Label titleLbl = new Label(t.getTitle());
            titleLbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#1E293B; -fx-font-size:15px;");
            
            Label amtLbl = new Label(String.format("Your Bid: %.0f Rs.", b.getProposedAmount()));
            amtLbl.setStyle("-fx-text-fill:#64748B; -fx-font-size:13px;");

            String st = switch (b.getStatus()) {
                case ACCEPTED -> "✅ Accepted — Time to work!";
                case REJECTED -> "❌ Rejected";
                default       -> "⏳ Pending";
            };
            Label statusLbl = new Label(st);
            statusLbl.setStyle(b.getStatus() == Bid.Status.ACCEPTED 
                ? "-fx-text-fill:#22C55E; -fx-font-weight:bold;" 
                : "-fx-text-fill:#F59E0B; -fx-font-weight:bold;");

            card.getChildren().addAll(titleLbl, amtLbl, statusLbl);
            contentBox.getChildren().add(card);
        }
    }

    private void loadPosterBids() {
        contentBox.getChildren().clear();
        int posterId = SessionManager.getInstance().getCurrentUser().getUserID();
        List<Task> myTasks = TaskDAO.getTasksByPoster(posterId);

        boolean hasAnyBids = false;

        for (Task t : myTasks) {
            List<Bid> bids = BidDAO.getBidsForTask(t.getTaskID());
            if (bids.isEmpty()) continue;
            hasAnyBids = true;

            VBox card = new VBox(12);
            card.setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:12; -fx-padding:16;" +
                          "-fx-border-color:#E2E8F0; -fx-border-radius:12; -fx-border-width:1;" +
                          "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.03),8,0,0,2);");

            Label taskTitle = new Label("Task: " + t.getTitle());
            taskTitle.setStyle("-fx-font-weight:bold; -fx-text-fill:#1E293B; -fx-font-size:15px;");
            card.getChildren().add(taskTitle);

            for (Bid b : bids) {
                HBox bidRow = new HBox(12);
                bidRow.setStyle("-fx-background-color:#F8FAFC; -fx-background-radius:8; -fx-padding:10;");

                double rating = ReviewDAO.getAverageRating(b.getHelperID());
                String ratingDisplay = rating > 0 ? String.format(" (⭐ %.1f)", rating) : " (No reviews yet)";
                
                String nameText = b.getHelperName() + ratingDisplay;
                Label nameLbl = new Label(nameText);
                nameLbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#1E293B; -fx-font-size:14px;");
                
                Label amtLbl = new Label(String.format("Proposed: %.0f LHT", b.getProposedAmount()));
                amtLbl.setStyle("-fx-text-fill:#64748B; -fx-font-size:12px;");
                VBox bidInfo = new VBox(2, nameLbl, amtLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                if (t.getStatus() == Task.Status.OPEN && b.getStatus() == Bid.Status.PENDING) {
                    Button acceptBtn = new Button("Accept");
                    acceptBtn.setStyle("-fx-background-color:#22C55E; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6; -fx-cursor:hand;");
                    acceptBtn.setOnAction(e -> {
                        if (!EscrowManager.holdFunds(t.getPosterID(), b.getProposedAmount())) {
                            showFeedback("Insufficient funds! Please add money to your wallet.", false);
                            return;
                        }
                        
                        // FIX: Using your proper TaskDAO method to assign the helper and status!
                        TaskDAO.acceptBid(t.getTaskID(), b.getHelperID(), b.getProposedAmount());
                        BidDAO.acceptBid(b.getBidID(), t.getTaskID());
                        
                        showFeedback("✅ Bid accepted! Rs. " + String.format("%,.0f", b.getProposedAmount()) + " held in escrow.", true);
                        loadPosterBids();
                    });
                    bidRow.getChildren().addAll(bidInfo, spacer, acceptBtn);
                } else {
                    String st = b.getStatus() == Bid.Status.ACCEPTED ? "✅ Accepted" : "❌ Rejected";
                    Label stLbl = new Label(st);
                    stLbl.setStyle("-fx-text-fill:#64748B; -fx-font-size:11px; -fx-font-weight:bold;");
                    bidRow.getChildren().addAll(bidInfo, spacer, stLbl);
                }

                card.getChildren().add(bidRow);
            }
            contentBox.getChildren().add(card);
        }

        if (!hasAnyBids) {
            contentBox.getChildren().add(new Label("No incoming bids yet. Hang tight!"));
        }
    }

    private void showFeedback(String msg, boolean good) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle(good
            ? "-fx-text-fill:#22C55E; -fx-font-size:13px; -fx-background-color:#DCFCE7; -fx-background-radius:10; -fx-padding:8 14;"
            : "-fx-text-fill:#EF4444; -fx-font-size:13px; -fx-background-color:#FFF0F0; -fx-background-radius:10; -fx-padding:8 14;");
        feedbackLabel.setVisible(true);
    }

    @FXML private void handleRefresh() {
        if (isHelper) loadHelperBids();
        else loadPosterBids();
        feedbackLabel.setVisible(false);
    }

    @FXML private void handleBack() {
        SceneManager.switchScene("dashboard.fxml", "Dashboard");
    }
}