package com.subliminalsearch.simpleprojectresourcemanager.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.subliminalsearch.simpleprojectresourcemanager.model.User;
import com.subliminalsearch.simpleprojectresourcemanager.service.LDAPService;
import com.subliminalsearch.simpleprojectresourcemanager.service.SessionManager;

import javax.naming.NamingException;

/**
 * Login view with LDAP/Active Directory authentication
 */
public class LoginView {
    
    private final Stage stage;
    private final LDAPService ldapService;
    private final SessionManager sessionManager;
    
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private ProgressIndicator progressIndicator;
    private Button loginButton;
    
    // Callbacks
    private Runnable onSuccessfulLogin;
    
    public LoginView(Stage stage) {
        this.stage = stage;
        this.ldapService = new LDAPService();
        this.sessionManager = SessionManager.getInstance();
        
        setupUI();
    }
    
    private void setupUI() {
        stage.setTitle("Simple Project Resource Manager - Login");
        stage.initStyle(StageStyle.DECORATED);
        
        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(40));
        mainContainer.setStyle("-fx-background-color: #f5f5f5;");
        
        // Company logo/header
        Label titleLabel = new Label("Project Resource Manager");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#2c3e50"));
        
        Label subtitleLabel = new Label("Sign in with your Windows credentials");
        subtitleLabel.setFont(Font.font("Arial", 14));
        subtitleLabel.setTextFill(Color.web("#7f8c8d"));
        
        // Login form
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(15);
        formGrid.setAlignment(Pos.CENTER);
        formGrid.setMaxWidth(400);
        
        // Username field
        Label usernameLabel = new Label("Username:");
        usernameLabel.setFont(Font.font("Arial", 14));
        usernameField = new TextField();
        usernameField.setPromptText("Enter your Windows username");
        usernameField.setPrefWidth(250);
        usernameField.setFont(Font.font("Arial", 14));
        
        // Get current Windows username as default
        String defaultUsername = System.getProperty("user.name");
        if (defaultUsername != null && !defaultUsername.isEmpty()) {
            usernameField.setText(defaultUsername);
        }
        
        // Password field
        Label passwordLabel = new Label("Password:");
        passwordLabel.setFont(Font.font("Arial", 14));
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your Windows password");
        passwordField.setPrefWidth(250);
        passwordField.setFont(Font.font("Arial", 14));
        
        // Add fields to grid
        formGrid.add(usernameLabel, 0, 0);
        formGrid.add(usernameField, 1, 0);
        formGrid.add(passwordLabel, 0, 1);
        formGrid.add(passwordField, 1, 1);
        
        // Remember me checkbox
        CheckBox rememberMeCheckBox = new CheckBox("Remember me");
        rememberMeCheckBox.setFont(Font.font("Arial", 12));
        
        // Error label
        errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Arial", 12));
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(400);
        
        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(30, 30);
        progressIndicator.setVisible(false);
        
        // Login button
        loginButton = new Button("Sign In");
        loginButton.setPrefWidth(120);
        loginButton.setPrefHeight(35);
        loginButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        loginButton.setStyle(
            "-fx-background-color: #3498db; " +
            "-fx-text-fill: white; " +
            "-fx-cursor: hand;"
        );
        
        loginButton.setOnMouseEntered(e -> 
            loginButton.setStyle(
                "-fx-background-color: #2980b9; " +
                "-fx-text-fill: white; " +
                "-fx-cursor: hand;"
            )
        );
        
        loginButton.setOnMouseExited(e -> 
            loginButton.setStyle(
                "-fx-background-color: #3498db; " +
                "-fx-text-fill: white; " +
                "-fx-cursor: hand;"
            )
        );
        
        // Button actions
        loginButton.setOnAction(e -> handleLogin());
        
