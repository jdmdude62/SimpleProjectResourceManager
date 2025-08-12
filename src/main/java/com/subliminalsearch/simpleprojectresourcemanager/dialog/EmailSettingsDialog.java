package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.EmailConfiguration;
import com.subliminalsearch.simpleprojectresourcemanager.service.EmailService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailSettingsDialog extends Dialog<EmailConfiguration> {
    private static final Logger logger = LoggerFactory.getLogger(EmailSettingsDialog.class);
    
    private final TextField smtpServerField;
    private final Spinner<Integer> smtpPortSpinner;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final TextField fromAddressField;
    private final TextField fromNameField;
    private final RadioButton sslRadio;
    private final RadioButton tlsRadio;
    private final RadioButton noneRadio;
    private final Spinner<Integer> timeoutSpinner;
    private final CheckBox enabledCheckBox;
    private final CheckBox ntlmAuthCheckBox;
    private final TextField domainField;
    private final Button testButton;
    private final Label statusLabel;
    
    private EmailConfiguration configuration;
    
    public EmailSettingsDialog() {
        setTitle("Email Settings");
        setHeaderText("Configure SMTP settings for sending reports");
        
        // Load existing configuration
        configuration = EmailConfiguration.load();
        
        // Create form fields
        smtpServerField = new TextField(configuration.getSmtpServer());
        smtpServerField.setPromptText("mail.company.com");
        
        smtpPortSpinner = new Spinner<>(1, 65535, configuration.getSmtpPort());
        smtpPortSpinner.setEditable(true);
        smtpPortSpinner.setPrefWidth(100);
        
        usernameField = new TextField(configuration.getUsername());
        usernameField.setPromptText("username@company.com");
        
        passwordField = new PasswordField();
        passwordField.setText(configuration.getPassword());
        passwordField.setPromptText("Password");
        
        fromAddressField = new TextField(configuration.getFromAddress());
        fromAddressField.setPromptText("noreply@company.com");
        
        fromNameField = new TextField(configuration.getFromName());
        fromNameField.setPromptText("Project Resource Manager");
        
        // Security options
        ToggleGroup securityGroup = new ToggleGroup();
        sslRadio = new RadioButton("SSL (Port 465)");
        sslRadio.setToggleGroup(securityGroup);
        
        tlsRadio = new RadioButton("TLS/STARTTLS (Port 587)");
        tlsRadio.setToggleGroup(securityGroup);
        
        noneRadio = new RadioButton("None (Port 25)");
        noneRadio.setToggleGroup(securityGroup);
        
        // Set initial security selection
        if (configuration.isUseSSL()) {
            sslRadio.setSelected(true);
        } else if (configuration.isUseTLS()) {
            tlsRadio.setSelected(true);
        } else {
            noneRadio.setSelected(true);
        }
        
        // Update port when security changes
        sslRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) smtpPortSpinner.getValueFactory().setValue(465);
        });
        
        tlsRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) smtpPortSpinner.getValueFactory().setValue(587);
        });
        
        noneRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) smtpPortSpinner.getValueFactory().setValue(25);
        });
        
        timeoutSpinner = new Spinner<>(5000, 120000, configuration.getConnectionTimeout(), 5000);
        timeoutSpinner.setEditable(true);
        timeoutSpinner.setPrefWidth(120);
        
        enabledCheckBox = new CheckBox("Enable email notifications");
        enabledCheckBox.setSelected(configuration.isEnabled());
        
        ntlmAuthCheckBox = new CheckBox("Use NTLM Authentication (Exchange)");
        ntlmAuthCheckBox.setSelected(configuration.isUseNTLMAuth());
        
        domainField = new TextField(configuration.getDomain());
        domainField.setPromptText("DOMAIN");
        domainField.setDisable(!configuration.isUseNTLMAuth());
        
        // Enable/disable domain field based on NTLM checkbox
        ntlmAuthCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            domainField.setDisable(!newVal);
        });
        
        testButton = new Button("Test Connection");
        testButton.setOnAction(e -> testConnection());
        
        statusLabel = new Label();
        statusLabel.setWrapText(true);
        
        // Create layout
        GridPane grid = createFormLayout();
        
        // Add note about internal email only
        Label noteLabel = new Label(
            "Note: Currently configured for internal email only. External email addresses " +
            "require additional server configuration."
        );
        noteLabel.setWrapText(true);
        noteLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic; -fx-font-size: 11px;");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(grid, noteLabel, statusLabel);
        
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                updateConfiguration();
                configuration.save();
                return configuration;
            }
            return null;
        });
        
        // Set dialog size
        getDialogPane().setPrefWidth(600);
        getDialogPane().setPrefHeight(650);
    }
    
    private GridPane createFormLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        int row = 0;
        
        // Enable checkbox
        grid.add(enabledCheckBox, 0, row++, 2, 1);
        
        // Separator
        Separator sep1 = new Separator();
        grid.add(sep1, 0, row++, 2, 1);
        
        // Server settings
        Label serverLabel = new Label("Server Settings");
        serverLabel.setStyle("-fx-font-weight: bold;");
        grid.add(serverLabel, 0, row++, 2, 1);
        
        grid.add(new Label("SMTP Server:"), 0, row);
        grid.add(smtpServerField, 1, row);
        GridPane.setHgrow(smtpServerField, Priority.ALWAYS);
        row++;
        
        grid.add(new Label("Port:"), 0, row);
        grid.add(smtpPortSpinner, 1, row);
        row++;
        
        grid.add(new Label("Security:"), 0, row);
        VBox securityBox = new VBox(5);
        securityBox.getChildren().addAll(tlsRadio, sslRadio, noneRadio);
        grid.add(securityBox, 1, row);
        row++;
        
        grid.add(new Label("Timeout (ms):"), 0, row);
        grid.add(timeoutSpinner, 1, row);
        row++;
        
        // Separator
        Separator sep2 = new Separator();
        grid.add(sep2, 0, row++, 2, 1);
        
        // Authentication
        Label authLabel = new Label("Authentication");
        authLabel.setStyle("-fx-font-weight: bold;");
        grid.add(authLabel, 0, row++, 2, 1);
        
        grid.add(new Label("Username:"), 0, row);
        grid.add(usernameField, 1, row);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        row++;
        
        grid.add(new Label("Password:"), 0, row);
        grid.add(passwordField, 1, row);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);
        row++;
        
        grid.add(ntlmAuthCheckBox, 0, row++, 2, 1);
        
        grid.add(new Label("Domain:"), 0, row);
        grid.add(domainField, 1, row);
        GridPane.setHgrow(domainField, Priority.ALWAYS);
        row++;
        
        // Separator
        Separator sep3 = new Separator();
        grid.add(sep3, 0, row++, 2, 1);
        
        // Sender information
        Label senderLabel = new Label("Sender Information");
        senderLabel.setStyle("-fx-font-weight: bold;");
        grid.add(senderLabel, 0, row++, 2, 1);
        
        grid.add(new Label("From Address:"), 0, row);
        grid.add(fromAddressField, 1, row);
        GridPane.setHgrow(fromAddressField, Priority.ALWAYS);
        row++;
        
        grid.add(new Label("From Name:"), 0, row);
        grid.add(fromNameField, 1, row);
        GridPane.setHgrow(fromNameField, Priority.ALWAYS);
        row++;
        
        // Test button
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(testButton);
        grid.add(buttonBox, 0, row++, 2, 1);
        
        return grid;
    }
    
    private void updateConfiguration() {
        configuration.setSmtpServer(smtpServerField.getText().trim());
        configuration.setSmtpPort(smtpPortSpinner.getValue());
        configuration.setUsername(usernameField.getText().trim());
        configuration.setPassword(passwordField.getText());
        configuration.setFromAddress(fromAddressField.getText().trim());
        configuration.setFromName(fromNameField.getText().trim());
        configuration.setUseSSL(sslRadio.isSelected());
        configuration.setUseTLS(tlsRadio.isSelected());
        configuration.setConnectionTimeout(timeoutSpinner.getValue());
        configuration.setEnabled(enabledCheckBox.isSelected());
        configuration.setUseNTLMAuth(ntlmAuthCheckBox.isSelected());
        configuration.setDomain(domainField.getText().trim());
    }
    
    private void testConnection() {
        updateConfiguration();
        
        if (!configuration.isConfigured()) {
            statusLabel.setText("Please fill in all required fields.");
            statusLabel.setStyle("-fx-text-fill: #c62828;");
            return;
        }
        
        // Get test email address
        TextInputDialog dialog = new TextInputDialog(configuration.getUsername());
        dialog.setTitle("Test Email");
        dialog.setHeaderText("Send Test Email");
        dialog.setContentText("Enter recipient email address:");
        
        dialog.showAndWait().ifPresent(email -> {
            statusLabel.setText("Sending test email...");
            statusLabel.setStyle("-fx-text-fill: #1976d2;");
            testButton.setDisable(true);
            
            Task<Boolean> testTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    EmailService emailService = new EmailService(configuration);
                    return emailService.sendTestEmail(email);
                }
            };
            
            testTask.setOnSucceeded(e -> {
                if (testTask.getValue()) {
                    statusLabel.setText("Test email sent successfully!");
                    statusLabel.setStyle("-fx-text-fill: #2e7d32;");
                } else {
                    statusLabel.setText("Failed to send test email. Check settings and try again.");
                    statusLabel.setStyle("-fx-text-fill: #c62828;");
                }
                testButton.setDisable(false);
            });
            
            testTask.setOnFailed(e -> {
                Throwable ex = testTask.getException();
                statusLabel.setText("Error: " + ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: #c62828;");
                logger.error("Failed to send test email", ex);
                testButton.setDisable(false);
            });
            
            new Thread(testTask).start();
        });
    }
}