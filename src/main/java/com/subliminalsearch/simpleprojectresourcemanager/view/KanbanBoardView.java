package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class KanbanBoardView {
    private final Stage stage;
    private final Project project;
    private final TaskRepository taskRepository;
    private final List<Resource> resources;
    
    private HBox columnsContainer;
    private Map<String, VBox> columnContainers;
    private Map<String, ScrollPane> columnScrollPanes;
    private ObservableList<Task> allTasks;
    
    // Field Service specific columns
    private static final String[] COLUMN_NAMES = {
        "Backlog",
        "Ready",
        "In Progress",
        "On Site",
        "Awaiting Materials",
        "Weather Hold",
        "In Review",
        "Completed",
        "Blocked"
    };
    
    private static final Map<String, Task.TaskStatus> COLUMN_STATUS_MAP = Map.of(
        "Backlog", Task.TaskStatus.NOT_STARTED,
        "Ready", Task.TaskStatus.NOT_STARTED,
        "In Progress", Task.TaskStatus.IN_PROGRESS,
        "On Site", Task.TaskStatus.IN_PROGRESS,
        "Awaiting Materials", Task.TaskStatus.BLOCKED,
        "Weather Hold", Task.TaskStatus.BLOCKED,
        "In Review", Task.TaskStatus.REVIEW,
        "Completed", Task.TaskStatus.COMPLETED,
        "Blocked", Task.TaskStatus.BLOCKED
    );
    
    private static final Map<String, Color> COLUMN_COLORS = Map.of(
        "Backlog", Color.LIGHTGRAY,
        "Ready", Color.LIGHTBLUE,
        "In Progress", Color.LIGHTYELLOW,
        "On Site", Color.LIGHTGREEN,
        "Awaiting Materials", Color.ORANGE,
        "Weather Hold", Color.LIGHTCORAL,
        "In Review", Color.PLUM,
        "Completed", Color.LIGHTGREEN,
        "Blocked", Color.SALMON
    );
    
    public KanbanBoardView(Project project, TaskRepository taskRepository, List<Resource> resources) {
        this.project = project;
        this.taskRepository = taskRepository;
        this.resources = resources;
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.setTitle("Kanban Board - " + project.getProjectId());
        this.columnContainers = new HashMap<>();
        this.columnScrollPanes = new HashMap<>();
        
        initialize();
        loadTasks();
    }
    
    private void initialize() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // Header
        HBox header = createHeader();
        
        // Kanban columns
        columnsContainer = new HBox(10);
        columnsContainer.setFillHeight(true);
        
        for (String columnName : COLUMN_NAMES) {
            VBox column = createKanbanColumn(columnName);
            columnsContainer.getChildren().add(column);
        }
        
        // Make columns scrollable horizontally if needed
        ScrollPane mainScroll = new ScrollPane(columnsContainer);
        mainScroll.setFitToHeight(true);
        mainScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Layout
        VBox.setVgrow(mainScroll, Priority.ALWAYS);
        root.getChildren().addAll(header, mainScroll);
        
        // Scene
        Scene scene = new Scene(root, 1600, 900);
        // Try to load CSS, but don't fail if not found
        try {
            if (getClass().getResource("/css/kanban.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/kanban.css").toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Could not load kanban.css: " + e.getMessage());
        }
        stage.setScene(scene);
        stage.setMinWidth(1400);
        stage.setMinHeight(700);
        stage.centerOnScreen();
    }
    
    private HBox createHeader() {
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
        
        // View controls
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setOnAction(e -> loadTasks());
        
        Button filterBtn = new Button("🔍 Filter");
        filterBtn.setOnAction(e -> showFilterDialog());
        
        Button listViewBtn = new Button("📋 List View");
        listViewBtn.setOnAction(e -> switchToListView());
        
        Button statsBtn = new Button("📊 Statistics");
        statsBtn.setOnAction(e -> showStatistics());
        
        header.getChildren().addAll(projectInfo, spacer, refreshBtn, filterBtn, listViewBtn, statsBtn);
        return header;
    }
    
    private VBox createKanbanColumn(String columnName) {
        VBox column = new VBox(10);
        column.setMinWidth(200);
        column.setPrefWidth(250);
        column.setMaxWidth(300);
        column.setStyle("-fx-background-color: #f8f8f8; -fx-background-radius: 5; -fx-padding: 10;");
        
        // Column header
        HBox columnHeader = new HBox(10);
        columnHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(columnName);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        Label countLabel = new Label("(0)");
        countLabel.setStyle("-fx-text-fill: #666;");
        countLabel.setId(columnName + "_count");
        
        columnHeader.getChildren().addAll(titleLabel, countLabel);
        
        // Task container
        VBox taskContainer = new VBox(8);
        taskContainer.setStyle("-fx-padding: 5;");
        taskContainer.setMinHeight(650); // Ensure minimum height for drop zone
        columnContainers.put(columnName, taskContainer);
        
        // Make task container scrollable
        ScrollPane scrollPane = new ScrollPane(taskContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefHeight(700);
        scrollPane.setMinHeight(650);
        columnScrollPanes.put(columnName, scrollPane);
        
        // Setup drag and drop for BOTH the container and the scroll pane
        setupColumnDragAndDrop(taskContainer, columnName);
        setupColumnDragAndDrop(scrollPane, columnName);
        
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        column.getChildren().addAll(columnHeader, scrollPane);
        
        // Add column color indicator
        Color color = COLUMN_COLORS.getOrDefault(columnName, Color.LIGHTGRAY);
        String colorStyle = String.format("-fx-border-color: %s; -fx-border-width: 2 0 0 0;",
            toHexString(color));
        column.setStyle(column.getStyle() + "; " + colorStyle);
        
        return column;
    }
    
    private Node createTaskCard(Task task) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                     "-fx-border-color: #ddd; -fx-border-radius: 5; -fx-cursor: hand;");
        card.setUserData(task);
        
        // Task header
        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label codeLabel = new Label(task.getTaskCode());
        codeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Priority indicator
        Circle priorityIndicator = createPriorityIndicator(task.getPriority());
        
        header.getChildren().addAll(codeLabel, spacer, priorityIndicator);
        
        // Task title
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-weight: bold;");
        
        // Task details
        VBox details = new VBox(3);
        
        if (task.getAssignedTo() != null) {
            Resource assignee = resources.stream()
                .filter(r -> r.getId().equals(task.getAssignedTo()))
                .findFirst()
                .orElse(null);
            if (assignee != null) {
                Label assigneeLabel = new Label("👤 " + assignee.getName());
                assigneeLabel.setStyle("-fx-font-size: 11px;");
                details.getChildren().add(assigneeLabel);
            }
        }
        
        if (task.getPlannedEnd() != null) {
            Label dueLabel = new Label("📅 " + task.getPlannedEnd().format(DateTimeFormatter.ofPattern("MMM dd")));
            dueLabel.setStyle("-fx-font-size: 11px;");
            
            // Highlight if overdue
            if (task.isOverdue()) {
                dueLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: red; -fx-font-weight: bold;");
            }
            details.getChildren().add(dueLabel);
        }
        
        if (task.getLocation() != null && !task.getLocation().isEmpty()) {
            Label locationLabel = new Label("📍 " + task.getLocation());
            locationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #0066cc;");
            details.getChildren().add(locationLabel);
        }
        
        // Progress bar if in progress
        if (task.getProgressPercentage() > 0 && task.getProgressPercentage() < 100) {
            ProgressBar progressBar = new ProgressBar(task.getProgressPercentage() / 100.0);
            progressBar.setPrefWidth(Double.MAX_VALUE);
            progressBar.setPrefHeight(5);
            details.getChildren().add(progressBar);
        }
        
        // Tags for special conditions
        HBox tags = new HBox(5);
        
        if (task.isBlocked()) {
            Label blockedTag = new Label("BLOCKED");
            blockedTag.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; " +
                              "-fx-padding: 2 5; -fx-background-radius: 3; -fx-font-size: 10px;");
            tags.getChildren().add(blockedTag);
        }
        
        // Show block reason preview if present
        if (task.getRiskNotes() != null) {
            String preview = "";
            if (task.getRiskNotes().contains("Weather hold:")) {
                int start = task.getRiskNotes().indexOf("Weather hold:") + 13;
                int end = Math.min(start + 30, task.getRiskNotes().indexOf("[", start));
                if (end == -1) end = Math.min(start + 30, task.getRiskNotes().length());
                preview = "🌧 " + task.getRiskNotes().substring(start, end).trim();
                if (task.getRiskNotes().length() > end) preview += "...";
            } else if (task.getRiskNotes().contains("Awaiting materials:")) {
                int start = task.getRiskNotes().indexOf("Awaiting materials:") + 19;
                int end = Math.min(start + 30, task.getRiskNotes().indexOf("[", start));
                if (end == -1) end = Math.min(start + 30, task.getRiskNotes().length());
                preview = "📦 " + task.getRiskNotes().substring(start, end).trim();
                if (task.getRiskNotes().length() > end) preview += "...";
            }
            
            if (!preview.isEmpty()) {
                Label reasonLabel = new Label(preview);
                reasonLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666; -fx-font-style: italic;");
                reasonLabel.setWrapText(true);
                reasonLabel.setTooltip(new Tooltip("Click to view/edit details"));
                reasonLabel.setCursor(javafx.scene.Cursor.HAND);
                card.getChildren().add(reasonLabel);
            }
        }
        
        if (task.getEquipmentRequired() != null && !task.getEquipmentRequired().isEmpty()) {
            Label equipTag = new Label("🔧");
            equipTag.setTooltip(new Tooltip("Equipment Required: " + task.getEquipmentRequired()));
            tags.getChildren().add(equipTag);
        }
        
        if (task.getSafetyRequirements() != null && !task.getSafetyRequirements().isEmpty()) {
            Label safetyTag = new Label("⚠️");
            safetyTag.setTooltip(new Tooltip("Safety Requirements: " + task.getSafetyRequirements()));
            tags.getChildren().add(safetyTag);
        }
        
        card.getChildren().addAll(header, titleLabel);
        if (!details.getChildren().isEmpty()) {
            card.getChildren().add(details);
        }
        if (!tags.getChildren().isEmpty()) {
            card.getChildren().add(tags);
        }
        
        // Setup drag and drop for the card
        setupCardDragAndDrop(card);
        
        // Double-click to edit, single click for block details
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editTask(task);
            } else if (e.getClickCount() == 1) {
                // Check if task has block reasons to show
                if (task.getRiskNotes() != null) {
                    if (task.getRiskNotes().contains("Weather hold:")) {
                        showWeatherHoldDialog(task);
                        loadTasks(); // Refresh after potential edit
                    } else if (task.getRiskNotes().contains("Awaiting materials:")) {
                        showMaterialsDialog(task);
                        loadTasks(); // Refresh after potential edit
                    }
                }
            }
        });
        
        return card;
    }
    
    private Circle createPriorityIndicator(Task.TaskPriority priority) {
        Circle circle = new Circle(5);
        if (priority == null) priority = Task.TaskPriority.MEDIUM;
        
        switch (priority) {
            case CRITICAL:
                circle.setFill(Color.RED);
                break;
            case HIGH:
                circle.setFill(Color.ORANGE);
                break;
            case MEDIUM:
                circle.setFill(Color.YELLOW);
                break;
            case LOW:
                circle.setFill(Color.GREEN);
                break;
        }
        
        Tooltip.install(circle, new Tooltip(priority.name() + " Priority"));
        return circle;
    }
    
    private void setupCardDragAndDrop(Node card) {
        card.setOnDragDetected(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                Task task = (Task) card.getUserData();
                System.out.println("Starting drag for task: " + task.getTitle() + " (ID: " + task.getId() + ")");
                
                Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(task.getId().toString());
                db.setContent(content);
                
                // Add visual feedback
                card.setOpacity(0.5);
                db.setDragView(card.snapshot(null, null));
                
                e.consume();
            }
        });
        
        card.setOnDragDone(e -> {
            // Reset visual feedback
            card.setOpacity(1.0);
            e.consume();
        });
    }
    
    private void setupColumnDragAndDrop(Node container, String columnName) {
        container.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
            }
        });
        
        container.setOnDragEntered(e -> {
            if (e.getDragboard().hasString()) {
                System.out.println("Drag entered column: " + columnName);
                // Visual feedback for the column
                if (container instanceof VBox) {
                    container.setStyle(container.getStyle() + "; -fx-background-color: #e0f0ff;");
                } else if (container instanceof ScrollPane) {
                    Node content = ((ScrollPane) container).getContent();
                    if (content != null) {
                        content.setStyle(content.getStyle() + "; -fx-background-color: #e0f0ff;");
                    }
                }
                e.consume();
            }
        });
        
        container.setOnDragExited(e -> {
            System.out.println("Drag exited column: " + columnName);
            // Reset visual feedback
            if (container instanceof VBox) {
                container.setStyle(container.getStyle().replace("; -fx-background-color: #e0f0ff;", ""));
            } else if (container instanceof ScrollPane) {
                Node content = ((ScrollPane) container).getContent();
                if (content != null) {
                    content.setStyle(content.getStyle().replace("; -fx-background-color: #e0f0ff;", ""));
                }
            }
            e.consume();
        });
        
        container.setOnDragDropped(e -> {
            System.out.println("Drop detected in column: " + columnName);
            Dragboard db = e.getDragboard();
            boolean success = false;
            
            if (db.hasString()) {
                try {
                    Long taskId = Long.parseLong(db.getString());
                    System.out.println("Dropping task ID: " + taskId + " into column: " + columnName);
                    
                    Task task = allTasks.stream()
                        .filter(t -> t.getId().equals(taskId))
                        .findFirst()
                        .orElse(null);
                    
                    if (task != null) {
                        System.out.println("Found task: " + task.getTitle());
                        // Update task status based on column
                        Task.TaskStatus newStatus = COLUMN_STATUS_MAP.get(columnName);
                        updateTaskStatus(task, newStatus, columnName);
                        success = true;
                    } else {
                        System.err.println("Task not found with ID: " + taskId);
                    }
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid task ID in drag and drop: " + db.getString());
                }
            }
            
            e.setDropCompleted(success);
            e.consume();
        });
    }
    
    private void updateTaskStatus(Task task, Task.TaskStatus newStatus, String columnName) {
        Task.TaskStatus oldStatus = task.getStatus();
        System.out.println("Updating task '" + task.getTitle() + "' from status " + oldStatus + " to column " + columnName);
        
        // Validation rules
        if (!validateStatusTransition(task, oldStatus, newStatus, columnName)) {
            System.out.println("Validation failed for transition");
            return;
        }
        
        // Update task based on new column
        task.setStatus(newStatus);
        System.out.println("New status will be: " + newStatus);
        
        // Field service specific updates
        switch (columnName) {
            case "Backlog":
                // Moving back to backlog - clean up all markers
                if (task.getRiskNotes() != null) {
                    String cleanNotes = task.getRiskNotes()
                        .replace("[Ready]", "")
                        .replace("[OnSite]", "")
                        .replace("[weather]", "")
                        .replace("[materials]", "");
                    // Also remove the prefixes if moving back to backlog
                    if (cleanNotes.contains("Awaiting materials:") || cleanNotes.contains("Weather hold:")) {
                        cleanNotes = "";
                    }
                    task.setRiskNotes(cleanNotes.trim().isEmpty() ? null : cleanNotes.trim());
                }
                task.setStatus(Task.TaskStatus.NOT_STARTED);
                task.setProgressPercentage(0);
                break;
                
            case "Ready":
                // Task is ready to be worked on
                // Add marker to keep it in Ready column
                String notes = task.getRiskNotes() != null ? task.getRiskNotes() : "";
                if (!notes.contains("[Ready]")) {
                    task.setRiskNotes(notes + " [Ready]");
                }
                break;
                
            case "In Progress":
                // Remove Ready marker if present
                if (task.getRiskNotes() != null) {
                    task.setRiskNotes(task.getRiskNotes().replace("[Ready]", "").trim());
                }
                if (task.getActualStart() == null) {
                    task.setActualStart(LocalDate.now());
                }
                if (task.getProgressPercentage() == 0) {
                    task.setProgressPercentage(25);
                }
                break;
                
            case "On Site":
                // Technician is on site
                task.setStatus(Task.TaskStatus.IN_PROGRESS);
                if (task.getProgressPercentage() < 50) {
                    task.setProgressPercentage(50);
                }
                // Add marker for on site
                String onSiteNotes = task.getRiskNotes() != null ? task.getRiskNotes() : "";
                if (!onSiteNotes.contains("[OnSite]")) {
                    task.setRiskNotes(onSiteNotes + " [OnSite]");
                }
                break;
                
            case "Awaiting Materials":
                task.setStatus(Task.TaskStatus.BLOCKED);
                // Could prompt for what materials are needed
                showMaterialsDialog(task);
                break;
                
            case "Weather Hold":
                task.setStatus(Task.TaskStatus.BLOCKED);
                // Could log weather conditions
                showWeatherHoldDialog(task);
                break;
                
            case "In Review":
                task.setStatus(Task.TaskStatus.REVIEW);
                if (task.getProgressPercentage() < 90) {
                    task.setProgressPercentage(90);
                }
                break;
                
            case "Completed":
                task.setStatus(Task.TaskStatus.COMPLETED);
                task.setProgressPercentage(100);
                if (task.getActualEnd() == null) {
                    task.setActualEnd(LocalDate.now());
                }
                break;
                
            case "Blocked":
                task.setStatus(Task.TaskStatus.BLOCKED);
                showBlockedReasonDialog(task);
                break;
        }
        
        // Save to database
        taskRepository.update(task);
        
        // Refresh the board
        loadTasks();
        
        // Show notification
        showNotification("Task '" + task.getTitle() + "' moved to " + columnName);
    }
    
    private boolean validateStatusTransition(Task task, Task.TaskStatus oldStatus, 
                                            Task.TaskStatus newStatus, String columnName) {
        // Check for required fields before completion
        if (columnName.equals("Completed")) {
            if (task.getAssignedTo() == null) {
                showAlert("Cannot Complete", "Task must be assigned before completion.");
                return false;
            }
            // Check if all subtasks are completed
            List<Task> subtasks = allTasks.stream()
                .filter(t -> task.getId().equals(t.getParentTaskId()))
                .collect(Collectors.toList());
            
            long incompleteSubtasks = subtasks.stream()
                .filter(t -> t.getStatus() != Task.TaskStatus.COMPLETED)
                .count();
                
            if (incompleteSubtasks > 0) {
                showAlert("Cannot Complete", "All subtasks must be completed first. " +
                         incompleteSubtasks + " subtask(s) remaining.");
                return false;
            }
        }
        
        // Check dependencies before starting
        if (columnName.equals("In Progress") || columnName.equals("On Site")) {
            // Would check task dependencies here
        }
        
        return true;
    }
    
    private void loadTasks() {
        allTasks = FXCollections.observableArrayList(taskRepository.findByProjectId(project.getId()));
        
        // Clear all columns
        for (VBox container : columnContainers.values()) {
            container.getChildren().clear();
        }
        
        // Distribute tasks to columns
        Map<String, List<Task>> tasksByColumn = new HashMap<>();
        for (String column : COLUMN_NAMES) {
            tasksByColumn.put(column, new ArrayList<>());
        }
        
        for (Task task : allTasks) {
            String column = determineColumn(task);
            tasksByColumn.get(column).add(task);
        }
        
        // Add task cards to columns
        for (String columnName : COLUMN_NAMES) {
            VBox container = columnContainers.get(columnName);
            List<Task> tasks = tasksByColumn.get(columnName);
            
            // Sort tasks by priority and due date
            tasks.sort((a, b) -> {
                // First by priority
                if (a.getPriority() != b.getPriority()) {
                    if (a.getPriority() == null) return 1;
                    if (b.getPriority() == null) return -1;
                    return a.getPriority().compareTo(b.getPriority());
                }
                // Then by due date
                if (a.getPlannedEnd() != null && b.getPlannedEnd() != null) {
                    return a.getPlannedEnd().compareTo(b.getPlannedEnd());
                }
                return 0;
            });
            
            for (Task task : tasks) {
                container.getChildren().add(createTaskCard(task));
            }
            
            // Update count
            Label countLabel = (Label) stage.getScene().lookup("#" + columnName + "_count");
            if (countLabel != null) {
                countLabel.setText("(" + tasks.size() + ")");
            }
        }
    }
    
    private String determineColumn(Task task) {
        // Map tasks to appropriate columns based on status and metadata
        if (task.getStatus() == null) {
            return "Backlog";
        }
        
        // Check for special field service states
        if (task.getStatus() == Task.TaskStatus.BLOCKED) {
            // Check task notes or custom fields to determine specific hold reason
            if (task.getRiskNotes() != null) {
                String notes = task.getRiskNotes().toLowerCase();
                if (notes.contains("weather hold:") || notes.contains("[weather]")) {
                    return "Weather Hold";
                } else if (notes.contains("awaiting materials:") || notes.contains("[materials]")) {
                    return "Awaiting Materials";
                } else if (task.getRiskNotes().contains("[Ready]")) {
                    // Special marker to keep in Ready column despite BLOCKED status
                    return "Ready";
                }
            }
            return "Blocked";
        }
        
        switch (task.getStatus()) {
            case NOT_STARTED:
                // Check for Ready marker in notes or if start date is today/past
                if (task.getRiskNotes() != null && task.getRiskNotes().contains("[Ready]")) {
                    return "Ready";
                }
                if (task.getPlannedStart() != null && 
                    !task.getPlannedStart().isAfter(LocalDate.now())) {
                    return "Ready";
                }
                return "Backlog";
                
            case IN_PROGRESS:
                // Check for special markers
                if (task.getRiskNotes() != null && task.getRiskNotes().contains("[OnSite]")) {
                    return "On Site";
                }
                // Check for review status
                if (task.getProgressPercentage() >= 90) {
                    return "In Review";
                }
                // Check if on site (could use location or custom field)
                if (task.getLocation() != null && !task.getLocation().isEmpty() &&
                    task.getProgressPercentage() >= 50) {
                    return "On Site";
                }
                return "In Progress";
                
            case REVIEW:
                return "In Review";
                
            case COMPLETED:
                return "Completed";
                
            case CANCELLED:
                return "Backlog";
                
            default:
                return "Backlog";
        }
    }
    
    private void showMaterialsDialog(Task task) {
        // Extract existing materials info if present
        String existing = "";
        if (task.getRiskNotes() != null && task.getRiskNotes().contains("Awaiting materials:")) {
            int start = task.getRiskNotes().indexOf("Awaiting materials:") + 19;
            int end = task.getRiskNotes().indexOf("[", start);
            if (end == -1) end = task.getRiskNotes().length();
            existing = task.getRiskNotes().substring(start, end).trim();
        }
        
        // Create custom dialog with larger text area
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Materials Required");
        dialog.setHeaderText("Task on hold - Awaiting Materials\nTask: " + task.getTitle());
        dialog.setResizable(true);
        
        // Create text area for multi-line input - much larger
        TextArea textArea = new TextArea(existing);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(100);
        textArea.setMinWidth(800);
        textArea.setMinHeight(400);
        textArea.setWrapText(true);
        textArea.setPromptText("List the materials needed, expected delivery dates, suppliers, etc.");
        
        VBox content = new VBox(10);
        content.setMinWidth(850);
        content.setMinHeight(450);
        content.getChildren().addAll(new Label("What materials are needed?"), textArea);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(900);
        dialog.getDialogPane().setMinHeight(550);
        
        // Force the dialog window to be larger
        dialog.setWidth(950);
        dialog.setHeight(600);
        dialog.setOnShown(e -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setMinWidth(950);
            stage.setMinHeight(600);
        });
        
        // Add buttons
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Convert result
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return textArea.getText();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(materials -> {
            task.setEquipmentRequired(materials);
            // Always use the standard prefix for consistent detection
            task.setRiskNotes("Awaiting materials: " + materials + " [materials]");
            taskRepository.update(task);
        });
    }
    
    private void showWeatherHoldDialog(Task task) {
        // Extract existing weather info if present
        String existing = "";
        if (task.getRiskNotes() != null && task.getRiskNotes().contains("Weather hold:")) {
            int start = task.getRiskNotes().indexOf("Weather hold:") + 13;
            int end = task.getRiskNotes().indexOf("[", start);
            if (end == -1) end = task.getRiskNotes().length();
            existing = task.getRiskNotes().substring(start, end).trim();
        }
        
        // Create custom dialog with larger text area
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Weather Hold");
        dialog.setHeaderText("Task on hold due to weather\nTask: " + task.getTitle());
        dialog.setResizable(true);
        
        // Create text area for multi-line input - much larger
        TextArea textArea = new TextArea(existing);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(100);
        textArea.setMinWidth(800);
        textArea.setMinHeight(400);
        textArea.setWrapText(true);
        textArea.setPromptText("Describe weather conditions, expected duration, safety concerns, etc.");
        
        VBox content = new VBox(10);
        content.setMinWidth(850);
        content.setMinHeight(450);
        content.getChildren().addAll(new Label("Weather conditions and impact:"), textArea);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(900);
        dialog.getDialogPane().setMinHeight(550);
        
        // Force the dialog window to be larger
        dialog.setWidth(950);
        dialog.setHeight(600);
        dialog.setOnShown(e -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setMinWidth(950);
            stage.setMinHeight(600);
        });
        
        // Add buttons
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Convert result
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return textArea.getText();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(weather -> {
            // Always use the standard prefix for consistent detection
            task.setRiskNotes("Weather hold: " + weather + " [weather]");
            taskRepository.update(task);
        });
    }
    
    private void showBlockedReasonDialog(Task task) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Task Blocked");
        dialog.setHeaderText("Why is this task blocked?");
        dialog.setContentText("Reason:");
        
        dialog.showAndWait().ifPresent(reason -> {
            task.setRiskNotes("Blocked: " + reason);
            taskRepository.update(task);
        });
    }
    
    private void editTask(Task task) {
        // Would open task edit dialog
        showNotification("Edit task: " + task.getTitle());
    }
    
    private void showFilterDialog() {
        // Would show filter options
        showNotification("Filter dialog would open here");
    }
    
    private void switchToListView() {
        // Close kanban and open list view
        stage.close();
    }
    
    private void showStatistics() {
        // Calculate and show statistics
        Map<String, Long> stats = allTasks.stream()
            .collect(Collectors.groupingBy(
                t -> t.getStatus() != null ? t.getStatus().name() : "UNKNOWN",
                Collectors.counting()
            ));
        
        StringBuilder sb = new StringBuilder("Task Statistics:\n\n");
        stats.forEach((status, count) -> 
            sb.append(status).append(": ").append(count).append("\n"));
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Statistics");
        alert.setHeaderText("Project Task Statistics");
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }
    
    private void showNotification(String message) {
        // Would show a temporary notification
        System.out.println("Notification: " + message);
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
    
    private static class Circle extends javafx.scene.shape.Circle {
        public Circle(double radius) {
            super(radius);
        }
    }
    
    public void show() {
        stage.show();
    }
}