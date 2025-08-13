package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AssignmentDialog extends Dialog<Assignment> {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentDialog.class);
    
    private final ComboBox<Project> projectCombo;
    private final ComboBox<Resource> resourceCombo;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final Spinner<Integer> travelOutSpinner;
    private final Spinner<Integer> travelBackSpinner;
    private final CheckBox overrideCheckBox;
    private final TextField overrideReasonField;
    private final TextArea notesArea;
    private final Label projectManagerLabel;
    private final Button editProjectButton;
    
    private final boolean isEditMode;
    private final Assignment existingAssignment;
    private final List<Project> availableProjects;
    private final List<Resource> availableResources;
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
        
        // Create form fields
        projectCombo = new ComboBox<>(FXCollections.observableArrayList(projects));
        projectCombo.setConverter(createProjectStringConverter());
        
        resourceCombo = new ComboBox<>(FXCollections.observableArrayList(resources));
        resourceCombo.setConverter(createResourceStringConverter());
        
        startDatePicker = new DatePicker();
        startDatePicker.setValue(LocalDate.now());
        
        endDatePicker = new DatePicker();
        endDatePicker.setValue(LocalDate.now().plusDays(7));
        
        travelOutSpinner = new Spinner<>(0, 10, 1);
        travelOutSpinner.setEditable(true);
        
        travelBackSpinner = new Spinner<>(0, 10, 1);
        travelBackSpinner.setEditable(true);
        
        overrideCheckBox = new CheckBox("Override conflicts");
        overrideCheckBox.setSelected(false);
        
        overrideReasonField = new TextField();
        overrideReasonField.setPromptText("Reason for override (optional)");
        overrideReasonField.setDisable(true);
        
        notesArea = new TextArea();
        notesArea.setPromptText("Assignment notes (optional)");
        notesArea.setPrefRowCount(2);
        
        // Create project manager label
        projectManagerLabel = new Label("(Select a project to see PM)");
        projectManagerLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
        
        // Create edit project button
        editProjectButton = new Button("Edit Client Info");
        editProjectButton.setDisable(true);
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
        
        // Update date picker constraints and PM label based on project selection
        projectCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (existingAssignment == null) {
                    // Only auto-set dates for NEW assignments, not when editing existing ones
                    startDatePicker.setValue(newVal.getStartDate());
                    endDatePicker.setValue(newVal.getStartDate().plusDays(7));
                }
                updateProjectManagerLabel(newVal);
                editProjectButton.setDisable(false);
            } else {
                projectManagerLabel.setText("(Select a project to see PM)");
                projectManagerLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
                editProjectButton.setDisable(true);
            }
        });
        
        // Ensure end date is not before start date
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && endDatePicker.getValue() != null && newVal.isAfter(endDatePicker.getValue())) {
                endDatePicker.setValue(newVal.plusDays(1));
            }
            checkForConflicts();
        });
        
        // Check conflicts when dates or resource change
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> checkForConflicts());
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
                updateProjectManagerLabel(selectedProject);
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
        }
        
        // Create form layout
        GridPane grid = createFormLayout();
        
        // Set up dialog pane
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
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
        
        // Set dialog size
        getDialogPane().setPrefWidth(550);
        getDialogPane().setPrefHeight(500);
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
        
        // Project
        grid.add(new Label("Project:"), 0, row);
        HBox projectBox = new HBox(10);
        projectBox.getChildren().addAll(projectCombo, editProjectButton);
        HBox.setHgrow(projectCombo, Priority.ALWAYS);
        grid.add(projectBox, 1, row);
        GridPane.setHgrow(projectBox, Priority.ALWAYS);
        row++;
        
        // Project Manager (display only)
        grid.add(new Label("Project Manager:"), 0, row);
        grid.add(projectManagerLabel, 1, row);
        row++;
        
        // Resource
        grid.add(new Label("Resource:"), 0, row);
        grid.add(resourceCombo, 1, row);
        GridPane.setHgrow(resourceCombo, Priority.ALWAYS);
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
                return availableProjects.stream()
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
    
    private void updateProjectManagerLabel(Project project) {
        if (project == null || schedulingService == null) {
            projectManagerLabel.setText("(No project selected)");
            projectManagerLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
            return;
        }
        
        try {
            List<ProjectManager> managers = schedulingService.getAllProjectManagers();
            ProjectManager projectManager = managers.stream()
                .filter(pm -> pm.getId().equals(project.getProjectManagerId()))
                .findFirst()
                .orElse(null);
            
            if (projectManager != null) {
                projectManagerLabel.setText(projectManager.getName());
                projectManagerLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold;");
            } else {
                projectManagerLabel.setText("Unassigned");
                projectManagerLabel.setStyle("-fx-text-fill: #999999; -fx-font-style: italic;");
            }
        } catch (Exception e) {
            logger.warn("Could not load project manager for project: " + project.getProjectId(), e);
            projectManagerLabel.setText("(Unable to load)");
            projectManagerLabel.setStyle("-fx-text-fill: #cc0000; -fx-font-style: italic;");
        }
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
    
    /**
     * Show a confirmation dialog before deleting an assignment
     */
    public static boolean showDeleteConfirmation(Assignment assignment, Project project, Resource resource) {
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
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}