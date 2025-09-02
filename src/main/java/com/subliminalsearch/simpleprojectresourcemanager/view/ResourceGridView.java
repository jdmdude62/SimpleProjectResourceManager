package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.ResourceType;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ResourceGridView {
    private static final Logger logger = LoggerFactory.getLogger(ResourceGridView.class);
    
    private final SchedulingService schedulingService;
    private final Stage stage;
    private TableView<Resource> tableView;
    private ObservableList<Resource> resources;
    
    public ResourceGridView(SchedulingService schedulingService) {
        this(schedulingService, null);
    }
    
    public ResourceGridView(SchedulingService schedulingService, Window owner) {
        this.schedulingService = schedulingService;
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.setTitle("Resource Grid Editor - Manage All Resources");
        
        // Set owner window if provided
        if (owner != null) {
            this.stage.initOwner(owner);
        }
        
        // Set minimum and preferred size for the stage
        this.stage.setMinWidth(1200);
        this.stage.setMinHeight(700);
        this.stage.setWidth(1400);
        this.stage.setHeight(800);
        
        initialize();
    }
    
    private void initialize() {
        // Load data
        loadResources();
        
        // Create table
        tableView = new TableView<>();
        tableView.setEditable(true);
        tableView.setItems(resources);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Set row height and font size for better visibility
        tableView.setRowFactory(tv -> {
            TableRow<Resource> row = new TableRow<>();
            row.setPrefHeight(40);
            row.setStyle("-fx-font-size: 14px;");
            return row;
        });
        
        // Create columns
        createColumns();
        
        // Layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        
        // Add title label
        Label titleLabel = new Label("Resource Management - Edit Resource Details");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Add instruction label
        Label instructionLabel = new Label("Double-click cells to edit. Changes are saved automatically.");
        instructionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        
        // Add refresh button
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshData());
        
        // Add new resource button
        Button addBtn = new Button("Add New Resource");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> addNewResource());
        
        HBox buttonBar = new HBox(10);
        buttonBar.getChildren().addAll(addBtn, refreshBtn);
        
        root.getChildren().addAll(titleLabel, instructionLabel, buttonBar, tableView);
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
    }
    
    private void loadResources() {
        List<Resource> resourceList = schedulingService.getAllResources();
        resources = FXCollections.observableArrayList(resourceList);
    }
    
    private void createColumns() {
        // Name column (editable)
        TableColumn<Resource, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);
        nameCol.setEditable(true);
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(event -> {
            Resource resource = event.getRowValue();
            resource.setName(event.getNewValue());
            saveResource(resource);
        });
        
        // Email column (editable)
        TableColumn<Resource, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(250);
        emailCol.setEditable(true);
        emailCol.setCellFactory(TextFieldTableCell.forTableColumn());
        emailCol.setOnEditCommit(event -> {
            Resource resource = event.getRowValue();
            resource.setEmail(event.getNewValue());
            saveResource(resource);
        });
        
        // Phone column (editable)
        TableColumn<Resource, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        phoneCol.setPrefWidth(150);
        phoneCol.setEditable(true);
        phoneCol.setCellFactory(TextFieldTableCell.forTableColumn());
        phoneCol.setOnEditCommit(event -> {
            Resource resource = event.getRowValue();
            resource.setPhone(event.getNewValue());
            saveResource(resource);
        });
        
        // Resource Type column (display only for now since ResourceType is complex)
        TableColumn<Resource, String> typeCol = new TableColumn<>("Type");
        typeCol.setPrefWidth(150);
        typeCol.setEditable(false);
        typeCol.setCellValueFactory(cellData -> {
            Resource resource = cellData.getValue();
            ResourceType type = resource.getResourceType();
            if (type != null) {
                return new SimpleStringProperty(type.getName());
            }
            return new SimpleStringProperty("Unknown");
        });
        typeCol.setStyle("-fx-alignment: CENTER-LEFT;");
        
        // Assignment Count column (non-editable)
        TableColumn<Resource, String> assignmentCol = new TableColumn<>("Assignments");
        assignmentCol.setPrefWidth(120);
        assignmentCol.setEditable(false);
        assignmentCol.setCellValueFactory(cellData -> {
            Resource resource = cellData.getValue();
            int count = schedulingService.getAssignmentsByResourceId(resource.getId()).size();
            return new SimpleStringProperty(String.valueOf(count));
        });
        assignmentCol.setStyle("-fx-alignment: CENTER;");
        
        // Delete column with button
        TableColumn<Resource, Void> deleteCol = new TableColumn<>("Actions");
        deleteCol.setPrefWidth(100);
        deleteCol.setEditable(false);
        
        Callback<TableColumn<Resource, Void>, TableCell<Resource, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Resource, Void> call(final TableColumn<Resource, Void> param) {
                final TableCell<Resource, Void> cell = new TableCell<>() {
                    private final Button deleteBtn = new Button("🗑");
                    private final Button editBtn = new Button("✏");
                    
                    {
                        deleteBtn.setStyle(
                            "-fx-background-color: #ff6b6b;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 16px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 5 10 5 10;" +
                            "-fx-background-radius: 3;"
                        );
                        
                        deleteBtn.setOnMouseEntered(e -> 
                            deleteBtn.setStyle(
                                "-fx-background-color: #ff5252;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-background-radius: 3;"
                            )
                        );
                        
                        deleteBtn.setOnMouseExited(e -> 
                            deleteBtn.setStyle(
                                "-fx-background-color: #ff6b6b;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-background-radius: 3;"
                            )
                        );
                        
                        editBtn.setStyle(
                            "-fx-background-color: #2196F3;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 16px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 5 10 5 10;" +
                            "-fx-background-radius: 3;"
                        );
                        
                        editBtn.setOnMouseEntered(e -> 
                            editBtn.setStyle(
                                "-fx-background-color: #1976D2;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-background-radius: 3;"
                            )
                        );
                        
                        editBtn.setOnMouseExited(e -> 
                            editBtn.setStyle(
                                "-fx-background-color: #2196F3;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-background-radius: 3;"
                            )
                        );
                        
                        deleteBtn.setOnAction(event -> {
                            Resource resource = getTableView().getItems().get(getIndex());
                            deleteResource(resource);
                        });
                        
                        editBtn.setOnAction(event -> {
                            Resource resource = getTableView().getItems().get(getIndex());
                            editResourceDetails(resource);
                        });
                    }
                    
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox hbox = new HBox(5);
                            hbox.setAlignment(javafx.geometry.Pos.CENTER);
                            hbox.getChildren().addAll(editBtn, deleteBtn);
                            setGraphic(hbox);
                        }
                    }
                };
                return cell;
            }
        };
        
        deleteCol.setCellFactory(cellFactory);
        
        // Add all columns to table
        tableView.getColumns().addAll(
            nameCol,
            emailCol,
            phoneCol,
            typeCol,
            assignmentCol,
            deleteCol
        );
    }
    
    private void saveResource(Resource resource) {
        try {
            schedulingService.updateResource(resource);
            logger.info("Updated resource: {}", resource.getName());
        } catch (Exception e) {
            logger.error("Error saving resource", e);
            showAlert("Save Error", "Failed to save resource: " + e.getMessage());
            refreshData();
        }
    }
    
    private void deleteResource(Resource resource) {
        // Check if resource has assignments
        List<com.subliminalsearch.simpleprojectresourcemanager.model.Assignment> assignments = 
            schedulingService.getAssignmentsByResourceId(resource.getId());
        
        String message;
        if (!assignments.isEmpty()) {
            message = String.format(
                "This resource has %d assignment(s).\n\n" +
                "Resource: %s\n" +
                "Email: %s\n\n" +
                "You cannot delete a resource with active assignments.\n" +
                "Please reassign or delete the assignments first.",
                assignments.size(),
                resource.getName(),
                resource.getEmail() != null ? resource.getEmail() : "No email"
            );
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Delete Resource");
            alert.setHeaderText("Resource has active assignments");
            alert.setContentText(message);
            alert.initOwner(stage);
            alert.showAndWait();
            return;
        }
        
        message = String.format(
            "Are you sure you want to delete this resource?\n\n" +
            "Resource: %s\n" +
            "Email: %s\n" +
            "Type: %s",
            resource.getName(),
            resource.getEmail() != null ? resource.getEmail() : "No email",
            resource.getResourceType()
        );
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Resource");
        confirm.setHeaderText("Delete resource: " + resource.getName());
        confirm.setContentText(message);
        confirm.initOwner(stage);
        confirm.initModality(Modality.WINDOW_MODAL);
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                schedulingService.deleteResource(resource.getId());
                logger.info("Deleted resource: {}", resource.getName());
                
                // Remove from table immediately
                resources.remove(resource);
                tableView.refresh();
                
            } catch (Exception e) {
                logger.error("Error deleting resource", e);
                showAlert("Delete Error", "Failed to delete resource: " + e.getMessage());
            }
        }
    }
    
    private void editResourceDetails(Resource resource) {
        // Open detailed edit dialog for advanced properties
        com.subliminalsearch.simpleprojectresourcemanager.dialog.ResourceDialog dialog = 
            new com.subliminalsearch.simpleprojectresourcemanager.dialog.ResourceDialog(resource);
        dialog.initOwner(stage);
        
        Optional<Resource> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                Resource updated = result.get();
                schedulingService.updateResource(updated);
                refreshData();
            } catch (Exception e) {
                logger.error("Error updating resource", e);
                showAlert("Update Error", "Failed to update resource: " + e.getMessage());
            }
        }
    }
    
    private void addNewResource() {
        com.subliminalsearch.simpleprojectresourcemanager.dialog.ResourceDialog dialog = 
            new com.subliminalsearch.simpleprojectresourcemanager.dialog.ResourceDialog();
        dialog.initOwner(stage);
        
        Optional<Resource> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                Resource newResource = result.get();
                Resource created = schedulingService.createResource(
                    newResource.getName(),
                    newResource.getEmail(),
                    newResource.getResourceType()
                );
                created.setPhone(newResource.getPhone());
                schedulingService.updateResource(created);
                refreshData();
            } catch (Exception e) {
                logger.error("Error creating resource", e);
                showAlert("Create Error", "Failed to create resource: " + e.getMessage());
            }
        }
    }
    
    private void refreshData() {
        loadResources();
        tableView.setItems(resources);
        tableView.refresh();
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(stage);
        alert.showAndWait();
    }
    
    public void show() {
        stage.show();
    }
    
    public void hide() {
        stage.hide();
    }
}