package com.lendahand.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.lendahand.dao.UserDAO;
import com.lendahand.models.User;
import com.lendahand.utils.SceneManager;
import com.lendahand.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements Initializable {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("---- LOGIN SCREEN LOADED SUCCESSFULLY ----");
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        errorLabel.setVisible(false);
        String email = emailField.getText().trim().toLowerCase();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        User user = UserDAO.login(email, password);
        if (user != null) {
            SessionManager.getInstance().setCurrentUser(user);
            SceneManager.switchScene("dashboard.fxml", "Dashboard");
        } else {
            showError("Invalid email or password.");
        }
    }

    @FXML
    private void handleGoToRegister() {
        System.out.println("Button Clicked! Attempting to switch to register.fxml");
        SceneManager.switchScene("register.fxml", "Create Account");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}