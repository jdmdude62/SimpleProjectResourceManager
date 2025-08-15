package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ResourceUnavailabilityDialog extends Dialog<TechnicianUnavailability> {
    private static final Logger logger = LoggerFactory.getLogger(ResourceUnavailabilityDialog.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    
    private final ComboBox<Resource> resourceCombo;
    private final ComboBox<UnavailabilityType> typeCombo;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final TextField reasonField;
    private final TextArea descriptionArea;
    private final CheckBox recurringCheckBox;
    private final TextField recurrencePatternField;
    private final CheckBox requiresApprovalCheckBox;
    
    private final SchedulingService schedulingService;
    private final List<Resource> resources;
    private TechnicianUnavailability editingUnavailability;
    
    public ResourceUnavailabilityDialog(SchedulingService schedulingService, List<Resource> resources) {
        this(schedulingService, resources, null, null);
    }
    
    public ResourceUnavailabilityDialog(SchedulingService schedulingService, List<Resource> resources, 
                                       Resource preselectedResource, TechnicianUnavailability existingUnavailability) {
        this.schedulingService = schedulingService;
        this.resources = resources;
        this.editingUnavailability = existingUnavailability;
        
        setTitle(existingUnavailability == null ? "Mark Resource Unavailable" : "Edit Resource Unavailability");
        setHeaderText(existingUnavailability == null ? 
            "Enter details for resource unavailability" : 
            "Update resource unavailability details");
        
        // Create form controls
        resourceCombo = new ComboBox<>(FXCollections.observableArrayList(resources));
        resourceCombo.setCellFactory(new Callback<ListView<Resource>, ListCell<Resource>>() {
            @Override
            public ListCell<Resource> call(ListView<Resource> l) {
                return new ListCell<Resource>() {
                    @Override
                    protected void updateItem(Resource item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "" : item.getName());
                    }
                };
            }
        });
        resourceCombo.setButtonCell(resourceCombo.getCellFactory().call(null));
        resourceCombo.setPrefWidth(300);
        
        typeCombo = new ComboBox<>(FXCollections.observableArrayList(UnavailabilityType.values()));
        typeCombo.setPrefWidth(300);
        
        startDatePicker = new DatePicker();
        startDatePicker.setPrefWidth(300);
        startDatePicker.setPromptText("Start date");
        
        endDatePicker = new DatePicker();
        endDatePicker.setPrefWidth(300);
        endDatePicker.setPromptText("End date");
        
        reasonField = new TextField();
        reasonField.setPromptText("Brief reason (e.g., Annual vacation, Medical appointment)");
        reasonField.setPrefWidth(300);
        
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Additional details (optional)");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPrefWidth(300);
        descriptionArea.setWrapText(true);
        
        recurringCheckBox = new CheckBox("Recurring unavailability");
        recurrencePatternField = new TextField();
        recurrencePatternField.setPromptText("e.g., WEEKLY:FRIDAY or MONTHLY:FIRST_MONDAY");
        recurrencePatternField.setPrefWidth(300);
        recurrencePatternField.setDisable(true);
        
        requiresApprovalCheckBox = new CheckBox("Requires manager approval");
        requiresApprovalCheckBox.setSelected(true);
        
        // Set up date validation
        startDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && endDatePicker.getValue() != null && newDate.isAfter(endDatePicker.getValue())) {
                endDatePicker.setValue(newDate);
            }
        });
        
        endDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && startDatePicker.getValue() != null && newDate.isBefore(startDatePicker.getValue())) {
                startDatePicker.setValue(newDate);
            }
        });
        
        // Enable/disable recurrence pattern field
        recurringCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            recurrencePatternField.setDisable(!isSelected);
            if (!isSelected) {
                recurrencePatternField.clear();
            }
        });
        
        // Pre-populate if editing or preselected
        if (preselectedResource != null) {
            resourceCombo.setValue(preselectedResource);
            resourceCombo.setDisable(true);
        }
        
        if (existingUnavailability != null) {
            populateFromExisting(existingUnavailability);
        } else {
            // Set defaults for new unavailability
            typeCombo.setValue(UnavailabilityType.VACATION);
            startDatePicker.setValue(LocalDate.now().plusDays(1));
            endDatePicker.setValue(LocalDate.now().plusDays(1));
        }
        
        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        
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
        
        grid.add(recurringCheckBox, 1, row++);
        
        grid.add(new Label("Pattern:"), 0, row);
        grid.add(recurrencePatternField, 1, row++);
        
        grid.add(requiresApprovalCheckBox, 1, row++);
        
        // Add conflict check section
        VBox conflictSection = createConflictCheckSection();
        grid.add(conflictSection, 0, row, 2, 1);
        
        getDialogPane().setContent(grid);
        
        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Enable/disable save button based on validation
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        // Validation
        Runnable validateForm = () -> {
            boolean valid = resourceCombo.getValue() != null &&
                          typeCombo.getValue() != null &&
                          startDatePicker.getValue() != null &&
                          endDatePicker.getValue() != null &&
                          !reasonField.getText().trim().isEmpty();
            saveButton.setDisable(!valid);
        };
        
        resourceCombo.valueProperty().addListener((obs, old, val) -> validateForm.run());
        typeCombo.valueProperty().addListener((obs, old, val) -> validateForm.run());
        startDatePicker.valueProperty().addListener((obs, old, val) -> validateForm.run());
        endDatePicker.valueProperty().addListener((obs, old, val) -> validateForm.run());
        reasonField.textProperty().addListener((obs, old, val) -> validateForm.run());
        
        // Convert result
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createUnavailability();
            }
            return null;
        });
        
        // Initial validation
        validateForm.run();
    }
    
    private VBox createConflictCheckSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(10, 0, 0, 0));
        
        Button checkConflictsButton = new Button("Check for Conflicts");
        Label conflictLabel = new Label();
        conflictLabel.setWrapText(true);
        conflictLabel.setStyle("-fx-text-fill: #d9534f;");
        
        checkConflictsButton.setOnAction(e -> {
            if (resourceCombo.getValue() != null && 
                startDatePicker.getValue() != null && 
                endDatePicker.getValue() != null) {
                
                Resource resource = resourceCombo.getValue();
                LocalDate start = startDatePicker.getValue();
                LocalDate end = endDatePicker.getValue();
                
                // Check for existing assignments
                List<Assignment> conflicts = schedulingService.getConflictingAssignments(
                    resource.getId(), start, end);
                
                if (conflicts.isEmpty()) {
                    conflictLabel.setText("✓ No conflicts found. Resource is available during this period.");
                    conflictLabel.setStyle("-fx-text-fill: #5cb85c;");
                } else {
                    String conflictText = String.format(
                        "⚠ Warning: Resource has %d assignment(s) during this period. " +
                        "These assignments may need to be reassigned.",
                        conflicts.size()
                    );
                    conflictLabel.setText(conflictText);
                    conflictLabel.setStyle("-fx-text-fill: #f0ad4e;");
                }
            }
        });
        
        section.getChildren().addAll(
            new Separator(),
            checkConflictsButton,
            conflictLabel
        );
        
        return section;
    }
    
    private void populateFromExisting(TechnicianUnavailability unavailability) {
        // Find the resource
        resources.stream()
            .filter(r -> r.getId().equals(unavailability.getResourceId()))
            .findFirst()
            .ifPresent(resourceCombo::setValue);
        
        resourceCombo.setDisable(true); // Can't change resource when editing
        
        typeCombo.setValue(unavailability.getType());
        startDatePicker.setValue(unavailability.getStartDate());
        endDatePicker.setValue(unavailability.getEndDate());
        reasonField.setText(unavailability.getReason());
        
        if (unavailability.getDescription() != null) {
            descriptionArea.setText(unavailability.getDescription());
        }
        
        recurringCheckBox.setSelected(unavailability.isRecurring());
        if (unavailability.getRecurrencePattern() != null) {
            recurrencePatternField.setText(unavailability.getRecurrencePattern());
        }
        
        requiresApprovalCheckBox.setSelected(!unavailability.isApproved());
    }
    
    private TechnicianUnavailability createUnavailability() {
        TechnicianUnavailability unavailability;
        
        if (editingUnavailability != null) {
            unavailability = editingUnavailability;
        } else {
            unavailability = new TechnicianUnavailability();
        }
        
        unavailability.setResourceId(resourceCombo.getValue().getId());
        unavailability.setType(typeCombo.getValue());
        unavailability.setStartDate(startDatePicker.getValue());
        unavailability.setEndDate(endDatePicker.getValue());
        unavailability.setReason(reasonField.getText().trim());
        
        String description = descriptionArea.getText().trim();
        if (!description.isEmpty()) {
            unavailability.setDescription(description);
        }
        
        unavailability.setRecurring(recurringCheckBox.isSelected());
        if (recurringCheckBox.isSelected()) {
            unavailability.setRecurrencePattern(recurrencePatternField.getText().trim());
        }
        
        // If doesn't require approval, mark as auto-approved
        if (!requiresApprovalCheckBox.isSelected()) {
            unavailability.setApproved(true);
            unavailability.setApprovedBy("Auto-approved");
        }
        
        logger.info("Created unavailability for resource {} from {} to {}", 
            resourceCombo.getValue().getName(),
            startDatePicker.getValue(),
            endDatePicker.getValue());
        
        return unavailability;
    }
}