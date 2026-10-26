package com.lendahand.controllers;

import java.util.List;
import java.util.Optional;

import com.lendahand.dao.BidDAO;
import com.lendahand.dao.ReviewDAO;
import com.lendahand.dao.TaskDAO;
import com.lendahand.models.Bid;
import com.lendahand.models.Review;
import com.lendahand.models.Task;
import com.lendahand.services.EscrowManager;
import com.lendahand.utils.SceneManager;
import com.lendahand.utils.SessionManager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class TaskDetailsController {

    @FXML private Label    titleLabel;
    @FXML private Label    categoryLabel;
    @FXML private Label    addressLabel;
    @FXML private Label    budgetLabel;
    @FXML private Label    statusLabel;
    @FXML private Label    posterLabel;
    @FXML private TextArea descriptionArea;

    @FXML private javafx.scene.layout.VBox helperBidSection;
    @FXML private TextField bidAmountField;
    @FXML private Label     bidStatusLabel;
    @FXML private Button    submitBidBtn;

    @FXML private javafx.scene.layout.VBox posterBidSection;
    @FXML private ListView<Bid> bidsListView;
    @FXML private Button acceptBidBtn;
    @FXML private Button markDoneBtn;
    @FXML private Button cancelTaskBtn;

    @FXML private Label feedbackLabel;

    private Task currentTask;

    public void setTask(Task task) {
        this.currentTask = task;
        populateTaskInfo();
        configureForRole();
        loadBids();
    }

    private void populateTaskInfo() {
        titleLabel.setText(currentTask.getTitle());
        categoryLabel.setText(currentTask.getCategory());
        addressLabel.setText(currentTask.getAddress());

        budgetLabel.setText(String.format("Rs. %,.0f", currentTask.getBudget()));

        statusLabel.setText(currentTask.getStatus().name().replace("_", " "));
        posterLabel.setText(currentTask.getPosterName() != null
            ? "Posted by: " + currentTask.getPosterName() : "");
        descriptionArea.setText(currentTask.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        applyStatusStyle();
    }

    private void configureForRole() {
        boolean isPoster = SessionManager.getInstance().isPoster();
        boolean isOwner  = currentTask.getPosterID()
                           == SessionManager.getInstance().getCurrentUser().getUserID();

        helperBidSection.setVisible(!isPoster && currentTask.getStatus() == Task.Status.OPEN);
        helperBidSection.setManaged(!isPoster && currentTask.getStatus() == Task.Status.OPEN);

        posterBidSection.setVisible(isPoster && isOwner);
        posterBidSection.setManaged(isPoster && isOwner);

        markDoneBtn.setVisible(currentTask.getStatus() == Task.Status.IN_PROGRESS);
        markDoneBtn.setManaged(currentTask.getStatus() == Task.Status.IN_PROGRESS);

        acceptBidBtn.setVisible(currentTask.getStatus() == Task.Status.OPEN);
        acceptBidBtn.setManaged(currentTask.getStatus() == Task.Status.OPEN);

        boolean canCancel = currentTask.getStatus() == Task.Status.OPEN
                         || currentTask.getStatus() == Task.Status.IN_PROGRESS;
        cancelTaskBtn.setVisible(isPoster && isOwner && canCancel);
        cancelTaskBtn.setManaged(isPoster && isOwner && canCancel);

        if (!isPoster) {
            int helperID = SessionManager.getInstance().getCurrentUser().getUserID();
            if (BidDAO.hasHelperBid(currentTask.getTaskID(), helperID)) {
                submitBidBtn.setDisable(true);
                bidStatusLabel.setText("You have already placed a bid on this task.");
                bidStatusLabel.setVisible(true);
            }
        }
    }

    private void loadBids() {
        List<Bid> bids = BidDAO.getBidsForTask(currentTask.getTaskID());
        bidsListView.setItems(FXCollections.observableArrayList(bids));
        bidsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Bid b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setText(null); return; }
                setText(String.format("%s  →  Rs. %,.0f  [%s]",
                    b.getHelperName() != null ? b.getHelperName() : "Helper #" + b.getHelperID(),
                    b.getProposedAmount(),
                    b.getStatus()));
            }
        });
    }

    @FXML
    private void handleSubmitBid() {
        String amtStr = bidAmountField.getText().trim();
        if (amtStr.isEmpty()) { showFeedback("Enter your proposed amount.", false); return; }

        double amount;
        try {
            amount = Double.parseDouble(amtStr.replace(",", ""));
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showFeedback("Enter a valid positive amount.", false);
            return;
        }

        int helperID = SessionManager.getInstance().getCurrentUser().getUserID();
        Bid bid = new Bid(currentTask.getTaskID(), helperID, amount);
        if (BidDAO.createBid(bid)) {
            showFeedback(String.format("✅  Bid of Rs. %,.0f submitted!", amount), true);
            submitBidBtn.setDisable(true);
            bidAmountField.setDisable(true);
        } else {
            showFeedback("Failed to submit bid. Try again.", false);
        }
    }

    @FXML
    private void handleAcceptBid() {
        Bid selected = bidsListView.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Select a bid to accept.", false); return; }
        if (selected.getStatus() != Bid.Status.PENDING) {
            showFeedback("This bid is no longer pending.", false); return;
        }

        double agreed   = selected.getProposedAmount();
        int    posterID = currentTask.getPosterID();

        if (!EscrowManager.holdFunds(posterID, agreed)) {
            showFeedback(String.format(
                "Insufficient wallet balance. You need Rs. %,.0f.", agreed), false);
            return;
        }

        TaskDAO.acceptBid(currentTask.getTaskID(), selected.getHelperID(), agreed);
        BidDAO.acceptBid(selected.getBidID(), currentTask.getTaskID());

        showFeedback(String.format(
            "✅  Bid accepted! Rs. %,.0f held in escrow. Helper: %s",
            agreed, selected.getHelperName()), true);

        currentTask = TaskDAO.getTaskById(currentTask.getTaskID());
        populateTaskInfo();
        configureForRole();
        loadBids();
    }

    @FXML
    private void handleMarkDone() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            String.format("Mark as DONE? Rs. %,.0f will be sent to the Helper.",
                currentTask.getAcceptedAmount()),
            ButtonType.YES, ButtonType.CANCEL);
        confirm.setTitle("Confirm Completion");
        
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                // 1. Release funds from escrow to the helper
                EscrowManager.releaseFunds(currentTask.getTaskID());
                
                // 2. Figure out who the assigned helper was
                int helperId = -1;
                List<Bid> bids = BidDAO.getBidsForTask(currentTask.getTaskID());
                for (Bid b : bids) {
                    if (b.getStatus() == Bid.Status.ACCEPTED) {
                        helperId = b.getHelperID();
                        break;
                    }
                }

                // 3. Pop open a sleek Review Dialog!
                if (helperId != -1) {
                    Dialog<Review> reviewDialog = new Dialog<>();
                    reviewDialog.setTitle("Rate Helper");
                    reviewDialog.setHeaderText("Task Complete! How did the helper do?");

                    ButtonType submitButtonType = new ButtonType("Submit Review", ButtonBar.ButtonData.OK_DONE);
                    reviewDialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(10);
                    grid.setPadding(new Insets(20, 100, 10, 10));

                    ComboBox<Integer> ratingBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));
                    ratingBox.setValue(5); // Default is 5 stars

                    TextArea commentArea = new TextArea();
                    commentArea.setPromptText("Write your review here...");
                    commentArea.setPrefRowCount(3);

                    grid.add(new Label("Rating (1-5 Stars):"), 0, 0);
                    grid.add(ratingBox, 1, 0);
                    grid.add(new Label("Comment:"), 0, 1);
                    grid.add(commentArea, 1, 1);

                    reviewDialog.getDialogPane().setContent(grid);

                    final int finalHelperId = helperId;
                    reviewDialog.setResultConverter(dialogButton -> {
                        if (dialogButton == submitButtonType) {
                            return new Review(currentTask.getTaskID(), currentTask.getPosterID(), finalHelperId, ratingBox.getValue(), commentArea.getText());
                        }
                        return null;
                    });

                    Optional<Review> result = reviewDialog.showAndWait();
                    result.ifPresent(review -> {
                        ReviewDAO.createReview(review);
                    });
                }

                showFeedback(String.format("✅  Task complete! Rs. %,.0f sent to the Helper.", currentTask.getAcceptedAmount()), true);
                currentTask = TaskDAO.getTaskById(currentTask.getTaskID());
                populateTaskInfo();
                configureForRole();
            }
        });
    }

    @FXML
    private void handleCancelTask() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Cancel this task? If a bid was accepted, funds will be refunded to your wallet.",
            ButtonType.YES, ButtonType.CANCEL);
        confirm.setTitle("Cancel Task");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (currentTask.getStatus() == Task.Status.IN_PROGRESS)
                    EscrowManager.refundFunds(currentTask.getTaskID());
                else
                    TaskDAO.cancelTask(currentTask.getTaskID());
                showFeedback("Task cancelled.", true);
                currentTask = TaskDAO.getTaskById(currentTask.getTaskID());
                populateTaskInfo();
                configureForRole();
            }
        });
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("dashboard.fxml", "Dashboard");
    }

    private void showFeedback(String msg, boolean good) {
        feedbackLabel.setText(msg);
        feedbackLabel.setVisible(true);
        feedbackLabel.setStyle(good
            ? "-fx-text-fill:#00c896; -fx-font-size:13px;"
            : "-fx-text-fill:#ff3b5c; -fx-font-size:13px;");
    }

    private void applyStatusStyle() {
        String style = switch (currentTask.getStatus()) {
            case OPEN        -> "-fx-text-fill:#00c896;";
            case IN_PROGRESS -> "-fx-text-fill:#f5a623;";
            case DONE        -> "-fx-text-fill:#a0a0b0;";
            case CANCELLED   -> "-fx-text-fill:#ff3b5c;";
        };
        statusLabel.setStyle(style + "-fx-font-weight:bold; -fx-font-size:13px;");
    }
}