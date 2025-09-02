package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Certification;
import com.subliminalsearch.simpleprojectresourcemanager.repository.CertificationRepository;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CertificationManagementView {
    private static final Logger logger = LoggerFactory.getLogger(CertificationManagementView.class);
    
    private final Stage stage;
    private final CertificationRepository repository;
    private TableView<Certification> tableView;
    private ObservableList<Certification> certifications;
    
    public CertificationManagementView(Window owner) {
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.initOwner(owner);
        this.stage.setTitle("Certification Management");
        
        // Initialize repository
        DatabaseConfig dbConfig = new DatabaseConfig();
        this.repository = new CertificationRepository(dbConfig.getDataSource());
        
        initializeUI();
        loadCertifications();
        
        // Set size
        this.stage.setWidth(1000);
        this.stage.setHeight(600);
        
        // Center on owner if available
        if (owner != null) {
            stage.setX(owner.getX() + (owner.getWidth() - 1000) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 600) / 2);
        }
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Header
        Label titleLabel = new Label("Manage Certifications");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        // Toolbar
        HBox toolbar = new HBox(10);
        Button addBtn = new Button("Add Certification");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        Button refreshBtn = new Button("Refresh");
        
        addBtn.setOnAction(e -> showAddDialog());
        editBtn.setOnAction(e -> showEditDialog());
        deleteBtn.setOnAction(e -> deleteCertification());
        refreshBtn.setOnAction(e -> loadCertifications());
        
        toolbar.getChildren().addAll(addBtn, editBtn, deleteBtn, refreshBtn);
        
        // Table
        tableView = new TableView<>();
        certifications = FXCollections.observableArrayList();
        tableView.setItems(certifications);
        
        // Columns
        TableColumn<Certification, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<Certification, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);
        
        TableColumn<Certification, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(300);
        
        TableColumn<Certification, String> orgCol = new TableColumn<>("Issuing Organization");
        orgCol.setCellValueFactory(new PropertyValueFactory<>("issuingOrganization"));
        orgCol.setPrefWidth(200);
        
        TableColumn<Certification, String> validityCol = new TableColumn<>("Validity (Months)");
        validityCol.setCellValueFactory(cellData -> {
            Integer months = cellData.getValue().getValidityPeriodMonths();
            return new SimpleStringProperty(months != null ? months.toString() : "Permanent");
        });
        validityCol.setPrefWidth(120);
        
        TableColumn<Certification, String> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isActive() ? "Yes" : "No"));
        activeCol.setPrefWidth(70);
        
        tableView.getColumns().addAll(idCol, nameCol, descCol, orgCol, validityCol, activeCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Enable row selection
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // Double-click to edit
        tableView.setRowFactory(tv -> {
            TableRow<Certification> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog();
                }
            });
            return row;
        });
        
        // Layout
        VBox.setVgrow(tableView, Priority.ALWAYS);
        root.getChildren().addAll(titleLabel, toolbar, tableView);
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
    }
    
    private void loadCertifications() {
        try {
            certifications.clear();
            certifications.addAll(repository.findAll());
            logger.info("Loaded {} certifications", certifications.size());
        } catch (Exception e) {
            logger.error("Failed to load certifications", e);
            showError("Failed to load certifications: " + e.getMessage());
        }
    }
    
    private void showAddDialog() {
        Dialog<Certification> dialog = new Dialog<>();
        dialog.setTitle("Add Certification");
        dialog.setHeaderText("Enter certification details");
        dialog.initOwner(stage);
        
        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Certification name");
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(3);
        TextField orgField = new TextField();
        orgField.setPromptText("Issuing organization");
        TextField validityField = new TextField();
        validityField.setPromptText("Validity in months (leave empty for permanent)");
        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(true);
        
        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Issuing Org:"), 0, 2);
        grid.add(orgField, 1, 2);
        grid.add(new Label("Validity (months):"), 0, 3);
        grid.add(validityField, 1, 3);
        grid.add(activeCheck, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Enable/disable save button
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        nameField.textProperty().addListener((obs, old, text) -> 
            saveButton.setDisable(text.trim().isEmpty()));
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Certification cert = new Certification();
                cert.setName(nameField.getText().trim());
                cert.setDescription(descField.getText().trim());
                cert.setIssuingOrganization(orgField.getText().trim());
                
                String validityText = validityField.getText().trim();
                if (!validityText.isEmpty()) {
                    try {
                        cert.setValidityPeriodMonths(Integer.parseInt(validityText));
                    } catch (NumberFormatException e) {
                        showError("Invalid validity period. Please enter a number.");
                        return null;
                    }
                }
                
                cert.setActive(activeCheck.isSelected());
                return cert;
            }
            return null;
        });
        
        Optional<Certification> result = dialog.showAndWait();
        result.ifPresent(cert -> {
            try {
                repository.save(cert);
                loadCertifications();
                showInfo("Certification added successfully.");
            } catch (Exception e) {
                logger.error("Failed to save certification", e);
                showError("Failed to save certification: " + e.getMessage());
            }
        });
    }
    
    private void showEditDialog() {
        Certification selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a certification to edit.");
            return;
        }
        
        Dialog<Certification> dialog = new Dialog<>();
        dialog.setTitle("Edit Certification");
        dialog.setHeaderText("Update certification details");
        dialog.initOwner(stage);
        
        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField(selected.getName());
        TextArea descField = new TextArea(selected.getDescription());
        descField.setPrefRowCount(3);
        TextField orgField = new TextField(selected.getIssuingOrganization());
        TextField validityField = new TextField(
            selected.getValidityPeriodMonths() != null ? selected.getValidityPeriodMonths().toString() : "");
        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(selected.isActive());
        
        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Issuing Org:"), 0, 2);
        grid.add(orgField, 1, 2);
        grid.add(new Label("Validity (months):"), 0, 3);
        grid.add(validityField, 1, 3);
        grid.add(activeCheck, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Enable/disable save button
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        nameField.textProperty().addListener((obs, old, text) -> 
            saveButton.setDisable(text.trim().isEmpty()));
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selected.setName(nameField.getText().trim());
                selected.setDescription(descField.getText().trim());
                selected.setIssuingOrganization(orgField.getText().trim());
                
                String validityText = validityField.getText().trim();
                if (!validityText.isEmpty()) {
                    try {
                        selected.setValidityPeriodMonths(Integer.parseInt(validityText));
                    } catch (NumberFormatException e) {
                        showError("Invalid validity period. Please enter a number.");
                        return null;
                    }
                } else {
                    selected.setValidityPeriodMonths(null);
                }
                
                selected.setActive(activeCheck.isSelected());
                return selected;
            }
            return null;
        });
        
        Optional<Certification> result = dialog.showAndWait();
        result.ifPresent(cert -> {
            try {
                repository.save(cert);
                loadCertifications();
                showInfo("Certification updated successfully.");
            } catch (Exception e) {
                logger.error("Failed to update certification", e);
                showError("Failed to update certification: " + e.getMessage());
            }
        });
    }
    
    private void deleteCertification() {
        Certification selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a certification to delete.");
            return;
        }
        
        Alert confirm = DialogUtils.createScreenAwareAlert(Alert.AlertType.CONFIRMATION, stage);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Certification");
        confirm.setContentText("Are you sure you want to delete '" + selected.getName() + "'?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                repository.delete(selected.getId());
                loadCertifications();
                showInfo("Certification deleted successfully.");
            } catch (Exception e) {
                logger.error("Failed to delete certification", e);
                showError("Failed to delete certification: " + e.getMessage());
            }
        }
    }
    
    private void showInfo(String message) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.INFORMATION, stage);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.WARNING, stage);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.ERROR, stage);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void show() {
        stage.show();
    }
}