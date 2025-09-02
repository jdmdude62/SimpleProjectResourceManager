package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShopAutoAssignDialog extends Dialog<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ShopAutoAssignDialog.class);
    
    private final ComboBox<Project> shopProjectCombo;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final ListView<Resource> resourceListView;
    private final CheckBox selectAllCheckBox;
    private final CheckBox skipHolidaysCheckBox;
    private final CheckBox excludeWeekendsCheckBox;
    private final CheckBox deleteExistingCheckBox;
    private final Button previewButton;
    private final Label previewLabel;
    
    private final SchedulingService schedulingService;
    private final List<Project> shopProjects;
    private final List<Resource> availableResources;
    
    public ShopAutoAssignDialog(SchedulingService schedulingService, Window owner) {
        this.schedulingService = schedulingService;
        
        setTitle("Auto-Assign SHOP Time");
        setHeaderText("Automatically assign available resources to SHOP for unscheduled weekdays");
        
        // Get all SHOP projects
        this.shopProjects = schedulingService.getAllProjects().stream()
            .filter(p -> "SHOP".equalsIgnoreCase(p.getProjectId()) || 
                         "SHOP".equalsIgnoreCase(p.getProjectId().trim()))
            .collect(Collectors.toList());
        
        logger.info("Found {} SHOP projects in database", shopProjects.size());
        for (Project p : shopProjects) {
            logger.info("SHOP Project: ID='{}', Desc='{}', Dates={} to {}", 
                p.getProjectId(), p.getDescription(), p.getStartDate(), p.getEndDate());
        }
        
        // Get all active resources
        this.availableResources = schedulingService.getAllResources().stream()
            .filter(Resource::isActive)
            .collect(Collectors.toList());
        
        // Initialize controls
        shopProjectCombo = new ComboBox<>(FXCollections.observableArrayList(shopProjects));
        shopProjectCombo.setConverter(new StringConverter<Project>() {
            @Override
            public String toString(Project project) {
                return project != null ? 
                    project.getDescription() + " (" + 
                    project.getStartDate() + " to " + project.getEndDate() + ")" : "";
            }
            
            @Override
            public Project fromString(String string) {
                return null;
            }
        });
        shopProjectCombo.setPrefWidth(400);
        
        // Set default dates to current month
        YearMonth currentMonth = YearMonth.now();
        startDatePicker = new DatePicker(currentMonth.atDay(1));
        endDatePicker = new DatePicker(currentMonth.atEndOfMonth());
        
        // Resource selection list
        resourceListView = new ListView<>(FXCollections.observableArrayList(availableResources));
        resourceListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        resourceListView.setPrefHeight(250);
        resourceListView.setCellFactory(lv -> new ListCell<Resource>() {
            @Override
            protected void updateItem(Resource resource, boolean empty) {
                super.updateItem(resource, empty);
                if (empty || resource == null) {
                    setText(null);
                } else {
                    String typeInfo = resource.getResourceType() != null ? 
                        " (" + resource.getResourceType().getName() + ")" : "";
                    setText(resource.getName() + typeInfo);
                }
            }
        });
        
        // Select all checkbox
        selectAllCheckBox = new CheckBox("Select All Resources");
        selectAllCheckBox.setOnAction(e -> {
            if (selectAllCheckBox.isSelected()) {
                resourceListView.getSelectionModel().selectAll();
            } else {
                resourceListView.getSelectionModel().clearSelection();
            }
        });
        
        // Skip holidays checkbox - now uses Holiday Calendar
        skipHolidaysCheckBox = new CheckBox("Skip Company Holidays (from Holiday Calendar)");
        skipHolidaysCheckBox.setSelected(true);
        
        // Exclude weekends checkbox
        excludeWeekendsCheckBox = new CheckBox("Exclude Weekends (Saturday/Sunday)");
        excludeWeekendsCheckBox.setSelected(true);
        
        // Delete existing SHOP assignments checkbox
        deleteExistingCheckBox = new CheckBox("Delete existing SHOP assignments first");
        deleteExistingCheckBox.setSelected(false);
        
        // Preview button and label
        previewButton = new Button("Preview");
        previewButton.setOnAction(e -> showPreview());
        previewLabel = new Label("");
        previewLabel.setStyle("-fx-font-weight: bold;");
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // SHOP Project selection
        grid.add(new Label("SHOP Project:"), 0, row);
        grid.add(shopProjectCombo, 1, row);
        row++;
        
        // Date range
        grid.add(new Label("Date Range:"), 0, row);
        HBox dateBox = new HBox(10);
        dateBox.getChildren().addAll(startDatePicker, new Label("to"), endDatePicker);
        grid.add(dateBox, 1, row);
        row++;
        
        // Options
        grid.add(new Label("Options:"), 0, row);
        VBox optionsBox = new VBox(5);
        optionsBox.getChildren().addAll(excludeWeekendsCheckBox, skipHolidaysCheckBox, deleteExistingCheckBox);
        grid.add(optionsBox, 1, row);
        row++;
        
        // Resource selection
        grid.add(new Label("Resources:"), 0, row);
        VBox resourceBox = new VBox(5);
        resourceBox.getChildren().addAll(selectAllCheckBox, resourceListView);
        grid.add(resourceBox, 1, row);
        GridPane.setVgrow(resourceBox, Priority.ALWAYS);
        row++;
        
        // Preview
        HBox previewBox = new HBox(10);
        previewBox.getChildren().addAll(previewButton, previewLabel);
        grid.add(previewBox, 1, row);
        row++;
        
        // Info text
        Label infoLabel = new Label(
            "This will create individual SHOP assignments for each selected resource\n" +
            "on weekdays where they have no existing assignments or unavailability."
        );
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        grid.add(infoLabel, 0, row, 2, 1);
        
        // Set up dialog
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefWidth(600);
        getDialogPane().setPrefHeight(550);
        
        // Enable/disable OK button based on selection
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Create Assignments");
        okButton.setDisable(true);
        
        // Validation
        shopProjectCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateInputs());
        resourceListView.getSelectionModel().getSelectedItems().addListener(
            (javafx.collections.ListChangeListener<Resource>) c -> validateInputs()
        );
        
        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Project shopProject = shopProjectCombo.getValue();
                List<Resource> selectedResources = new ArrayList<>(resourceListView.getSelectionModel().getSelectedItems());
                
                logger.info("Dialog OK pressed - Project: {}, Start: {}, End: {}, Resources selected: {}, Delete existing: {}, Skip holidays: {}, Exclude weekends: {}", 
                    shopProject != null ? shopProject.getProjectId() : "null",
                    startDatePicker.getValue(),
                    endDatePicker.getValue(),
                    selectedResources.size(),
                    deleteExistingCheckBox.isSelected(),
                    skipHolidaysCheckBox.isSelected(),
                    excludeWeekendsCheckBox.isSelected());
                
                try {
                    // Delete existing SHOP assignments if requested
                    if (deleteExistingCheckBox.isSelected()) {
                        int deletedCount = schedulingService.deleteShopAssignments(shopProject, selectedResources);
                        logger.info("Deleted {} existing SHOP assignments", deletedCount);
                    }
                    
                    int count = schedulingService.autoAssignShopTime(
                        shopProject,
                        startDatePicker.getValue(),
                        endDatePicker.getValue(),
                        selectedResources,
                        skipHolidaysCheckBox.isSelected(),
                        excludeWeekendsCheckBox.isSelected()
                    );
                    logger.info("Auto-assign completed with {} assignments created", count);
                    return count;
                } catch (Exception e) {
                    logger.error("Failed to auto-assign SHOP time", e);
                    showError("Failed to create assignments: " + e.getMessage());
                    return 0;
                }
            }
            return null;
        });
        
        // Select first SHOP project if available
        if (!shopProjects.isEmpty()) {
            shopProjectCombo.setValue(shopProjects.get(0));
        } else {
            logger.warn("No SHOP projects found in database!");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No SHOP Project");
            alert.setHeaderText("No SHOP project found");
            alert.setContentText("Please create a project with ID 'SHOP' first using the Project dialog with the SHOP radio button selected.");
            alert.showAndWait();
        }
    }
    
    private void validateInputs() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        boolean valid = shopProjectCombo.getValue() != null &&
                       !resourceListView.getSelectionModel().getSelectedItems().isEmpty() &&
                       startDatePicker.getValue() != null &&
                       endDatePicker.getValue() != null &&
                       !startDatePicker.getValue().isAfter(endDatePicker.getValue());
        okButton.setDisable(!valid);
    }
    
    private void showPreview() {
        if (shopProjectCombo.getValue() == null || 
            resourceListView.getSelectionModel().getSelectedItems().isEmpty()) {
            previewLabel.setText("Select project and resources first");
            return;
        }
        
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        int weekdays = 0;
        LocalDate current = start;
        
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek().getValue() <= 5) {
                weekdays++;
            }
            current = current.plusDays(1);
        }
        
        int resourceCount = resourceListView.getSelectionModel().getSelectedItems().size();
        int maxAssignments = weekdays * resourceCount;
        
        previewLabel.setText(String.format("Up to %d assignments (%d weekdays × %d resources)", 
            maxAssignments, weekdays, resourceCount));
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}