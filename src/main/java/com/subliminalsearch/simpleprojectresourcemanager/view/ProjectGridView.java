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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
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
import java.util.Optional;
import java.util.Set;
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
    private List<Project> filteredProjects;
    private Runnable onProjectsChangedCallback;
    
    public ProjectGridView(SchedulingService schedulingService) {
        this(schedulingService, null, null);
    }
    
    public ProjectGridView(SchedulingService schedulingService, Window owner) {
        this(schedulingService, owner, null);
    }
    
    public ProjectGridView(SchedulingService schedulingService, Window owner, List<Project> filteredProjects) {
        this.schedulingService = schedulingService;
        this.filteredProjects = filteredProjects;
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.setTitle("Project Grid Editor - Bulk Edit Projects");
        
        // Set owner window if provided
        if (owner != null) {
            this.stage.initOwner(owner);
        }
        
        // Set minimum and preferred size for the stage (reduced by 20%)
        this.stage.setMinWidth(1120);
        this.stage.setMinHeight(640);
        this.stage.setWidth(1280);
        this.stage.setHeight(720);
        
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
            row.setPrefHeight(35);
            row.setStyle("-fx-font-size: 13px;");
            return row;
        });
        
        // Create columns
        createColumns();
        
        // Layout
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        
        // Add title label
        Label titleLabel = new Label("Project Grid Editor - Double-click cells to edit");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Add save info label
        Label infoLabel = new Label("Changes are saved automatically when you finish editing each cell");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        // Add refresh button and highlight unassigned button
        Button refreshButton = new Button("Refresh Data");
        refreshButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 16px;");
        refreshButton.setOnAction(e -> refreshData());
        
        Button highlightUnassignedButton = new Button("Highlight Unassigned");
        highlightUnassignedButton.setStyle("-fx-font-size: 12px; -fx-padding: 8px 16px;");
        highlightUnassignedButton.setOnAction(e -> highlightUnassignedProjects());
        
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(refreshButton, highlightUnassignedButton);
        
        root.getChildren().addAll(titleLabel, infoLabel, tableView, buttonBox);
        VBox.setVgrow(tableView, javafx.scene.layout.Priority.ALWAYS);
        
        // Create scene with proper initial size (reduced by 20%)
        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        
        // Position the stage relative to owner if available, otherwise center on screen
        if (stage.getOwner() != null) {
            Window owner = stage.getOwner();
            stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
        } else {
            stage.centerOnScreen();
        }
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
        List<Project> projectList;
        if (filteredProjects != null && !filteredProjects.isEmpty()) {
            projectList = filteredProjects;
            stage.setTitle("Project Grid Editor - Editing " + filteredProjects.size() + " Filtered Projects");
        } else {
            projectList = schedulingService.getAllProjects();
            logger.debug("Loaded {} projects from database", projectList.size());
            for (Project p : projectList) {
                logger.debug("Grid project: {} - {} (ID: {})", p.getProjectId(), p.getDescription(), p.getId());
            }
        }
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
        
        // Assigned Resources column (read-only)
        TableColumn<Project, String> resourcesCol = new TableColumn<>("Assigned Resources");
        resourcesCol.setCellValueFactory(cellData -> {
            Project project = cellData.getValue();
            List<com.subliminalsearch.simpleprojectresourcemanager.model.Assignment> assignments = 
                schedulingService.getAssignmentsByProjectId(project.getId());
            
            if (assignments.isEmpty()) {
                return new SimpleStringProperty("None");
            }
            
            // Get unique resource names for this project
            Set<String> resourceNames = new java.util.HashSet<>();
            for (com.subliminalsearch.simpleprojectresourcemanager.model.Assignment assignment : assignments) {
                // Get resource name from assignment
                List<com.subliminalsearch.simpleprojectresourcemanager.model.Resource> allResources = 
                    schedulingService.getAllResources();
                allResources.stream()
                    .filter(r -> r.getId().equals(assignment.getResourceId()))
                    .findFirst()
                    .ifPresent(resource -> resourceNames.add(resource.getName()));
            }
            
            // Join resource names with commas
            String resourceList = String.join(", ", resourceNames);
            return new SimpleStringProperty(resourceList.isEmpty() ? "None" : resourceList);
        });
        resourcesCol.setPrefWidth(200);
        resourcesCol.setEditable(false);
        resourcesCol.setStyle("-fx-alignment: CENTER-LEFT;");
        
        // Delete column with button
        TableColumn<Project, Void> deleteCol = new TableColumn<>("Actions");
        deleteCol.setPrefWidth(80);
        deleteCol.setEditable(false);
        
        Callback<TableColumn<Project, Void>, TableCell<Project, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Project, Void> call(final TableColumn<Project, Void> param) {
                final TableCell<Project, Void> cell = new TableCell<>() {
                    private final Button deleteBtn = new Button("🗑");
                    
                    {
                        deleteBtn.setStyle(
                            "-fx-background-color: #ff6b6b;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 16px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 5 10 5 10;" +
                            "-fx-background-radius: 3;"
                        );
                        
                        deleteBtn.setOnMouseEntered(e -> 
                            deleteBtn.setStyle(
                                "-fx-background-color: #ff5252;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-background-radius: 3;"
                            )
                        );
                        
                        deleteBtn.setOnMouseExited(e -> 
                            deleteBtn.setStyle(
                                "-fx-background-color: #ff6b6b;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-background-radius: 3;"
                            )
                        );
                        
                        deleteBtn.setOnAction(event -> {
                            Project project = getTableView().getItems().get(getIndex());
                            deleteProject(project);
                        });
                    }
                    
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox hbox = new HBox();
                            hbox.setAlignment(javafx.geometry.Pos.CENTER);
                            hbox.getChildren().add(deleteBtn);
                            setGraphic(hbox);
                        }
                    }
                };
                return cell;
            }
        };
        
        deleteCol.setCellFactory(cellFactory);
        
        // Add all columns to table
        tableView.getColumns().addAll(
            projectIdCol,
            descriptionCol,
            pmCol,
            startDateCol,
            endDateCol,
            statusCol,
            resourcesCol,
            deleteCol
        );
    }
    
    private void saveProject(Project project) {
        try {
            schedulingService.updateProject(project);
            
            // Notify callback if set
            if (onProjectsChangedCallback != null) {
                onProjectsChangedCallback.run();
            }
            logger.info("Updated project: {}", project.getProjectId());
        } catch (Exception e) {
            logger.error("Error saving project", e);
            showAlert("Save Error", "Failed to save project: " + e.getMessage());
            refreshData();
        }
    }
    
    private void deleteProject(Project project) {
        // Check if project has assignments
        List<com.subliminalsearch.simpleprojectresourcemanager.model.Assignment> assignments = 
            schedulingService.getAssignmentsByProjectId(project.getId());
        
        String message;
        if (!assignments.isEmpty()) {
            message = String.format(
                "This project has %d assignment(s).\n\n" +
                "Project: %s\n" +
                "Description: %s\n\n" +
                "Deleting will remove all assignments and the project.\n\n" +
                "Are you sure you want to delete this project?",
                assignments.size(),
                project.getProjectId(),
                project.getDescription() != null ? project.getDescription() : "No description"
            );
        } else {
            message = String.format(
                "Are you sure you want to delete this project?\n\n" +
                "Project: %s\n" +
                "Description: %s",
                project.getProjectId(),
                project.getDescription() != null ? project.getDescription() : "No description"
            );
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Project");
        confirm.setHeaderText("Delete project: " + project.getProjectId());
        confirm.setContentText(message);
        
        // Set owner window
        confirm.initOwner(stage);
        confirm.initModality(Modality.WINDOW_MODAL);
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (!assignments.isEmpty()) {
                    // Delete project with assignments
                    schedulingService.deleteProjectWithAssignments(project.getId());
                } else {
                    // Delete just the project
                    schedulingService.deleteProject(project.getId());
                }
                
                logger.info("Deleted project: {}", project.getProjectId());
                
                // Remove from table immediately
                projects.remove(project);
                tableView.refresh();
                
                // Notify callback if set
                if (onProjectsChangedCallback != null) {
                    onProjectsChangedCallback.run();
                }
                
            } catch (Exception e) {
                logger.error("Error deleting project", e);
                showAlert("Delete Error", "Failed to delete project: " + e.getMessage());
            }
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
    
    public void setOnProjectsChanged(Runnable callback) {
        this.onProjectsChangedCallback = callback;
    }
    
    private void highlightUnassignedProjects() {
        // Get all assignments to identify which projects have them
        List<com.subliminalsearch.simpleprojectresourcemanager.model.Assignment> allAssignments = 
            schedulingService.getAssignmentsByDateRange(
                LocalDate.now().minusYears(1), 
                LocalDate.now().plusYears(1));
        
        Set<Long> projectsWithAssignments = allAssignments.stream()
            .map(a -> a.getProjectId())
            .collect(Collectors.toSet());
        
        // Apply row factory to highlight unassigned projects
        tableView.setRowFactory(tv -> {
            TableRow<Project> row = new TableRow<Project>() {
                @Override
                protected void updateItem(Project item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle("");
                    } else {
                        if (!projectsWithAssignments.contains(item.getId())) {
                            // Highlight unassigned projects in yellow
                            setStyle("-fx-background-color: #ffffcc;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            };
            return row;
        });
        
        // Force table refresh
        tableView.refresh();
        
        // Show alert with unassigned projects
        List<Project> unassignedProjects = projects.stream()
            .filter(p -> !projectsWithAssignments.contains(p.getId()))
            .collect(Collectors.toList());
        
        if (!unassignedProjects.isEmpty()) {
            StringBuilder message = new StringBuilder("Found " + unassignedProjects.size() + " unassigned projects (highlighted in yellow):\n\n");
            for (Project p : unassignedProjects) {
                message.append("• ").append(p.getProjectId()).append(" - ").append(p.getDescription()).append("\n");
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Unassigned Projects");
            alert.setHeaderText("Projects without any resource assignments");
            alert.setContentText(message.toString());
            alert.initOwner(stage);
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Unassigned Projects");
            alert.setHeaderText(null);
            alert.setContentText("All projects have resource assignments.");
            alert.initOwner(stage);
            alert.showAndWait();
        }
    }
}