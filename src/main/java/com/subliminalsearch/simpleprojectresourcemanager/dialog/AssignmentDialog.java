package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.dialog.ResourceSkillFilterDialog;
import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import com.subliminalsearch.simpleprojectresourcemanager.util.HelpButton;
import com.subliminalsearch.simpleprojectresourcemanager.util.InputValidator;
import com.subliminalsearch.simpleprojectresourcemanager.util.InputValidator.ValidationResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AssignmentDialog extends Dialog<Assignment> {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentDialog.class);
    
    private final ComboBox<Project> projectCombo;
    private final ComboBox<Resource> resourceCombo;
    private final ComboBox<ProjectManager> projectManagerCombo;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final Spinner<Integer> travelOutSpinner;
    private final Spinner<Integer> travelBackSpinner;
    private final CheckBox overrideCheckBox;
    private final TextField overrideReasonField;
    private final TextArea notesArea;
    private final TextField locationField;
    private final Button editProjectButton;
    
    // Filter fields
    private final DatePicker filterStartDatePicker;
    private final Spinner<Integer> filterDateRangeSpinner;
    private final TextField filterProjectIdField;
    private final TextField filterDescriptionField;
    private final Button clearFiltersButton;
    
    private final boolean isEditMode;
    private final Assignment existingAssignment;
    private final List<Project> availableProjects;
    private final List<Resource> availableResources;
    private ObservableList<Project> filteredProjects;
    private SchedulingService schedulingService;
    private Label conflictWarningLabel;
    private Label validationLabel;
    
    public AssignmentDialog(List<Project> projects, List<Resource> resources) {
        this(null, projects, resources, null);
    }
    
    public AssignmentDialog(List<Project> projects, List<Resource> resources, SchedulingService schedulingService) {
        this(null, projects, resources, schedulingService);
    }
    
    public AssignmentDialog(Assignment assignment, List<Project> projects, List<Resource> resources) {
        this(assignment, projects, resources, null);
    }
    
    public AssignmentDialog(Assignment assignment, List<Project> projects, List<Resource> resources, SchedulingService schedulingService) {
        // Only in edit mode if assignment has an ID (not a duplicate)
        this.isEditMode = (assignment != null && assignment.getId() != null);
        this.existingAssignment = assignment;
        this.availableProjects = projects;
        this.availableResources = resources;
        this.schedulingService = schedulingService;
        
        setTitle(isEditMode ? "Edit Assignment" : "Create New Assignment");
        setHeaderText(isEditMode ? "Edit assignment details" : "Assign resource to project");
        
        // Initialize filtered projects list
        this.filteredProjects = FXCollections.observableArrayList(projects);
        
        // Create filter fields
        filterStartDatePicker = new DatePicker();
        filterStartDatePicker.setPromptText("Estimated start date");
        filterStartDatePicker.setPrefWidth(150);
        
        // Add date validation to filter date picker as well
        addFlexibleDateParser(filterStartDatePicker);
        
        filterDateRangeSpinner = new Spinner<>(0, 365, 15);
        filterDateRangeSpinner.setEditable(true);
        filterDateRangeSpinner.setPrefWidth(80);
        filterDateRangeSpinner.setTooltip(new Tooltip("±days from start date"));
        
        filterProjectIdField = new TextField();
        filterProjectIdField.setPromptText("Project ID...");
        filterProjectIdField.setPrefWidth(150);
        
        filterDescriptionField = new TextField();
        filterDescriptionField.setPromptText("Description...");
        filterDescriptionField.setPrefWidth(200);
        
        clearFiltersButton = new Button("Clear");
        clearFiltersButton.setOnAction(e -> clearFilters());
        
        // Set up filter listeners
        filterStartDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterDateRangeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterProjectIdField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterDescriptionField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Create form fields
        projectCombo = new ComboBox<>(filteredProjects);
        projectCombo.setConverter(createProjectStringConverter());
        
        resourceCombo = new ComboBox<>(FXCollections.observableArrayList(resources));
        resourceCombo.setConverter(createResourceStringConverter());
        
        startDatePicker = new DatePicker();
        startDatePicker.setValue(LocalDate.now());
        
        endDatePicker = new DatePicker();
        endDatePicker.setValue(LocalDate.now().plusDays(7));
        
        travelOutSpinner = new Spinner<>(0, 10, 0);
        travelOutSpinner.setEditable(true);
        
        travelBackSpinner = new Spinner<>(0, 10, 0);
        travelBackSpinner.setEditable(true);
        
        overrideCheckBox = new CheckBox("Override conflicts");
        overrideCheckBox.setSelected(false);
        
        overrideReasonField = new TextField();
        overrideReasonField.setPromptText("Reason for override (optional)");
        overrideReasonField.setDisable(true);
        
        notesArea = new TextArea();
        notesArea.setPromptText("Assignment notes (optional)");
        notesArea.setPrefRowCount(2);
        
        locationField = new TextField();
        locationField.setPromptText("Location/Phase (optional, e.g., Dallas, Phase 1)");
        
        // Create project manager combo box
        projectManagerCombo = new ComboBox<>();
        projectManagerCombo.setPromptText("Select a project manager");
        projectManagerCombo.setConverter(createProjectManagerStringConverter());
        
        // Load project managers if scheduling service is available
        if (schedulingService != null) {
            List<ProjectManager> managers = schedulingService.getAllProjectManagers();
            projectManagerCombo.setItems(FXCollections.observableArrayList(managers));
        }
        
        // Create edit project button
        editProjectButton = new Button("Edit Client Info");
        editProjectButton.setVisible(false);
        editProjectButton.setManaged(false);
        editProjectButton.setOnAction(e -> editProjectClientInfo());
        
        // Create conflict warning label
        conflictWarningLabel = new Label();
        conflictWarningLabel.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        conflictWarningLabel.setVisible(false);
        conflictWarningLabel.setWrapText(true);
        
        // Create validation feedback label
        validationLabel = new Label();
        validationLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11px;");
        validationLabel.setVisible(false);
        validationLabel.setWrapText(true);
        
        // Set up override reason field behavior
        overrideCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            overrideReasonField.setDisable(!newVal);
            if (!newVal) {
                overrideReasonField.clear();
            }
        });
        
        // Update date picker constraints and PM combo based on project selection
        projectCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (existingAssignment == null) {
                    // Only auto-set dates for NEW assignments, not when editing existing ones
                    startDatePicker.setValue(newVal.getStartDate());
                    endDatePicker.setValue(newVal.getStartDate().plusDays(7));
                }
                updateProjectManagerCombo(newVal);
                editProjectButton.setVisible(true);
                editProjectButton.setManaged(true);
            } else {
                projectManagerCombo.setValue(null);
                editProjectButton.setVisible(false);
                editProjectButton.setManaged(false);
            }
        });
        
        // Add flexible date parsing and validation
        addFlexibleDateParser(startDatePicker);
        addFlexibleDateParser(endDatePicker);
        
        // Ensure end date is not before start date
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && endDatePicker.getValue() != null && newVal.isAfter(endDatePicker.getValue())) {
                endDatePicker.setValue(newVal.plusDays(1));
            }
            checkForConflicts();
            validateDates();
        });
        
        // Check conflicts when dates or resource change
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            checkForConflicts();
            validateDates();
        });
        resourceCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            checkForConflicts();
            updateResourceStyle();
        });
        
        // Populate fields if editing or duplicating
        if (existingAssignment != null) {
            // Find and select project
            Project selectedProject = projects.stream()
                .filter(p -> p.getId().equals(existingAssignment.getProjectId()))
                .findFirst()
                .orElse(null);
            
            if (selectedProject != null) {
                projectCombo.setValue(selectedProject);
                updateProjectManagerCombo(selectedProject);
                editProjectButton.setVisible(true);
                editProjectButton.setManaged(true);
            }
            
            // Find and select resource
            resources.stream()
                .filter(r -> r.getId().equals(existingAssignment.getResourceId()))
                .findFirst()
                .ifPresent(resourceCombo::setValue);
            
            startDatePicker.setValue(existingAssignment.getStartDate());
            endDatePicker.setValue(existingAssignment.getEndDate());
            travelOutSpinner.getValueFactory().setValue(existingAssignment.getTravelOutDays());
            travelBackSpinner.getValueFactory().setValue(existingAssignment.getTravelBackDays());
            overrideCheckBox.setSelected(existingAssignment.isOverride());
            overrideReasonField.setText(existingAssignment.getOverrideReason() != null ? 
                                      existingAssignment.getOverrideReason() : "");
            notesArea.setText(existingAssignment.getNotes() != null ? existingAssignment.getNotes() : "");
            locationField.setText(existingAssignment.getLocation() != null ? existingAssignment.getLocation() : "");
        }
        
        // Create form layout
        GridPane grid = createFormLayout();
        
        // Set up dialog pane
        getDialogPane().setContent(grid);
        
        // Create custom Help button type
        ButtonType helpButtonType = new ButtonType("Help", ButtonBar.ButtonData.HELP);
        getDialogPane().getButtonTypes().addAll(helpButtonType, ButtonType.OK, ButtonType.CANCEL);
        
        // Configure help button
        Button helpBtn = (Button) getDialogPane().lookupButton(helpButtonType);
        helpBtn.setOnAction(e -> {
            HelpButton.showHelpDialog(
                "Assignment Dialog Help",
                "**Resource Assignment**\n\n" +
                "**Project Filters:**\n" +
                "• **Start Date:** Filter projects by estimated start\n" +
                "• **Date Range:** ± days tolerance from start date\n" +
                "• **Project ID:** Filter by project number\n" +
                "• **Description:** Filter by project description\n\n" +
                "**Assignment Details:**\n" +
                "• **Project:** Select target project from dropdown\n" +
                "• **Resource:** Choose available resource/technician\n" +
                "• **Project Manager:** Auto-filled from project\n" +
                "• **Start/End Dates:** Assignment period\n" +
                "• **Travel Days:** Days needed for travel to/from site\n\n" +
                "**Conflict Override:**\n" +
                "• Check to override scheduling conflicts\n" +
                "• Provide reason for override (required)\n" +
                "• System will warn about conflicts:\n" +
                "  - Double booking\n" +
                "  - Travel time conflicts\n" +
                "  - Resource unavailability\n\n" +
                "**Date Entry Tips:**\n" +
                "• Type dates as MM/DD/YYYY or M/D/YY\n" +
                "• Use calendar picker for selection\n" +
                "• Dates auto-validate on entry\n\n" +
                "**Validation:**\n" +
                "• End date must be after start date\n" +
                "• Dates must be within project timeline\n" +
                "• Resource must be available\n" +
                "• Travel days cannot be negative",
                getDialogPane().getScene().getWindow()
            );
            e.consume(); // Prevent dialog from closing
        });
        
        // Enable/disable OK button based on validation
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        
        // Add listeners for validation
        projectCombo.valueProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        resourceCombo.valueProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        overrideCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        overrideReasonField.textProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        
        // Set initial button state AFTER listeners are attached
        okButton.setDisable(!isValidInput());
        
        // Set result converter
        setResultConverter(createResultConverter());
        
        // Set dialog size (increased to accommodate filters and all fields)
        getDialogPane().setPrefWidth(650);
        getDialogPane().setPrefHeight(700);
        getDialogPane().setMinHeight(650);
    }
    
    private void applyFilters() {
        List<Project> filtered = new ArrayList<>(availableProjects);
        
        // Filter by estimated start date range
        if (filterStartDatePicker.getValue() != null) {
            LocalDate centerDate = filterStartDatePicker.getValue();
            int rangeDays = filterDateRangeSpinner.getValue();
            LocalDate startRange = centerDate.minusDays(rangeDays);
            LocalDate endRange = centerDate.plusDays(rangeDays);
            
            filtered = filtered.stream()
                .filter(p -> {
                    // Include projects whose date range overlaps with the filter range
                    return !(p.getEndDate().isBefore(startRange) || p.getStartDate().isAfter(endRange));
                })
                .collect(Collectors.toList());
        }
        
        // Filter by project ID (fuzzy search - case insensitive)
        String projectIdFilter = filterProjectIdField.getText().trim().toLowerCase();
        if (!projectIdFilter.isEmpty()) {
            filtered = filtered.stream()
                .filter(p -> p.getProjectId().toLowerCase().contains(projectIdFilter))
                .collect(Collectors.toList());
        }
        
        // Filter by description (fuzzy search - case insensitive)
        String descriptionFilter = filterDescriptionField.getText().trim().toLowerCase();
        if (!descriptionFilter.isEmpty()) {
            filtered = filtered.stream()
                .filter(p -> p.getDescription().toLowerCase().contains(descriptionFilter))
                .collect(Collectors.toList());
        }
        
        // Update the filtered list and refresh combo box
        filteredProjects.clear();
        filteredProjects.addAll(filtered);
        
        // Update project count label
        HBox projectBox = (HBox) projectCombo.getParent();
        if (projectBox != null && projectBox.getChildren().size() > 1) {
            Label countLabel = (Label) projectBox.getChildren().get(1);
            countLabel.setText("(" + filtered.size() + " projects)");
            
            // Change color based on filter status
            if (filtered.size() < availableProjects.size()) {
                countLabel.setStyle("-fx-text-fill: #1976d2; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                countLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            }
        }
        
        // Preserve current selection if it's still in the filtered list
        Project currentSelection = projectCombo.getValue();
        if (currentSelection != null && !filtered.contains(currentSelection)) {
            projectCombo.setValue(null);
        }
    }
    
    private void clearFilters() {
        filterStartDatePicker.setValue(null);
        filterDateRangeSpinner.getValueFactory().setValue(15);
        filterProjectIdField.clear();
        filterDescriptionField.clear();
        
        // Reset to show all projects
        filteredProjects.clear();
        filteredProjects.addAll(availableProjects);
        
        // Update count label
        HBox projectBox = (HBox) projectCombo.getParent();
        if (projectBox != null && projectBox.getChildren().size() > 1) {
            Label countLabel = (Label) projectBox.getChildren().get(1);
            countLabel.setText("(" + availableProjects.size() + " projects)");
            countLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        }
    }
    
    private GridPane createFormLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // Validation feedback at the very top
        grid.add(validationLabel, 0, row, 2, 1);
        row++;
        
        // Conflict warning below validation
        grid.add(conflictWarningLabel, 0, row, 2, 1);
        row++;
        
        // Add filter section
        VBox filterSection = new VBox(10);
        filterSection.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label filterLabel = new Label("Filter Projects:");
        filterLabel.setStyle("-fx-font-weight: bold;");
        
        HBox dateFilterBox = new HBox(10);
        dateFilterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dateFilterBox.getChildren().addAll(
            new Label("Start Date:"), filterStartDatePicker,
            new Label("±"), filterDateRangeSpinner, new Label("days")
        );
        
        HBox textFilterBox = new HBox(10);
        textFilterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        textFilterBox.getChildren().addAll(
            new Label("Project ID:"), filterProjectIdField,
            new Label("Description:"), filterDescriptionField,
            clearFiltersButton
        );
        
        filterSection.getChildren().addAll(filterLabel, dateFilterBox, textFilterBox);
        grid.add(filterSection, 0, row, 2, 1);
        row++;
        
        // Add some spacing after filters
        grid.add(new Label(""), 0, row);
        row++;
        
        // Edit Client Info button (separate row, right-aligned)
        HBox editButtonBox = new HBox();
        editButtonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        editButtonBox.getChildren().add(editProjectButton);
        grid.add(editButtonBox, 1, row);
        GridPane.setHgrow(editButtonBox, Priority.ALWAYS);
        row++;
        
        // Project
        grid.add(new Label("Project:"), 0, row);
        HBox projectBox = new HBox(10);
        Label projectCountLabel = new Label("(" + filteredProjects.size() + " projects)");
        projectCountLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        projectBox.getChildren().addAll(projectCombo, projectCountLabel);
        HBox.setHgrow(projectCombo, Priority.ALWAYS);
        grid.add(projectBox, 1, row);
        GridPane.setHgrow(projectBox, Priority.ALWAYS);
        row++;
        
        // Project Manager
        grid.add(new Label("Project Manager:"), 0, row);
        grid.add(projectManagerCombo, 1, row);
        GridPane.setHgrow(projectManagerCombo, Priority.ALWAYS);
        row++;
        
        // Resource with skill filter button
        grid.add(new Label("Resource:"), 0, row);
        HBox resourceBox = new HBox(10);
        Button filterBySkillsBtn = new Button("Filter by Skills");
        filterBySkillsBtn.setOnAction(e -> showSkillFilterDialog());
        Button clearResourceFilterBtn = new Button("Clear");
        clearResourceFilterBtn.setVisible(false);
        clearResourceFilterBtn.setManaged(false);
        clearResourceFilterBtn.setOnAction(e -> clearResourceFilter());
        resourceBox.getChildren().addAll(resourceCombo, filterBySkillsBtn, clearResourceFilterBtn);
        HBox.setHgrow(resourceCombo, Priority.ALWAYS);
        grid.add(resourceBox, 1, row);
        GridPane.setHgrow(resourceBox, Priority.ALWAYS);
        row++;
        
        // Start Date
        grid.add(new Label("Start Date:"), 0, row);
        grid.add(startDatePicker, 1, row);
        row++;
        
        // End Date
        grid.add(new Label("End Date:"), 0, row);
        grid.add(endDatePicker, 1, row);
        row++;
        
        // Travel Days
        Label travelLabel = new Label("Travel Days:");
        HBox travelBox = new HBox(10);
        travelBox.getChildren().addAll(
            new Label("Out:"), travelOutSpinner,
            new Label("Back:"), travelBackSpinner
        );
        grid.add(travelLabel, 0, row);
        grid.add(travelBox, 1, row);
        row++;
        
        // Override
        grid.add(new Label("Override:"), 0, row);
        grid.add(overrideCheckBox, 1, row);
        row++;
        
        // Override Reason
        grid.add(new Label("Override Reason:"), 0, row);
        grid.add(overrideReasonField, 1, row);
        GridPane.setHgrow(overrideReasonField, Priority.ALWAYS);
        row++;
        
        // Location/Phase
        grid.add(new Label("Location/Phase:"), 0, row);
        grid.add(locationField, 1, row);
        GridPane.setHgrow(locationField, Priority.ALWAYS);
        row++;
        
        // Notes
        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row);
        GridPane.setHgrow(notesArea, Priority.ALWAYS);
        GridPane.setVgrow(notesArea, Priority.ALWAYS);
        
        return grid;
    }
    
    private boolean isValidInput() {
        // Create validation feedback message
        StringBuilder issues = new StringBuilder();
        boolean valid = true;
        
        // Project and resource required
        if (projectCombo.getValue() == null) {
            issues.append("• Select a project\n");
            valid = false;
        }
        
        if (resourceCombo.getValue() == null) {
            issues.append("• Select a resource\n");
            valid = false;
        }
        
        // Dates required
        if (startDatePicker.getValue() == null) {
            issues.append("• Select a start date\n");
            valid = false;
        }
        
        if (endDatePicker.getValue() == null) {
            issues.append("• Select an end date\n");
            valid = false;
        }
        
        // Start date must be before or equal to end date
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null &&
            startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            issues.append("• Start date must be before or equal to end date\n");
            valid = false;
        }
        
        // Note: We don't validate against project dates because assignments can extend beyond project boundaries
        // This is common for setup/teardown, documentation, or follow-up work
        
        // Update validation feedback label
        if (!valid) {
            validationLabel.setText(issues.toString());
            validationLabel.setVisible(true);
        } else {
            validationLabel.setVisible(false);
        }
        
        // Override reason is optional - users can override without providing a reason
        
        return valid;
    }
    
    private StringConverter<Project> createProjectStringConverter() {
        return new StringConverter<Project>() {
            @Override
            public String toString(Project project) {
                return project != null ? project.getProjectId() + " - " + project.getDescription() : "";
            }
            
            @Override
            public Project fromString(String string) {
                return filteredProjects.stream()
                    .filter(p -> (p.getProjectId() + " - " + p.getDescription()).equals(string))
                    .findFirst()
                    .orElse(null);
            }
        };
    }
    
    private StringConverter<Resource> createResourceStringConverter() {
        return new StringConverter<Resource>() {
            @Override
            public String toString(Resource resource) {
                if (resource != null) {
                    String type = resource.getResourceType() != null ? 
                                 resource.getResourceType().getName() : "Unknown";
                    return resource.getName() + " (" + type + ")";
                }
                return "";
            }
            
            @Override
            public Resource fromString(String string) {
                return availableResources.stream()
                    .filter(r -> {
                        String type = r.getResourceType() != null ? 
                                     r.getResourceType().getName() : "Unknown";
                        return (r.getName() + " (" + type + ")").equals(string);
                    })
                    .findFirst()
                    .orElse(null);
            }
        };
    }
    
    private Callback<ButtonType, Assignment> createResultConverter() {
        return dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    Assignment assignment;
                    
                    if (isEditMode && existingAssignment != null) {
                        // Update existing assignment
                        assignment = existingAssignment;
                        assignment.setProjectId(projectCombo.getValue().getId());
                        assignment.setResourceId(resourceCombo.getValue().getId());
                        assignment.setStartDate(startDatePicker.getValue());
                        assignment.setEndDate(endDatePicker.getValue());
                        assignment.setTravelOutDays(travelOutSpinner.getValue());
                        assignment.setTravelBackDays(travelBackSpinner.getValue());
                        assignment.setOverride(overrideCheckBox.isSelected());
                        assignment.setOverrideReason(overrideCheckBox.isSelected() ? 
                                                   overrideReasonField.getText().trim() : null);
                        assignment.setNotes(notesArea.getText().trim().isEmpty() ? 
                                          null : notesArea.getText().trim());
                        assignment.setLocation(locationField.getText().trim().isEmpty() ? 
                                             null : locationField.getText().trim());
                        
                        // Update project's PM if changed
                        if (projectManagerCombo.getValue() != null && projectCombo.getValue() != null) {
                            Project project = projectCombo.getValue();
                            project.setProjectManagerId(projectManagerCombo.getValue().getId());
                            if (schedulingService != null) {
                                try {
                                    schedulingService.updateProject(project);
                                } catch (Exception e) {
                                    logger.error("Failed to update project manager", e);
                                }
                            }
                        }
                    } else {
                        // Create new assignment
                        assignment = new Assignment(
                            projectCombo.getValue().getId(),
                            resourceCombo.getValue().getId(),
                            startDatePicker.getValue(),
                            endDatePicker.getValue(),
                            travelOutSpinner.getValue(),
                            travelBackSpinner.getValue()
                        );
                        assignment.setOverride(overrideCheckBox.isSelected());
                        assignment.setOverrideReason(overrideCheckBox.isSelected() ? 
                                                   overrideReasonField.getText().trim() : null);
                        assignment.setNotes(notesArea.getText().trim().isEmpty() ? 
                                          null : notesArea.getText().trim());
                        assignment.setLocation(locationField.getText().trim().isEmpty() ? 
                                             null : locationField.getText().trim());
                    }
                    
                    return assignment;
                    
                } catch (Exception e) {
                    logger.error("Error creating assignment from dialog", e);
                    
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to create assignment");
                    errorAlert.setContentText("Please check your input and try again.");
                    errorAlert.showAndWait();
                    
                    return null;
                }
            }
            return null;
        };
    }
    
    private void editProjectClientInfo() {
        Project selectedProject = projectCombo.getValue();
        if (selectedProject == null) {
            return;
        }
        
        // Get the scheduling service to fetch project managers
        List<ProjectManager> projectManagers = null;
        if (schedulingService != null) {
            projectManagers = schedulingService.getActiveProjectManagers();
        }
        
        // Create and show the project dialog
        ProjectDialog dialog = new ProjectDialog(selectedProject, 
                                                 schedulingService != null ? schedulingService.getProjectRepository() : null, 
                                                 projectManagers);
        DialogUtils.initializeDialog(dialog, this.getDialogPane().getScene().getWindow());
        Optional<Project> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            Project updatedProject = result.get();
            
            // Update the project in the combo box
            int index = availableProjects.indexOf(selectedProject);
            if (index >= 0) {
                availableProjects.set(index, updatedProject);
                projectCombo.setItems(FXCollections.observableArrayList(availableProjects));
                projectCombo.setValue(updatedProject);
            }
            
            // Save the updated project if we have a scheduling service
            if (schedulingService != null) {
                try {
                    schedulingService.updateProject(updatedProject);
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Project client information updated successfully!");
                    alert.showAndWait();
                } catch (Exception e) {
                    logger.error("Failed to update project", e);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to update project");
                    alert.setContentText("An error occurred while saving the project: " + e.getMessage());
                    alert.showAndWait();
                }
            }
        }
    }
    
    private void checkForConflicts() {
        if (schedulingService == null || resourceCombo.getValue() == null || 
            startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            conflictWarningLabel.setVisible(false);
            return;
        }
        
        Resource selectedResource = resourceCombo.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        // Check for conflicts with this resource's existing assignments
        List<Assignment> existingAssignments = schedulingService.getAssignmentsByResource(selectedResource.getId());
        
        boolean hasConflict = false;
        StringBuilder conflictDetails = new StringBuilder();
        
        for (Assignment existing : existingAssignments) {
            // Skip checking against itself if editing
            if (isEditMode && existingAssignment != null && 
                existing.getId().equals(existingAssignment.getId())) {
                continue;
            }
            
            // Check for date overlap
            if (!(endDate.isBefore(existing.getStartDate()) || startDate.isAfter(existing.getEndDate()))) {
                hasConflict = true;
                Project conflictProject = availableProjects.stream()
                    .filter(p -> p.getId().equals(existing.getProjectId()))
                    .findFirst()
                    .orElse(null);
                    
                if (conflictProject != null) {
                    if (conflictDetails.length() > 0) {
                        conflictDetails.append(", ");
                    }
                    conflictDetails.append(conflictProject.getProjectId())
                                  .append(" (")
                                  .append(existing.getStartDate())
                                  .append(" to ")
                                  .append(existing.getEndDate())
                                  .append(")");
                }
            }
        }
        
        if (hasConflict) {
            conflictWarningLabel.setText("⚠️ Conflict: " + selectedResource.getName() + 
                                        " is already assigned to: " + conflictDetails.toString());
            conflictWarningLabel.setVisible(true);
            
            // Enable override checkbox automatically when conflict detected
            overrideCheckBox.setDisable(false);
        } else {
            conflictWarningLabel.setVisible(false);
            overrideCheckBox.setSelected(false);
            overrideCheckBox.setDisable(true);
        }
    }
    
    private void updateProjectManagerCombo(Project project) {
        if (project == null || schedulingService == null) {
            projectManagerCombo.setValue(null);
            return;
        }
        
        try {
            List<ProjectManager> managers = schedulingService.getAllProjectManagers();
            ProjectManager projectManager = managers.stream()
                .filter(pm -> pm.getId().equals(project.getProjectManagerId()))
                .findFirst()
                .orElse(null);
            
            projectManagerCombo.setValue(projectManager);
        } catch (Exception e) {
            logger.warn("Could not load project manager for project: " + project.getProjectId(), e);
            projectManagerCombo.setValue(null);
        }
    }
    
    private StringConverter<ProjectManager> createProjectManagerStringConverter() {
        return new StringConverter<ProjectManager>() {
            @Override
            public String toString(ProjectManager pm) {
                return pm != null ? pm.getName() : "";
            }
            
            @Override
            public ProjectManager fromString(String string) {
                if (projectManagerCombo.getItems() == null) {
                    return null;
                }
                return projectManagerCombo.getItems().stream()
                    .filter(pm -> pm.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        };
    }
    
    private void updateResourceStyle() {
        if (resourceCombo.getValue() == null) {
            return;
        }
        
        // Update the ComboBox cell factory to show conflicted resources
        resourceCombo.setCellFactory(new Callback<ListView<Resource>, ListCell<Resource>>() {
            @Override
            public ListCell<Resource> call(ListView<Resource> param) {
                return new ListCell<Resource>() {
                    @Override
                    protected void updateItem(Resource resource, boolean empty) {
                        super.updateItem(resource, empty);
                        if (empty || resource == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            String typeText = resource.getResourceType() != null ? 
                                resource.getResourceType().getName() : "Unknown";
                            setText(resource.getName() + " (" + typeText + ")");
                            
                            // Check if this resource has conflicts with current dates
                            if (schedulingService != null && startDatePicker.getValue() != null && 
                                endDatePicker.getValue() != null) {
                                List<Assignment> assignments = schedulingService.getAssignmentsByResource(resource.getId());
                                boolean hasConflict = false;
                                
                                for (Assignment a : assignments) {
                                    // Skip self in edit mode
                                    if (isEditMode && existingAssignment != null && 
                                        a.getId().equals(existingAssignment.getId())) {
                                        continue;
                                    }
                                    
                                    if (!(endDatePicker.getValue().isBefore(a.getStartDate()) || 
                                          startDatePicker.getValue().isAfter(a.getEndDate()))) {
                                        hasConflict = true;
                                        break;
                                    }
                                }
                                
                                if (hasConflict) {
                                    setStyle("-fx-text-fill: #c62828; -fx-background-color: #ffebee;");
                                    setText(resource.getName() + " (CONFLICT)");
                                } else {
                                    setStyle("");
                                }
                            }
                        }
                    }
                };
            }
        });
    }
    
    private void addFlexibleDateParser(DatePicker picker) {
        picker.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String text = picker.getEditor().getText();
                if (text != null && !text.trim().isEmpty()) {
                    try {
                        // Try to parse with multiple formats
                        LocalDate date = parseFlexibleDate(text);
                        if (date != null) {
                            picker.setValue(date);
                            picker.getEditor().setStyle("");
                        } else {
                            // Show error styling for invalid date
                            picker.getEditor().setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
                            validationLabel.setText("Invalid date format. Use MM/DD/YYYY or YYYY-MM-DD");
                            validationLabel.setVisible(true);
                        }
                    } catch (Exception e) {
                        // Invalid date format
                        picker.getEditor().setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
                        validationLabel.setText("Invalid date format. Use MM/DD/YYYY or YYYY-MM-DD");
                        validationLabel.setVisible(true);
                    }
                } else {
                    picker.getEditor().setStyle("");
                }
            }
        });
        
        // Clear error styling when user starts typing again
        picker.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (picker.getEditor().isFocused()) {
                picker.getEditor().setStyle("");
            }
        });
    }
    
    private LocalDate parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        dateStr = dateStr.trim();
        
        // Common date formats to try
        String[] patterns = {
            "M/d/yyyy", "MM/dd/yyyy", "M-d-yyyy", "MM-dd-yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd", "d/M/yyyy", "dd/MM/yyyy",
            "M/d/yy", "MM/dd/yy"  // Also support 2-digit years
        };
        
        for (String pattern : patterns) {
            try {
                LocalDate date = LocalDate.parse(dateStr, 
                    java.time.format.DateTimeFormatter.ofPattern(pattern));
                
                // If 2-digit year, adjust to 2000s or 1900s
                if (pattern.endsWith("yy") && date.getYear() < 100) {
                    int year = date.getYear();
                    if (year < 50) {
                        date = date.plusYears(2000);
                    } else {
                        date = date.plusYears(1900);
                    }
                }
                
                return date;
            } catch (Exception e) {
                // Try next pattern
            }
        }
        
        // Try smart parsing for common formats
        if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] parts = dateStr.split("/");
            try {
                int month = Integer.parseInt(parts[0]);
                int day = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                // Invalid date components
            }
        }
        
        return null;
    }
    
    private void validateDates() {
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            ValidationResult result = InputValidator.validateDateRange(
                startDatePicker.getValue(),
                endDatePicker.getValue()
            );
            
            if (!result.isValid()) {
                validationLabel.setText(result.getErrorMessage());
                validationLabel.setVisible(true);
                startDatePicker.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
                endDatePicker.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
            } else {
                // Clear date validation error if dates are valid
                if (!conflictWarningLabel.isVisible()) {
                    validationLabel.setVisible(false);
                }
                startDatePicker.setStyle("");
                endDatePicker.setStyle("");
            }
        }
    }
    
    private void showSkillFilterDialog() {
        Project selectedProject = projectCombo.getValue();
        if (selectedProject == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Select Project First");
            alert.setHeaderText(null);
            alert.setContentText("Please select a project first to filter resources by required skills.");
            alert.showAndWait();
            return;
        }
        
        // Show the skill filter dialog
        ResourceSkillFilterDialog filterDialog = new ResourceSkillFilterDialog(
            getDialogPane().getScene().getWindow(),
            availableResources
        );
        
        List<Resource> filteredResources = filterDialog.showAndWait();
        if (filteredResources != null && !filteredResources.isEmpty()) {
            
            // Update the resource combo box with filtered resources
            Resource currentSelection = resourceCombo.getValue();
            resourceCombo.setItems(FXCollections.observableArrayList(filteredResources));
            
            // Restore selection if still in filtered list
            if (currentSelection != null && filteredResources.contains(currentSelection)) {
                resourceCombo.setValue(currentSelection);
            } else if (filteredResources.size() == 1) {
                // Auto-select if only one resource matches
                resourceCombo.setValue(filteredResources.get(0));
            }
            
            // Update the button text to show filter is active
            HBox resourceBox = (HBox) resourceCombo.getParent();
            if (resourceBox != null && resourceBox.getChildren().size() >= 3) {
                Button filterBtn = (Button) resourceBox.getChildren().get(1);
                filterBtn.setText("Filter by Skills (" + filteredResources.size() + ")");
                filterBtn.setStyle("-fx-base: #4caf50;");
                
                // Show clear button
                Button clearBtn = (Button) resourceBox.getChildren().get(2);
                clearBtn.setVisible(true);
                clearBtn.setManaged(true);
            }
        }
    }
    
    private void clearResourceFilter() {
        // Restore all resources
        resourceCombo.setItems(FXCollections.observableArrayList(availableResources));
        
        // Update buttons
        HBox resourceBox = (HBox) resourceCombo.getParent();
        if (resourceBox != null && resourceBox.getChildren().size() >= 3) {
            Button filterBtn = (Button) resourceBox.getChildren().get(1);
            filterBtn.setText("Filter by Skills");
            filterBtn.setStyle("");
            
            // Hide clear button
            Button clearBtn = (Button) resourceBox.getChildren().get(2);
            clearBtn.setVisible(false);
            clearBtn.setManaged(false);
        }
    }
    
    /**
     * Show a confirmation dialog before deleting an assignment
     */
    public static boolean showDeleteConfirmation(Assignment assignment, Project project, Resource resource) {
        return showDeleteConfirmation(assignment, project, resource, null);
    }
    
    /**
     * Show a confirmation dialog before deleting an assignment with owner window
     */
    public static boolean showDeleteConfirmation(Assignment assignment, Project project, Resource resource, Window owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Assignment");
        alert.setHeaderText("Delete assignment");
        
        String projectName = project != null ? project.getProjectId() : "Unknown Project";
        String resourceName = resource != null ? resource.getName() : "Unknown Resource";
        
        alert.setContentText("Are you sure you want to delete this assignment?\n\n" +
                            "Project: " + projectName + "\n" +
                            "Resource: " + resourceName + "\n" +
                            "Dates: " + assignment.getStartDate() + " to " + assignment.getEndDate() + "\n\n" +
                            "This action cannot be undone.");
        
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}