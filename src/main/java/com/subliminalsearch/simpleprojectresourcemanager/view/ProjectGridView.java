package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectGridView {
    private static final Logger logger = LoggerFactory.getLogger(ProjectGridView.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final SchedulingService schedulingService;
    private final Stage stage;
    private TableView<Project> tableView;
    private ObservableList<Project> projects;
    private Map<Long, ProjectManager> managerMap;
    private ObservableList<String> managerNames;
    
    public ProjectGridView(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.setTitle("Project Grid Editor - Bulk Edit Projects");
        
        // Set minimum and preferred size for the stage
        this.stage.setMinWidth(1400);
        this.stage.setMinHeight(800);
        this.stage.setWidth(1600);
        this.stage.setHeight(900);
        
        initialize();
    }
    
    private void initialize() {
        // Load data
        loadProjectManagers();
        loadProjects();
        
        // Create table
        tableView = new TableView<>();
        tableView.setEditable(true);
        tableView.setItems(projects);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Set row height and font size for better visibility
        tableView.setRowFactory(tv -> {
            TableRow<Project> row = new TableRow<>();
            row.setPrefHeight(40);
            row.setStyle("-fx-font-size: 14px;");
            return row;
        });
        
        // Create columns
        createColumns();
        
        // Layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        
        // Add title label
        Label titleLabel = new Label("Project Grid Editor - Double-click cells to edit");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Add save info label
        Label infoLabel = new Label("Changes are saved automatically when you finish editing each cell");
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
        
        // Add refresh button
        Button refreshButton = new Button("Refresh Data");
        refreshButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");
        refreshButton.setOnAction(e -> refreshData());
        
        root.getChildren().addAll(titleLabel, infoLabel, tableView, refreshButton);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
        
        // Create scene with proper initial size
        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        
        // Center the stage on screen
        stage.centerOnScreen();
    }
    
    private void loadProjectManagers() {
        List<ProjectManager> managers = schedulingService.getAllProjectManagers();
        managerMap = new HashMap<>();
        for (ProjectManager pm : managers) {
            managerMap.put(pm.getId(), pm);
        }
        
        // Create list of manager names for ComboBox
        managerNames = FXCollections.observableArrayList(
            managers.stream()
                .map(ProjectManager::getName)
                .collect(Collectors.toList())
        );
    }
    
    private void loadProjects() {
        List<Project> projectList = schedulingService.getAllProjects();
        projects = FXCollections.observableArrayList(projectList);
    }
    
    private void createColumns() {
        // Project ID column (non-editable)
        TableColumn<Project, String> projectIdCol = new TableColumn<>("Project ID");
        projectIdCol.setCellValueFactory(new PropertyValueFactory<>("projectId"));
        projectIdCol.setPrefWidth(120);
        projectIdCol.setEditable(false);
        projectIdCol.setStyle("-fx-alignment: CENTER;");
        
        // Description column (editable)
        TableColumn<Project, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setPrefWidth(350);
        descriptionCol.setEditable(true);
        descriptionCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descriptionCol.setOnEditCommit(event -> {
            Project project = event.getRowValue();
            project.setDescription(event.getNewValue());
            saveProject(project);
        });
        
        // Project Manager column (editable with ComboBox)
        TableColumn<Project, String> pmCol = new TableColumn<>("Project Manager");
        pmCol.setPrefWidth(200);
        pmCol.setEditable(true);
        
        // Custom cell value factory for PM column
        pmCol.setCellValueFactory(cellData -> {
            Project project = cellData.getValue();
            Long pmId = project.getProjectManagerId();
            if (pmId != null && managerMap.containsKey(pmId)) {
                return new SimpleStringProperty(managerMap.get(pmId).getName());
            }
            return new SimpleStringProperty("Unassigned");
        });
        
        // ComboBox cell factory for PM column
        pmCol.setCellFactory(ComboBoxTableCell.forTableColumn(managerNames));
        pmCol.setOnEditCommit(event -> {
            Project project = event.getRowValue();
            String newManagerName = event.getNewValue();
            
            // Find the PM by name
            ProjectManager selectedPM = managerMap.values().stream()
                .filter(pm -> pm.getName().equals(newManagerName))
                .findFirst()
                .orElse(null);
            
            if (selectedPM != null) {
                project.setProjectManagerId(selectedPM.getId());
                saveProject(project);
            }
        });
        
        // Start Date column (editable)
        TableColumn<Project, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setPrefWidth(150);
        startDateCol.setEditable(true);
        startDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getStartDate();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "");
        });
        startDateCol.setCellFactory(TextFieldTableCell.forTableColumn());
        startDateCol.setOnEditCommit(event -> {
            Project project = event.getRowValue();
            try {
                LocalDate newDate = LocalDate.parse(event.getNewValue(), DATE_FORMAT);
                project.setStartDate(newDate);
                saveProject(project);
            } catch (DateTimeParseException e) {
                showAlert("Invalid Date", "Please enter date in format: yyyy-MM-dd");
                tableView.refresh();
            }
        });
        startDateCol.setStyle("-fx-alignment: CENTER;");
        
        // End Date column (editable)
        TableColumn<Project, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setPrefWidth(150);
        endDateCol.setEditable(true);
        endDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getEndDate();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMAT) : "");
        });
        endDateCol.setCellFactory(TextFieldTableCell.forTableColumn());
        endDateCol.setOnEditCommit(event -> {
            Project project = event.getRowValue();
            try {
                LocalDate newDate = LocalDate.parse(event.getNewValue(), DATE_FORMAT);
                project.setEndDate(newDate);
                saveProject(project);
            } catch (DateTimeParseException e) {
                showAlert("Invalid Date", "Please enter date in format: yyyy-MM-dd");
                tableView.refresh();
            }
        });
        endDateCol.setStyle("-fx-alignment: CENTER;");
        
        // Status column (editable with ComboBox)
        TableColumn<Project, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> {
            ProjectStatus status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.name() : "ACTIVE");
        });
        statusCol.setPrefWidth(120);
        statusCol.setEditable(true);
        statusCol.setCellFactory(ComboBoxTableCell.forTableColumn("ACTIVE", "ON_HOLD", "COMPLETED", "CANCELLED"));
        statusCol.setOnEditCommit(event -> {
            Project project = event.getRowValue();
            try {
                ProjectStatus newStatus = ProjectStatus.valueOf(event.getNewValue());
                project.setStatus(newStatus);
                saveProject(project);
            } catch (IllegalArgumentException e) {
                showAlert("Invalid Status", "Invalid status value: " + event.getNewValue());
                tableView.refresh();
            }
        });
        statusCol.setStyle("-fx-alignment: CENTER;");
        
        // Add all columns to table
        tableView.getColumns().addAll(
            projectIdCol,
            descriptionCol,
            pmCol,
            startDateCol,
            endDateCol,
            statusCol
        );
    }
    
    private void saveProject(Project project) {
        try {
            schedulingService.updateProject(project);
            logger.info("Updated project: {}", project.getProjectId());
        } catch (Exception e) {
            logger.error("Error saving project", e);
            showAlert("Save Error", "Failed to save project: " + e.getMessage());
            refreshData();
        }
    }
    
    private void refreshData() {
        loadProjectManagers();
        loadProjects();
        tableView.setItems(projects);
        tableView.refresh();
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public void show() {
        stage.show();
    }
    
    public void hide() {
        stage.hide();
    }
}