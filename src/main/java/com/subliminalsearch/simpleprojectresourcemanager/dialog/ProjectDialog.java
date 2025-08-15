package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.service.ProjectNumberGenerator;
import com.subliminalsearch.simpleprojectresourcemanager.util.InputValidator;
import com.subliminalsearch.simpleprojectresourcemanager.util.InputValidator.ValidationResult;
import com.subliminalsearch.simpleprojectresourcemanager.util.InputValidator.ValidationType;
import com.subliminalsearch.simpleprojectresourcemanager.util.PhoneFormatter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectDialog extends Dialog<Project> {
    private static final Logger logger = LoggerFactory.getLogger(ProjectDialog.class);
    
    private final TextField projectIdField;
    private final TextField descriptionField;
    private final ComboBox<ProjectManager> projectManagerCombo;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final ComboBox<ProjectStatus> statusCombo;
    private final CheckBox autoGenerateCheckBox;
    private final Button generateButton;
    
    // Client contact fields
    private final TextField contactNameField;
    private final TextField contactEmailField;
    private final TextField contactPhoneField;
    private final TextField contactCompanyField;
    private final TextField contactRoleField;
    private final CheckBox sendReportsCheckBox;
    private final ComboBox<String> reportFrequencyCombo;
    
    // Validation error labels
    private final Label projectIdErrorLabel;
    private final Label emailErrorLabel;
    private final Label phoneErrorLabel;
    private final Label dateRangeErrorLabel;
    
    private final boolean isEditMode;
    private final Project existingProject;
    private ProjectNumberGenerator projectNumberGenerator;
    private List<ProjectManager> projectManagers;
    
    public ProjectDialog() {
        this(null, null, null);
    }
    
    public ProjectDialog(Project project) {
        this(project, null, null);
    }
    
    public ProjectDialog(ProjectRepository projectRepository) {
        this(null, projectRepository, null);
    }
    
    public ProjectDialog(Project project, ProjectRepository projectRepository, List<ProjectManager> projectManagers) {
        this.isEditMode = (project != null);
        this.existingProject = project;
        this.projectManagers = projectManagers;
        
        // Initialize project number generator if repository is provided
        if (projectRepository != null) {
            this.projectNumberGenerator = new ProjectNumberGenerator(projectRepository);
        }
        
        setTitle(isEditMode ? "Edit Project" : "Create New Project");
        setHeaderText(isEditMode ? "Edit project details" : "Enter project details");
        
        // Create form fields
        projectIdField = new TextField();
        projectIdField.setPromptText("e.g., PRJ-2025-0001");
        
        // Auto-generate checkbox and button for new projects
        autoGenerateCheckBox = new CheckBox("Auto-generate project number");
        generateButton = new Button("Generate");
        
        descriptionField = new TextField();
        descriptionField.setPromptText("Project description");
        
        // Project Manager combo box
        projectManagerCombo = new ComboBox<>();
        if (projectManagers != null && !projectManagers.isEmpty()) {
            projectManagerCombo.setItems(FXCollections.observableArrayList(projectManagers));
            // Find and set "Unassigned" as default
            projectManagers.stream()
                .filter(pm -> "Unassigned".equals(pm.getName()))
                .findFirst()
                .ifPresent(projectManagerCombo::setValue);
        }
        projectManagerCombo.setConverter(new javafx.util.StringConverter<ProjectManager>() {
            @Override
            public String toString(ProjectManager pm) {
                return pm != null ? pm.getName() : "";
            }
            
            @Override
            public ProjectManager fromString(String string) {
                return projectManagers != null ? projectManagers.stream()
                    .filter(pm -> pm.getName().equals(string))
                    .findFirst()
                    .orElse(null) : null;
            }
        });
        
        startDatePicker = new DatePicker();
        startDatePicker.setValue(LocalDate.now());
        
        endDatePicker = new DatePicker();
        endDatePicker.setValue(LocalDate.now().plusDays(30));
        
        statusCombo = new ComboBox<>(FXCollections.observableArrayList(ProjectStatus.values()));
        statusCombo.setValue(ProjectStatus.ACTIVE);
        
        // Initialize client contact fields
        contactNameField = new TextField();
        contactNameField.setPromptText("Client contact name");
        
        contactEmailField = new TextField();
        contactEmailField.setPromptText("email1@example.com; email2@example.com");
        
        contactPhoneField = new TextField();
        PhoneFormatter.applyPhoneFormat(contactPhoneField);
        
        contactCompanyField = new TextField();
        contactCompanyField.setPromptText("Company name");
        
        contactRoleField = new TextField();
        contactRoleField.setPromptText("e.g., Project Manager, Owner");
        
        sendReportsCheckBox = new CheckBox("Send automated reports");
        sendReportsCheckBox.setSelected(true);
        
        reportFrequencyCombo = new ComboBox<>(FXCollections.observableArrayList(
            "DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY"
        ));
        reportFrequencyCombo.setValue("WEEKLY");
        
        // Initialize error labels
        projectIdErrorLabel = new Label();
        projectIdErrorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11px;");
        projectIdErrorLabel.setVisible(false);
        projectIdErrorLabel.setManaged(false);
        
        emailErrorLabel = new Label();
        emailErrorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11px;");
        emailErrorLabel.setVisible(false);
        emailErrorLabel.setManaged(false);
        
        phoneErrorLabel = new Label();
        phoneErrorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11px;");
        phoneErrorLabel.setVisible(false);
        phoneErrorLabel.setManaged(false);
        
        dateRangeErrorLabel = new Label();
        dateRangeErrorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11px;");
        dateRangeErrorLabel.setVisible(false);
        dateRangeErrorLabel.setManaged(false);
        
        // Add real-time validation listeners
        setupValidation();
        
        // Populate fields if editing
        if (isEditMode && existingProject != null) {
            projectIdField.setText(existingProject.getProjectId());
            descriptionField.setText(existingProject.getDescription());
            startDatePicker.setValue(existingProject.getStartDate());
            endDatePicker.setValue(existingProject.getEndDate());
            statusCombo.setValue(existingProject.getStatus());
            
            // Set project manager if available
            if (existingProject.getProjectManagerId() != null && projectManagers != null) {
                projectManagers.stream()
                    .filter(pm -> pm.getId().equals(existingProject.getProjectManagerId()))
                    .findFirst()
                    .ifPresent(projectManagerCombo::setValue);
            }
            
            // Populate client contact fields
            if (existingProject.getContactName() != null) {
                contactNameField.setText(existingProject.getContactName());
            }
            if (existingProject.getContactEmail() != null) {
                contactEmailField.setText(existingProject.getContactEmail());
            }
            if (existingProject.getContactPhone() != null) {
                PhoneFormatter.setPhoneNumber(contactPhoneField, existingProject.getContactPhone());
            }
            if (existingProject.getContactCompany() != null) {
                contactCompanyField.setText(existingProject.getContactCompany());
            }
            if (existingProject.getContactRole() != null) {
                contactRoleField.setText(existingProject.getContactRole());
            }
            sendReportsCheckBox.setSelected(existingProject.isSendReports());
            if (existingProject.getReportFrequency() != null) {
                reportFrequencyCombo.setValue(existingProject.getReportFrequency());
            }
            
            // Disable project ID field in edit mode
            projectIdField.setDisable(true);
        }
        
        // Create form layout with tabs
        TabPane tabPane = createTabbedLayout();
        
        // Set up dialog pane
        getDialogPane().setContent(tabPane);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Enable/disable OK button based on validation
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(!isValidInput());
        
        // Add listeners for validation
        projectIdField.textProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(!isValidInput()));
        
        // Set result converter
        setResultConverter(createResultConverter());
        
        // Set dialog size
        getDialogPane().setPrefWidth(600);
        getDialogPane().setPrefHeight(500);
    }
    
    private void setupValidation() {
        // Project ID validation
        projectIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isEditMode && newVal != null && !newVal.trim().isEmpty()) {
                ValidationResult result = InputValidator.validateProjectId(newVal);
                updateFieldValidation(projectIdField, projectIdErrorLabel, result);
            }
        });
        
        // Email validation - allow multiple emails separated by semicolons
        contactEmailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                String[] emails = newVal.split(";");
                boolean allValid = true;
                String errorMsg = null;
                
                for (String email : emails) {
                    email = email.trim();
                    if (!email.isEmpty()) {
                        ValidationResult result = InputValidator.validateEmail(email);
                        if (!result.isValid()) {
                            allValid = false;
                            errorMsg = result.getErrorMessage();
                            break;
                        }
                    }
                }
                
                ValidationResult finalResult = new ValidationResult(allValid, errorMsg);
                updateFieldValidation(contactEmailField, emailErrorLabel, finalResult);
            } else {
                emailErrorLabel.setVisible(false);
                emailErrorLabel.setManaged(false);
                contactEmailField.setStyle("");
            }
        });
        
        // Phone validation - validate using unformatted phone
        contactPhoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                String unformattedPhone = PhoneFormatter.getUnformattedPhone(contactPhoneField);
                ValidationResult result = InputValidator.validatePhone(unformattedPhone);
                updateFieldValidation(contactPhoneField, phoneErrorLabel, result);
            } else {
                phoneErrorLabel.setVisible(false);
                phoneErrorLabel.setManaged(false);
                contactPhoneField.setStyle("");
            }
        });
        
        // Date range validation
        Runnable validateDates = () -> {
            ValidationResult result = InputValidator.validateDateRange(
                startDatePicker.getValue(), 
                endDatePicker.getValue()
            );
            updateDatePickerValidation(result);
        };
        
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates.run());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates.run());
        
        // Add flexible date parsing
        addFlexibleDateParser(startDatePicker);
        addFlexibleDateParser(endDatePicker);
    }
    
    private void updateFieldValidation(TextField field, Label errorLabel, ValidationResult result) {
        if (!result.isValid() && result.getErrorMessage() != null) {
            errorLabel.setText(result.getErrorMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            field.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
        } else {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            field.setStyle("");
        }
    }
    
    private void updateDatePickerValidation(ValidationResult result) {
        if (!result.isValid() && result.getErrorMessage() != null) {
            dateRangeErrorLabel.setText(result.getErrorMessage());
            dateRangeErrorLabel.setVisible(true);
            dateRangeErrorLabel.setManaged(true);
            startDatePicker.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
            endDatePicker.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
        } else {
            dateRangeErrorLabel.setVisible(false);
            dateRangeErrorLabel.setManaged(false);
            startDatePicker.setStyle("");
            endDatePicker.setStyle("");
        }
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
                        }
                    } catch (Exception e) {
                        // Invalid date format - picker will handle it
                    }
                }
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
            "yyyy-MM-dd", "yyyy/MM/dd", "d/M/yyyy", "dd/MM/yyyy"
        };
        
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr, 
                    java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                // Try next pattern
            }
        }
        
        return null;
    }
    
    private TabPane createTabbedLayout() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Project Details tab
        Tab projectTab = new Tab("Project Details");
        projectTab.setContent(createProjectDetailsGrid());
        
        // Client Contact tab
        Tab contactTab = new Tab("Client Contact");
        contactTab.setContent(createClientContactGrid());
        
        tabPane.getTabs().addAll(projectTab, contactTab);
        return tabPane;
    }
    
    private GridPane createProjectDetailsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // Project ID
        grid.add(new Label("Project ID:"), 0, row);
        VBox projectIdContainer = new VBox(3);
        HBox projectIdBox = new HBox(5);
        projectIdBox.getChildren().addAll(projectIdField, generateButton);
        HBox.setHgrow(projectIdField, Priority.ALWAYS);
        projectIdContainer.getChildren().addAll(projectIdBox, projectIdErrorLabel);
        grid.add(projectIdContainer, 1, row);
        GridPane.setHgrow(projectIdContainer, Priority.ALWAYS);
        row++;
        
        // Auto-generate checkbox (only for new projects)
        if (!isEditMode) {
            grid.add(autoGenerateCheckBox, 1, row++);
        }
        
        // Description
        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionField, 1, row);
        GridPane.setHgrow(descriptionField, Priority.ALWAYS);
        row++;
        
        // Project Manager
        grid.add(new Label("Project Manager:"), 0, row);
        grid.add(projectManagerCombo, 1, row);
        GridPane.setHgrow(projectManagerCombo, Priority.ALWAYS);
        row++;
        
        // Start Date
        grid.add(new Label("Start Date:"), 0, row);
        grid.add(startDatePicker, 1, row);
        row++;
        
        // End Date
        grid.add(new Label("End Date:"), 0, row);
        VBox endDateContainer = new VBox(3);
        endDateContainer.getChildren().addAll(endDatePicker, dateRangeErrorLabel);
        grid.add(endDateContainer, 1, row);
        row++;
        
        // Status
        grid.add(new Label("Status:"), 0, row);
        grid.add(statusCombo, 1, row);
        row++;
        
        return grid;
    }
    
    private GridPane createClientContactGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // Contact Name
        grid.add(new Label("Contact Name:"), 0, row);
        grid.add(contactNameField, 1, row);
        GridPane.setHgrow(contactNameField, Priority.ALWAYS);
        row++;
        
        // Contact Role
        grid.add(new Label("Role/Title:"), 0, row);
        grid.add(contactRoleField, 1, row);
        GridPane.setHgrow(contactRoleField, Priority.ALWAYS);
        row++;
        
        // Company
        grid.add(new Label("Company:"), 0, row);
        grid.add(contactCompanyField, 1, row);
        GridPane.setHgrow(contactCompanyField, Priority.ALWAYS);
        row++;
        
        // Email (with note about multiple emails)
        grid.add(new Label("Email Address(es):"), 0, row);
        VBox emailBox = new VBox(5);
        Label emailHint = new Label("Separate multiple emails with semicolons");
        emailHint.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        emailBox.getChildren().addAll(
            contactEmailField,
            emailHint,
            emailErrorLabel
        );
        grid.add(emailBox, 1, row);
        GridPane.setHgrow(emailBox, Priority.ALWAYS);
        row++;
        
        // Phone
        grid.add(new Label("Phone:"), 0, row);
        VBox phoneBox = new VBox(3);
        phoneBox.getChildren().addAll(contactPhoneField, phoneErrorLabel);
        grid.add(phoneBox, 1, row);
        GridPane.setHgrow(phoneBox, Priority.ALWAYS);
        row++;
        
        // Separator
        Separator separator = new Separator();
        grid.add(separator, 0, row++, 2, 1);
        
        // Report Settings
        grid.add(new Label("Report Settings:"), 0, row++, 2, 1);
        
        // Send Reports checkbox
        grid.add(sendReportsCheckBox, 1, row++);
        
        // Report Frequency
        grid.add(new Label("Report Frequency:"), 0, row);
        grid.add(reportFrequencyCombo, 1, row);
        reportFrequencyCombo.setMaxWidth(Double.MAX_VALUE);
        row++;
        
        // Bind report frequency to checkbox
        reportFrequencyCombo.disableProperty().bind(sendReportsCheckBox.selectedProperty().not());
        
        return grid;
    }
    
    private boolean isValidInput() {
        boolean valid = true;
        
        // Project ID validation (unless editing)
        if (!isEditMode) {
            String projectId = projectIdField.getText();
            if (projectId == null || projectId.trim().isEmpty()) {
                return false;
            }
            ValidationResult projectIdResult = InputValidator.validateProjectId(projectId);
            if (!projectIdResult.isValid()) {
                return false;
            }
        }
        
        // Description required
        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) {
            return false;
        }
        
        // Date validation
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            return false;
        }
        
        ValidationResult dateResult = InputValidator.validateDateRange(
            startDatePicker.getValue(), 
            endDatePicker.getValue()
        );
        if (!dateResult.isValid()) {
            return false;
        }
        
        // Email validation (if provided)
        String email = contactEmailField.getText();
        if (email != null && !email.trim().isEmpty()) {
            String[] emails = email.split(";");
            for (String e : emails) {
                e = e.trim();
                if (!e.isEmpty()) {
                    ValidationResult emailResult = InputValidator.validateEmail(e);
                    if (!emailResult.isValid()) {
                        return false;
                    }
                }
            }
        }
        
        // Phone validation (if provided)
        String phone = PhoneFormatter.getUnformattedPhone(contactPhoneField);
        if (phone != null && !phone.isEmpty()) {
            ValidationResult phoneResult = InputValidator.validatePhone(phone);
            if (!phoneResult.isValid()) {
                return false;
            }
        }
        
        return valid;
    }
    
    private Callback<ButtonType, Project> createResultConverter() {
        return dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    Project project;
                    
                    if (isEditMode && existingProject != null) {
                        // Update existing project
                        project = existingProject;
                        project.setDescription(descriptionField.getText().trim());
                        project.setStartDate(startDatePicker.getValue());
                        project.setEndDate(endDatePicker.getValue());
                        project.setStatus(statusCombo.getValue());
                        
                        // Set project manager ID
                        if (projectManagerCombo.getValue() != null) {
                            project.setProjectManagerId(projectManagerCombo.getValue().getId());
                        }
                        
                        // Update client contact information
                        project.setContactName(contactNameField.getText().trim());
                        project.setContactEmail(contactEmailField.getText().trim());
                        project.setContactPhone(PhoneFormatter.getUnformattedPhone(contactPhoneField));
                        project.setContactCompany(contactCompanyField.getText().trim());
                        project.setContactRole(contactRoleField.getText().trim());
                        project.setSendReports(sendReportsCheckBox.isSelected());
                        project.setReportFrequency(reportFrequencyCombo.getValue());
                    } else {
                        // Create new project
                        project = new Project(
                            projectIdField.getText().trim(),
                            descriptionField.getText().trim(),
                            startDatePicker.getValue(),
                            endDatePicker.getValue()
                        );
                        project.setStatus(statusCombo.getValue());
                        
                        // Set project manager ID
                        if (projectManagerCombo.getValue() != null) {
                            project.setProjectManagerId(projectManagerCombo.getValue().getId());
                        }
                        
                        // Set client contact information for new project
                        project.setContactName(contactNameField.getText().trim());
                        project.setContactEmail(contactEmailField.getText().trim());
                        project.setContactPhone(PhoneFormatter.getUnformattedPhone(contactPhoneField));
                        project.setContactCompany(contactCompanyField.getText().trim());
                        project.setContactRole(contactRoleField.getText().trim());
                        project.setSendReports(sendReportsCheckBox.isSelected());
                        project.setReportFrequency(reportFrequencyCombo.getValue());
                    }
                    
                    return project;
                    
                } catch (Exception e) {
                    logger.error("Error creating project from dialog", e);
                    
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to create project");
                    errorAlert.setContentText("Please check your input and try again.");
                    errorAlert.showAndWait();
                    
                    return null;
                }
            }
            return null;
        };
    }
    
    /**
     * Show a confirmation dialog before deleting a project
     */
    public static boolean showDeleteConfirmation(Project project) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Project");
        alert.setHeaderText("Delete project: " + project.getProjectId());
        alert.setContentText("Are you sure you want to delete this project?\n\n" +
                            "Project: " + project.getDescription() + "\n" +
                            "This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Show project details in a read-only dialog
     */
    public static void showProjectDetails(Project project) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Project Details");
        dialog.setHeaderText(project.getProjectId() + " - " + project.getDescription());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        content.getChildren().addAll(
            createDetailRow("Project ID:", project.getProjectId()),
            createDetailRow("Description:", project.getDescription()),
            createDetailRow("Start Date:", project.getStartDate().toString()),
            createDetailRow("End Date:", project.getEndDate().toString()),
            createDetailRow("Status:", project.getStatus().toString()),
            createDetailRow("Created:", project.getCreatedAt().toString())
        );
        
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(400);
        
        dialog.showAndWait();
    }
    
    private static HBox createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        
        Label labelComponent = new Label(label);
        labelComponent.setStyle("-fx-font-weight: bold;");
        labelComponent.setPrefWidth(100);
        
        Label valueComponent = new Label(value);
        
        row.getChildren().addAll(labelComponent, valueComponent);
        return row;
    }
}