package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.FinancialService;
import com.subliminalsearch.simpleprojectresourcemanager.util.FinancialCalculator;
import com.subliminalsearch.simpleprojectresourcemanager.util.FinancialCalculator.FinancialSummary;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Financial Timeline View - Provides visual representation of financial data over time
 * Uses centralized FinancialCalculator for all calculations to ensure accuracy
 */
public class FinancialTimelineView extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(FinancialTimelineView.class);
    
    private final Project project;
    private final FinancialService financialService;
    
    private LineChart<String, Number> timelineChart;
    private TableView<FinancialMetric> metricsTable;
    private Label summaryLabel;
    private Label auditLabel;
    private ComboBox<String> viewModeCombo;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    
    private List<ActualCost> actualCosts;
    private List<PurchaseOrder> purchaseOrders;
    private List<ChangeOrder> changeOrders;
    private FinancialSummary currentSummary;
    
    public FinancialTimelineView(Project project, FinancialService financialService) {
        this.project = project;
        this.financialService = financialService;
        
        setTitle("Financial Timeline - " + project.getProjectId() + " " + project.getDescription());
        
        loadFinancialData();
        initializeUI();
        updateTimeline();
    }
    
    private void loadFinancialData() {
        try {
            actualCosts = financialService.getActualCostsForProject(project.getId());
            purchaseOrders = financialService.getPurchaseOrdersForProject(project.getId());
            changeOrders = financialService.getChangeOrdersForProject(project.getId());
            
            currentSummary = FinancialCalculator.calculateProjectFinancials(
                project, actualCosts, purchaseOrders, changeOrders);
                
            logger.info("Loaded financial data for project {}: {} costs, {} POs, {} change orders",
                project.getProjectId(), actualCosts.size(), purchaseOrders.size(), changeOrders.size());
        } catch (Exception e) {
            logger.error("Failed to load financial data", e);
            actualCosts = new ArrayList<>();
            purchaseOrders = new ArrayList<>();
            changeOrders = new ArrayList<>();
        }
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Header with controls
        HBox header = createHeader();
        
        // Main content split
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.setDividerPositions(0.6);
        
        // Timeline chart
        VBox chartBox = new VBox(5);
        chartBox.getChildren().addAll(
            new Label("Financial Timeline"),
            createTimelineChart()
        );
        
        // Bottom section with metrics and summary
        HBox bottomSection = new HBox(10);
        
        // Metrics table
        VBox metricsBox = new VBox(5);
        metricsBox.getChildren().addAll(
            new Label("Key Financial Metrics"),
            createMetricsTable()
        );
        HBox.setHgrow(metricsBox, Priority.ALWAYS);
        
        // Summary panel
        VBox summaryBox = createSummaryPanel();
        
        bottomSection.getChildren().addAll(metricsBox, summaryBox);
        
        splitPane.getItems().addAll(chartBox, bottomSection);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        
        // Audit trail footer
        auditLabel = new Label();
        auditLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        
        root.getChildren().addAll(header, splitPane, auditLabel);
        
        Scene scene = new Scene(root, 1200, 700);
        setScene(scene);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        viewModeCombo = new ComboBox<>();
        viewModeCombo.getItems().addAll(
            "Daily Costs",
            "Cumulative Costs",
            "Budget vs Actual",
            "Burn Rate Analysis",
            "Cost Categories",
            "PO Timeline"
        );
        viewModeCombo.setValue("Cumulative Costs");
        viewModeCombo.setOnAction(e -> updateTimeline());
        
        startDatePicker = new DatePicker(project.getStartDate());
        endDatePicker = new DatePicker(project.getEndDate());
        
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> {
            loadFinancialData();
            updateTimeline();
        });
        
        Button exportBtn = new Button("Export Data");
        exportBtn.setOnAction(e -> exportTimelineData());
        
        Button validateBtn = new Button("Validate");
        validateBtn.setStyle("-fx-background-color: #ffcc00;");
        validateBtn.setOnAction(e -> validateFinancialData());
        
        header.getChildren().addAll(
            new Label("View:"), viewModeCombo,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            new Label("From:"), startDatePicker,
            new Label("To:"), endDatePicker,
            refreshBtn,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            exportBtn,
            validateBtn
        );
        
        return header;
    }
    
    private LineChart<String, Number> createTimelineChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount ($)");
        yAxis.setForceZeroInRange(false);
        
        timelineChart = new LineChart<>(xAxis, yAxis);
        timelineChart.setTitle("Financial Timeline");
        timelineChart.setCreateSymbols(false);
        timelineChart.setAnimated(false);
        timelineChart.setPrefHeight(400);
        
        return timelineChart;
    }
    
    private TableView<FinancialMetric> createMetricsTable() {
        metricsTable = new TableView<>();
        
        TableColumn<FinancialMetric, String> metricCol = new TableColumn<>("Metric");
        metricCol.setCellValueFactory(new PropertyValueFactory<>("metric"));
        metricCol.setPrefWidth(200);
        
        TableColumn<FinancialMetric, Double> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setPrefWidth(150);
        valueCol.setCellFactory(tc -> new TableCell<FinancialMetric, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.2f", value));
                }
            }
        });
        
        TableColumn<FinancialMetric, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(tc -> new TableCell<FinancialMetric, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.contains("OVER") || status.contains("HIGH")) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (status.contains("WARNING")) {
                        setStyle("-fx-text-fill: orange;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });
        
        metricsTable.getColumns().addAll(metricCol, valueCol, statusCol);
        metricsTable.setPrefHeight(200);
        
        return metricsTable;
    }
    
    private VBox createSummaryPanel() {
        VBox summaryBox = new VBox(5);
        summaryBox.setPadding(new Insets(10));
        summaryBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc;");
        summaryBox.setPrefWidth(350);
        
        Label titleLabel = new Label("Financial Summary");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        summaryLabel = new Label();
        summaryLabel.setWrapText(true);
        
        summaryBox.getChildren().addAll(titleLabel, summaryLabel);
        
        return summaryBox;
    }
    
    private void updateTimeline() {
        String viewMode = viewModeCombo.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        timelineChart.getData().clear();
        
        switch (viewMode) {
            case "Daily Costs":
                showDailyCosts(startDate, endDate);
                break;
            case "Cumulative Costs":
                showCumulativeCosts(startDate, endDate);
                break;
            case "Budget vs Actual":
                showBudgetVsActual(startDate, endDate);
                break;
            case "Burn Rate Analysis":
                showBurnRateAnalysis(startDate, endDate);
                break;
            case "Cost Categories":
                showCostCategories(startDate, endDate);
                break;
            case "PO Timeline":
                showPOTimeline(startDate, endDate);
                break;
        }
        
        updateMetrics();
        updateSummary();
        updateAuditTrail();
    }
    
    private void showCumulativeCosts(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Double> dailyCosts = FinancialCalculator.calculateCostByPeriod(
            actualCosts, startDate, endDate);
        Map<LocalDate, Double> cumulativeCosts = FinancialCalculator.calculateCumulativeCost(dailyCosts);
        
        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Cumulative Actual Costs");
        
        XYChart.Series<String, Number> budgetSeries = new XYChart.Series<>();
        budgetSeries.setName("Budget Line");
        
        XYChart.Series<String, Number> projectedSeries = new XYChart.Series<>();
        projectedSeries.setName("Projected Total");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        
        // Calculate budget line (linear from start to end)
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        double dailyBudget = currentSummary.revisedBudget / totalDays;
        double runningBudget = 0;
        
        for (Map.Entry<LocalDate, Double> entry : cumulativeCosts.entrySet()) {
            String dateStr = entry.getKey().format(formatter);
            actualSeries.getData().add(new XYChart.Data<>(dateStr, entry.getValue()));
            
            runningBudget += dailyBudget;
            budgetSeries.getData().add(new XYChart.Data<>(dateStr, runningBudget));
        }
        
        // Add projected line based on burn rate
        double burnRate = FinancialCalculator.calculateBurnRate(project, actualCosts);
        if (burnRate > 0 && !cumulativeCosts.isEmpty()) {
            LocalDate lastDate = cumulativeCosts.keySet().stream().max(LocalDate::compareTo).orElse(LocalDate.now());
            double lastValue = cumulativeCosts.get(lastDate);
            
            LocalDate projectionDate = lastDate.plusDays(30);
            if (projectionDate.isAfter(endDate)) {
                projectionDate = endDate;
            }
            
            long projectionDays = ChronoUnit.DAYS.between(lastDate, projectionDate);
            double projectedValue = lastValue + (burnRate * projectionDays);
            
            projectedSeries.getData().add(new XYChart.Data<>(lastDate.format(formatter), lastValue));
            projectedSeries.getData().add(new XYChart.Data<>(projectionDate.format(formatter), projectedValue));
        }
        
        timelineChart.getData().addAll(actualSeries, budgetSeries, projectedSeries);
    }
    
    private void showDailyCosts(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Double> dailyCosts = FinancialCalculator.calculateCostByPeriod(
            actualCosts, startDate, endDate);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Costs");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        
        dailyCosts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                if (entry.getValue() > 0) {
                    series.getData().add(new XYChart.Data<>(
                        entry.getKey().format(formatter), 
                        entry.getValue()
                    ));
                }
            });
        
        timelineChart.getData().add(series);
    }
    
    private void showBudgetVsActual(LocalDate startDate, LocalDate endDate) {
        // Implementation for budget vs actual comparison
        showCumulativeCosts(startDate, endDate);
    }
    
    private void showBurnRateAnalysis(LocalDate startDate, LocalDate endDate) {
        double burnRate = FinancialCalculator.calculateBurnRate(project, actualCosts);
        LocalDate depletionDate = FinancialCalculator.projectBudgetDepletion(
            project, burnRate, currentSummary.totalActualCost);
        
        XYChart.Series<String, Number> burnSeries = new XYChart.Series<>();
        burnSeries.setName("Daily Burn Rate");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        
        // Show constant burn rate line
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            burnSeries.getData().add(new XYChart.Data<>(
                current.format(formatter), burnRate
            ));
            current = current.plusDays(7); // Weekly points
        }
        
        timelineChart.getData().add(burnSeries);
        
        // Update title with depletion date
        if (depletionDate != null) {
            timelineChart.setTitle(String.format(
                "Burn Rate Analysis - Budget Depletion: %s", 
                depletionDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            ));
        }
    }
    
    private void showCostCategories(LocalDate startDate, LocalDate endDate) {
        // Group costs by category
        Map<String, XYChart.Series<String, Number>> categorySeries = new HashMap<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        
        for (ActualCost cost : actualCosts) {
            if (cost.getCostDate() != null && 
                !cost.getCostDate().isBefore(startDate) && 
                !cost.getCostDate().isAfter(endDate) &&
                cost.getStatus() != ActualCost.CostStatus.DISPUTED) {
                
                String category = cost.getCategory() != null ? 
                    cost.getCategory().toString() : "OTHER";
                
                categorySeries.computeIfAbsent(category, k -> {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName(k);
                    return series;
                }).getData().add(new XYChart.Data<>(
                    cost.getCostDate().format(formatter),
                    cost.getAmount()
                ));
            }
        }
        
        timelineChart.getData().addAll(categorySeries.values());
    }
    
    private void showPOTimeline(LocalDate startDate, LocalDate endDate) {
        XYChart.Series<String, Number> poSeries = new XYChart.Series<>();
        poSeries.setName("Purchase Orders");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        
        purchaseOrders.stream()
            .filter(po -> po.getOrderDate() != null &&
                         !po.getOrderDate().isBefore(startDate) &&
                         !po.getOrderDate().isAfter(endDate))
            .sorted(Comparator.comparing(PurchaseOrder::getOrderDate))
            .forEach(po -> {
                poSeries.getData().add(new XYChart.Data<>(
                    po.getOrderDate().format(formatter),
                    po.getAmount()
                ));
            });
        
        timelineChart.getData().add(poSeries);
    }
    
    private void updateMetrics() {
        ObservableList<FinancialMetric> metrics = FXCollections.observableArrayList();
        
        metrics.add(new FinancialMetric("Total Budget", 
            currentSummary.totalBudget, "BASELINE"));
        metrics.add(new FinancialMetric("Revised Budget", 
            currentSummary.revisedBudget, "WITH CHANGES"));
        metrics.add(new FinancialMetric("Total Actual Cost", 
            currentSummary.totalActualCost, getStatus(currentSummary.totalActualCost, currentSummary.revisedBudget)));
        metrics.add(new FinancialMetric("Total Committed", 
            currentSummary.totalCommitted, getStatus(currentSummary.totalCommitted, currentSummary.revisedBudget)));
        metrics.add(new FinancialMetric("Projected Total", 
            currentSummary.projectedTotal, getStatus(currentSummary.projectedTotal, currentSummary.revisedBudget)));
        metrics.add(new FinancialMetric("Budget Variance", 
            currentSummary.budgetVariance, currentSummary.budgetVariance < 0 ? "OVER BUDGET" : "OK"));
        metrics.add(new FinancialMetric("Utilization %", 
            currentSummary.budgetUtilization, currentSummary.getHealthStatus()));
        metrics.add(new FinancialMetric("Cost Performance Index", 
            currentSummary.costPerformanceIndex, currentSummary.costPerformanceIndex < 1 ? "WARNING" : "GOOD"));
        
        double burnRate = FinancialCalculator.calculateBurnRate(project, actualCosts);
        metrics.add(new FinancialMetric("Daily Burn Rate", burnRate, "PER DAY"));
        
        metricsTable.setItems(metrics);
    }
    
    private String getStatus(double actual, double budget) {
        double ratio = actual / budget;
        if (ratio > 1.0) return "OVER BUDGET";
        if (ratio > 0.9) return "HIGH RISK";
        if (ratio > 0.75) return "ON TRACK";
        return "HEALTHY";
    }
    
    private void updateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Project: %s\n", project.getProjectId()));
        summary.append(String.format("Status: %s\n\n", currentSummary.getHealthStatus()));
        
        summary.append("Budget Summary:\n");
        summary.append(String.format("  Original: $%,.2f\n", currentSummary.totalBudget));
        summary.append(String.format("  Changes: $%,.2f\n", currentSummary.totalChangeOrderImpact));
        summary.append(String.format("  Revised: $%,.2f\n\n", currentSummary.revisedBudget));
        
        summary.append("Cost Summary:\n");
        summary.append(String.format("  Actual: $%,.2f\n", currentSummary.totalActualCost));
        summary.append(String.format("  Committed POs: $%,.2f\n", currentSummary.committedPOAmount));
        summary.append(String.format("  Pending: $%,.2f\n", currentSummary.pendingPOAmount));
        summary.append(String.format("  Projected: $%,.2f\n\n", currentSummary.projectedTotal));
        
        summary.append(String.format("Variance: $%,.2f (%.1f%%)\n", 
            currentSummary.budgetVariance, 
            currentSummary.budgetUtilization));
        
        if (currentSummary.isOverBudget()) {
            summary.append("\n⚠️ PROJECT IS OVER BUDGET");
        } else if (currentSummary.isHighRisk()) {
            summary.append("\n⚠️ HIGH RISK - Monitor closely");
        }
        
        summaryLabel.setText(summary.toString());
    }
    
    private void updateAuditTrail() {
        auditLabel.setText(String.format(
            "Calculation performed at %s using %s | %d records processed | CPI: %.2f",
            currentSummary.calculatedAt.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            currentSummary.calculationMethod,
            currentSummary.recordCount,
            currentSummary.costPerformanceIndex
        ));
    }
    
    private void validateFinancialData() {
        List<String> warnings = FinancialCalculator.validateFinancialData(
            project, actualCosts, purchaseOrders);
        
        if (warnings.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Validation Results");
            alert.setHeaderText("Financial Data Validation");
            alert.setContentText("✓ All financial data passed validation checks");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Results");
            alert.setHeaderText("Financial Data Warnings");
            
            TextArea textArea = new TextArea();
            textArea.setText(String.join("\n", warnings));
            textArea.setEditable(false);
            textArea.setWrapText(true);
            
            alert.getDialogPane().setContent(textArea);
            alert.getDialogPane().setPrefSize(600, 400);
            alert.showAndWait();
        }
    }
    
    private void exportTimelineData() {
        // Export functionality would go here
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export");
        alert.setHeaderText("Export Financial Timeline Data");
        alert.setContentText("Timeline data export will be implemented in Phase 3");
        alert.showAndWait();
    }
    
    // Helper class for metrics table
    public static class FinancialMetric {
        private final SimpleStringProperty metric;
        private final SimpleDoubleProperty value;
        private final SimpleStringProperty status;
        
        public FinancialMetric(String metric, double value, String status) {
            this.metric = new SimpleStringProperty(metric);
            this.value = new SimpleDoubleProperty(value);
            this.status = new SimpleStringProperty(status);
        }
        
        public String getMetric() { return metric.get(); }
        public double getValue() { return value.get(); }
        public String getStatus() { return status.get(); }
    }
}