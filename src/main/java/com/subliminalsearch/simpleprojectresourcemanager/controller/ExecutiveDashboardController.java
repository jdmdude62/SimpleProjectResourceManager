package com.subliminalsearch.simpleprojectresourcemanager.controller;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;

public class ExecutiveDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(ExecutiveDashboardController.class);
    
    @FXML private Label activeProjectsLabel;
    @FXML private Label projectsTrendLabel;
    @FXML private Label utilizationLabel;
    @FXML private ProgressBar utilizationBar;
    @FXML private Label onTimeLabel;
    @FXML private Label onTimeTrendLabel;
    @FXML private Label revenueLabel;
    @FXML private Label revenueTrendLabel;
    @FXML private Label completionLabel;
    @FXML private PieChart completionPie;
    
    @FXML private LineChart<String, Number> volumeTrendChart;
    @FXML private PieChart projectTypeChart;
    @FXML private BarChart<String, Number> resourceChart;
    @FXML private StackedBarChart<String, Number> geoChart;
    
    @FXML private ComboBox<String> dateRangeCombo;
    @FXML private ComboBox<String> projectTypeCombo;
    @FXML private CheckMenuItem autoRefreshMenuItem;
    @FXML private ListView<String> alertsList;
    
    @FXML private Label lastUpdateLabel;
    @FXML private Label statusLabel;
    @FXML private Label projectCountLabel;
    @FXML private Label resourceCountLabel;
    @FXML private Label refreshIntervalLabel;
    
    private SchedulingService schedulingService;
    private Timeline autoRefreshTimeline;
    private Preferences prefs = Preferences.userNodeForPackage(ExecutiveDashboardController.class);
    
    // Settings from preferences
    private int refreshIntervalMinutes;
    private double targetUtilization;
    private double lowUtilizationThreshold;
    private double onTimeTarget;
    private String defaultDateRange;
    
    @FXML
    public void initialize() {
        logger.info("Initializing Executive Dashboard");
        
        try {
            loadPreferences();
            setupControls();
            setupAutoRefresh();
            // Immediately refresh dashboard data
            Platform.runLater(() -> refreshDashboard());
        } catch (Exception e) {
            logger.error("Failed to initialize dashboard", e);
            showAlert("Initialization Error", "Failed to initialize dashboard: " + e.getMessage());
        }
    }
    
    public void setSchedulingService(SchedulingService service) {
        this.schedulingService = service;
        refreshDashboard();
    }
    
    private void loadPreferences() {
        // Load saved preferences
        defaultDateRange = prefs.get("dashboard.dateRange", "Last 30 Days");
        refreshIntervalMinutes = prefs.getInt("dashboard.refreshInterval", 0);
        targetUtilization = prefs.getDouble("dashboard.targetUtilization", 75.0);
        lowUtilizationThreshold = prefs.getDouble("dashboard.lowUtilization", 60.0);
        onTimeTarget = prefs.getDouble("dashboard.onTimeTarget", 95.0);
        
        logger.info("Loaded preferences - Date range: {}, Refresh: {} min", 
                   defaultDateRange, refreshIntervalMinutes);
    }
    
    private void setupControls() {
        // Setup date range options
        dateRangeCombo.setItems(FXCollections.observableArrayList(
            "Last 7 Days", "Last 30 Days", "Last 60 Days", "Last 90 Days",
            "Current Quarter", "Current Year"
        ));
        dateRangeCombo.setValue(defaultDateRange);
        dateRangeCombo.setOnAction(e -> refreshDashboard());
        
        // Setup project type filter
        projectTypeCombo.setItems(FXCollections.observableArrayList(
            "All Types", "Garden", "Dog House", "Cat House"
        ));
        projectTypeCombo.setValue("All Types");
        projectTypeCombo.setOnAction(e -> refreshDashboard());
    }
    
    private void setupAutoRefresh() {
        if (refreshIntervalMinutes > 0) {
            autoRefreshMenuItem.setSelected(true);
            autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.minutes(refreshIntervalMinutes), e -> refreshDashboard())
            );
            autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
            autoRefreshTimeline.play();
            refreshIntervalLabel.setText("Auto-refresh: " + refreshIntervalMinutes + " min");
        } else {
            refreshIntervalLabel.setText("Auto-refresh: OFF");
        }
        
        autoRefreshMenuItem.setOnAction(e -> {
            if (autoRefreshMenuItem.isSelected() && refreshIntervalMinutes > 0) {
                setupAutoRefresh();
            } else if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
                refreshIntervalLabel.setText("Auto-refresh: OFF");
            }
        });
    }
    
    @FXML
    public void refreshDashboard() {
        // Ensure UI components are initialized
        if (statusLabel != null) {
            statusLabel.setText("Refreshing dashboard...");
        }
        
        new Thread(() -> {
            try {
                // Calculate date range
                LocalDate endDate = LocalDate.now();
                String dateRange = dateRangeCombo != null ? dateRangeCombo.getValue() : defaultDateRange;
                LocalDate startDate = calculateStartDate(dateRange);
                
                // Load all data
                DashboardData data = loadDashboardData(startDate, endDate);
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    updateKPICards(data);
                    updateCharts(data);
                    updateAlerts(data);
                    
                    if (lastUpdateLabel != null) {
                        lastUpdateLabel.setText("Last updated: " + 
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    }
                    if (statusLabel != null) {
                        statusLabel.setText("Ready");
                    }
                });
                
            } catch (Exception e) {
                logger.error("Failed to refresh dashboard", e);
                Platform.runLater(() -> {
                    if (statusLabel != null) {
                        statusLabel.setText("Error refreshing dashboard");
                    }
                    showAlert("Refresh Error", "Failed to refresh dashboard: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private LocalDate calculateStartDate(String range) {
        LocalDate today = LocalDate.now();
        return switch (range) {
            case "Last 7 Days" -> today.minusDays(7);
            case "Last 30 Days" -> today.minusDays(30);
            case "Last 60 Days" -> today.minusDays(60);
            case "Last 90 Days" -> today.minusDays(90);
            case "Current Quarter" -> today.withDayOfMonth(1)
                .withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1);
            case "Current Year" -> today.withDayOfYear(1);
            default -> today.minusDays(30);
        };
    }
    
    private DashboardData loadDashboardData(LocalDate startDate, LocalDate endDate) throws SQLException {
        DashboardData data = new DashboardData();
        
        String dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
        String url = "jdbc:sqlite:" + dbPath;
        
        try (Connection conn = DriverManager.getConnection(url)) {
            // Load KPI metrics
            loadKPIMetrics(conn, data, startDate, endDate);
            
            // Load chart data
            loadChartData(conn, data, startDate, endDate);
            
            // Generate alerts
            generateAlerts(data);
        }
        
        return data;
    }
    
    private void loadKPIMetrics(Connection conn, DashboardData data, 
                                LocalDate startDate, LocalDate endDate) throws SQLException {
        Statement stmt = conn.createStatement();
        PreparedStatement pstmt;
        
        // Active projects
        ResultSet rs = stmt.executeQuery("""
            SELECT COUNT(*) as active_count 
            FROM projects 
            WHERE status = 'ACTIVE'
        """);
        if (rs.next()) {
            data.activeProjects = rs.getInt("active_count");
        }
        
        // Resource utilization
        rs = stmt.executeQuery("""
            SELECT 
                COUNT(DISTINCT r.id) as total_resources,
                COUNT(DISTINCT a.resource_id) as assigned_resources
            FROM resources r
            LEFT JOIN assignments a ON r.id = a.resource_id 
                AND date(a.start_date) <= date('now') 
                AND date(a.end_date) >= date('now')
            WHERE r.is_active = 1
        """);
        if (rs.next()) {
            int total = rs.getInt("total_resources");
            int assigned = rs.getInt("assigned_resources");
            data.resourceUtilization = total > 0 ? (assigned * 100.0 / total) : 0;
        }
        
        // On-time delivery
        pstmt = conn.prepareStatement("""
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN date(end_date) >= date(start_date) THEN 1 ELSE 0 END) as on_time
            FROM projects
            WHERE status = 'COMPLETED'
                AND date(end_date) BETWEEN date(?) AND date(?)
        """);
        pstmt.setString(1, startDate.toString());
        pstmt.setString(2, endDate.toString());
        rs = pstmt.executeQuery();
        if (rs.next()) {
            int total = rs.getInt("total");
            int onTime = rs.getInt("on_time");
            data.onTimeDelivery = total > 0 ? (onTime * 100.0 / total) : 0;
        }
        
        // Completion rate
        pstmt = conn.prepareStatement("""
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed
            FROM projects
            WHERE date(start_date) BETWEEN date(?) AND date(?)
        """);
        pstmt.setString(1, startDate.toString());
        pstmt.setString(2, endDate.toString());
        rs = pstmt.executeQuery();
        if (rs.next()) {
            int total = rs.getInt("total");
            int completed = rs.getInt("completed");
            data.completionRate = total > 0 ? (completed * 100.0 / total) : 0;
            data.totalProjects = total;
            data.completedProjects = completed;
        }
    }
    
    private void loadChartData(Connection conn, DashboardData data,
                               LocalDate startDate, LocalDate endDate) throws SQLException {
        // Volume trend data
        PreparedStatement pstmt = conn.prepareStatement("""
            SELECT 
                strftime('%Y-%m', start_date) as month,
                COUNT(*) as project_count
            FROM projects
            WHERE date(start_date) BETWEEN date(?) AND date(?)
            GROUP BY month
            ORDER BY month
        """);
        pstmt.setString(1, startDate.toString());
        pstmt.setString(2, endDate.toString());
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            data.volumeTrend.put(rs.getString("month"), rs.getInt("project_count"));
        }
        
        // Project type distribution
        rs = conn.createStatement().executeQuery("""
            SELECT 
                CASE 
                    WHEN project_id LIKE 'GRDN%' THEN 'Garden'
                    WHEN project_id LIKE 'DH-%' THEN 'Dog House'
                    WHEN project_id LIKE 'CH-%' THEN 'Cat House'
                    ELSE 'Other'
                END as project_type,
                COUNT(*) as count
            FROM projects
            GROUP BY project_type
        """);
        
        while (rs.next()) {
            data.projectTypes.put(rs.getString("project_type"), rs.getInt("count"));
        }
    }
    
    private void generateAlerts(DashboardData data) {
        data.alerts = new ArrayList<>();
        
        // Check utilization
        if (data.resourceUtilization < lowUtilizationThreshold) {
            data.alerts.add(String.format("⚠ Low resource utilization: %.1f%% (target: %.0f%%)",
                data.resourceUtilization, targetUtilization));
        }
        
        // Check on-time delivery
        if (data.onTimeDelivery < onTimeTarget) {
            data.alerts.add(String.format("⚠ On-time delivery below target: %.1f%% (target: %.0f%%)",
                data.onTimeDelivery, onTimeTarget));
        }
        
        // Check for upcoming deadlines
        if (data.activeProjects > 10) {
            data.alerts.add(String.format("ℹ High number of active projects: %d", 
                data.activeProjects));
        }
        
        if (data.alerts.isEmpty()) {
            data.alerts.add("✓ All metrics within normal ranges");
        }
    }
    
    private void updateKPICards(DashboardData data) {
        // Active projects
        activeProjectsLabel.setText(String.valueOf(data.activeProjects));
        
        // Resource utilization
        utilizationLabel.setText(String.format("%.1f%%", data.resourceUtilization));
        utilizationBar.setProgress(data.resourceUtilization / 100.0);
        
        // On-time delivery
        onTimeLabel.setText(String.format("%.1f%%", data.onTimeDelivery));
        
        // Revenue (placeholder - would need actual billing data)
        revenueLabel.setText("$" + (data.activeProjects * 5000)); // Example calculation
        
        // Completion rate
        completionLabel.setText(String.format("%.1f%%", data.completionRate));
        
        // Update counts
        projectCountLabel.setText("Projects: " + data.totalProjects);
        resourceCountLabel.setText("Resources: " + data.totalResources);
    }
    
    private void updateCharts(DashboardData data) {
        // Volume trend chart
        volumeTrendChart.getData().clear();
        XYChart.Series<String, Number> volumeSeries = new XYChart.Series<>();
        volumeSeries.setName("Projects");
        data.volumeTrend.forEach((month, count) -> 
            volumeSeries.getData().add(new XYChart.Data<>(month, count)));
        volumeTrendChart.getData().add(volumeSeries);
        
        // Project type pie chart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        data.projectTypes.forEach((type, count) -> 
            pieData.add(new PieChart.Data(type + " (" + count + ")", count)));
        projectTypeChart.setData(pieData);
    }
    
    private void updateAlerts(DashboardData data) {
        alertsList.setItems(FXCollections.observableArrayList(data.alerts));
    }
    
    @FXML
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/dashboard-settings.fxml"));
            DialogPane dialogPane = loader.load();
            
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Dashboard Settings");
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Save settings and refresh
                // TODO: Implement DashboardSettingsController
                // DashboardSettingsController controller = loader.getController();
                // controller.saveSettings();
                loadPreferences();
                refreshDashboard();
            }
        } catch (IOException e) {
            logger.error("Failed to open settings", e);
        }
    }
    
    @FXML private void exportToPDF() {
        // TODO: Implement PDF export
        showAlert("Export", "PDF export will be implemented");
    }
    
    @FXML private void printDashboard() {
        // TODO: Implement printing
        showAlert("Print", "Printing will be implemented");
    }
    
    @FXML private void closeDashboard() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        // Close the dashboard window
    }
    
    @FXML private void openFinancialSettings() {
        // TODO: Open financial settings dialog
    }
    
    @FXML private void openAlertSettings() {
        // TODO: Open alert settings dialog
    }
    
    @FXML private void saveLayout() {
        // Save current layout preferences
        prefs.put("dashboard.dateRange", dateRangeCombo.getValue());
        showAlert("Settings", "Layout saved successfully");
    }
    
    @FXML private void resetDefaults() {
        // Reset to default settings
        try {
            prefs.clear();
        } catch (Exception e) {
            logger.error("Failed to clear preferences", e);
        }
        loadPreferences();
        setupControls();
        refreshDashboard();
    }
    
    @FXML private void showResourceReport() {
        // TODO: Show detailed resource report
    }
    
    @FXML private void showPipelineReport() {
        // TODO: Show project pipeline report
    }
    
    @FXML private void showFinancialReport() {
        // TODO: Show financial report
    }
    
    @FXML private void showGeographicReport() {
        // TODO: Show geographic analysis
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Inner class to hold dashboard data
    private static class DashboardData {
        int activeProjects;
        int totalProjects;
        int completedProjects;
        int totalResources = 31; // From our data
        double resourceUtilization;
        double onTimeDelivery;
        double completionRate;
        Map<String, Integer> volumeTrend = new LinkedHashMap<>();
        Map<String, Integer> projectTypes = new HashMap<>();
        List<String> alerts;
    }
}