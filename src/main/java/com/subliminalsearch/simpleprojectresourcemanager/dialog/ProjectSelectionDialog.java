package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectSelectionDialog extends Dialog<Project> {
    private static final Logger logger = LoggerFactory.getLogger(ProjectSelectionDialog.class);
    
    private final ComboBox<Project> projectCombo;
    
    // Filter fields
    private final DatePicker filterStartDatePicker;
    private final Spinner<Integer> filterDateRangeSpinner;
    private final TextField filterProjectIdField;
    private final TextField filterDescriptionField;
    private final Button clearFiltersButton;
    
    private final List<Project> availableProjects;
    private ObservableList<Project> filteredProjects;
    
    public ProjectSelectionDialog(List<Project> projects) {
        this(projects, "Select Project", "Select a project to continue");
    }
    
    public ProjectSelectionDialog(List<Project> projects, String title, String headerText) {
        this.availableProjects = projects;
        
        setTitle(title);
        setHeaderText(headerText);
        
        // Initialize filtered projects list
        this.filteredProjects = FXCollections.observableArrayList(projects);
        
        // Create filter fields
        filterStartDatePicker = new DatePicker();
        filterStartDatePicker.setPromptText("Estimated start date");
        filterStartDatePicker.setPrefWidth(150);
        
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
        
        // Create project combo box
        projectCombo = new ComboBox<>(filteredProjects);
        projectCombo.setConverter(createProjectStringConverter());
        projectCombo.setPrefWidth(450);
        projectCombo.setMaxWidth(Double.MAX_VALUE);
        
        // Select first project by default if available
        if (!filteredProjects.isEmpty()) {
            projectCombo.setValue(filteredProjects.get(0));
        }
        
        // Create form layout
        GridPane grid = createFormLayout();
        
        // Set up dialog pane
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Enable/disable OK button based on selection
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(projectCombo.getValue() == null);
        
        // Add listener for validation
        projectCombo.valueProperty().addListener((obs, oldVal, newVal) -> 
            okButton.setDisable(newVal == null));
        
        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return projectCombo.getValue();
            }
            return null;
        });
        
        // Set dialog size
        getDialogPane().setPrefWidth(550);
        getDialogPane().setPrefHeight(400);
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
        } else if (currentSelection == null && !filtered.isEmpty()) {
            // Auto-select first item if nothing selected
            projectCombo.setValue(filtered.get(0));
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
        
        // Select first project if available
        if (!filteredProjects.isEmpty()) {
            projectCombo.setValue(filteredProjects.get(0));
        }
    }
    
    private GridPane createFormLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
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
        
        // Project selection
        grid.add(new Label("Select Project:"), 0, row);
        HBox projectBox = new HBox(10);
        Label projectCountLabel = new Label("(" + filteredProjects.size() + " projects)");
        projectCountLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        projectBox.getChildren().addAll(projectCombo, projectCountLabel);
        HBox.setHgrow(projectCombo, Priority.ALWAYS);
        grid.add(projectBox, 1, row);
        GridPane.setHgrow(projectBox, Priority.ALWAYS);
        row++;
        
        // Add project details section
        VBox detailsSection = new VBox(5);
        detailsSection.setPadding(new Insets(10));
        detailsSection.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 3;");
        
        Label detailsLabel = new Label("Project Details:");
        detailsLabel.setStyle("-fx-font-weight: bold;");
        
        Label projectDetailsLabel = new Label("Select a project to view details");
        projectDetailsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        projectDetailsLabel.setWrapText(true);
        
        detailsSection.getChildren().addAll(detailsLabel, projectDetailsLabel);
        
        // Update details when project is selected
        projectCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String details = String.format(
                    "Project: %s\nDescription: %s\nDates: %s to %s\nStatus: %s",
                    newVal.getProjectId(),
                    newVal.getDescription(),
                    newVal.getStartDate(),
                    newVal.getEndDate(),
                    newVal.getStatus()
                );
                projectDetailsLabel.setText(details);
                projectDetailsLabel.setStyle("-fx-font-size: 11px;");
            } else {
                projectDetailsLabel.setText("Select a project to view details");
                projectDetailsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
            }
        });
        
        grid.add(detailsSection, 0, row, 2, 1);
        GridPane.setHgrow(detailsSection, Priority.ALWAYS);
        
        return grid;
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
}