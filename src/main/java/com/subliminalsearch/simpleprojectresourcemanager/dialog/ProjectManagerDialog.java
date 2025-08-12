package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProjectManagerDialog extends Dialog<ProjectManager> {
    private TextField nameField;
    private TextField emailField;
    private TextField phoneField;
    private TextField departmentField;
    private CheckBox activeCheckBox;
    
    private final ProjectManager existingManager;

    // Constructor for creating new project manager
    public ProjectManagerDialog() {
        this(null);
    }

    // Constructor for editing existing project manager
    public ProjectManagerDialog(ProjectManager manager) {
        this.existingManager = manager;
        
        setTitle(manager == null ? "New Project Manager" : "Edit Project Manager");
        setHeaderText(manager == null ? 
            "Enter project manager details" : 
            "Edit project manager: " + manager.getName());
        
        // Set dialog icon
        DialogPane dialogPane = getDialogPane();
        dialogPane.getStylesheets().add(
            getClass().getResource("/css/scheduler.css").toExternalForm()
        );
        
        // Set dialog size to be much larger
        dialogPane.setMinWidth(1200);
        dialogPane.setMinHeight(700);
        dialogPane.setPrefSize(1400, 800);
        dialogPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setResizable(true);
        
        // Force the window size
        setWidth(1400);
        setHeight(800);
        
        // Create form fields
        createFormFields();
        
        // Create buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Enable/Disable save button depending on input
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        
        // Validation function
        Runnable validateForm = () -> {
            boolean isValid = !nameField.getText().trim().isEmpty();
            saveButton.setDisable(!isValid);
        };
        
        // Initial validation for edit mode
        validateForm.run();
        
        // Validation listeners for all fields that might change
        nameField.textProperty().addListener((observable, oldValue, newValue) -> validateForm.run());
        emailField.textProperty().addListener((observable, oldValue, newValue) -> validateForm.run());
        phoneField.textProperty().addListener((observable, oldValue, newValue) -> validateForm.run());
        departmentField.textProperty().addListener((observable, oldValue, newValue) -> validateForm.run());
        activeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> validateForm.run());
        
        // Convert result
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createProjectManagerFromInput();
            }
            return null;
        });
        
        // Focus on name field
        dialogPane.setContent(createContent());
        nameField.requestFocus();
        
        // Ensure proper sizing when dialog is shown
        setOnShowing(event -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            stage.setMinWidth(1400);
            stage.setMinHeight(800);
            stage.setWidth(1400);
            stage.setHeight(800);
            stage.centerOnScreen();
        });
    }
    
    private void createFormFields() {
        nameField = new TextField();
        nameField.setPromptText("Enter manager name");
        
        emailField = new TextField();
        emailField.setPromptText("email@example.com");
        
        phoneField = new TextField();
        phoneField.setPromptText("(555) 123-4567");
        
        departmentField = new TextField();
        departmentField.setPromptText("Engineering, Sales, etc.");
        
        activeCheckBox = new CheckBox("Active");
        activeCheckBox.setSelected(true);
        
        // Pre-fill fields if editing
        if (existingManager != null) {
            nameField.setText(existingManager.getName());
            emailField.setText(existingManager.getEmail() != null ? existingManager.getEmail() : "");
            phoneField.setText(existingManager.getPhone() != null ? existingManager.getPhone() : "");
            departmentField.setText(existingManager.getDepartment() != null ? existingManager.getDepartment() : "");
            activeCheckBox.setSelected(existingManager.isActive());
            
            // Disable name field for "Unassigned" manager
            if ("Unassigned".equals(existingManager.getName())) {
                nameField.setDisable(true);
            }
        }
    }
    
    private GridPane createContent() {
        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(25);
        grid.setPadding(new Insets(50, 50, 50, 50));
        grid.setPrefWidth(1200);
        grid.setMinWidth(1000);
        grid.setPrefHeight(600);
        grid.setMinHeight(500);
        
        // Make all text fields much wider
        nameField.setPrefWidth(800);
        nameField.setMinWidth(600);
        emailField.setPrefWidth(800);
        emailField.setMinWidth(600);
        phoneField.setPrefWidth(800);
        phoneField.setMinWidth(600);
        departmentField.setPrefWidth(800);
        departmentField.setMinWidth(600);
        
        // Make text fields taller
        nameField.setPrefHeight(40);
        emailField.setPrefHeight(40);
        phoneField.setPrefHeight(40);
        departmentField.setPrefHeight(40);
        
        // Create labels with larger font
        Label nameLabel = new Label("Name:*");
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label emailLabel = new Label("Email:");
        emailLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label phoneLabel = new Label("Phone:");
        phoneLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label departmentLabel = new Label("Department:");
        departmentLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label statusLabel = new Label("Status:");
        statusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Style text fields
        String fieldStyle = "-fx-font-size: 16px; -fx-padding: 8px;";
        nameField.setStyle(fieldStyle);
        emailField.setStyle(fieldStyle);
        phoneField.setStyle(fieldStyle);
        departmentField.setStyle(fieldStyle);
        
        // Make checkbox bigger
        activeCheckBox.setStyle("-fx-font-size: 16px;");
        activeCheckBox.setPrefHeight(40);
        
        // Add form fields
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        
        grid.add(emailLabel, 0, 1);
        grid.add(emailField, 1, 1);
        GridPane.setHgrow(emailField, Priority.ALWAYS);
        
        grid.add(phoneLabel, 0, 2);
        grid.add(phoneField, 1, 2);
        GridPane.setHgrow(phoneField, Priority.ALWAYS);
        
        grid.add(departmentLabel, 0, 3);
        grid.add(departmentField, 1, 3);
        GridPane.setHgrow(departmentField, Priority.ALWAYS);
        
        grid.add(statusLabel, 0, 4);
        grid.add(activeCheckBox, 1, 4);
        
        // Add note with more spacing
        Label note = new Label("* Required field");
        note.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-font-style: italic;");
        grid.add(note, 1, 5);
        GridPane.setMargin(note, new Insets(20, 0, 0, 0));
        
        return grid;
    }
    
    private ProjectManager createProjectManagerFromInput() {
        ProjectManager manager = existingManager != null ? existingManager : new ProjectManager();
        
        manager.setName(nameField.getText().trim());
        manager.setEmail(emailField.getText().trim());
        manager.setPhone(phoneField.getText().trim());
        manager.setDepartment(departmentField.getText().trim());
        manager.setActive(activeCheckBox.isSelected());
        
        return manager;
    }
    
    // Static method for delete confirmation
    public static boolean showDeleteConfirmation(ProjectManager manager) {
        if ("Unassigned".equals(manager.getName())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Cannot Delete");
            alert.setHeaderText("Cannot delete system manager");
            alert.setContentText("The 'Unassigned' project manager cannot be deleted as it is required by the system.");
            alert.showAndWait();
            return false;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Project Manager");
        alert.setHeaderText("Delete project manager: " + manager.getName());
        alert.setContentText("Are you sure you want to delete this project manager?\n" +
                            "Projects assigned to this manager will be set to 'Unassigned'.");
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    // Static method to show manager list dialog  
    public static void showManagerListDialog(java.util.function.Supplier<java.util.List<ProjectManager>> managersSupplier,
                                            java.util.function.Consumer<ProjectManager> onEdit,
                                            java.util.function.Consumer<ProjectManager> onDelete,
                                            Runnable onAdd,
                                            Runnable onRefresh) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Project Managers");
        dialog.setHeaderText("Manage Project Managers");
        dialog.setResizable(true);
        
        // Force the dialog window to be much larger
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setMinHeight(900);
        dialogPane.setMinWidth(1200);
        dialogPane.setPrefSize(1400, 1000);
        dialogPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // Also set on the dialog itself
        dialog.setWidth(1400);
        dialog.setHeight(1000);
        
        // Create list view with larger dimensions
        ListView<ProjectManager> listView = new ListView<>();
        
        // Helper method to refresh the list
        Runnable refreshList = () -> {
            java.util.List<ProjectManager> freshManagers = managersSupplier.get();
            listView.getItems().clear();
            listView.getItems().addAll(freshManagers);
        };
        
        // Initial population
        refreshList.run();
        listView.setPrefHeight(800);
        listView.setPrefWidth(1200);
        listView.setMinHeight(700);
        listView.setStyle("-fx-font-size: 16px;");
        
        // Custom cell factory to show more info
        listView.setCellFactory(lv -> new ListCell<ProjectManager>() {
            @Override
            protected void updateItem(ProjectManager item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(5);
                    vbox.setPadding(new Insets(10));
                    Label nameLabel = new Label(item.getName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 20px;");
                    
                    String details = "";
                    if (item.getDepartment() != null && !item.getDepartment().isEmpty()) {
                        details += item.getDepartment();
                    }
                    if (item.getEmail() != null && !item.getEmail().isEmpty()) {
                        details += (details.isEmpty() ? "" : " | ") + item.getEmail();
                    }
                    if (!item.isActive()) {
                        details += (details.isEmpty() ? "" : " | ") + "INACTIVE";
                    }
                    
                    if (!details.isEmpty()) {
                        Label detailsLabel = new Label(details);
                        detailsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
                        vbox.getChildren().addAll(nameLabel, detailsLabel);
                    } else {
                        vbox.getChildren().add(nameLabel);
                    }
                    
                    setPrefHeight(80);
                    setGraphic(vbox);
                }
            }
        });
        
        // Buttons with larger size
        Button addButton = new Button("Add New");
        Button editButton = new Button("Edit");
        Button deleteButton = new Button("Delete");
        
        // Style buttons to be larger
        String buttonStyle = "-fx-font-size: 18px; -fx-padding: 15px 30px;";
        addButton.setStyle(buttonStyle);
        editButton.setStyle(buttonStyle);
        deleteButton.setStyle(buttonStyle);
        
        editButton.setDisable(true);
        deleteButton.setDisable(true);
        
        // Enable/disable buttons based on selection
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection || "Unassigned".equals(newVal.getName()));
        });
        
        // Button actions
        addButton.setOnAction(e -> {
            onAdd.run();
            // After the add dialog closes, refresh the list
            onRefresh.run();
            refreshList.run();
        });
        
        editButton.setOnAction(e -> {
            ProjectManager selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                onEdit.accept(selected);
                // After the edit dialog closes, refresh the list
                onRefresh.run();
                refreshList.run();
            }
        });
        
        deleteButton.setOnAction(e -> {
            ProjectManager selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                onDelete.accept(selected);
                // After delete, refresh the list
                onRefresh.run();
                refreshList.run();
            }
        });
        
        // Layout
        HBox buttonBox = new HBox(30);
        buttonBox.getChildren().addAll(addButton, editButton, deleteButton);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));
        
        // Make buttons same width but much larger
        addButton.setPrefWidth(200);
        addButton.setPrefHeight(60);
        editButton.setPrefWidth(200);
        editButton.setPrefHeight(60);
        deleteButton.setPrefWidth(200);
        deleteButton.setPrefHeight(60);
        
        VBox content = new VBox(30);
        content.setPadding(new Insets(30));
        content.getChildren().addAll(listView, buttonBox);
        VBox.setVgrow(listView, Priority.ALWAYS);
        
        // Set the content and ensure it fills the space
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        
        // Apply inline styles to ensure the dialog is large
        dialog.getDialogPane().setStyle("-fx-pref-width: 1400px; -fx-pref-height: 1000px; -fx-min-width: 1200px; -fx-min-height: 900px;");
        
        // Force proper size before showing
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setMinWidth(1400);
        stage.setMinHeight(1000);
        stage.setWidth(1400);
        stage.setHeight(1000);
        stage.centerOnScreen();
        
        // Show the dialog
        dialog.showAndWait();
    }
}