        // Enter key support
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });
        
        // Info section
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(20, 0, 0, 0));
        
        Label infoLabel = new Label("Access based on Active Directory groups:");
        infoLabel.setFont(Font.font("Arial", 12));
        infoLabel.setTextFill(Color.web("#7f8c8d"));
        
        Label groupsLabel = new Label("• Project Managers - Full project management access\n" +
                                      "• CyberMetal - Technician calendar and assignment view");
        groupsLabel.setFont(Font.font("Arial", 11));
        groupsLabel.setTextFill(Color.web("#95a5a6"));
        groupsLabel.setWrapText(true);
        groupsLabel.setMaxWidth(400);
        
        infoBox.getChildren().addAll(infoLabel, groupsLabel);
        
        // Add all components to main container
        mainContainer.getChildren().addAll(
            titleLabel,
            subtitleLabel,
            new Separator(),
            formGrid,
            rememberMeCheckBox,
            errorLabel,
            progressIndicator,
            loginButton,
            infoBox
        );
        
        // Create scene
        Scene scene = new Scene(mainContainer, 500, 550);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        
        // Focus on password field if username is pre-filled
        Platform.runLater(() -> {
            if (!usernameField.getText().isEmpty()) {
                passwordField.requestFocus();
            } else {
                usernameField.requestFocus();
            }
        });
    }
    
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        // Validation
        if (username.isEmpty()) {
            showError("Please enter your username");
            usernameField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showError("Please enter your password");
            passwordField.requestFocus();
            return;
        }
        
        // Show progress
        showProgress(true);
        errorLabel.setVisible(false);
        
        // Perform authentication in background thread
        new Thread(() -> {
            try {
                // Authenticate with LDAP
                User user = ldapService.authenticate(username, password);
                
                // Store in session
                sessionManager.setCurrentUser(user);
                sessionManager.createSession(user);
                
                // Success - switch to main application
                Platform.runLater(() -> {
                    showProgress(false);
                    
                    // Clear sensitive data
                    passwordField.clear();
                    
                    // Log successful login
                    System.out.println("User logged in: " + user.getFullName() + 
                                      " (" + user.getUsername() + ") - Role: " + user.getRole());
                    
                    // Close login window and open main application
                    if (onSuccessfulLogin != null) {
                        onSuccessfulLogin.run();
                    }
                    
                    stage.close();
                });
                
            } catch (SecurityException e) {
                // Invalid credentials
                Platform.runLater(() -> {
                    showProgress(false);
                    showError("Invalid username or password. Please try again.");
                    passwordField.clear();
                    passwordField.requestFocus();
                });
                
            } catch (NamingException e) {
                // LDAP connection error
                Platform.runLater(() -> {
                    showProgress(false);
                    
                    if (e.getMessage().contains("Connection refused")) {
                        showError("Cannot connect to authentication server. Please contact IT support.");
                    } else if (e.getMessage().contains("timeout")) {
                        showError("Connection timeout. Please check your network connection.");
                    } else {
                        showError("Authentication error: " + e.getMessage());
                    }
                });
                
            } catch (Exception e) {
                // Unexpected error
                Platform.runLater(() -> {
                    showProgress(false);
                    showError("An unexpected error occurred. Please try again or contact support.");
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void showProgress(boolean show) {
        progressIndicator.setVisible(show);
        loginButton.setDisable(show);
        usernameField.setDisable(show);
        passwordField.setDisable(show);
    }
    
    public void show() {
        stage.show();
    }
    
    public void setOnSuccessfulLogin(Runnable callback) {
        this.onSuccessfulLogin = callback;
    }
    
    // For testing with mock authentication
    public void enableTestMode() {
        Button testButton = new Button("Test Login (Dev Only)");
        testButton.setOnAction(e -> {
            // Create test user
            User testUser = new User("testuser");
            testUser.setFullName("Test User");
            testUser.setEmail("test@company.com");
            testUser.setRole(LDAPService.UserRole.PROJECT_MANAGER);
            
            sessionManager.setCurrentUser(testUser);
            sessionManager.createSession(testUser);
            
            if (onSuccessfulLogin != null) {
                onSuccessfulLogin.run();
            }
            stage.close();
        });
        
        VBox content = (VBox) stage.getScene().getRoot();
        content.getChildren().add(testButton);
    }
}