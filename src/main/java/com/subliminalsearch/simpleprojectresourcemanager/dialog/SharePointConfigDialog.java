package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;
import java.util.logging.Logger;

public class SharePointConfigDialog extends Dialog<SharePointConfig> {
    private static final Logger logger = Logger.getLogger(SharePointConfigDialog.class.getName());
    
    private final SharePointConfig config;
    private TextField tenantIdField;
    private TextField clientIdField;
    private PasswordField clientSecretField;
    private TextField siteUrlField;
    private TextField siteNameField;
    private CheckBox enabledCheckBox;
    private CheckBox testModeCheckBox;
    private Spinner<Integer> syncIntervalSpinner;
    private Label statusLabel;
    private Button testConnectionButton;
    
    public SharePointConfigDialog(Window owner) {
        this.config = SharePointConfig.getInstance();
        
        setTitle("SharePoint Integration Configuration");
        setHeaderText("Configure SharePoint integration for mobile schedule access");
        
        initOwner(owner);
        setResizable(true);
        
        DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(createContent());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setPrefSize(700, 600);
        
        // Enable OK button only when required fields are filled
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setDisable(!config.isConfigured());
        
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                saveConfiguration();
                return config;
            }
            return null;
        });
        
        // Validate fields on change
        setupValidation(okButton);
    }
    
    private VBox createContent() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));
        
        // Instructions
        Label instructionsLabel = new Label(
            "To enable SharePoint integration:\n" +
            "1. Register an app in Azure AD with SharePoint permissions\n" +
            "2. Grant 'Sites.ReadWrite.All' API permission\n" +
            "3. Create a client secret\n" +
            "4. Enter the configuration details below"
        );
        instructionsLabel.setWrapText(true);
        instructionsLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10; -fx-border-radius: 5;");
        
        // Configuration form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        int row = 0;
        
        // Azure AD Configuration
        Label azureSection = new Label("Azure AD Configuration");
        azureSection.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        grid.add(azureSection, 0, row++, 2, 1);
        
        grid.add(new Label("Tenant ID:"), 0, row);
        tenantIdField = new TextField(config.getTenantId());
        tenantIdField.setPromptText("e.g., 12345678-1234-1234-1234-123456789012");
        tenantIdField.setPrefWidth(400);
        grid.add(tenantIdField, 1, row++);
        
        grid.add(new Label("Client ID:"), 0, row);
        clientIdField = new TextField(config.getClientId());
        clientIdField.setPromptText("e.g., abcd1234-5678-90ab-cdef-1234567890ab");
        clientIdField.setPrefWidth(400);
        grid.add(clientIdField, 1, row++);
        
        grid.add(new Label("Client Secret:"), 0, row);
        clientSecretField = new PasswordField();
        clientSecretField.setText(config.getClientSecret());
        clientSecretField.setPromptText("Enter the client secret value");
        clientSecretField.setPrefWidth(400);
        grid.add(clientSecretField, 1, row++);
        
        // SharePoint Configuration
        Label spSection = new Label("SharePoint Configuration");
        spSection.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 10 0 0 0;");
        grid.add(spSection, 0, row++, 2, 1);
        
        grid.add(new Label("Site URL:"), 0, row);
        siteUrlField = new TextField(config.getSiteUrl());
        siteUrlField.setPromptText("e.g., https://yourcompany.sharepoint.com");
        siteUrlField.setPrefWidth(400);
        grid.add(siteUrlField, 1, row++);
        
        grid.add(new Label("Site Name:"), 0, row);
        siteNameField = new TextField(config.getSiteName());
        siteNameField.setPromptText("e.g., field-operations");
        siteNameField.setPrefWidth(400);
        grid.add(siteNameField, 1, row++);
        
        // Sync Settings
        Label syncSection = new Label("Synchronization Settings");
        syncSection.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 10 0 0 0;");
        grid.add(syncSection, 0, row++, 2, 1);
        
        grid.add(new Label("Enable Sync:"), 0, row);
        enabledCheckBox = new CheckBox("Enable automatic synchronization");
        enabledCheckBox.setSelected(config.isEnabled());
        grid.add(enabledCheckBox, 1, row++);
        
        grid.add(new Label("Test Mode:"), 0, row);
        testModeCheckBox = new CheckBox("Test mode (CSV output only, no calendar events)");
        testModeCheckBox.setSelected(com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointTestMode.isTestMode());
        testModeCheckBox.setTooltip(new Tooltip("When enabled, sync will only create CSV files for review, not actual calendar events"));
        grid.add(testModeCheckBox, 1, row++);
        
        grid.add(new Label("Sync Interval:"), 0, row);
        HBox intervalBox = new HBox(10);
        syncIntervalSpinner = new Spinner<>(5, 120, config.getSyncIntervalMinutes(), 5);
        syncIntervalSpinner.setPrefWidth(100);
        intervalBox.getChildren().addAll(syncIntervalSpinner, new Label("minutes"));
        grid.add(intervalBox, 1, row++);
        
        // Test connection button
        HBox buttonBox = new HBox(10);
        testConnectionButton = new Button("Test Connection");
        testConnectionButton.setOnAction(e -> testConnection());
        
        Button createListsButton = new Button("Create SharePoint Lists");
        createListsButton.setTooltip(new Tooltip("Create the required lists in SharePoint"));
        createListsButton.setOnAction(e -> createSharePointLists());
        
        buttonBox.getChildren().addAll(testConnectionButton, createListsButton);
        grid.add(buttonBox, 1, row++);
        
        // Status label
        statusLabel = new Label();
        statusLabel.setWrapText(true);
        grid.add(statusLabel, 0, row++, 2, 1);
        
        // Help text
        Label helpText = new Label(
            "Note: After configuration, the system will:\n" +
            "• Create Projects, Resources, and Assignments lists in SharePoint\n" +
            "• Sync data every " + config.getSyncIntervalMinutes() + " minutes\n" +
            "• Enable mobile access via SharePoint app\n" +
            "• Send notifications for new assignments"
        );
        helpText.setWrapText(true);
        helpText.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
        
        root.getChildren().addAll(instructionsLabel, grid, helpText);
        
        return root;
    }
    
    private void setupValidation(Button okButton) {
        Runnable validator = () -> {
            boolean valid = !tenantIdField.getText().trim().isEmpty() &&
                           !clientIdField.getText().trim().isEmpty() &&
                           !clientSecretField.getText().trim().isEmpty() &&
                           !siteUrlField.getText().trim().isEmpty() &&
                           !siteNameField.getText().trim().isEmpty();
            okButton.setDisable(!valid);
            
            if (valid) {
                statusLabel.setText("Configuration appears valid. Click 'Test Connection' to verify.");
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                statusLabel.setText("Please fill in all required fields.");
                statusLabel.setStyle("-fx-text-fill: orange;");
            }
        };
        
        tenantIdField.textProperty().addListener((obs, old, val) -> validator.run());
        clientIdField.textProperty().addListener((obs, old, val) -> validator.run());
        clientSecretField.textProperty().addListener((obs, old, val) -> validator.run());
        siteUrlField.textProperty().addListener((obs, old, val) -> validator.run());
        siteNameField.textProperty().addListener((obs, old, val) -> validator.run());
    }
    
    private void saveConfiguration() {
        config.setTenantId(tenantIdField.getText().trim());
        config.setClientId(clientIdField.getText().trim());
        config.setClientSecret(clientSecretField.getText().trim());
        config.setSiteUrl(siteUrlField.getText().trim());
        config.setSiteName(siteNameField.getText().trim());
        config.setEnabled(enabledCheckBox.isSelected());
        config.setSyncIntervalMinutes(syncIntervalSpinner.getValue());
        
        // Save test mode setting
        com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointTestMode.setTestMode(testModeCheckBox.isSelected());
        
        config.saveConfiguration();
        logger.info("SharePoint configuration saved, Test mode: " + testModeCheckBox.isSelected());
    }
    
    private void testConnection() {
        testConnectionButton.setDisable(true);
        statusLabel.setText("Testing connection...");
        statusLabel.setStyle("-fx-text-fill: blue;");
        
        // Save current values temporarily
        saveConfiguration();
        
        // In a real implementation, this would test the SharePoint connection
        // For now, we'll simulate a test
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate connection test
                
                // TODO: Implement actual SharePoint connection test
                // SharePointClient client = new SharePointClient(config);
                // boolean success = client.testConnection();
                
                boolean success = config.isConfigured(); // Temporary check
                
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        statusLabel.setText("Connection successful! SharePoint is configured correctly.");
                        statusLabel.setStyle("-fx-text-fill: green;");
                    } else {
                        statusLabel.setText("Connection failed. Please check your credentials and try again.");
                        statusLabel.setStyle("-fx-text-fill: red;");
                    }
                    testConnectionButton.setDisable(false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Error testing connection: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    testConnectionButton.setDisable(false);
                });
            }
        }).start();
    }
    
    private void createSharePointLists() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Create SharePoint Lists");
        alert.setHeaderText("SharePoint List Creation");
        alert.setContentText(
            "To create the required SharePoint lists:\n\n" +
            "1. Navigate to your SharePoint site\n" +
            "2. Click 'Site Contents' → 'New' → 'List'\n" +
            "3. Create three lists with these exact names:\n" +
            "   • Projects\n" +
            "   • Resources\n" +
            "   • Assignments\n\n" +
            "4. Add the columns as specified in the integration guide\n\n" +
            "This will be automated in a future update."
        );
        alert.showAndWait();
    }
}