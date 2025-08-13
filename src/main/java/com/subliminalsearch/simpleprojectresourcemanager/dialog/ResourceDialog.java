package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.ResourceCategory;
import com.subliminalsearch.simpleprojectresourcemanager.model.ResourceType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ResourceDialog extends Dialog<Resource> {
    private static final Logger logger = LoggerFactory.getLogger(ResourceDialog.class);
    
    private final TextField nameField;
    private final TextField emailField;
    private final ComboBox<ResourceCategory> categoryCombo;
    private final TextField typeNameField;
    private final CheckBox activeCheckBox;
    
    private final boolean isEditMode;
    private final Resource existingResource;
    
    public ResourceDialog() {
        this(null);
    }
    
    public ResourceDialog(Resource resource) {
        this.isEditMode = (resource != null);
        this.existingResource = resource;
        
        setTitle(isEditMode ? "Edit Resource" : "Create New Resource");
        setHeaderText(isEditMode ? "Edit resource details" : "Enter resource details");
        
        // Create form fields
        nameField = new TextField();
        nameField.setPromptText("Full name or company name");
        
        emailField = new TextField();
        emailField.setPromptText("email@example.com");
        
        categoryCombo = new ComboBox<>(FXCollections.observableArrayList(ResourceCategory.values()));
        categoryCombo.setValue(ResourceCategory.INTERNAL);
        
        typeNameField = new TextField();
        typeNameField.setPromptText("e.g., Full-Time Employee, Contractor, Vendor");
        
        activeCheckBox = new CheckBox("Active");
        activeCheckBox.setSelected(true);
        
        // Populate fields if editing
        if (isEditMode && existingResource != null) {
            nameField.setText(existingResource.getName());
            emailField.setText(existingResource.getEmail() != null ? existingResource.getEmail() : "");
            
            if (existingResource.getResourceType() != null) {
                categoryCombo.setValue(existingResource.getResourceType().getCategory());
                typeNameField.setText(existingResource.getResourceType().getName());
            }
            
            activeCheckBox.setSelected(existingResource.isActive());
        }
        
        // Set default type name based on category selection
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && typeNameField.getText().isEmpty()) {
                switch (newVal) {
                    case INTERNAL -> typeNameField.setText("Full-Time Employee");
                    case CONTRACTOR -> typeNameField.setText("Contractor");
                    case VENDOR -> typeNameField.setText("Vendor");
                }
            }
        });
        
        // Create form layout
        GridPane grid = createFormLayout();
        
        // Set up dialog pane
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Enable/disable OK button based on validation
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(!isValidInput());
        
        // Add listeners for validation
        nameField.textProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        emailField.textProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        typeNameField.textProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        
        // Set result converter
        setResultConverter(createResultConverter());
        
        // Set dialog size
        getDialogPane().setPrefWidth(500);
        getDialogPane().setPrefHeight(450);
    }
    
    private GridPane createFormLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // Name
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        row++;
        
        // Email
        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row);
        GridPane.setHgrow(emailField, Priority.ALWAYS);
        row++;
        
        // Category
        grid.add(new Label("Category:"), 0, row);
        grid.add(categoryCombo, 1, row);
        row++;
        
        // Type Name
        grid.add(new Label("Type:"), 0, row);
        grid.add(typeNameField, 1, row);
        GridPane.setHgrow(typeNameField, Priority.ALWAYS);
        row++;
        
        // Active checkbox
        grid.add(new Label("Status:"), 0, row);
        grid.add(activeCheckBox, 1, row);
        row++;
        
        return grid;
    }
    
    private boolean isValidInput() {
        // Name required
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            return false;
        }
        
        // Email format validation (if provided)
        String email = emailField.getText();
        if (email != null && !email.trim().isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return false;
            }
        }
        
        // Category required
        if (categoryCombo.getValue() == null) {
            return false;
        }
        
        // Type name required
        if (typeNameField.getText() == null || typeNameField.getText().trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    private Callback<ButtonType, Resource> createResultConverter() {
        return dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    Resource resource;
                    
                    // Create or get resource type
                    ResourceType resourceType = new ResourceType(
                        typeNameField.getText().trim(),
                        categoryCombo.getValue()
                    );
                    
                    if (isEditMode && existingResource != null) {
                        // Update existing resource
                        resource = existingResource;
                        resource.setName(nameField.getText().trim());
                        resource.setEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                        resource.setResourceType(resourceType);
                        resource.setActive(activeCheckBox.isSelected());
                    } else {
                        // Create new resource
                        resource = new Resource(
                            nameField.getText().trim(),
                            emailField.getText().trim().isEmpty() ? null : emailField.getText().trim(),
                            resourceType
                        );
                        resource.setActive(activeCheckBox.isSelected());
                    }
                    
                    return resource;
                    
                } catch (Exception e) {
                    logger.error("Error creating resource from dialog", e);
                    
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to create resource");
                    errorAlert.setContentText("Please check your input and try again.");
                    errorAlert.showAndWait();
                    
                    return null;
                }
            }
            return null;
        };
    }
    
    /**
     * Show a confirmation dialog before deleting a resource
     */
    public static boolean showDeleteConfirmation(Resource resource) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Resource");
        alert.setHeaderText("Delete resource: " + resource.getName());
        alert.setContentText("Are you sure you want to delete this resource?\n\n" +
                            "Resource: " + resource.getName() + "\n" +
                            "Type: " + (resource.getResourceType() != null ? resource.getResourceType().getName() : "Unknown") + "\n\n" +
                            "This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Show resource details in a read-only dialog
     */
    public static void showResourceDetails(Resource resource) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Resource Details");
        dialog.setHeaderText(resource.getName());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        content.getChildren().addAll(
            createDetailRow("Name:", resource.getName()),
            createDetailRow("Email:", resource.getEmail() != null ? resource.getEmail() : "N/A"),
            createDetailRow("Category:", resource.getResourceType() != null ? 
                          resource.getResourceType().getCategory().toString() : "N/A"),
            createDetailRow("Type:", resource.getResourceType() != null ? 
                          resource.getResourceType().getName() : "N/A"),
            createDetailRow("Status:", resource.isActive() ? "Active" : "Inactive"),
            createDetailRow("Created:", resource.getCreatedAt().toString())
        );
        
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(400);
        
        dialog.showAndWait();
    }
    
    private static HBox createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        
        Label labelComponent = new Label(label);
        labelComponent.setStyle("-fx-font-weight: bold;");
        labelComponent.setPrefWidth(100);
        
        Label valueComponent = new Label(value);
        
        row.getChildren().addAll(labelComponent, valueComponent);
        return row;
    }
}