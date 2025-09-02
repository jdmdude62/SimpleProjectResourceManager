package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BatchDeleteProjectsDialog extends Dialog<ButtonType> {
    private final SchedulingService schedulingService;
    private TableView<ProjectRow> projectTable;
    private ObservableList<ProjectRow> projectRows;
    private Label selectionLabel;
    private DatePicker startDateFilter;
    private DatePicker endDateFilter;
    private ComboBox<ProjectStatus> statusFilter;
    private TextField searchField;
    
    public BatchDeleteProjectsDialog(SchedulingService schedulingService, Window owner) {
        this.schedulingService = schedulingService;
        
        setTitle("Batch Delete Projects");
        setHeaderText("Select projects to delete (useful for cleaning up test imports)");
        initOwner(owner);
        
        // Create content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(900);
        content.setPrefHeight(600);
        
        // Filter controls
        HBox filterBox = createFilterControls();
        
        // Create table
        projectTable = createProjectTable();
        VBox.setVgrow(projectTable, Priority.ALWAYS);
        
        // Selection controls
        HBox selectionBox = createSelectionControls();
        
        // Selection info
        selectionLabel = new Label("0 projects selected for deletion");
        selectionLabel.setStyle("-fx-font-weight: bold;");
        
        // Warning
        Label warningLabel = new Label("⚠️ Warning: Deleted projects cannot be recovered. All associated assignments will also be deleted.");
        warningLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        warningLabel.setWrapText(true);
        
        // Add components
        content.getChildren().addAll(
            filterBox,
            new Separator(),
            projectTable,
            selectionBox,
            selectionLabel,
            new Separator(),
            warningLabel
        );
        
        // Set content
        getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType deleteButton = new ButtonType("Delete Selected", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(deleteButton, ButtonType.CANCEL);
        
        // Enable delete button only when projects are selected
        Button deleteBtn = (Button) getDialogPane().lookupButton(deleteButton);
        deleteBtn.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
        deleteBtn.setDisable(true);
        
        // Load projects
        loadProjects();
        
        // Setup listeners
        projectRows.forEach(row -> row.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateSelectionLabel();
            deleteBtn.setDisable(getSelectedProjects().isEmpty());
        }));
        
        // Handle delete
        setResultConverter(buttonType -> {
            if (buttonType == deleteButton) {
                performDelete();
            }
            return buttonType;
        });
    }
    
    private HBox createFilterControls() {
        HBox box = new HBox(10);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label filterLabel = new Label("Filters:");
        filterLabel.setStyle("-fx-font-weight: bold;");
        
        // Date range
        Label startLabel = new Label("From:");
        startDateFilter = new DatePicker();
        startDateFilter.setPrefWidth(120);
        startDateFilter.setPromptText("Start date");
        
        Label endLabel = new Label("To:");
        endDateFilter = new DatePicker();
        endDateFilter.setPrefWidth(120);
        endDateFilter.setPromptText("End date");
        
        // Status filter
        Label statusLabel = new Label("Status:");
        statusFilter = new ComboBox<>();
        statusFilter.getItems().add(null); // All statuses
        statusFilter.getItems().addAll(ProjectStatus.values());
        statusFilter.setPromptText("All Statuses");
        statusFilter.setPrefWidth(120);
        
        // Search
        Label searchLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("Project ID or description...");
        searchField.setPrefWidth(200);
        
        Button applyButton = new Button("Apply Filters");
        applyButton.setOnAction(e -> applyFilters());
        
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> {
            startDateFilter.setValue(null);
            endDateFilter.setValue(null);
            statusFilter.setValue(null);
            searchField.clear();
            loadProjects();
        });
        
        box.getChildren().addAll(
            filterLabel,
            startLabel, startDateFilter,
            endLabel, endDateFilter,
            statusLabel, statusFilter,
            searchLabel, searchField,
            applyButton, clearButton
        );
        
        return box;
    }
    
    private TableView<ProjectRow> createProjectTable() {
        TableView<ProjectRow> table = new TableView<>();
        table.setEditable(true);
        
        // Select column
        TableColumn<ProjectRow, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setPrefWidth(60);
        selectCol.setEditable(true);
        
        // Project ID column
        TableColumn<ProjectRow, String> idCol = new TableColumn<>("Project ID");
        idCol.setCellValueFactory(cellData -> cellData.getValue().projectIdProperty());
        idCol.setPrefWidth(100);
        
        // Description column
        TableColumn<ProjectRow, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        descCol.setPrefWidth(250);
        
        // Status column
        TableColumn<ProjectRow, ProjectStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(80);
        
        // Start Date column
        TableColumn<ProjectRow, LocalDate> startCol = new TableColumn<>("Start Date");
        startCol.setCellValueFactory(cellData -> cellData.getValue().startDateProperty());
        startCol.setPrefWidth(100);
        startCol.setCellFactory(column -> new TableCell<ProjectRow, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
            }
        });
        
        // End Date column
        TableColumn<ProjectRow, LocalDate> endCol = new TableColumn<>("End Date");
        endCol.setCellValueFactory(cellData -> cellData.getValue().endDateProperty());
        endCol.setPrefWidth(100);
        endCol.setCellFactory(column -> new TableCell<ProjectRow, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
            }
        });
        
        // Assignments count column
        TableColumn<ProjectRow, Integer> assignCol = new TableColumn<>("Assignments");
        assignCol.setCellValueFactory(cellData -> cellData.getValue().assignmentCountProperty().asObject());
        assignCol.setPrefWidth(100);
        assignCol.setStyle("-fx-alignment: CENTER;");
        
        table.getColumns().addAll(selectCol, idCol, descCol, statusCol, startCol, endCol, assignCol);
        
        projectRows = FXCollections.observableArrayList();
        table.setItems(projectRows);
        
        return table;
    }
    
    private HBox createSelectionControls() {
        HBox box = new HBox(10);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Button selectAllButton = new Button("Select All");
        selectAllButton.setOnAction(e -> {
            projectRows.forEach(row -> row.setSelected(true));
        });
        
        Button selectNoneButton = new Button("Select None");
        selectNoneButton.setOnAction(e -> {
            projectRows.forEach(row -> row.setSelected(false));
        });
        
        Button selectUnassignedButton = new Button("Select Unassigned");
        selectUnassignedButton.setOnAction(e -> {
            projectRows.forEach(row -> row.setSelected(row.getAssignmentCount() == 0));
        });
        
        box.getChildren().addAll(selectAllButton, selectNoneButton, selectUnassignedButton);
        
        return box;
    }
    
    private void loadProjects() {
        projectRows.clear();
        
        List<Project> projects = schedulingService.getAllProjects();
        
        for (Project project : projects) {
            List<Assignment> assignments = schedulingService.getAssignmentsByProjectId(project.getId());
            ProjectRow row = new ProjectRow(project, assignments.size());
            
            // Add listener for selection changes
            row.selectedProperty().addListener((obs, oldVal, newVal) -> {
                updateSelectionLabel();
                Button deleteBtn = (Button) getDialogPane().lookupButton(
                    getDialogPane().getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                        .findFirst().orElse(null));
                if (deleteBtn != null) {
                    deleteBtn.setDisable(getSelectedProjects().isEmpty());
                }
            });
            
            projectRows.add(row);
        }
        
        updateSelectionLabel();
    }
    
    private void applyFilters() {
        projectRows.clear();
        
        List<Project> projects = schedulingService.getAllProjects();
        LocalDate startFilter = startDateFilter.getValue();
        LocalDate endFilter = endDateFilter.getValue();
        ProjectStatus statusFilterValue = statusFilter.getValue();
        String searchText = searchField.getText().toLowerCase();
        
        for (Project project : projects) {
            // Apply filters
            if (startFilter != null && project.getEndDate() != null && 
                project.getEndDate().isBefore(startFilter)) {
                continue;
            }
            
            if (endFilter != null && project.getStartDate() != null && 
                project.getStartDate().isAfter(endFilter)) {
                continue;
            }
            
            if (statusFilterValue != null && project.getStatus() != statusFilterValue) {
                continue;
            }
            
            if (!searchText.isEmpty()) {
                String projectId = project.getProjectId().toLowerCase();
                String description = project.getDescription() != null ? 
                    project.getDescription().toLowerCase() : "";
                    
                if (!projectId.contains(searchText) && !description.contains(searchText)) {
                    continue;
                }
            }
            
            List<Assignment> assignments = schedulingService.getAssignmentsByProjectId(project.getId());
            ProjectRow row = new ProjectRow(project, assignments.size());
            
            // Add listener for selection changes
            row.selectedProperty().addListener((obs, oldVal, newVal) -> {
                updateSelectionLabel();
                Button deleteBtn = (Button) getDialogPane().lookupButton(
                    getDialogPane().getButtonTypes().stream()
                        .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                        .findFirst().orElse(null));
                if (deleteBtn != null) {
                    deleteBtn.setDisable(getSelectedProjects().isEmpty());
                }
            });
            
            projectRows.add(row);
        }
        
        updateSelectionLabel();
    }
    
    private void updateSelectionLabel() {
        List<ProjectRow> selected = getSelectedProjects();
        int totalAssignments = selected.stream()
            .mapToInt(ProjectRow::getAssignmentCount)
            .sum();
            
        String text = String.format("%d projects selected for deletion", selected.size());
        if (totalAssignments > 0) {
            text += String.format(" (affecting %d assignments)", totalAssignments);
        }
        selectionLabel.setText(text);
    }
    
    private List<ProjectRow> getSelectedProjects() {
        return projectRows.stream()
            .filter(ProjectRow::isSelected)
            .collect(Collectors.toList());
    }
    
    private void performDelete() {
        List<ProjectRow> selected = getSelectedProjects();
        if (selected.isEmpty()) {
            return;
        }
        
        // Confirm deletion
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Selected Projects");
        
        int totalAssignments = selected.stream()
            .mapToInt(ProjectRow::getAssignmentCount)
            .sum();
            
        String content = String.format(
            "You are about to delete %d projects.\n" +
            "This will also delete %d assignments.\n\n" +
            "This action cannot be undone. Continue?",
            selected.size(), totalAssignments
        );
        confirm.setContentText(content);
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        
        // Delete projects
        int deleted = 0;
        int errors = 0;
        
        for (ProjectRow row : selected) {
            try {
                // Use deleteProjectWithAssignments to delete projects even if they have assignments
                schedulingService.deleteProjectWithAssignments(row.getProject().getId());
                deleted++;
            } catch (Exception e) {
                errors++;
                System.err.println("Failed to delete project " + 
                    row.getProject().getProjectId() + ": " + e.getMessage());
            }
        }
        
        // Show result
        Alert resultAlert = new Alert(errors > 0 ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
        resultAlert.setTitle("Deletion Complete");
        resultAlert.setHeaderText(null);
        
        if (errors > 0) {
            resultAlert.setContentText(String.format(
                "Deleted %d projects successfully.\n%d projects failed to delete.",
                deleted, errors
            ));
        } else {
            resultAlert.setContentText(String.format("Successfully deleted %d projects.", deleted));
        }
        
        resultAlert.showAndWait();
    }
    
    // Inner class for table rows
    public static class ProjectRow {
        private final Project project;
        private final SimpleBooleanProperty selected;
        private final SimpleStringProperty projectId;
        private final SimpleStringProperty description;
        private final SimpleObjectProperty<ProjectStatus> status;
        private final SimpleObjectProperty<LocalDate> startDate;
        private final SimpleObjectProperty<LocalDate> endDate;
        private final SimpleIntegerProperty assignmentCount;
        
        public ProjectRow(Project project, int assignmentCount) {
            this.project = project;
            this.selected = new SimpleBooleanProperty(false);
            this.projectId = new SimpleStringProperty(project.getProjectId());
            this.description = new SimpleStringProperty(project.getDescription());
            this.status = new SimpleObjectProperty<>(project.getStatus());
            this.startDate = new SimpleObjectProperty<>(project.getStartDate());
            this.endDate = new SimpleObjectProperty<>(project.getEndDate());
            this.assignmentCount = new SimpleIntegerProperty(assignmentCount);
        }
        
        public Project getProject() { return project; }
        
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean value) { selected.set(value); }
        public SimpleBooleanProperty selectedProperty() { return selected; }
        
        public String getProjectId() { return projectId.get(); }
        public SimpleStringProperty projectIdProperty() { return projectId; }
        
        public String getDescription() { return description.get(); }
        public SimpleStringProperty descriptionProperty() { return description; }
        
        public ProjectStatus getStatus() { return status.get(); }
        public SimpleObjectProperty<ProjectStatus> statusProperty() { return status; }
        
        public LocalDate getStartDate() { return startDate.get(); }
        public SimpleObjectProperty<LocalDate> startDateProperty() { return startDate; }
        
        public LocalDate getEndDate() { return endDate.get(); }
        public SimpleObjectProperty<LocalDate> endDateProperty() { return endDate; }
        
        public int getAssignmentCount() { return assignmentCount.get(); }
        public SimpleIntegerProperty assignmentCountProperty() { return assignmentCount; }
    }
}