package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.TechnicianUnavailability;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ManageUnavailabilityDialog extends Dialog<Void> {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    
    private final SchedulingService schedulingService;
    private final Resource resource;
    private final TableView<TechnicianUnavailability> tableView;
    private final ObservableList<TechnicianUnavailability> unavailabilities;
    
    public ManageUnavailabilityDialog(SchedulingService schedulingService, Resource resource) {
        this.schedulingService = schedulingService;
        this.resource = resource;
        this.unavailabilities = FXCollections.observableArrayList();
        
        setTitle("Manage Unavailability - " + resource.getName());
        setHeaderText("View and manage unavailability periods for " + resource.getName());
        setResizable(true);
        
        // Create table
        tableView = new TableView<>();
        tableView.setItems(unavailabilities);
        tableView.setPrefWidth(800);
        tableView.setPrefHeight(400);
        
        // Create columns
        TableColumn<TechnicianUnavailability, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getType().getDisplayName()));
        typeCol.setPrefWidth(120);
        
        TableColumn<TechnicianUnavailability, String> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStartDate().format(DATE_FORMAT)));
        startCol.setPrefWidth(100);
        
        TableColumn<TechnicianUnavailability, String> endCol = new TableColumn<>("End Date");
        endCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getEndDate().format(DATE_FORMAT)));
        endCol.setPrefWidth(100);
        
        TableColumn<TechnicianUnavailability, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getReason()));
        reasonCol.setPrefWidth(200);
        
        TableColumn<TechnicianUnavailability, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> {
            TechnicianUnavailability u = cellData.getValue();
            String status = u.isApproved() ? "Approved" : "Pending";
            if (u.isApproved() && u.getApprovedBy() != null) {
                status += " by " + u.getApprovedBy();
            }
            return new SimpleStringProperty(status);
        });
        statusCol.setPrefWidth(150);
        
        TableColumn<TechnicianUnavailability, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(130);
        
        Callback<TableColumn<TechnicianUnavailability, Void>, TableCell<TechnicianUnavailability, Void>> cellFactory = 
            new Callback<TableColumn<TechnicianUnavailability, Void>, TableCell<TechnicianUnavailability, Void>>() {
                @Override
                public TableCell<TechnicianUnavailability, Void> call(final TableColumn<TechnicianUnavailability, Void> param) {
                    final TableCell<TechnicianUnavailability, Void> cell = new TableCell<TechnicianUnavailability, Void>() {
                        private final Button editBtn = new Button("Edit");
                        private final Button deleteBtn = new Button("Delete");
                        
                        {
                            editBtn.setOnAction(event -> {
                                TechnicianUnavailability unavailability = getTableView().getItems().get(getIndex());
                                editUnavailability(unavailability);
                            });
                            
                            deleteBtn.setOnAction(event -> {
                                TechnicianUnavailability unavailability = getTableView().getItems().get(getIndex());
                                deleteUnavailability(unavailability);
                            });
                            
                            editBtn.setPrefWidth(55);
                            deleteBtn.setPrefWidth(60);
                            deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
                        }
                        
                        @Override
                        public void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                HBox buttons = new HBox(5);
                                buttons.getChildren().addAll(editBtn, deleteBtn);
                                setGraphic(buttons);
                            }
                        }
                    };
                    return cell;
                }
            };
        
        actionsCol.setCellFactory(cellFactory);
        
        tableView.getColumns().addAll(typeCol, startCol, endCol, reasonCol, statusCol, actionsCol);
        
        // Load data
        loadUnavailabilities();
        
        // Create button bar
        HBox buttonBar = new HBox(10);
        buttonBar.setPadding(new Insets(10));
        
        Button addNewBtn = new Button("Add New Unavailability");
        addNewBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addNewBtn.setOnAction(e -> addNewUnavailability());
        
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadUnavailabilities());
        
        buttonBar.getChildren().addAll(addNewBtn, refreshBtn);
        
        // Create layout
        BorderPane layout = new BorderPane();
        layout.setCenter(tableView);
        layout.setBottom(buttonBar);
        
        // Add info label if no unavailabilities
        if (unavailabilities.isEmpty()) {
            Label noDataLabel = new Label("No unavailability periods found for " + resource.getName());
            noDataLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            VBox centerBox = new VBox(10);
            centerBox.setPadding(new Insets(20));
            centerBox.getChildren().addAll(noDataLabel, tableView);
            layout.setCenter(centerBox);
        }
        
        getDialogPane().setContent(layout);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Set dialog size
        getDialogPane().setPrefWidth(850);
        getDialogPane().setPrefHeight(500);
    }
    
    private void loadUnavailabilities() {
        List<TechnicianUnavailability> list = schedulingService.getResourceUnavailabilities(resource.getId());
        unavailabilities.setAll(list);
    }
    
    private void addNewUnavailability() {
        ResourceUnavailabilityDialog dialog = new ResourceUnavailabilityDialog(
            schedulingService, 
            schedulingService.getAllResources(),
            resource,
            null
        );
        
        Optional<TechnicianUnavailability> result = dialog.showAndWait();
        if (result.isPresent()) {
            TechnicianUnavailability unavailability = result.get();
            schedulingService.createUnavailability(
                unavailability.getResourceId(),
                unavailability.getType(),
                unavailability.getStartDate(),
                unavailability.getEndDate(),
                unavailability.getReason()
            );
            loadUnavailabilities();
            showInfo("Unavailability Added", "The unavailability period has been added successfully.");
        }
    }
    
    private void editUnavailability(TechnicianUnavailability unavailability) {
        ResourceUnavailabilityDialog dialog = new ResourceUnavailabilityDialog(
            schedulingService,
            schedulingService.getAllResources(),
            resource,
            unavailability
        );
        
        Optional<TechnicianUnavailability> result = dialog.showAndWait();
        if (result.isPresent()) {
            TechnicianUnavailability updated = result.get();
            // Update in database - we need to add an update method to the service
            // For now, we'll delete and recreate
            schedulingService.deleteUnavailability(unavailability.getId());
            schedulingService.createUnavailability(
                updated.getResourceId(),
                updated.getType(),
                updated.getStartDate(),
                updated.getEndDate(),
                updated.getReason()
            );
            loadUnavailabilities();
            showInfo("Unavailability Updated", "The unavailability period has been updated successfully.");
        }
    }
    
    private void deleteUnavailability(TechnicianUnavailability unavailability) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Unavailability Period");
        confirm.setContentText(String.format(
            "Are you sure you want to delete this %s period from %s to %s?",
            unavailability.getType().getDisplayName(),
            unavailability.getStartDate().format(DATE_FORMAT),
            unavailability.getEndDate().format(DATE_FORMAT)
        ));
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            schedulingService.deleteUnavailability(unavailability.getId());
            loadUnavailabilities();
            showInfo("Unavailability Deleted", "The unavailability period has been deleted successfully.");
        }
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}