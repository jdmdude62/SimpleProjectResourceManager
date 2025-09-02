package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.model.TaskDependency;
import com.subliminalsearch.simpleprojectresourcemanager.model.TaskDependency.DependencyType;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskDependencyRepository;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import com.subliminalsearch.simpleprojectresourcemanager.util.HelpButton;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class GanttChartView {
    private final Project project;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final List<Resource> resources;
    private final Stage stage;
    
    // UI Components
    private ScrollPane mainScrollPane;
    private GridPane ganttGrid;
    private VBox taskListPanel;
    private HBox timelineHeader;
    private Pane ganttCanvas;
    private ComboBox<String> zoomComboBox;
    private CheckBox showDependenciesCheckBox;
    private CheckBox showCriticalPathCheckBox;
    private CheckBox showProgressCheckBox;
    private Label statusLabel;
    
    // Data
    private ObservableList<Task> tasks;
    private Map<Long, TaskBar> taskBars = new HashMap<>();
    private Map<Long, List<TaskDependency>> taskDependencies = new HashMap<>(); // Task ID -> List of dependencies
    private List<TaskDependency> dependencies = new ArrayList<>();
    private Set<Long> criticalPath = new HashSet<>();
    
    // View settings
    private LocalDate viewStartDate;
    private LocalDate viewEndDate;
    private double dayWidth = 50; // pixels per day (Day view default)
    private double taskHeight = 35; // height of each task row
    private double headerHeight = 60; // height of timeline header
    private int zoomLevel = 0; // 0=Day, 1=Week, 2=Month (default to Day view)
    
    // Colors - Improved scheme for better contrast and visibility
    private static final Color COLOR_TASK_NORMAL = Color.web("#0066CC");  // Darker blue
    private static final Color COLOR_TASK_CRITICAL = Color.web("#DC3545");
    private static final Color COLOR_TASK_COMPLETE = Color.web("#28A745");
    private static final Color COLOR_TASK_PROGRESS = Color.web("#FFC107");  // Amber for progress
    private static final Color COLOR_MILESTONE = Color.web("#F39C12");
    private static final Color COLOR_DEPENDENCY = Color.web("#6C757D");
    private static final Color COLOR_TODAY = Color.web("#E67E22");
    private static final Color COLOR_WEEKEND = Color.web("#ECF0F1");
    private static final Color COLOR_GRID = Color.web("#BDC3C7");
    
    // Interaction state
    private TaskBar draggedTaskBar = null;
    private boolean isDraggingStart = false;
    private boolean isDraggingEnd = false;
    private double dragStartX = 0;
    
    public GanttChartView(Project project, TaskRepository taskRepository, List<Resource> resources) {
        this(project, taskRepository, null, resources, null);
    }
    
    public GanttChartView(Project project, TaskRepository taskRepository, TaskDependencyRepository dependencyRepository, List<Resource> resources) {
        this(project, taskRepository, dependencyRepository, resources, null);
    }
    
    public GanttChartView(Project project, TaskRepository taskRepository, TaskDependencyRepository dependencyRepository, List<Resource> resources, javafx.stage.Window owner) {
        this.project = project;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.resources = resources;
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.setTitle("Gantt Chart - " + project.getProjectId());
        
        if (owner != null) {
            this.stage.initOwner(owner);
        }
        
        initialize(owner);
        loadTasks();
    }
    
    private void initialize(javafx.stage.Window owner) {
        BorderPane root = new BorderPane();
        root.setPrefSize(1200, 700);
        
        // Top toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Main content area
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.3);
        
        // Left panel - Task list
        taskListPanel = createTaskListPanel();
        taskListPanel.setMinWidth(200);
        taskListPanel.setPrefWidth(350);
        ScrollPane taskScrollPane = new ScrollPane(taskListPanel);
        taskScrollPane.setFitToWidth(true);
        taskScrollPane.setMinWidth(200);
        
        // Right panel - Gantt chart
        VBox ganttContainer = new VBox();
        timelineHeader = createTimelineHeader();
        ganttCanvas = createGanttCanvas();
        
        ScrollPane ganttScrollPane = new ScrollPane(ganttCanvas);
        ganttScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ganttScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ganttScrollPane.setFitToHeight(true);
        
        // Synchronize vertical scrolling
        taskScrollPane.vvalueProperty().bindBidirectional(ganttScrollPane.vvalueProperty());
        
        // Put timeline header in its own scroll pane that syncs with gantt horizontal scroll
        ScrollPane headerScrollPane = new ScrollPane(timelineHeader);
        headerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        headerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        headerScrollPane.setPrefHeight(headerHeight);
        headerScrollPane.setMinHeight(headerHeight);
        headerScrollPane.setMaxHeight(headerHeight);
        
        // Bind horizontal scrolling
        headerScrollPane.hvalueProperty().bind(ganttScrollPane.hvalueProperty());
        
        ganttContainer.getChildren().addAll(headerScrollPane, ganttScrollPane);
        VBox.setVgrow(ganttScrollPane, Priority.ALWAYS);
        
        splitPane.getItems().addAll(taskScrollPane, ganttContainer);
        
        root.setCenter(splitPane);
        
        // Bottom status bar
        statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(5));
        root.setBottom(statusLabel);
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/gantt-chart.css").toExternalForm());
        stage.setScene(scene);
        
        // Position on the same screen as owner
        if (owner != null) {
            DialogUtils.positionStageOnOwnerScreen(stage, owner, 0.9, 0.85);
        } else {
            stage.setWidth(1400);
            stage.setHeight(800);
            stage.centerOnScreen();
        }
        
        mainScrollPane = ganttScrollPane;
        
        // Setup keyboard shortcuts
        setupKeyboardShortcuts(scene);
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        
        // Zoom controls
        Label zoomLabel = new Label("Zoom:");
        zoomComboBox = new ComboBox<>();
        zoomComboBox.getItems().addAll("Day View", "Week View", "Month View", "Quarter View");
        zoomComboBox.getSelectionModel().select(0); // Default to Day View
        zoomComboBox.setOnAction(e -> updateZoomLevel());
        
        // View options
        showDependenciesCheckBox = new CheckBox("Show Dependencies");
        showDependenciesCheckBox.setSelected(true);
        showDependenciesCheckBox.setOnAction(e -> refreshGanttChart());
        
        showCriticalPathCheckBox = new CheckBox("Critical Path");
        showCriticalPathCheckBox.setOnAction(e -> {
            if (showCriticalPathCheckBox.isSelected()) {
                calculateCriticalPath();
            } else {
                // Clear critical path when unchecked
                criticalPath.clear();
            }
            refreshGanttChart();
        });
        
        showProgressCheckBox = new CheckBox("Show Progress");
        showProgressCheckBox.setSelected(true);
        showProgressCheckBox.setOnAction(e -> refreshGanttChart());
        
        // Navigation buttons
        Button todayButton = new Button("Today");
        todayButton.setOnAction(e -> scrollToToday());
        
        Button fitButton = new Button("Fit to Window");
        fitButton.setOnAction(e -> fitToWindow());
        
        // Export button
        Button exportButton = new Button("Export");
        exportButton.setOnAction(e -> exportChart());
        
        // Refresh button
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadTasks());
        
        // Add Dependency button
        Button addDependencyButton = new Button("Add Dependency");
        addDependencyButton.setOnAction(e -> showCreateDependencyDialog());
        addDependencyButton.setDisable(dependencyRepository == null);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Create help button
        Button helpButton = HelpButton.create(
            "Gantt Chart Help",
            "**Gantt Chart View**\n\n" +
            "**Controls:**\n" +
            "• **Zoom:** Change time scale (Day/Week/Month/Quarter)\n" +
            "• **Dependencies:** Show/hide task dependencies\n" +
            "• **Critical Path:** Highlight critical path tasks\n" +
            "• **Progress:** Show/hide task completion status\n\n" +
            "**Interactions:**\n" +
            "• **Drag tasks** to reschedule\n" +
            "• **Resize task bars** to change duration\n" +
            "• **Click tasks** to view details\n" +
            "• **Scroll** to navigate timeline\n" +
            "• **Double-click** to edit task\n\n" +
            "**Task Dependencies:**\n" +
            "• **FS:** Finish-to-Start (default)\n" +
            "• **FF:** Finish-to-Finish\n" +
            "• **SS:** Start-to-Start\n" +
            "• **SF:** Start-to-Finish\n\n" +
            "**Colors:**\n" +
            "• **Blue:** Normal tasks\n" +
            "• **Red:** Critical path tasks\n" +
            "• **Green:** Completed tasks\n" +
            "• **Orange:** Milestones\n" +
            "• **Gray:** Dependencies"
        );
        
        toolbar.getChildren().addAll(
            zoomLabel, zoomComboBox,
            new Separator(),
            showDependenciesCheckBox,
            showCriticalPathCheckBox,
            showProgressCheckBox,
            new Separator(),
            addDependencyButton,
            todayButton,
            fitButton,
            new Separator(),
            exportButton,
            refreshButton,
            spacer,
            helpButton
        );
        
        return toolbar;
    }
    
    private VBox createTaskListPanel() {
        VBox panel = new VBox();
        panel.setSpacing(0);
        panel.setPadding(new Insets(0));
        panel.setStyle("-fx-background-color: white;");
        
        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        header.setPrefHeight(headerHeight);
        header.setMinHeight(headerHeight);
        header.setMaxHeight(headerHeight);
        
        Label headerLabel = new Label("Tasks");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        header.getChildren().add(headerLabel);
        
        panel.getChildren().add(header);
        
        return panel;
    }
    
    private HBox createTimelineHeader() {
        HBox header = new HBox();
        header.setPrefHeight(headerHeight);
        header.setMinHeight(headerHeight);
        header.setMaxHeight(headerHeight);
        header.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        
        return header;
    }
    
    private Pane createGanttCanvas() {
        Pane canvas = new Pane();
        canvas.setStyle("-fx-background-color: white;");
        canvas.setPrefWidth(1800); // Initial width for proper rendering
        canvas.setPrefHeight(400); // Initial height
        
        // Handle mouse events for interaction
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnMouseMoved(this::handleMouseMoved);
        
        return canvas;
    }
    
    private void loadTasks() {
        tasks = FXCollections.observableArrayList(taskRepository.findByProjectId(project.getId()));
        
        // Load dependencies if repository is available
        if (dependencyRepository != null) {
            dependencies = dependencyRepository.findByProjectId(project.getId());
        }
        
        // Sort tasks by start date, then by ID for consistent ordering
        tasks.sort((a, b) -> {
            LocalDate aStart = a.getPlannedStart() != null ? a.getPlannedStart() : LocalDate.now();
            LocalDate bStart = b.getPlannedStart() != null ? b.getPlannedStart() : LocalDate.now();
            int dateComp = aStart.compareTo(bStart);
            return dateComp != 0 ? dateComp : a.getId().compareTo(b.getId());
        });
        
        // Calculate view date range
        calculateDateRange();
        
        // Load dependencies
        loadDependencies();
        
        // Update canvas size first
        long days = ChronoUnit.DAYS.between(viewStartDate, viewEndDate);
        ganttCanvas.setPrefWidth(days * dayWidth);
        ganttCanvas.setPrefHeight(Math.max(tasks.size() * taskHeight + 20, 400));
        
        // Refresh the display
        refreshDisplay();
    }
    
    private void calculateDateRange() {
        LocalDate minDate = LocalDate.now();
        LocalDate maxDate = LocalDate.now().plusMonths(1);
        
        for (Task task : tasks) {
            if (task.getPlannedStart() != null && task.getPlannedStart().isBefore(minDate)) {
                minDate = task.getPlannedStart();
            }
            if (task.getPlannedEnd() != null && task.getPlannedEnd().isAfter(maxDate)) {
                maxDate = task.getPlannedEnd();
            }
            // Also check actual dates
            if (task.getActualStart() != null && task.getActualStart().isBefore(minDate)) {
                minDate = task.getActualStart();
            }
            if (task.getActualEnd() != null && task.getActualEnd().isAfter(maxDate)) {
                maxDate = task.getActualEnd();
            }
        }
        
        // Add some padding
        viewStartDate = minDate.minusWeeks(1);
        viewEndDate = maxDate.plusWeeks(2);
    }
    
    private void loadDependencies() {
        taskDependencies.clear();
        
        // Load from dependency repository if available
        if (dependencyRepository != null) {
            List<TaskDependency> allDeps = dependencyRepository.findByProjectId(project.getId());
            for (TaskDependency dep : allDeps) {
                taskDependencies.computeIfAbsent(dep.getPredecessorId(), k -> new ArrayList<>()).add(dep);
            }
        } else {
            // Fallback: Load from parent-child relationships
            for (Task task : tasks) {
                if (task.getParentTaskId() != null) {
                    // Create a default finish-to-start dependency
                    TaskDependency dep = new TaskDependency(task.getParentTaskId(), task.getId());
                    taskDependencies.computeIfAbsent(task.getParentTaskId(), k -> new ArrayList<>()).add(dep);
                }
            }
        }
    }
    
    private void refreshDisplay() {
        // Clear existing display
        taskListPanel.getChildren().removeIf(node -> !(node instanceof HBox && 
            ((HBox)node).getChildren().stream().anyMatch(child -> child instanceof Label && 
            ((Label)child).getText().equals("Tasks"))));
        
        ganttCanvas.getChildren().clear();
        taskBars.clear();
        
        // Update timeline header
        updateTimelineHeader();
        
        // Draw grid
        drawGrid();
        
        // Draw today line
        drawTodayLine();
        
        // Add tasks
        int row = 0;
        for (Task task : tasks) {
            addTaskRow(task, row);
            row++;
        }
        
        // Draw dependencies
        if (showDependenciesCheckBox.isSelected()) {
            drawDependencies();
        }
        
        // Update canvas size
        long days = ChronoUnit.DAYS.between(viewStartDate, viewEndDate);
        ganttCanvas.setPrefWidth(days * dayWidth);
        ganttCanvas.setPrefHeight(tasks.size() * taskHeight + 20);
    }
    
    private void updateTimelineHeader() {
        timelineHeader.getChildren().clear();
        
        long days = ChronoUnit.DAYS.between(viewStartDate, viewEndDate);
        Canvas headerCanvas = new Canvas(days * dayWidth, headerHeight);
        GraphicsContext gc = headerCanvas.getGraphicsContext2D();
        
        // Draw based on zoom level
        if (zoomLevel == 0) { // Day view
            drawDayHeaders(gc);
        } else if (zoomLevel == 1) { // Week view
            drawWeekHeaders(gc);
        } else if (zoomLevel == 2) { // Month view
            drawMonthHeaders(gc);
        } else { // Quarter view
            drawQuarterHeaders(gc);
        }
        
        timelineHeader.getChildren().add(headerCanvas);
    }
    
    private void drawMonthHeaders(GraphicsContext gc) {
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.setFill(Color.BLACK);
        gc.setStroke(COLOR_GRID);
        gc.setLineWidth(0.5);
        
        LocalDate current = viewStartDate.withDayOfMonth(1);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        
        while (current.isBefore(viewEndDate)) {
            LocalDate monthEnd = current.plusMonths(1).minusDays(1);
            if (monthEnd.isAfter(viewEndDate)) {
                monthEnd = viewEndDate;
            }
            
            long startDay = ChronoUnit.DAYS.between(viewStartDate, current);
            long endDay = ChronoUnit.DAYS.between(viewStartDate, monthEnd);
            
            double x = Math.max(0, startDay * dayWidth);
            double width = (endDay - startDay + 1) * dayWidth;
            
            // Draw month header
            gc.setFill(Color.web("#f8f9fa"));
            gc.fillRect(x, 0, width, 30);
            
            gc.setFill(Color.BLACK);
            String monthText = current.format(monthFormatter);
            gc.fillText(monthText, x + 5, 20);
            
            // Draw vertical line
            gc.strokeLine(x, 0, x, headerHeight);
            
            // Draw week numbers
            LocalDate weekStart = current;
            while (weekStart.isBefore(monthEnd)) {
                WeekFields weekFields = WeekFields.of(Locale.getDefault());
                int weekNumber = weekStart.get(weekFields.weekOfWeekBasedYear());
                
                long weekStartDay = ChronoUnit.DAYS.between(viewStartDate, weekStart);
                double weekX = weekStartDay * dayWidth;
                
                gc.setFont(Font.font("System", 10));
                gc.fillText("W" + weekNumber, weekX + 2, 45);
                
                weekStart = weekStart.plusWeeks(1);
            }
            
            current = current.plusMonths(1);
        }
    }
    
    private void drawDayHeaders(GraphicsContext gc) {
        gc.setFont(Font.font("System", 10));
        LocalDate current = viewStartDate;
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("d");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");
        
        int dayIndex = 0;
        while (current.isBefore(viewEndDate) || current.equals(viewEndDate)) {
            double x = dayIndex * dayWidth;
            
            // Weekend shading
            if (current.getDayOfWeek() == DayOfWeek.SATURDAY || 
                current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                gc.setFill(COLOR_WEEKEND);
                gc.fillRect(x, 0, dayWidth, headerHeight);
            }
            
            // Day number
            gc.setFill(Color.BLACK);
            String dayText = current.format(dayFormatter);
            gc.fillText(dayText, x + 5, 20);
            
            // Day name
            String dayName = current.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            gc.fillText(dayName, x + 5, 35);
            
            // Month on first day
            if (current.getDayOfMonth() == 1) {
                gc.setFont(Font.font("System", FontWeight.BOLD, 10));
                gc.fillText(current.format(monthFormatter), x + 5, 50);
                gc.setFont(Font.font("System", 10));
            }
            
            // Grid line
            gc.setStroke(COLOR_GRID);
            gc.setLineWidth(0.5);
            gc.strokeLine(x, 0, x, headerHeight);
            
            current = current.plusDays(1);
            dayIndex++;
        }
    }
    
    private void drawWeekHeaders(GraphicsContext gc) {
        gc.setFont(Font.font("System", FontWeight.BOLD, 11));
        gc.setFill(Color.BLACK);
        gc.setStroke(COLOR_GRID);
        gc.setLineWidth(0.5);
        
        LocalDate current = viewStartDate;
        DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("MMM d");
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("d");
        
        // Draw week headers in top section
        LocalDate weekStart = current.with(DayOfWeek.MONDAY);
        while (weekStart.isBefore(viewEndDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(viewEndDate)) {
                weekEnd = viewEndDate;
            }
            
            long startDay = ChronoUnit.DAYS.between(viewStartDate, weekStart);
            long endDay = ChronoUnit.DAYS.between(viewStartDate, weekEnd);
            
            double x = Math.max(0, startDay * dayWidth);
            double width = (endDay - startDay + 1) * dayWidth;
            
            // Draw week header background
            gc.setFill(Color.web("#f8f9fa"));
            gc.fillRect(x, 0, width, 25);
            
            // Draw week text
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.BOLD, 11));
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int weekNumber = weekStart.get(weekFields.weekOfWeekBasedYear());
            String weekText = "Week " + weekNumber + " (" + weekStart.format(weekFormatter) + ")";
            gc.fillText(weekText, x + 5, 18);
            
            // Draw vertical line
            gc.strokeLine(x, 0, x, headerHeight);
            
            weekStart = weekStart.plusWeeks(1);
        }
        
        // Draw individual day labels in bottom section
        current = viewStartDate;
        int dayIndex = 0;
        gc.setFont(Font.font("System", 9));
        
        while (current.isBefore(viewEndDate) || current.equals(viewEndDate)) {
            double x = dayIndex * dayWidth;
            
            // Weekend shading
            if (current.getDayOfWeek() == DayOfWeek.SATURDAY || 
                current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                gc.setFill(COLOR_WEEKEND);
                gc.fillRect(x, 25, dayWidth, headerHeight - 25);
            }
            
            // Day label
            gc.setFill(Color.BLACK);
            String dayName = current.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            String dayNum = current.format(dayFormatter);
            gc.fillText(dayName, x + 2, 40);
            gc.fillText(dayNum, x + 2, 52);
            
            // Grid line
            gc.setStroke(COLOR_GRID);
            gc.setLineWidth(0.5);
            gc.strokeLine(x, 25, x, headerHeight);
            
            current = current.plusDays(1);
            dayIndex++;
        }
    }
    
    private void drawQuarterHeaders(GraphicsContext gc) {
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.setFill(Color.BLACK);
        gc.setStroke(COLOR_GRID);
        gc.setLineWidth(0.5);
        
        LocalDate current = viewStartDate.withDayOfMonth(1);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");
        
        // Draw quarters in top section
        int currentYear = current.getYear();
        for (int q = 1; q <= 4; q++) {
            LocalDate quarterStart = LocalDate.of(currentYear, (q - 1) * 3 + 1, 1);
            LocalDate quarterEnd = quarterStart.plusMonths(3).minusDays(1);
            
            if (quarterEnd.isBefore(viewStartDate) || quarterStart.isAfter(viewEndDate)) {
                continue;
            }
            
            long startDay = ChronoUnit.DAYS.between(viewStartDate, 
                quarterStart.isBefore(viewStartDate) ? viewStartDate : quarterStart);
            long endDay = ChronoUnit.DAYS.between(viewStartDate, 
                quarterEnd.isAfter(viewEndDate) ? viewEndDate : quarterEnd);
            
            double x = Math.max(0, startDay * dayWidth);
            double width = (endDay - startDay + 1) * dayWidth;
            
            // Draw quarter header background
            gc.setFill(Color.web("#e9ecef"));
            gc.fillRect(x, 0, width, 20);
            
            // Draw quarter text
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.BOLD, 12));
            String quarterText = "Q" + q + " " + currentYear;
            gc.fillText(quarterText, x + 5, 15);
            
            // Draw vertical line
            gc.strokeLine(x, 0, x, headerHeight);
        }
        
        // Draw months in middle section
        current = viewStartDate.withDayOfMonth(1);
        while (current.isBefore(viewEndDate)) {
            LocalDate monthEnd = current.plusMonths(1).minusDays(1);
            if (monthEnd.isAfter(viewEndDate)) {
                monthEnd = viewEndDate;
            }
            
            long startDay = ChronoUnit.DAYS.between(viewStartDate, current);
            long endDay = ChronoUnit.DAYS.between(viewStartDate, monthEnd);
            
            double x = Math.max(0, startDay * dayWidth);
            double width = (endDay - startDay + 1) * dayWidth;
            
            // Draw month background
            gc.setFill(Color.web("#f8f9fa"));
            gc.fillRect(x, 20, width, 20);
            
            // Draw month text
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", 10));
            String monthText = current.format(monthFormatter);
            gc.fillText(monthText, x + 3, 35);
            
            // Draw vertical line
            gc.strokeLine(x, 20, x, headerHeight);
            
            current = current.plusMonths(1);
        }
        
        // Draw week numbers in bottom section
        LocalDate weekStart = viewStartDate.with(DayOfWeek.MONDAY);
        gc.setFont(Font.font("System", 8));
        
        while (weekStart.isBefore(viewEndDate)) {
            long startDay = ChronoUnit.DAYS.between(viewStartDate, weekStart);
            double x = Math.max(0, startDay * dayWidth);
            
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int weekNumber = weekStart.get(weekFields.weekOfWeekBasedYear());
            
            gc.setFill(Color.GRAY);
            gc.fillText("W" + weekNumber, x + 2, 55);
            
            weekStart = weekStart.plusWeeks(1);
        }
    }
    
    private void drawGrid() {
        double canvasHeight = Math.max(ganttCanvas.getPrefHeight(), tasks.size() * taskHeight + 20);
        double canvasWidth = ganttCanvas.getPrefWidth();
        
        LocalDate current = viewStartDate;
        int dayIndex = 0;
        
        while (current.isBefore(viewEndDate) || current.equals(viewEndDate)) {
            double x = dayIndex * dayWidth;
            
            // Weekend shading
            if (current.getDayOfWeek() == DayOfWeek.SATURDAY || 
                current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                Rectangle weekendRect = new Rectangle(dayWidth, canvasHeight);
                weekendRect.setFill(COLOR_WEEKEND);
                weekendRect.setOpacity(0.3);
                weekendRect.setLayoutX(x);
                ganttCanvas.getChildren().add(weekendRect);
            }
            
            // Vertical grid lines
            Line gridLine = new Line(x, 0, x, canvasHeight);
            gridLine.setStroke(COLOR_GRID);
            gridLine.setStrokeWidth(0.5);
            gridLine.setOpacity(0.5);
            ganttCanvas.getChildren().add(gridLine);
            
            current = current.plusDays(1);
            dayIndex++;
        }
        
        // Horizontal grid lines
        int numRows = Math.max(tasks.size(), 10); // At least 10 rows for visual consistency
        for (int i = 0; i <= numRows; i++) {
            double y = i * taskHeight;
            Line gridLine = new Line(0, y, canvasWidth, y);
            gridLine.setStroke(COLOR_GRID);
            gridLine.setStrokeWidth(0.5);
            gridLine.setOpacity(0.5);
            ganttCanvas.getChildren().add(gridLine);
        }
    }
    
    private void drawTodayLine() {
        LocalDate today = LocalDate.now();
        if (today.isAfter(viewStartDate) && today.isBefore(viewEndDate)) {
            long daysFromStart = ChronoUnit.DAYS.between(viewStartDate, today);
            double x = daysFromStart * dayWidth;
            double canvasHeight = Math.max(ganttCanvas.getPrefHeight(), tasks.size() * taskHeight + 20);
            
            Line todayLine = new Line(x, 0, x, canvasHeight);
            todayLine.setStroke(COLOR_TODAY);
            todayLine.setStrokeWidth(2);
            todayLine.getStrokeDashArray().addAll(5d, 5d);
            ganttCanvas.getChildren().add(todayLine);
            
            // Today label
            Label todayLabel = new Label("Today");
            todayLabel.setTextFill(COLOR_TODAY);
            todayLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
            todayLabel.setLayoutX(x + 2);
            todayLabel.setLayoutY(2);
            ganttCanvas.getChildren().add(todayLabel);
        }
    }
    
    private void addTaskRow(Task task, int row) {
        // Add task info to left panel
        HBox taskInfo = new HBox(10);
        taskInfo.setPadding(new Insets(5, 10, 5, 10));
        taskInfo.setPrefHeight(taskHeight);
        taskInfo.setMinHeight(taskHeight);
        taskInfo.setMaxHeight(taskHeight);
        taskInfo.setAlignment(Pos.CENTER_LEFT);
        
        // Task name
        Label nameLabel = new Label(task.getTitle());
        nameLabel.setMinWidth(150);
        nameLabel.setPrefWidth(250);
        nameLabel.setMaxWidth(400);
        nameLabel.setWrapText(false);
        nameLabel.setEllipsisString("...");
        
        // Add tooltip to show full task name
        Tooltip taskTooltip = new Tooltip(task.getTitle());
        nameLabel.setTooltip(taskTooltip);
        
        // Resource
        Label resourceLabel = new Label(task.getAssignedToName() != null ? 
            task.getAssignedToName() : "Unassigned");
        resourceLabel.setMinWidth(80);
        resourceLabel.setPrefWidth(120);
        resourceLabel.setStyle("-fx-text-fill: #666;");
        
        // Add spacer to push content to the left
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        taskInfo.getChildren().addAll(nameLabel, resourceLabel, spacer);
        
        // Hover effect
        taskInfo.setOnMouseEntered(e -> taskInfo.setStyle("-fx-background-color: #f0f0f0;"));
        taskInfo.setOnMouseExited(e -> taskInfo.setStyle(""));
        
        taskListPanel.getChildren().add(taskInfo);
        
        // Add task bar to Gantt chart
        if (task.getPlannedStart() != null && task.getPlannedEnd() != null) {
            Node taskBarNode = createTaskBar(task, row);
            ganttCanvas.getChildren().add(taskBarNode);
            
            // Progress bar is now integrated into the task bar group, so we don't need to add it separately
        } else if (task.getPlannedStart() != null) {
            // Milestone (single date)
            Node milestone = createMilestone(task, row);
            ganttCanvas.getChildren().add(milestone);
        }
    }
    
    private Node createTaskBar(Task task, int row) {
        LocalDate startDate = task.getPlannedStart();
        LocalDate endDate = task.getPlannedEnd();
        
        long startDay = ChronoUnit.DAYS.between(viewStartDate, startDate);
        long duration = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        double x = startDay * dayWidth;
        double width = duration * dayWidth;
        double y = row * taskHeight + 10;
        
        // Create a group to hold the task bar and its components
        Group taskGroup = new Group();
        taskGroup.setLayoutX(x);
        taskGroup.setLayoutY(y);
        
        TaskBar taskBar = new TaskBar(task, 0, 0, width, taskHeight - 20);
        
        // Set color based on critical path and status
        if (showCriticalPathCheckBox.isSelected() && criticalPath.contains(task.getId())) {
            taskBar.setFill(COLOR_TASK_CRITICAL);
        } else if (task.getStatus() == Task.TaskStatus.COMPLETED) {
            taskBar.setFill(COLOR_TASK_COMPLETE);
        } else {
            taskBar.setFill(COLOR_TASK_NORMAL);
        }
        
        // Add the task bar as the base layer
        taskGroup.getChildren().add(taskBar);
        
        // Add progress bar if enabled and task has progress
        if (showProgressCheckBox.isSelected() && task.getProgressPercentage() != null && 
            task.getProgressPercentage() > 0) {
            double progressWidth = width * (task.getProgressPercentage() / 100.0);
            double progressHeight = (taskHeight - 20) * 0.4; // 40% of task bar height
            Rectangle progressBar = new Rectangle(progressWidth, progressHeight);
            progressBar.setFill(COLOR_TASK_PROGRESS);  // Use amber color
            progressBar.setOpacity(0.8);
            progressBar.setLayoutY((taskHeight - 20 - progressHeight) / 2); // Center vertically
            taskGroup.getChildren().add(progressBar);
        }
        
        // Add resource badge on the right side of the task bar
        if (task.getAssignedToName() != null && !task.getAssignedToName().isEmpty()) {
            Node resourceBadge = createResourceBadge(task.getAssignedToName());
            // Position badge on the right side with padding
            resourceBadge.setLayoutX(width - 22); // Right side (16 is badge width + padding)
            resourceBadge.setLayoutY((taskHeight - 20 - 16) / 2); // Center vertically (16 is badge height)
            taskGroup.getChildren().add(resourceBadge);
        }
        
        // Add progress percentage text with better visibility
        if (showProgressCheckBox.isSelected() && task.getProgressPercentage() != null && 
            task.getProgressPercentage() > 0) {
            // Create background for text visibility at the start of the bar
            Rectangle textBg = new Rectangle(30, 14);
            textBg.setFill(Color.BLACK);
            textBg.setOpacity(0.7);
            textBg.setLayoutX(0);  // Start at the left edge of the task bar
            textBg.setLayoutY(3);
            textBg.setArcWidth(4);
            textBg.setArcHeight(4);
            taskGroup.getChildren().add(textBg);
            
            Text progressText = new Text(task.getProgressPercentage() + "%");
            progressText.setFill(Color.WHITE);
            progressText.setFont(Font.font("System", FontWeight.BOLD, 10));
            progressText.setLayoutX(3); // Small padding from left edge
            progressText.setLayoutY(12); // Fixed vertical position near top
            taskGroup.getChildren().add(progressText);
        }
        
        // Ensure content is clipped to bar width
        Rectangle clip = new Rectangle(width, taskHeight - 20);
        taskGroup.setClip(clip);
        
        // Add enhanced tooltip with resource info
        int progress = task.getProgressPercentage() != null ? task.getProgressPercentage() : 0;
        String assignedTo = task.getAssignedToName() != null ? task.getAssignedToName() : "Unassigned";
        Tooltip tooltip = new Tooltip(String.format("%s\n%s to %s\nAssigned to: %s\nProgress: %d%%\nStatus: %s",
            task.getTitle(),
            startDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
            endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
            assignedTo,
            progress,
            task.getStatus() != null ? task.getStatus() : "Not Set"));
        Tooltip.install(taskGroup, tooltip);
        
        // Store the actual TaskBar for reference
        taskBars.put(task.getId(), taskBar);
        
        // Add context menu for dependency management
        setupDependencyContextMenu(taskBar, task);
        
        return taskGroup;
    }
    
    private Node createProgressBar(Task task, TaskBar taskBar) {
        // taskBar is the actual rectangle, need to get its parent's position
        double progressWidth = taskBar.getWidth() * (task.getProgressPercentage() / 100.0);
        
        // Get the position from the parent taskGroup
        Node taskGroup = taskBar.getParent();
        double x = taskGroup != null ? taskGroup.getLayoutX() : 0;
        double y = taskGroup != null ? taskGroup.getLayoutY() + 2 : 2;
        
        // Create a group to hold both the progress bar and text
        StackPane progressGroup = new StackPane();
        progressGroup.setLayoutX(x);
        progressGroup.setLayoutY(y);
        progressGroup.setMouseTransparent(true);
        
        // Create the progress bar
        Rectangle progressBar = new Rectangle(progressWidth, taskBar.getHeight() - 4);
        progressBar.setFill(Color.web("#2ECC71"));
        progressBar.setOpacity(0.7);
        
        // Add task title text
        Text progressText = new Text(task.getTitle() + " - " + task.getProgressPercentage() + "%");
        progressText.setFill(Color.BLACK);
        progressText.setFont(Font.font("System", FontWeight.NORMAL, 10));
        
        // Add both to the group
        progressGroup.getChildren().addAll(progressBar, progressText);
        progressGroup.setAlignment(Pos.CENTER_LEFT);
        progressGroup.setPrefWidth(progressWidth);
        progressGroup.setMaxWidth(progressWidth);
        
        // Add padding for text
        StackPane.setMargin(progressText, new Insets(0, 0, 0, 5));
        
        // Ensure text is clipped to progress bar width
        Rectangle clip = new Rectangle(progressWidth, taskBar.getHeight() - 4);
        progressGroup.setClip(clip);
        
        return progressGroup;
    }
    
    private Node createMilestone(Task task, int row) {
        LocalDate date = task.getPlannedStart();
        long dayFromStart = ChronoUnit.DAYS.between(viewStartDate, date);
        double x = dayFromStart * dayWidth + dayWidth / 2;
        double y = row * taskHeight + taskHeight / 2;
        
        Polygon diamond = new Polygon();
        diamond.getPoints().addAll(new Double[]{
            x, y - 8.0,
            x + 8.0, y,
            x, y + 8.0,
            x - 8.0, y
        });
        diamond.setFill(COLOR_MILESTONE);
        
        Tooltip tooltip = new Tooltip(String.format("%s\n%s\nMilestone",
            task.getTitle(),
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))));
        Tooltip.install(diamond, tooltip);
        
        return diamond;
    }
    
    private void drawDependencies() {
        for (Map.Entry<Long, List<TaskDependency>> entry : taskDependencies.entrySet()) {
            TaskBar fromBar = taskBars.get(entry.getKey());
            if (fromBar != null) {
                for (TaskDependency dep : entry.getValue()) {
                    TaskBar toBar = taskBars.get(dep.getSuccessorId());
                    if (toBar != null) {
                        drawDependencyLine(fromBar, toBar, dep.getDependencyType());
                    }
                }
            }
        }
    }
    
    private void drawDependencyLine(TaskBar from, TaskBar to, DependencyType type) {
        // Get positions from parent nodes if they exist
        Node fromParent = from.getParent();
        Node toParent = to.getParent();
        
        double startX, startY, endX, endY;
        
        // Adjust connection points based on dependency type
        switch (type) {
            case FINISH_TO_START:
            default:
                // Connect from end of predecessor to start of successor
                startX = (fromParent != null ? fromParent.getLayoutX() : from.getLayoutX()) + from.getWidth();
                startY = (fromParent != null ? fromParent.getLayoutY() : from.getLayoutY()) + from.getHeight() / 2;
                endX = toParent != null ? toParent.getLayoutX() : to.getLayoutX();
                endY = (toParent != null ? toParent.getLayoutY() : to.getLayoutY()) + to.getHeight() / 2;
                break;
                
            case START_TO_START:
                // Connect from start of predecessor to start of successor
                startX = fromParent != null ? fromParent.getLayoutX() : from.getLayoutX();
                startY = (fromParent != null ? fromParent.getLayoutY() : from.getLayoutY()) + from.getHeight() / 2;
                endX = toParent != null ? toParent.getLayoutX() : to.getLayoutX();
                endY = (toParent != null ? toParent.getLayoutY() : to.getLayoutY()) + to.getHeight() / 2;
                break;
                
            case FINISH_TO_FINISH:
                // Connect from end of predecessor to end of successor
                startX = (fromParent != null ? fromParent.getLayoutX() : from.getLayoutX()) + from.getWidth();
                startY = (fromParent != null ? fromParent.getLayoutY() : from.getLayoutY()) + from.getHeight() / 2;
                endX = (toParent != null ? toParent.getLayoutX() : to.getLayoutX()) + to.getWidth();
                endY = (toParent != null ? toParent.getLayoutY() : to.getLayoutY()) + to.getHeight() / 2;
                break;
                
            case START_TO_FINISH:
                // Connect from start of predecessor to end of successor
                startX = fromParent != null ? fromParent.getLayoutX() : from.getLayoutX();
                startY = (fromParent != null ? fromParent.getLayoutY() : from.getLayoutY()) + from.getHeight() / 2;
                endX = (toParent != null ? toParent.getLayoutX() : to.getLayoutX()) + to.getWidth();
                endY = (toParent != null ? toParent.getLayoutY() : to.getLayoutY()) + to.getHeight() / 2;
                break;
        }
        
        // Create path with arrow
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(COLOR_DEPENDENCY);
        line.setStrokeWidth(1.5);
        
        // Arrow head
        Polygon arrowHead = new Polygon();
        double angle = Math.atan2(endY - startY, endX - startX);
        double arrowLength = 8;
        double arrowAngle = Math.PI / 6;
        
        arrowHead.getPoints().addAll(new Double[]{
            endX,
            endY,
            endX - arrowLength * Math.cos(angle - arrowAngle),
            endY - arrowLength * Math.sin(angle - arrowAngle),
            endX - arrowLength * Math.cos(angle + arrowAngle),
            endY - arrowLength * Math.sin(angle + arrowAngle)
        });
        arrowHead.setFill(COLOR_DEPENDENCY);
        
        ganttCanvas.getChildren().addAll(line, arrowHead);
    }
    
    private void calculateCriticalPath() {
        // Simple critical path calculation
        // In a real implementation, this would use CPM algorithm
        criticalPath.clear();
        
        // For now, mark tasks with no slack as critical
        for (Task task : tasks) {
            if (task.getPlannedStart() != null && task.getPlannedEnd() != null) {
                // Check if task has dependencies
                boolean hasDependents = dependencies.stream()
                    .anyMatch(dep -> dep.getPredecessorId().equals(task.getId()));
                boolean isDependency = dependencies.stream()
                    .anyMatch(dep -> dep.getSuccessorId().equals(task.getId()));
                
                if (hasDependents || isDependency) {
                    criticalPath.add(task.getId());
                }
            }
        }
    }
    
    private void updateZoomLevel() {
        int selectedIndex = zoomComboBox.getSelectionModel().getSelectedIndex();
        zoomLevel = selectedIndex;
        
        // Adjust day width based on zoom level
        switch (zoomLevel) {
            case 0: // Day view
                dayWidth = 50;
                break;
            case 1: // Week view
                dayWidth = 20;
                break;
            case 2: // Month view
                dayWidth = 30;
                break;
            case 3: // Quarter view
                dayWidth = 10;
                break;
        }
        
        refreshDisplay();
    }
    
    private void scrollToToday() {
        LocalDate today = LocalDate.now();
        if (today.isAfter(viewStartDate) && today.isBefore(viewEndDate)) {
            long daysFromStart = ChronoUnit.DAYS.between(viewStartDate, today);
            double scrollPosition = (daysFromStart * dayWidth) / ganttCanvas.getWidth();
            mainScrollPane.setHvalue(scrollPosition);
        }
    }
    
    private void fitToWindow() {
        double availableWidth = mainScrollPane.getViewportBounds().getWidth();
        long totalDays = ChronoUnit.DAYS.between(viewStartDate, viewEndDate);
        dayWidth = availableWidth / totalDays;
        refreshDisplay();
    }
    
    private void exportChart() {
        // Export functionality - could export to PDF, PNG, or Excel
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export");
        alert.setContentText("Export functionality would be implemented here");
        alert.showAndWait();
    }
    
    private void refreshGanttChart() {
        refreshDisplay();
    }
    
    // Mouse event handlers for interactive features
    private void handleMousePressed(MouseEvent event) {
        // Get mouse position in canvas coordinates
        Point2D canvasPoint = ganttCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
        
        // Find which task bar was clicked by checking positions
        for (Map.Entry<Long, TaskBar> entry : taskBars.entrySet()) {
            TaskBar taskBar = entry.getValue();
            Node taskGroup = taskBar.getParent();
            if (taskGroup == null) continue;
            
            double taskStartX = taskGroup.getLayoutX();
            double taskEndX = taskStartX + taskBar.getWidth();
            double taskTopY = taskGroup.getLayoutY();
            double taskBottomY = taskTopY + taskBar.getHeight();
            
            // Check if click is within this task bar's bounds
            if (canvasPoint.getX() >= taskStartX - 10 && 
                canvasPoint.getX() <= taskEndX + 10 &&
                canvasPoint.getY() >= taskTopY && 
                canvasPoint.getY() <= taskBottomY) {
                
                draggedTaskBar = taskBar;
                
                // Check if clicking near edges for resizing
                if (Math.abs(canvasPoint.getX() - taskStartX) < 10) {
                    isDraggingStart = true;
                    isDraggingEnd = false;
                    dragStartX = canvasPoint.getX();
                } else if (Math.abs(canvasPoint.getX() - taskEndX) < 10) {
                    isDraggingEnd = true;
                    isDraggingStart = false;
                    dragStartX = canvasPoint.getX();
                } else {
                    isDraggingStart = false;
                    isDraggingEnd = false;
                    // For moving, store the offset from the task's left edge
                    dragStartX = canvasPoint.getX() - taskStartX;
                }
                
                event.consume();
                break;
            }
        }
    }
    
    private void handleMouseDragged(MouseEvent event) {
        if (draggedTaskBar != null) {
            Node taskGroup = draggedTaskBar.getParent();
            if (taskGroup == null) return;
            
            // Get mouse position in scene coordinates, then convert to canvas coordinates
            Point2D scenePoint = new Point2D(event.getSceneX(), event.getSceneY());
            Point2D canvasPoint = ganttCanvas.sceneToLocal(scenePoint);
            
            if (isDraggingStart) {
                // Resize from start
                double newX = canvasPoint.getX();
                double currentEnd = taskGroup.getLayoutX() + draggedTaskBar.getWidth();
                double newWidth = currentEnd - newX;
                
                if (newWidth > dayWidth && newX >= 0) {
                    taskGroup.setLayoutX(newX);
                    draggedTaskBar.setWidth(newWidth);
                    updateTaskDates(draggedTaskBar);
                }
            } else if (isDraggingEnd) {
                // Resize from end - keep start position, change width
                double currentX = taskGroup.getLayoutX();
                double newEndX = canvasPoint.getX();
                double newWidth = newEndX - currentX;
                if (newWidth > dayWidth && newWidth < ganttCanvas.getWidth()) {
                    draggedTaskBar.setWidth(newWidth);
                    // Update the StackPane width too
                    if (taskGroup instanceof StackPane) {
                        ((StackPane)taskGroup).setPrefWidth(newWidth);
                        ((StackPane)taskGroup).setMaxWidth(newWidth);
                        
                        // Also update any clipping rectangles
                        if (taskGroup.getClip() instanceof Rectangle) {
                            ((Rectangle)taskGroup.getClip()).setWidth(newWidth);
                        }
                    }
                    updateTaskDates(draggedTaskBar);
                }
            } else {
                // Move entire task
                double newX = canvasPoint.getX() - dragStartX;
                if (newX >= 0 && newX + draggedTaskBar.getWidth() <= ganttCanvas.getWidth()) {
                    taskGroup.setLayoutX(newX);
                    updateTaskDates(draggedTaskBar);
                }
            }
            
            // Update status
            updateStatus(draggedTaskBar.task);
            event.consume();
        }
    }
    
    private void handleMouseReleased(MouseEvent event) {
        if (draggedTaskBar != null) {
            Node taskGroup = draggedTaskBar.getParent();
            if (taskGroup != null) {
                // Snap to grid
                double snappedX = Math.round(taskGroup.getLayoutX() / dayWidth) * dayWidth;
                taskGroup.setLayoutX(snappedX);
                
                double snappedWidth = Math.round(draggedTaskBar.getWidth() / dayWidth) * dayWidth;
                draggedTaskBar.setWidth(Math.max(dayWidth, snappedWidth));
                
                // Update the StackPane width too
                if (taskGroup instanceof StackPane) {
                    ((StackPane)taskGroup).setPrefWidth(snappedWidth);
                }
                
                // Save changes
                updateTaskDates(draggedTaskBar);
                saveTaskChanges(draggedTaskBar.task);
                
                // Refresh dependencies
                if (showDependenciesCheckBox.isSelected()) {
                    refreshDisplay();
                }
            }
        }
        
        draggedTaskBar = null;
        isDraggingStart = false;
        isDraggingEnd = false;
        event.consume();
    }
    
    private void handleMouseMoved(MouseEvent event) {
        // Get mouse position in canvas coordinates
        Point2D canvasPoint = ganttCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
        boolean foundTask = false;
        
        // Check each task bar to see if mouse is over it
        for (Map.Entry<Long, TaskBar> entry : taskBars.entrySet()) {
            TaskBar taskBar = entry.getValue();
            Node taskGroup = taskBar.getParent();
            if (taskGroup == null) continue;
            
            double taskStartX = taskGroup.getLayoutX();
            double taskEndX = taskStartX + taskBar.getWidth();
            double taskTopY = taskGroup.getLayoutY();
            double taskBottomY = taskTopY + taskBar.getHeight();
            
            // Check if mouse is within this task bar's bounds
            if (canvasPoint.getX() >= taskStartX - 10 && 
                canvasPoint.getX() <= taskEndX + 10 &&
                canvasPoint.getY() >= taskTopY && 
                canvasPoint.getY() <= taskBottomY) {
                
                foundTask = true;
                
                // Check if near edges
                if (Math.abs(canvasPoint.getX() - taskStartX) < 10) {
                    ganttCanvas.setCursor(javafx.scene.Cursor.H_RESIZE);
                } else if (Math.abs(canvasPoint.getX() - taskEndX) < 10) {
                    ganttCanvas.setCursor(javafx.scene.Cursor.H_RESIZE);
                } else {
                    ganttCanvas.setCursor(javafx.scene.Cursor.MOVE);
                }
                break;
            }
        }
        
        if (!foundTask) {
            ganttCanvas.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }
    
    private void updateTaskDates(TaskBar taskBar) {
        Node taskGroup = taskBar.getParent();
        if (taskGroup == null) return;
        
        long startDay = Math.round(taskGroup.getLayoutX() / dayWidth);
        long duration = Math.round(taskBar.getWidth() / dayWidth);
        
        LocalDate newStart = viewStartDate.plusDays(startDay);
        LocalDate newEnd = newStart.plusDays(duration - 1);
        
        taskBar.task.setPlannedStart(newStart);
        taskBar.task.setPlannedEnd(newEnd);
    }
    
    private void saveTaskChanges(Task task) {
        taskRepository.update(task);
        statusLabel.setText("Task updated: " + task.getTitle());
    }
    
    private void updateStatus(Task task) {
        if (task.getPlannedStart() != null && task.getPlannedEnd() != null) {
            statusLabel.setText(String.format("%s: %s to %s",
                task.getTitle(),
                task.getPlannedStart().format(DateTimeFormatter.ofPattern("MMM d")),
                task.getPlannedEnd().format(DateTimeFormatter.ofPattern("MMM d"))));
        }
    }
    
    private void setupKeyboardShortcuts(Scene scene) {
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F5:
                    loadTasks();
                    break;
                case T:
                    if (event.isControlDown()) {
                        scrollToToday();
                    }
                    break;
                case PLUS:
                case ADD:
                    if (event.isControlDown()) {
                        dayWidth *= 1.2;
                        refreshDisplay();
                    }
                    break;
                case MINUS:
                case SUBTRACT:
                    if (event.isControlDown()) {
                        dayWidth /= 1.2;
                        refreshDisplay();
                    }
                    break;
            }
        });
    }
    
    private void setupDependencyContextMenu(TaskBar taskBar, Task task) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem createDependency = new MenuItem("Create Dependency From This Task...");
        createDependency.setOnAction(e -> showCreateDependencyDialog(task, true));
        
        MenuItem createPredecessor = new MenuItem("Add Predecessor...");
        createPredecessor.setOnAction(e -> showCreateDependencyDialog(task, false));
        
        MenuItem viewDependencies = new MenuItem("View Dependencies");
        viewDependencies.setOnAction(e -> showDependenciesDialog(task));
        
        MenuItem removeDependencies = new MenuItem("Remove All Dependencies");
        removeDependencies.setOnAction(e -> removeTaskDependencies(task));
        
        contextMenu.getItems().addAll(
            createDependency,
            createPredecessor,
            new SeparatorMenuItem(),
            viewDependencies,
            removeDependencies
        );
        
        taskBar.setOnContextMenuRequested(e -> {
            contextMenu.show(taskBar, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }
    
    private void showCreateDependencyDialog() {
        if (dependencyRepository == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Dependencies Not Available");
            alert.setHeaderText(null);
            alert.setContentText("Task dependency repository is not initialized.");
            alert.showAndWait();
            return;
        }
        
        if (tasks == null || tasks.size() < 2) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Insufficient Tasks");
            alert.setHeaderText(null);
            alert.setContentText("You need at least 2 tasks to create a dependency.");
            alert.showAndWait();
            return;
        }
        
        Dialog<TaskDependency> dialog = new Dialog<>();
        dialog.setTitle("Create Task Dependency");
        dialog.setHeaderText("Select predecessor and successor tasks");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        ComboBox<Task> predecessorCombo = new ComboBox<>();
        predecessorCombo.setItems(tasks);
        predecessorCombo.setConverter(new StringConverter<Task>() {
            @Override
            public String toString(Task task) {
                return task != null ? task.getTitle() : "";
            }
            
            @Override
            public Task fromString(String string) {
                return null;
            }
        });
        predecessorCombo.setPrefWidth(300);
        
        ComboBox<Task> successorCombo = new ComboBox<>();
        successorCombo.setItems(tasks);
        successorCombo.setConverter(new StringConverter<Task>() {
            @Override
            public String toString(Task task) {
                return task != null ? task.getTitle() : "";
            }
            
            @Override
            public Task fromString(String string) {
                return null;
            }
        });
        successorCombo.setPrefWidth(300);
        
        ComboBox<DependencyType> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList(DependencyType.values()));
        typeCombo.setValue(DependencyType.FINISH_TO_START);
        typeCombo.setConverter(new StringConverter<DependencyType>() {
            @Override
            public String toString(DependencyType type) {
                return type != null ? type.getDisplayName() : "";
            }
            
            @Override
            public DependencyType fromString(String string) {
                return null;
            }
        });
        
        Spinner<Integer> lagSpinner = new Spinner<>(-30, 30, 0);
        lagSpinner.setEditable(true);
        
        grid.add(new Label("Predecessor Task:"), 0, 0);
        grid.add(predecessorCombo, 1, 0);
        grid.add(new Label("Successor Task:"), 0, 1);
        grid.add(successorCombo, 1, 1);
        grid.add(new Label("Dependency Type:"), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label("Lag Days:"), 0, 3);
        grid.add(lagSpinner, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType createButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButton && 
                predecessorCombo.getValue() != null && 
                successorCombo.getValue() != null) {
                
                Task predecessor = predecessorCombo.getValue();
                Task successor = successorCombo.getValue();
                
                if (predecessor.getId().equals(successor.getId())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Dependency");
                    alert.setHeaderText(null);
                    alert.setContentText("A task cannot depend on itself.");
                    alert.showAndWait();
                    return null;
                }
                
                return new TaskDependency(
                    predecessor.getId(),
                    successor.getId(),
                    typeCombo.getValue(),
                    lagSpinner.getValue()
                );
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(dependency -> {
            try {
                // Check for cyclic dependencies
                if (dependencyRepository.hasCyclicDependency(
                        dependency.getPredecessorId(), 
                        dependency.getSuccessorId())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Dependency");
                    alert.setHeaderText("Cannot create dependency");
                    alert.setContentText("This would create a circular dependency.");
                    alert.showAndWait();
                    return;
                }
                
                // Save the dependency
                dependencyRepository.save(dependency);
                
                // Refresh the view
                loadTasks();
                refreshDisplay();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Dependency created successfully!");
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to create dependency");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        });
    }
    
    private void showCreateDependencyDialog(Task fromTask, boolean isFromPredecessor) {
        if (dependencyRepository == null) {
            // Initialize repository if not provided
            return;
        }
        
        Dialog<TaskDependency> dialog = new Dialog<>();
        dialog.setTitle("Create Task Dependency");
        dialog.setHeaderText(isFromPredecessor ? 
            "Select task that depends on: " + fromTask.getTitle() :
            "Select predecessor for: " + fromTask.getTitle());
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        ComboBox<Task> taskCombo = new ComboBox<>();
        taskCombo.setItems(FXCollections.observableArrayList(
            tasks.stream()
                .filter(t -> !t.getId().equals(fromTask.getId()))
                .collect(Collectors.toList())
        ));
        taskCombo.setConverter(new StringConverter<Task>() {
            @Override
            public String toString(Task task) {
                return task != null ? task.getTitle() : "";
            }
            
            @Override
            public Task fromString(String string) {
                return null;
            }
        });
        taskCombo.setPrefWidth(300);
        
        ComboBox<DependencyType> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList(DependencyType.values()));
        typeCombo.setValue(DependencyType.FINISH_TO_START);
        typeCombo.setConverter(new StringConverter<DependencyType>() {
            @Override
            public String toString(DependencyType type) {
                return type != null ? type.getDisplayName() : "";
            }
            
            @Override
            public DependencyType fromString(String string) {
                return null;
            }
        });
        
        Spinner<Integer> lagSpinner = new Spinner<>(-30, 30, 0);
        lagSpinner.setEditable(true);
        
        grid.add(new Label(isFromPredecessor ? "Successor Task:" : "Predecessor Task:"), 0, 0);
        grid.add(taskCombo, 1, 0);
        grid.add(new Label("Dependency Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Lag Days:"), 0, 2);
        grid.add(lagSpinner, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType createButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButton && taskCombo.getValue() != null) {
                Task otherTask = taskCombo.getValue();
                
                if (isFromPredecessor) {
                    return new TaskDependency(
                        fromTask.getId(),
                        otherTask.getId(),
                        typeCombo.getValue(),
                        lagSpinner.getValue()
                    );
                } else {
                    return new TaskDependency(
                        otherTask.getId(),
                        fromTask.getId(),
                        typeCombo.getValue(),
                        lagSpinner.getValue()
                    );
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(dependency -> {
            try {
                // Check for cyclic dependencies
                if (dependencyRepository.hasCyclicDependency(
                        dependency.getPredecessorId(), 
                        dependency.getSuccessorId())) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Dependency");
                    alert.setHeaderText("Cannot create dependency");
                    alert.setContentText("This would create a circular dependency.");
                    alert.showAndWait();
                    return;
                }
                
                dependencyRepository.save(dependency);
                loadDependencies();
                refreshDisplay();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to create dependency");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        });
    }
    
    private void showDependenciesDialog(Task task) {
        if (dependencyRepository == null) return;
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Task Dependencies");
        dialog.setHeaderText("Dependencies for: " + task.getTitle());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label predecessorsLabel = new Label("Predecessors:");
        predecessorsLabel.setFont(Font.font(null, FontWeight.BOLD, 12));
        
        ListView<TaskDependency> predecessorsList = new ListView<>();
        predecessorsList.setItems(FXCollections.observableArrayList(
            dependencyRepository.findPredecessors(task.getId())
        ));
        predecessorsList.setPrefHeight(100);
        
        Label successorsLabel = new Label("Successors:");
        successorsLabel.setFont(Font.font(null, FontWeight.BOLD, 12));
        
        ListView<TaskDependency> successorsList = new ListView<>();
        successorsList.setItems(FXCollections.observableArrayList(
            dependencyRepository.findSuccessors(task.getId())
        ));
        successorsList.setPrefHeight(100);
        
        content.getChildren().addAll(
            predecessorsLabel, predecessorsList,
            new Separator(),
            successorsLabel, successorsList
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    private void removeTaskDependencies(Task task) {
        if (dependencyRepository == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Dependencies");
        confirm.setHeaderText("Remove all dependencies for: " + task.getTitle());
        confirm.setContentText("This will remove all predecessor and successor relationships. Continue?");
        
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                dependencyRepository.deleteByTaskId(task.getId());
                loadDependencies();
                refreshDisplay();
            }
        });
    }
    
    // Inner class for task bars
    private class TaskBar extends Rectangle {
        private final Task task;
        
        public TaskBar(Task task, double x, double y, double width, double height) {
            super(x, y, width, height);
            this.task = task;
            setLayoutX(x);
            setLayoutY(y);
            setWidth(width);
            setHeight(height);
            setArcWidth(5);
            setArcHeight(5);
            setStroke(Color.DARKGRAY);
            setStrokeWidth(0.5);
        }
    }
    
    public void show() {
        stage.show();
    }
    
    private Node createResourceBadge(String resourceName) {
        // Create a container for the resource badge
        StackPane badge = new StackPane();
        badge.setAlignment(Pos.CENTER);
        
        // Create initials from resource name
        String initials = getInitials(resourceName);
        
        // Create a circle background for the initials
        Circle circle = new Circle(8);
        circle.setFill(Color.web("#495057"));  // Dark gray for better visibility
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(1.5);
        
        Text initialsText = new Text(initials);
        initialsText.setFill(Color.WHITE);
        initialsText.setFont(Font.font("System", FontWeight.BOLD, 8));
        
        badge.getChildren().addAll(circle, initialsText);
        
        // Add tooltip with full resource name
        Tooltip.install(badge, new Tooltip(resourceName));
        
        return badge;
    }
    
    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        } else {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }
}