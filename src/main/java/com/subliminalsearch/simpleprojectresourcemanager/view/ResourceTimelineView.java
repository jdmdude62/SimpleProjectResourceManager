package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceTimelineView {
    private final Stage stage;
    private final Project project;
    private final TaskRepository taskRepository;
    private final ResourceRepository resourceRepository;
    
    private GridPane timelineGrid;
    private ScrollPane scrollPane;
    private LocalDate startDate;
    private LocalDate endDate;
    private int daysToShow = 30;
    private Map<String, List<Task>> resourceTaskMap;
    private Map<String, Map<LocalDate, Double>> utilizationMap;
    
    private static final Color COLOR_AVAILABLE = Color.LIGHTGREEN;
    private static final Color COLOR_LOW = Color.LIGHTBLUE;
    private static final Color COLOR_MEDIUM = Color.YELLOW;
    private static final Color COLOR_HIGH = Color.ORANGE;
    private static final Color COLOR_OVERALLOCATED = Color.RED;
    private static final Color COLOR_WEEKEND = Color.LIGHTGRAY;
    private static final Color COLOR_BLOCKED = Color.DARKRED;
    
    public ResourceTimelineView(Project project, TaskRepository taskRepository, ResourceRepository resourceRepository) {
        this.project = project;
        this.taskRepository = taskRepository;
        this.resourceRepository = resourceRepository;
        this.stage = new Stage();
        // Start with the current month view
        this.startDate = LocalDate.now().withDayOfMonth(1);
        this.endDate = startDate.plusMonths(1).minusDays(1);
        this.daysToShow = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        initialize();
    }
    
    private void initialize() {
        stage.setTitle("Resource Timeline - " + project.getProjectId());
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Controls bar
        HBox controls = createControlsBar();
        
        // Legend
        HBox legend = createLegend();
        
        // Timeline grid in scroll pane
        scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        timelineGrid = new GridPane();
        timelineGrid.setGridLinesVisible(true);
        scrollPane.setContent(timelineGrid);
        
        // Status bar
        Label statusBar = new Label("Loading timeline...");
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 5;");
        
        root.getChildren().addAll(controls, legend, scrollPane, statusBar);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Calculate width to show full month: resource column (150) + (days * cell width)
        // Each day cell is 40px wide, plus some padding for scrollbar and borders
        int windowWidth = Math.max(1400, 200 + (daysToShow * 40) + 50);
        Scene scene = new Scene(root, windowWidth, 700);
        scene.getStylesheets().add(getClass().getResource("/css/timeline.css").toExternalForm());
        stage.setScene(scene);
        
        // Ensure the window opens at the calculated size
        stage.setMinWidth(windowWidth);
        stage.setMinHeight(700);
        stage.setWidth(windowWidth);
        stage.setHeight(700);
        
        loadTimelineData();
        buildTimelineGrid();
        
        statusBar.setText(String.format("Showing %d resources across %d days", 
            resourceTaskMap.size(), daysToShow));
    }
    
    private HBox createControlsBar() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(5));
        controls.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #ddd;");
        
        // Date range controls
        DatePicker startPicker = new DatePicker(startDate);
        startPicker.setPromptText("Start Date");
        startPicker.setPrefWidth(120);
        
        DatePicker endPicker = new DatePicker(endDate);
        endPicker.setPromptText("End Date");
        endPicker.setPrefWidth(120);
        
        Button updateButton = new Button("Update");
        updateButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        
        // View options
        ComboBox<String> viewMode = new ComboBox<>();
        viewMode.getItems().addAll("Daily", "Weekly", "Monthly");
        viewMode.setValue("Daily");
        viewMode.setPrefWidth(100);
        
        ComboBox<String> resourceFilter = new ComboBox<>();
        resourceFilter.getItems().add("All Resources");
        resourceFilter.getItems().add("Field Technicians");
        resourceFilter.getItems().add("Equipment");
        resourceFilter.getItems().add("Vehicles");
        resourceFilter.getItems().add("Third Party");
        resourceFilter.setValue("All Resources");
        resourceFilter.setPrefWidth(150);
        
        // Quick date ranges
        Button todayBtn = new Button("Today");
        Button weekBtn = new Button("This Week");
        Button monthBtn = new Button("This Month");
        Button quarterBtn = new Button("This Quarter");
        
        todayBtn.setOnAction(e -> {
            startDate = LocalDate.now();
            endDate = startDate.plusDays(1);
            startPicker.setValue(startDate);
            endPicker.setValue(endDate);
            refreshTimeline();
        });
        
        weekBtn.setOnAction(e -> {
            startDate = LocalDate.now().with(DayOfWeek.MONDAY);
            endDate = startDate.plusDays(6);
            startPicker.setValue(startDate);
            endPicker.setValue(endDate);
            refreshTimeline();
        });
        
        monthBtn.setOnAction(e -> {
            startDate = LocalDate.now().withDayOfMonth(1);
            endDate = startDate.plusMonths(1).minusDays(1);
            startPicker.setValue(startDate);
            endPicker.setValue(endDate);
            refreshTimeline();
        });
        
        quarterBtn.setOnAction(e -> {
            startDate = LocalDate.now().withDayOfMonth(1);
            endDate = startDate.plusMonths(3).minusDays(1);
            startPicker.setValue(startDate);
            endPicker.setValue(endDate);
            refreshTimeline();
        });
        
        updateButton.setOnAction(e -> {
            startDate = startPicker.getValue();
            endDate = endPicker.getValue();
            if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
                daysToShow = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
                refreshTimeline();
            }
        });
        
        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        controls.getChildren().addAll(
            new Label("From:"), startPicker,
            new Label("To:"), endPicker,
            updateButton,
            sep1,
            todayBtn, weekBtn, monthBtn, quarterBtn,
            sep2,
            new Label("View:"), viewMode,
            new Label("Filter:"), resourceFilter
        );
        
        resourceFilter.setOnAction(e -> {
            filterResources(resourceFilter.getValue());
        });
        
        return controls;
    }
    
    private HBox createLegend() {
        HBox legend = new HBox(15);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(5));
        legend.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        
        legend.getChildren().addAll(
            createLegendItem("Available", COLOR_AVAILABLE),
            createLegendItem("< 25%", COLOR_LOW),
            createLegendItem("25-50%", COLOR_MEDIUM),
            createLegendItem("50-75%", COLOR_HIGH),
            createLegendItem("75-100%", COLOR_OVERALLOCATED),
            createLegendItem("Blocked", COLOR_BLOCKED),
            createLegendItem("Weekend", COLOR_WEEKEND)
        );
        
        return legend;
    }
    
    private HBox createLegendItem(String text, Color color) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER_LEFT);
        
        Rectangle rect = new Rectangle(15, 15);
        rect.setFill(color);
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(0.5);
        
        Label label = new Label(text);
        
        item.getChildren().addAll(rect, label);
        return item;
    }
    
    private void loadTimelineData() {
        resourceTaskMap = new HashMap<>();
        utilizationMap = new HashMap<>();
        
        // Load all resources
        List<Resource> resources = resourceRepository.findActiveResources();
        
        // Load all tasks for the project
        List<Task> projectTasks = taskRepository.findByProjectId(project.getId());
        
        // Group tasks by resource
        for (Resource resource : resources) {
            String resourceKey = resource.getName();
            List<Task> resourceTasks = projectTasks.stream()
                .filter(t -> t.getAssignedTo() != null && t.getAssignedTo().equals(resource.getId()))
                .collect(Collectors.toList());
            
            if (!resourceTasks.isEmpty()) {
                resourceTaskMap.put(resourceKey, resourceTasks);
                
                // Calculate utilization for each day
                Map<LocalDate, Double> dailyUtilization = new HashMap<>();
                LocalDate current = startDate;
                
                while (!current.isAfter(endDate)) {
                    double utilization = calculateUtilizationForDay(resourceTasks, current);
                    dailyUtilization.put(current, utilization);
                    current = current.plusDays(1);
                }
                
                utilizationMap.put(resourceKey, dailyUtilization);
            }
        }
        
        // Add unassigned tasks as a special row
        List<Task> unassignedTasks = projectTasks.stream()
            .filter(t -> t.getAssignedTo() == null)
            .collect(Collectors.toList());
        
        if (!unassignedTasks.isEmpty()) {
            resourceTaskMap.put("⚠ Unassigned", unassignedTasks);
            
            Map<LocalDate, Double> unassignedUtilization = new HashMap<>();
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                final LocalDate checkDate = current;
                unassignedUtilization.put(current, 
                    unassignedTasks.stream()
                        .anyMatch(t -> isTaskActiveOnDate(t, checkDate)) ? 1.0 : 0.0);
                current = current.plusDays(1);
            }
            utilizationMap.put("⚠ Unassigned", unassignedUtilization);
        }
    }
    
    private double calculateUtilizationForDay(List<Task> tasks, LocalDate date) {
        double totalHours = 0;
        double workingHours = 8.0; // Standard working day
        
        for (Task task : tasks) {
            if (isTaskActiveOnDate(task, date)) {
                // Check for blocked status
                if (task.getStatus() == Task.TaskStatus.BLOCKED) {
                    return -1.0; // Special value for blocked
                }
                
                // Calculate hours for this task on this day
                if (task.getEstimatedHours() != null && task.getPlannedStart() != null && task.getPlannedEnd() != null) {
                    long taskDays = ChronoUnit.DAYS.between(task.getPlannedStart(), task.getPlannedEnd()) + 1;
                    if (taskDays > 0) {
                        double dailyHours = task.getEstimatedHours() / taskDays;
                        totalHours += dailyHours;
                    }
                } else {
                    // Default to 4 hours if no estimate
                    totalHours += 4.0;
                }
            }
        }
        
        return totalHours / workingHours;
    }
    
    private boolean isTaskActiveOnDate(Task task, LocalDate date) {
        LocalDate taskStart = task.getPlannedStart();
        LocalDate taskEnd = task.getPlannedEnd();
        
        if (taskStart == null || taskEnd == null) {
            return false;
        }
        
        return !date.isBefore(taskStart) && !date.isAfter(taskEnd);
    }
    
    private void buildTimelineGrid() {
        timelineGrid.getChildren().clear();
        timelineGrid.getColumnConstraints().clear();
        timelineGrid.getRowConstraints().clear();
        
        // Header row
        Label cornerLabel = new Label("Resource / Date");
        cornerLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e0e0; -fx-padding: 5;");
        cornerLabel.setMinWidth(150);
        cornerLabel.setMaxWidth(150);
        timelineGrid.add(cornerLabel, 0, 0);
        
        // Date headers
        LocalDate current = startDate;
        int col = 1;
        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("EEE");
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd");
        
        while (!current.isAfter(endDate)) {
            VBox dateHeader = new VBox(2);
            dateHeader.setAlignment(Pos.CENTER);
            dateHeader.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 3;");
            dateHeader.setMinWidth(40);
            
            Label dayLabel = new Label(current.format(dayFormat));
            dayLabel.setStyle("-fx-font-size: 10px;");
            
            Label dateLabel = new Label(current.format(dateFormat));
            dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            
            // Highlight weekends
            if (current.getDayOfWeek() == DayOfWeek.SATURDAY || current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                dateHeader.setStyle("-fx-background-color: #d0d0d0; -fx-padding: 3;");
            }
            
            // Highlight today
            if (current.equals(LocalDate.now())) {
                dateHeader.setStyle("-fx-background-color: #b3d9ff; -fx-padding: 3; -fx-border-color: #007bff; -fx-border-width: 2;");
            }
            
            dateHeader.getChildren().addAll(dayLabel, dateLabel);
            timelineGrid.add(dateHeader, col, 0);
            
            current = current.plusDays(1);
            col++;
        }
        
        // Resource rows
        int row = 1;
        for (Map.Entry<String, List<Task>> entry : resourceTaskMap.entrySet()) {
            String resourceName = entry.getKey();
            List<Task> tasks = entry.getValue();
            
            // Resource name cell
            Label resourceLabel = new Label(resourceName);
            resourceLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #f0f0f0; -fx-padding: 5;");
            resourceLabel.setMinWidth(150);
            resourceLabel.setMaxWidth(150);
            
            // Add task count
            long activeTaskCount = tasks.stream()
                .filter(t -> t.getStatus() != Task.TaskStatus.COMPLETED && 
                            t.getStatus() != Task.TaskStatus.CANCELLED)
                .count();
            if (activeTaskCount > 0) {
                resourceLabel.setText(resourceName + " (" + activeTaskCount + ")");
            }
            
            timelineGrid.add(resourceLabel, 0, row);
            
            // Add utilization cells
            Map<LocalDate, Double> dailyUtilization = utilizationMap.get(resourceName);
            if (dailyUtilization != null) {
                current = startDate;
                col = 1;
                
                while (!current.isAfter(endDate)) {
                    Double utilization = dailyUtilization.get(current);
                    StackPane cell = createUtilizationCell(utilization, current, resourceName, tasks);
                    timelineGrid.add(cell, col, row);
                    
                    current = current.plusDays(1);
                    col++;
                }
            }
            
            row++;
        }
        
        // Add summary row
        addSummaryRow(row);
    }
    
    private StackPane createUtilizationCell(Double utilization, LocalDate date, String resourceName, List<Task> tasks) {
        StackPane cell = new StackPane();
        cell.setMinWidth(40);
        cell.setMinHeight(30);
        cell.setAlignment(Pos.CENTER);
        
        Rectangle background = new Rectangle(40, 30);
        
        // Determine color based on utilization
        Color cellColor;
        String tooltipText;
        
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cellColor = COLOR_WEEKEND;
            tooltipText = "Weekend";
        } else if (utilization == null || utilization == 0) {
            cellColor = COLOR_AVAILABLE;
            tooltipText = "Available";
        } else if (utilization < 0) {
            cellColor = COLOR_BLOCKED;
            tooltipText = "Blocked";
        } else if (utilization < 0.25) {
            cellColor = COLOR_LOW;
            tooltipText = String.format("%.0f%% utilized", utilization * 100);
        } else if (utilization < 0.5) {
            cellColor = COLOR_MEDIUM;
            tooltipText = String.format("%.0f%% utilized", utilization * 100);
        } else if (utilization < 0.75) {
            cellColor = COLOR_HIGH;
            tooltipText = String.format("%.0f%% utilized", utilization * 100);
        } else {
            cellColor = COLOR_OVERALLOCATED;
            tooltipText = utilization > 1.0 ? 
                String.format("%.0f%% OVERALLOCATED!", utilization * 100) :
                String.format("%.0f%% utilized", utilization * 100);
        }
        
        background.setFill(cellColor);
        background.setStroke(Color.LIGHTGRAY);
        background.setStrokeWidth(0.5);
        
        // Add task indicators
        List<Task> dayTasks = tasks.stream()
            .filter(t -> isTaskActiveOnDate(t, date))
            .collect(Collectors.toList());
        
        if (!dayTasks.isEmpty()) {
            VBox indicators = new VBox(2);
            indicators.setAlignment(Pos.CENTER);
            
            for (Task task : dayTasks) {
                if (indicators.getChildren().size() >= 2) {
                    Label more = new Label("+" + (dayTasks.size() - 2));
                    more.setStyle("-fx-font-size: 8px; -fx-text-fill: white;");
                    indicators.getChildren().add(more);
                    break;
                }
                
                Label taskIndicator = new Label("●");
                taskIndicator.setStyle("-fx-font-size: 8px; -fx-text-fill: " + 
                    getPriorityColor(task.getPriority()));
                indicators.getChildren().add(taskIndicator);
            }
            
            cell.getChildren().addAll(background, indicators);
        } else {
            cell.getChildren().add(background);
        }
        
        // Add tooltip with task details
        StringBuilder tooltipBuilder = new StringBuilder();
        tooltipBuilder.append(resourceName).append(" - ").append(date.toString()).append("\n");
        tooltipBuilder.append(tooltipText);
        
        if (!dayTasks.isEmpty()) {
            tooltipBuilder.append("\n\nTasks:");
            for (Task task : dayTasks) {
                tooltipBuilder.append("\n• ").append(task.getTitle());
                if (task.getPriority() != null) {
                    tooltipBuilder.append(" [").append(task.getPriority()).append("]");
                }
                if (task.getStatus() != null) {
                    tooltipBuilder.append(" - ").append(task.getStatus());
                }
            }
        }
        
        Tooltip tooltip = new Tooltip(tooltipBuilder.toString());
        tooltip.setShowDelay(Duration.millis(200));
        tooltip.setAutoHide(false); // Don't auto-hide
        tooltip.setHideDelay(Duration.millis(1000)); // Longer hide delay
        tooltip.setStyle("-fx-background-color: #ffffcc; -fx-text-fill: black; -fx-border-color: #666; -fx-border-width: 1; -fx-font-size: 11px; -fx-padding: 5;");
        
        // Custom positioning to avoid blocking the cell
        cell.setOnMouseEntered(e -> {
            tooltip.show(cell, 
                e.getScreenX() + 15,  // Offset to the right
                e.getScreenY() - 30); // Offset above
        });
        
        cell.setOnMouseExited(e -> {
            tooltip.hide();
        });
        
        // Click to show task details
        cell.setOnMouseClicked(e -> {
            if (!dayTasks.isEmpty()) {
                showDayDetails(resourceName, date, dayTasks);
            }
        });
        
        return cell;
    }
    
    private String getPriorityColor(Task.TaskPriority priority) {
        if (priority == null) return "gray";
        switch (priority) {
            case CRITICAL: return "darkred";
            case HIGH: return "red";
            case MEDIUM: return "orange";
            case LOW: return "green";
            default: return "gray";
        }
    }
    
    private void addSummaryRow(int row) {
        Label summaryLabel = new Label("Daily Capacity %");
        summaryLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #d0d0d0; -fx-padding: 5;");
        summaryLabel.setMinWidth(150);
        timelineGrid.add(summaryLabel, 0, row);
        
        LocalDate current = startDate;
        int col = 1;
        
        while (!current.isAfter(endDate)) {
            double totalCapacity = 0;
            int resourceCount = 0;
            
            for (Map.Entry<String, Map<LocalDate, Double>> entry : utilizationMap.entrySet()) {
                if (!entry.getKey().startsWith("⚠")) { // Skip unassigned
                    Double utilization = entry.getValue().get(current);
                    if (utilization != null && utilization >= 0) {
                        totalCapacity += utilization;
                        resourceCount++;
                    }
                }
            }
            
            double avgCapacity = resourceCount > 0 ? totalCapacity / resourceCount : 0;
            
            Label capacityLabel = new Label(String.format("%.0f%%", avgCapacity * 100));
            capacityLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            
            StackPane summaryCell = new StackPane();
            summaryCell.setMinWidth(40);
            summaryCell.setMinHeight(25);
            summaryCell.setAlignment(Pos.CENTER);
            
            Rectangle bg = new Rectangle(40, 25);
            if (avgCapacity > 0.8) {
                bg.setFill(Color.LIGHTCORAL);
            } else if (avgCapacity > 0.6) {
                bg.setFill(Color.LIGHTYELLOW);
            } else {
                bg.setFill(Color.LIGHTGREEN);
            }
            bg.setStroke(Color.GRAY);
            bg.setStrokeWidth(0.5);
            
            summaryCell.getChildren().addAll(bg, capacityLabel);
            timelineGrid.add(summaryCell, col, row);
            
            current = current.plusDays(1);
            col++;
        }
    }
    
    private void showDayDetails(String resourceName, LocalDate date, List<Task> tasks) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Tasks for " + resourceName + " on " + date);
        dialog.setHeaderText(null);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setMinWidth(500);
        content.setMinHeight(300);
        
        for (Task task : tasks) {
            VBox taskBox = new VBox(5);
            taskBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-padding: 10;");
            
            Label titleLabel = new Label(task.getTitle());
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            
            Label detailsLabel = new Label(String.format("Status: %s | Priority: %s | Progress: %d%%",
                task.getStatus(), task.getPriority(), task.getProgressPercentage()));
            
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                Label descLabel = new Label(task.getDescription());
                descLabel.setWrapText(true);
                taskBox.getChildren().add(descLabel);
            }
            
            taskBox.getChildren().addAll(titleLabel, detailsLabel);
            content.getChildren().add(taskBox);
        }
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }
    
    private void filterResources(String filterType) {
        // Reload data with filter
        loadTimelineData();
        
        if (!filterType.equals("All Resources")) {
            Map<String, List<Task>> filteredMap = new HashMap<>();
            Map<String, Map<LocalDate, Double>> filteredUtilization = new HashMap<>();
            
            for (Map.Entry<String, List<Task>> entry : resourceTaskMap.entrySet()) {
                // Simple filtering based on resource name patterns
                String resourceName = entry.getKey();
                boolean include = false;
                
                switch (filterType) {
                    case "Field Technicians":
                        include = !resourceName.startsWith("⚠") && 
                                 !resourceName.toLowerCase().contains("vehicle") &&
                                 !resourceName.toLowerCase().contains("equipment");
                        break;
                    case "Equipment":
                        include = resourceName.toLowerCase().contains("equipment");
                        break;
                    case "Vehicles":
                        include = resourceName.toLowerCase().contains("vehicle");
                        break;
                    case "Third Party":
                        include = resourceName.toLowerCase().contains("vendor") ||
                                 resourceName.toLowerCase().contains("contractor");
                        break;
                }
                
                if (include) {
                    filteredMap.put(resourceName, entry.getValue());
                    filteredUtilization.put(resourceName, utilizationMap.get(resourceName));
                }
            }
            
            resourceTaskMap = filteredMap;
            utilizationMap = filteredUtilization;
        }
        
        buildTimelineGrid();
    }
    
    private void refreshTimeline() {
        // Recalculate days to show
        daysToShow = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        // Resize window if needed to accommodate new date range
        int newWindowWidth = Math.max(1400, 200 + (daysToShow * 40) + 50);
        if (stage.getWidth() < newWindowWidth) {
            stage.setWidth(newWindowWidth);
            stage.setMinWidth(newWindowWidth);
        }
        
        loadTimelineData();
        buildTimelineGrid();
    }
    
    public void show() {
        stage.show();
        stage.centerOnScreen();
    }
}