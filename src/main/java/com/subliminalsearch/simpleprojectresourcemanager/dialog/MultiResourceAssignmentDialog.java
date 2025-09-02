package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MultiResourceAssignmentDialog extends Dialog<List<Assignment>> {
    private static final Logger logger = LoggerFactory.getLogger(MultiResourceAssignmentDialog.class);
    
    private final ComboBox<Project> projectCombo;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final Spinner<Integer> travelOutSpinner;
    private final Spinner<Integer> travelBackSpinner;
    private final CheckBox overrideConflictsCheckBox;
    private final TextField overrideReasonField;
    private final TextArea notesArea;
    
    // Filter fields
    private final DatePicker filterStartDatePicker;
    private final Spinner<Integer> filterDateRangeSpinner;
    private final TextField filterProjectIdField;
    private final TextField filterDescriptionField;
    private final Button clearFiltersButton;
    
    private final TableView<ResourceSelection> resourceTable;
    private final ObservableList<ResourceSelection> resourceSelections;
    private final Label conflictSummaryLabel;
    private final TextArea conflictDetailsArea;
    
    private final List<Project> availableProjects;
    private final List<Resource> availableResources;
    private ObservableList<Project> filteredProjects;
    private final SchedulingService schedulingService;
    private final Map<Long, List<String>> resourceConflicts = new HashMap<>();
    
    public MultiResourceAssignmentDialog(List<Project> projects, List<Resource> resources, 
                                        SchedulingService schedulingService) {
        this.availableProjects = projects;
        this.availableResources = resources;
        this.schedulingService = schedulingService;
        
        setTitle("Multi-Resource Assignment");
        setHeaderText("Assign multiple resources to a project");
        
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
        
        // Initialize form fields
        projectCombo = new ComboBox<>(filteredProjects);
        projectCombo.setConverter(createProjectStringConverter());
        projectCombo.setPrefWidth(400);
        
        startDatePicker = new DatePicker(LocalDate.now());
        endDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        
        travelOutSpinner = new Spinner<>(0, 10, 1);
        travelOutSpinner.setEditable(true);
        travelOutSpinner.setPrefWidth(80);
        
        travelBackSpinner = new Spinner<>(0, 10, 1);
        travelBackSpinner.setEditable(true);
        travelBackSpinner.setPrefWidth(80);
        
        overrideConflictsCheckBox = new CheckBox("Override conflicts for all resources");
        overrideReasonField = new TextField();
        overrideReasonField.setPromptText("Reason for override (required if conflicts exist)");
        overrideReasonField.setDisable(true);
        
        notesArea = new TextArea();
        notesArea.setPromptText("Assignment notes (optional)");
        notesArea.setPrefRowCount(2);
        
        // Initialize resource selection table
        resourceSelections = FXCollections.observableArrayList();
        resourceTable = createResourceTable();
        
        // Initialize conflict display
        conflictSummaryLabel = new Label("No conflicts detected");
        conflictSummaryLabel.setStyle("-fx-font-weight: bold;");
        conflictDetailsArea = new TextArea();
        conflictDetailsArea.setEditable(false);
        conflictDetailsArea.setPrefRowCount(4);
        conflictDetailsArea.setVisible(false);
        conflictDetailsArea.setManaged(false);
        
        // Set up listeners
        setupListeners();
        
        // Load resources into table
        loadResources();
        
        // Create layout
        VBox content = createLayout();
        getDialogPane().setContent(content);
        
        // Add buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Configure OK button
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Create Assignments");
        okButton.setDisable(true);
        
        // Enable/disable OK button based on validation
        projectCombo.valueProperty().addListener((obs, old, val) -> validateAndUpdateUI());
        startDatePicker.valueProperty().addListener((obs, old, val) -> validateAndUpdateUI());
        endDatePicker.valueProperty().addListener((obs, old, val) -> validateAndUpdateUI());
        
        // Set result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return createAssignments();
            }
            return null;
        });
        
        // Set dialog size
        getDialogPane().setPrefWidth(900);
        getDialogPane().setPrefHeight(700);
        getDialogPane().setMinHeight(600);
    }
    
    private TableView<ResourceSelection> createResourceTable() {
        TableView<ResourceSelection> table = new TableView<>();
        
        // Selection checkbox column
        TableColumn<ResourceSelection, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setPrefWidth(60);
        selectCol.setEditable(true);
        
        // Resource name column
        TableColumn<ResourceSelection, String> nameCol = new TableColumn<>("Resource");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(200);
        
        // Resource type column
        TableColumn<ResourceSelection, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        typeCol.setPrefWidth(150);
        
        // Availability status column
        TableColumn<ResourceSelection, String> availabilityCol = new TableColumn<>("Availability");
        availabilityCol.setCellValueFactory(cellData -> cellData.getValue().availabilityProperty());
        availabilityCol.setPrefWidth(250);
        availabilityCol.setCellFactory(column -> new TableCell<ResourceSelection, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("CONFLICT")) {
                        setTextFill(Color.RED);
                        setStyle("-fx-font-weight: bold;");
                    } else if (item.equals("Available")) {
                        setTextFill(Color.GREEN);
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.ORANGE);
                        setStyle("");
                    }
                }
            }
        });
        
        // Conflict details column
        TableColumn<ResourceSelection, String> conflictCol = new TableColumn<>("Conflict Details");
        conflictCol.setCellValueFactory(cellData -> cellData.getValue().conflictDetailsProperty());
        conflictCol.setPrefWidth(280);
        
        table.getColumns().addAll(selectCol, nameCol, typeCol, availabilityCol, conflictCol);
        table.setItems(resourceSelections);
        table.setEditable(true);
        
        // Enable row selection highlighting
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        return table;
    }
    
    private void loadResources() {
        resourceSelections.clear();
        for (Resource resource : availableResources) {
            resourceSelections.add(new ResourceSelection(resource));
        }
    }
    
    private void setupListeners() {
        // Update conflicts when dates change
        startDatePicker.valueProperty().addListener((obs, old, val) -> checkAllConflicts());
        endDatePicker.valueProperty().addListener((obs, old, val) -> checkAllConflicts());
        projectCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                startDatePicker.setValue(val.getStartDate());
                endDatePicker.setValue(val.getStartDate().plusDays(7));
            }
            checkAllConflicts();
        });
        
        // Enable override reason field when checkbox is selected
        overrideConflictsCheckBox.selectedProperty().addListener((obs, old, val) -> {
            overrideReasonField.setDisable(!val);
            if (!val) {
                overrideReasonField.clear();
            }
        });
        
        // Check conflicts when resources are selected/deselected
        for (ResourceSelection selection : resourceSelections) {
            selection.selectedProperty().addListener((obs, old, val) -> {
                if (val) {
                    checkConflictsForResource(selection);
                }
                updateConflictSummary();
                validateAndUpdateUI();
            });
        }
    }
    
    private void checkAllConflicts() {
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            return;
        }
        
        resourceConflicts.clear();
        for (ResourceSelection selection : resourceSelections) {
            checkConflictsForResource(selection);
        }
        updateConflictSummary();
    }
    
    private void checkConflictsForResource(ResourceSelection selection) {
        if (schedulingService == null || startDatePicker.getValue() == null || 
            endDatePicker.getValue() == null) {
            return;
        }
        
        Resource resource = selection.getResource();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        // Get existing assignments for this resource
        List<Assignment> existingAssignments = schedulingService.getAssignmentsByResource(resource.getId());
        List<String> conflicts = new ArrayList<>();
        
        for (Assignment existing : existingAssignments) {
            // Check for date overlap
            if (!(endDate.isBefore(existing.getStartDate()) || startDate.isAfter(existing.getEndDate()))) {
                Project conflictProject = availableProjects.stream()
                    .filter(p -> p.getId().equals(existing.getProjectId()))
                    .findFirst()
                    .orElse(null);
                
                if (conflictProject != null) {
                    conflicts.add(String.format("%s (%s to %s)",
                        conflictProject.getProjectId(),
                        existing.getStartDate().format(DateTimeFormatter.ofPattern("MM/dd")),
                        existing.getEndDate().format(DateTimeFormatter.ofPattern("MM/dd"))));
                }
            }
        }
        
        // Update the selection with conflict information
        if (!conflicts.isEmpty()) {
            resourceConflicts.put(resource.getId(), conflicts);
            selection.setAvailability("CONFLICT");
            selection.setConflictDetails(String.join(", ", conflicts));
        } else {
            resourceConflicts.remove(resource.getId());
            selection.setAvailability("Available");
            selection.setConflictDetails("");
        }
    }
    
    private void updateConflictSummary() {
        long selectedCount = resourceSelections.stream()
            .filter(ResourceSelection::isSelected)
            .count();
        
        long conflictCount = resourceSelections.stream()
            .filter(ResourceSelection::isSelected)
            .filter(sel -> resourceConflicts.containsKey(sel.getResource().getId()))
            .count();
        
        if (selectedCount == 0) {
            conflictSummaryLabel.setText("No resources selected");
            conflictSummaryLabel.setTextFill(Color.BLACK);
            conflictDetailsArea.setVisible(false);
            conflictDetailsArea.setManaged(false);
        } else if (conflictCount == 0) {
            conflictSummaryLabel.setText(String.format("✓ %d resources selected - No conflicts", selectedCount));
            conflictSummaryLabel.setTextFill(Color.GREEN);
            conflictDetailsArea.setVisible(false);
            conflictDetailsArea.setManaged(false);
        } else {
            conflictSummaryLabel.setText(String.format("⚠ %d of %d selected resources have conflicts", 
                                                      conflictCount, selectedCount));
            conflictSummaryLabel.setTextFill(Color.DARKORANGE);
            
            // Show conflict details
            StringBuilder details = new StringBuilder();
            for (ResourceSelection sel : resourceSelections) {
                if (sel.isSelected() && resourceConflicts.containsKey(sel.getResource().getId())) {
                    details.append("• ").append(sel.getName()).append(": ")
                          .append(sel.getConflictDetails()).append("\n");
                }
            }
            conflictDetailsArea.setText(details.toString());
            conflictDetailsArea.setVisible(true);
            conflictDetailsArea.setManaged(true);
        }
    }
    
    private void validateAndUpdateUI() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        
        boolean hasSelectedResources = resourceSelections.stream().anyMatch(ResourceSelection::isSelected);
        boolean hasValidDates = startDatePicker.getValue() != null && 
                               endDatePicker.getValue() != null &&
                               !startDatePicker.getValue().isAfter(endDatePicker.getValue());
        boolean hasProject = projectCombo.getValue() != null;
        
        // Check if we have conflicts and need override reason
        boolean hasConflicts = resourceSelections.stream()
            .filter(ResourceSelection::isSelected)
            .anyMatch(sel -> resourceConflicts.containsKey(sel.getResource().getId()));
        
        boolean canProceed = hasSelectedResources && hasValidDates && hasProject;
        
        if (hasConflicts && !overrideConflictsCheckBox.isSelected()) {
            canProceed = false;
        }
        
        if (hasConflicts && overrideConflictsCheckBox.isSelected() && 
            overrideReasonField.getText().trim().isEmpty()) {
            canProceed = false;
        }
        
        okButton.setDisable(!canProceed);
        
        // Update button text based on selection count
        long selectedCount = resourceSelections.stream()
            .filter(ResourceSelection::isSelected)
            .count();
        okButton.setText(selectedCount > 0 ? 
            String.format("Create %d Assignment%s", selectedCount, selectedCount > 1 ? "s" : "") : 
            "Create Assignments");
    }
    
    private VBox createLayout() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        
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
        
        // Project selection
        GridPane projectGrid = new GridPane();
        projectGrid.setHgap(10);
        projectGrid.setVgap(10);
        
        Label projectLabelText = new Label("Project:");
        HBox projectBox = new HBox(10);
        Label projectCountLabel = new Label("(" + filteredProjects.size() + " projects)");
        projectCountLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        projectBox.getChildren().addAll(projectCombo, projectCountLabel);
        HBox.setHgrow(projectCombo, Priority.ALWAYS);
        
        projectGrid.add(projectLabelText, 0, 0);
        projectGrid.add(projectBox, 1, 0);
        GridPane.setHgrow(projectBox, Priority.ALWAYS);
        
        // Date selection
        HBox dateBox = new HBox(10);
        dateBox.getChildren().addAll(
            new Label("Start Date:"), startDatePicker,
            new Label("End Date:"), endDatePicker
        );
        projectGrid.add(dateBox, 0, 1, 2, 1);
        
        // Travel days
        HBox travelBox = new HBox(10);
        travelBox.getChildren().addAll(
            new Label("Travel Days - Out:"), travelOutSpinner,
            new Label("Back:"), travelBackSpinner
        );
        projectGrid.add(travelBox, 0, 2, 2, 1);
        
        // Resource selection section
        Label resourceLabel = new Label("Select Resources:");
        resourceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Quick selection buttons
        HBox quickSelectBox = new HBox(10);
        Button selectAllBtn = new Button("Select All");
        Button selectNoneBtn = new Button("Select None");
        Button selectAvailableBtn = new Button("Select Available Only");
        
        selectAllBtn.setOnAction(e -> {
            resourceSelections.forEach(sel -> sel.setSelected(true));
            validateAndUpdateUI();
        });
        
        selectNoneBtn.setOnAction(e -> {
            resourceSelections.forEach(sel -> sel.setSelected(false));
            validateAndUpdateUI();
        });
        
        selectAvailableBtn.setOnAction(e -> {
            resourceSelections.forEach(sel -> 
                sel.setSelected(!resourceConflicts.containsKey(sel.getResource().getId())));
            validateAndUpdateUI();
        });
        
        quickSelectBox.getChildren().addAll(selectAllBtn, selectNoneBtn, selectAvailableBtn);
        
        // Conflict summary section
        VBox conflictBox = new VBox(5);
        conflictBox.getChildren().addAll(conflictSummaryLabel, conflictDetailsArea);
        conflictBox.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10; " +
                           "-fx-border-color: #ddd; -fx-border-radius: 5;");
        
        // Override section
        VBox overrideBox = new VBox(5);
        overrideBox.getChildren().addAll(overrideConflictsCheckBox, overrideReasonField);
        
        // Notes section
        Label notesLabel = new Label("Notes:");
        
        // Add all sections to layout
        layout.getChildren().addAll(
            filterSection,
            new Separator(),
            projectGrid,
            new Separator(),
            resourceLabel,
            quickSelectBox,
            resourceTable,
            conflictBox,
            overrideBox,
            notesLabel,
            notesArea
        );
        
        VBox.setVgrow(resourceTable, Priority.ALWAYS);
        
        return layout;
    }
    
    private List<Assignment> createAssignments() {
        List<Assignment> assignments = new ArrayList<>();
        Project project = projectCombo.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        int travelOut = travelOutSpinner.getValue();
        int travelBack = travelBackSpinner.getValue();
        String notes = notesArea.getText().trim();
        boolean override = overrideConflictsCheckBox.isSelected();
        String overrideReason = overrideReasonField.getText().trim();
        
        for (ResourceSelection selection : resourceSelections) {
            if (selection.isSelected()) {
                Assignment assignment = new Assignment(
                    project.getId(),
                    selection.getResource().getId(),
                    startDate,
                    endDate,
                    travelOut,
                    travelBack
                );
                
                if (!notes.isEmpty()) {
                    assignment.setNotes(notes);
                }
                
                if (override && resourceConflicts.containsKey(selection.getResource().getId())) {
                    assignment.setOverride(true);
                    assignment.setOverrideReason(overrideReason);
                }
                
                assignments.add(assignment);
            }
        }
        
        return assignments;
    }
    
    private StringConverter<Project> createProjectStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Project project) {
                return project != null ? 
                    project.getProjectId() + " - " + project.getDescription() : "";
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
    
    // Inner class for resource selection tracking
    public static class ResourceSelection {
        private final Resource resource;
        private final BooleanProperty selected;
        private final StringProperty name;
        private final StringProperty type;
        private final StringProperty availability;
        private final StringProperty conflictDetails;
        
        public ResourceSelection(Resource resource) {
            this.resource = resource;
            this.selected = new SimpleBooleanProperty(false);
            this.name = new SimpleStringProperty(resource.getName());
            this.type = new SimpleStringProperty(
                resource.getResourceType() != null ? resource.getResourceType().getName() : "Unknown");
            this.availability = new SimpleStringProperty("Available");
            this.conflictDetails = new SimpleStringProperty("");
        }
        
        public Resource getResource() { return resource; }
        
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean value) { selected.set(value); }
        public BooleanProperty selectedProperty() { return selected; }
        
        public String getName() { return name.get(); }
        public StringProperty nameProperty() { return name; }
        
        public String getType() { return type.get(); }
        public StringProperty typeProperty() { return type; }
        
        public String getAvailability() { return availability.get(); }
        public void setAvailability(String value) { availability.set(value); }
        public StringProperty availabilityProperty() { return availability; }
        
        public String getConflictDetails() { return conflictDetails.get(); }
        public void setConflictDetails(String value) { conflictDetails.set(value); }
        public StringProperty conflictDetailsProperty() { return conflictDetails; }
    }
}