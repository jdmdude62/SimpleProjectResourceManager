package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.model.TaskDependency;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskDependencyRepository;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.view.CriticalPathView;
import com.subliminalsearch.simpleprojectresourcemanager.view.DashboardView;
import com.subliminalsearch.simpleprojectresourcemanager.view.GanttChartView;
import com.subliminalsearch.simpleprojectresourcemanager.view.MapView;
import com.subliminalsearch.simpleprojectresourcemanager.service.SharePointExportService;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskListView {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    // Custom DatePicker table cell for editing dates
    private static class DatePickerTableCell<S> extends TableCell<S, LocalDate> {
        private DatePicker datePicker;
        
        public DatePickerTableCell() {
            super();
        }
        
        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createDatePicker();
                setText(null);
                setGraphic(datePicker);
            }
        }
        
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getDate());
            setGraphic(null);
        }
        
        @Override
        public void updateItem(LocalDate item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (datePicker != null) {
                        datePicker.setValue(getItem());
                    }
                    setText(null);
                    setGraphic(datePicker);
                } else {
                    setText(getDate());
                    setGraphic(null);
                }
            }
        }
        
        private void createDatePicker() {
            datePicker = new DatePicker(getItem());
            datePicker.setConverter(new StringConverter<LocalDate>() {
                @Override
                public String toString(LocalDate date) {
                    if (date != null) {
                        return DATE_FORMAT.format(date);
                    } else {
                        return "";
                    }
                }
                
                @Override
                public LocalDate fromString(String string) {
                    if (string != null && !string.isEmpty()) {
                        return LocalDate.parse(string, DATE_FORMAT);
                    } else {
                        return null;
                    }
                }
            });
            datePicker.setOnAction(e -> {
                commitEdit(datePicker.getValue());
            });
        }
        
        private String getDate() {
            return getItem() == null ? "" : DATE_FORMAT.format(getItem());
        }
    }
    
    private final Stage stage;
    private final Project project;
    private final TaskRepository taskRepository;
    private final ResourceRepository resourceRepository;
    private final List<Resource> resources;
    private final List<Assignment> projectAssignments;
    private final SchedulingService schedulingService;
    
    private TableView<Task> tableView;
    private ObservableList<Task> tasks;
    private ObservableList<Task> allTasks; // Keep all tasks for filtering
    private Label statusLabel;
    private TextField searchField;
    private ComboBox<String> statusFilter;
    private ComboBox<String> priorityFilter;
    private ComboBox<String> assignedFilter;
    private DatePicker startDateFilter;
    private DatePicker endDateFilter;
    private TaskDependencyRepository dependencyRepository;
    
    public TaskListView(Project project, TaskRepository taskRepository, List<Resource> resources, 
                       List<Assignment> projectAssignments, SchedulingService schedulingService) {
        this(project, taskRepository, resources, projectAssignments, schedulingService, null);
    }
    
    public TaskListView(Project project, TaskRepository taskRepository, List<Resource> resources, 
                       List<Assignment> projectAssignments, SchedulingService schedulingService, javafx.stage.Window owner) {
        this.project = project;
        this.taskRepository = taskRepository;
        this.resourceRepository = schedulingService.getResourceRepository();
        this.resources = resources;
        this.projectAssignments = projectAssignments;
        this.schedulingService = schedulingService;
        this.dependencyRepository = new TaskDependencyRepository(taskRepository.getDataSource());
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.setTitle("Task Management - " + project.getProjectId());
        
        if (owner != null) {
            this.stage.initOwner(owner);
        }
        
        initialize(owner);
        loadTasks();
    }
    
    private void initialize(javafx.stage.Window owner) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // Header with project info and controls
        VBox header = createHeader();
        
        // Create table (but it will be hidden initially)
        tableView = createTaskTable();
        
        // Status bar
        statusLabel = new Label("Loading tasks...");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        // Layout
        VBox.setVgrow(tableView, Priority.ALWAYS);
        root.getChildren().addAll(header, tableView, statusLabel);
        
        // Scene - make it large enough to view without stretching
        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        stage.setMinWidth(1400);
        stage.setMinHeight(700);
        
        // Position on the same screen as owner
        if (owner != null) {
            DialogUtils.positionStageOnOwnerScreen(stage, owner, 0.95, 0.9);
        } else {
            stage.centerOnScreen();
        }
        
        // Open dashboard view by default after window is shown
        stage.setOnShown(e -> openDashboardView());
    }
    
    private VBox createHeader() {
        VBox headerContainer = new VBox(10);
        
        // Main header with project info and buttons
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 5;");
        
        // Project info
        VBox projectInfo = new VBox(5);
        Label projectLabel = new Label("Project: " + project.getProjectId());
        projectLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label descLabel = new Label(project.getDescription());
        descLabel.setStyle("-fx-font-size: 12px;");
        projectInfo.getChildren().addAll(projectLabel, descLabel);
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // View buttons - Dashboard first as default
        Button dashboardBtn = new Button("📊 Dashboard");
        dashboardBtn.setStyle("-fx-font-size: 14px;");
        dashboardBtn.setOnAction(e -> openDashboardView());
        
        Button listViewBtn = new Button("📋 List");
        listViewBtn.setStyle("-fx-font-size: 14px;");
        listViewBtn.setOnAction(e -> showListView());
        
        Button kanbanBtn = new Button("📋 Kanban");
        kanbanBtn.setStyle("-fx-font-size: 14px;");
        kanbanBtn.setOnAction(e -> openKanbanView());
        
        Button ganttBtn = new Button("📊 Gantt Chart");
        ganttBtn.setStyle("-fx-font-size: 14px;");
        ganttBtn.setOnAction(e -> openGanttChart());
        
        Button timelineBtn = new Button("📅 Timeline");
        timelineBtn.setStyle("-fx-font-size: 14px;");
        timelineBtn.setOnAction(e -> openTimelineView());
        
        Button calendarBtn = new Button("📆 Calendar");
        calendarBtn.setStyle("-fx-font-size: 14px;");
        calendarBtn.setOnAction(e -> openCalendarView());
        
        Button criticalPathBtn = new Button("🔀 Critical Path");
        criticalPathBtn.setStyle("-fx-font-size: 14px;");
        criticalPathBtn.setOnAction(e -> openCriticalPathView());
        
        Button mapBtn = new Button("🗺 Map");
        mapBtn.setStyle("-fx-font-size: 14px;");
        mapBtn.setOnAction(e -> openMapView());
        
        // Action buttons
        Button addTaskBtn = new Button("➕ Add Task");
        addTaskBtn.setStyle("-fx-font-size: 14px;");
        addTaskBtn.setOnAction(e -> addNewTask());
        
        Button addSubtaskBtn = new Button("📂 Add Subtask");
        addSubtaskBtn.setStyle("-fx-font-size: 14px;");
        addSubtaskBtn.setOnAction(e -> addSubtask());
        
        Button manageDepsBtn = new Button("🔗 Dependencies");
        manageDepsBtn.setStyle("-fx-font-size: 14px;");
        manageDepsBtn.setOnAction(e -> manageDependencies());
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-font-size: 14px;");
        refreshBtn.setOnAction(e -> loadTasks());
        
        Button exportBtn = new Button("📤 Export");
        exportBtn.setStyle("-fx-font-size: 14px;");
        exportBtn.setOnAction(e -> showExportToSharePointDialog());
        
        header.getChildren().addAll(projectInfo, spacer, dashboardBtn, listViewBtn, kanbanBtn, ganttBtn,
                                  timelineBtn, calendarBtn, criticalPathBtn, mapBtn, 
                                  new Separator(Orientation.VERTICAL),
                                  addTaskBtn, addSubtaskBtn, manageDepsBtn, refreshBtn, exportBtn);
        
        // Resource panel showing assigned resources
        HBox resourcePanel = createResourcePanel();
        
        // Filter panel
        VBox filterPanel = createFilterPanel();
        
        headerContainer.getChildren().addAll(header, resourcePanel, filterPanel);
        return headerContainer;
    }
    
    private VBox createFilterPanel() {
        VBox filterContainer = new VBox(10);
        filterContainer.setPadding(new Insets(10));
        filterContainer.setStyle("-fx-background-color: #f8f8f8; -fx-background-radius: 5; -fx-border-color: #d0d0d0; -fx-border-radius: 5;");
        
        Label filterLabel = new Label("🔍 Filters");
        filterLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // First row of filters
        HBox filterRow1 = new HBox(15);
        filterRow1.setAlignment(Pos.CENTER_LEFT);
        
        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search tasks...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Status filter
        statusFilter = new ComboBox<>();
        statusFilter.setPromptText("All Statuses");
        statusFilter.getItems().addAll("All Statuses", "Not Started", "In Progress", "Completed", "On Hold", "Cancelled", "Blocked", "In Review");
        statusFilter.setPrefWidth(150);
        statusFilter.setOnAction(e -> applyFilters());
        
        // Priority filter
        priorityFilter = new ComboBox<>();
        priorityFilter.setPromptText("All Priorities");
        priorityFilter.getItems().addAll("All Priorities", "Critical", "High", "Medium", "Low");
        priorityFilter.setPrefWidth(150);
        priorityFilter.setOnAction(e -> applyFilters());
        
        // Assigned to filter
        assignedFilter = new ComboBox<>();
        assignedFilter.setPromptText("All Resources");
        assignedFilter.getItems().add("All Resources");
        assignedFilter.getItems().add("Unassigned");
        for (Resource resource : resources) {
            assignedFilter.getItems().add(resource.getName());
        }
        assignedFilter.setPrefWidth(150);
        assignedFilter.setOnAction(e -> applyFilters());
        
        filterRow1.getChildren().addAll(
            new Label("Search:"), searchField,
            new Label("Status:"), statusFilter,
            new Label("Priority:"), priorityFilter,
            new Label("Assigned:"), assignedFilter
        );
        
        // Second row of filters
        HBox filterRow2 = new HBox(15);
        filterRow2.setAlignment(Pos.CENTER_LEFT);
        
        // Date range filters
        startDateFilter = new DatePicker();
        startDateFilter.setPromptText("Start date from");
        startDateFilter.setPrefWidth(150);
        startDateFilter.setOnAction(e -> applyFilters());
        
        endDateFilter = new DatePicker();
        endDateFilter.setPromptText("End date to");
        endDateFilter.setPrefWidth(150);
        endDateFilter.setOnAction(e -> applyFilters());
        
        // Clear filters button
        Button clearFiltersBtn = new Button("❌ Clear Filters");
        clearFiltersBtn.setOnAction(e -> clearFilters());
        
        // Show completed checkbox
        showCompletedCheck = new CheckBox("Show Completed");
        showCompletedCheck.setSelected(true);
        showCompletedCheck.setOnAction(e -> applyFilters());
        
        // Show only overdue
        showOverdueCheck = new CheckBox("Overdue Only");
        showOverdueCheck.setOnAction(e -> applyFilters());
        
        filterRow2.getChildren().addAll(
            new Label("Start Date:"), startDateFilter,
            new Label("End Date:"), endDateFilter,
            showCompletedCheck,
            showOverdueCheck,
            clearFiltersBtn
        );
        
        // Collapsible behavior
        filterRow1.setVisible(false);
        filterRow2.setVisible(false);
        filterRow1.setManaged(false);
        filterRow2.setManaged(false);
        
        filterLabel.setOnMouseClicked(e -> {
            boolean visible = !filterRow1.isVisible();
            filterRow1.setVisible(visible);
            filterRow2.setVisible(visible);
            filterRow1.setManaged(visible);
            filterRow2.setManaged(visible);
            filterLabel.setText(visible ? "🔽 Filters" : "🔍 Filters");
        });
        
        filterContainer.getChildren().addAll(filterLabel, filterRow1, filterRow2);
        return filterContainer;
    }
    
    private HBox createResourcePanel() {
        HBox panel = new HBox(15);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #e8f4f8; -fx-background-radius: 5; -fx-border-color: #b0d4e3; -fx-border-radius: 5;");
        
        // Label
        Label titleLabel = new Label("📋 Project Resources:");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c5aa0;");
        
        // Create resource summary
        VBox resourceList = new VBox(3);
        
        if (projectAssignments.isEmpty()) {
            Label noResourcesLabel = new Label("No resources assigned to this project");
            noResourcesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            resourceList.getChildren().add(noResourcesLabel);
        } else {
            // Group assignments by resource
            Map<Long, List<Assignment>> assignmentsByResource = projectAssignments.stream()
                .collect(Collectors.groupingBy(Assignment::getResourceId));
            
            // Create a summary for each resource
            int count = 0;
            for (Map.Entry<Long, List<Assignment>> entry : assignmentsByResource.entrySet()) {
                if (count >= 3) {
                    Label moreLabel = new Label("... and " + (assignmentsByResource.size() - 3) + " more");
                    moreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");
                    resourceList.getChildren().add(moreLabel);
                    break;
                }
                
                Long resourceId = entry.getKey();
                List<Assignment> assignments = entry.getValue();
                
                // Get resource details
                Optional<Resource> resource = schedulingService.getResourceById(resourceId);
                if (resource.isPresent()) {
                    Resource res = resource.get();
                    
                    // Calculate total days assigned
                    long totalDays = assignments.stream()
                        .mapToLong(a -> java.time.temporal.ChronoUnit.DAYS.between(
                            a.getStartDate(), a.getEndDate()) + 1)
                        .sum();
                    
                    // Format date ranges
                    String dateRanges = assignments.stream()
                        .map(a -> a.getStartDate().format(DateTimeFormatter.ofPattern("MMM d")) + 
                                 " - " + a.getEndDate().format(DateTimeFormatter.ofPattern("MMM d")))
                        .collect(Collectors.joining(", "));
                    
                    HBox resourceRow = new HBox(10);
                    
                    // Resource name with emoji based on type
                    String emoji = "👤"; // Default emoji
                    if (res.getResourceType() != null && res.getResourceType().getName() != null) {
                        String typeName = res.getResourceType().getName().toUpperCase();
                        if (typeName.contains("TECH") || typeName.contains("FIELD")) {
                            emoji = "👷";
                        } else if (typeName.contains("VENDOR") || typeName.contains("THIRD")) {
                            emoji = "🏢";
                        }
                    }
                    Label nameLabel = new Label(emoji + " " + res.getName());
                    nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                    nameLabel.setMinWidth(150);
                    
                    // Assignment details
                    Label detailsLabel = new Label(totalDays + " days: " + dateRanges);
                    detailsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
                    
                    resourceRow.getChildren().addAll(nameLabel, detailsLabel);
                    resourceList.getChildren().add(resourceRow);
                    count++;
                }
            }
        }
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Summary stats
        VBox stats = new VBox(2);
        Label totalResourcesLabel = new Label("Total Resources: " + 
            projectAssignments.stream().map(Assignment::getResourceId).distinct().count());
        totalResourcesLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c5aa0;");
        
        Label totalAssignmentsLabel = new Label("Total Assignments: " + projectAssignments.size());
        totalAssignmentsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c5aa0;");
        
        stats.getChildren().addAll(totalResourcesLabel, totalAssignmentsLabel);
        
        panel.getChildren().addAll(titleLabel, resourceList, spacer, stats);
        return panel;
    }
    
    private TableView<Task> createTaskTable() {
        TableView<Task> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Status indicator column
        TableColumn<Task, String> statusCol = new TableColumn<>("");
        statusCol.setPrefWidth(30);
        statusCol.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            if (task.isOverdue()) return new SimpleStringProperty("🔴");
            if (task.isBlocked()) return new SimpleStringProperty("🟠");
            if (task.isCompleted()) return new SimpleStringProperty("🟢");
            return new SimpleStringProperty("⚪");
        });
        statusCol.setStyle("-fx-alignment: CENTER;");
        
        // Task ID column
        TableColumn<Task, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("taskCode"));
        idCol.setPrefWidth(80);
        idCol.setEditable(false);
        
        // Title column
        TableColumn<Task, String> titleCol = new TableColumn<>("Task Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(250);
        titleCol.setCellFactory(TextFieldTableCell.forTableColumn());
        titleCol.setOnEditCommit(event -> {
            Task task = event.getRowValue();
            String oldValue = task.getTitle();
            String newValue = event.getNewValue();
            System.out.println("Edit commit: changing title from '" + oldValue + "' to '" + newValue + "'");
            task.setTitle(newValue);
            saveTask(task);
            System.out.println("Task saved with ID: " + task.getId());
        });
        
        // Parent Task column for hierarchy
        TableColumn<Task, String> parentCol = new TableColumn<>("Parent Task");
        parentCol.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            if (task.getParentTaskId() != null) {
                // Find parent task name
                Task parent = tasks.stream()
                    .filter(t -> t.getId().equals(task.getParentTaskId()))
                    .findFirst()
                    .orElse(null);
                return new SimpleStringProperty(parent != null ? parent.getTaskCode() + ": " + parent.getTitle() : "Task #" + task.getParentTaskId());
            }
            return new SimpleStringProperty("");
        });
        parentCol.setPrefWidth(150);
        parentCol.setCellFactory(ComboBoxTableCell.forTableColumn(
            FXCollections.observableArrayList(getAvailableParentTasks())
        ));
        parentCol.setOnEditCommit(event -> {
            Task task = event.getRowValue();
            String newParent = event.getNewValue();
            
            if (newParent == null || newParent.isEmpty()) {
                task.setParentTaskId(null);
            } else {
                // Parse parent task ID from the display string
                Task parent = tasks.stream()
                    .filter(t -> (t.getTaskCode() + ": " + t.getTitle()).equals(newParent))
                    .findFirst()
                    .orElse(null);
                if (parent != null && !parent.getId().equals(task.getId())) {
                    task.setParentTaskId(parent.getId());
                }
            }
            saveTask(task);
            loadTasks(); // Reload to update hierarchy display
        });
        
        // Dependencies column
        TableColumn<Task, String> depsCol = new TableColumn<>("Dependencies");
        depsCol.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            List<TaskDependency> deps = taskRepository.findDependenciesBySuccessor(task.getId());
            if (!deps.isEmpty()) {
                String depsStr = deps.stream()
                    .map(d -> {
                        Task pred = tasks.stream()
                            .filter(t -> t.getId().equals(d.getPredecessorId()))
                            .findFirst()
                            .orElse(null);
                        String taskRef = pred != null ? pred.getTaskCode() : "#" + d.getPredecessorId();
                        return taskRef + " (" + d.getDependencyType().getCode() + ")";
                    })
                    .collect(Collectors.joining(", "));
                return new SimpleStringProperty(depsStr);
            }
            return new SimpleStringProperty("");
        });
        depsCol.setPrefWidth(150);
        
        // Type column
        TableColumn<Task, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTaskType().getDisplayName()));
        typeCol.setPrefWidth(100);
        typeCol.setCellFactory(ComboBoxTableCell.forTableColumn(
            Arrays.stream(Task.TaskType.values())
                .map(Task.TaskType::getDisplayName)
                .toArray(String[]::new)
        ));
        typeCol.setOnEditCommit(event -> {
            Task task = event.getRowValue();
            Task.TaskType newType = Arrays.stream(Task.TaskType.values())
                .filter(t -> t.getDisplayName().equals(event.getNewValue()))
                .findFirst()
                .orElse(Task.TaskType.GENERAL);
            task.setTaskType(newType);
            saveTask(task);
        });
        
        // Priority column with color coding and combo box editing
        TableColumn<Task, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getPriority().getDisplayName()));
        priorityCol.setPrefWidth(80);
        priorityCol.setCellFactory(ComboBoxTableCell.forTableColumn(
            Arrays.stream(Task.TaskPriority.values())
                .map(Task.TaskPriority::getDisplayName)
                .toArray(String[]::new)
        ));
        priorityCol.setOnEditCommit(event -> {
            Task task = event.getRowValue();
            Task.TaskPriority newPriority = Arrays.stream(Task.TaskPriority.values())
                .filter(p -> p.getDisplayName().equals(event.getNewValue()))
                .findFirst()
                .orElse(Task.TaskPriority.MEDIUM);
            task.setPriority(newPriority);
            saveTask(task);
            // Refresh the row to update color
            tableView.refresh();
        });
        
        // Status column
        TableColumn<Task, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus().getDisplayName()));
        statusColumn.setPrefWidth(100);
        statusColumn.setCellFactory(ComboBoxTableCell.forTableColumn(
            Arrays.stream(Task.TaskStatus.values())
                .map(Task.TaskStatus::getDisplayName)
                .toArray(String[]::new)
        ));
        statusColumn.setOnEditCommit(event -> {
            Task task = event.getRowValue();
            Task.TaskStatus newStatus = Arrays.stream(Task.TaskStatus.values())
                .filter(s -> s.getDisplayName().equals(event.getNewValue()))
                .findFirst()
                .orElse(Task.TaskStatus.NOT_STARTED);
            task.setStatus(newStatus);
            taskRepository.updateStatus(task.getId(), newStatus);
            loadTasks(); // Refresh to update indicators
        });
        
        // Progress column
        TableColumn<Task, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getCompletionRate()).asObject());
        progressCol.setPrefWidth(100);
        progressCol.setCellFactory(ProgressBarTableCell.forTableColumn());
        
        // Assigned To column
        TableColumn<Task, String> assignedCol = new TableColumn<>("Assigned To");
        assignedCol.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            if (task.getAssignedToName() != null) {
                return new SimpleStringProperty(task.getAssignedToName());
            }
            return new SimpleStringProperty("Unassigned");
        });
        assignedCol.setPrefWidth(120);
        assignedCol.setCellFactory(ComboBoxTableCell.forTableColumn(
            FXCollections.observableArrayList(
                resources.stream()
                    .map(Resource::getName)
                    .collect(Collectors.toList())
            )
        ));
        assignedCol.setOnEditCommit(event -> {
            Task task = event.getRowValue();
            String resourceName = event.getNewValue();
            
            // Find the resource by name and set its ID
            Resource selectedResource = resources.stream()
                .filter(r -> r.getName().equals(resourceName))
                .findFirst()
                .orElse(null);
            
            if (selectedResource != null) {
                task.setAssignedTo(selectedResource.getId());
                task.setAssignedToName(selectedResource.getName());
            } else {
                task.setAssignedTo(null);
                task.setAssignedToName(null);
            }
            saveTask(task);
        });
        
        // Start Date column with DatePicker
        TableColumn<Task, LocalDate> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getPlannedStart()));
        startDateCol.setPrefWidth(120);
        startDateCol.setCellFactory(column -> new DatePickerTableCell<>());
        startDateCol.setOnEditCommit(event -> {
            Task task = event.getRowValue();
            task.setPlannedStart(event.getNewValue());
            saveTask(task);
        });
        
        // End Date column with DatePicker
        TableColumn<Task, LocalDate> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getPlannedEnd()));
        endDateCol.setPrefWidth(120);
        endDateCol.setCellFactory(column -> new DatePickerTableCell<>());
        endDateCol.setOnEditCommit(event -> {
            Task task = event.getRowValue();
            task.setPlannedEnd(event.getNewValue());
            saveTask(task);
        });
        
        // Estimated Hours column
        TableColumn<Task, Double> hoursCol = new TableColumn<>("Est. Hours");
        hoursCol.setCellValueFactory(new PropertyValueFactory<>("estimatedHours"));
        hoursCol.setPrefWidth(80);
        hoursCol.setStyle("-fx-alignment: CENTER;");
        
        // Actions column
        TableColumn<Task, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(120);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button detailsBtn = new Button("📝");
            private final Button deleteBtn = new Button("🗑️");
            
            {
                detailsBtn.setTooltip(new Tooltip("View Details"));
                deleteBtn.setTooltip(new Tooltip("Delete Task"));
                
                detailsBtn.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    showTaskDetails(task);
                });
                
                deleteBtn.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    deleteTask(task);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, detailsBtn, deleteBtn);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
        
        // Add all columns
        table.getColumns().addAll(
            statusCol, idCol, titleCol, parentCol, depsCol, typeCol, priorityCol, statusColumn,
            progressCol, assignedCol, startDateCol, endDateCol, hoursCol, actionsCol
        );
        
        // Row factory for visual indicators
        table.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldTask, newTask) -> {
                if (newTask != null) {
                    if (newTask.isOverdue()) {
                        row.setStyle("-fx-background-color: #ffcccc;");
                    } else if (newTask.isBlocked()) {
                        row.setStyle("-fx-background-color: #ffe6cc;");
                    } else if (newTask.isCompleted()) {
                        row.setStyle("-fx-background-color: #ccffcc;");
                    } else {
                        row.setStyle("");
                    }
                }
            });
            return row;
        });
        
        return table;
    }
    
    private void loadTasks() {
        List<Task> loadedTasks = taskRepository.findByProjectId(project.getId());
        
        // Sort tasks by parent-child hierarchy
        List<Task> sortedTasks = new ArrayList<>();
        addTasksToHierarchy(sortedTasks, loadedTasks, null, 0);
        
        allTasks = FXCollections.observableArrayList(sortedTasks);
        tasks = FXCollections.observableArrayList(sortedTasks);
        tableView.setItems(tasks);
        
        // Update status
        updateStatusLabel();
    }
    
    private void updateStatusLabel() {
        long completedCount = tasks.stream().filter(Task::isCompleted).count();
        long overdueCount = tasks.stream().filter(Task::isOverdue).count();
        long blockedCount = tasks.stream().filter(Task::isBlocked).count();
        
        statusLabel.setText(String.format("Showing: %d of %d tasks | Completed: %d | Overdue: %d | Blocked: %d",
            tasks.size(), allTasks.size(), completedCount, overdueCount, blockedCount));
    }
    
    private void addTasksToHierarchy(List<Task> sortedList, List<Task> allTasks, Long parentId, int level) {
        List<Task> children = allTasks.stream()
            .filter(t -> {
                if (parentId == null) {
                    return t.getParentTaskId() == null;
                } else {
                    return parentId.equals(t.getParentTaskId());
                }
            })
            .sorted((a, b) -> {
                // Sort by planned start date, then by ID
                if (a.getPlannedStart() != null && b.getPlannedStart() != null) {
                    int dateCompare = a.getPlannedStart().compareTo(b.getPlannedStart());
                    if (dateCompare != 0) return dateCompare;
                }
                return Long.compare(a.getId(), b.getId());
            })
            .collect(Collectors.toList());
        
        for (Task child : children) {
            sortedList.add(child);
            // Recursively add children
            addTasksToHierarchy(sortedList, allTasks, child.getId(), level + 1);
        }
    }
    
    private List<String> getAvailableParentTasks() {
        List<String> parentOptions = new ArrayList<>();
        parentOptions.add(""); // Empty option for no parent
        
        if (tasks != null) {
            for (Task task : tasks) {
                parentOptions.add(task.getTaskCode() + ": " + task.getTitle());
            }
        }
        return parentOptions;
    }
    
    private void addNewTask() {
        Task newTask = new Task();
        newTask.setProjectId(project.getId());
        newTask.setTitle("New Task");
        newTask.setTaskCode(taskRepository.generateTaskCode(project.getId()));
        newTask.setStatus(Task.TaskStatus.NOT_STARTED);
        newTask.setPriority(Task.TaskPriority.MEDIUM);
        newTask.setTaskType(Task.TaskType.GENERAL);
        
        // Find the max date from existing tasks to position new task at the end
        LocalDate maxDate = taskRepository.findMaxDateForProject(project.getId());
        if (maxDate == null) {
            maxDate = LocalDate.now(); // Only use today if no tasks exist
        }
        
        newTask.setPlannedStart(maxDate);
        newTask.setPlannedEnd(maxDate.plusDays(7));
        newTask.setProgressPercentage(0);
        
        taskRepository.create(newTask);
        loadTasks();
    }
    
    private void saveTask(Task task) {
        try {
            taskRepository.update(task);
            System.out.println("Successfully updated task: " + task.getId() + " - " + task.getTitle());
        } catch (Exception e) {
            System.err.println("Error saving task: " + e.getMessage());
            e.printStackTrace();
            
            // Show error to user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Failed to save task");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
            
            // Reload to restore original values
            loadTasks();
        }
    }
    
    private void deleteTask(Task task) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Task");
        confirm.setHeaderText("Delete task: " + task.getTitle());
        confirm.setContentText("Are you sure you want to delete this task?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            taskRepository.delete(task.getId());
            loadTasks();
        }
    }
    
    private void showTaskDetails(Task task) {
        // TODO: Implement detailed task dialog
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Task Details");
        info.setHeaderText(task.getTitle());
        info.setContentText(String.format(
            "Code: %s\nType: %s\nPriority: %s\nStatus: %s\nProgress: %d%%\n" +
            "Start: %s\nEnd: %s\nEstimated Hours: %.1f\n\nDescription:\n%s",
            task.getTaskCode(),
            task.getTaskType().getDisplayName(),
            task.getPriority().getDisplayName(),
            task.getStatus().getDisplayName(),
            task.getProgressPercentage(),
            task.getPlannedStart() != null ? task.getPlannedStart().format(DATE_FORMAT) : "Not set",
            task.getPlannedEnd() != null ? task.getPlannedEnd().format(DATE_FORMAT) : "Not set",
            task.getEstimatedHours() != null ? task.getEstimatedHours() : 0.0,
            task.getDescription() != null ? task.getDescription() : "No description"
        ));
        info.showAndWait();
    }
    
    private void addSubtask() {
        // Get selected task to be the parent
        Task selectedTask = tableView.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Selection");
            alert.setHeaderText("Please select a parent task");
            alert.setContentText("Select the task that will be the parent of the new subtask.");
            alert.showAndWait();
            return;
        }
        
        Task newSubtask = new Task();
        newSubtask.setProjectId(project.getId());
        newSubtask.setParentTaskId(selectedTask.getId());
        newSubtask.setTitle("New Subtask of " + selectedTask.getTitle());
        newSubtask.setTaskCode(taskRepository.generateTaskCode(project.getId()));
        newSubtask.setStatus(Task.TaskStatus.NOT_STARTED);
        newSubtask.setPriority(Task.TaskPriority.MEDIUM);
        newSubtask.setTaskType(Task.TaskType.GENERAL);
        newSubtask.setPlannedStart(selectedTask.getPlannedStart());
        newSubtask.setPlannedEnd(selectedTask.getPlannedEnd());
        newSubtask.setProgressPercentage(0);
        
        taskRepository.create(newSubtask);
        loadTasks();
    }
    
    private void manageDependencies() {
        Task selectedTask = tableView.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Selection");
            alert.setHeaderText("Please select a task");
            alert.setContentText("Select the task to manage its dependencies.");
            alert.showAndWait();
            return;
        }
        
        // Create dependency management dialog
        Dialog<List<TaskDependency>> dialog = new Dialog<>();
        dialog.setTitle("Manage Dependencies");
        dialog.setHeaderText("Dependencies for: " + selectedTask.getTaskCode() + " - " + selectedTask.getTitle());
        dialog.getDialogPane().setPrefWidth(600);
        dialog.getDialogPane().setPrefHeight(400);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Current dependencies list
        Label currentLabel = new Label("Current Dependencies (this task depends on):");
        currentLabel.setStyle("-fx-font-weight: bold;");
        
        ListView<String> depsList = new ListView<>();
        List<TaskDependency> currentDeps = taskRepository.findDependenciesBySuccessor(selectedTask.getId());
        ObservableList<String> depsDisplay = FXCollections.observableArrayList();
        
        for (TaskDependency dep : currentDeps) {
            Task predTask = tasks.stream()
                .filter(t -> t.getId().equals(dep.getPredecessorId()))
                .findFirst()
                .orElse(null);
            if (predTask != null) {
                depsDisplay.add(predTask.getTaskCode() + ": " + predTask.getTitle() + 
                    " (" + dep.getDependencyType().getDisplayName() + ")");
            }
        }
        depsList.setItems(depsDisplay);
        depsList.setPrefHeight(150);
        
        // Add new dependency controls
        HBox addDepBox = new HBox(10);
        addDepBox.setAlignment(Pos.CENTER_LEFT);
        
        ComboBox<String> taskCombo = new ComboBox<>();
        ObservableList<String> availableTasks = FXCollections.observableArrayList();
        for (Task task : tasks) {
            if (!task.getId().equals(selectedTask.getId())) {
                availableTasks.add(task.getTaskCode() + ": " + task.getTitle());
            }
        }
        taskCombo.setItems(availableTasks);
        taskCombo.setPrefWidth(250);
        taskCombo.setPromptText("Select predecessor task");
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList(
            Arrays.stream(TaskDependency.DependencyType.values())
                .map(TaskDependency.DependencyType::getDisplayName)
                .toArray(String[]::new)
        ));
        typeCombo.setValue(TaskDependency.DependencyType.FINISH_TO_START.getDisplayName());
        typeCombo.setPrefWidth(150);
        
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            String selectedPred = taskCombo.getValue();
            if (selectedPred != null) {
                Task predTask = tasks.stream()
                    .filter(t -> (t.getTaskCode() + ": " + t.getTitle()).equals(selectedPred))
                    .findFirst()
                    .orElse(null);
                    
                if (predTask != null) {
                    TaskDependency newDep = new TaskDependency();
                    newDep.setPredecessorId(predTask.getId());
                    newDep.setSuccessorId(selectedTask.getId());
                    
                    String typeStr = typeCombo.getValue();
                    TaskDependency.DependencyType depType = Arrays.stream(TaskDependency.DependencyType.values())
                        .filter(dt -> dt.getDisplayName().equals(typeStr))
                        .findFirst()
                        .orElse(TaskDependency.DependencyType.FINISH_TO_START);
                    newDep.setDependencyType(depType);
                    newDep.setLagDays(0);
                    
                    taskRepository.createDependency(newDep);
                    
                    // Refresh the list
                    depsDisplay.add(predTask.getTaskCode() + ": " + predTask.getTitle() + 
                        " (" + depType.getDisplayName() + ")");
                    taskCombo.setValue(null);
                }
            }
        });
        
        Button removeBtn = new Button("Remove Selected");
        removeBtn.setOnAction(e -> {
            int selectedIndex = depsList.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < currentDeps.size()) {
                TaskDependency depToRemove = currentDeps.get(selectedIndex);
                taskRepository.deleteDependency(depToRemove.getId());
                currentDeps.remove(selectedIndex);
                depsDisplay.remove(selectedIndex);
            }
        });
        
        addDepBox.getChildren().addAll(new Label("Depends on:"), taskCombo, typeCombo, addBtn);
        
        content.getChildren().addAll(currentLabel, depsList, removeBtn, new Separator(), 
            new Label("Add New Dependency:"), addDepBox);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        dialog.showAndWait();
        loadTasks(); // Reload to update dependencies display
    }
    
    public void show() {
        stage.show();
    }
    
    public void hide() {
        stage.hide();
    }
    
    private void openKanbanView() {
        KanbanBoardView kanbanView = new KanbanBoardView(project, taskRepository, resources, stage);
        kanbanView.show();
        // Optionally close this view
        // stage.close();
    }
    
    private void openGanttChart() {
        GanttChartView ganttView = new GanttChartView(project, taskRepository, dependencyRepository, resources, stage);
        ganttView.show();
    }
    
    private void openTimelineView() {
        ResourceTimelineView timelineView = new ResourceTimelineView(project, taskRepository, resourceRepository, stage);
        timelineView.show();
    }
    
    private void openCalendarView() {
        CalendarMatrixView calendarView = new CalendarMatrixView(project, taskRepository, resourceRepository, stage);
        calendarView.show();
    }
    
    private void openCriticalPathView() {
        CriticalPathView criticalPathView = new CriticalPathView(project, taskRepository, stage);
        criticalPathView.show();
    }
    
    private void openDashboardView() {
        try {
            DashboardView dashboardView = new DashboardView(project, taskRepository, resourceRepository, stage);
            dashboardView.show();
        } catch (Exception e) {
            System.err.println("Error opening Dashboard: " + e.getMessage());
            e.printStackTrace();
            
            // Show error dialog
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Dashboard Error");
            alert.setHeaderText("Unable to open Dashboard");
            alert.setContentText("There was an error opening the Dashboard view. This may be due to a JavaFX compatibility issue.\n\nError: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void openMapView() {
        MapView mapView = new MapView(project, taskRepository, resourceRepository, schedulingService.getAssignmentRepository(), stage);
        mapView.show();
    }
    
    private void showListView() {
        // Close any open view windows and return focus to the main list table
        // This is already the default view, so just ensure it's visible
        if (!stage.isShowing()) {
            stage.show();
        }
        stage.toFront();
        stage.requestFocus();
    }
    
    private CheckBox showCompletedCheck;
    private CheckBox showOverdueCheck;
    
    private void applyFilters() {
        if (allTasks == null) return;
        
        List<Task> filtered = new ArrayList<>(allTasks);
        
        // Text search filter
        if (searchField != null && searchField.getText() != null && !searchField.getText().trim().isEmpty()) {
            String searchText = searchField.getText().toLowerCase();
            filtered = filtered.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(searchText) ||
                           (t.getDescription() != null && t.getDescription().toLowerCase().contains(searchText)) ||
                           (t.getTaskCode() != null && t.getTaskCode().toLowerCase().contains(searchText)))
                .collect(Collectors.toList());
        }
        
        // Status filter
        if (statusFilter != null && statusFilter.getValue() != null && !"All Statuses".equals(statusFilter.getValue())) {
            String statusDisplay = statusFilter.getValue();
            // Convert display name to enum name
            String status = statusDisplay.toUpperCase().replace(" ", "_");
            filtered = filtered.stream()
                .filter(t -> t.getStatus() != null && t.getStatus().name().equals(status))
                .collect(Collectors.toList());
        }
        
        // Priority filter
        if (priorityFilter != null && priorityFilter.getValue() != null && !"All Priorities".equals(priorityFilter.getValue())) {
            String priorityDisplay = priorityFilter.getValue();
            // Convert display name to enum name
            String priority = priorityDisplay.toUpperCase();
            filtered = filtered.stream()
                .filter(t -> t.getPriority() != null && t.getPriority().name().equals(priority))
                .collect(Collectors.toList());
        }
        
        // Assigned resource filter
        if (assignedFilter != null && assignedFilter.getValue() != null && !"All Resources".equals(assignedFilter.getValue())) {
            String resourceName = assignedFilter.getValue();
            if ("Unassigned".equals(resourceName)) {
                filtered = filtered.stream()
                    .filter(t -> t.getAssignedTo() == null)
                    .collect(Collectors.toList());
            } else {
                // Find resource by name
                Resource selectedResource = resources.stream()
                    .filter(r -> r.getName().equals(resourceName))
                    .findFirst()
                    .orElse(null);
                if (selectedResource != null) {
                    filtered = filtered.stream()
                        .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().equals(selectedResource.getId()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        // Date range filters
        if (startDateFilter != null && startDateFilter.getValue() != null) {
            LocalDate startDate = startDateFilter.getValue();
            filtered = filtered.stream()
                .filter(t -> t.getPlannedStart() != null && !t.getPlannedStart().isBefore(startDate))
                .collect(Collectors.toList());
        }
        
        if (endDateFilter != null && endDateFilter.getValue() != null) {
            LocalDate endDate = endDateFilter.getValue();
            filtered = filtered.stream()
                .filter(t -> t.getPlannedEnd() != null && !t.getPlannedEnd().isAfter(endDate))
                .collect(Collectors.toList());
        }
        
        // Show completed filter
        if (showCompletedCheck != null && !showCompletedCheck.isSelected()) {
            filtered = filtered.stream()
                .filter(t -> t.getStatus() != Task.TaskStatus.COMPLETED)
                .collect(Collectors.toList());
        }
        
        // Overdue only filter
        if (showOverdueCheck != null && showOverdueCheck.isSelected()) {
            filtered = filtered.stream()
                .filter(Task::isOverdue)
                .collect(Collectors.toList());
        }
        
        // Update the table
        tasks.setAll(filtered);
        updateStatusLabel();
    }
    
    private void clearFilters() {
        if (searchField != null) searchField.clear();
        if (statusFilter != null) statusFilter.setValue(null);
        if (priorityFilter != null) priorityFilter.setValue(null);
        if (assignedFilter != null) assignedFilter.setValue(null);
        if (startDateFilter != null) startDateFilter.setValue(null);
        if (endDateFilter != null) endDateFilter.setValue(null);
        
        // Reset checkboxes
        if (showCompletedCheck != null) showCompletedCheck.setSelected(true);
        if (showOverdueCheck != null) showOverdueCheck.setSelected(false);
        
        // Reset to show all tasks
        if (allTasks != null) {
            tasks.setAll(allTasks);
            updateStatusLabel();
        }
    }
    
    private void showExportToSharePointDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Export to SharePoint");
        dialog.setHeaderText("Export assignments for technician calendars");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);
        
        // Export options
        Label instructionLabel = new Label("Choose export format:");
        instructionLabel.setStyle("-fx-font-weight: bold;");
        
        RadioButton csvOption = new RadioButton("CSV for SharePoint List Import");
        csvOption.setSelected(true);
        RadioButton icsOption = new RadioButton("ICS Calendar Format");
        RadioButton individualOption = new RadioButton("Individual Technician Schedules");
        
        ToggleGroup formatGroup = new ToggleGroup();
        csvOption.setToggleGroup(formatGroup);
        icsOption.setToggleGroup(formatGroup);
        individualOption.setToggleGroup(formatGroup);
        
        // Date range selection
        Label dateLabel = new Label("Date Range:");
        dateLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
        
        DatePicker startDate = new DatePicker(LocalDate.now());
        DatePicker endDate = new DatePicker(LocalDate.now().plusMonths(1));
        
        HBox dateBox = new HBox(10);
        dateBox.getChildren().addAll(new Label("From:"), startDate, new Label("To:"), endDate);
        
        // File location
        Label fileLabel = new Label("Export Location:");
        fileLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
        
        TextField fileField = new TextField();
        fileField.setText(System.getProperty("user.home") + "\\SharePoint_Export_" + 
                         LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv");
        fileField.setPrefWidth(400);
        
        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Export File");
            fileChooser.setInitialFileName("SharePoint_Export.csv");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("Calendar Files", "*.ics"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            File file = fileChooser.showSaveDialog(dialog.getOwner());
            if (file != null) {
                fileField.setText(file.getAbsolutePath());
            }
        });
        
        HBox fileBox = new HBox(10);
        fileBox.getChildren().addAll(fileField, browseBtn);
        
        // Progress indicator
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(480);
        progressBar.setVisible(false);
        
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: green;");
        
        content.getChildren().addAll(
            instructionLabel,
            csvOption,
            icsOption, 
            individualOption,
            new Separator(),
            dateLabel,
            dateBox,
            new Separator(),
            fileLabel,
            fileBox,
            progressBar,
            statusLabel
        );
        
        dialog.getDialogPane().setContent(content);
        
        ButtonType exportButton = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(exportButton, cancelButton);
        
        // Export action
        Button exportBtn = (Button) dialog.getDialogPane().lookupButton(exportButton);
        exportBtn.setOnAction(e -> {
            progressBar.setVisible(true);
            statusLabel.setText("Exporting...");
            
            // Create export service
            SharePointExportService exportService = new SharePointExportService(
                taskRepository, resourceRepository
            );
            
            try {
                String filePath = fileField.getText();
                
                if (csvOption.isSelected()) {
                    // Export current project to CSV
                    if (project != null) {
                        exportService.exportAssignmentsToCSV(filePath, project);
                        statusLabel.setText("✓ Export completed successfully!");
                        statusLabel.setStyle("-fx-text-fill: green;");
                        
                        // Show success alert
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Export Complete");
                        alert.setHeaderText("SharePoint export completed");
                        alert.setContentText("File saved to: " + filePath + "\n\n" +
                                           "You can now import this CSV file into your SharePoint list.");
                        alert.showAndWait();
                    }
                } else if (individualOption.isSelected()) {
                    // Export individual schedules
                    String directory = new File(filePath).getParent();
                    List<Project> projects = List.of(project);
                    exportService.exportAllTechnicianCalendars(directory, projects);
                    
                    statusLabel.setText("✓ Individual schedules exported!");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Export Complete");
                    alert.setHeaderText("Individual technician schedules exported");
                    alert.setContentText("Files saved to: " + directory);
                    alert.showAndWait();
                }
                
            } catch (IOException ex) {
                statusLabel.setText("✗ Export failed: " + ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText("Could not export to SharePoint format");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            } finally {
                progressBar.setVisible(false);
            }
        });
        
        dialog.showAndWait();
    }
}