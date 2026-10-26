package com.lendahand.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.lendahand.dao.UserDAO;
import com.lendahand.models.Helper;
import com.lendahand.models.TaskPoster;
import com.lendahand.models.User;
import com.lendahand.utils.SceneManager;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class RegisterController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private RadioButton posterRadio;
    @FXML private RadioButton helperRadio;
    @FXML private VBox registrationFormBox; // The container for the entire form

    @FXML private Label errorLabel;
    @FXML private Label successLabel; // We will add this to the FXML!

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ToggleGroup group = new ToggleGroup();
        posterRadio.setToggleGroup(group);
        helperRadio.setToggleGroup(group);
        
        // Hide success label initially and make it vanish from layout
        if (successLabel != null) {
            successLabel.setVisible(false);
            successLabel.setManaged(false);
        }
        
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleRegister() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        
        // --- 1. Validate All Fields ---
        String name = nameField.getText().trim();
        String email = emailField.getText().trim().toLowerCase();
        String pass = passwordField.getText();
        String confirm = confirmField.getText();
        
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            showFeedback("All fields are required.", false);
            return;
        }

        if (name.length() < 3) {
            showFeedback("Full Name must be at least 3 characters.", false);
            return;
        }

        if (!email.contains("@") && !email.contains(".com")) {
            showFeedback("Please use a valid student email address.", false);
            return;
        }

        if (pass.length() < 6) {
            showFeedback("Password must be at least 6 characters.", false);
            return;
        }

        if (!pass.equals(confirm)) {
            showFeedback("Passwords do not match.", false);
            return;
        }

        // Check if role is selected
        if (!posterRadio.isSelected() && !helperRadio.isSelected()) {
            showFeedback("Please select whether you want to Post Tasks or Help.", false);
            return;
        }

        // --- 2. Create User Model ---
        boolean isPoster = posterRadio.isSelected();
        User newUser;

        // Poster gets Rs. 200 wallet bonus
        if (isPoster) {
            newUser = new TaskPoster(0, name, email, pass, 200.0);
        } else {
            // Helper starts with Rs. 50 bonus
            newUser = new Helper(0, name, email, pass, 50.0);
        }

        // --- 3. Attempt DB Registration ---
        // Final sanity check for unique email before submitting to DB
        if (UserDAO.emailExists(email)) {
            showFeedback("This email is already registered.", false);
            return;
        }

        if (UserDAO.registerUser(newUser)) {
            // --- SUCCESS FLOW WITH NULL-SAFETY ---
            
            if (registrationFormBox != null) {
                registrationFormBox.setVisible(false);
                registrationFormBox.setManaged(false);
            }

            if (errorLabel != null) {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            }

            if (successLabel != null) {
                successLabel.setText("✅ Account created successfully! Preparing your dashboard...");
                successLabel.setVisible(true);
                successLabel.setManaged(true);
            }
            
            // Wait 2.5 seconds, then switch to login smoothly
            PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
            pause.setOnFinished(event -> SceneManager.switchScene("login.fxml", "Login"));
            pause.play();

        } else {
            showFeedback("Failed to create account. Please try again.", false);
        }
    }

    private void showFeedback(String msg, boolean good) {
        if (good) {
            if (successLabel != null) {
                successLabel.setText("✅ " + msg);
                successLabel.setVisible(true);
                successLabel.setManaged(true);
            }
            if (errorLabel != null) {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            }
        } else {
            if (errorLabel != null) {
                errorLabel.setText("⚠️ " + msg);
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
            if (successLabel != null) {
                successLabel.setVisible(false);
                successLabel.setManaged(false);
            }
        }
    }

    @FXML private void handleGoToLogin() {
        SceneManager.switchScene("login.fxml", "Login");
    }
}