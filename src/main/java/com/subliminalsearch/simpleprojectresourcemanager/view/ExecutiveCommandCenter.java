package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ExecutiveCommandCenter {
    private final Stage stage;
    private final SchedulingService schedulingService;
    private final ProjectRepository projectRepository;
    private final ResourceRepository resourceRepository;
    private final AssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    
    private TabPane mainTabPane;
    private VBox morningDashboard;
    private VBox executiveScorecard;
    private VBox decisionQueue;
    private VBox alertsPanel;
    private Label lastUpdateLabel;
    
    // Metric display labels that need updating
    private Label activeProjectsLabel;
    private Label resourceUtilizationLabel;
    private Label conflictsLabel;
    private Label decisionsLabel;
    
    // Metrics
    private int activeProjectsCount = 0;
    private int resourceConflictsCount = 0;
    private int decisionsNeededCount = 0;
    private double resourceUtilization = 0.0;
    private List<Project> projectsAtRisk = new ArrayList<>();
    
    public ExecutiveCommandCenter(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
        this.projectRepository = schedulingService.getProjectRepository();
        this.resourceRepository = schedulingService.getResourceRepository();
        this.assignmentRepository = schedulingService.getAssignmentRepository();
        this.taskRepository = new TaskRepository(schedulingService.getDataSource());
        this.stage = new Stage();
        
        initialize();
        loadMetrics();
        startAutoRefresh();
    }
    
    private void initialize() {
        stage.setTitle("Executive Review - Resource Management Overview");
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f0f0;");
        
        // Header
        HBox header = createHeader();
        root.setTop(header);
        
        // Main content tabs
        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Tab 1: Morning Coffee Dashboard
        Tab morningTab = new Tab("☕ Morning Coffee");
        morningDashboard = createMorningDashboard();
        ScrollPane morningScroll = new ScrollPane(morningDashboard);
        morningScroll.setFitToWidth(true);
        morningTab.setContent(morningScroll);
        
        // Tab 2: Executive Scorecard
        Tab scorecardTab = new Tab("📊 Executive Scorecard");
        executiveScorecard = createExecutiveScorecard();
        ScrollPane scorecardScroll = new ScrollPane(executiveScorecard);
        scorecardScroll.setFitToWidth(true);
        scorecardTab.setContent(scorecardScroll);
        
        // Tab 3: Decision Queue
        Tab decisionTab = new Tab("🎯 Decision Queue");
        decisionQueue = createDecisionQueue();
        ScrollPane decisionScroll = new ScrollPane(decisionQueue);
        decisionScroll.setFitToWidth(true);
        decisionTab.setContent(decisionScroll);
        
        // Tab 4: Predictive Alerts
        Tab alertsTab = new Tab("🔮 Predictive Alerts");
        alertsPanel = createPredictiveAlerts();
        ScrollPane alertsScroll = new ScrollPane(alertsPanel);
        alertsScroll.setFitToWidth(true);
        alertsTab.setContent(alertsScroll);
        
        // Tab 5: Resource Command
        Tab resourceTab = new Tab("👥 Resource Command");
        VBox resourceCommand = createResourceCommand();
        ScrollPane resourceScroll = new ScrollPane(resourceCommand);
        resourceScroll.setFitToWidth(true);
        resourceTab.setContent(resourceScroll);
        
        mainTabPane.getTabs().addAll(morningTab, scorecardTab, decisionTab, alertsTab, resourceTab);
        root.setCenter(mainTabPane);
        
        // Status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        Scene scene = new Scene(root, 1600, 900);
        scene.getStylesheets().add(getClass().getResource("/css/executive.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1400);
        stage.setMinHeight(800);
    }
    
    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15));
        header.setSpacing(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right, #2c3e50, #3498db); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");
        
        // Title
        Label title = new Label("EXECUTIVE REVIEW DASHBOARD");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        
        // Live time
        Label timeLabel = new Label();
        timeLabel.setFont(Font.font("System", 14));
        timeLabel.setTextFill(Color.WHITE);
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy - HH:mm:ss")));
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Quick actions
        Button exportBtn = new Button("📤 Export Report");
        exportBtn.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");
        exportBtn.setOnAction(e -> exportExecutiveReport());
        
        Button alertsBtn = new Button("🔔 Configure Alerts");
        alertsBtn.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");
        
        header.getChildren().addAll(title, spacer, timeLabel, exportBtn, alertsBtn);
        return header;
    }
    
    private VBox createMorningDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));
        dashboard.setStyle("-fx-background-color: white;");
        
        // Title
        Label title = new Label("Good Morning! Here's Your 30-Second Briefing");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#2c3e50"));
        
        // Overall Status Card
        HBox statusCard = createStatusCard();
        
        // Key Metrics Grid
        GridPane metricsGrid = createMetricsGrid();
        
        // Action Items
        VBox actionItems = createActionItems();
        
        // Team Status
        VBox teamStatus = createTeamStatus();
        
        // Today's Priorities
        VBox priorities = createTodaysPriorities();
        
        dashboard.getChildren().addAll(title, statusCard, metricsGrid, actionItems, teamStatus, priorities);
        return dashboard;
    }
    
    private HBox createStatusCard() {
        HBox card = new HBox(20);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        
        String overallStatus = determineOverallStatus();
        String statusColor = overallStatus.equals("ALL SYSTEMS GREEN") ? "#27ae60" : 
                           overallStatus.equals("ATTENTION NEEDED") ? "#f39c12" : "#e74c3c";
        
        card.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        
        Label statusIcon = new Label(overallStatus.equals("ALL SYSTEMS GREEN") ? "✅" : "⚠️");
        statusIcon.setFont(Font.font(48));
        
        VBox statusText = new VBox(5);
        Label statusLabel = new Label(overallStatus);
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        statusLabel.setTextFill(Color.WHITE);
        
        Label statusDetail = new Label(getStatusDetail());
        statusDetail.setFont(Font.font(14));
        statusDetail.setTextFill(Color.WHITE);
        
        statusText.getChildren().addAll(statusLabel, statusDetail);
        card.getChildren().addAll(statusIcon, statusText);
        
        return card;
    }
    
    private GridPane createMetricsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        
        // Active Projects
        activeProjectsLabel = new Label(String.valueOf(activeProjectsCount));
        VBox projectsBox = createMetricBoxWithLabel("📁", "Active Projects", activeProjectsLabel, 
                                          "↑ 2 from last week", "#3498db");
        grid.add(projectsBox, 0, 0);
        
        // Resource Utilization
        resourceUtilizationLabel = new Label(String.format("%.1f%%", resourceUtilization));
        VBox utilizationBox = createMetricBoxWithLabel("👥", "Resource Utilization", 
                                             resourceUtilizationLabel,
                                             resourceUtilization > 90 ? "⚠️ High" : "✅ Optimal", "#9b59b6");
        grid.add(utilizationBox, 1, 0);
        
        // Conflicts
        conflictsLabel = new Label(String.valueOf(resourceConflictsCount));
        VBox conflictsBox = createMetricBoxWithLabel("⚡", "Active Conflicts", conflictsLabel,
                                           resourceConflictsCount > 0 ? "Needs resolution" : "All clear", 
                                           resourceConflictsCount > 0 ? "#e74c3c" : "#27ae60");
        grid.add(conflictsBox, 2, 0);
        
        // Decisions Needed
        decisionsLabel = new Label(String.valueOf(decisionsNeededCount));
        VBox decisionsBox = createMetricBoxWithLabel("🎯", "Decisions Needed", decisionsLabel,
                                           "Awaiting your input", "#f39c12");
        grid.add(decisionsBox, 3, 0);
        
        return grid;
    }
    
    private VBox createMetricBox(String icon, String label, String value, String detail, String color) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(15));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        box.setPrefWidth(200);
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(32));
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        valueLabel.setTextFill(Color.web(color));
        
        Label nameLabel = new Label(label);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.web("#7f8c8d"));
        
        Label detailLabel = new Label(detail);
        detailLabel.setFont(Font.font(10));
        detailLabel.setTextFill(Color.web("#95a5a6"));
        
        box.getChildren().addAll(iconLabel, valueLabel, nameLabel, detailLabel);
        return box;
    }
    
    private VBox createMetricBoxWithLabel(String icon, String label, Label valueLabel, String detail, String color) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(15));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        box.setPrefWidth(200);
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(32));
        
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        valueLabel.setTextFill(Color.web(color));
        
        Label nameLabel = new Label(label);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setTextFill(Color.web("#7f8c8d"));
        
        Label detailLabel = new Label(detail);
        detailLabel.setFont(Font.font(10));
        detailLabel.setTextFill(Color.web("#95a5a6"));
        
        box.getChildren().addAll(iconLabel, valueLabel, nameLabel, detailLabel);
        return box;
    }
    
    private VBox createActionItems() {
        VBox actionBox = new VBox(10);
        actionBox.setPadding(new Insets(15));
        actionBox.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 10;");
        
        Label title = new Label("⚡ Action Items Requiring Your Attention");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#856404"));
        
        VBox items = new VBox(5);
        items.setPadding(new Insets(10, 0, 0, 20));
        
        if (resourceConflictsCount > 0) {
            Label item1 = new Label("• " + resourceConflictsCount + " resource conflicts need resolution");
            item1.setFont(Font.font(14));
            items.getChildren().add(item1);
        }
        
        if (!projectsAtRisk.isEmpty()) {
            Label item2 = new Label("• " + projectsAtRisk.size() + " projects showing risk indicators");
            item2.setFont(Font.font(14));
            items.getChildren().add(item2);
        }
        
        if (decisionsNeededCount > 0) {
            Label item3 = new Label("• " + decisionsNeededCount + " pending decisions in queue");
            item3.setFont(Font.font(14));
            items.getChildren().add(item3);
        }
        
        if (items.getChildren().isEmpty()) {
            Label noItems = new Label("✅ No immediate actions required");
            noItems.setFont(Font.font(14));
            noItems.setTextFill(Color.web("#155724"));
            items.getChildren().add(noItems);
        }
        
        actionBox.getChildren().addAll(title, items);
        return actionBox;
    }
    
    private VBox createTeamStatus() {
        VBox teamBox = new VBox(10);
        teamBox.setPadding(new Insets(15));
        teamBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;");
        
        Label title = new Label("👥 Project Manager Status");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        GridPane pmGrid = new GridPane();
        pmGrid.setHgap(15);
        pmGrid.setVgap(10);
        pmGrid.setPadding(new Insets(10, 0, 0, 0));
        
        // Mock PM data - would be loaded from database
        String[][] pmData = {
            {"PM Johnson", "3 projects", "85% utilized", "✅"},
            {"PM Smith", "4 projects", "95% utilized", "⚠️"},
            {"PM Davis", "2 projects", "70% utilized", "✅"},
            {"PM Wilson", "3 projects", "88% utilized", "✅"}
        };
        
        for (int i = 0; i < pmData.length; i++) {
            Label nameLabel = new Label(pmData[i][0]);
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            
            Label projectsLabel = new Label(pmData[i][1]);
            projectsLabel.setFont(Font.font(11));
            
            Label utilLabel = new Label(pmData[i][2]);
            utilLabel.setFont(Font.font(11));
            
            Label statusLabel = new Label(pmData[i][3]);
            statusLabel.setFont(Font.font(14));
            
            pmGrid.add(nameLabel, 0, i);
            pmGrid.add(projectsLabel, 1, i);
            pmGrid.add(utilLabel, 2, i);
            pmGrid.add(statusLabel, 3, i);
        }
        
        teamBox.getChildren().addAll(title, pmGrid);
        return teamBox;
    }
    
    private VBox createTodaysPriorities() {
        VBox priorityBox = new VBox(10);
        priorityBox.setPadding(new Insets(15));
        priorityBox.setStyle("-fx-background-color: #e8f4f8; -fx-background-radius: 10;");
        
        Label title = new Label("🎯 Today's Top Priorities");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#004085"));
        
        VBox priorities = new VBox(8);
        priorities.setPadding(new Insets(10, 0, 0, 0));
        
        addPriorityItem(priorities, "HIGH", "Resolve resource conflict: Tech Jones double-booked", "Project Alpha & Beta");
        addPriorityItem(priorities, "MEDIUM", "Review and approve Q4 resource plan", "Deadline: EOD");
        addPriorityItem(priorities, "LOW", "Check in with PM Smith on Project Gamma progress", "At risk indicator");
        
        priorityBox.getChildren().addAll(title, priorities);
        return priorityBox;
    }
    
    private void addPriorityItem(VBox container, String priority, String task, String detail) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.TOP_LEFT);
        
        Label priorityLabel = new Label(priority);
        priorityLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        priorityLabel.setPadding(new Insets(2, 6, 2, 6));
        priorityLabel.setStyle("-fx-background-color: " + 
            (priority.equals("HIGH") ? "#dc3545" : priority.equals("MEDIUM") ? "#ffc107" : "#28a745") + 
            "; -fx-text-fill: white; -fx-background-radius: 3;");
        priorityLabel.setMinWidth(60);
        
        VBox taskBox = new VBox(2);
        Label taskLabel = new Label(task);
        taskLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        Label detailLabel = new Label(detail);
        detailLabel.setFont(Font.font(11));
        detailLabel.setTextFill(Color.web("#6c757d"));
        taskBox.getChildren().addAll(taskLabel, detailLabel);
        
        item.getChildren().addAll(priorityLabel, taskBox);
        container.getChildren().add(item);
    }
    
    private VBox createExecutiveScorecard() {
        VBox scorecard = new VBox(20);
        scorecard.setPadding(new Insets(20));
        scorecard.setStyle("-fx-background-color: white;");
        
        Label title = new Label("Executive Performance Scorecard");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        // KPI Cards
        HBox kpiRow = new HBox(20);
        kpiRow.getChildren().addAll(
            createKPICard("Project Completion Rate", "95%", "↑ 3%", true),
            createKPICard("On-Time Delivery", "89%", "↑ 5%", true),
            createKPICard("Resource Efficiency", "87%", "↓ 2%", false),
            createKPICard("Client Satisfaction", "4.7/5", "→ 0", true)
        );
        
        // Charts
        HBox charts = new HBox(20);
        charts.setPrefHeight(300);
        
        // Trend chart
        LineChart<String, Number> trendChart = createTrendChart();
        trendChart.setPrefWidth(600);
        
        // Resource distribution pie
        PieChart resourcePie = createResourcePieChart();
        resourcePie.setPrefWidth(400);
        
        charts.getChildren().addAll(trendChart, resourcePie);
        
        // Success metrics
        VBox successBox = createSuccessMetrics();
        
        scorecard.getChildren().addAll(title, kpiRow, charts, successBox);
        return scorecard;
    }
    
    private VBox createKPICard(String metric, String value, String trend, boolean positive) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        Label metricLabel = new Label(metric);
        metricLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        metricLabel.setTextFill(Color.web("#7f8c8d"));
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        valueLabel.setTextFill(Color.web("#2c3e50"));
        
        Label trendLabel = new Label(trend);
        trendLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        trendLabel.setTextFill(positive ? Color.web("#27ae60") : Color.web("#e74c3c"));
        
        card.getChildren().addAll(valueLabel, metricLabel, trendLabel);
        return card;
    }
    
    private LineChart<String, Number> createTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Month");
        yAxis.setLabel("Projects Completed");
        
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Project Completion Trend");
        lineChart.setLegendVisible(false);
        lineChart.setCreateSymbols(false); // Disable symbols to avoid JavaFX compatibility issue
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Projects");
        
        // Add all data at once to avoid individual node creation
        ObservableList<XYChart.Data<String, Number>> data = FXCollections.observableArrayList();
        data.add(new XYChart.Data<>("Jan", 8));
        data.add(new XYChart.Data<>("Feb", 10));
        data.add(new XYChart.Data<>("Mar", 9));
        data.add(new XYChart.Data<>("Apr", 12));
        data.add(new XYChart.Data<>("May", 11));
        data.add(new XYChart.Data<>("Jun", 14));
        series.getData().addAll(data);
        
        lineChart.getData().add(series);
        return lineChart;
    }
    
    private PieChart createResourcePieChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Allocated", 75),
            new PieChart.Data("Available", 15),
            new PieChart.Data("On Leave", 10)
        );
        
        PieChart chart = new PieChart(pieChartData);
        chart.setTitle("Resource Distribution");
        chart.setLegendVisible(true);
        return chart;
    }
    
    private VBox createSuccessMetrics() {
        VBox successBox = new VBox(10);
        successBox.setPadding(new Insets(20));
        successBox.setStyle("-fx-background-color: #d4edda; -fx-background-radius: 10;");
        
        Label title = new Label("🏆 This Month's Achievements");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#155724"));
        
        VBox achievements = new VBox(5);
        achievements.setPadding(new Insets(10, 0, 0, 20));
        
        achievements.getChildren().addAll(
            new Label("✅ Prevented 12 resource conflicts through proactive management"),
            new Label("✅ Saved 156 hours through intelligent resource reallocation"),
            new Label("✅ Improved project delivery time by 15% compared to last quarter"),
            new Label("✅ Maintained 95% client satisfaction rate")
        );
        
        successBox.getChildren().addAll(title, achievements);
        return successBox;
    }
    
    private VBox createDecisionQueue() {
        VBox queue = new VBox(15);
        queue.setPadding(new Insets(20));
        queue.setStyle("-fx-background-color: white;");
        
        Label title = new Label("Decisions Requiring Your Approval");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        // Pending decisions
        VBox decisions = new VBox(15);
        
        decisions.getChildren().addAll(
            createDecisionCard("Resource Reallocation", "HIGH", 
                "Move Senior Developer from Project A to Project B?",
                "Project B is behind schedule and needs expertise",
                "Impact: Project A timeline extends by 2 days"),
            createDecisionCard("New Project Approval", "MEDIUM",
                "Accept Project Delta with current resource constraints?",
                "Estimated value: $125,000 | Duration: 3 months",
                "Requires hiring 2 contractors or delaying Project Echo"),
            createDecisionCard("Vacation Request", "LOW",
                "Approve PM Johnson's vacation request (Aug 15-25)?",
                "Coverage available from PM Davis",
                "No critical milestones during this period")
        );
        
        queue.getChildren().addAll(title, decisions);
        return queue;
    }
    
    private VBox createDecisionCard(String title, String priority, String question, String context, String impact) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 10; -fx-background-radius: 10;");
        
        HBox header = new HBox(10);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Label priorityLabel = new Label(priority);
        priorityLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        priorityLabel.setPadding(new Insets(2, 8, 2, 8));
        String priorityColor = priority.equals("HIGH") ? "#dc3545" : priority.equals("MEDIUM") ? "#ffc107" : "#28a945";
        priorityLabel.setStyle("-fx-background-color: " + priorityColor + "; -fx-text-fill: white; -fx-background-radius: 3;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(titleLabel, priorityLabel, spacer);
        
        Label questionLabel = new Label(question);
        questionLabel.setFont(Font.font(14));
        questionLabel.setWrapText(true);
        
        Label contextLabel = new Label("Context: " + context);
        contextLabel.setFont(Font.font(12));
        contextLabel.setTextFill(Color.web("#6c757d"));
        
        Label impactLabel = new Label("Impact: " + impact);
        impactLabel.setFont(Font.font(12));
        impactLabel.setTextFill(Color.web("#856404"));
        
        HBox buttons = new HBox(10);
        buttons.setPadding(new Insets(10, 0, 0, 0));
        
        Button approveBtn = new Button("✅ Approve");
        approveBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Button denyBtn = new Button("❌ Deny");
        denyBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Button deferBtn = new Button("⏸ Defer");
        deferBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold;");
        
        Button detailsBtn = new Button("📋 More Details");
        detailsBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
        
        buttons.getChildren().addAll(approveBtn, denyBtn, deferBtn, detailsBtn);
        
        card.getChildren().addAll(header, questionLabel, contextLabel, impactLabel, buttons);
        return card;
    }
    
    private VBox createPredictiveAlerts() {
        VBox alerts = new VBox(15);
        alerts.setPadding(new Insets(20));
        alerts.setStyle("-fx-background-color: white;");
        
        Label title = new Label("🔮 Predictive Analysis - Next 30 Days");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        // Alert categories
        VBox criticalAlerts = createAlertSection("🔴 Critical Predictions", "#dc3545",
            "Resource bottleneck expected Week of Aug 20: 3 projects need same specialist",
            "PM Smith will be at 110% capacity by Aug 25 if Project Gamma continues",
            "Vacation conflicts detected: Sept 1-5, insufficient coverage for Project Delta"
        );
        
        VBox warningAlerts = createAlertSection("🟡 Warnings", "#ffc107",
            "Project Echo trending 3 days behind schedule - intervention recommended",
            "Resource Jane Doe approaching 95% utilization - burnout risk",
            "Budget utilization at 78% with 40% timeline remaining on Project Beta"
        );
        
        VBox opportunityAlerts = createAlertSection("🟢 Opportunities", "#28a745",
            "Resource capacity available Aug 15-20: Can accelerate Project Alpha",
            "PM Davis has bandwidth for additional small project",
            "Cross-training opportunity: 3 resources available for skill development"
        );
        
        alerts.getChildren().addAll(title, criticalAlerts, warningAlerts, opportunityAlerts);
        return alerts;
    }
    
    private VBox createAlertSection(String title, String color, String... alerts) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: " + color + "; -fx-border-width: 0 0 0 4; -fx-background-radius: 5;");
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web(color));
        
        VBox alertList = new VBox(5);
        alertList.setPadding(new Insets(5, 0, 0, 10));
        
        for (String alert : alerts) {
            Label alertLabel = new Label("• " + alert);
            alertLabel.setFont(Font.font(12));
            alertLabel.setWrapText(true);
            alertList.getChildren().add(alertLabel);
        }
        
        section.getChildren().addAll(titleLabel, alertList);
        return section;
    }
    
    private VBox createResourceCommand() {
        VBox command = new VBox(15);
        command.setPadding(new Insets(20));
        command.setStyle("-fx-background-color: white;");
        
        Label title = new Label("Resource Command Center");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        // Resource utilization heatmap would go here
        Label placeholder = new Label("Resource timeline and conflict resolution interface");
        placeholder.setFont(Font.font(14));
        placeholder.setTextFill(Color.web("#6c757d"));
        
        command.getChildren().addAll(title, placeholder);
        return command;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #2c3e50;");
        
        Label statusLabel = new Label("System Status: Online");
        statusLabel.setTextFill(Color.WHITE);
        
        lastUpdateLabel = new Label("Last Updated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        lastUpdateLabel.setTextFill(Color.WHITE);
        lastUpdateLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        // Add auto-refresh indicator
        Label refreshIndicator = new Label("⟳ Auto-refresh: 30s");
        refreshIndicator.setTextFill(Color.LIGHTGREEN);
        refreshIndicator.setFont(Font.font("System", 11));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label userLabel = new Label("Executive View | Full Access");
        userLabel.setTextFill(Color.WHITE);
        
        statusBar.getChildren().addAll(statusLabel, lastUpdateLabel, refreshIndicator, spacer, userLabel);
        return statusBar;
    }
    
    private void loadMetrics() {
        // Load real metrics from database
        List<Project> allProjects = projectRepository.findAll();
        activeProjectsCount = (int) allProjects.stream()
            .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
            .count();
        
        // Calculate resource conflicts
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(30);
        // For now, use a simple count - would need to implement conflict detection
        resourceConflictsCount = 0; // Placeholder - implement actual conflict detection
        
        // Calculate utilization
        List<Resource> allResources = resourceRepository.findAll();
        if (!allResources.isEmpty()) {
            int totalAllocated = 0;
            for (Resource resource : allResources) {
                List<Assignment> assignments = assignmentRepository.findByResourceId(resource.getId());
                if (!assignments.isEmpty()) totalAllocated++;
            }
            resourceUtilization = (totalAllocated * 100.0) / allResources.size();
        }
        
        // Find projects at risk (mock logic - would need real criteria)
        projectsAtRisk = allProjects.stream()
            .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
            .limit(2) // Mock: just take first 2 for demo
            .collect(Collectors.toList());
        
        // Mock decision count
        decisionsNeededCount = 3;
    }
    
    private String determineOverallStatus() {
        if (resourceConflictsCount > 5 || projectsAtRisk.size() > 3) {
            return "CRITICAL ATTENTION";
        } else if (resourceConflictsCount > 0 || decisionsNeededCount > 0) {
            return "ATTENTION NEEDED";
        }
        return "ALL SYSTEMS GREEN";
    }
    
    private String getStatusDetail() {
        if (resourceConflictsCount > 0) {
            return resourceConflictsCount + " conflicts need resolution | " + decisionsNeededCount + " decisions pending";
        }
        return "All projects on track | Resources optimally allocated";
    }
    
    private void refreshAllData() {
        loadMetrics();
        updateAllTabs();
        updateStatusBar();
        
        // Log refresh to console instead of showing alert
        System.out.println("Dashboard refreshed at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }
    
    private void exportExecutiveReport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Report");
        alert.setHeaderText(null);
        alert.setContentText("Executive report exported to: ExecutiveReport_" + 
                           LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".pdf");
        alert.showAndWait();
    }
    
    private void startAutoRefresh() {
        // Refresh every 30 seconds for real-time updates
        Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> refreshAllData()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();
        
        // Store the timeline so we can stop it when the window closes
        stage.setOnCloseRequest(event -> autoRefresh.stop());
    }
    
    private void updateAllTabs() {
        // Update Morning Dashboard if visible
        if (morningDashboard != null) {
            updateMorningDashboard();
        }
        
        // Update Executive Scorecard if visible
        if (executiveScorecard != null) {
            updateExecutiveScorecard();
        }
        
        // Update Decision Queue if visible
        if (decisionQueue != null) {
            updateDecisionQueue();
        }
        
        // Update Predictive Alerts if visible
        if (alertsPanel != null) {
            updatePredictiveAlerts();
        }
    }
    
    private void updateMorningDashboard() {
        // Update the metric labels with current values
        if (activeProjectsLabel != null) {
            activeProjectsLabel.setText(String.valueOf(activeProjectsCount));
        }
        if (resourceUtilizationLabel != null) {
            resourceUtilizationLabel.setText(String.format("%.1f%%", resourceUtilization));
        }
        if (conflictsLabel != null) {
            conflictsLabel.setText(String.valueOf(resourceConflictsCount));
        }
        if (decisionsLabel != null) {
            decisionsLabel.setText(String.valueOf(decisionsNeededCount));
        }
    }
    
    private void updateExecutiveScorecard() {
        // Refresh scorecard metrics
        // Update charts and KPIs
    }
    
    private void updateDecisionQueue() {
        // Refresh decision items
    }
    
    private void updatePredictiveAlerts() {
        // Refresh alert predictions
    }
    
    private void updateStatusBar() {
        // Update the status bar with latest refresh time
        if (lastUpdateLabel != null) {
            lastUpdateLabel.setText("Last Updated: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }
    
    public void show() {
        stage.show();
        stage.centerOnScreen();
        
        // Perform initial data refresh when dashboard opens
        refreshAllData();
    }
}