package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.TechnicianUnavailability;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceAvailabilityDialog extends Dialog<Void> {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    
    private final SchedulingService schedulingService;
    private final ComboBox<Resource> resourceCombo;
    private final TableView<TechnicianUnavailability> tableView;
    private final ObservableList<TechnicianUnavailability> unavailabilities;
    private final Label summaryLabel;
    
    public ResourceAvailabilityDialog(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
        this.unavailabilities = FXCollections.observableArrayList();
        
        setTitle("Resource Availability Management");
        setHeaderText("Manage unavailability periods for all resources");
        setResizable(true);
        
        // Create resource selector
        List<Resource> allResources = schedulingService.getAllResources();
        resourceCombo = new ComboBox<>(FXCollections.observableArrayList(allResources));
        resourceCombo.setPromptText("Select a resource...");
        resourceCombo.setPrefWidth(300);
        resourceCombo.setConverter(new javafx.util.StringConverter<Resource>() {
            @Override
            public String toString(Resource resource) {
                return resource != null ? resource.getName() + " (" + resource.getResourceType() + ")" : "";
            }
            
            @Override
            public Resource fromString(String string) {
                return null;
            }
        });
        
        // Create summary label
        summaryLabel = new Label("Select a resource to view their availability");
        summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        // Create table
        tableView = new TableView<>();
        tableView.setItems(unavailabilities);
        tableView.setPrefWidth(900);
        tableView.setPrefHeight(350);
        tableView.setPlaceholder(new Label("Select a resource to view unavailability periods"));
        
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
        
        TableColumn<TechnicianUnavailability, String> durationCol = new TableColumn<>("Days");
        durationCol.setCellValueFactory(cellData -> {
            TechnicianUnavailability u = cellData.getValue();
            long days = u.getStartDate().until(u.getEndDate()).getDays() + 1;
            return new SimpleStringProperty(String.valueOf(days));
        });
        durationCol.setPrefWidth(60);
        
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
        
        tableView.getColumns().addAll(typeCol, startCol, endCol, durationCol, reasonCol, statusCol);
        
        // Action buttons for selected resource
        Button addNewBtn = new Button("Add Unavailability");
        addNewBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addNewBtn.setDisable(true);
        addNewBtn.setOnAction(e -> addNewUnavailability());
        
        Button editBtn = new Button("Edit Selected");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> editSelectedUnavailability());
        
        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> deleteSelectedUnavailability());
        
        Button viewAllBtn = new Button("View All Resources");
        viewAllBtn.setOnAction(e -> viewAllResourcesUnavailability());
        
        // Enable/disable buttons based on selection
        resourceCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadUnavailabilities(newVal);
                addNewBtn.setDisable(false);
                updateSummary(newVal);
            } else {
                unavailabilities.clear();
                addNewBtn.setDisable(true);
                editBtn.setDisable(true);
                deleteBtn.setDisable(true);
                summaryLabel.setText("Select a resource to view their availability");
            }
        });
        
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);
        });
        
        // Create layout
        VBox topSection = new VBox(10);
        topSection.setPadding(new Insets(10));
        
        HBox resourceSelector = new HBox(10);
        resourceSelector.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        resourceSelector.getChildren().addAll(
            new Label("Resource:"), 
            resourceCombo,
            summaryLabel
        );
        
        HBox buttonBar = new HBox(10);
        buttonBar.getChildren().addAll(addNewBtn, editBtn, deleteBtn, new Separator(), viewAllBtn);
        
        topSection.getChildren().addAll(resourceSelector, buttonBar);
        
        BorderPane layout = new BorderPane();
        layout.setTop(topSection);
        layout.setCenter(tableView);
        
        // Add a bottom info panel
        VBox infoPanel = new VBox(5);
        infoPanel.setPadding(new Insets(10));
        infoPanel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        
        Label infoLabel = new Label("Tips:");
        infoLabel.setStyle("-fx-font-weight: bold;");
        
        Label tip1 = new Label("• Add unavailability periods to prevent scheduling during vacations, training, or other absences");
        Label tip2 = new Label("• Resources with NO assignments can still have unavailability periods set");
        Label tip3 = new Label("• Unavailability periods appear as patterned bars on the timeline");
        
        tip1.setStyle("-fx-font-size: 11px;");
        tip2.setStyle("-fx-font-size: 11px;");
        tip3.setStyle("-fx-font-size: 11px;");
        
        infoPanel.getChildren().addAll(infoLabel, tip1, tip2, tip3);
        layout.setBottom(infoPanel);
        
        getDialogPane().setContent(layout);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Set dialog size
        getDialogPane().setPrefWidth(950);
        getDialogPane().setPrefHeight(600);
        
        // Auto-select first resource if available
        if (!allResources.isEmpty()) {
            resourceCombo.setValue(allResources.get(0));
        }
    }
    
    private void loadUnavailabilities(Resource resource) {
        List<TechnicianUnavailability> list = schedulingService.getResourceUnavailabilities(resource.getId());
        unavailabilities.setAll(list);
    }
    
    private void updateSummary(Resource resource) {
        List<TechnicianUnavailability> list = schedulingService.getResourceUnavailabilities(resource.getId());
        
        if (list.isEmpty()) {
            summaryLabel.setText("No unavailability periods - fully available for scheduling");
            summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #28a745;");
        } else {
            // Calculate total days unavailable
            long totalDays = list.stream()
                .mapToLong(u -> u.getStartDate().until(u.getEndDate()).getDays() + 1)
                .sum();
            
            // Find next upcoming unavailability
            LocalDate today = LocalDate.now();
            Optional<TechnicianUnavailability> nextUnavailable = list.stream()
                .filter(u -> u.getEndDate().isAfter(today))
                .min((u1, u2) -> u1.getStartDate().compareTo(u2.getStartDate()));
            
            String summary = String.format("%d period(s), %d total days", list.size(), totalDays);
            if (nextUnavailable.isPresent()) {
                TechnicianUnavailability next = nextUnavailable.get();
                if (next.getStartDate().isBefore(today) || next.getStartDate().equals(today)) {
                    summary += " - Currently unavailable until " + next.getEndDate().format(DATE_FORMAT);
                    summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc3545;");
                } else {
                    summary += " - Next: " + next.getStartDate().format(DATE_FORMAT);
                    summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffc107;");
                }
            } else {
                summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            }
            
            summaryLabel.setText(summary);
        }
    }
    
    private void addNewUnavailability() {
        Resource selected = resourceCombo.getValue();
        if (selected == null) return;
        
        ResourceUnavailabilityDialog dialog = new ResourceUnavailabilityDialog(
            schedulingService, 
            schedulingService.getAllResources(),
            selected,
            null
        );
        
        // Initialize dialog position to same screen as parent
        Window owner = getDialogPane().getScene().getWindow();
        DialogUtils.initializeDialog(dialog, owner);
        
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
            loadUnavailabilities(selected);
            updateSummary(selected);
            showInfo("Unavailability Added", "The unavailability period has been added successfully.");
        }
    }
    
    private void editSelectedUnavailability() {
        TechnicianUnavailability selected = tableView.getSelectionModel().getSelectedItem();
        Resource resource = resourceCombo.getValue();
        if (selected == null || resource == null) return;
        
        ResourceUnavailabilityDialog dialog = new ResourceUnavailabilityDialog(
            schedulingService,
            schedulingService.getAllResources(),
            resource,
            selected
        );
        
        // Initialize dialog position to same screen as parent
        Window owner = getDialogPane().getScene().getWindow();
        DialogUtils.initializeDialog(dialog, owner);
        
        Optional<TechnicianUnavailability> result = dialog.showAndWait();
        if (result.isPresent()) {
            TechnicianUnavailability updated = result.get();
            // Update in database - for now delete and recreate
            schedulingService.deleteUnavailability(selected.getId());
            schedulingService.createUnavailability(
                updated.getResourceId(),
                updated.getType(),
                updated.getStartDate(),
                updated.getEndDate(),
                updated.getReason()
            );
            loadUnavailabilities(resource);
            updateSummary(resource);
            showInfo("Unavailability Updated", "The unavailability period has been updated successfully.");
        }
    }
    
    private void deleteSelectedUnavailability() {
        TechnicianUnavailability selected = tableView.getSelectionModel().getSelectedItem();
        Resource resource = resourceCombo.getValue();
        if (selected == null || resource == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Unavailability Period");
        confirm.setContentText(String.format(
            "Are you sure you want to delete this %s period from %s to %s?",
            selected.getType().getDisplayName(),
            selected.getStartDate().format(DATE_FORMAT),
            selected.getEndDate().format(DATE_FORMAT)
        ));
        
        // Initialize dialog position to same screen as parent
        Window owner = getDialogPane().getScene().getWindow();
        DialogUtils.initializeDialog(confirm, owner);
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            schedulingService.deleteUnavailability(selected.getId());
            loadUnavailabilities(resource);
            updateSummary(resource);
            showInfo("Unavailability Deleted", "The unavailability period has been deleted successfully.");
        }
    }
    
    private void viewAllResourcesUnavailability() {
        // Create a summary dialog showing all resources and their unavailability
        Dialog<Void> summaryDialog = new Dialog<>();
        summaryDialog.setTitle("All Resources Unavailability Summary");
        summaryDialog.setHeaderText("Overview of all resource unavailability periods");
        
        // Initialize dialog position to same screen as parent
        Window owner = getDialogPane().getScene().getWindow();
        DialogUtils.initializeDialog(summaryDialog, owner);
        
        TextArea summaryText = new TextArea();
        summaryText.setEditable(false);
        summaryText.setPrefWidth(600);
        summaryText.setPrefHeight(400);
        
        StringBuilder summary = new StringBuilder();
        List<Resource> allResources = schedulingService.getAllResources();
        LocalDate today = LocalDate.now();
        
        for (Resource resource : allResources) {
            List<TechnicianUnavailability> unavails = schedulingService.getResourceUnavailabilities(resource.getId());
            
            summary.append(resource.getName()).append(" (").append(resource.getResourceType()).append(")\n");
            
            if (unavails.isEmpty()) {
                summary.append("  ✓ Fully available\n");
            } else {
                for (TechnicianUnavailability u : unavails) {
                    String status = "";
                    if (u.getEndDate().isBefore(today)) {
                        status = " [PAST]";
                    } else if (u.getStartDate().isBefore(today) || u.getStartDate().equals(today)) {
                        status = " [CURRENT]";
                    } else {
                        status = " [FUTURE]";
                    }
                    
                    summary.append(String.format("  • %s: %s to %s%s\n",
                        u.getType().getDisplayName(),
                        u.getStartDate().format(DATE_FORMAT),
                        u.getEndDate().format(DATE_FORMAT),
                        status
                    ));
                }
            }
            summary.append("\n");
        }
        
        summaryText.setText(summary.toString());
        summaryDialog.getDialogPane().setContent(summaryText);
        summaryDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        summaryDialog.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Initialize dialog position to same screen as parent
        Window owner = getDialogPane().getScene().getWindow();
        DialogUtils.initializeDialog(alert, owner);
        
        alert.showAndWait();
    }
}