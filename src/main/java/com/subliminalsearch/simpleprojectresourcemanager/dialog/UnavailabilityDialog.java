package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.TechnicianUnavailability;
import com.subliminalsearch.simpleprojectresourcemanager.model.UnavailabilityType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class UnavailabilityDialog extends Dialog<TechnicianUnavailability> {
    private static final Logger logger = LoggerFactory.getLogger(UnavailabilityDialog.class);
    
    private final ComboBox<Resource> resourceCombo;
    private final ComboBox<UnavailabilityType> typeCombo;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final TextField reasonField;
    private final TextArea descriptionArea;
    private final CheckBox approvedCheckBox;
    private final TextField approvedByField;
    private final CheckBox recurringCheckBox;
    private final TextField recurrencePatternField;
    
    private final List<Resource> availableResources;

    public UnavailabilityDialog(List<Resource> resources) {
        this(null, resources);
    }
    
    public UnavailabilityDialog(TechnicianUnavailability unavailability, List<Resource> resources) {
        this.availableResources = resources;
        
        setTitle(unavailability == null ? "New Technician Unavailability" : "Edit Technician Unavailability");
        setHeaderText(null);
        
        // Initialize components
        resourceCombo = new ComboBox<>(FXCollections.observableArrayList(resources));
        resourceCombo.setCellFactory(createResourceCellFactory());
        resourceCombo.setButtonCell(createResourceListCell());
        resourceCombo.setPrefWidth(200);
        
        typeCombo = new ComboBox<>(FXCollections.observableArrayList(UnavailabilityType.values()));
        typeCombo.setPrefWidth(200);
        
        startDatePicker = new DatePicker(LocalDate.now());
        endDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        
        reasonField = new TextField();
        reasonField.setPromptText("Brief reason for unavailability");
        reasonField.setPrefWidth(300);
        
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Detailed description (optional)");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPrefWidth(300);
        
        approvedCheckBox = new CheckBox("Approved");
        approvedByField = new TextField();
        approvedByField.setPromptText("Approved by (manager/supervisor)");
        approvedByField.setPrefWidth(200);
        
        recurringCheckBox = new CheckBox("Recurring");
        recurrencePatternField = new TextField();
        recurrencePatternField.setPromptText("e.g., WEEKLY:FRIDAY, MONTHLY:LAST_FRIDAY");
        recurrencePatternField.setPrefWidth(250);
        recurrencePatternField.setDisable(true);
        
        // Set up form layout
        GridPane grid = createFormLayout();
        
        // Set up recurring checkbox behavior
        recurringCheckBox.setOnAction(e -> {
            recurrencePatternField.setDisable(!recurringCheckBox.isSelected());
            if (!recurringCheckBox.isSelected()) {
                recurrencePatternField.clear();
            }
        });
        
        // Set up approval checkbox behavior
        approvedCheckBox.setOnAction(e -> {
            approvedByField.setDisable(!approvedCheckBox.isSelected());
            if (!approvedCheckBox.isSelected()) {
                approvedByField.clear();
            }
        });
        
        // Initially disable approved by field
        approvedByField.setDisable(!approvedCheckBox.isSelected());
        
        // Populate fields if editing
        if (unavailability != null) {
            populateFields(unavailability);
        } else {
            // Set defaults for new unavailability
            typeCombo.setValue(UnavailabilityType.VACATION);
        }
        
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Set validation AFTER adding button types
        setupValidation();
        
        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return createUnavailabilityFromFields(unavailability);
            }
            return null;
        });
        
        // Focus on resource combo
        resourceCombo.requestFocus();
    }
    
    private GridPane createFormLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        int row = 0;
        
        grid.add(new Label("Resource:"), 0, row);
        grid.add(resourceCombo, 1, row++);
        
        grid.add(new Label("Type:"), 0, row);
        grid.add(typeCombo, 1, row++);
        
        grid.add(new Label("Start Date:"), 0, row);
        grid.add(startDatePicker, 1, row++);
        
        grid.add(new Label("End Date:"), 0, row);
        grid.add(endDatePicker, 1, row++);
        
        grid.add(new Label("Reason:"), 0, row);
        grid.add(reasonField, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionArea, 1, row++);
        
        // Approval section
        VBox approvalBox = new VBox(5);
        approvalBox.getChildren().addAll(approvedCheckBox, approvedByField);
        grid.add(new Label("Approval:"), 0, row);
        grid.add(approvalBox, 1, row++);
        
        // Recurrence section
        VBox recurrenceBox = new VBox(5);
        recurrenceBox.getChildren().addAll(recurringCheckBox, recurrencePatternField);
        grid.add(new Label("Recurrence:"), 0, row);
        grid.add(recurrenceBox, 1, row++);
        
        return grid;
    }
    
    private void setupValidation() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        
        // Enable OK button only when required fields are filled
        okButton.disableProperty().bind(
            resourceCombo.valueProperty().isNull()
                .or(typeCombo.valueProperty().isNull())
                .or(startDatePicker.valueProperty().isNull())
                .or(endDatePicker.valueProperty().isNull())
                .or(reasonField.textProperty().isEmpty())
        );
        
        // Date validation
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && endDatePicker.getValue() != null && newVal.isAfter(endDatePicker.getValue())) {
                endDatePicker.setValue(newVal.plusDays(1));
            }
        });
        
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && startDatePicker.getValue() != null && newVal.isBefore(startDatePicker.getValue())) {
                showValidationError("End date cannot be before start date");
                endDatePicker.setValue(startDatePicker.getValue().plusDays(1));
            }
        });
    }
    
    private void populateFields(TechnicianUnavailability unavailability) {
        // Find and set resource
        availableResources.stream()
            .filter(r -> r.getId().equals(unavailability.getResourceId()))
            .findFirst()
            .ifPresent(resourceCombo::setValue);
            
        typeCombo.setValue(unavailability.getType());
        startDatePicker.setValue(unavailability.getStartDate());
        endDatePicker.setValue(unavailability.getEndDate());
        reasonField.setText(unavailability.getReason() != null ? unavailability.getReason() : "");
        descriptionArea.setText(unavailability.getDescription() != null ? unavailability.getDescription() : "");
        approvedCheckBox.setSelected(unavailability.isApproved());
        approvedByField.setText(unavailability.getApprovedBy() != null ? unavailability.getApprovedBy() : "");
        recurringCheckBox.setSelected(unavailability.isRecurring());
        recurrencePatternField.setText(unavailability.getRecurrencePattern() != null ? unavailability.getRecurrencePattern() : "");
        
        // Update field states based on checkboxes
        approvedByField.setDisable(!approvedCheckBox.isSelected());
        recurrencePatternField.setDisable(!recurringCheckBox.isSelected());
    }
    
    private TechnicianUnavailability createUnavailabilityFromFields(TechnicianUnavailability existing) {
        TechnicianUnavailability unavailability = existing != null ? existing : new TechnicianUnavailability();
        
        unavailability.setResourceId(resourceCombo.getValue().getId());
        unavailability.setType(typeCombo.getValue());
        unavailability.setStartDate(startDatePicker.getValue());
        unavailability.setEndDate(endDatePicker.getValue());
        unavailability.setReason(reasonField.getText().trim());
        
        String description = descriptionArea.getText().trim();
        unavailability.setDescription(description.isEmpty() ? null : description);
        
        unavailability.setApproved(approvedCheckBox.isSelected());
        
        String approvedBy = approvedByField.getText().trim();
        unavailability.setApprovedBy(approvedBy.isEmpty() ? null : approvedBy);
        
        unavailability.setRecurring(recurringCheckBox.isSelected());
        
        String pattern = recurrencePatternField.getText().trim();
        unavailability.setRecurrencePattern(pattern.isEmpty() ? null : pattern);
        
        return unavailability;
    }
    
    private Callback<ListView<Resource>, ListCell<Resource>> createResourceCellFactory() {
        return listView -> createResourceListCell();
    }
    
    private ListCell<Resource> createResourceListCell() {
        return new ListCell<Resource>() {
            @Override
            protected void updateItem(Resource resource, boolean empty) {
                super.updateItem(resource, empty);
                if (empty || resource == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (%s)", resource.getName(), resource.getEmail()));
                }
            }
        };
    }
    
    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Static utility methods for showing details and confirmation dialogs
    public static void showUnavailabilityDetails(TechnicianUnavailability unavailability, Resource resource) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Unavailability Details");
        alert.setHeaderText(String.format("%s - %s", resource.getName(), unavailability.getType()));
        
        StringBuilder content = new StringBuilder();
        content.append(String.format("Resource: %s (%s)\n", resource.getName(), resource.getEmail()));
        content.append(String.format("Type: %s\n", unavailability.getType()));
        content.append(String.format("Period: %s to %s\n", unavailability.getStartDate(), unavailability.getEndDate()));
        content.append(String.format("Reason: %s\n", unavailability.getReason()));
        
        if (unavailability.getDescription() != null) {
            content.append(String.format("Description: %s\n", unavailability.getDescription()));
        }
        
        content.append(String.format("Status: %s\n", unavailability.isApproved() ? "Approved" : "Pending Approval"));
        
        if (unavailability.isApproved() && unavailability.getApprovedBy() != null) {
            content.append(String.format("Approved by: %s\n", unavailability.getApprovedBy()));
        }
        
        if (unavailability.isRecurring()) {
            content.append(String.format("Recurring: %s\n", unavailability.getRecurrencePattern()));
        }
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
    
    public static boolean showDeleteConfirmation(TechnicianUnavailability unavailability, Resource resource) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Unavailability");
        alert.setHeaderText("Are you sure you want to delete this unavailability period?");
        alert.setContentText(String.format("%s: %s from %s to %s\n\nThis action cannot be undone.", 
            resource.getName(), 
            unavailability.getType(),
            unavailability.getStartDate(),
            unavailability.getEndDate()));
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}