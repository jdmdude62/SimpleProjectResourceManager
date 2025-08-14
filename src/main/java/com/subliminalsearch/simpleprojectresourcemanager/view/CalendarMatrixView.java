package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarMatrixView {
    private final Stage stage;
    private final Project project;
    private final TaskRepository taskRepository;
    private final ResourceRepository resourceRepository;
    
    private GridPane calendarGrid;
    private YearMonth currentMonth;
    private ComboBox<Resource> resourceFilter;
    private Label monthYearLabel;
    private Map<LocalDate, List<Task>> dateTaskMap;
    private Resource selectedResource;
    private ToggleGroup viewGroup;
    private LocalDate currentWeekStart;
    private LocalDate currentDayView;
    
    public CalendarMatrixView(Project project, TaskRepository taskRepository, ResourceRepository resourceRepository) {
        this.project = project;
        this.taskRepository = taskRepository;
        this.resourceRepository = resourceRepository;
        this.stage = new Stage();
        this.currentMonth = YearMonth.now();
        this.currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.SUNDAY);
        this.currentDayView = LocalDate.now();
        
        initialize();
    }
    
    private void initialize() {
        stage.setTitle("Calendar Matrix - " + project.getProjectId());
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Header with navigation and filters
        HBox header = createHeader();
        
        // Calendar grid
        ScrollPane scrollPane = new ScrollPane();
        calendarGrid = new GridPane();
        calendarGrid.setHgap(2);
        calendarGrid.setVgap(2);
        calendarGrid.setPadding(new Insets(10));
        calendarGrid.setStyle("-fx-background-color: #f0f0f0;");
        scrollPane.setContent(calendarGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        // Legend
        HBox legend = createLegend();
        
        root.getChildren().addAll(header, scrollPane, legend);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/css/calendar.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        
        loadTaskData();
        buildCalendar();
    }
    
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #ccc;");
        
        // Previous button
        Button prevButton = new Button("◀");
        prevButton.setOnAction(e -> navigatePrevious());
        
        // Month/Year label
        monthYearLabel = new Label();
        monthYearLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        monthYearLabel.setMinWidth(350);  // Fixed minimum width to prevent button shifting
        monthYearLabel.setAlignment(Pos.CENTER);
        updateMonthYearLabel();
        
        // Next button
        Button nextButton = new Button("▶");
        nextButton.setOnAction(e -> navigateNext());
        
        // Today button
        Button todayButton = new Button("Today");
        todayButton.setOnAction(e -> navigateToday());
        
        // Resource filter
        Label resourceLabel = new Label("Resource:");
        resourceFilter = new ComboBox<>();
        resourceFilter.setPrefWidth(200);
        
        // Set up converter to display resource name instead of toString
        resourceFilter.setConverter(new javafx.util.StringConverter<Resource>() {
            @Override
            public String toString(Resource resource) {
                return resource != null ? resource.getName() : "";
            }
            
            @Override
            public Resource fromString(String string) {
                return resourceFilter.getItems().stream()
                    .filter(r -> r != null && r.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        // Load resources
        List<Resource> resources = resourceRepository.findActiveResources();
        Resource allResources = new Resource();
        allResources.setId(-1L);
        allResources.setName("All Resources");
        
        resourceFilter.getItems().add(allResources);
        resourceFilter.getItems().addAll(resources);
        resourceFilter.setValue(allResources);
        
        resourceFilter.setOnAction(e -> {
            selectedResource = resourceFilter.getValue();
            if (selectedResource != null && selectedResource.getId() == -1L) {
                selectedResource = null;
            }
            loadTaskData();
            refreshCurrentView();
        });
        
        // View mode buttons
        viewGroup = new ToggleGroup();
        RadioButton monthView = new RadioButton("Month");
        RadioButton weekView = new RadioButton("Week");
        RadioButton dayView = new RadioButton("Day");
        monthView.setToggleGroup(viewGroup);
        weekView.setToggleGroup(viewGroup);
        dayView.setToggleGroup(viewGroup);
        monthView.setSelected(true);
        monthView.setUserData("MONTH");
        weekView.setUserData("WEEK");
        dayView.setUserData("DAY");
        
        // Wire up view mode changes
        monthView.setOnAction(e -> showMonthView());
        weekView.setOnAction(e -> {
            // Initialize week start based on current month being viewed
            LocalDate firstOfMonth = currentMonth.atDay(1);
            currentWeekStart = firstOfMonth.with(java.time.DayOfWeek.SUNDAY);
            showWeekView();
        });
        dayView.setOnAction(e -> {
            // Initialize day view to first of current month
            currentDayView = currentMonth.atDay(1);
            showDayView();
        });
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(
            prevButton, monthYearLabel, nextButton, todayButton,
            new Separator(), resourceLabel, resourceFilter,
            new Separator(), monthView, weekView, dayView,
            spacer
        );
        
        return header;
    }
    
    private void updateMonthYearLabel() {
        monthYearLabel.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + 
                              " " + currentMonth.getYear());
    }
    
    private void navigatePrevious() {
        String viewMode = (String) viewGroup.getSelectedToggle().getUserData();
        switch (viewMode) {
            case "MONTH":
                currentMonth = currentMonth.minusMonths(1);
                updateMonthYearLabel();
                loadTaskData();
                buildCalendar();
                break;
            case "WEEK":
                currentWeekStart = currentWeekStart.minusWeeks(1);
                loadTaskData();
                showWeekView();
                break;
            case "DAY":
                currentDayView = currentDayView.minusDays(1);
                loadTaskData();
                showDayView();
                break;
        }
    }
    
    private void navigateNext() {
        String viewMode = (String) viewGroup.getSelectedToggle().getUserData();
        switch (viewMode) {
            case "MONTH":
                currentMonth = currentMonth.plusMonths(1);
                updateMonthYearLabel();
                loadTaskData();
                buildCalendar();
                break;
            case "WEEK":
                currentWeekStart = currentWeekStart.plusWeeks(1);
                loadTaskData();
                showWeekView();
                break;
            case "DAY":
                currentDayView = currentDayView.plusDays(1);
                loadTaskData();
                showDayView();
                break;
        }
    }
    
    private void navigateToday() {
        String viewMode = (String) viewGroup.getSelectedToggle().getUserData();
        LocalDate today = LocalDate.now();
        switch (viewMode) {
            case "MONTH":
                currentMonth = YearMonth.now();
                updateMonthYearLabel();
                loadTaskData();
                buildCalendar();
                break;
            case "WEEK":
                currentWeekStart = today.with(java.time.DayOfWeek.SUNDAY);
                loadTaskData();
                showWeekView();
                break;
            case "DAY":
                currentDayView = today;
                loadTaskData();
                showDayView();
                break;
        }
    }
    
    private void refreshCurrentView() {
        String viewMode = (String) viewGroup.getSelectedToggle().getUserData();
        switch (viewMode) {
            case "MONTH":
                buildCalendar();
                break;
            case "WEEK":
                showWeekView();
                break;
            case "DAY":
                showDayView();
                break;
        }
    }
    
    private void showMonthView() {
        updateMonthYearLabel();
        loadTaskData();
        buildCalendar();
    }
    
    private void loadTaskData() {
        dateTaskMap = new HashMap<>();
        
        // Get all tasks for the project
        List<Task> projectTasks = taskRepository.findByProjectId(project.getId());
        
        // Filter by selected resource if applicable
        if (selectedResource != null) {
            projectTasks = projectTasks.stream()
                .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().equals(selectedResource.getId()))
                .collect(Collectors.toList());
        }
        
        // Group tasks by date
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        
        for (Task task : projectTasks) {
            LocalDate taskStart = task.getPlannedStart();
            LocalDate taskEnd = task.getPlannedEnd();
            
            if (taskStart == null || taskEnd == null) continue;
            
            // Add task to each day it spans
            LocalDate current = taskStart.isBefore(monthStart) ? monthStart : taskStart;
            LocalDate end = taskEnd.isAfter(monthEnd) ? monthEnd : taskEnd;
            
            while (!current.isAfter(end)) {
                dateTaskMap.computeIfAbsent(current, k -> new ArrayList<>()).add(task);
                current = current.plusDays(1);
            }
        }
    }
    
    private void buildCalendar() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();
        
        // Add day of week headers
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setStyle("-fx-background-color: #d0d0d0; -fx-padding: 5;");
            
            GridPane.setFillWidth(dayLabel, true);
            calendarGrid.add(dayLabel, i, 0);
            
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(14.28); // 100/7
            calendarGrid.getColumnConstraints().add(col);
        }
        
        // Calculate the starting position
        LocalDate firstDay = currentMonth.atDay(1);
        int dayOfWeekValue = firstDay.getDayOfWeek().getValue();
        int startCol = dayOfWeekValue % 7; // Sunday = 0
        
        // Add day cells
        int row = 1;
        int col = startCol;
        int daysInMonth = currentMonth.lengthOfMonth();
        
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            VBox dayCell = createDayCell(date);
            
            GridPane.setFillWidth(dayCell, true);
            GridPane.setFillHeight(dayCell, true);
            calendarGrid.add(dayCell, col, row);
            
            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }
        
        // Set row constraints for equal height
        for (int i = 0; i <= row; i++) {
            RowConstraints rowConstraint = new RowConstraints();
            if (i == 0) {
                rowConstraint.setMinHeight(30);
                rowConstraint.setPrefHeight(30);
            } else {
                rowConstraint.setMinHeight(120);
                rowConstraint.setPrefHeight(120);
                rowConstraint.setVgrow(Priority.ALWAYS);
            }
            calendarGrid.getRowConstraints().add(rowConstraint);
        }
    }
    
    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(2);
        cell.setPadding(new Insets(5));
        cell.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1;");
        
        // Add hover effect
        cell.setOnMouseEntered(e -> cell.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #007bff; -fx-border-width: 2;"));
        cell.setOnMouseExited(e -> {
            if (date.equals(LocalDate.now())) {
                cell.setStyle("-fx-background-color: #e6f3ff; -fx-border-color: #007bff; -fx-border-width: 2;");
            } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                cell.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 1;");
            } else {
                cell.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1;");
            }
        });
        
        // Highlight today
        if (date.equals(LocalDate.now())) {
            cell.setStyle("-fx-background-color: #e6f3ff; -fx-border-color: #007bff; -fx-border-width: 2;");
        }
        
        // Weekend styling
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cell.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 1;");
        }
        
        // Day number
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Tasks for this day
        List<Task> dayTasks = dateTaskMap.get(date);
        if (dayTasks != null && !dayTasks.isEmpty()) {
            // Show task count
            Label taskCount = new Label(dayTasks.size() + " task" + (dayTasks.size() > 1 ? "s" : ""));
            taskCount.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
            
            // Show first few tasks
            VBox taskList = new VBox(2);
            int displayCount = Math.min(3, dayTasks.size());
            
            for (int i = 0; i < displayCount; i++) {
                Task task = dayTasks.get(i);
                HBox taskItem = createTaskItem(task);
                taskList.getChildren().add(taskItem);
            }
            
            if (dayTasks.size() > 3) {
                Label moreLabel = new Label("+" + (dayTasks.size() - 3) + " more");
                moreLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #999;");
                taskList.getChildren().add(moreLabel);
            }
            
            cell.getChildren().addAll(dayNumber, taskCount, taskList);
        } else {
            cell.getChildren().add(dayNumber);
        }
        
        // Click to show day details
        cell.setOnMouseClicked(e -> showDayDetails(date, dayTasks));
        
        return cell;
    }
    
    private HBox createTaskItem(Task task) {
        HBox item = new HBox(2);
        item.setAlignment(Pos.CENTER_LEFT);
        
        // Priority indicator
        Label priorityDot = new Label("●");
        priorityDot.setStyle("-fx-font-size: 8px; -fx-text-fill: " + getPriorityColor(task.getPriority()) + ";");
        
        // Task title (truncated)
        String title = task.getTitle();
        if (title.length() > 15) {
            title = title.substring(0, 15) + "...";
        }
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 10px;");
        
        // Status indicator
        String statusStyle = getStatusStyle(task.getStatus());
        titleLabel.setStyle(titleLabel.getStyle() + statusStyle);
        
        item.getChildren().addAll(priorityDot, titleLabel);
        
        return item;
    }
    
    private String getPriorityColor(Task.TaskPriority priority) {
        if (priority == null) return "#999";
        switch (priority) {
            case CRITICAL: return "#dc3545";
            case HIGH: return "#fd7e14";
            case MEDIUM: return "#ffc107";
            case LOW: return "#28a745";
            default: return "#999";
        }
    }
    
    private String getStatusStyle(Task.TaskStatus status) {
        if (status == null) return "";
        switch (status) {
            case COMPLETED: return "; -fx-text-fill: #28a745; -fx-font-style: italic;";
            case IN_PROGRESS: return "; -fx-text-fill: #007bff;";
            case BLOCKED: return "; -fx-text-fill: #dc3545; -fx-font-weight: bold;";
            case REVIEW: return "; -fx-text-fill: #6f42c1;";
            default: return "";
        }
    }
    
    private void showDayDetails(LocalDate date, List<Task> tasks) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Tasks for " + date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        dialog.setHeaderText(null);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setMinWidth(600);
        content.setMinHeight(400);
        
        if (tasks == null || tasks.isEmpty()) {
            Label noTasksLabel = new Label("No tasks scheduled for this day");
            noTasksLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
            content.getChildren().add(noTasksLabel);
        } else {
            ScrollPane scrollPane = new ScrollPane();
            VBox taskContainer = new VBox(10);
            
            for (Task task : tasks) {
                VBox taskCard = createDetailedTaskCard(task);
                taskContainer.getChildren().add(taskCard);
            }
            
            scrollPane.setContent(taskContainer);
            scrollPane.setFitToWidth(true);
            content.getChildren().add(scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }
        
        // Add button to create new task for this day
        Button addTaskButton = new Button("Add Task for This Day");
        addTaskButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addTaskButton.setOnAction(e -> {
            createTaskForDay(date);
            dialog.close();
            loadTaskData();
            refreshCurrentView();
        });
        
        content.getChildren().add(addTaskButton);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Set dialog size
        dialog.setOnShown(e -> {
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMinWidth(650);
            dialogStage.setMinHeight(500);
        });
        
        dialog.showAndWait();
    }
    
    private VBox createDetailedTaskCard(Task task) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");
        
        // Title
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Status and Priority
        HBox statusRow = new HBox(10);
        Label statusLabel = new Label("Status: " + task.getStatus());
        Label priorityLabel = new Label("Priority: " + task.getPriority());
        Label progressLabel = new Label("Progress: " + task.getProgressPercentage() + "%");
        statusRow.getChildren().addAll(statusLabel, priorityLabel, progressLabel);
        
        // Dates
        String dateInfo = String.format("Scheduled: %s to %s",
            task.getPlannedStart() != null ? task.getPlannedStart().toString() : "N/A",
            task.getPlannedEnd() != null ? task.getPlannedEnd().toString() : "N/A"
        );
        Label dateLabel = new Label(dateInfo);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        // Description
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            Label descLabel = new Label(task.getDescription());
            descLabel.setWrapText(true);
            descLabel.setStyle("-fx-font-size: 12px;");
            card.getChildren().add(descLabel);
        }
        
        // Progress bar
        ProgressBar progressBar = new ProgressBar(task.getProgressPercentage() / 100.0);
        progressBar.setPrefWidth(200);
        
        card.getChildren().addAll(titleLabel, statusRow, dateLabel, progressBar);
        
        return card;
    }
    
    private void showWeekView() {
        // Clear the grid
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();
        
        // Use the current week start
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        
        // Update label
        monthYearLabel.setText("Week of " + currentWeekStart.format(DateTimeFormatter.ofPattern("MMM d")) + 
                              " - " + weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        
        // Create week view as a list-based layout for each day
        String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        
        // Add column constraints for 7 days
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(14.28); // Equal width for each day
            col.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(col);
        }
        
        // Add day headers in row 0
        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = currentWeekStart.plusDays(i);
            VBox dayHeader = new VBox(2);
            dayHeader.setAlignment(Pos.CENTER);
            dayHeader.setStyle("-fx-background-color: #d0d0d0; -fx-padding: 8; -fx-border-color: #bbb;");
            
            Label dayNameLabel = new Label(dayNames[i]);
            dayNameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            
            Label dateLabel = new Label(dayDate.format(DateTimeFormatter.ofPattern("MMM d")));
            dateLabel.setFont(Font.font("System", 11));
            
            // Add task count
            List<Task> dayTasks = dateTaskMap.getOrDefault(dayDate, new ArrayList<>());
            Label countLabel = new Label(dayTasks.size() + " tasks");
            countLabel.setFont(Font.font("System", 10));
            countLabel.setStyle("-fx-text-fill: #666;");
            
            dayHeader.getChildren().addAll(dayNameLabel, dateLabel, countLabel);
            GridPane.setFillWidth(dayHeader, true);
            calendarGrid.add(dayHeader, i, 0);
        }
        
        // Add scrollable task list for each day in row 1
        for (int i = 0; i < 7; i++) {
            LocalDate cellDate = currentWeekStart.plusDays(i);
            List<Task> dayTasks = dateTaskMap.getOrDefault(cellDate, new ArrayList<>());
            
            // Create scrollable container for tasks
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
            scrollPane.setMinHeight(400);
            scrollPane.setMaxHeight(500);
            
            VBox taskList = new VBox(3);
            taskList.setPadding(new Insets(5));
            taskList.setStyle("-fx-background-color: white;");
            
            // Add tasks
            if (dayTasks.isEmpty()) {
                Label noTasksLabel = new Label("No tasks");
                noTasksLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-padding: 10;");
                taskList.getChildren().add(noTasksLabel);
            } else {
                for (Task task : dayTasks) {
                    VBox taskCard = createCompactTaskCard(task);
                    
                    // Add click handler
                    final LocalDate cellDateFinal = cellDate;
                    taskCard.setOnMouseClicked(e -> showDayDetails(cellDateFinal, dayTasks));
                    
                    taskList.getChildren().add(taskCard);
                }
            }
            
            // Add button to create new task for this day
            Button addTaskBtn = new Button("+ Add Task");
            addTaskBtn.setMaxWidth(Double.MAX_VALUE);
            addTaskBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 11px;");
            addTaskBtn.setOnAction(e -> {
                createTaskForDay(cellDate);
                loadTaskData();
                refreshCurrentView();
            });
            taskList.getChildren().add(addTaskBtn);
            
            scrollPane.setContent(taskList);
            GridPane.setFillWidth(scrollPane, true);
            GridPane.setVgrow(scrollPane, Priority.ALWAYS);
            calendarGrid.add(scrollPane, i, 1);
        }
        
        // Add row constraints
        RowConstraints headerRow = new RowConstraints();
        headerRow.setMinHeight(80);
        headerRow.setVgrow(Priority.NEVER);
        calendarGrid.getRowConstraints().add(headerRow);
        
        RowConstraints contentRow = new RowConstraints();
        contentRow.setVgrow(Priority.ALWAYS);
        calendarGrid.getRowConstraints().add(contentRow);
    }
    
    private void showDayView() {
        // Clear the grid
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();
        
        // Update label
        monthYearLabel.setText(currentDayView.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        
        // Create day view as a single list of tasks
        VBox dayContainer = new VBox(10);
        dayContainer.setPadding(new Insets(10));
        dayContainer.setStyle("-fx-background-color: white;");
        
        // Header with day info
        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setStyle("-fx-background-color: #d0d0d0; -fx-padding: 15; -fx-border-color: #bbb;");
        
        Label dayLabel = new Label(currentDayView.format(DateTimeFormatter.ofPattern("EEEE")));
        dayLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        Label dateLabel = new Label(currentDayView.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        dateLabel.setFont(Font.font("System", 14));
        
        // Get tasks for this day
        List<Task> dayTasks = dateTaskMap.getOrDefault(currentDayView, new ArrayList<>());
        
        Label taskCountLabel = new Label(dayTasks.size() + " tasks scheduled");
        taskCountLabel.setFont(Font.font("System", 12));
        taskCountLabel.setStyle("-fx-text-fill: #666;");
        
        headerBox.getChildren().addAll(dayLabel, dateLabel, taskCountLabel);
        
        // Create scrollable task list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        scrollPane.setMinHeight(450);
        
        VBox taskListContainer = new VBox(5);
        taskListContainer.setPadding(new Insets(10));
        taskListContainer.setStyle("-fx-background-color: white;");
        
        if (dayTasks.isEmpty()) {
            Label noTasksLabel = new Label("No tasks scheduled for this day");
            noTasksLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 14px; -fx-padding: 20;");
            taskListContainer.getChildren().add(noTasksLabel);
        } else {
            // Group tasks by status or priority
            Map<Task.TaskStatus, List<Task>> tasksByStatus = dayTasks.stream()
                .collect(Collectors.groupingBy(
                    task -> task.getStatus() != null ? task.getStatus() : Task.TaskStatus.NOT_STARTED
                ));
            
            // Show tasks by status groups
            for (Task.TaskStatus status : Task.TaskStatus.values()) {
                List<Task> statusTasks = tasksByStatus.get(status);
                if (statusTasks != null && !statusTasks.isEmpty()) {
                    // Status header
                    Label statusHeader = new Label(status.toString() + " (" + statusTasks.size() + ")");
                    statusHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
                    statusHeader.setStyle("-fx-text-fill: #444; -fx-padding: 5 0 5 0;");
                    taskListContainer.getChildren().add(statusHeader);
                    
                    // Add tasks for this status
                    for (Task task : statusTasks) {
                        HBox taskCard = createDetailedDayTaskCard(task);
                        taskListContainer.getChildren().add(taskCard);
                    }
                    
                    // Add separator
                    Separator separator = new Separator();
                    separator.setStyle("-fx-padding: 5 0 5 0;");
                    taskListContainer.getChildren().add(separator);
                }
            }
        }
        
        // Add button to create new task
        Button addTaskBtn = new Button("+ Add New Task for This Day");
        addTaskBtn.setMaxWidth(Double.MAX_VALUE);
        addTaskBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10;");
        addTaskBtn.setOnAction(e -> {
            createTaskForDay(currentDayView);
            loadTaskData();
            refreshCurrentView();
        });
        taskListContainer.getChildren().add(addTaskBtn);
        
        scrollPane.setContent(taskListContainer);
        
        // Add everything to the grid
        calendarGrid.add(headerBox, 0, 0);
        calendarGrid.add(scrollPane, 0, 1);
        
        // Set up constraints
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(100);
        calendarGrid.getColumnConstraints().add(col);
        
        RowConstraints headerRow = new RowConstraints();
        headerRow.setMinHeight(100);
        headerRow.setVgrow(Priority.NEVER);
        calendarGrid.getRowConstraints().add(headerRow);
        
        RowConstraints contentRow = new RowConstraints();
        contentRow.setVgrow(Priority.ALWAYS);
        calendarGrid.getRowConstraints().add(contentRow);
    }
    
    private String getTaskColor(Task task) {
        if (task.getPriority() == Task.TaskPriority.CRITICAL) return "#ffcccc";
        if (task.getPriority() == Task.TaskPriority.HIGH) return "#ffe6cc";
        if (task.getStatus() == Task.TaskStatus.COMPLETED) return "#ccffcc";
        if (task.getStatus() == Task.TaskStatus.BLOCKED) return "#ffccff";
        return "#e6f3ff";
    }
    
    private void createTaskForDay(LocalDate date) {
        // Create a new task - it will be positioned at end of day due to datetime ordering
        Task newTask = new Task();
        newTask.setProjectId(project.getId());
        newTask.setTitle("New Task - " + date.format(DateTimeFormatter.ofPattern("MMM d")));
        newTask.setTaskCode(taskRepository.generateTaskCode(project.getId()));
        newTask.setStatus(Task.TaskStatus.NOT_STARTED);
        newTask.setPriority(Task.TaskPriority.MEDIUM);
        newTask.setTaskType(Task.TaskType.GENERAL);
        
        // Set task to the selected date
        // The TaskRepository will add current time when inserting to ensure proper ordering
        newTask.setPlannedStart(date);
        newTask.setPlannedEnd(date.plusDays(1)); // Default 1 day duration
        
        newTask.setProgressPercentage(0);
        newTask.setEstimatedHours(8.0); // Default 8 hours
        
        // Create the task in the database
        Task createdTask = taskRepository.create(newTask);
        
        // Show success message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Task Created");
        alert.setHeaderText("New Task Added");
        alert.setContentText("Task '" + createdTask.getTitle() + "' has been created for " + 
                           date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + 
                           "\n\nNote: New tasks appear at the end of the day's schedule.");
        alert.showAndWait();
    }
    
    private void showTimeSlotDetails(LocalDate date, int hour, List<Task> allDayTasks) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(String.format("Tasks for %s at %d:00", 
            date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), hour));
        dialog.setHeaderText(null);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setMinWidth(600);
        content.setMinHeight(400);
        
        // Filter tasks for this time slot (simplified - in reality would need actual time data)
        List<Task> hourTasks = new ArrayList<>();
        if (hour >= 8 && hour <= 17 && !allDayTasks.isEmpty()) {
            int tasksPerHour = Math.max(1, allDayTasks.size() / 10);
            int startIdx = (hour - 8) * tasksPerHour;
            int endIdx = Math.min(startIdx + tasksPerHour, allDayTasks.size());
            for (int i = startIdx; i < endIdx; i++) {
                hourTasks.add(allDayTasks.get(i));
            }
        }
        
        if (hourTasks.isEmpty()) {
            Label noTasksLabel = new Label("No tasks scheduled for this time slot");
            noTasksLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
            content.getChildren().add(noTasksLabel);
        } else {
            ScrollPane scrollPane = new ScrollPane();
            VBox taskContainer = new VBox(10);
            
            for (Task task : hourTasks) {
                VBox taskCard = createDetailedTaskCard(task);
                taskContainer.getChildren().add(taskCard);
            }
            
            scrollPane.setContent(taskContainer);
            scrollPane.setFitToWidth(true);
            content.getChildren().add(scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }
        
        // Add button to create new task for this time slot
        Button addTaskButton = new Button(String.format("Add Task for %d:00", hour));
        addTaskButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addTaskButton.setOnAction(e -> {
            createTaskForDay(date);  // For now, use same method as day view
            dialog.close();
            loadTaskData();
            refreshCurrentView();
        });
        
        content.getChildren().add(addTaskButton);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Set dialog size
        dialog.setOnShown(e -> {
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            dialogStage.setMinWidth(650);
            dialogStage.setMinHeight(500);
        });
        
        dialog.showAndWait();
    }
    
    private VBox createCompactTaskCard(Task task) {
        VBox card = new VBox(2);
        card.setPadding(new Insets(5));
        card.setStyle("-fx-background-color: " + getTaskColor(task) + "; -fx-border-radius: 3; -fx-background-radius: 3;");
        
        // Title
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        titleLabel.setTextFill(Color.BLACK);
        titleLabel.setWrapText(true);
        
        // Status and priority in one line
        HBox statusLine = new HBox(5);
        Label statusLabel = new Label(task.getStatus() != null ? task.getStatus().toString() : "");
        statusLabel.setFont(Font.font(9));
        statusLabel.setTextFill(Color.BLACK);
        
        Label priorityLabel = new Label(task.getPriority() != null ? "• " + task.getPriority().toString() : "");
        priorityLabel.setFont(Font.font(9));
        priorityLabel.setTextFill(Color.BLACK);
        
        statusLine.getChildren().addAll(statusLabel, priorityLabel);
        
        // Progress bar (thin)
        ProgressBar progressBar = new ProgressBar(task.getProgressPercentage() / 100.0);
        progressBar.setPrefHeight(5);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        
        card.getChildren().addAll(titleLabel, statusLine, progressBar);
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + getTaskColor(task) + 
            "; -fx-border-radius: 3; -fx-background-radius: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: " + getTaskColor(task) + 
            "; -fx-border-radius: 3; -fx-background-radius: 3;"));
        
        return card;
    }
    
    private HBox createDetailedDayTaskCard(Task task) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: white; -fx-border-color: " + getTaskColor(task) + 
            "; -fx-border-width: 0 0 0 4; -fx-border-radius: 3;");
        card.setAlignment(Pos.CENTER_LEFT);
        
        // Progress indicator
        ProgressIndicator progress = new ProgressIndicator(task.getProgressPercentage() / 100.0);
        progress.setMaxSize(30, 30);
        
        // Task details
        VBox details = new VBox(3);
        HBox.setHgrow(details, Priority.ALWAYS);
        
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        HBox metaInfo = new HBox(10);
        Label priorityLabel = new Label("Priority: " + (task.getPriority() != null ? task.getPriority() : "None"));
        priorityLabel.setFont(Font.font(10));
        priorityLabel.setStyle("-fx-text-fill: #666;");
        
        Label progressLabel = new Label("Progress: " + task.getProgressPercentage() + "%");
        progressLabel.setFont(Font.font(10));
        progressLabel.setStyle("-fx-text-fill: #666;");
        
        metaInfo.getChildren().addAll(priorityLabel, progressLabel);
        
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            Label descLabel = new Label(task.getDescription());
            descLabel.setWrapText(true);
            descLabel.setFont(Font.font(11));
            descLabel.setStyle("-fx-text-fill: #444;");
            details.getChildren().add(descLabel);
        }
        
        details.getChildren().addAll(titleLabel, metaInfo);
        
        card.getChildren().addAll(progress, details);
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: " + 
            getTaskColor(task) + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 3;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-border-color: " + 
            getTaskColor(task) + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 3;"));
        
        return card;
    }
    
    private HBox createLegend() {
        HBox legend = new HBox(15);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(5));
        legend.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #ddd;");
        
        legend.getChildren().addAll(
            createLegendItem("Critical", Color.web("#dc3545")),
            createLegendItem("High", Color.web("#fd7e14")),
            createLegendItem("Medium", Color.web("#ffc107")),
            createLegendItem("Low", Color.web("#28a745")),
            new Separator(),
            createLegendItem("Completed", Color.web("#28a745")),
            createLegendItem("In Progress", Color.web("#007bff")),
            createLegendItem("Blocked", Color.web("#dc3545")),
            createLegendItem("Review", Color.web("#6f42c1"))
        );
        
        return legend;
    }
    
    private HBox createLegendItem(String text, Color color) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER_LEFT);
        
        Label dot = new Label("●");
        dot.setTextFill(color);
        
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px;");
        
        item.getChildren().addAll(dot, label);
        return item;
    }
    
    public void show() {
        stage.show();
        stage.centerOnScreen();
    }
}