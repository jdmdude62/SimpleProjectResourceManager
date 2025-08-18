package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private ScrollPane mainScrollPane;
    private Timeline autoScrollTimeline;
    private Map<String, VBox> columnMap = new HashMap<>(); // Maps column name to its VBox container
    
    // Mouse-based drag state
    private Node draggedCard = null;
    private Task draggedTask = null;
    private double dragOffsetX, dragOffsetY;
    private Node dragPreview = null;
    private String sourceColumn = null;
    
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
        System.out.println("After initialize(), columnMap size: " + columnMap.size());
        System.out.println("columnMap contents: " + columnMap.keySet());
        loadTasks();
    }
    
    private void initialize() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // Header
        HBox header = createHeader();
        
        // Kanban columns
        columnsContainer = new HBox(15);  // Increased spacing between columns
        columnsContainer.setFillHeight(true);
        
        for (String columnName : COLUMN_NAMES) {
            VBox column = createKanbanColumn(columnName);
            columnsContainer.getChildren().add(column);
        }
        
        // Make columns scrollable horizontally if needed
        mainScrollPane = new ScrollPane(columnsContainer);
        mainScrollPane.setFitToHeight(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Setup auto-scroll on drag
        setupAutoScroll();
        
        // Main drop handler disabled - using individual column handlers
        // setupMainDropHandler();
        
        // Layout
        VBox.setVgrow(mainScrollPane, Priority.ALWAYS);
        root.getChildren().addAll(header, mainScrollPane);
        
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
        
        // Cleanup on close
        stage.setOnHidden(e -> {
            stopAutoScroll();
        });
    }
    
    private void setupMainDropHandler() {
        // Setup a single drop handler on the main container that determines the correct column
        columnsContainer.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
            }
        });
        
        columnsContainer.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            
            if (db.hasString()) {
                // Find which column the drop occurred in based on coordinates
                double dropX = e.getX();
                String targetColumn = null;
                
                for (String columnName : COLUMN_NAMES) {
                    VBox column = columnMap.get(columnName);
                    if (column != null) {
                        // Get column bounds in parent coordinates
                        double columnLeft = column.getLayoutX();
                        double columnRight = columnLeft + column.getWidth();
                        
                        if (dropX >= columnLeft && dropX <= columnRight) {
                            targetColumn = columnName;
                            break;
                        }
                    }
                }
                
                if (targetColumn != null) {
                    System.out.println("Drop detected at X=" + dropX + " in column: " + targetColumn);
                    
                    try {
                        Long taskId = Long.parseLong(db.getString());
                        Task task = allTasks.stream()
                            .filter(t -> t.getId().equals(taskId))
                            .findFirst()
                            .orElse(null);
                        
                        if (task != null) {
                            Task.TaskStatus newStatus = COLUMN_STATUS_MAP.get(targetColumn);
                            updateTaskStatus(task, newStatus, targetColumn);
                            success = true;
                        }
                    } catch (NumberFormatException ex) {
                        System.err.println("Invalid task ID: " + db.getString());
                    }
                } else {
                    System.out.println("Drop at X=" + dropX + " - no column found");
                }
            }
            
            e.setDropCompleted(success);
            e.consume();
        });
    }
    
    private void setupAutoScroll() {
        // Initialize the auto-scroll timeline
        autoScrollTimeline = new Timeline(new KeyFrame(Duration.millis(30), e -> {
            // This will be triggered during drag operations
        }));
        autoScrollTimeline.setCycleCount(Animation.INDEFINITE);
        
        // Setup drag event handlers for auto-scrolling
        // Use addEventHandler instead of addEventFilter to avoid interfering with drop detection
        mainScrollPane.addEventHandler(DragEvent.DRAG_OVER, e -> {
            handleAutoScroll(e);
            // Don't consume the event so it can propagate to columns
        });
        
        mainScrollPane.addEventHandler(DragEvent.DRAG_EXITED, e -> {
            stopAutoScroll();
            // Don't consume the event
        });
        
        // Don't handle DRAG_DROPPED at the ScrollPane level - let columns handle it
    }
    
    private void handleAutoScroll(DragEvent event) {
        // Get the bounds of the scroll pane
        Bounds bounds = mainScrollPane.getBoundsInLocal();
        double x = event.getX();
        double width = bounds.getWidth();
        
        // Define the edge zone for triggering auto-scroll (in pixels)
        double edgeSize = 150.0;  // Increased from 100 to make it easier to trigger
        
        // Calculate dynamic scroll speed based on distance from edge
        double scrollSpeedBase = 5.0;
        double maxScrollSpeed = 20.0;
        
        // Stop any existing auto-scroll
        autoScrollTimeline.stop();
        
        // Check if we're near the edges
        if (x < edgeSize) {
            // Near left edge - scroll left
            // Speed increases as we get closer to the edge
            double distanceRatio = (edgeSize - x) / edgeSize;
            double scrollSpeed = scrollSpeedBase + (maxScrollSpeed - scrollSpeedBase) * distanceRatio;
            
            autoScrollTimeline = new Timeline(new KeyFrame(Duration.millis(20), e -> {
                double currentH = mainScrollPane.getHvalue();
                double contentWidth = mainScrollPane.getContent().getBoundsInLocal().getWidth();
                double viewportWidth = mainScrollPane.getViewportBounds().getWidth();
                double scrollableWidth = contentWidth - viewportWidth;
                if (scrollableWidth > 0) {
                    double newH = Math.max(0, currentH - (scrollSpeed / scrollableWidth));
                    mainScrollPane.setHvalue(newH);
                }
            }));
            autoScrollTimeline.setCycleCount(Animation.INDEFINITE);
            autoScrollTimeline.play();
        } else if (x > width - edgeSize) {
            // Near right edge - scroll right
            // Speed increases as we get closer to the edge
            double distanceRatio = (x - (width - edgeSize)) / edgeSize;
            double scrollSpeed = scrollSpeedBase + (maxScrollSpeed - scrollSpeedBase) * distanceRatio;
            
            autoScrollTimeline = new Timeline(new KeyFrame(Duration.millis(20), e -> {
                double currentH = mainScrollPane.getHvalue();
                double contentWidth = mainScrollPane.getContent().getBoundsInLocal().getWidth();
                double viewportWidth = mainScrollPane.getViewportBounds().getWidth();
                double scrollableWidth = contentWidth - viewportWidth;
                if (scrollableWidth > 0) {
                    double newH = Math.min(1.0, currentH + (scrollSpeed / scrollableWidth));
                    mainScrollPane.setHvalue(newH);
                }
            }));
            autoScrollTimeline.setCycleCount(Animation.INDEFINITE);
            autoScrollTimeline.play();
        } else {
            // Not near edges, stop scrolling
            stopAutoScroll();
        }
    }
    
    private void createDragPreview(Node card) {
        // Make the original card semi-transparent
        card.setOpacity(0.3);
        
        // Create a snapshot of the card
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = card.snapshot(params, null);
        
        // Create an ImageView with the snapshot
        ImageView ghostImage = new ImageView(snapshot);
        ghostImage.setOpacity(0.7);
        ghostImage.setMouseTransparent(true); // So it doesn't interfere with mouse events
        
        // Create a container for the ghost that can be positioned
        dragPreview = new Group(ghostImage);
        dragPreview.setManaged(false); // Don't let it affect layout
        
        // Add the ghost to the root of the scene
        if (stage.getScene() != null && stage.getScene().getRoot() instanceof Pane) {
            Pane root = (Pane) stage.getScene().getRoot();
            root.getChildren().add(dragPreview);
        }
    }
    
    private void removeDragPreview() {
        // Remove the ghost image
        if (dragPreview != null && dragPreview.getParent() instanceof Pane) {
            ((Pane) dragPreview.getParent()).getChildren().remove(dragPreview);
            dragPreview = null;
        }
        
        // Reset opacity of the dragged card
        if (draggedCard != null) {
            draggedCard.setOpacity(1.0);
        }
    }
    
    private void updateDragPreview(double sceneX, double sceneY) {
        if (dragPreview != null) {
            // Position the ghost at the mouse location, offset by the original drag offset
            dragPreview.setLayoutX(sceneX - dragOffsetX);
            dragPreview.setLayoutY(sceneY - dragOffsetY);
        }
    }
    
    private void handleMouseDragAutoScroll(double mouseX) {
        double viewportWidth = mainScrollPane.getViewportBounds().getWidth();
        double edgeSize = 100.0;
        
        if (mouseX < edgeSize) {
            // Scroll left
            if (autoScrollTimeline != null) {
                autoScrollTimeline.stop();
            }
            autoScrollTimeline = new Timeline(new KeyFrame(Duration.millis(20), e -> {
                double currentH = mainScrollPane.getHvalue();
                mainScrollPane.setHvalue(Math.max(0, currentH - 0.02));
            }));
            autoScrollTimeline.setCycleCount(Animation.INDEFINITE);
            autoScrollTimeline.play();
        } else if (mouseX > viewportWidth - edgeSize) {
            // Scroll right
            if (autoScrollTimeline != null) {
                autoScrollTimeline.stop();
            }
            autoScrollTimeline = new Timeline(new KeyFrame(Duration.millis(20), e -> {
                double currentH = mainScrollPane.getHvalue();
                mainScrollPane.setHvalue(Math.min(1.0, currentH + 0.02));
            }));
            autoScrollTimeline.setCycleCount(Animation.INDEFINITE);
            autoScrollTimeline.play();
        } else {
            stopAutoScroll();
        }
    }
    
    private void highlightTargetColumn(double mouseX) {
        String targetColumn = findColumnAtPosition(mouseX);
        clearColumnHighlights();
        
        if (targetColumn != null && columnContainers.containsKey(targetColumn)) {
            VBox container = columnContainers.get(targetColumn);
            container.setStyle(container.getStyle() + "; -fx-background-color: #e0f0ff;");
        }
    }
    
    private void highlightTargetColumnDirect(double containerX) {
        clearColumnHighlights();
        
        // Find column directly using container coordinates
        for (String columnName : COLUMN_NAMES) {
            VBox column = columnMap.get(columnName);
            if (column != null) {
                Bounds bounds = column.getBoundsInParent();
                if (containerX >= bounds.getMinX() && containerX <= bounds.getMaxX()) {
                    VBox container = columnContainers.get(columnName);
                    if (container != null) {
                        container.setStyle(container.getStyle() + "; -fx-background-color: #e0f0ff;");
                    }
                    break;
                }
            }
        }
    }
    
    private void clearColumnHighlights() {
        for (VBox container : columnContainers.values()) {
            container.setStyle(container.getStyle().replace("; -fx-background-color: #e0f0ff;", ""));
        }
    }
    
    private String findColumnAtPosition(double x) {
        // Adjust x for scroll position
        double scrollX = x + mainScrollPane.getHvalue() * 
            (columnsContainer.getWidth() - mainScrollPane.getViewportBounds().getWidth());
        
        System.out.println("=== findColumnAtPosition Debug ===");
        System.out.println("Input X: " + x);
        System.out.println("Scroll H-value: " + mainScrollPane.getHvalue());
        System.out.println("Container width: " + columnsContainer.getWidth());
        System.out.println("Viewport width: " + mainScrollPane.getViewportBounds().getWidth());
        System.out.println("Adjusted scrollX: " + scrollX);
        System.out.println("columnMap size: " + columnMap.size());
        System.out.println("columnMap keys: " + columnMap.keySet());
        
        for (String columnName : COLUMN_NAMES) {
            VBox column = columnMap.get(columnName);
            if (column != null) {
                Bounds bounds = column.getBoundsInParent();
                System.out.println("Column " + columnName + " bounds: " + bounds.getMinX() + " - " + bounds.getMaxX());
                if (scrollX >= bounds.getMinX() && scrollX <= bounds.getMaxX()) {
                    System.out.println("==> FOUND COLUMN: " + columnName);
                    return columnName;
                }
            } else {
                System.out.println("Column " + columnName + " not found in columnMap!");
            }
        }
        System.out.println("==> NO COLUMN FOUND");
        return null;
    }
    
    private void stopAutoScroll() {
        if (autoScrollTimeline != null) {
            autoScrollTimeline.stop();
        }
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
        
        Button ganttBtn = new Button("📊 Gantt Chart");
        ganttBtn.setOnAction(e -> showGanttChart());
        
        Button statsBtn = new Button("📈 Statistics");
        statsBtn.setOnAction(e -> showStatistics());
        
        header.getChildren().addAll(projectInfo, spacer, refreshBtn, filterBtn, listViewBtn, ganttBtn, statsBtn);
        return header;
    }
    
    private VBox createKanbanColumn(String columnName) {
        VBox column = new VBox(10);
        column.setMinWidth(220);  // Increased minimum width
        column.setPrefWidth(260);  // Increased preferred width
        column.setMaxWidth(320);  // Increased maximum width
        column.setStyle("-fx-background-color: #f8f8f8; -fx-background-radius: 5; -fx-padding: 10;");
        
        // Store the column for later reference
        columnMap.put(columnName, column);
        System.out.println("Registered column '" + columnName + "' in columnMap");
        
        // Column header
        HBox columnHeader = new HBox(10);
        columnHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(columnName);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        Label countLabel = new Label("(0)");
        countLabel.setStyle("-fx-text-fill: #666;");
        countLabel.setId(columnName + "_count");
        
        columnHeader.getChildren().addAll(titleLabel, countLabel);
        
        // Task container - make it scrollable within itself
        ScrollPane scrollPane = new ScrollPane();
        VBox taskContainer = new VBox(8);
        taskContainer.setStyle("-fx-padding: 5;");
        taskContainer.setMinHeight(650);
        taskContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        taskContainer.setFillWidth(true);
        taskContainer.setMaxHeight(Double.MAX_VALUE);
        
        scrollPane.setContent(taskContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefHeight(700);
        scrollPane.setMinHeight(650);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        
        columnContainers.put(columnName, taskContainer);
        columnScrollPanes.put(columnName, scrollPane);
        System.out.println("Registered containers for column '" + columnName + "'");
        
        // Setup drop handling directly on each column's scroll pane
        scrollPane.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        
        scrollPane.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            
            if (db.hasString()) {
                try {
                    Long taskId = Long.parseLong(db.getString());
                    System.out.println("Direct drop in column: " + columnName);
                    
                    Task task = allTasks.stream()
                        .filter(t -> t.getId().equals(taskId))
                        .findFirst()
                        .orElse(null);
                    
                    if (task != null) {
                        Task.TaskStatus newStatus = COLUMN_STATUS_MAP.get(columnName);
                        updateTaskStatus(task, newStatus, columnName);
                        success = true;
                    }
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid task ID: " + db.getString());
                }
            }
            
            e.setDropCompleted(success);
            e.consume();
        });
        
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
        // Mouse-based drag implementation
        card.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                draggedCard = card;
                draggedTask = (Task) card.getUserData();
                dragOffsetX = e.getX();
                dragOffsetY = e.getY();
                
                // Find source column
                sourceColumn = null;
                for (Map.Entry<String, VBox> entry : columnContainers.entrySet()) {
                    if (entry.getValue().getChildren().contains(card)) {
                        sourceColumn = entry.getKey();
                        System.out.println("Found card in columnContainers: " + entry.getKey());
                        break;
                    }
                }
                
                // If not found in columnContainers, might be in a different state
                if (sourceColumn == null) {
                    System.out.println("WARNING: Card not found in any columnContainer!");
                    // Try to determine from task status
                    sourceColumn = determineColumn(draggedTask);
                    System.out.println("Determined source column from task: " + sourceColumn);
                }
                
                System.out.println("Mouse pressed on task: " + draggedTask.getTitle() + " in column: " + sourceColumn);
                
                // Create drag preview
                createDragPreview(card);
                
                e.consume();
            }
        });
        
        card.setOnMouseDragged(e -> {
            if (draggedCard != null) {
                // Get mouse position relative to scroll pane for auto-scroll
                Point2D localPoint = mainScrollPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                
                // Get mouse position relative to columns container for column detection
                Point2D containerPoint = columnsContainer.sceneToLocal(e.getSceneX(), e.getSceneY());
                
                System.out.println("Mouse dragged - Scene: (" + e.getSceneX() + ", " + e.getSceneY() + 
                                   ") ScrollPane: (" + localPoint.getX() + ", " + localPoint.getY() + 
                                   ") Container: (" + containerPoint.getX() + ", " + containerPoint.getY() + ")");
                
                // Update ghost position to follow mouse
                updateDragPreview(e.getSceneX(), e.getSceneY());
                
                // Handle auto-scroll using scroll pane coordinates
                handleMouseDragAutoScroll(localPoint.getX());
                
                // Highlight target column using container coordinates
                highlightTargetColumnDirect(containerPoint.getX());
                
                e.consume();
            }
        });
        
        card.setOnMouseReleased(e -> {
            if (draggedCard != null) {
                // Use a different approach - convert to columnsContainer coordinates
                Point2D scenePoint = new Point2D(e.getSceneX(), e.getSceneY());
                Point2D containerPoint = columnsContainer.sceneToLocal(scenePoint);
                
                System.out.println("\n=== MOUSE RELEASED ===");
                System.out.println("Scene coordinates: (" + e.getSceneX() + ", " + e.getSceneY() + ")");
                System.out.println("Container coordinates: (" + containerPoint.getX() + ", " + containerPoint.getY() + ")");
                System.out.println("Source column: " + sourceColumn);
                
                // Find column directly using container coordinates
                String targetColumn = null;
                for (String columnName : COLUMN_NAMES) {
                    VBox column = columnMap.get(columnName);
                    if (column != null) {
                        Bounds bounds = column.getBoundsInParent();
                        System.out.println("Checking " + columnName + " bounds: " + bounds.getMinX() + " - " + bounds.getMaxX() + " against X: " + containerPoint.getX());
                        if (containerPoint.getX() >= bounds.getMinX() && containerPoint.getX() <= bounds.getMaxX()) {
                            targetColumn = columnName;
                            System.out.println("==> DROP TARGET: " + columnName);
                            break;
                        }
                    }
                }
                
                System.out.println("Target column found: " + targetColumn);
                System.out.println("Source column is: " + sourceColumn);
                System.out.println("Columns are equal? " + (targetColumn != null && targetColumn.equals(sourceColumn)));
                System.out.println("Columns are different? " + (targetColumn != null && !targetColumn.equals(sourceColumn)));
                
                if (targetColumn != null && !targetColumn.equals(sourceColumn)) {
                    System.out.println("ATTEMPTING TO DROP - Task: " + draggedTask.getTitle() + " from " + sourceColumn + " to " + targetColumn);
                    Task.TaskStatus newStatus = COLUMN_STATUS_MAP.get(targetColumn);
                    System.out.println("New status will be: " + newStatus);
                    updateTaskStatus(draggedTask, newStatus, targetColumn);
                } else {
                    System.out.println("DROP CANCELLED - Target: " + targetColumn + ", Source: " + sourceColumn);
                }
                
                // Clean up
                removeDragPreview();
                clearColumnHighlights();
                draggedCard = null;
                draggedTask = null;
                sourceColumn = null;
                stopAutoScroll();
                
                e.consume();
            }
        });
    }
    
    private String currentDropTarget = null;
    
    private void setupColumnDragAndDrop(Node container, String columnName) {
        // Get the actual task container (VBox) from the ScrollPane
        VBox taskContainer = columnContainers.get(columnName);
        
        container.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
            }
        });
        
        container.setOnDragEntered(e -> {
            if (e.getDragboard().hasString()) {
                System.out.println("Drag entered column: " + columnName);
                currentDropTarget = columnName;
                // Visual feedback - highlight the task container
                if (taskContainer != null && !taskContainer.getStyle().contains("#e0f0ff")) {
                    taskContainer.setStyle(taskContainer.getStyle() + "; -fx-background-color: #e0f0ff;");
                }
                e.consume();
            }
        });
        
        container.setOnDragExited(e -> {
            System.out.println("Drag exited column: " + columnName);
            // Reset visual feedback
            if (taskContainer != null) {
                taskContainer.setStyle(taskContainer.getStyle().replace("; -fx-background-color: #e0f0ff;", ""));
            }
            e.consume();
        });
        
        container.setOnDragDropped(e -> {
            System.out.println("Drop detected in column: " + columnName);
            System.out.println("  Dragboard has string: " + e.getDragboard().hasString());
            
            // Always process the drop if we have a valid dragboard
            if (!e.getDragboard().hasString()) {
                System.out.println("  No string in dragboard, rejecting drop");
                e.setDropCompleted(false);
                e.consume();
                return;
            }
            
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
            
            // Clear the drop target after handling
            currentDropTarget = null;
        });
    }
    
    private void updateTaskStatus(Task task, Task.TaskStatus newStatus, String columnName) {
        Task.TaskStatus oldStatus = task.getStatus();
        System.out.println("\n=== updateTaskStatus Called ===");
        System.out.println("Task: '" + task.getTitle() + "'");
        System.out.println("Task ID: " + task.getId());
        System.out.println("Old status: " + oldStatus);
        System.out.println("New status requested: " + newStatus);
        System.out.println("Target column: " + columnName);
        
        // Validation rules
        if (!validateStatusTransition(task, oldStatus, newStatus, columnName)) {
            System.out.println("VALIDATION FAILED - Status transition not allowed");
            return;
        }
        
        // Update task based on new column
        task.setStatus(newStatus);
        System.out.println("Status updated to: " + newStatus);
        
        // Field service specific updates
        switch (columnName) {
            case "Backlog":
                // Moving back to backlog - clean up all markers
                System.out.println("Moving to BACKLOG - cleaning all markers");
                System.out.println("Before: RiskNotes = " + task.getRiskNotes());
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
                System.out.println("After: RiskNotes = " + task.getRiskNotes());
                task.setStatus(Task.TaskStatus.NOT_STARTED);
                task.setProgressPercentage(0);
                break;
                
            case "Ready":
                // Task is ready to be worked on
                // Clean up OnSite marker and add Ready marker
                String notes = task.getRiskNotes() != null ? task.getRiskNotes() : "";
                notes = notes.replace("[OnSite]", "").trim();
                if (!notes.contains("[Ready]")) {
                    notes = (notes + " [Ready]").trim();
                }
                task.setRiskNotes(notes);
                task.setStatus(Task.TaskStatus.NOT_STARTED); // Explicitly set status
                break;
                
            case "In Progress":
                // Remove Ready and OnSite markers if present
                if (task.getRiskNotes() != null) {
                    String cleanedNotes = task.getRiskNotes()
                        .replace("[Ready]", "")
                        .replace("[OnSite]", "")
                        .trim();
                    task.setRiskNotes(cleanedNotes.isEmpty() ? null : cleanedNotes);
                }
                if (task.getActualStart() == null) {
                    task.setActualStart(LocalDate.now());
                }
                // Set progress to a range that keeps it in "In Progress" (not 0, but less than 90)
                if (task.getProgressPercentage() == 0 || task.getProgressPercentage() >= 90) {
                    task.setProgressPercentage(25);
                }
                // Clear location to prevent auto-routing to "On Site"
                task.setLocation(null);
                task.setStatus(Task.TaskStatus.IN_PROGRESS);
                break;
                
            case "On Site":
                // Technician is on site
                task.setStatus(Task.TaskStatus.IN_PROGRESS);
                if (task.getProgressPercentage() < 50) {
                    task.setProgressPercentage(50);
                }
                // Remove Ready marker and add OnSite marker
                String onSiteNotes = task.getRiskNotes() != null ? task.getRiskNotes() : "";
                onSiteNotes = onSiteNotes.replace("[Ready]", "").trim();
                if (!onSiteNotes.contains("[OnSite]")) {
                    onSiteNotes = (onSiteNotes + " [OnSite]").trim();
                }
                task.setRiskNotes(onSiteNotes);
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
        System.out.println("Saving to database - Task: " + task.getTitle());
        System.out.println("  Final Status: " + task.getStatus());
        System.out.println("  Final RiskNotes: " + task.getRiskNotes());
        taskRepository.update(task);
        
        // Verify the save
        taskRepository.findById(task.getId()).ifPresent(verifyTask -> {
            System.out.println("Verified from DB - Status: " + verifyTask.getStatus() + ", RiskNotes: " + verifyTask.getRiskNotes());
        });
        
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
        System.out.println("\n=== loadTasks() called ===");
        allTasks = FXCollections.observableArrayList(taskRepository.findByProjectId(project.getId()));
        System.out.println("Loaded " + allTasks.size() + " tasks from database");
        
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
        System.out.println("determineColumn for task: " + task.getTitle());
        System.out.println("  Status: " + task.getStatus());
        System.out.println("  RiskNotes: " + task.getRiskNotes());
        
        // Map tasks to appropriate columns based on status and metadata
        if (task.getStatus() == null) {
            System.out.println("  -> Backlog (null status)");
            return "Backlog";
        }
        
        // Check for special field service states
        if (task.getStatus() == Task.TaskStatus.BLOCKED) {
            // Check task notes or custom fields to determine specific hold reason
            if (task.getRiskNotes() != null) {
                String notes = task.getRiskNotes().toLowerCase();
                if (notes.contains("weather hold:") || notes.contains("[weather]")) {
                    System.out.println("  -> Weather Hold");
                    return "Weather Hold";
                } else if (notes.contains("awaiting materials:") || notes.contains("[materials]")) {
                    System.out.println("  -> Awaiting Materials");
                    return "Awaiting Materials";
                } else if (task.getRiskNotes().contains("[Ready]")) {
                    // Special marker to keep in Ready column despite BLOCKED status
                    System.out.println("  -> Ready (has [Ready] marker with BLOCKED)");
                    return "Ready";
                }
            }
            System.out.println("  -> Blocked");
            return "Blocked";
        }
        
        switch (task.getStatus()) {
            case NOT_STARTED:
                // Check for Ready marker in notes ONLY - don't auto-move based on dates
                if (task.getRiskNotes() != null && task.getRiskNotes().contains("[Ready]")) {
                    System.out.println("  -> Ready (has [Ready] marker)");
                    return "Ready";
                }
                // Removed automatic date-based Ready placement to allow manual control
                System.out.println("  -> Backlog (NOT_STARTED, no [Ready] marker)");
                return "Backlog";
                
            case IN_PROGRESS:
                // Check for special markers
                if (task.getRiskNotes() != null && task.getRiskNotes().contains("[OnSite]")) {
                    System.out.println("  -> On Site (has [OnSite] marker)");
                    return "On Site";
                }
                // Check for review status
                System.out.println("  Progress: " + task.getProgressPercentage() + "%");
                if (task.getProgressPercentage() >= 90) {
                    System.out.println("  -> In Review (progress >= 90%)");
                    return "In Review";
                }
                // Check if on site (could use location or custom field)
                System.out.println("  Location: " + task.getLocation());
                if (task.getLocation() != null && !task.getLocation().isEmpty() &&
                    task.getProgressPercentage() >= 50) {
                    System.out.println("  -> On Site (has location and progress >= 50%)");
                    return "On Site";
                }
                System.out.println("  -> In Progress");
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
    
    private void showGanttChart() {
        // Open Gantt Chart view
        GanttChartView ganttChart = new GanttChartView(project, taskRepository, resources);
        ganttChart.show();
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