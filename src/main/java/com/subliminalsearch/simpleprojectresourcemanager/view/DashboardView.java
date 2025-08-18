package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardView {
    private final Stage stage;
    private final Project project;
    private final TaskRepository taskRepository;
    private final ResourceRepository resourceRepository;
    
    private GridPane dashboardGrid;
    private List<Task> allTasks;
    private List<Resource> allResources;
    
    public DashboardView(Project project, TaskRepository taskRepository, ResourceRepository resourceRepository) {
        this.project = project;
        this.taskRepository = taskRepository;
        this.resourceRepository = resourceRepository;
        this.stage = new Stage();
        
        initialize();
    }
    
    private void initialize() {
        stage.setTitle("Project Dashboard - " + project.getProjectId());
        
        ScrollPane scrollPane = new ScrollPane();
        dashboardGrid = new GridPane();
        dashboardGrid.setHgap(15);
        dashboardGrid.setVgap(15);
        dashboardGrid.setPadding(new Insets(15));
        dashboardGrid.setStyle("-fx-background-color: #f5f5f5;");
        
        scrollPane.setContent(dashboardGrid);
        scrollPane.setFitToWidth(true);
        
        loadData();
        buildDashboard();
        
        Scene scene = new Scene(scrollPane, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.setMaximized(false);
    }
    
    private void loadData() {
        allTasks = taskRepository.findByProjectId(project.getId());
        allResources = resourceRepository.findActiveResources();
    }
    
    private void buildDashboard() {
        // Row 0: Header with project info
        VBox headerCard = createHeaderCard();
        dashboardGrid.add(headerCard, 0, 0, 3, 1);
        
        // Row 1: Key metrics cards
        VBox taskMetricsCard = createTaskMetricsCard();
        VBox scheduleMetricsCard = createScheduleMetricsCard();
        VBox resourceMetricsCard = createResourceMetricsCard();
        VBox riskMetricsCard = createRiskMetricsCard();
        
        dashboardGrid.add(taskMetricsCard, 0, 1);
        dashboardGrid.add(scheduleMetricsCard, 1, 1);
        dashboardGrid.add(resourceMetricsCard, 2, 1);
        dashboardGrid.add(riskMetricsCard, 3, 1);
        
        // Row 2: Charts
        VBox taskStatusChart = createTaskStatusChart();
        VBox priorityChart = createPriorityChart();
        VBox progressChart = createProgressChart();
        
        dashboardGrid.add(taskStatusChart, 0, 2);
        dashboardGrid.add(priorityChart, 1, 2);
        dashboardGrid.add(progressChart, 2, 2, 2, 1);
        
        // Row 3: Field Service Specific Metrics
        VBox fieldServiceCard = createFieldServiceMetricsCard();
        VBox weatherImpactCard = createWeatherImpactCard();
        VBox materialsCard = createMaterialsTrackingCard();
        
        dashboardGrid.add(fieldServiceCard, 0, 3);
        dashboardGrid.add(weatherImpactCard, 1, 3);
        dashboardGrid.add(materialsCard, 2, 3, 2, 1);
        
        // Row 4: Resource utilization and timeline
        VBox utilizationChart = createUtilizationChart();
        VBox timelineChart = createTimelineChart();
        
        dashboardGrid.add(utilizationChart, 0, 4, 2, 1);
        dashboardGrid.add(timelineChart, 2, 4, 2, 1);
        
        // Row 5: Critical items and alerts
        VBox criticalTasksCard = createCriticalTasksCard();
        VBox alertsCard = createAlertsCard();
        
        dashboardGrid.add(criticalTasksCard, 0, 5, 2, 1);
        dashboardGrid.add(alertsCard, 2, 5, 2, 1);
    }
    
    private VBox createHeaderCard() {
        VBox card = createCard("Project Overview");
        
        Label projectName = new Label(project.getProjectId());
        projectName.setFont(Font.font("System", FontWeight.BOLD, 24));
        
        Label description = new Label(project.getDescription());
        description.setWrapText(true);
        
        HBox dateInfo = new HBox(20);
        Label startDate = new Label("Start: " + project.getStartDate());
        Label endDate = new Label("End: " + project.getEndDate());
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), project.getEndDate());
        Label remaining = new Label("Days Remaining: " + daysRemaining);
        remaining.setTextFill(daysRemaining < 30 ? Color.RED : Color.GREEN);
        
        dateInfo.getChildren().addAll(startDate, endDate, remaining);
        
        // Overall progress
        double overallProgress = calculateOverallProgress();
        ProgressBar progressBar = new ProgressBar(overallProgress / 100);
        progressBar.setPrefWidth(400);
        Label progressLabel = new Label(String.format("Overall Progress: %.1f%%", overallProgress));
        
        card.getChildren().addAll(projectName, description, dateInfo, progressBar, progressLabel);
        
        return card;
    }
    
    private VBox createTaskMetricsCard() {
        VBox card = createMetricCard("Tasks", String.valueOf(allTasks.size()), Color.BLUE);
        
        long completed = allTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED).count();
        long inProgress = allTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS).count();
        long notStarted = allTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.NOT_STARTED).count();
        
        addMetricDetail(card, "Completed", String.valueOf(completed));
        addMetricDetail(card, "In Progress", String.valueOf(inProgress));
        addMetricDetail(card, "Not Started", String.valueOf(notStarted));
        
        return card;
    }
    
    private VBox createScheduleMetricsCard() {
        VBox card = createMetricCard("Schedule", "", Color.GREEN);
        
        long overdueTasks = allTasks.stream()
            .filter(t -> t.getPlannedEnd() != null && 
                        t.getPlannedEnd().isBefore(LocalDate.now()) &&
                        t.getStatus() != Task.TaskStatus.COMPLETED)
            .count();
        
        long tasksThisWeek = allTasks.stream()
            .filter(t -> {
                LocalDate weekStart = LocalDate.now();
                LocalDate weekEnd = weekStart.plusDays(7);
                return t.getPlannedStart() != null &&
                       !t.getPlannedStart().isBefore(weekStart) &&
                       !t.getPlannedStart().isAfter(weekEnd);
            })
            .count();
        
        Label statusLabel = new Label(overdueTasks > 0 ? "AT RISK" : "ON TRACK");
        statusLabel.setTextFill(overdueTasks > 0 ? Color.RED : Color.GREEN);
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        card.getChildren().add(1, statusLabel);
        
        addMetricDetail(card, "Overdue", String.valueOf(overdueTasks));
        addMetricDetail(card, "This Week", String.valueOf(tasksThisWeek));
        
        return card;
    }
    
    private VBox createResourceMetricsCard() {
        VBox card = createMetricCard("Resources", String.valueOf(allResources.size()), Color.ORANGE);
        
        // Count resources with tasks
        Set<Long> assignedResourceIds = allTasks.stream()
            .map(Task::getAssignedTo)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        long activeResources = assignedResourceIds.size();
        long availableResources = allResources.size() - activeResources;
        
        addMetricDetail(card, "Active", String.valueOf(activeResources));
        addMetricDetail(card, "Available", String.valueOf(availableResources));
        
        return card;
    }
    
    private VBox createRiskMetricsCard() {
        VBox card = createMetricCard("Risks", "", Color.RED);
        
        long highRiskTasks = allTasks.stream()
            .filter(t -> t.getRiskLevel() == Task.RiskLevel.HIGH)
            .count();
        
        long blockedTasks = allTasks.stream()
            .filter(t -> t.getStatus() == Task.TaskStatus.BLOCKED)
            .count();
        
        Label riskLabel = new Label(highRiskTasks > 5 ? "HIGH" : highRiskTasks > 0 ? "MEDIUM" : "LOW");
        riskLabel.setTextFill(highRiskTasks > 5 ? Color.RED : highRiskTasks > 0 ? Color.ORANGE : Color.GREEN);
        riskLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        card.getChildren().add(1, riskLabel);
        
        addMetricDetail(card, "High Risk", String.valueOf(highRiskTasks));
        addMetricDetail(card, "Blocked", String.valueOf(blockedTasks));
        
        return card;
    }
    
    private VBox createFieldServiceMetricsCard() {
        VBox card = createCard("Field Service Metrics");
        
        long onSiteTasks = allTasks.stream()
            .filter(t -> t.getLocation() != null && !t.getLocation().isEmpty())
            .count();
        
        long weatherImpacted = allTasks.stream()
            .filter(t -> t.getRiskNotes() != null && t.getRiskNotes().contains("[weather]"))
            .count();
        
        long materialDelays = allTasks.stream()
            .filter(t -> t.getRiskNotes() != null && t.getRiskNotes().contains("[materials]"))
            .count();
        
        addMetricRow(card, "On-Site Tasks", String.valueOf(onSiteTasks));
        addMetricRow(card, "Weather Impacted", String.valueOf(weatherImpacted));
        addMetricRow(card, "Material Delays", String.valueOf(materialDelays));
        
        // Travel time calculation
        double totalTravelDays = allTasks.stream()
            .filter(t -> t.getLocation() != null)
            .count() * 0.5; // Assume half day travel average
        
        addMetricRow(card, "Est. Travel Days", String.format("%.1f", totalTravelDays));
        
        return card;
    }
    
    private VBox createWeatherImpactCard() {
        VBox card = createCard("Weather Impact Analysis");
        
        List<Task> weatherTasks = allTasks.stream()
            .filter(t -> t.getRiskNotes() != null && t.getRiskNotes().contains("[weather]"))
            .collect(Collectors.toList());
        
        if (weatherTasks.isEmpty()) {
            Label noImpact = new Label("No weather impacts reported");
            noImpact.setStyle("-fx-text-fill: green;");
            card.getChildren().add(noImpact);
        } else {
            for (int i = 0; i < Math.min(3, weatherTasks.size()); i++) {
                Task task = weatherTasks.get(i);
                Label taskLabel = new Label("• " + task.getTitle());
                taskLabel.setStyle("-fx-font-size: 11px;");
                card.getChildren().add(taskLabel);
            }
            
            if (weatherTasks.size() > 3) {
                Label moreLabel = new Label("+" + (weatherTasks.size() - 3) + " more tasks");
                moreLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
                card.getChildren().add(moreLabel);
            }
        }
        
        return card;
    }
    
    private VBox createMaterialsTrackingCard() {
        VBox card = createCard("Materials & Equipment Status");
        
        List<Task> materialTasks = allTasks.stream()
            .filter(t -> t.getEquipmentRequired() != null && !t.getEquipmentRequired().isEmpty())
            .collect(Collectors.toList());
        
        addMetricRow(card, "Tasks Requiring Equipment", String.valueOf(materialTasks.size()));
        
        List<Task> materialDelays = allTasks.stream()
            .filter(t -> t.getRiskNotes() != null && t.getRiskNotes().contains("[materials]"))
            .collect(Collectors.toList());
        
        if (!materialDelays.isEmpty()) {
            Label delayHeader = new Label("Material Delays:");
            delayHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
            card.getChildren().add(delayHeader);
            
            for (int i = 0; i < Math.min(3, materialDelays.size()); i++) {
                Task task = materialDelays.get(i);
                Label taskLabel = new Label("• " + task.getTitle());
                taskLabel.setStyle("-fx-font-size: 11px;");
                card.getChildren().add(taskLabel);
            }
        }
        
        return card;
    }
    
    private VBox createTaskStatusChart() {
        VBox card = createCard("Task Status Distribution");
        
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        Map<Task.TaskStatus, Long> statusCounts = allTasks.stream()
            .collect(Collectors.groupingBy(
                t -> t.getStatus() != null ? t.getStatus() : Task.TaskStatus.NOT_STARTED,
                Collectors.counting()
            ));
        
        statusCounts.forEach((status, count) -> {
            pieChartData.add(new PieChart.Data(status.toString(), count));
        });
        
        PieChart chart = new PieChart(pieChartData);
        chart.setLegendSide(Side.BOTTOM);
        chart.setPrefHeight(250);
        chart.setLabelsVisible(true);
        
        card.getChildren().add(chart);
        
        return card;
    }
    
    private VBox createPriorityChart() {
        VBox card = createCard("Task Priority Breakdown");
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(250);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        Map<Task.TaskPriority, Long> priorityCounts = allTasks.stream()
            .collect(Collectors.groupingBy(
                t -> t.getPriority() != null ? t.getPriority() : Task.TaskPriority.MEDIUM,
                Collectors.counting()
            ));
        
        // Add chart to card first
        card.getChildren().add(barChart);
        
        // Delay adding data to avoid JavaFX compatibility issues
        javafx.application.Platform.runLater(() -> {
            try {
                for (Map.Entry<Task.TaskPriority, Long> entry : priorityCounts.entrySet()) {
                    XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey().toString(), entry.getValue());
                    series.getData().add(data);
                }
                barChart.getData().clear();
                barChart.getData().add(series);
            } catch (Exception e) {
                System.err.println("Error adding data to priority chart: " + e.getMessage());
                // Remove chart and add fallback
                card.getChildren().clear();
                Label titleLabel = new Label("Task Priority Breakdown");
                titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                card.getChildren().add(titleLabel);
                
                VBox fallback = new VBox(5);
                fallback.setPadding(new Insets(10));
                for (Map.Entry<Task.TaskPriority, Long> entry : priorityCounts.entrySet()) {
                    Label label = new Label(entry.getKey() + ": " + entry.getValue() + " tasks");
                    label.setStyle("-fx-font-size: 14px;");
                    fallback.getChildren().add(label);
                }
                card.getChildren().add(fallback);
            }
        });
        
        return card;
    }
    
    private VBox createProgressChart() {
        VBox card = createCard("Progress Overview");
        
        // Create stacked bar chart for progress
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Tasks");
        
        StackedBarChart<String, Number> stackedChart = new StackedBarChart<>(xAxis, yAxis);
        stackedChart.setPrefHeight(250);
        
        XYChart.Series<String, Number> completedSeries = new XYChart.Series<>();
        completedSeries.setName("Completed");
        
        XYChart.Series<String, Number> inProgressSeries = new XYChart.Series<>();
        inProgressSeries.setName("In Progress");
        
        XYChart.Series<String, Number> notStartedSeries = new XYChart.Series<>();
        notStartedSeries.setName("Not Started");
        
        // Group by week
        Map<String, Map<Task.TaskStatus, Long>> weeklyStatus = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < 4; i++) {
            String weekLabel = "Week " + (i + 1);
            LocalDate weekStart = today.plusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);
            
            Map<Task.TaskStatus, Long> statusCount = allTasks.stream()
                .filter(t -> t.getPlannedStart() != null &&
                            !t.getPlannedStart().isBefore(weekStart) &&
                            !t.getPlannedStart().isAfter(weekEnd))
                .collect(Collectors.groupingBy(
                    t -> t.getStatus() != null ? t.getStatus() : Task.TaskStatus.NOT_STARTED,
                    Collectors.counting()
                ));
            
            completedSeries.getData().add(new XYChart.Data<>(weekLabel, 
                statusCount.getOrDefault(Task.TaskStatus.COMPLETED, 0L)));
            inProgressSeries.getData().add(new XYChart.Data<>(weekLabel, 
                statusCount.getOrDefault(Task.TaskStatus.IN_PROGRESS, 0L)));
            notStartedSeries.getData().add(new XYChart.Data<>(weekLabel, 
                statusCount.getOrDefault(Task.TaskStatus.NOT_STARTED, 0L)));
        }
        
        stackedChart.getData().addAll(completedSeries, inProgressSeries, notStartedSeries);
        card.getChildren().add(stackedChart);
        
        return card;
    }
    
    private VBox createUtilizationChart() {
        VBox card = createCard("Resource Utilization");
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("Utilization %");
        
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setPrefHeight(300);
        lineChart.setLegendVisible(false);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Utilization");
        
        // Calculate utilization for next 7 days
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            double utilization = calculateUtilizationForDate(date);
            series.getData().add(new XYChart.Data<>(date.getDayOfWeek().toString().substring(0, 3), 
                                                    utilization * 100));
        }
        
        lineChart.getData().add(series);
        card.getChildren().add(lineChart);
        
        return card;
    }
    
    private VBox createTimelineChart() {
        VBox card = createCard("Upcoming Milestones");
        
        List<Task> upcomingTasks = allTasks.stream()
            .filter(t -> t.getPlannedStart() != null && 
                        t.getPlannedStart().isAfter(LocalDate.now()) &&
                        t.getPlannedStart().isBefore(LocalDate.now().plusDays(30)))
            .sorted(Comparator.comparing(Task::getPlannedStart))
            .limit(5)
            .collect(Collectors.toList());
        
        if (upcomingTasks.isEmpty()) {
            Label noTasks = new Label("No upcoming tasks in the next 30 days");
            card.getChildren().add(noTasks);
        } else {
            for (Task task : upcomingTasks) {
                HBox taskRow = new HBox(10);
                Label dateLabel = new Label(task.getPlannedStart().toString());
                dateLabel.setStyle("-fx-font-weight: bold;");
                Label taskLabel = new Label(task.getTitle());
                taskRow.getChildren().addAll(dateLabel, taskLabel);
                card.getChildren().add(taskRow);
            }
        }
        
        return card;
    }
    
    private VBox createCriticalTasksCard() {
        VBox card = createCard("Critical Tasks");
        
        List<Task> criticalTasks = allTasks.stream()
            .filter(t -> t.getPriority() == Task.TaskPriority.CRITICAL ||
                        (t.getPlannedEnd() != null && 
                         t.getPlannedEnd().isBefore(LocalDate.now()) &&
                         t.getStatus() != Task.TaskStatus.COMPLETED))
            .limit(5)
            .collect(Collectors.toList());
        
        if (criticalTasks.isEmpty()) {
            Label noCritical = new Label("No critical tasks");
            noCritical.setStyle("-fx-text-fill: green;");
            card.getChildren().add(noCritical);
        } else {
            for (Task task : criticalTasks) {
                VBox taskItem = new VBox(2);
                Label title = new Label("• " + task.getTitle());
                title.setStyle("-fx-font-weight: bold;");
                
                String status = "Status: " + task.getStatus();
                if (task.getPlannedEnd() != null && task.getPlannedEnd().isBefore(LocalDate.now())) {
                    status += " (OVERDUE)";
                }
                Label statusLabel = new Label(status);
                statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: red;");
                
                taskItem.getChildren().addAll(title, statusLabel);
                card.getChildren().add(taskItem);
            }
        }
        
        return card;
    }
    
    private VBox createAlertsCard() {
        VBox card = createCard("Alerts & Notifications");
        
        List<String> alerts = new ArrayList<>();
        
        // Check for overdue tasks
        long overdueTasks = allTasks.stream()
            .filter(t -> t.getPlannedEnd() != null && 
                        t.getPlannedEnd().isBefore(LocalDate.now()) &&
                        t.getStatus() != Task.TaskStatus.COMPLETED)
            .count();
        
        if (overdueTasks > 0) {
            alerts.add("⚠ " + overdueTasks + " tasks are overdue");
        }
        
        // Check for blocked tasks
        long blockedTasks = allTasks.stream()
            .filter(t -> t.getStatus() == Task.TaskStatus.BLOCKED)
            .count();
        
        if (blockedTasks > 0) {
            alerts.add("🚫 " + blockedTasks + " tasks are blocked");
        }
        
        // Check for weather impacts
        long weatherImpacted = allTasks.stream()
            .filter(t -> t.getRiskNotes() != null && t.getRiskNotes().contains("[weather]"))
            .count();
        
        if (weatherImpacted > 0) {
            alerts.add("🌧 " + weatherImpacted + " tasks impacted by weather");
        }
        
        // Check for material delays
        long materialDelays = allTasks.stream()
            .filter(t -> t.getRiskNotes() != null && t.getRiskNotes().contains("[materials]"))
            .count();
        
        if (materialDelays > 0) {
            alerts.add("📦 " + materialDelays + " tasks waiting for materials");
        }
        
        if (alerts.isEmpty()) {
            Label noAlerts = new Label("✓ No alerts at this time");
            noAlerts.setStyle("-fx-text-fill: green;");
            card.getChildren().add(noAlerts);
        } else {
            for (String alert : alerts) {
                Label alertLabel = new Label(alert);
                alertLabel.setStyle("-fx-font-size: 12px;");
                card.getChildren().add(alertLabel);
            }
        }
        
        return card;
    }
    
    private VBox createCard(String title) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        card.getChildren().add(titleLabel);
        
        return card;
    }
    
    private VBox createMetricCard(String title, String value, Color color) {
        VBox card = createCard(title);
        
        if (!value.isEmpty()) {
            Label valueLabel = new Label(value);
            valueLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
            valueLabel.setTextFill(color);
            card.getChildren().add(valueLabel);
        }
        
        return card;
    }
    
    private void addMetricDetail(VBox card, String label, String value) {
        HBox row = new HBox(10);
        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-size: 12px;");
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        row.getChildren().addAll(labelNode, valueNode);
        card.getChildren().add(row);
    }
    
    private void addMetricRow(VBox card, String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label labelNode = new Label(label + ":");
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(labelNode, spacer, valueNode);
        card.getChildren().add(row);
    }
    
    private double calculateOverallProgress() {
        if (allTasks.isEmpty()) return 0;
        
        double totalProgress = allTasks.stream()
            .mapToInt(Task::getProgressPercentage)
            .average()
            .orElse(0);
        
        return totalProgress;
    }
    
    private double calculateUtilizationForDate(LocalDate date) {
        long tasksOnDate = allTasks.stream()
            .filter(t -> t.getPlannedStart() != null && t.getPlannedEnd() != null &&
                        !date.isBefore(t.getPlannedStart()) && !date.isAfter(t.getPlannedEnd()))
            .count();
        
        return Math.min(1.0, tasksOnDate / (double) allResources.size());
    }
    
    public void show() {
        stage.show();
        stage.centerOnScreen();
        stage.toFront();
        stage.requestFocus();
        // Temporarily set always on top to ensure it appears above the Task List
        stage.setAlwaysOnTop(true);
        // Remove always on top after a short delay
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
            javafx.application.Platform.runLater(() -> stage.setAlwaysOnTop(false));
        });
    }
}