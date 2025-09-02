package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.OpenItem;
import com.subliminalsearch.simpleprojectresourcemanager.service.OpenItemService;
import com.subliminalsearch.simpleprojectresourcemanager.service.OpenItemMetadataService;
import com.subliminalsearch.simpleprojectresourcemanager.util.AutoCompleteComboBox;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class OpenItemDialog extends Dialog<OpenItem> {
    private final OpenItem item;
    private final Long projectId;
    private final OpenItemService service;
    private final OpenItemMetadataService metadataService;
    
    private TextField titleField;
    private TextArea descriptionArea;
    private ComboBox<OpenItem.Priority> priorityBox;
    private ComboBox<OpenItem.ItemStatus> statusBox;
    private ComboBox<OpenItem.HealthStatus> healthBox;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Slider progressSlider;
    private Label progressLabel;
    private ComboBox<String> categoryBox;
    private ComboBox<String> tagsBox;
    private TextField estimatedHoursField;
    private TextField actualHoursField;
    private TextArea notesArea;
    
    public OpenItemDialog(OpenItem existingItem, Long projectId, OpenItemService service) {
        this.item = existingItem != null ? existingItem : new OpenItem(projectId, "");
        this.projectId = projectId;
        this.service = service;
        this.metadataService = new OpenItemMetadataService(service.getDataSource());
        
        setupDialog();
        setupForm();
        populateForm();
        setupResultConverter();
    }
    
    private void setupDialog() {
        setTitle(item.getId() == null ? "New Open Item" : "Edit Open Item");
        setHeaderText(item.getId() == null ? "Create a new open item" : "Edit open item: " + item.getTitle());
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
    }
    
    private void setupForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Basic Information
        titleField = new TextField();
        titleField.setPromptText("Enter item title");
        titleField.setPrefWidth(300);
        
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Enter description");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        
        // Category dropdown with industrial automation categories from database
        categoryBox = new ComboBox<>();
        categoryBox.setEditable(true); // Allow custom entries
        categoryBox.setItems(metadataService.getAllCategories());
        categoryBox.setPromptText("Select or type to filter categories");
        categoryBox.setPrefWidth(250);
        
        // Configure autocomplete
        AutoCompleteComboBox.configure(categoryBox);
        
        // Add listener to save custom categories
        categoryBox.setOnAction(e -> {
            String value = categoryBox.getValue();
            if (value != null && !value.trim().isEmpty() && 
                !categoryBox.getItems().contains(value)) {
                metadataService.addCategory(value);
                // Update items and reconfigure autocomplete
                AutoCompleteComboBox.updateItems(categoryBox, metadataService.getAllCategories());
            }
        });
        
        // Priority and Status
        priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll(OpenItem.Priority.values());
        priorityBox.setEditable(true); // Enable typing for search
        StringConverter<OpenItem.Priority> priorityConverter = new StringConverter<OpenItem.Priority>() {
            @Override
            public String toString(OpenItem.Priority priority) {
                return priority != null ? priority.getDisplayName() : "";
            }
            
            @Override
            public OpenItem.Priority fromString(String string) {
                for (OpenItem.Priority p : OpenItem.Priority.values()) {
                    if (p.getDisplayName().equalsIgnoreCase(string)) {
                        return p;
                    }
                }
                return null;
            }
        };
        priorityBox.setConverter(priorityConverter);
        AutoCompleteComboBox.configure(priorityBox, priorityConverter);
        
        statusBox = new ComboBox<>();
        statusBox.getItems().addAll(OpenItem.ItemStatus.values());
        statusBox.setEditable(true); // Enable typing for search
        StringConverter<OpenItem.ItemStatus> statusConverter = new StringConverter<OpenItem.ItemStatus>() {
            @Override
            public String toString(OpenItem.ItemStatus status) {
                return status != null ? status.getDisplayName() : "";
            }
            
            @Override
            public OpenItem.ItemStatus fromString(String string) {
                for (OpenItem.ItemStatus s : OpenItem.ItemStatus.values()) {
                    if (s.getDisplayName().equalsIgnoreCase(string)) {
                        return s;
                    }
                }
                return null;
            }
        };
        statusBox.setConverter(statusConverter);
        AutoCompleteComboBox.configure(statusBox, statusConverter);
        
        healthBox = new ComboBox<>();
        healthBox.getItems().addAll(OpenItem.HealthStatus.values());
        healthBox.setEditable(true); // Enable typing for search
        StringConverter<OpenItem.HealthStatus> healthConverter = new StringConverter<OpenItem.HealthStatus>() {
            @Override
            public String toString(OpenItem.HealthStatus health) {
                return health != null ? health.getDisplayName() : "";
            }
            
            @Override
            public OpenItem.HealthStatus fromString(String string) {
                for (OpenItem.HealthStatus h : OpenItem.HealthStatus.values()) {
                    if (h.getDisplayName().equalsIgnoreCase(string)) {
                        return h;
                    }
                }
                return null;
            }
        };
        healthBox.setConverter(healthConverter);
        AutoCompleteComboBox.configure(healthBox, healthConverter);
        
        // Dates
        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Estimated start date");
        
        endDatePicker = new DatePicker();
        endDatePicker.setPromptText("Estimated end date");
        
        // Progress
        progressSlider = new Slider(0, 100, 0);
        progressSlider.setShowTickLabels(true);
        progressSlider.setShowTickMarks(true);
        progressSlider.setMajorTickUnit(25);
        progressSlider.setMinorTickCount(5);
        progressSlider.setBlockIncrement(5);
        progressSlider.setSnapToTicks(true);
        
        progressLabel = new Label("0%");
        progressSlider.valueProperty().addListener((obs, old, value) -> {
            progressLabel.setText(String.format("%.0f%%", value));
        });
        
        HBox progressBox = new HBox(10);
        progressBox.getChildren().addAll(progressSlider, progressLabel);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        
        // Hours
        estimatedHoursField = new TextField();
        estimatedHoursField.setPromptText("Estimated hours");
        estimatedHoursField.setPrefWidth(100);
        
        actualHoursField = new TextField();
        actualHoursField.setPromptText("Actual hours");
        actualHoursField.setPrefWidth(100);
        
        HBox hoursBox = new HBox(10);
        hoursBox.getChildren().addAll(
            new Label("Est:"), estimatedHoursField,
            new Label("Actual:"), actualHoursField
        );
        
        // Tags dropdown with common industrial automation tags from database
        tagsBox = new ComboBox<>();
        tagsBox.setEditable(true); // Allow custom entries
        tagsBox.setItems(metadataService.getAllTags());
        tagsBox.setPromptText("Select or type to filter tags");
        tagsBox.setPrefWidth(250);
        
        // Configure autocomplete
        AutoCompleteComboBox.configure(tagsBox);
        
        // Add listener to save custom tags
        tagsBox.setOnAction(e -> {
            String value = tagsBox.getValue();
            if (value != null && !value.trim().isEmpty() && 
                !tagsBox.getItems().contains(value)) {
                metadataService.addTag(value);
                // Update items and reconfigure autocomplete
                AutoCompleteComboBox.updateItems(tagsBox, metadataService.getAllTags());
            }
        });
        
        notesArea = new TextArea();
        notesArea.setPromptText("Additional notes");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        
        // Add to grid
        int row = 0;
        grid.add(new Label("Title:"), 0, row);
        grid.add(titleField, 1, row++, 2, 1);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionArea, 1, row++, 2, 1);
        
        grid.add(new Label("Category:"), 0, row);
        grid.add(categoryBox, 1, row++, 2, 1);
        
        grid.add(new Label("Priority:"), 0, row);
        grid.add(priorityBox, 1, row);
        
        grid.add(new Label("Status:"), 2, row);
        grid.add(statusBox, 3, row++);
        
        grid.add(new Label("Health:"), 0, row);
        grid.add(healthBox, 1, row++);
        
        // Fix date layout - keep labels and fields together
        HBox dateBox = new HBox(10);
        dateBox.getChildren().addAll(
            new Label("Start Date:"), startDatePicker,
            new Label("End Date:"), endDatePicker
        );
        grid.add(new Label("Dates:"), 0, row);
        grid.add(dateBox, 1, row++, 3, 1);
        
        grid.add(new Label("Progress:"), 0, row);
        grid.add(progressBox, 1, row++, 3, 1);
        
        grid.add(new Label("Hours:"), 0, row);
        grid.add(hoursBox, 1, row++, 3, 1);
        
        grid.add(new Label("Tags:"), 0, row);
        grid.add(tagsBox, 1, row++, 3, 1);
        
        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++, 3, 1);
        
        getDialogPane().setContent(grid);
        
        // Validation
        Button saveButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
        saveButton.disableProperty().bind(titleField.textProperty().isEmpty());
    }
    
    private void populateForm() {
        if (item.getId() != null) {
            titleField.setText(item.getTitle());
            descriptionArea.setText(item.getDescription());
            categoryBox.setValue(item.getCategory());
            priorityBox.setValue(item.getPriority());
            statusBox.setValue(item.getStatus());
            healthBox.setValue(item.getHealthStatus());
            startDatePicker.setValue(item.getEstimatedStartDate());
            endDatePicker.setValue(item.getEstimatedEndDate());
            progressSlider.setValue(item.getProgressPercentage());
            
            if (item.getEstimatedHours() != null) {
                estimatedHoursField.setText(item.getEstimatedHours().toString());
            }
            if (item.getActualHours() != null) {
                actualHoursField.setText(item.getActualHours().toString());
            }
            
            // For tags, if there are multiple comma-separated, show the first one
            if (item.getTags() != null && !item.getTags().isEmpty()) {
                String[] tags = item.getTags().split(",");
                if (tags.length > 0) {
                    tagsBox.setValue(tags[0].trim());
                }
            }
            notesArea.setText(item.getNotes());
        } else {
            // Defaults for new item
            priorityBox.setValue(OpenItem.Priority.MEDIUM);
            statusBox.setValue(OpenItem.ItemStatus.NOT_STARTED);
            healthBox.setValue(OpenItem.HealthStatus.ON_TRACK);
            progressSlider.setValue(0);
        }
    }
    
    private void setupResultConverter() {
        setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                // Update item from form
                item.setTitle(titleField.getText().trim());
                item.setDescription(descriptionArea.getText().trim());
                String category = categoryBox.getValue() != null ? categoryBox.getValue().trim() : "";
                item.setCategory(category);
                if (!category.isEmpty()) {
                    metadataService.addCategory(category); // Track usage
                }
                item.setPriority(priorityBox.getValue());
                item.setStatus(statusBox.getValue());
                item.setHealthStatus(healthBox.getValue());
                item.setEstimatedStartDate(startDatePicker.getValue());
                item.setEstimatedEndDate(endDatePicker.getValue());
                item.setProgressPercentage((int) progressSlider.getValue());
                
                // Parse hours
                try {
                    if (!estimatedHoursField.getText().trim().isEmpty()) {
                        item.setEstimatedHours(Double.parseDouble(estimatedHoursField.getText().trim()));
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
                
                try {
                    if (!actualHoursField.getText().trim().isEmpty()) {
                        item.setActualHours(Double.parseDouble(actualHoursField.getText().trim()));
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
                
                String tag = tagsBox.getValue() != null ? tagsBox.getValue().trim() : "";
                item.setTags(tag);
                if (!tag.isEmpty()) {
                    metadataService.addTag(tag); // Track usage
                }
                item.setNotes(notesArea.getText().trim());
                
                // Save to database
                if (item.getId() == null) {
                    // For new items, create it first then update with all fields
                    item.setProjectId(projectId);
                    OpenItem created = service.createOpenItem(projectId, item.getTitle(), item.getDescription());
                    
                    if (created == null || created.getId() == null) {
                        // Creation failed
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Failed to create open item");
                        alert.setContentText("The item could not be created. Please check the logs.");
                        alert.showAndWait();
                        return null;
                    }
                    
                    // Now update the created item with all the other fields
                    created.setCategory(item.getCategory());
                    created.setPriority(item.getPriority());
                    created.setStatus(item.getStatus());
                    created.setHealthStatus(item.getHealthStatus());
                    created.setEstimatedStartDate(item.getEstimatedStartDate());
                    created.setEstimatedEndDate(item.getEstimatedEndDate());
                    created.setProgressPercentage(item.getProgressPercentage());
                    created.setEstimatedHours(item.getEstimatedHours());
                    created.setActualHours(item.getActualHours());
                    created.setTags(item.getTags());
                    created.setNotes(item.getNotes());
                    
                    // Update with all fields
                    OpenItem updated = service.updateOpenItem(created);
                    
                    // Don't refresh cache here - let the service manage its own cache
                    
                    return updated;
                } else {
                    return service.updateOpenItem(item);
                }
            }
            return null;
        });
    }
}