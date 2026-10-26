package com.lendahand.controllers;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ResourceBundle;

import com.lendahand.dao.UserDAO;
import com.lendahand.models.User;
import com.lendahand.utils.DatabaseConnection;
import com.lendahand.utils.SceneManager;
import com.lendahand.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class WalletController implements Initializable {

    @FXML private Label     balanceLabel;
    @FXML private Label     feedbackLabel;
    @FXML private TextField amountField;
    @FXML private Button    depositBtn;
    @FXML private Button    withdrawBtn;

    private User currentUser;
    private boolean isDeposit = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = UserDAO.getUserById(
            SessionManager.getInstance().getCurrentUser().getUserID());
        refreshBalance();
        feedbackLabel.setVisible(false);
    }

    private void refreshBalance() {
        currentUser = UserDAO.getUserById(currentUser.getUserID());
        SessionManager.getInstance().setCurrentUser(currentUser);
        balanceLabel.setText(String.format("Rs. %,.2f", currentUser.getWalletBalance()));
    }

    @FXML private void handleDepositTab() {
        isDeposit = true;
        depositBtn.setVisible(true);  depositBtn.setManaged(true);
        withdrawBtn.setVisible(false); withdrawBtn.setManaged(false);
        amountField.setPromptText("Enter amount to deposit (Rs.)");
        amountField.clear();
        feedbackLabel.setVisible(false);
    }

    @FXML private void handleWithdrawTab() {
        isDeposit = false;
        depositBtn.setVisible(false);  depositBtn.setManaged(false);
        withdrawBtn.setVisible(true);  withdrawBtn.setManaged(true);
        amountField.setPromptText("Enter amount to withdraw (Rs.)");
        amountField.clear();
        feedbackLabel.setVisible(false);
    }

    @FXML private void handleDeposit() {
        double amount = parseAmount();
        if (amount < 0) return;
        if (amount > 1_000_000) { showError("Maximum single deposit is Rs. 10,00,000."); return; }

        double newBal = currentUser.getWalletBalance() + amount;
        UserDAO.updateWallet(currentUser.getUserID(), newBal);
        logTransaction(currentUser.getUserID(), "DEPOSIT", amount,
            String.format("Wallet deposit of Rs. %,.2f", amount));
        refreshBalance();
        amountField.clear();
        showSuccess(String.format("✅  Rs. %,.2f deposited successfully!", amount));
    }

    @FXML private void handleWithdraw() {
        double amount = parseAmount();
        if (amount < 0) return;
        if (amount > currentUser.getWalletBalance()) {
            showError(String.format("Insufficient balance. Available: Rs. %,.2f",
                currentUser.getWalletBalance()));
            return;
        }

        double newBal = currentUser.getWalletBalance() - amount;
        UserDAO.updateWallet(currentUser.getUserID(), newBal);
        logTransaction(currentUser.getUserID(), "WITHDRAWAL", amount,
            String.format("Wallet withdrawal of Rs. %,.2f", amount));
        refreshBalance();
        amountField.clear();
        showSuccess(String.format("✅  Rs. %,.2f withdrawn successfully!", amount));
    }

    @FXML private void handleBack() {
        SceneManager.switchScene("dashboard.fxml", "Dashboard");
    }

    // ── Helpers ───────────────────────────────────────────────────

    private double parseAmount() {
        String text = amountField.getText().trim();
        if (text.isEmpty()) { showError("Please enter an amount."); return -1; }
        try {
            double v = Double.parseDouble(text.replace(",", ""));
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            showError("Please enter a valid positive number.");
            return -1;
        }
    }

    public static void logTransaction(int userID, String type, double amount, String note) {
        String sql = "INSERT INTO wallet_transactions (user_id, type, amount, note) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, note);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[Wallet] Log transaction failed: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle(
            "-fx-text-fill: #ff3b5c; -fx-font-size: 12px;" +
            "-fx-background-color: #ff3b5c18; -fx-background-radius: 8;" +
            "-fx-padding: 8 12 8 12; -fx-border-color: #ff3b5c33;" +
            "-fx-border-radius: 8; -fx-border-width: 1;");
        feedbackLabel.setVisible(true);
    }

    private void showSuccess(String msg) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle(
            "-fx-text-fill: #00c896; -fx-font-size: 12px;" +
            "-fx-background-color: #00c89618; -fx-background-radius: 8;" +
            "-fx-padding: 8 12 8 12; -fx-border-color: #00c89633;" +
            "-fx-border-radius: 8; -fx-border-width: 1;");
        feedbackLabel.setVisible(true);
    }
}