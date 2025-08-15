package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportCenterView extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(ReportCenterView.class);
    
    private final SchedulingService schedulingService;
    private final TabPane reportTabs;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    
    // Report services
    private ClientReportService clientReportService;
    private ResourceUtilizationReportService resourceReportService;
    private ProjectPipelineReportService pipelineReportService;
    private RevenueReportService revenueReportService;
    private GeographicReportService geographicReportService;
    private CompletionAnalyticsService completionAnalyticsService;
    
    // Current report data
    private File currentReportFile;
    private ReportType currentReportType;
    
    public enum ReportType {
        CLIENT_PROJECT("Client Project Report"),
        RESOURCE_UTILIZATION("Resource Utilization Report"),
        PROJECT_PIPELINE("Project Pipeline Report"),
        REVENUE_BUDGET("Revenue & Budget Report"),
        GEOGRAPHIC_DISTRIBUTION("Geographic Distribution Report"),
        COMPLETION_ANALYTICS("Completion Analytics Report");
        
        private final String displayName;
        
        ReportType(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    private Project preloadProject = null;
    
    public ReportCenterView(SchedulingService schedulingService) {
        this(schedulingService, null);
    }
    
    public ReportCenterView(SchedulingService schedulingService, Project projectToPreload) {
        this.schedulingService = schedulingService;
        this.preloadProject = projectToPreload;
        
        // Initialize date pickers before creating toolbar
        this.startDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
        this.endDatePicker = new DatePicker(LocalDate.now());
        
        // Initialize report services
        this.clientReportService = new ClientReportService();
        this.resourceReportService = new ResourceUtilizationReportService(schedulingService);
        this.pipelineReportService = new ProjectPipelineReportService(schedulingService);
        this.revenueReportService = new RevenueReportService(schedulingService);
        this.geographicReportService = new GeographicReportService(schedulingService);
        this.completionAnalyticsService = new CompletionAnalyticsService(schedulingService);
        
        setTitle("Report Center - Preview and Send Reports");
        initModality(Modality.APPLICATION_MODAL);
        
        // Create main layout
        BorderPane root = new BorderPane();
        
        // Create toolbar
        ToolBar toolBar = createToolBar();
        root.setTop(toolBar);
        
        // Create report tabs
        reportTabs = new TabPane();
        reportTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        reportTabs.setPrefSize(1600, 800);  // Set preferred size for tab pane
        setupReportTabs();
        root.setCenter(reportTabs);
        
        // Create status bar
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);
        
        statusLabel = new Label("Select a report type to preview");
        
        statusBar.getChildren().addAll(statusLabel, progressBar);
        root.setBottom(statusBar);
        
        Scene scene = new Scene(root, 1600, 900);
        setScene(scene);
        
        // Set stage size constraints
        setMinWidth(1400);
        setMinHeight(800);
        setWidth(1600);
        setHeight(900);
        
        // Center on screen
        centerOnScreen();
        
        // If a project was provided, preload its previews
        if (preloadProject != null) {
            Platform.runLater(() -> preloadProjectReports(preloadProject));
        }
    }
    
    public void selectReport(String reportName) {
        // Map report names to tabs
        switch (reportName) {
            case "Revenue/Budget Report":
                reportTabs.getSelectionModel().select(3); // Revenue tab
                break;
            case "Resource Utilization Report":
                reportTabs.getSelectionModel().select(1); // Resource tab
                break;
            case "Project Status Report":
                reportTabs.getSelectionModel().select(0); // Client Projects tab
                break;
            case "Workload Report":
                reportTabs.getSelectionModel().select(1); // Resource tab (workload is part of resource utilization)
                break;
            default:
                logger.warn("Unknown report type: " + reportName);
        }
    }
    
    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();
        
        // Date range controls
        Button refreshButton = new Button("Refresh All Previews");
        refreshButton.setOnAction(e -> {
            // Refresh the current tab's preview
            Tab selectedTab = reportTabs.getSelectionModel().getSelectedItem();
            if (selectedTab != null) {
                refreshCurrentTabPreview(selectedTab);
            }
        });
        
        Separator separator = new Separator();
        
        Button saveButton = new Button("Save as PDF");
        saveButton.setOnAction(e -> saveCurrentReport());
        
        Button emailButton = new Button("Email Report");
        emailButton.setOnAction(e -> emailCurrentReport());
        
        Button printButton = new Button("Print");
        printButton.setOnAction(e -> printCurrentReport());
        
        toolBar.getItems().addAll(
            new Label("Date Range:"),
            new Label("From:"), startDatePicker,
            new Label("To:"), endDatePicker,
            refreshButton,
            separator,
            saveButton, emailButton, printButton
        );
        
        return toolBar;
    }
    
    private void setupReportTabs() {
        // Clear existing tabs before adding new ones
        reportTabs.getTabs().clear();
        
        // Client Project Reports Tab
        Tab clientTab = new Tab("Client Projects");
        clientTab.setContent(createClientReportView());
        
        // Resource Utilization Tab
        Tab resourceTab = new Tab("Resource Utilization");
        resourceTab.setContent(createResourceUtilizationView());
        
        // Project Pipeline Tab
        Tab pipelineTab = new Tab("Project Pipeline");
        pipelineTab.setContent(createProjectPipelineView());
        
        // Revenue & Budget Tab
        Tab revenueTab = new Tab("Revenue & Budget");
        revenueTab.setContent(createRevenueView());
        
        // Geographic Distribution Tab
        Tab geographicTab = new Tab("Geographic");
        geographicTab.setContent(createGeographicView());
        
        // Completion Analytics Tab
        Tab analyticsTab = new Tab("Analytics");
        analyticsTab.setContent(createAnalyticsView());
        
        reportTabs.getTabs().addAll(
            clientTab, resourceTab, pipelineTab, 
            revenueTab, geographicTab, analyticsTab
        );
    }
    
    private ScrollPane createClientReportView() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        
        // Create filtered project list
        List<Project> allProjects = new ArrayList<>(schedulingService.getAllProjects());
        ObservableList<Project> filteredProjects = FXCollections.observableArrayList(allProjects);
        
        // Project selector
        ComboBox<Project> projectCombo = new ComboBox<>(filteredProjects);
        projectCombo.setPromptText("Select a project to preview report");
        projectCombo.setPrefWidth(400);
        
        // Create filter controls
        DatePicker filterStartDatePicker = new DatePicker();
        filterStartDatePicker.setPromptText("Estimated start date");
        filterStartDatePicker.setPrefWidth(150);
        
        Spinner<Integer> filterDateRangeSpinner = new Spinner<>(0, 365, 15);
        filterDateRangeSpinner.setEditable(true);
        filterDateRangeSpinner.setPrefWidth(80);
        filterDateRangeSpinner.setTooltip(new Tooltip("±days from start date"));
        
        TextField filterProjectIdField = new TextField();
        filterProjectIdField.setPromptText("Project ID...");
        filterProjectIdField.setPrefWidth(150);
        
        TextField filterDescriptionField = new TextField();
        filterDescriptionField.setPromptText("Description...");
        filterDescriptionField.setPrefWidth(200);
        
        Button clearFiltersButton = new Button("Clear");
        
        Label projectCountLabel = new Label("(" + allProjects.size() + " projects)");
        projectCountLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        
        // Filter function
        Runnable applyFilters = () -> {
            List<Project> filtered = new ArrayList<>(allProjects);
            
            // Filter by estimated start date range
            if (filterStartDatePicker.getValue() != null) {
                LocalDate centerDate = filterStartDatePicker.getValue();
                int rangeDays = filterDateRangeSpinner.getValue();
                LocalDate startRange = centerDate.minusDays(rangeDays);
                LocalDate endRange = centerDate.plusDays(rangeDays);
                
                filtered = filtered.stream()
                    .filter(p -> !(p.getEndDate().isBefore(startRange) || p.getStartDate().isAfter(endRange)))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Filter by project ID (fuzzy search)
            String projectIdFilter = filterProjectIdField.getText().trim().toLowerCase();
            if (!projectIdFilter.isEmpty()) {
                filtered = filtered.stream()
                    .filter(p -> p.getProjectId().toLowerCase().contains(projectIdFilter))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Filter by description (fuzzy search)
            String descriptionFilter = filterDescriptionField.getText().trim().toLowerCase();
            if (!descriptionFilter.isEmpty()) {
                filtered = filtered.stream()
                    .filter(p -> p.getDescription().toLowerCase().contains(descriptionFilter))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Also apply date range filter from the main date pickers
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate != null && endDate != null) {
                filtered = filtered.stream()
                    .filter(p -> !p.getEndDate().isBefore(startDate) && !p.getStartDate().isAfter(endDate))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Update the filtered list
            filteredProjects.clear();
            filteredProjects.addAll(filtered);
            
            // Update count label
            projectCountLabel.setText("(" + filtered.size() + " projects)");
            if (filtered.size() < allProjects.size()) {
                projectCountLabel.setStyle("-fx-text-fill: #1976d2; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                projectCountLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            }
        };
        
        // Set up filter listeners
        filterStartDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());
        filterDateRangeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());
        filterProjectIdField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());
        filterDescriptionField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters.run());
        
        clearFiltersButton.setOnAction(e -> {
            filterStartDatePicker.setValue(null);
            filterDateRangeSpinner.getValueFactory().setValue(15);
            filterProjectIdField.clear();
            filterDescriptionField.clear();
            applyFilters.run();
        });
        
        // Update project list when main date range changes
        Runnable updateProjectList = () -> applyFilters.run();
        
        // Set up proper string converter for display
        projectCombo.setConverter(new javafx.util.StringConverter<Project>() {
            @Override
            public String toString(Project project) {
                if (project == null) {
                    return "";
                }
                return project.getProjectId() + " - " + project.getDescription();
            }
            
            @Override
            public Project fromString(String string) {
                return projectCombo.getItems().stream()
                    .filter(p -> (p.getProjectId() + " - " + p.getDescription()).equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        // Update project list when date range changes
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateProjectList.run());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateProjectList.run());
        
        // Initialize the project list
        updateProjectList.run();
        
        // Preview container
        VBox previewContainer = new VBox(10);
        previewContainer.setAlignment(Pos.CENTER);
        
        projectCombo.setOnAction(e -> {
            Project selected = projectCombo.getValue();
            if (selected != null) {
                generateClientProjectPreview(selected, previewContainer);
            }
        });
        
        // Create filter section
        VBox filterSection = new VBox(10);
        filterSection.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label filterLabel = new Label("Filter Projects:");
        filterLabel.setStyle("-fx-font-weight: bold;");
        
        HBox dateFilterBox = new HBox(10);
        dateFilterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dateFilterBox.getChildren().addAll(
            new Label("Start Date:"), filterStartDatePicker,
            new Label("±"), filterDateRangeSpinner, new Label("days")
        );
        
        HBox textFilterBox = new HBox(10);
        textFilterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        textFilterBox.getChildren().addAll(
            new Label("Project ID:"), filterProjectIdField,
            new Label("Description:"), filterDescriptionField,
            clearFiltersButton
        );
        
        filterSection.getChildren().addAll(filterLabel, dateFilterBox, textFilterBox);
        
        // Project selection with count
        HBox projectSelectionBox = new HBox(10);
        projectSelectionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        projectSelectionBox.getChildren().addAll(projectCombo, projectCountLabel);
        
        content.getChildren().addAll(
            filterSection,
            new Label("Select Project for Report:"),
            projectSelectionBox,
            new Separator(),
            previewContainer
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }
    
    private ScrollPane createResourceUtilizationView() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        
        // Options panel
        HBox options = new HBox(10);
        options.setAlignment(Pos.CENTER);
        
        CheckBox showChartsCheck = new CheckBox("Include Charts");
        showChartsCheck.setSelected(true);
        
        CheckBox showDetailsCheck = new CheckBox("Show Detailed Breakdown");
        showDetailsCheck.setSelected(true);
        
        Button generateButton = new Button("Generate Preview");
        
        options.getChildren().addAll(showChartsCheck, showDetailsCheck, generateButton);
        
        // Preview container
        VBox previewContainer = new VBox(10);
        previewContainer.setAlignment(Pos.CENTER);
        
        generateButton.setOnAction(e -> {
            generateResourceUtilizationPreview(
                showChartsCheck.isSelected(), 
                showDetailsCheck.isSelected(),
                previewContainer
            );
        });
        
        content.getChildren().addAll(
            new Label("Resource Utilization Report Options:"),
            options,
            new Separator(),
            previewContainer
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }
    
    private ScrollPane createProjectPipelineView() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        
        // Forecast period selector
        HBox options = new HBox(10);
        options.setAlignment(Pos.CENTER);
        
        ComboBox<String> periodCombo = new ComboBox<>();
        periodCombo.getItems().addAll("Next 30 Days", "Next Quarter", "Next 6 Months", "Next Year");
        periodCombo.setValue("Next Quarter");
        
        CheckBox includeProposed = new CheckBox("Include Proposed Projects");
        includeProposed.setSelected(false);
        
        Button generateButton = new Button("Generate Forecast");
        
        options.getChildren().addAll(
            new Label("Forecast Period:"), periodCombo,
            includeProposed, generateButton
        );
        
        // Preview container
        VBox previewContainer = new VBox(10);
        previewContainer.setAlignment(Pos.CENTER);
        
        generateButton.setOnAction(e -> {
            generatePipelinePreview(
                periodCombo.getValue(),
                includeProposed.isSelected(),
                previewContainer
            );
        });
        
        content.getChildren().addAll(
            new Label("Project Pipeline & Forecast:"),
            options,
            new Separator(),
            previewContainer
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }
    
    private ScrollPane createRevenueView() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        
        // Note about revenue tracking
        Label noteLabel = new Label(
            "Note: Revenue tracking requires project budget data to be configured.\n" +
            "This is a preview of the report structure."
        );
        noteLabel.setWrapText(true);
        noteLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        
        Button generateButton = new Button("Generate Revenue Report Preview");
        
        // Preview container
        VBox previewContainer = new VBox(10);
        previewContainer.setAlignment(Pos.CENTER);
        
        generateButton.setOnAction(e -> generateRevenuePreview(previewContainer));
        
        content.getChildren().addAll(
            new Label("Revenue & Budget Tracking:"),
            noteLabel,
            generateButton,
            new Separator(),
            previewContainer
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }
    
    private ScrollPane createGeographicView() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        
        // Options
        CheckBox showHeatMap = new CheckBox("Show Concentration Heat Map");
        showHeatMap.setSelected(true);
        
        CheckBox showTravelAnalysis = new CheckBox("Include Travel Analysis");
        showTravelAnalysis.setSelected(true);
        
        Button generateButton = new Button("Generate Geographic Report");
        
        HBox options = new HBox(10);
        options.setAlignment(Pos.CENTER);
        options.getChildren().addAll(showHeatMap, showTravelAnalysis, generateButton);
        
        // Preview container
        VBox previewContainer = new VBox(10);
        previewContainer.setAlignment(Pos.CENTER);
        
        generateButton.setOnAction(e -> {
            generateGeographicPreview(
                showHeatMap.isSelected(),
                showTravelAnalysis.isSelected(),
                previewContainer
            );
        });
        
        content.getChildren().addAll(
            new Label("Geographic Distribution Analysis:"),
            options,
            new Separator(),
            previewContainer
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }
    
    private ScrollPane createAnalyticsView() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        
        // Time period selector
        ComboBox<String> periodCombo = new ComboBox<>();
        periodCombo.getItems().addAll("Last Month", "Last Quarter", "Last 6 Months", "Last Year");
        periodCombo.setValue("Last Quarter");
        
        Button generateButton = new Button("Generate Analytics");
        
        HBox options = new HBox(10);
        options.setAlignment(Pos.CENTER);
        options.getChildren().addAll(new Label("Analysis Period:"), periodCombo, generateButton);
        
        // Preview container
        VBox previewContainer = new VBox(10);
        previewContainer.setAlignment(Pos.CENTER);
        
        generateButton.setOnAction(e -> {
            generateAnalyticsPreview(periodCombo.getValue(), previewContainer);
        });
        
        content.getChildren().addAll(
            new Label("Completion Rate Analytics:"),
            options,
            new Separator(),
            previewContainer
        );
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }
    
    // Preview generation methods
    private void generateClientProjectPreview(Project project, VBox container) {
        container.getChildren().clear();
        
        statusLabel.setText("Generating preview for " + project.getProjectId() + "...");
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        
        Task<Image> task = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                // Generate PDF report
                currentReportFile = clientReportService.generateProjectReport(project);
                currentReportType = ReportType.CLIENT_PROJECT;
                
                // Convert first page to image for preview
                return convertPdfPageToImage(currentReportFile, 0);
            }
        };
        
        task.setOnSucceeded(e -> {
            Image preview = task.getValue();
            if (preview != null) {
                displayPreview(preview, container, project.getProjectId() + " Report Preview");
            }
            progressBar.setVisible(false);
            statusLabel.setText("Preview generated successfully");
        });
        
        task.setOnFailed(e -> {
            showError("Failed to generate preview: " + task.getException().getMessage());
            progressBar.setVisible(false);
            statusLabel.setText("Preview generation failed");
        });
        
        new Thread(task).start();
    }
    
    private void generateResourceUtilizationPreview(boolean includeCharts, boolean showDetails, VBox container) {
        container.getChildren().clear();
        
        statusLabel.setText("Generating resource utilization preview...");
        progressBar.setVisible(true);
        
        Task<Image> task = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                // Generate report
                currentReportFile = resourceReportService.generateReport(includeCharts, showDetails);
                currentReportType = ReportType.RESOURCE_UTILIZATION;
                
                return convertPdfPageToImage(currentReportFile, 0);
            }
        };
        
        task.setOnSucceeded(e -> {
            Image preview = task.getValue();
            if (preview != null) {
                displayPreview(preview, container, "Resource Utilization Preview");
            }
            progressBar.setVisible(false);
            statusLabel.setText("Preview generated successfully");
        });
        
        task.setOnFailed(e -> {
            showError("Failed to generate preview: " + task.getException().getMessage());
            progressBar.setVisible(false);
        });
        
        new Thread(task).start();
    }
    
    private void generatePipelinePreview(String period, boolean includeProposed, VBox container) {
        container.getChildren().clear();
        statusLabel.setText("Generating pipeline forecast preview...");
        progressBar.setVisible(true);
        
        Task<Image> task = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                currentReportFile = pipelineReportService.generateReport(period, includeProposed);
                currentReportType = ReportType.PROJECT_PIPELINE;
                return convertPdfPageToImage(currentReportFile, 0);
            }
        };
        
        task.setOnSucceeded(e -> {
            Image preview = task.getValue();
            if (preview != null) {
                displayPreview(preview, container, "Project Pipeline Preview");
            }
            progressBar.setVisible(false);
            statusLabel.setText("Preview generated successfully");
        });
        
        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            logger.error("Failed to generate preview", exception);
            showError("Failed to generate preview: " + (exception != null ? exception.getMessage() : "Unknown error"));
            progressBar.setVisible(false);
        });
        
        new Thread(task).start();
    }
    
    private void generateRevenuePreview(VBox container) {
        container.getChildren().clear();
        statusLabel.setText("Generating revenue report preview...");
        progressBar.setVisible(true);
        
        Task<Image> task = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                // Use the date range from the date pickers
                LocalDate start = startDatePicker.getValue();
                LocalDate end = endDatePicker.getValue();
                currentReportFile = revenueReportService.generateReport(start, end);
                currentReportType = ReportType.REVENUE_BUDGET;
                return convertPdfPageToImage(currentReportFile, 0);
            }
        };
        
        task.setOnSucceeded(e -> {
            Image preview = task.getValue();
            if (preview != null) {
                displayPreview(preview, container, "Revenue & Budget Preview");
            }
            progressBar.setVisible(false);
            statusLabel.setText("Preview generated successfully");
        });
        
        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            logger.error("Failed to generate preview", exception);
            showError("Failed to generate preview: " + (exception != null ? exception.getMessage() : "Unknown error"));
            progressBar.setVisible(false);
        });
        
        new Thread(task).start();
    }
    
    private void generateGeographicPreview(boolean showHeatMap, boolean showTravel, VBox container) {
        container.getChildren().clear();
        statusLabel.setText("Generating geographic distribution preview...");
        progressBar.setVisible(true);
        
        Task<Image> task = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                currentReportFile = geographicReportService.generateReport(showHeatMap, showTravel);
                currentReportType = ReportType.GEOGRAPHIC_DISTRIBUTION;
                return convertPdfPageToImage(currentReportFile, 0);
            }
        };
        
        task.setOnSucceeded(e -> {
            Image preview = task.getValue();
            if (preview != null) {
                displayPreview(preview, container, "Geographic Distribution Preview");
            }
            progressBar.setVisible(false);
            statusLabel.setText("Preview generated successfully");
        });
        
        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            logger.error("Failed to generate preview", exception);
            showError("Failed to generate preview: " + (exception != null ? exception.getMessage() : "Unknown error"));
            progressBar.setVisible(false);
        });
        
        new Thread(task).start();
    }
    
    private void generateAnalyticsPreview(String period, VBox container) {
        container.getChildren().clear();
        statusLabel.setText("Generating analytics preview...");
        progressBar.setVisible(true);
        
        Task<Image> task = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                currentReportFile = completionAnalyticsService.generateReport(period);
                currentReportType = ReportType.COMPLETION_ANALYTICS;
                return convertPdfPageToImage(currentReportFile, 0);
            }
        };
        
        task.setOnSucceeded(e -> {
            Image preview = task.getValue();
            if (preview != null) {
                displayPreview(preview, container, "Completion Analytics Preview");
            }
            progressBar.setVisible(false);
            statusLabel.setText("Preview generated successfully");
        });
        
        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            logger.error("Failed to generate preview", exception);
            showError("Failed to generate preview: " + (exception != null ? exception.getMessage() : "Unknown error"));
            progressBar.setVisible(false);
        });
        
        new Thread(task).start();
    }
    
    private void preloadProjectReports(Project project) {
        // Select the Client Projects tab first
        reportTabs.getSelectionModel().select(0);
        
        // Find the project combo box and select the project
        Tab clientTab = reportTabs.getTabs().get(0);
        ScrollPane scrollPane = (ScrollPane) clientTab.getContent();
        VBox content = (VBox) scrollPane.getContent();
        
        // Find the project combo box
        content.getChildren().stream()
            .filter(node -> node instanceof ComboBox<?>)
            .map(node -> (ComboBox<Project>) node)
            .findFirst()
            .ifPresent(combo -> {
                // Select the project
                combo.setValue(project);
                // Fire the action to generate preview
                combo.getOnAction().handle(null);
            });
        
        // Generate Resource Utilization report
        Platform.runLater(() -> {
            reportTabs.getSelectionModel().select(1);
            Tab resourceTab = reportTabs.getTabs().get(1);
            ScrollPane resourceScroll = (ScrollPane) resourceTab.getContent();
            VBox resourceContent = (VBox) resourceScroll.getContent();
            resourceContent.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> (HBox) node)
                .flatMap(hbox -> hbox.getChildren().stream())
                .filter(node -> node instanceof Button && ((Button) node).getText().equals("Generate Preview"))
                .findFirst()
                .ifPresent(button -> ((Button) button).fire());
        });
        
        // Switch back to Client Projects tab
        Platform.runLater(() -> reportTabs.getSelectionModel().select(0));
    }
    
    private void refreshCurrentTabPreview(Tab tab) {
        // Determine which tab is selected and refresh its content
        String tabText = tab.getText();
        switch (tabText) {
            case "Client Projects":
                // Trigger refresh for client project view
                break;
            case "Resource Utilization":
                // Find and click the generate button in the resource tab
                ScrollPane scrollPane = (ScrollPane) tab.getContent();
                VBox content = (VBox) scrollPane.getContent();
                content.getChildren().stream()
                    .filter(node -> node instanceof HBox)
                    .map(node -> (HBox) node)
                    .flatMap(hbox -> hbox.getChildren().stream())
                    .filter(node -> node instanceof Button && ((Button) node).getText().equals("Generate Preview"))
                    .findFirst()
                    .ifPresent(button -> ((Button) button).fire());
                break;
            case "Project Pipeline":
                // Similar logic for other tabs
                break;
            default:
                break;
        }
    }
    
    private void displayPreview(Image image, VBox container, String title) {
        displayMultiPagePreview(currentReportFile, container, title, 0);
    }
    
    private void displayMultiPagePreview(File pdfFile, VBox container, String title, int currentPage) {
        if (pdfFile == null) return;
        
        try {
            // Get total page count
            int totalPages;
            try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(pdfFile)) {
                totalPages = doc.getNumberOfPages();
            }
            
            // Render current page
            Image pageImage = convertPdfPageToImage(pdfFile, currentPage);
            
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            ImageView imageView = new ImageView(pageImage);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);
            
            // Add page navigation
            HBox pageControls = new HBox(10);
            pageControls.setAlignment(Pos.CENTER);
            
            Button prevButton = new Button("Previous Page");
            prevButton.setDisable(currentPage == 0);
            prevButton.setOnAction(e -> displayMultiPagePreview(pdfFile, container, title, currentPage - 1));
            
            Button nextButton = new Button("Next Page");
            nextButton.setDisable(currentPage >= totalPages - 1);
            nextButton.setOnAction(e -> displayMultiPagePreview(pdfFile, container, title, currentPage + 1));
            
            Label pageLabel = new Label(String.format("Page %d of %d", currentPage + 1, totalPages));
            
            pageControls.getChildren().addAll(prevButton, pageLabel, nextButton);
            
            container.getChildren().clear();
            container.getChildren().addAll(titleLabel, pageControls, imageView);
            
        } catch (IOException e) {
            logger.error("Failed to display preview page", e);
            showError("Failed to display preview: " + e.getMessage());
        }
    }
    
    private Image convertPdfPageToImage(File pdfFile, int pageNumber) throws IOException {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage bufferedImage = renderer.renderImageWithDPI(pageNumber, 150);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        }
    }
    
    private void saveCurrentReport() {
        if (currentReportFile == null) {
            showError("No report to save. Please generate a preview first.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.setInitialFileName(currentReportType.toString().replace(" ", "_") + "_" + 
                                       LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".pdf");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File selectedFile = fileChooser.showSaveDialog(this);
        if (selectedFile != null) {
            try {
                java.nio.file.Files.copy(
                    currentReportFile.toPath(),
                    selectedFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                statusLabel.setText("Report saved to " + selectedFile.getName());
            } catch (IOException e) {
                showError("Failed to save report: " + e.getMessage());
            }
        }
    }
    
    private void emailCurrentReport() {
        if (currentReportFile == null) {
            showError("No report to email. Please generate a preview first.");
            return;
        }
        
        EmailConfiguration emailConfig = EmailConfiguration.load();
        if (!emailConfig.isConfigured()) {
            showError("Email not configured. Please configure email settings first.");
            return;
        }
        
        // Show email dialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Email Report");
        dialog.setHeaderText("Send " + currentReportType + " to:");
        dialog.setContentText("Email address:");
        
        dialog.showAndWait().ifPresent(email -> {
            EmailService emailService = new EmailService(emailConfig);
            
            statusLabel.setText("Sending email...");
            progressBar.setVisible(true);
            
            Task<Boolean> emailTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    String subject = currentReportType + " - " + 
                                   LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                    String body = "Please find attached the " + currentReportType + 
                                " generated on " + LocalDate.now();
                    
                    return emailService.sendEmail(email, subject, body, currentReportFile);
                }
            };
            
            emailTask.setOnSucceeded(e -> {
                if (emailTask.getValue()) {
                    statusLabel.setText("Email sent successfully");
                } else {
                    statusLabel.setText("Failed to send email");
                }
                progressBar.setVisible(false);
            });
            
            emailTask.setOnFailed(e -> {
                showError("Failed to send email: " + emailTask.getException().getMessage());
                progressBar.setVisible(false);
            });
            
            new Thread(emailTask).start();
        });
    }
    
    private void printCurrentReport() {
        if (currentReportFile == null) {
            showError("No report to print. Please generate a preview first.");
            return;
        }
        
        // TODO: Implement printing functionality
        showInfo("Print functionality will be available in a future update.");
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}