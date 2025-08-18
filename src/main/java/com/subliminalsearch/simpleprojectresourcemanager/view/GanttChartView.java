package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.Task;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
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
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private Map<Long, List<Long>> dependencies = new HashMap<>(); // Task ID -> List of dependent task IDs
    private Set<Long> criticalPath = new HashSet<>();
    
    // View settings
    private LocalDate viewStartDate;
    private LocalDate viewEndDate;
    private double dayWidth = 50; // pixels per day (Day view default)
    private double taskHeight = 35; // height of each task row
    private double headerHeight = 60; // height of timeline header
    private int zoomLevel = 0; // 0=Day, 1=Week, 2=Month (default to Day view)
    
    // Colors
    private static final Color COLOR_TASK_NORMAL = Color.web("#4A90E2");
    private static final Color COLOR_TASK_CRITICAL = Color.web("#E74C3C");
    private static final Color COLOR_TASK_COMPLETE = Color.web("#27AE60");
    private static final Color COLOR_MILESTONE = Color.web("#F39C12");
    private static final Color COLOR_DEPENDENCY = Color.web("#7F8C8D");
    private static final Color COLOR_TODAY = Color.web("#E67E22");
    private static final Color COLOR_WEEKEND = Color.web("#ECF0F1");
    private static final Color COLOR_GRID = Color.web("#BDC3C7");
    
    // Interaction state
    private TaskBar draggedTaskBar = null;
    private boolean isDraggingStart = false;
    private boolean isDraggingEnd = false;
    private double dragStartX = 0;
    
    public GanttChartView(Project project, TaskRepository taskRepository, List<Resource> resources) {
        this.project = project;
        this.taskRepository = taskRepository;
        this.resources = resources;
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.setTitle("Gantt Chart - " + project.getProjectId());
        
        initialize();
        loadTasks();
    }
    
    private void initialize() {
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
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        toolbar.getChildren().addAll(
            zoomLabel, zoomComboBox,
            new Separator(),
            showDependenciesCheckBox,
            showCriticalPathCheckBox,
            showProgressCheckBox,
            new Separator(),
            todayButton,
            fitButton,
            new Separator(),
            exportButton,
            refreshButton
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
        dependencies.clear();
        
        // Load dependencies from task relationships
        for (Task task : tasks) {
            if (task.getParentTaskId() != null) {
                // Parent-child dependency
                dependencies.computeIfAbsent(task.getParentTaskId(), k -> new ArrayList<>()).add(task.getId());
            }
            
            // Could also load from a separate dependencies table if available
            // This is where you'd load finish-to-start, start-to-start relationships etc.
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
            
            // Get the actual TaskBar from our map for progress bar calculation
            TaskBar taskBar = taskBars.get(task.getId());
            
            // Add progress bar if enabled and task has progress
            if (showProgressCheckBox.isSelected() && task.getProgressPercentage() != null && 
                task.getProgressPercentage() > 0 && taskBar != null) {
                Node progressBar = createProgressBar(task, taskBar);
                ganttCanvas.getChildren().add(progressBar);
            }
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
        
        // Create a group to hold the task bar and its text
        StackPane taskGroup = new StackPane();
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
        
        // Add task title text on the bar
        Text taskText = new Text(task.getTitle());
        taskText.setFill(Color.WHITE);
        taskText.setFont(Font.font("System", FontWeight.NORMAL, 11));
        taskText.setStroke(Color.BLACK);
        taskText.setStrokeWidth(0.5);
        taskText.setMouseTransparent(true);  // Make text not block mouse events
        
        // Add both to the group
        taskGroup.getChildren().addAll(taskBar, taskText);
        taskGroup.setAlignment(Pos.CENTER_LEFT);
        taskGroup.setPrefWidth(width);
        
        // Ensure text is clipped to bar width
        Rectangle clip = new Rectangle(width, taskHeight - 20);
        taskGroup.setClip(clip);
        
        // Add tooltip
        int progress = task.getProgressPercentage() != null ? task.getProgressPercentage() : 0;
        Tooltip tooltip = new Tooltip(String.format("%s\n%s to %s\nProgress: %d%%\nStatus: %s",
            task.getTitle(),
            startDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
            endDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
            progress,
            task.getStatus() != null ? task.getStatus() : "Not Set"));
        Tooltip.install(taskGroup, tooltip);
        
        // Store the actual TaskBar for reference
        taskBars.put(task.getId(), taskBar);
        
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
        for (Map.Entry<Long, List<Long>> entry : dependencies.entrySet()) {
            TaskBar fromBar = taskBars.get(entry.getKey());
            if (fromBar != null) {
                for (Long toId : entry.getValue()) {
                    TaskBar toBar = taskBars.get(toId);
                    if (toBar != null) {
                        drawDependencyLine(fromBar, toBar);
                    }
                }
            }
        }
    }
    
    private void drawDependencyLine(TaskBar from, TaskBar to) {
        // Get positions from parent nodes if they exist
        Node fromParent = from.getParent();
        Node toParent = to.getParent();
        
        double startX = (fromParent != null ? fromParent.getLayoutX() : from.getLayoutX()) + from.getWidth();
        double startY = (fromParent != null ? fromParent.getLayoutY() : from.getLayoutY()) + from.getHeight() / 2;
        double endX = toParent != null ? toParent.getLayoutX() : to.getLayoutX();
        double endY = (toParent != null ? toParent.getLayoutY() : to.getLayoutY()) + to.getHeight() / 2;
        
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
                boolean hasDependents = dependencies.containsKey(task.getId()) && 
                                       !dependencies.get(task.getId()).isEmpty();
                boolean isDependency = dependencies.values().stream()
                    .anyMatch(list -> list.contains(task.getId()));
                
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
}