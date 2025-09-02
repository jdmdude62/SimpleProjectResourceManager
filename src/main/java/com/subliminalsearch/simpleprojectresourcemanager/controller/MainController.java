package com.subliminalsearch.simpleprojectresourcemanager.controller;

import com.subliminalsearch.simpleprojectresourcemanager.component.TimelineView;
import com.subliminalsearch.simpleprojectresourcemanager.component.ToastNotification;
import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.controller.ClientReportController;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.AssignmentDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.EmailSettingsDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.HolidayDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.MultiResourceAssignmentDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.OpenItemsManagementDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.POImportDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ProjectDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ProjectManagerDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ProjectSelectionDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ResourceDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ResourceUnavailabilityDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ManageUnavailabilityDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ResourceAvailabilityDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ShopAutoAssignDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.UtilizationSettingsDialog;
import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.model.CompanyHoliday;
import com.subliminalsearch.simpleprojectresourcemanager.model.EmailConfiguration;
import com.subliminalsearch.simpleprojectresourcemanager.model.HolidayType;
import com.subliminalsearch.simpleprojectresourcemanager.model.UtilizationSettings;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.TechnicianUnavailability;
import com.subliminalsearch.simpleprojectresourcemanager.model.UnavailabilityType;
import com.subliminalsearch.simpleprojectresourcemanager.service.FinancialService;
import com.subliminalsearch.simpleprojectresourcemanager.service.POSpreadsheetImportService;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.service.SoftDeleteService;
import com.subliminalsearch.simpleprojectresourcemanager.service.UndoManager;
import com.subliminalsearch.simpleprojectresourcemanager.service.UtilizationService;
import com.subliminalsearch.simpleprojectresourcemanager.util.DatabaseMonitor;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import com.subliminalsearch.simpleprojectresourcemanager.view.ExecutiveCommandCenter;
import com.subliminalsearch.simpleprojectresourcemanager.view.HolidayCalendarView;
import com.subliminalsearch.simpleprojectresourcemanager.view.FinancialTimelineView;
import com.subliminalsearch.simpleprojectresourcemanager.view.FinancialTrackingDialog;
import com.subliminalsearch.simpleprojectresourcemanager.view.ProjectGridView;
import com.subliminalsearch.simpleprojectresourcemanager.view.ResourceGridView;
import com.subliminalsearch.simpleprojectresourcemanager.view.ReportCenterView;
import com.subliminalsearch.simpleprojectresourcemanager.view.TaskListView;
import com.subliminalsearch.simpleprojectresourcemanager.repository.TaskRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    private final SchedulingService schedulingService;
    private final DatabaseConfig databaseConfig;
    private UtilizationService utilizationService;
    private com.subliminalsearch.simpleprojectresourcemanager.service.SharePointSyncService sharePointSyncService;

    // FXML Components - Main Layout
    @FXML private BorderPane mainBorderPane;
    @FXML private MenuBar menuBar;
    @FXML private ToolBar toolBar;
    @FXML private VBox filterPanel;
    @FXML private BorderPane timelineContainer;
    @FXML private Label statusLabel;
    
    // FXML Components - Menu Items
    @FXML private MenuItem menuNewProject;
    @FXML private MenuItem menuNewResource;
    @FXML private MenuItem menuNewAssignment;
    @FXML private MenuItem menuMultiAssignment;
    @FXML private MenuItem menuProjectManagers;
    @FXML private MenuItem menuProjectGrid;
    @FXML private MenuItem menuResourceGrid;
    @FXML private MenuItem menuProjectTasks;
    @FXML private MenuItem menuImportExcel;
    @FXML private MenuItem menuBatchDeleteProjects;
    @FXML private MenuItem menuHolidayCalendarView;
    @FXML private MenuItem menuResourceAvailabilityView;
    @FXML private MenuItem menuCertificationManagement;
    @FXML private MenuItem menuSkillsManagement;
    @FXML private MenuItem menuResourceQualifications;
    @FXML private MenuItem menuSkillCategoryManagement;
    @FXML private MenuItem menuEmailSettings;
    @FXML private MenuItem menuUtilizationSettings;
    @FXML private MenuItem menuDomainLogins;
    @FXML private MenuItem menuSharePointSettings;
    @FXML private MenuItem menuSyncNow;
    @FXML private MenuItem menuViewSyncLog;
    
    @FXML
    public void initialize() {
        System.out.println("============ CONTROLLER INITIALIZE CALLED ============");
        
        // Debug: Find menu items manually by traversing the menu bar
        if (menuBar != null) {
            System.out.println("MenuBar found, searching for SharePoint menu items...");
            for (Menu menu : menuBar.getMenus()) {
                System.out.println("Found menu: " + menu.getText());
                if ("Tools".equals(menu.getText()) || "SYNC TEST".equals(menu.getText())) {
                    for (MenuItem item : menu.getItems()) {
                        System.out.println("  Found menu item: " + item.getText());
                        if (item.getText() != null && 
                            (item.getText().contains("SharePoint Sync Now") || 
                             item.getText().contains("Test SharePoint Sync"))) {
                            System.out.println("  >> Binding SharePoint sync to: " + item.getText());
                            item.setOnAction(event -> {
                                System.out.println("MENU CLICKED! Item: " + item.getText());
                                syncSharePointNow();
                            });
                        }
                    }
                }
            }
        } else {
            System.out.println("ERROR: menuBar is NULL!");
        }
        
        // Also try the original binding
        if (menuSyncNow != null) {
            System.out.println("menuSyncNow field found, binding action manually");
            menuSyncNow.setOnAction(event -> {
                System.out.println("Menu clicked via field binding!");
                syncSharePointNow();
            });
        } else {
            System.out.println("WARNING: menuSyncNow field is NULL");
        }
        
        // Bind the toolbar button
        if (btnSharePointSync != null) {
            System.out.println("SharePoint Sync button found, binding action");
            btnSharePointSync.setOnAction(event -> {
                System.out.println("SHAREPOINT SYNC BUTTON CLICKED!");
                syncSharePointNow();
            });
            
            // Remove auto-trigger test code
        } else {
            System.out.println("WARNING: btnSharePointSync is NULL");
        }
    }
    @FXML private MenuItem menuReportCenter;
    @FXML private MenuItem menuFinancialTracking;
    @FXML private RadioMenuItem menuMonthView;
    @FXML private RadioMenuItem menuWeekView;
    @FXML private RadioMenuItem menuDayView;
    @FXML private CheckMenuItem menuShowAllResources;
    @FXML private MenuItem menuRefresh;
    @FXML private MenuItem menuQuickRevenueBudget;
    @FXML private MenuItem menuQuickResourceUtil;
    @FXML private MenuItem menuQuickProjectStatus;
    @FXML private MenuItem menuQuickWorkload;
    @FXML private MenuItem menuUserGuide;
    @FXML private MenuItem menuKeyboardShortcuts;
    @FXML private MenuItem menuAbout;

    // FXML Components - Toolbar
    @FXML private Button btnPrevDay;
    @FXML private Button btnPrevWeek;
    @FXML private Button btnPrevMonth;
    @FXML private Button btnToday;
    @FXML private Button btnNextDay;
    @FXML private Button btnNextWeek;
    @FXML private Button btnNextMonth;
    
    @FXML private ComboBox<String> viewModeCombo;
    @FXML private DatePicker currentDatePicker;
    @FXML private ComboBox<String> zoomCombo;

    // FXML Components - Filter Panel
    @FXML private ComboBox<String> projectManagerFilter;
    @FXML private ComboBox<String> projectFilter;
    @FXML private ComboBox<String> resourceFilter;
    @FXML private ComboBox<ProjectStatus> statusFilter;
    @FXML private Button btnClearFilters;
    @FXML private CheckBox showUnavailabilityCheckBox;

    // FXML Components - Action Buttons
    @FXML private Button btnNewProject;
    @FXML private Button btnNewResource;
    @FXML private Button btnNewAssignment;
    @FXML private Button btnAutoAssignShop;
    @FXML private Button btnExecutiveView;
    @FXML private Button btnSharePointSync;

    // Timeline Component
    private TimelineView timelineView;
    private LocalDate currentDisplayDate;

    public MainController(SchedulingService schedulingService, DatabaseConfig databaseConfig) {
        this.schedulingService = schedulingService;
        this.databaseConfig = databaseConfig;
        this.currentDisplayDate = LocalDate.now();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing main controller...");
        
        // Run database migrations
        try {
            if (com.subliminalsearch.simpleprojectresourcemanager.util.DatabaseMigration
                    .hasProjectIdUniqueConstraint(schedulingService.getDataSource())) {
                logger.info("Running migration to remove UNIQUE constraint from project_id...");
                com.subliminalsearch.simpleprojectresourcemanager.util.DatabaseMigration
                    .removeProjectIdUniqueConstraint(schedulingService.getDataSource());
            }
        } catch (Exception e) {
            logger.error("Failed to run database migrations", e);
            // Don't show error for migrations, just log it
        }
        
        // Normalize resource types separately (non-critical)
        try {
            logger.info("Checking resource type normalization...");
            com.subliminalsearch.simpleprojectresourcemanager.util.NormalizeResourceTypes
                .normalizeTypes(schedulingService.getDataSource());
        } catch (Exception e) {
            // This is non-critical, just log it
            logger.warn("Could not normalize resource types (non-critical): {}", e.getMessage());
        }
        
        // Initialize utilization service
        utilizationService = new UtilizationService(schedulingService.getDataSource());
        
        // Initialize SharePoint sync service
        sharePointSyncService = com.subliminalsearch.simpleprojectresourcemanager.service.SharePointSyncService
            .getInstance(schedulingService.getDataSource());
        // Temporarily disabled auto-start due to Azure module access issues
        // sharePointSyncService.startSync();
        logger.info("SharePoint sync service initialized (auto-start disabled)");
        
        // Initialize method binding
        initialize();
        
        setupToolbar();
        setupFilters();
        setupTimeline();
        setupEventHandlers();
        
        refreshData();
        updateStatusLabel();
        
        // Add keyboard shortcut for SharePoint sync (Ctrl+Shift+S)
        setupKeyboardShortcuts();
        
        logger.info("Main controller initialized successfully");
    }

    private void setupKeyboardShortcuts() {
        // Set up keyboard shortcut for SharePoint sync
        timelineView.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.isShiftDown() && 
                event.getCode() == javafx.scene.input.KeyCode.S) {
                System.out.println("KEYBOARD SHORTCUT: Ctrl+Shift+S detected!");
                syncSharePointNow();
                event.consume();
            }
        });
        
        // Also add to menu bar
        menuBar.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.isShiftDown() && 
                event.getCode() == javafx.scene.input.KeyCode.S) {
                System.out.println("KEYBOARD SHORTCUT: Ctrl+Shift+S detected!");
                syncSharePointNow();
                event.consume();
            }
        });
        
        logger.info("Keyboard shortcuts configured - Press Ctrl+Shift+S for SharePoint sync");
        System.out.println("KEYBOARD SHORTCUT CONFIGURED: Press Ctrl+Shift+S to trigger SharePoint sync");
    }
    
    private void setupToolbar() {
        // Initialize view mode combo - Week, Month, Quarter views
        viewModeCombo.setItems(FXCollections.observableArrayList("Week", "Month", "Quarter"));
        viewModeCombo.setValue("Month");
        
        // Initialize current date picker
        currentDatePicker.setValue(currentDisplayDate);
        
        // Initialize zoom combo
        // Remove zoom levels below 100% as they're too tight to use effectively
        zoomCombo.setItems(FXCollections.observableArrayList("100%", "125%", "150%", "200%", "250%", "300%"));
        zoomCombo.setValue("100%");
        
        // Add nav-button class to all navigation buttons
        btnPrevDay.getStyleClass().add("nav-button");
        btnPrevWeek.getStyleClass().add("nav-button");
        btnPrevMonth.getStyleClass().add("nav-button");
        btnNextDay.getStyleClass().add("nav-button");
        btnNextWeek.getStyleClass().add("nav-button");
        btnNextMonth.getStyleClass().add("nav-button");
        
        // Make all navigation buttons equal size and same height as Today button (36px)
        String navButtonStyle = "-fx-padding: 5 8 5 8 !important; -fx-font-size: 12px !important; -fx-min-width: 40 !important; -fx-pref-width: 40 !important; -fx-max-width: 40 !important; -fx-min-height: 36 !important; -fx-pref-height: 36 !important;";
        
        // Apply equal sizing to all navigation buttons
        btnPrevDay.setStyle(navButtonStyle);
        btnPrevWeek.setStyle(navButtonStyle);
        btnPrevMonth.setStyle(navButtonStyle);
        btnNextDay.setStyle(navButtonStyle);
        btnNextWeek.setStyle(navButtonStyle);
        btnNextMonth.setStyle(navButtonStyle);
        
        // Set button tooltips with clear descriptions
        btnPrevDay.setTooltip(new Tooltip("Go back one day (Ctrl+Left)"));
        btnPrevWeek.setTooltip(new Tooltip("Go back one week (Shift+Left)"));
        btnPrevMonth.setTooltip(new Tooltip("Go back one month (Ctrl+Shift+Left)"));
        btnToday.setTooltip(new Tooltip("Jump to today's date (Ctrl+Home)"));
        btnNextDay.setTooltip(new Tooltip("Go forward one day (Ctrl+Right)"));
        btnNextWeek.setTooltip(new Tooltip("Go forward one week (Shift+Right)"));
        btnNextMonth.setTooltip(new Tooltip("Go forward one month (Ctrl+Shift+Right)"));
    }

    private void setupFilters() {
        // Initialize status filter
        statusFilter.setItems(FXCollections.observableArrayList(ProjectStatus.values()));
        
        // Initialize project manager filter with unique managers
        updateProjectManagerFilter();
        
        // Initialize resource filter
        updateResourceFilter();
        
        // Set default selections
        projectManagerFilter.setValue("All Managers");
        projectFilter.setValue("All Projects");
        resourceFilter.setValue("All Resources");
        statusFilter.setValue(null); // All statuses
    }
    
    private void updateProjectManagerFilter() {
        // Get unique project managers from all projects
        List<String> managers = new ArrayList<>();
        managers.add("All Managers");
        
        // Get all project managers from the repository
        List<ProjectManager> allManagers = schedulingService.getAllProjectManagers();
        managers.addAll(allManagers.stream()
            .map(ProjectManager::getName)
            .sorted()
            .toList());
        
        String currentValue = projectManagerFilter.getValue();
        projectManagerFilter.setItems(FXCollections.observableArrayList(managers));
        if (currentValue != null && managers.contains(currentValue)) {
            projectManagerFilter.setValue(currentValue);
        } else {
            projectManagerFilter.setValue("All Managers");
        }
        
        updateProjectFilter(); // Update project filter based on selected manager
    }
    
    private void updateProjectFilter() {
        String selectedManager = projectManagerFilter.getValue();
        List<String> projectNames = new ArrayList<>();
        projectNames.add("All Projects");
        
        List<Project> projects = schedulingService.getAllProjects();
        
        // Filter projects by selected manager
        if (selectedManager != null && !selectedManager.equals("All Managers")) {
            // Get the selected project manager
            ProjectManager selectedPM = schedulingService.getAllProjectManagers().stream()
                .filter(pm -> pm.getName().equals(selectedManager))
                .findFirst()
                .orElse(null);
            
            if (selectedPM != null) {
                projects = projects.stream()
                    .filter(p -> p.getProjectManagerId() != null && 
                                p.getProjectManagerId().equals(selectedPM.getId()))
                    .toList();
            } else {
                // Handle unassigned case
                projects = projects.stream()
                    .filter(p -> p.getProjectManagerId() == null)
                    .toList();
            }
        }
        
        // Get unique project IDs (aggregate duplicates)
        Set<String> uniqueProjectIds = projects.stream()
            .map(Project::getProjectId)
            .collect(Collectors.toCollection(TreeSet::new)); // TreeSet for sorted unique values
        
        projectNames.addAll(uniqueProjectIds);
        
        String currentValue = projectFilter.getValue();
        projectFilter.setItems(FXCollections.observableArrayList(projectNames));
        if (currentValue != null && projectNames.contains(currentValue)) {
            projectFilter.setValue(currentValue);
        } else {
            projectFilter.setValue("All Projects");
        }
    }
    
    private void updateResourceFilter() {
        // Get all resources from the repository
        List<String> resourceNames = new ArrayList<>();
        resourceNames.add("All Resources");
        
        List<Resource> allResources = schedulingService.getAllResources();
        resourceNames.addAll(allResources.stream()
            .map(Resource::getName)
            .sorted()
            .toList());
        
        String currentValue = resourceFilter.getValue();
        resourceFilter.setItems(FXCollections.observableArrayList(resourceNames));
        if (currentValue != null && resourceNames.contains(currentValue)) {
            resourceFilter.setValue(currentValue);
        } else {
            resourceFilter.setValue("All Resources");
        }
    }

    private void setupTimeline() {
        // Create and initialize timeline view
        timelineView = new TimelineView();
        
        // Set the shared database config
        timelineView.setDatabaseConfig(databaseConfig);
        
        // Set utilization settings from service
        timelineView.setUtilizationSettings(utilizationService.getSettings());
        
        // Set up context menu callbacks
        timelineView.setOnEditProject(this::editProject);
        timelineView.setOnManageOpenItems(this::manageOpenItems);
        timelineView.setOnDeleteProject(this::deleteProject);
        timelineView.setOnDeleteProjectWithAssignments(this::deleteProjectWithAssignments);
        timelineView.setOnEditResource(this::editResource);
        timelineView.setOnDeleteResource(this::deleteResource);
        timelineView.setOnMarkResourceUnavailable(this::markResourceUnavailable);
        timelineView.setOnViewResourceUnavailability(this::viewResourceUnavailability);
        timelineView.setOnEditAssignment(this::editAssignment);
        timelineView.setOnDeleteAssignment(this::deleteAssignment);
        timelineView.setOnDuplicateAssignment(this::duplicateAssignment);
        timelineView.setOnShowProjectDetails(project -> 
            ProjectDialog.showProjectDetails(project, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null));
        timelineView.setOnShowResourceDetails(resource -> 
            ResourceDialog.showResourceDetails(resource, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null));
        timelineView.setOnChangeProjectStatus(this::changeProjectStatus);
        timelineView.setOnGenerateReport(this::generateClientReport);
        timelineView.setOnShowProjectTasks(this::showProjectTasks);
        timelineView.setOnViewInReportCenter(this::viewInReportCenter);
        timelineView.setOnApplyFilter(this::applyFilterFromTimeline);
        
        // Replace the timeline container content
        timelineContainer.setCenter(timelineView);
        
        // Set initial date range (6 weeks from start of current month)
        LocalDate startOfMonth = currentDisplayDate.withDayOfMonth(1);
        LocalDate endOfView = startOfMonth.plusDays(41);  // 42 days = 6 weeks
        timelineView.setDateRange(startOfMonth, endOfView);
    }

    private void setupEventHandlers() {
        // Navigation button handlers
        btnPrevDay.setOnAction(e -> navigatePreviousDay());
        btnPrevWeek.setOnAction(e -> navigatePreviousWeek());
        btnPrevMonth.setOnAction(e -> navigatePreviousMonth());
        btnToday.setOnAction(e -> navigateToday());
        btnNextDay.setOnAction(e -> navigateNextDay());
        btnNextWeek.setOnAction(e -> navigateNextWeek());
        btnNextMonth.setOnAction(e -> navigateNextMonth());
        
        // Date picker handler
        currentDatePicker.setOnAction(e -> {
            currentDisplayDate = currentDatePicker.getValue();
            refreshData();
        });
        
        // View mode handler
        viewModeCombo.setOnAction(e -> refreshData());
        
        // Zoom handler
        zoomCombo.setOnAction(e -> {
            String selectedZoom = zoomCombo.getValue();
            if (selectedZoom != null && timelineView != null) {
                // Parse percentage and convert to scale factor
                String percentStr = selectedZoom.replace("%", "");
                double zoomFactor = Double.parseDouble(percentStr) / 100.0;
                timelineView.setZoomLevel(zoomFactor);
                refreshData();
            }
        });
        
        // Filter handlers
        projectManagerFilter.setOnAction(e -> {
            updateProjectFilter(); // Update project list based on selected manager
            applyFilters();
        });
        projectFilter.setOnAction(e -> applyFilters());
        resourceFilter.setOnAction(e -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
        btnClearFilters.setOnAction(e -> clearFilters());
        
        // Unavailability toggle handler
        if (showUnavailabilityCheckBox != null) {
            showUnavailabilityCheckBox.setOnAction(e -> {
                if (timelineView != null) {
                    timelineView.setShowUnavailability(showUnavailabilityCheckBox.isSelected());
                }
            });
        }
        
        // Action button handlers
        btnNewProject.setOnAction(e -> createNewProject());
        btnNewResource.setOnAction(e -> createNewResource());
        btnNewAssignment.setOnAction(e -> createNewAssignment());
        
        if (btnAutoAssignShop != null) {
            btnAutoAssignShop.setOnAction(e -> autoAssignShop());
            logger.info("Auto-Assign SHOP button initialized");
        } else {
            logger.error("btnAutoAssignShop is null - button not found in FXML");
        }
        
        btnExecutiveView.setOnAction(e -> showExecutiveCommandCenter());
        
        // Menu item handlers
        menuNewProject.setOnAction(e -> createNewProject());
        menuNewResource.setOnAction(e -> createNewResource());
        menuShowAllResources.setOnAction(e -> {
            // Toggle changed - refresh timeline with all resources or filtered resources
            refreshData();
        });
        menuNewAssignment.setOnAction(e -> createNewAssignment());
        menuMultiAssignment.setOnAction(e -> createMultiResourceAssignment());
        menuProjectManagers.setOnAction(e -> manageProjectManagers());
        menuProjectGrid.setOnAction(e -> showProjectGridView());
        menuResourceGrid.setOnAction(e -> showResourceGridView());
        menuImportExcel.setOnAction(e -> showImportExcelDialog());
        menuBatchDeleteProjects.setOnAction(e -> showBatchDeleteDialog());
        menuProjectTasks.setOnAction(e -> showProjectTasks());
        
        // Email settings handler
        if (menuEmailSettings != null) {
            menuEmailSettings.setOnAction(e -> showEmailSettings());
        }
        
        // Report Center handler
        if (menuReportCenter != null) {
            menuReportCenter.setOnAction(e -> showReportCenter());
        }
        
        // View menu handlers (if menu items exist)
        if (menuHolidayCalendarView != null) {
            menuHolidayCalendarView.setOnAction(e -> showHolidayCalendarView());
        }
        if (menuResourceAvailabilityView != null) {
            menuResourceAvailabilityView.setOnAction(e -> showResourceAvailabilityView());
        }
        if (menuCertificationManagement != null) {
            menuCertificationManagement.setOnAction(e -> showCertificationManagement());
        }
        if (menuSkillsManagement != null) {
            menuSkillsManagement.setOnAction(e -> showSkillsManagement());
        }
        if (menuResourceQualifications != null) {
            menuResourceQualifications.setOnAction(e -> showResourceQualifications());
        }
        if (menuSkillCategoryManagement != null) {
            menuSkillCategoryManagement.setOnAction(e -> showSkillCategoryManagement());
        }
    }

    // Navigation Methods
    private void navigatePreviousDay() {
        currentDisplayDate = currentDisplayDate.minusDays(1);
        currentDatePicker.setValue(currentDisplayDate);
        refreshData();
    }

    private void navigatePreviousWeek() {
        currentDisplayDate = currentDisplayDate.minusWeeks(1);
        currentDatePicker.setValue(currentDisplayDate);
        refreshData();
    }

    private void navigatePreviousMonth() {
        currentDisplayDate = currentDisplayDate.minusMonths(1);
        currentDatePicker.setValue(currentDisplayDate);
        refreshData();
    }

    private void navigateToday() {
        currentDisplayDate = LocalDate.now();
        currentDatePicker.setValue(currentDisplayDate);
        refreshData();
    }

    private void navigateNextDay() {
        currentDisplayDate = currentDisplayDate.plusDays(1);
        currentDatePicker.setValue(currentDisplayDate);
        refreshData();
    }

    private void navigateNextWeek() {
        currentDisplayDate = currentDisplayDate.plusWeeks(1);
        currentDatePicker.setValue(currentDisplayDate);
        refreshData();
    }

    private void navigateNextMonth() {
        currentDisplayDate = currentDisplayDate.plusMonths(1);
        currentDatePicker.setValue(currentDisplayDate);
        refreshData();
    }

    // Filter Methods
    private void applyFilters() {
        refreshData();
    }

    private void clearFilters() {
        projectManagerFilter.setValue("All Managers");
        projectFilter.setValue("All Projects");
        resourceFilter.setValue("All Resources");
        statusFilter.setValue(null);
        updateProjectFilter(); // Reset project list
        refreshData();
    }
    
    private void applyFilterFromTimeline(String filterType, Object filterValue) {
        logger.info("Applying filter from timeline - Type: {}, Value: {}", filterType, filterValue);
        switch (filterType) {
            case "project":
                String projectId = (String) filterValue;
                logger.info("Setting project filter to: {}", projectId);
                // Make sure the project is in the combo box items
                if (!projectFilter.getItems().contains(projectId)) {
                    logger.info("Project {} not in filter list, updating filter list", projectId);
                    // Need to update the project filter list first
                    updateProjectFilter();
                }
                projectFilter.setValue(projectId);
                logger.info("Project filter set to: {}", projectFilter.getValue());
                break;
            case "resource":
                String resourceName = (String) filterValue;
                // Make sure the resource is in the combo box items
                if (!resourceFilter.getItems().contains(resourceName)) {
                    updateResourceFilter();
                }
                resourceFilter.setValue(resourceName);
                break;
            case "status":
                statusFilter.setValue((ProjectStatus) filterValue);
                break;
            case "manager":
                // Find the manager name from ID
                Long managerId = (Long) filterValue;
                List<ProjectManager> managers = schedulingService.getAllProjectManagers();
                managers.stream()
                    .filter(m -> m.getId().equals(managerId))
                    .findFirst()
                    .ifPresent(manager -> projectManagerFilter.setValue(manager.getName()));
                break;
            case "clear":
                clearFilters();
                return;
        }
        applyFilters();
    }

    // Action Methods
    private void createNewProject() {
        // Pass the project repository and project managers
        List<ProjectManager> managers = schedulingService.getActiveProjectManagers();
        ProjectDialog dialog = new ProjectDialog(null, schedulingService.getProjectRepository(), managers);
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        Optional<Project> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                Project project = result.get();
                Project createdProject = schedulingService.createProject(
                    project.getProjectId(),
                    project.getDescription(),
                    project.getStartDate(),
                    project.getEndDate()
                );
                
                // Update additional fields including project manager
                if (project.getProjectManagerId() != null) {
                    createdProject.setProjectManagerId(project.getProjectManagerId());
                }
                if (project.getStatus() != null) {
                    createdProject.setStatus(project.getStatus());
                }
                // Copy travel field
                createdProject.setTravel(project.isTravel());
                
                // Copy client contact fields
                createdProject.setContactName(project.getContactName());
                createdProject.setContactEmail(project.getContactEmail());
                createdProject.setContactPhone(project.getContactPhone());
                createdProject.setContactCompany(project.getContactCompany());
                createdProject.setContactRole(project.getContactRole());
                createdProject.setContactAddress(project.getContactAddress());
                createdProject.setSendReports(project.isSendReports());
                createdProject.setReportFrequency(project.getReportFrequency());
                
                schedulingService.updateProject(createdProject);
                
                logger.info("Created new project: {}", createdProject.getProjectId());
                updateProjectManagerFilter(); // Update the filter list with any new manager
                refreshData();
                
                showInfoAlert("Success", "Project '" + createdProject.getProjectId() + "' created successfully.");
                
            } catch (Exception e) {
                logger.error("Error creating project", e);
                showErrorAlert("Error", "Failed to create project: " + e.getMessage());
            }
        }
    }

    private void createNewResource() {
        ResourceDialog dialog = new ResourceDialog();
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        Optional<Resource> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                Resource resource = result.get();
                Resource createdResource = schedulingService.createResource(
                    resource.getName(),
                    resource.getEmail(),
                    resource.getResourceType()
                );
                
                // Update additional fields
                createdResource.setActive(resource.isActive());
                // Note: Resource update method would be needed in service
                
                logger.info("Created new resource: {}", createdResource.getName());
                updateResourceFilter(); // Update the resource filter
                refreshData();
                
                showInfoAlert("Success", "Resource '" + createdResource.getName() + "' created successfully.");
                
            } catch (Exception e) {
                logger.error("Error creating resource", e);
                showErrorAlert("Error", "Failed to create resource: " + e.getMessage());
            }
        }
    }

    private void manageProjectManagers() {
        ProjectManagerDialog.showManagerListDialog(
            // Supplier that always gets fresh data
            () -> schedulingService.getAllProjectManagers(),
            // Edit handler
            manager -> {
                ProjectManagerDialog editDialog = new ProjectManagerDialog(manager);
                DialogUtils.initializeDialog(editDialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
                Optional<ProjectManager> result = editDialog.showAndWait();
                if (result.isPresent()) {
                    try {
                        schedulingService.updateProjectManager(result.get());
                        updateProjectManagerFilter();
                        refreshData();
                        showInfoAlert("Success", "Project Manager updated successfully.");
                    } catch (Exception e) {
                        logger.error("Error updating project manager", e);
                        showErrorAlert("Error", "Failed to update project manager: " + e.getMessage());
                    }
                }
            },
            // Delete handler
            manager -> {
                if (ProjectManagerDialog.showDeleteConfirmation(manager)) {
                    try {
                        schedulingService.deleteProjectManager(manager.getId());
                        updateProjectManagerFilter();
                        refreshData();
                        // Visual feedback is sufficient - no need for success dialog
                    } catch (Exception e) {
                        logger.error("Error deleting project manager", e);
                        showErrorAlert("Error", "Failed to delete project manager: " + e.getMessage());
                    }
                }
            },
            // Add handler
            () -> {
                ProjectManagerDialog addDialog = new ProjectManagerDialog();
                DialogUtils.initializeDialog(addDialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
                Optional<ProjectManager> result = addDialog.showAndWait();
                if (result.isPresent()) {
                    try {
                        ProjectManager manager = result.get();
                        schedulingService.createProjectManager(
                            manager.getName(),
                            manager.getEmail(),
                            manager.getPhone(),
                            manager.getDepartment()
                        );
                        updateProjectManagerFilter();
                        refreshData();
                        showInfoAlert("Success", "Project Manager created successfully.");
                    } catch (Exception e) {
                        logger.error("Error creating project manager", e);
                        showErrorAlert("Error", "Failed to create project manager: " + e.getMessage());
                    }
                }
            },
            // Refresh handler - called after any action to update the filters
            () -> {
                updateProjectManagerFilter();
            }
        );
    }
    
    private void createNewAssignment() {
        List<Project> projects = schedulingService.getAllProjects();
        List<Resource> resources = schedulingService.getAllResources();
        
        if (projects.isEmpty()) {
            showErrorAlert("No Projects", "Please create at least one project before creating assignments.");
            return;
        }
        
        if (resources.isEmpty()) {
            showErrorAlert("No Resources", "Please create at least one resource before creating assignments.");
            return;
        }
        
        AssignmentDialog dialog = new AssignmentDialog(projects, resources, schedulingService);
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        Optional<Assignment> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                Assignment assignment = result.get();
                Assignment createdAssignment;
                
                if (assignment.isOverride()) {
                    createdAssignment = schedulingService.createAssignmentWithOverride(
                        assignment.getProjectId(),
                        assignment.getResourceId(),
                        assignment.getStartDate(),
                        assignment.getEndDate(),
                        assignment.getTravelOutDays(),
                        assignment.getTravelBackDays(),
                        assignment.getOverrideReason()
                    );
                } else {
                    createdAssignment = schedulingService.createAssignment(
                        assignment.getProjectId(),
                        assignment.getResourceId(),
                        assignment.getStartDate(),
                        assignment.getEndDate(),
                        assignment.getTravelOutDays(),
                        assignment.getTravelBackDays()
                    );
                }
                
                logger.info("Created new assignment: {}", createdAssignment.getId());
                refreshData();
                
                showInfoAlert("Success", "Assignment created successfully.");
                
            } catch (Exception e) {
                logger.error("Error creating assignment", e);
                showErrorAlert("Error", "Failed to create assignment: " + e.getMessage());
            }
        }
    }
    
    private void autoAssignShop() {
        logger.info("Auto-Assign SHOP button clicked");
        try {
            ShopAutoAssignDialog dialog = new ShopAutoAssignDialog(schedulingService, 
                timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
            DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
            
            logger.info("Showing SHOP auto-assign dialog");
            Optional<Integer> result = dialog.showAndWait();
            
            if (result.isPresent() && result.get() > 0) {
                int count = result.get();
                logger.info("SHOP auto-assign completed with {} assignments", count);
                refreshData();
                showInfoAlert("Success", String.format("Created %d SHOP assignments successfully.", count));
            } else {
                logger.info("SHOP auto-assign cancelled or no assignments created");
            }
        } catch (Exception e) {
            logger.error("Error in autoAssignShop", e);
            showErrorAlert("Error", "Failed to open SHOP auto-assign dialog: " + e.getMessage());
        }
    }
    
    private void createMultiResourceAssignment() {
        List<Project> projects = schedulingService.getAllProjects();
        List<Resource> resources = schedulingService.getAllResources();
        
        if (projects.isEmpty()) {
            showErrorAlert("No Projects", "Please create at least one project before creating assignments.");
            return;
        }
        
        if (resources.isEmpty()) {
            showErrorAlert("No Resources", "Please create at least one resource before creating assignments.");
            return;
        }
        
        MultiResourceAssignmentDialog dialog = new MultiResourceAssignmentDialog(projects, resources, schedulingService);
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        Optional<List<Assignment>> result = dialog.showAndWait();
        
        if (result.isPresent() && !result.get().isEmpty()) {
            List<Assignment> assignments = result.get();
            int successCount = 0;
            int failureCount = 0;
            StringBuilder errors = new StringBuilder();
            
            for (Assignment assignment : assignments) {
                try {
                    Assignment createdAssignment;
                    
                    if (assignment.isOverride()) {
                        createdAssignment = schedulingService.createAssignmentWithOverride(
                            assignment.getProjectId(),
                            assignment.getResourceId(),
                            assignment.getStartDate(),
                            assignment.getEndDate(),
                            assignment.getTravelOutDays(),
                            assignment.getTravelBackDays(),
                            assignment.getOverrideReason()
                        );
                    } else {
                        createdAssignment = schedulingService.createAssignment(
                            assignment.getProjectId(),
                            assignment.getResourceId(),
                            assignment.getStartDate(),
                            assignment.getEndDate(),
                            assignment.getTravelOutDays(),
                            assignment.getTravelBackDays()
                        );
                    }
                    
                    logger.info("Created assignment: {} for resource {}", 
                               createdAssignment.getId(), assignment.getResourceId());
                    successCount++;
                    
                } catch (Exception e) {
                    failureCount++;
                    Resource resource = resources.stream()
                        .filter(r -> r.getId().equals(assignment.getResourceId()))
                        .findFirst()
                        .orElse(null);
                    String resourceName = resource != null ? resource.getName() : "Unknown";
                    errors.append("• ").append(resourceName).append(": ").append(e.getMessage()).append("\n");
                    logger.error("Error creating assignment for resource {}", assignment.getResourceId(), e);
                }
            }
            
            refreshData();
            
            // Show result summary
            if (failureCount == 0) {
                showInfoAlert("Success", 
                    String.format("Successfully created %d assignment%s.", 
                                 successCount, successCount > 1 ? "s" : ""));
            } else if (successCount > 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Partial Success");
                alert.setHeaderText(String.format("Created %d of %d assignments", 
                                                  successCount, assignments.size()));
                alert.setContentText("Failed assignments:\n" + errors.toString());
                alert.showAndWait();
            } else {
                showErrorAlert("Error", "Failed to create any assignments:\n" + errors.toString());
            }
        }
    }

    
    // Context menu handlers
    private void changeProjectStatus(Project project, ProjectStatus newStatus) {
        try {
            project.setStatus(newStatus);
            schedulingService.updateProject(project);
            logger.info("Changed status of project {} to {}", project.getProjectId(), newStatus);
            refreshData();
            showInfoAlert("Success", "Project status changed to " + newStatus.getDisplayName());
        } catch (Exception e) {
            logger.error("Error changing project status", e);
            showErrorAlert("Error", "Failed to change project status: " + e.getMessage());
        }
    }
    
    private void editProject(Project project) {
        logger.info("=== EDIT PROJECT START ===");
        logger.info("Editing project {} with current (stale) travel={}", project.getProjectId(), project.isTravel());
        
        // Reload the project from database to get latest data
        Optional<Project> freshProject = schedulingService.getProjectById(project.getId());
        if (!freshProject.isPresent()) {
            showErrorAlert("Error", "Project no longer exists");
            return;
        }
        
        project = freshProject.get();
        logger.info("Reloaded project {} from DB with travel={}", project.getProjectId(), project.isTravel());
        
        // Pass project managers list for editing
        List<ProjectManager> managers = schedulingService.getActiveProjectManagers();
        ProjectDialog dialog = new ProjectDialog(project, schedulingService.getProjectRepository(), managers);
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        
        logger.info("About to show dialog for project {}", project.getProjectId());
        Optional<Project> result = dialog.showAndWait();
        logger.info("Dialog closed, result present: {}", result.isPresent());
        
        if (result.isPresent()) {
            try {
                Project updatedProject = result.get();
                logger.info("Dialog returned project {} with travel={}", updatedProject.getProjectId(), updatedProject.isTravel());
                schedulingService.updateProject(updatedProject);
                logger.info("Updated project: {} with travel={}", updatedProject.getProjectId(), updatedProject.isTravel());
                updateProjectManagerFilter(); // Update filter in case PM changed
                refreshData();
                showInfoAlert("Success", "Project updated successfully.");
                
            } catch (Exception e) {
                logger.error("Error updating project", e);
                showErrorAlert("Error", "Failed to update project: " + e.getMessage());
            }
        }
    }
    
    private void manageOpenItems(Project project) {
        logger.info("Managing open items for project: {}", project.getProjectId());
        
        // Reload the project from database to get latest data
        Optional<Project> freshProject = schedulingService.getProjectById(project.getId());
        if (!freshProject.isPresent()) {
            showErrorAlert("Error", "Project no longer exists");
            return;
        }
        
        project = freshProject.get();
        
        // Use the shared DatabaseConfig instance
        
        // Show Open Items management dialog
        OpenItemsManagementDialog dialog = new OpenItemsManagementDialog(
            project, 
            databaseConfig,
            timelineView.getScene() != null ? timelineView.getScene().getWindow() : null
        );
        
        dialog.showAndWait();
        
        // Optionally refresh timeline if needed
        logger.info("Closed open items dialog for project: {}", project.getProjectId());
    }
    
    private void deleteProject(Project project) {
        if (ProjectDialog.showDeleteConfirmation(project)) {
            try {
                // Print database stats before deletion
                System.out.println("\n*** BEFORE PROJECT DELETE ***");
                DatabaseMonitor.printDatabaseStats();
                
                schedulingService.deleteProject(project.getId());
                logger.info("Deleted project: {}", project.getProjectId());
                
                // Print database stats after deletion
                System.out.println("\n*** AFTER PROJECT DELETE ***");
                DatabaseMonitor.printDatabaseStats();
                
                refreshData();
                // Visual feedback is sufficient - no need for success dialog
                
            } catch (Exception e) {
                logger.error("Error deleting project", e);
                showErrorAlert("Error", "Failed to delete project: " + e.getMessage());
            }
        }
    }
    
    private void deleteProjectWithAssignments(Project project) {
        // Validate project is not null and has an ID
        if (project == null || project.getId() == null) {
            logger.error("Cannot delete project: project or project ID is null");
            showErrorAlert("Error", "Invalid project data");
            return;
        }
        
        logger.info("Attempting to delete project: {} (ID: {})", project.getProjectId(), project.getId());
        
        // Count assignments first
        List<Assignment> projectAssignments = schedulingService.getAssignmentsByProjectId(project.getId());
        int assignmentCount = projectAssignments.size();
        
        // Create custom confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Project");
        confirm.setHeaderText("Delete project: " + project.getProjectId());
        
        String message;
        if (assignmentCount > 0) {
            message = String.format(
                "This project has %d assignment%s.\n\n" +
                "Project: %s\n" +
                "Description: %s\n\n" +
                "Deleting will remove:\n" +
                "• All %d resource assignment%s\n" +
                "• All associated tasks\n" +
                "• The project itself\n\n" +
                "This action cannot be undone. Continue?",
                assignmentCount,
                assignmentCount == 1 ? "" : "s",
                project.getProjectId(),
                project.getDescription() != null ? project.getDescription() : "No description",
                assignmentCount,
                assignmentCount == 1 ? "" : "s"
            );
        } else {
            message = String.format(
                "Are you sure you want to delete this project?\n\n" +
                "Project: %s\n" +
                "Description: %s\n\n" +
                "This action cannot be undone.",
                project.getProjectId(),
                project.getDescription() != null ? project.getDescription() : "No description"
            );
        }
        confirm.setContentText(message);
        
        // Set owner window
        Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
        if (owner != null) {
            confirm.initOwner(owner);
            confirm.initModality(Modality.WINDOW_MODAL);
        }
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Store the project and assignments for potential undo
                UndoManager undoManager = UndoManager.getInstance();
                String undoId = undoManager.storeDeletedProject(project, projectAssignments, null);
                
                // Print database stats before deletion
                System.out.println("\n*** BEFORE PROJECT DELETE WITH ASSIGNMENTS ***");
                DatabaseMonitor.printDatabaseStats();
                
                schedulingService.deleteProjectWithAssignments(project.getId());
                logger.info("Deleted project {} with {} assignments", project.getProjectId(), assignmentCount);
                
                // Print database stats after deletion
                System.out.println("\n*** AFTER PROJECT DELETE WITH ASSIGNMENTS ***");
                DatabaseMonitor.printDatabaseStats();
                
                refreshData();
                
                // Show toast notification with undo option
                Window window = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
                ToastNotification.getInstance().showDeleteNotification(
                    window,
                    "Project",
                    project.getProjectId(),
                    assignmentCount > 0 ? assignmentCount + " assignments removed" : null,
                    v -> undoProjectDeletion(undoId)
                );
                
            } catch (Exception e) {
                logger.error("Error deleting project with assignments", e);
                showErrorAlert("Error", "Failed to delete project with assignments: " + e.getMessage());
            }
        }
    }
    
    private void undoProjectDeletion(String undoId) {
        UndoManager undoManager = UndoManager.getInstance();
        UndoManager.UndoableAction action = undoManager.getUndoableAction(undoId);
        
        if (action instanceof UndoManager.ProjectDeletion) {
            UndoManager.ProjectDeletion deletion = (UndoManager.ProjectDeletion) action;
            
            try {
                // Recreate the project
                Project restoredProject = schedulingService.createProject(
                    deletion.project.getProjectId(),
                    deletion.project.getDescription(),
                    deletion.project.getStartDate(),
                    deletion.project.getEndDate()
                );
                
                // Copy all other fields
                restoredProject.setStatus(deletion.project.getStatus());
                restoredProject.setContactAddress(deletion.project.getContactAddress());
                restoredProject.setProjectManagerId(deletion.project.getProjectManagerId());
                // ... copy other fields as needed
                schedulingService.updateProject(restoredProject);
                
                // Recreate assignments
                for (Assignment assignment : deletion.assignments) {
                    schedulingService.createAssignment(
                        restoredProject.getId(),
                        assignment.getResourceId(),
                        assignment.getStartDate(),
                        assignment.getEndDate(),
                        assignment.getTravelOutDays(),
                        assignment.getTravelBackDays()
                    );
                }
                
                logger.info("Restored project {} with {} assignments", 
                    deletion.project.getProjectId(), deletion.assignments.size());
                
                undoManager.removeUndoAction(undoId);
                refreshData();
                
                // Show confirmation
                Window window = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
                ToastNotification.getInstance().showInfo(window, 
                    "Project restored", 
                    deletion.project.getProjectId() + " and " + deletion.assignments.size() + " assignments");
                
            } catch (Exception e) {
                logger.error("Failed to restore project", e);
                showErrorAlert("Error", "Failed to restore project: " + e.getMessage());
            }
        }
    }
    
    private void undoAssignmentDeletion(String undoId) {
        UndoManager undoManager = UndoManager.getInstance();
        UndoManager.UndoableAction action = undoManager.getUndoableAction(undoId);
        
        if (action instanceof UndoManager.AssignmentDeletion) {
            UndoManager.AssignmentDeletion deletion = (UndoManager.AssignmentDeletion) action;
            
            try {
                // Recreate the assignment
                schedulingService.createAssignment(
                    deletion.assignment.getProjectId(),
                    deletion.assignment.getResourceId(),
                    deletion.assignment.getStartDate(),
                    deletion.assignment.getEndDate(),
                    deletion.assignment.getTravelOutDays(),
                    deletion.assignment.getTravelBackDays()
                );
                
                logger.info("Restored assignment {}", deletion.assignment.getId());
                
                undoManager.removeUndoAction(undoId);
                refreshData();
                
                // Show confirmation
                Window window = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
                ToastNotification.getInstance().showInfo(window, "Assignment restored", null);
                
            } catch (Exception e) {
                logger.error("Failed to restore assignment", e);
                showErrorAlert("Error", "Failed to restore assignment: " + e.getMessage());
            }
        }
    }
    
    private void undoResourceDeletion(String undoId) {
        UndoManager undoManager = UndoManager.getInstance();
        UndoManager.UndoableAction action = undoManager.getUndoableAction(undoId);
        
        if (action instanceof UndoManager.ResourceDeletion) {
            UndoManager.ResourceDeletion deletion = (UndoManager.ResourceDeletion) action;
            
            try {
                // Recreate the resource
                Resource restoredResource = schedulingService.createResource(
                    deletion.resource.getName(),
                    deletion.resource.getEmail(),
                    deletion.resource.getResourceType()
                );
                
                // Copy all other fields
                restoredResource.setPhone(deletion.resource.getPhone());
                // The resource was already created with the correct fields
                
                logger.info("Restored resource {}", deletion.resource.getName());
                
                undoManager.removeUndoAction(undoId);
                refreshData();
                
                // Show confirmation
                Window window = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
                ToastNotification.getInstance().showInfo(window, "Resource restored", deletion.resource.getName());
                
            } catch (Exception e) {
                logger.error("Failed to restore resource", e);
                showErrorAlert("Error", "Failed to restore resource: " + e.getMessage());
            }
        }
    }
    
    private void generateClientReport(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/client-report-dialog.fxml"));
            DialogPane dialogPane = loader.load();
            
            ClientReportController controller = loader.getController();
            controller.setProject(project);
            
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Generate Client Report - " + project.getProjectId());
            dialog.setResizable(true);
            
            dialog.showAndWait();
            
        } catch (IOException e) {
            logger.error("Failed to open client report dialog", e);
            showErrorAlert("Error", "Failed to open report generator: " + e.getMessage());
        }
    }
    
    private void showProjectTasks(Project project) {
        try {
            if (project != null) {
                // Create TaskRepository
                TaskRepository taskRepository = new TaskRepository(schedulingService.getDataSource());
                
                // Get all resources for assignment
                List<Resource> resources = schedulingService.getAllResources();
                
                // Get assignments for the selected project
                List<Assignment> projectAssignments = schedulingService.getAssignmentsByProject(project.getId());
                
                // Show task list view with owner window for proper screen positioning
                javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
                TaskListView taskListView = new TaskListView(project, taskRepository, resources, projectAssignments, schedulingService, owner);
                taskListView.show();
            }
        } catch (Exception e) {
            logger.error("Error showing project tasks", e);
            showErrorAlert("Error", "Failed to open task management: " + e.getMessage());
        }
    }
    
    private void viewInReportCenter(Project project) {
        try {
            if (project != null) {
                // Open Report Center with the project preloaded
                javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
                ReportCenterView reportCenter = new ReportCenterView(schedulingService, project, owner);
                reportCenter.show();
            }
        } catch (Exception e) {
            logger.error("Error opening report center", e);
            showErrorAlert("Error", "Failed to open report center: " + e.getMessage());
        }
    }
    
    private void editResource(Resource resource) {
        ResourceDialog dialog = new ResourceDialog(resource);
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        Optional<Resource> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                // Note: We'd need an updateResource method in the service
                // For now, we'll just refresh to show the changes
                logger.info("Resource edit requested: {}", resource.getName());
                refreshData();
                showInfoAlert("Success", "Resource updated successfully.");
                
            } catch (Exception e) {
                logger.error("Error updating resource", e);
                showErrorAlert("Error", "Failed to update resource: " + e.getMessage());
            }
        }
    }
    
    private void markResourceUnavailable(Resource resource) {
        ResourceUnavailabilityDialog dialog = new ResourceUnavailabilityDialog(
            schedulingService, 
            schedulingService.getAllResources(),
            resource,
            null
        );
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        
        Optional<TechnicianUnavailability> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                TechnicianUnavailability unavailability = result.get();
                schedulingService.createUnavailability(
                    unavailability.getResourceId(),
                    unavailability.getType(),
                    unavailability.getStartDate(),
                    unavailability.getEndDate(),
                    unavailability.getReason()
                );
                
                showInfoAlert("Resource Unavailability", 
                    "Resource marked as unavailable from " + unavailability.getStartDate() + 
                    " to " + unavailability.getEndDate());
                
                refreshData();
            } catch (Exception e) {
                logger.error("Failed to create resource unavailability", e);
                showErrorAlert("Failed to mark resource as unavailable", e.getMessage());
            }
        }
    }
    
    private void viewResourceUnavailability(Resource resource) {
        ManageUnavailabilityDialog dialog = new ManageUnavailabilityDialog(schedulingService, resource);
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        dialog.showAndWait();
        
        // Refresh the timeline to show any changes
        refreshData();
    }
    
    private void deleteResource(Resource resource) {
        Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
        
        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Resource");
        confirm.setHeaderText("Delete resource: " + resource.getName());
        confirm.setContentText("This resource will be deleted. You can undo this action within 10 seconds.");
        
        if (owner != null) {
            confirm.initOwner(owner);
        }
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Store the resource for undo
                String undoId = UndoManager.getInstance().storeDeletedResource(resource);
                
                // Delete the resource
                schedulingService.deleteResource(resource.getId());
                logger.info("Deleted resource: {}", resource.getName());
                
                refreshData();
                
                // Show toast notification with undo option
                ToastNotification.getInstance().showDeleteNotification(
                    owner,
                    "Resource",
                    resource.getName(),
                    resource.getEmail() != null ? "Email: " + resource.getEmail() : null,
                    v -> undoResourceDeletion(undoId)
                );
                
            } catch (Exception e) {
                logger.error("Error deleting resource", e);
                showErrorAlert("Error", "Failed to delete resource: " + e.getMessage());
            }
        }
    }
    
    private void editAssignment(Assignment assignment) {
        List<Project> projects = schedulingService.getAllProjects();
        List<Resource> resources = schedulingService.getAllResources();
        
        AssignmentDialog dialog;
        boolean isNewAssignment = false;
        
        // Check if this is a dummy assignment from unassigned project context menu
        if (assignment.getId() == null && assignment.getProjectId() != null) {
            // This is for creating a new assignment with pre-selected project
            isNewAssignment = true;
            dialog = new AssignmentDialog(assignment, projects, resources, schedulingService);
            
            // Find the project to show its ID in the title
            Project preselectedProject = projects.stream()
                .filter(p -> p.getId().equals(assignment.getProjectId()))
                .findFirst()
                .orElse(null);
            
            if (preselectedProject != null) {
                dialog.setTitle("Create Assignment for " + preselectedProject.getProjectId());
            }
        } else {
            // Regular edit of existing assignment
            dialog = new AssignmentDialog(assignment, projects, resources, schedulingService);
        }
        
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        Optional<Assignment> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                Assignment resultAssignment = result.get();
                
                // Check if this is a new assignment (from unassigned project) or an update
                if (isNewAssignment) {
                    // This is a new assignment from an unassigned project
                    // Make sure resource ID is set
                    if (resultAssignment.getResourceId() == null) {
                        showErrorAlert("Error", "Please select a resource for the assignment.");
                        return;
                    }
                    
                    Assignment createdAssignment;
                    if (resultAssignment.isOverride()) {
                        createdAssignment = schedulingService.createAssignmentWithOverride(
                            resultAssignment.getProjectId(),
                            resultAssignment.getResourceId(),
                            resultAssignment.getStartDate(),
                            resultAssignment.getEndDate(),
                            resultAssignment.getTravelOutDays(),
                            resultAssignment.getTravelBackDays(),
                            resultAssignment.getOverrideReason()
                        );
                    } else {
                        createdAssignment = schedulingService.createAssignment(
                            resultAssignment.getProjectId(),
                            resultAssignment.getResourceId(),
                            resultAssignment.getStartDate(),
                            resultAssignment.getEndDate(),
                            resultAssignment.getTravelOutDays(),
                            resultAssignment.getTravelBackDays()
                        );
                    }
                    
                    logger.info("Created new assignment for project ID: {}", resultAssignment.getProjectId());
                    refreshData();
                    showInfoAlert("Success", "Assignment created successfully.");
                } else {
                    // This is an update to an existing assignment
                    schedulingService.updateAssignment(resultAssignment);
                    logger.info("Updated assignment: {}", assignment.getId());
                    refreshData();
                    showInfoAlert("Success", "Assignment updated successfully.");
                }
                
            } catch (Exception e) {
                logger.error("Error processing assignment", e);
                showErrorAlert("Error", "Failed to process assignment: " + e.getMessage());
            }
        }
    }
    
    private void deleteAssignment(Assignment assignment) {
        // Find project and resource for confirmation dialog
        Project project = schedulingService.getProjectById(assignment.getProjectId()).orElse(null);
        Resource resource = schedulingService.getResourceById(assignment.getResourceId()).orElse(null);
        
        // Get the owner window from timeline
        Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
        
        if (AssignmentDialog.showDeleteConfirmation(assignment, project, resource, owner)) {
            try {
                // Store the assignment for potential undo
                UndoManager undoManager = UndoManager.getInstance();
                String undoId = undoManager.storeDeletedAssignment(assignment);
                
                // Print database stats before deletion
                System.out.println("\n*** BEFORE ASSIGNMENT DELETE ***");
                DatabaseMonitor.printDatabaseStats();
                
                schedulingService.deleteAssignment(assignment.getId());
                logger.info("Deleted assignment: {}", assignment.getId());
                
                // Print database stats after deletion
                System.out.println("\n*** AFTER ASSIGNMENT DELETE ***");
                DatabaseMonitor.printDatabaseStats();
                
                refreshData();
                
                // Show toast notification with undo option
                String description = String.format("%s → %s", 
                    project != null ? project.getProjectId() : "Unknown",
                    resource != null ? resource.getName() : "Unknown");
                ToastNotification.getInstance().showDeleteNotification(
                    owner,
                    "Assignment",
                    description,
                    null,
                    v -> undoAssignmentDeletion(undoId)
                );
                
            } catch (Exception e) {
                logger.error("Error deleting assignment", e);
                showErrorAlert("Error", "Failed to delete assignment: " + e.getMessage());
            }
        }
    }
    
    private void duplicateAssignment(Assignment assignment) {
        // Get lists for the dialog
        List<Project> projects = schedulingService.getAllProjects();
        List<Resource> resources = schedulingService.getAllResources();
        
        // Create a copy of the assignment with a new ID
        Assignment duplicated = new Assignment();
        duplicated.setProjectId(assignment.getProjectId());
        duplicated.setResourceId(assignment.getResourceId());
        duplicated.setStartDate(assignment.getStartDate());
        duplicated.setEndDate(assignment.getEndDate());
        duplicated.setTravelOutDays(assignment.getTravelOutDays());
        duplicated.setTravelBackDays(assignment.getTravelBackDays());
        duplicated.setNotes(assignment.getNotes() != null ? "Copy of: " + assignment.getNotes() : "Duplicated assignment");
        
        // Open dialog with pre-filled duplicate data
        AssignmentDialog dialog = new AssignmentDialog(duplicated, projects, resources, schedulingService);
        dialog.setTitle("Duplicate Assignment");
        dialog.setHeaderText("Modify the duplicated assignment details");
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        Optional<Assignment> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                Assignment newAssignment = result.get();
                Assignment created;
                
                if (newAssignment.isOverride()) {
                    created = schedulingService.createAssignmentWithOverride(
                        newAssignment.getProjectId(),
                        newAssignment.getResourceId(),
                        newAssignment.getStartDate(),
                        newAssignment.getEndDate(),
                        newAssignment.getTravelOutDays(),
                        newAssignment.getTravelBackDays(),
                        newAssignment.getOverrideReason()
                    );
                } else {
                    created = schedulingService.createAssignment(
                        newAssignment.getProjectId(),
                        newAssignment.getResourceId(),
                        newAssignment.getStartDate(),
                        newAssignment.getEndDate(),
                        newAssignment.getTravelOutDays(),
                        newAssignment.getTravelBackDays()
                    );
                }
                
                logger.info("Duplicated assignment: {} to new assignment: {}", assignment.getId(), created.getId());
                refreshData();
                showInfoAlert("Success", "Assignment duplicated successfully.");
                
            } catch (Exception e) {
                logger.error("Error duplicating assignment", e);
                showErrorAlert("Error", "Failed to duplicate assignment: " + e.getMessage());
            }
        }
    }
    
    // New feature management methods
    private void manageHolidayCalendar() {
        HolidayDialog dialog = new HolidayDialog();
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        Optional<CompanyHoliday> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                CompanyHoliday holiday = result.get();
                // TODO: Add service method to save holiday
                logger.info("Created new holiday: {} on {}", holiday.getName(), holiday.getDate());
                
                showInfoAlert("Success", "Company holiday created successfully.\n\n" +
                    "Note: Database integration pending - this will be saved once the holiday tables are created.");
                
            } catch (Exception e) {
                logger.error("Error creating holiday", e);
                showErrorAlert("Error", "Failed to create holiday: " + e.getMessage());
            }
        }
    }
    
    private void showHolidayCalendarView() {
        try {
            // Create the holiday calendar view
            HolidayCalendarView holidayView = new HolidayCalendarView();
            
            // Set up callbacks
            holidayView.setOnAddHoliday(() -> {
                HolidayDialog dialog = new HolidayDialog();
                DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
                Optional<CompanyHoliday> result = dialog.showAndWait();
                if (result.isPresent()) {
                    CompanyHoliday holiday = result.get();
                    // Save to database
                    if (saveHolidayToDatabase(holiday)) {
                        holidayView.getHolidays().add(holiday);
                        logger.info("Added holiday: {}", holiday.getName());
                        showInfoAlert("Success", "Holiday added successfully.");
                        refreshData(); // Refresh timeline to show new holiday
                    } else {
                        showErrorAlert("Error", "Failed to save holiday to database.");
                    }
                }
            });
            
            holidayView.setOnEditHoliday(holiday -> {
                HolidayDialog dialog = new HolidayDialog(holiday);
                DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
                Optional<CompanyHoliday> result = dialog.showAndWait();
                if (result.isPresent()) {
                    CompanyHoliday updated = result.get();
                    updated.setId(holiday.getId()); // Keep the original ID
                    // Update in database
                    if (updateHolidayInDatabase(updated)) {
                        int index = holidayView.getHolidays().indexOf(holiday);
                        if (index >= 0) {
                            holidayView.getHolidays().set(index, updated);
                        }
                        logger.info("Updated holiday: {}", updated.getName());
                        showInfoAlert("Success", "Holiday updated successfully.");
                        refreshData(); // Refresh timeline
                    } else {
                        showErrorAlert("Error", "Failed to update holiday in database.");
                    }
                }
            });
            
            holidayView.setOnDeleteHoliday(holiday -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Holiday");
                confirm.setHeaderText("Delete holiday: " + holiday.getName());
                confirm.setContentText("Are you sure you want to delete this holiday?");
                DialogUtils.initializeDialog(confirm, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
                
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    // Delete from database
                    if (deleteHolidayFromDatabase(holiday)) {
                        holidayView.getHolidays().remove(holiday);
                        logger.info("Deleted holiday: {}", holiday.getName());
                        showInfoAlert("Success", "Holiday deleted successfully.");
                        refreshData(); // Refresh timeline
                    } else {
                        showErrorAlert("Error", "Failed to delete holiday from database.");
                    }
                }
            });
            
            // Load holidays from database
            List<CompanyHoliday> holidays = loadAllCompanyHolidaysFromDatabase();
            holidayView.setHolidays(holidays);
            
            // Create and show window
            Stage stage = new Stage();
            stage.setTitle("Holiday Calendar");
            stage.initModality(Modality.NONE);
            
            // Set owner window for proper positioning
            Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            if (owner != null) {
                stage.initOwner(owner);
                // Position relative to owner
                stage.setX(owner.getX() + (owner.getWidth() - 1200) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - 800) / 2);
            }
            
            stage.setScene(new Scene(holidayView, 1200, 800));
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            stage.show();
            
        } catch (Exception e) {
            logger.error("Error showing holiday calendar view", e);
            showErrorAlert("Error", "Failed to show holiday calendar: " + e.getMessage());
        }
    }
    
    private void showProjectGridView() {
        try {
            // Get list of projects for filtering
            List<Project> allProjects = schedulingService.getAllProjects();
            if (allProjects.isEmpty()) {
                showInfoAlert("No Projects", "There are no projects to edit. Please create a project first.");
                return;
            }
            
            // Show project filter dialog
            Alert filterChoice = new Alert(Alert.AlertType.CONFIRMATION);
            filterChoice.setTitle("Project Grid Editor");
            filterChoice.setHeaderText("Select projects to edit in grid view");
            filterChoice.setContentText("Would you like to filter projects or edit all projects?");
            
            ButtonType filterButton = new ButtonType("Filter Projects");
            ButtonType allButton = new ButtonType("Edit All Projects");
            ButtonType cancelButton = ButtonType.CANCEL;
            
            filterChoice.getButtonTypes().setAll(filterButton, allButton, cancelButton);
            
            // Initialize dialog position
            Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            if (owner != null) {
                Stage dialogStage = (Stage) filterChoice.getDialogPane().getScene().getWindow();
                dialogStage.initOwner(owner);
            }
            
            Optional<ButtonType> result = filterChoice.showAndWait();
            
            if (result.isPresent()) {
                if (result.get() == filterButton) {
                    // Show project selection dialog with filtering
                    ProjectSelectionDialog selectionDialog = new ProjectSelectionDialog(allProjects,
                        "Filter Projects for Grid Editor",
                        "Select projects to edit (use filters to narrow down)");
                    
                    // Configure for multi-selection
                    configureMultiSelection(selectionDialog, allProjects);
                    DialogUtils.initializeDialog(selectionDialog, owner);
                    
                    Optional<Project> selectionResult = selectionDialog.showAndWait();
                    if (selectionResult.isPresent()) {
                        // Get filtered projects from dialog
                        List<Project> filteredProjects = getFilteredProjectsFromDialog(selectionDialog);
                        ProjectGridView gridView = new ProjectGridView(schedulingService, owner, filteredProjects);
                        gridView.setOnProjectsChanged(this::refreshData);
                        gridView.show();
                    }
                } else if (result.get() == allButton) {
                    // Open with all projects
                    ProjectGridView gridView = new ProjectGridView(schedulingService, owner);
                    gridView.setOnProjectsChanged(this::refreshData);
                    gridView.show();
                }
            }
        } catch (Exception e) {
            logger.error("Error showing project grid view", e);
            showErrorAlert("Error", "Failed to open project grid editor: " + e.getMessage());
        }
    }
    
    private void showResourceGridView() {
        try {
            ResourceGridView gridView = new ResourceGridView(schedulingService, timelineView.getScene().getWindow());
            gridView.show();
        } catch (Exception e) {
            logger.error("Error showing resource grid view", e);
            showErrorAlert("Error", "Failed to open resource grid editor: " + e.getMessage());
        }
    }
    
    private void showImportExcelDialog() {
        try {
            com.subliminalsearch.simpleprojectresourcemanager.dialog.ImportExcelDialog dialog = 
                new com.subliminalsearch.simpleprojectresourcemanager.dialog.ImportExcelDialog(
                    schedulingService, timelineView.getScene().getWindow());
            dialog.showAndWait();
            
            // Refresh data after import
            refreshData();
        } catch (Exception e) {
            logger.error("Error showing import dialog", e);
            showErrorAlert("Error", "Failed to open import dialog: " + e.getMessage());
        }
    }
    
    private void showBatchDeleteDialog() {
        try {
            com.subliminalsearch.simpleprojectresourcemanager.dialog.BatchDeleteProjectsDialog dialog = 
                new com.subliminalsearch.simpleprojectresourcemanager.dialog.BatchDeleteProjectsDialog(
                    schedulingService, timelineView.getScene().getWindow());
            dialog.showAndWait();
            
            // Refresh data after deletion
            refreshData();
        } catch (Exception e) {
            logger.error("Error showing batch delete dialog", e);
            showErrorAlert("Error", "Failed to open batch delete dialog: " + e.getMessage());
        }
    }
    
    private void configureMultiSelection(ProjectSelectionDialog dialog, List<Project> allProjects) {
        // This would require modifying ProjectSelectionDialog to support multi-selection
        // For now, we'll use the filtered list from the dialog
    }
    
    private List<Project> getFilteredProjectsFromDialog(ProjectSelectionDialog dialog) {
        // Get the filtered projects from the dialog's combo box items
        try {
            // Use reflection to access the filtered projects list
            java.lang.reflect.Field field = ProjectSelectionDialog.class.getDeclaredField("filteredProjects");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ObservableList<Project> filteredList = (ObservableList<Project>) field.get(dialog);
            return new ArrayList<>(filteredList);
        } catch (Exception e) {
            logger.error("Error getting filtered projects", e);
            return new ArrayList<>();
        }
    }
    
    private void showProjectTasks() {
        try {
            // Get list of projects for selection
            List<Project> projects = schedulingService.getAllProjects();
            if (projects.isEmpty()) {
                showInfoAlert("No Projects", "Please create a project first.");
                return;
            }
            
            // Create custom project selection dialog with filters
            ProjectSelectionDialog dialog = new ProjectSelectionDialog(projects, 
                "Select Project for Tasks", 
                "Select a project to manage tasks");
            DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
            
            Optional<Project> result = dialog.showAndWait();
            if (result.isPresent()) {
                Project selectedProject = result.get();
                
                // Create TaskRepository (we need to add this to the service)
                TaskRepository taskRepository = new TaskRepository(schedulingService.getDataSource());
                
                // Get all resources for assignment
                List<Resource> resources = schedulingService.getAllResources();
                
                // Get assignments for this project to show assigned resources
                List<Assignment> projectAssignments = schedulingService.getAssignmentsByProject(selectedProject.getId());
                
                // Show task list view
                javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
                TaskListView taskListView = new TaskListView(selectedProject, taskRepository, resources, projectAssignments, schedulingService, owner);
                taskListView.show();
            }
        } catch (Exception e) {
            logger.error("Error showing project tasks", e);
            showErrorAlert("Error", "Failed to open task management: " + e.getMessage());
        }
    }
    
    private void showResourceAvailabilityView() {
        ResourceAvailabilityDialog dialog = new ResourceAvailabilityDialog(schedulingService);
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        dialog.showAndWait();
        
        // Refresh timeline to show any changes
        refreshData();
    }
    
    @FXML
    private void openUtilizationSettings() {
        try {
            // Load current settings from service
            UtilizationSettings currentSettings = utilizationService.getSettings();
            
            // Create and show the dialog
            Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            UtilizationSettingsDialog dialog = new UtilizationSettingsDialog(currentSettings, owner);
            
            Optional<UtilizationSettings> result = dialog.showAndWait();
            if (result.isPresent()) {
                UtilizationSettings settings = result.get();
                // Save settings to database
                utilizationService.saveSettings(settings);
                // Update the timeline view
                timelineView.setUtilizationSettings(settings);
                refreshData(); // Refresh to apply new settings
                showInfoAlert("Success", "Utilization settings updated and saved successfully.");
            }
        } catch (Exception e) {
            logger.error("Error opening utilization settings", e);
            showErrorAlert("Error", "Failed to open utilization settings: " + e.getMessage());
        }
    }
    
    private void showCertificationManagement() {
        try {
            Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            com.subliminalsearch.simpleprojectresourcemanager.view.CertificationManagementView view = 
                new com.subliminalsearch.simpleprojectresourcemanager.view.CertificationManagementView(owner);
            view.show();
        } catch (Exception e) {
            logger.error("Error showing certification management", e);
            showErrorAlert("Error", "Failed to open certification management: " + e.getMessage());
        }
    }
    
    private void showSkillsManagement() {
        try {
            Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            com.subliminalsearch.simpleprojectresourcemanager.view.SkillsManagementView view = 
                new com.subliminalsearch.simpleprojectresourcemanager.view.SkillsManagementView(owner);
            view.show();
        } catch (Exception e) {
            logger.error("Error showing skills management", e);
            showErrorAlert("Error", "Failed to open skills management: " + e.getMessage());
        }
    }
    
    private void showResourceQualifications() {
        try {
            Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            com.subliminalsearch.simpleprojectresourcemanager.view.ResourceQualificationsView view = 
                new com.subliminalsearch.simpleprojectresourcemanager.view.ResourceQualificationsView(owner, schedulingService);
            view.show();
        } catch (Exception e) {
            logger.error("Error showing resource qualifications", e);
            showErrorAlert("Error", "Failed to open resource qualifications: " + e.getMessage());
        }
    }
    
    private void showSkillCategoryManagement() {
        try {
            Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            com.subliminalsearch.simpleprojectresourcemanager.view.SkillCategoryManagementView view = 
                new com.subliminalsearch.simpleprojectresourcemanager.view.SkillCategoryManagementView(owner);
            view.show();
        } catch (Exception e) {
            logger.error("Error showing skill category management", e);
            showErrorAlert("Error", "Failed to open skill category management: " + e.getMessage());
        }
    }
    
    private boolean saveHolidayToDatabase(CompanyHoliday holiday) {
        try {
            String sql = "INSERT INTO company_holidays (name, date, type, description, working_holiday_allowed, active) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            
            try (java.sql.Connection conn = schedulingService.getDataSource().getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                
                stmt.setString(1, holiday.getName());
                stmt.setString(2, holiday.getDate().toString());
                stmt.setString(3, holiday.getType().name());
                stmt.setString(4, holiday.getDescription() != null ? holiday.getDescription() : "");
                stmt.setBoolean(5, holiday.isWorkingHolidayAllowed());
                stmt.setBoolean(6, holiday.isActive());
                
                int rows = stmt.executeUpdate();
                
                if (rows > 0) {
                    // Get the generated ID
                    try (java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long newId = generatedKeys.getLong(1);
                            holiday.setId(newId);
                            logger.info("Holiday saved with ID {}: {} on {}", newId, holiday.getName(), holiday.getDate());
                        }
                    }
                    logger.info("Successfully saved holiday to database: {} on {}", holiday.getName(), holiday.getDate());
                    
                    // Verify it was saved
                    String verifySql = "SELECT COUNT(*) FROM company_holidays WHERE id = ?";
                    try (java.sql.PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                        verifyStmt.setLong(1, holiday.getId());
                        try (java.sql.ResultSet rs = verifyStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                logger.info("Verified holiday exists in database with ID {}", holiday.getId());
                            } else {
                                logger.error("Holiday was not found after saving!");
                            }
                        }
                    }
                    
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to save holiday to database", e);
        }
        return false;
    }
    
    private boolean updateHolidayInDatabase(CompanyHoliday holiday) {
        try {
            String sql = "UPDATE company_holidays SET name = ?, date = ?, type = ?, description = ?, " +
                        "working_holiday_allowed = ?, active = ? WHERE id = ?";
            
            try (java.sql.Connection conn = schedulingService.getDataSource().getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, holiday.getName());
                stmt.setString(2, holiday.getDate().toString());
                stmt.setString(3, holiday.getType().name());
                stmt.setString(4, holiday.getDescription() != null ? holiday.getDescription() : "");
                stmt.setBoolean(5, holiday.isWorkingHolidayAllowed());
                stmt.setBoolean(6, holiday.isActive());
                stmt.setLong(7, holiday.getId());
                
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    logger.info("Updated holiday in database: {} on {}", holiday.getName(), holiday.getDate());
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to update holiday in database", e);
        }
        return false;
    }
    
    private boolean deleteHolidayFromDatabase(CompanyHoliday holiday) {
        try {
            String sql = "DELETE FROM company_holidays WHERE id = ?";
            
            try (java.sql.Connection conn = schedulingService.getDataSource().getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(1, holiday.getId());
                
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    logger.info("Deleted holiday from database: {} on {}", holiday.getName(), holiday.getDate());
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to delete holiday from database", e);
        }
        return false;
    }
    
    private void createCompanyHolidaysTable(java.sql.Connection conn) throws java.sql.SQLException {
        String createTable = "CREATE TABLE IF NOT EXISTS company_holidays (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "date TEXT NOT NULL, " +
            "type TEXT, " +
            "description TEXT, " +
            "working_holiday_allowed BOOLEAN DEFAULT 0, " +
            "active BOOLEAN DEFAULT 1, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            logger.info("Created company_holidays table");
        }
    }
    
    private List<CompanyHoliday> loadAllCompanyHolidaysFromDatabase() {
        List<CompanyHoliday> holidays = new ArrayList<>();
        try {
            // First check if table exists
            try (java.sql.Connection conn = schedulingService.getDataSource().getConnection()) {
                java.sql.DatabaseMetaData meta = conn.getMetaData();
                java.sql.ResultSet tables = meta.getTables(null, null, "company_holidays", null);
                if (!tables.next()) {
                    logger.warn("Table 'company_holidays' does not exist! Creating it now...");
                    createCompanyHolidaysTable(conn);
                } else {
                    logger.debug("Table 'company_holidays' exists");
                }
            }
            
            String sql = "SELECT id, name, date, type, description, working_holiday_allowed, active " +
                        "FROM company_holidays WHERE active = 1 ORDER BY date";
            
            logger.debug("Loading all holidays from database with SQL: {}", sql);
            
            try (java.sql.Connection conn = schedulingService.getDataSource().getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
                 java.sql.ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String name = rs.getString("name");
                    String dateStr = rs.getString("date");
                    LocalDate date = LocalDate.parse(dateStr);
                    String typeStr = rs.getString("type");
                    HolidayType type = typeStr != null ? HolidayType.valueOf(typeStr) : HolidayType.COMPANY;
                    
                    logger.debug("Loading holiday: id={}, name={}, date={}, type={}", id, name, dateStr, typeStr);
                    
                    CompanyHoliday holiday = new CompanyHoliday(name, date, type);
                    holiday.setId(id);
                    String description = rs.getString("description");
                    if (description != null) {
                        holiday.setDescription(description);
                    }
                    holiday.setWorkingHolidayAllowed(rs.getBoolean("working_holiday_allowed"));
                    holiday.setActive(rs.getBoolean("active"));
                    
                    holidays.add(holiday);
                    logger.debug("Added holiday to list: {}", holiday.getName());
                }
            }
            
            logger.info("Loaded {} holidays from database", holidays.size());
            
        } catch (Exception e) {
            logger.error("Failed to load all holidays from database", e);
        }
        
        return holidays;
    }
    
    private List<CompanyHoliday> loadCompanyHolidaysFromDatabase(LocalDate startDate, LocalDate endDate) {
        List<CompanyHoliday> holidays = new ArrayList<>();
        try {
            String sql = "SELECT id, name, date, type, description, working_holiday_allowed, active " +
                        "FROM company_holidays WHERE active = 1 AND date BETWEEN ? AND ? ORDER BY date";
            
            try (java.sql.Connection conn = schedulingService.getDataSource().getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, startDate.toString());
                stmt.setString(2, endDate.toString());
                
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("name");
                        LocalDate date = LocalDate.parse(rs.getString("date"));
                        String typeStr = rs.getString("type");
                        HolidayType type = typeStr != null ? HolidayType.valueOf(typeStr) : HolidayType.COMPANY;
                        
                        CompanyHoliday holiday = new CompanyHoliday(name, date, type);
                        holiday.setId(rs.getLong("id"));
                        String description = rs.getString("description");
                        if (description != null) {
                            holiday.setDescription(description);
                        }
                        holiday.setWorkingHolidayAllowed(rs.getBoolean("working_holiday_allowed"));
                        holiday.setActive(rs.getBoolean("active"));
                        
                        holidays.add(holiday);
                    }
                }
            }
            
            logger.info("Loaded {} holidays from database for date range {} to {}", holidays.size(), startDate, endDate);
            
        } catch (Exception e) {
            logger.error("Failed to load holidays from database", e);
            // Fall back to sample holidays if database fails
            holidays = createSampleHolidays().stream()
                .filter(h -> !h.getDate().isBefore(startDate) && !h.getDate().isAfter(endDate))
                .collect(java.util.stream.Collectors.toList());
        }
        
        return holidays;
    }
    
    private List<CompanyHoliday> createSampleHolidays() {
        List<CompanyHoliday> holidays = new ArrayList<>();
        
        // Current year holidays
        int year = LocalDate.now().getYear();
        
        CompanyHoliday h1 = new CompanyHoliday("New Year's Day", LocalDate.of(year, 1, 1), HolidayType.COMPANY);
        h1.setDescription("Company closed");
        h1.setWorkingHolidayAllowed(false);
        holidays.add(h1);
        
        CompanyHoliday h2 = new CompanyHoliday("MLK Day", LocalDate.of(year, 1, 15), HolidayType.FEDERAL);
        h2.setDescription("Federal holiday");
        h2.setWorkingHolidayAllowed(false);
        holidays.add(h2);
        
        CompanyHoliday h3 = new CompanyHoliday("Presidents Day", LocalDate.of(year, 2, 19), HolidayType.FEDERAL);
        h3.setDescription("Federal holiday");
        h3.setWorkingHolidayAllowed(false);
        holidays.add(h3);
        
        CompanyHoliday h4 = new CompanyHoliday("Memorial Day", LocalDate.of(year, 5, 27), HolidayType.FEDERAL);
        h4.setDescription("Federal holiday");
        h4.setWorkingHolidayAllowed(false);
        holidays.add(h4);
        
        CompanyHoliday h5 = new CompanyHoliday("Independence Day", LocalDate.of(year, 7, 4), HolidayType.COMPANY);
        h5.setDescription("Company closed");
        h5.setWorkingHolidayAllowed(false);
        holidays.add(h5);
        
        CompanyHoliday h6 = new CompanyHoliday("Labor Day", LocalDate.of(year, 9, 2), HolidayType.FEDERAL);
        h6.setDescription("Federal holiday");
        h6.setWorkingHolidayAllowed(false);
        holidays.add(h6);
        
        CompanyHoliday h7 = new CompanyHoliday("Thanksgiving", LocalDate.of(year, 11, 28), HolidayType.COMPANY);
        h7.setDescription("Company closed");
        h7.setWorkingHolidayAllowed(false);
        holidays.add(h7);
        
        CompanyHoliday h8 = new CompanyHoliday("Black Friday", LocalDate.of(year, 11, 29), HolidayType.FLOATING);
        h8.setDescription("Optional work day");
        h8.setWorkingHolidayAllowed(true);
        holidays.add(h8);
        
        CompanyHoliday h9 = new CompanyHoliday("Christmas Eve", LocalDate.of(year, 12, 24), HolidayType.COMPANY);
        h9.setDescription("Half day");
        h9.setWorkingHolidayAllowed(false);
        holidays.add(h9);
        
        CompanyHoliday h10 = new CompanyHoliday("Christmas Day", LocalDate.of(year, 12, 25), HolidayType.COMPANY);
        h10.setDescription("Company closed");
        h10.setWorkingHolidayAllowed(false);
        holidays.add(h10);
        
        CompanyHoliday h11 = new CompanyHoliday("New Year's Eve", LocalDate.of(year, 12, 31), HolidayType.COMPANY);
        h11.setDescription("Half day");
        h11.setWorkingHolidayAllowed(false);
        holidays.add(h11);
        
        return holidays;
    }
    
    private List<TechnicianUnavailability> createSampleUnavailabilities() {
        List<TechnicianUnavailability> unavailabilities = new ArrayList<>();
        List<Resource> resources = schedulingService.getAllResources();
        
        if (!resources.isEmpty()) {
            // Add some sample unavailabilities
            LocalDate today = LocalDate.now();
            
            if (resources.size() > 0) {
                TechnicianUnavailability u1 = new TechnicianUnavailability(
                    resources.get(0).getId(),
                    UnavailabilityType.VACATION,
                    today.plusDays(5),
                    today.plusDays(7)
                );
                u1.setReason("Annual vacation");
                unavailabilities.add(u1);
            }
            
            if (resources.size() > 1) {
                TechnicianUnavailability u2 = new TechnicianUnavailability(
                    resources.get(1).getId(),
                    UnavailabilityType.SICK_LEAVE,
                    today.plusDays(10),
                    today.plusDays(10)
                );
                u2.setReason("Medical appointment");
                unavailabilities.add(u2);
            }
            
            if (resources.size() > 2) {
                TechnicianUnavailability u3 = new TechnicianUnavailability(
                    resources.get(2).getId(),
                    UnavailabilityType.TRAINING,
                    today.plusDays(15),
                    today.plusDays(19)
                );
                u3.setReason("Certification training");
                unavailabilities.add(u3);
            }
        }
        
        return unavailabilities;
    }

    // Data Methods
    private void refreshData() {
        try {
            // Don't update filters here - they should be updated separately to avoid circular calls
            
            // Get date range based on view mode
            LocalDate startDate;
            LocalDate endDate;
            
            String viewMode = viewModeCombo.getValue();
            switch (viewMode) {
                case "Week":
                    // Show the week containing the current display date
                    startDate = currentDisplayDate.minusDays(currentDisplayDate.getDayOfWeek().getValue() - 1);
                    endDate = startDate.plusDays(6);
                    break;
                case "Month":
                    // Show 6 weeks (42 days) starting from the beginning of the month
                    // This provides visibility into the next month for assignments that span boundaries
                    startDate = currentDisplayDate.withDayOfMonth(1);
                    // Always show 42 days (6 weeks) to maintain consistent grid size
                    endDate = startDate.plusDays(41);  // 42 days total (6 weeks)
                    break;
                case "Quarter":
                    // Show the quarter containing the current display date
                    int quarter = (currentDisplayDate.getMonthValue() - 1) / 3;
                    startDate = currentDisplayDate.withMonth(quarter * 3 + 1).withDayOfMonth(1);
                    endDate = startDate.plusMonths(3).minusDays(1);
                    break;
                default:
                    // Default to month view
                    startDate = currentDisplayDate.withDayOfMonth(1);
                    endDate = startDate.plusMonths(1).minusDays(1);
                    break;
            }
            
            // Update timeline date range
            timelineView.setDateRange(startDate, endDate);
            
            // Get all data for the timeline
            // Get ALL projects (not just those in date range) so we have project info for all assignments
            List<Project> allProjects = schedulingService.getAllProjects();
            List<Project> projects = schedulingService.getProjectsByDateRange(startDate, endDate);
            List<Resource> resources = schedulingService.getAllResources();
            // Get assignments by date range - these should be shown regardless of project dates
            List<Assignment> assignments = schedulingService.getAssignmentsByDateRange(startDate, endDate);
            
            logger.info("Data loaded - Projects: {}, Resources: {}, Assignments: {} for dates {} to {}", 
                projects.size(), resources.size(), assignments.size(), startDate, endDate);
            
            // Apply filters
            // Filter by Project Manager
            String selectedManager = projectManagerFilter.getValue();
            if (selectedManager != null && !selectedManager.equals("All Managers")) {
                // Get the selected project manager
                ProjectManager selectedPM = schedulingService.getAllProjectManagers().stream()
                    .filter(pm -> pm.getName().equals(selectedManager))
                    .findFirst()
                    .orElse(null);
                
                if (selectedPM != null) {
                    projects = projects.stream()
                        .filter(p -> p.getProjectManagerId() != null && 
                                    p.getProjectManagerId().equals(selectedPM.getId()))
                        .toList();
                } else {
                    // Handle unassigned case
                    projects = projects.stream()
                        .filter(p -> p.getProjectManagerId() == null)
                        .toList();
                }
            }
            
            // Filter by specific project
            String selectedProject = projectFilter.getValue();
            if (selectedProject != null && !selectedProject.equals("All Projects")) {
                projects = projects.stream()
                    .filter(p -> p.getProjectId().equals(selectedProject))
                    .toList();
            }
            
            // Filter by status
            ProjectStatus statusFilterValue = statusFilter.getValue();
            if (statusFilterValue != null) {
                projects = projects.stream()
                    .filter(p -> p.getStatus() == statusFilterValue)
                    .toList();
            }
            
            // Filter assignments based on filters, but include ALL assignments in date range
            // Only filter by project if a specific project or manager filter is applied
            boolean hasProjectFilter = selectedProject != null && !selectedProject.equals("All Projects");
            boolean hasManagerFilter = selectedManager != null && !selectedManager.equals("All Managers");
            boolean hasStatusFilter = statusFilterValue != null;
            
            if (hasProjectFilter || hasManagerFilter || hasStatusFilter) {
                // Only filter assignments if specific project filters are applied
                List<Long> filteredProjectIds = projects.stream().map(Project::getId).toList();
                logger.info("Filtering assignments by project IDs: {}", filteredProjectIds);
                int originalAssignmentCount = assignments.size();
                assignments = assignments.stream()
                    .filter(a -> filteredProjectIds.contains(a.getProjectId()))
                    .toList();
                logger.info("Filtered assignments from {} to {} based on project filters", originalAssignmentCount, assignments.size());
            } else {
                // No project filters applied - show ALL assignments in date range
                // But we need all projects for display purposes
                projects = allProjects;
                logger.info("No project filters applied - showing all {} assignments in date range", assignments.size());
            }
            
            // Filter resources to only show those with assignments in filtered projects
            String selectedResource = resourceFilter.getValue();
            if (selectedResource != null && !selectedResource.equals("All Resources")) {
                resources = resources.stream()
                    .filter(r -> r.getName().equals(selectedResource))
                    .toList();
                
                // Further filter assignments by selected resource
                List<Long> resourceIds = resources.stream().map(Resource::getId).toList();
                assignments = assignments.stream()
                    .filter(a -> resourceIds.contains(a.getResourceId()))
                    .toList();
            } else {
                // Check if we should show all resources or only those with assignments
                if (!menuShowAllResources.isSelected()) {
                    // Show only resources that have assignments in the filtered projects
                    Set<Long> resourceIdsWithAssignments = assignments.stream()
                        .map(Assignment::getResourceId)
                        .collect(java.util.stream.Collectors.toSet());
                    logger.info("Resource IDs with assignments: {}", resourceIdsWithAssignments);
                    int originalResourceCount = resources.size();
                    resources = resources.stream()
                        .filter(r -> resourceIdsWithAssignments.contains(r.getId()))
                        .toList();
                    logger.info("Filtered resources from {} to {} based on assignments", originalResourceCount, resources.size());
                } else {
                    // Show all active resources (including internal full-time employees without assignments)
                    resources = resources.stream()
                        .filter(Resource::isActive)
                        .toList();
                    logger.info("Showing all {} active resources (Show All Resources enabled)", resources.size());
                }
            }
            
            // Update timeline data
            // Fetch unavailabilities for the date range
            List<TechnicianUnavailability> unavailabilities = schedulingService.getUnavailabilitiesInDateRange(startDate, endDate);
            
            // Fetch company holidays from database
            List<CompanyHoliday> holidays = loadCompanyHolidaysFromDatabase(startDate, endDate);
            
            // Fetch project managers
            List<ProjectManager> projectManagers = schedulingService.getAllProjectManagers();
            
            // When filtering, use the already filtered projects list
            // If project filters are applied, use the filtered list, otherwise use all projects
            List<Project> projectsToDisplay;
            if (hasProjectFilter || hasManagerFilter || hasStatusFilter) {
                // Use the filtered projects when project-based filters are active
                projectsToDisplay = projects;
                logger.info("Using filtered projects list ({} projects) due to active filters", projectsToDisplay.size());
            } else if (selectedResource != null && !selectedResource.equals("All Resources")) {
                // When filtering by a specific resource only, show projects that have assignments for that resource
                Set<Long> projectIdsWithAssignments = assignments.stream()
                    .map(Assignment::getProjectId)
                    .collect(java.util.stream.Collectors.toSet());
                
                projectsToDisplay = allProjects.stream()
                    .filter(p -> projectIdsWithAssignments.contains(p.getId()))
                    .toList();
                logger.info("Filtered projects for resource '{}': {} projects with assignments", 
                    selectedResource, projectsToDisplay.size());
            } else {
                // No filters - show all projects
                projectsToDisplay = allProjects;
                logger.info("No filters applied - showing all {} projects", projectsToDisplay.size());
            }
            
            logger.info("Setting timeline data - Projects: {}, Resources: {}, Assignments: {}, Unavailabilities: {}, Holidays: {}", 
                projectsToDisplay.size(), resources.size(), assignments.size(), unavailabilities.size(), holidays.size());
            timelineView.getProjects().setAll(projectsToDisplay);
            timelineView.getResources().setAll(resources);
            timelineView.getAssignments().setAll(assignments);
            timelineView.getUnavailabilities().setAll(unavailabilities);
            timelineView.getCompanyHolidays().setAll(holidays);
            timelineView.setProjectManagers(projectManagers);
            
            // Detect and highlight conflicts
            Set<Long> conflicts = schedulingService.detectAllConflicts(startDate, endDate);
            timelineView.updateConflicts(conflicts);
            
            updateStatusLabel();
            
        } catch (Exception e) {
            logger.error("Error refreshing data", e);
            showErrorAlert("Data Error", "Failed to refresh data: " + e.getMessage());
        }
    }

    private void updateStatusLabel() {
        int projectCount = schedulingService.getProjectCount();
        int resourceCount = schedulingService.getResourceCount();
        int assignmentCount = schedulingService.getAssignmentCount();
        
        int displayingProjects = timelineView != null ? timelineView.getProjects().size() : 0;
        int displayingResources = timelineView != null ? timelineView.getResources().size() : 0;
        int conflictCount = timelineView != null ? timelineView.getConflictedAssignmentIds().size() : 0;
        
        StringBuilder status = new StringBuilder();
        status.append(String.format("Projects: %d | Resources: %d | Assignments: %d | Timeline: %d projects, %d resources", 
                projectCount, resourceCount, assignmentCount, displayingProjects, displayingResources));
        
        if (conflictCount > 0) {
            status.append(" | ⚠️ CONFLICTS: ").append(conflictCount);
        }
        
        statusLabel.setText(status.toString());
        
        // Style the status label based on conflicts
        if (conflictCount > 0) {
            statusLabel.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: inherit; -fx-font-weight: normal;");
        }
    }

    // Utility Methods
    private void showInfoAlert(String title, String message) {
        javafx.stage.Window owner = timelineView != null && timelineView.getScene() != null ? 
            timelineView.getScene().getWindow() : null;
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.INFORMATION, owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void openEditProjectDialog() {
        // Show project selection dialog for editing
        List<Project> projects = schedulingService.getAllProjects();
        
        logger.info("=== LOADING PROJECTS FOR EDIT ===");
        for (Project p : projects) {
            if (p.getProjectId().startsWith("25")) {
                logger.info("Project {} has travel={}", p.getProjectId(), p.isTravel());
                break; // Just log the first one as a sample
            }
        }
        
        if (projects.isEmpty()) {
            showInfoAlert("No Projects", "There are no projects to edit. Please create a project first.");
            return;
        }
        
        // Use the same ProjectSelectionDialog with filtering as Project Tasks
        ProjectSelectionDialog dialog = new ProjectSelectionDialog(projects,
            "Select Project to Edit",
            "Select a project to edit (you can filter by project ID or description)");
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        
        Optional<Project> result = dialog.showAndWait();
        if (result.isPresent()) {
            editProject(result.get());
        }
    }
    
    @FXML
    private void openFinancialTracking() {
        // Show project selection dialog
        List<Project> projects = schedulingService.getAllProjects();
        if (projects.isEmpty()) {
            showInfoAlert("No Projects", "Please create a project first before accessing financial tracking.");
            return;
        }
        
        ProjectSelectionDialog dialog = new ProjectSelectionDialog(projects, "Select Project", "Select a project for financial tracking");
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        
        Optional<Project> result = dialog.showAndWait();
        if (result.isPresent()) {
            Project selectedProject = result.get();
            // Open financial tracking dialog with FinancialService
            FinancialService financialService = new FinancialService(
                schedulingService.getDataSource(), 
                schedulingService.getProjectRepository()
            );
            javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            FinancialTrackingDialog financialDialog = new FinancialTrackingDialog(selectedProject, financialService, owner);
            financialDialog.showAndWait();
        }
    }
    
    @FXML
    private void openQuickRevenueBudget() {
        openReportCenterWithReport("Revenue/Budget Report");
    }
    
    @FXML
    private void openQuickResourceUtil() {
        openReportCenterWithReport("Resource Utilization Report");
    }
    
    @FXML
    private void openQuickProjectStatus() {
        openReportCenterWithReport("Project Status Report");
    }
    
    @FXML
    private void openQuickWorkload() {
        openReportCenterWithReport("Workload Report");
    }
    
    @FXML
    private void openFinancialTimeline() {
        // Show project selection dialog
        List<Project> projects = schedulingService.getAllProjects();
        if (projects.isEmpty()) {
            showInfoAlert("No Projects", "Please create a project first before accessing the financial timeline.");
            return;
        }
        
        ProjectSelectionDialog dialog = new ProjectSelectionDialog(projects, "Select Project", "Select a project for financial timeline");
        DialogUtils.initializeDialog(dialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        
        Optional<Project> result = dialog.showAndWait();
        if (result.isPresent()) {
            Project selectedProject = result.get();
            // Open financial timeline view with FinancialService
            FinancialService financialService = new FinancialService(
                schedulingService.getDataSource(), 
                schedulingService.getProjectRepository()
            );
            
            FinancialTimelineView timelineView = new FinancialTimelineView(selectedProject, financialService);
            timelineView.show();
        }
    }
    
    private void openReportCenterWithReport(String reportType) {
        try {
            javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            ReportCenterView reportCenter = new ReportCenterView(schedulingService, null, owner);
            reportCenter.selectReport(reportType);
            reportCenter.show();
        } catch (Exception e) {
            logger.error("Error opening report: " + reportType, e);
            showErrorAlert("Error", "Failed to open report: " + e.getMessage());
        }
    }
    
    @FXML
    private void openPOImport() {
        javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
        POImportDialog dialog = new POImportDialog(schedulingService.getDataSource(), owner);
        dialog.showAndWait();
        
        // If a PO was selected, you could use it for financial tracking
        POSpreadsheetImportService.PORecord selectedPO = dialog.getSelectedPO();
        if (selectedPO != null) {
            logger.info("Selected PO: {} - {} for use in project", selectedPO.poNumber, selectedPO.vendor);
            // Could open financial tracking with this PO pre-selected
        }
    }
    
    @FXML
    private void openUserGuide() {
        showInfoAlert("User Guide", "User guide will be available in a future update.");
    }
    
    @FXML
    private void openKeyboardShortcuts() {
        showInfoAlert("Keyboard Shortcuts", 
            "Common Shortcuts:\n\n" +
            "Ctrl+N - New Project\n" +
            "Ctrl+R - New Resource\n" +
            "Ctrl+A - New Assignment\n" +
            "F5 - Refresh View\n" +
            "Ctrl+Q - Exit Application");
    }
    
    @FXML
    private void openAbout() {
        showInfoAlert("About", 
            "Simple Project Resource Manager\n" +
            "Version 1.0\n\n" +
            "A comprehensive project scheduling and resource management application.\n\n" +
            "© 2025 Subliminal Search");
    }
    
    @FXML
    private void handleExit() {
        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Exit Application");
        confirmDialog.setHeaderText("Exit Simple Project Resource Manager");
        confirmDialog.setContentText("Are you sure you want to exit the application?");
        DialogUtils.initializeDialog(confirmDialog, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            logger.info("Application exit requested by user");
            
            // Clean shutdown
            try {
                // Close any open database connections if needed
                if (schedulingService != null) {
                    // Any cleanup needed
                }
                
                // Exit the application
                Platform.exit();
                System.exit(0);
            } catch (Exception e) {
                logger.error("Error during application shutdown", e);
                System.exit(1);
            }
        }
    }
    
    private void showErrorAlert(String title, String message) {
        javafx.stage.Window owner = timelineView != null && timelineView.getScene() != null ? 
            timelineView.getScene().getWindow() : null;
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.ERROR, owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void loadGardenDemoData() {
        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("Load Garden Demo Data");
        confirm.setHeaderText("Replace ALL Data with Garden Demo?");
        confirm.setContentText("WARNING: This will DELETE all existing data and replace it with garden-themed demo data.\n\nThis action cannot be undone!");
        DialogUtils.initializeDialog(confirm, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Show loading dialog
                    Alert loading = new Alert(Alert.AlertType.INFORMATION);
                    loading.setTitle("Loading Data");
                    loading.setHeaderText("Loading Garden Demo Data...");
                    loading.setContentText("Please wait while the database is being populated with garden-themed projects.");
                    DialogUtils.initializeDialog(loading, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
                    loading.show();
                    
                    // Run in background thread
                    new Thread(() -> {
                        try {
                            com.subliminalsearch.simpleprojectresourcemanager.util.DataManager.clearAndLoadGardenData();
                            
                            Platform.runLater(() -> {
                                loading.close();
                                refreshData();
                                
                                Alert success = new Alert(Alert.AlertType.INFORMATION);
                                success.setTitle("Success");
                                success.setHeaderText("Garden Demo Data Loaded!");
                                success.setContentText("The database now contains:\n• 4 Project Managers\n• 20 Garden Technicians\n• 47 Garden Projects\n• Tasks and Assignments with conflicts");
                                DialogUtils.initializeDialog(success, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
                                success.showAndWait();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> {
                                loading.close();
                                showErrorAlert("Error", "Failed to load demo data: " + ex.getMessage());
                            });
                        }
                    }).start();
                    
                } catch (Exception e) {
                    showErrorAlert("Error", "Failed to load demo data: " + e.getMessage());
                }
            }
        });
    }
    
    private void clearOldTestData() {
        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("Clear Old Test Data");
        confirm.setHeaderText("Delete Non-Garden Test Data?");
        confirm.setContentText("This will DELETE all projects, resources, and assignments that are not garden-themed.\n\nThis action cannot be undone!");
        DialogUtils.initializeDialog(confirm, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // For now, show what would be deleted
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Data Cleanup");
                    info.setHeaderText("Old Test Data Identified");
                    info.setContentText("The following non-garden data would be removed:\n• Projects without garden/plant/flower themes\n• Resources without garden-related names\n• Associated tasks and assignments\n\nUse the DatabaseCleaner utility to perform actual cleanup.");
                    DialogUtils.initializeDialog(info, timelineView.getScene() != null ? timelineView.getScene().getWindow() : null);
                    info.showAndWait();
                    
                    refreshData();
                } catch (Exception e) {
                    showErrorAlert("Error", "Failed to clear test data: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void handleExecutiveView() {
        showExecutiveCommandCenter();
    }
    
    private void showExecutiveCommandCenter() {
        try {
            Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            ExecutiveCommandCenter commandCenter = new ExecutiveCommandCenter(schedulingService, owner);
            commandCenter.show();
            logger.info("Opened Executive Command Center");
        } catch (Exception e) {
            logger.error("Error opening Executive Command Center", e);
            showErrorAlert("Error", "Failed to open Executive Command Center: " + e.getMessage());
        }
    }
    
    @FXML
    private void openExecutiveDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/executive-dashboard.fxml"));
            BorderPane dashboardRoot = loader.load();
            
            // Pass the scheduling service to the dashboard
            ExecutiveDashboardController dashboardController = loader.getController();
            dashboardController.setSchedulingService(schedulingService);
            
            Stage dashboardStage = new Stage();
            dashboardStage.setTitle("Executive Dashboard - Simple Project Resource Manager");
            dashboardStage.setScene(new Scene(dashboardRoot, 1200, 800));
            dashboardStage.initModality(Modality.NONE);
            
            // Set owner and position on same screen
            javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            if (owner != null) {
                dashboardStage.initOwner(owner);
                DialogUtils.positionStageOnOwnerScreen(dashboardStage, owner, 0.9, 0.85);
            }
            
            // Add stylesheet
            dashboardStage.getScene().getStylesheets().add(
                getClass().getResource("/css/executive-dashboard.css").toExternalForm());
            
            dashboardStage.show();
            logger.info("Opened Executive Dashboard");
        } catch (Exception e) {
            logger.error("Error opening Executive Dashboard", e);
            showErrorAlert("Error", "Failed to open Executive Dashboard: " + e.getMessage());
        }
    }
    
    private void showEmailSettings() {
        try {
            EmailSettingsDialog dialog = new EmailSettingsDialog();
            dialog.showAndWait().ifPresent(config -> {
                logger.info("Email settings updated");
                showInfoAlert("Success", "Email settings have been saved successfully.");
            });
        } catch (Exception e) {
            logger.error("Error showing email settings", e);
            showErrorAlert("Error", "Failed to open email settings: " + e.getMessage());
        }
    }
    
    @FXML
    private void configureDomainLogins() {
        try {
            javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            com.subliminalsearch.simpleprojectresourcemanager.dialog.DomainLoginConfigDialog dialog = 
                new com.subliminalsearch.simpleprojectresourcemanager.dialog.DomainLoginConfigDialog(owner, schedulingService.getDataSource());
            dialog.showAndWait();
            // Refresh resources after configuration
            refreshData();
        } catch (Exception e) {
            logger.error("Error configuring domain logins", e);
            showErrorAlert("Error", "Failed to configure domain logins: " + e.getMessage());
        }
    }
    
    @FXML
    private void configureSharePoint() {
        try {
            javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            com.subliminalsearch.simpleprojectresourcemanager.dialog.SharePointConfigDialog dialog = 
                new com.subliminalsearch.simpleprojectresourcemanager.dialog.SharePointConfigDialog(owner);
            dialog.showAndWait().ifPresent(config -> {
                logger.info("SharePoint configuration updated");
                if (config.isEnabled() && config.isConfigured()) {
                    showInfoAlert("Success", "SharePoint integration configured successfully. Sync will begin shortly.");
                }
            });
        } catch (Exception e) {
            logger.error("Error configuring SharePoint", e);
            showErrorAlert("Error", "Failed to configure SharePoint: " + e.getMessage());
        }
    }
    
    @FXML
    public void syncSharePointNow() {
        System.out.println("==========================================");
        System.out.println("SHAREPOINT SYNC MENU CLICKED!");
        System.out.println("==========================================");
        
        try {
            System.out.println("syncSharePointNow() method called");
            System.out.println("Thread: " + Thread.currentThread().getName());
            logger.info("Manual SharePoint sync triggered");
            
            // Show confirmation dialog
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Manual SharePoint Sync");
            confirmAlert.setHeaderText("Sync Assignments to SharePoint");
            confirmAlert.setContentText("This will sync all current assignments to SharePoint calendars.\n\n" +
                "Note: Running in " + (com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointTestMode.isTestMode() ? 
                "TEST MODE (CSV output only)" : "LIVE MODE (will create calendar events)") + "\n\n" +
                "Do you want to continue?");
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            System.out.println("Dialog result: " + result);
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.out.println("User clicked OK, starting sync thread...");
                System.out.println("schedulingService is: " + schedulingService);
                
                if (schedulingService == null) {
                    System.out.println("ERROR: schedulingService is null!");
                    showErrorAlert("Error", "Scheduling service not initialized");
                    return;
                }
                
                javax.sql.DataSource ds = schedulingService.getDataSource();
                System.out.println("DataSource: " + ds);
                
                if (ds == null) {
                    System.out.println("ERROR: DataSource is null!");
                    showErrorAlert("Error", "Database connection not available");
                    return;
                }
                
                // Run sync in background
                new Thread(() -> {
                    try {
                        System.out.println("Sync thread started, creating SimpleSharePointSync with DataSource: " + ds);
                        com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SimpleSharePointSync sync = 
                            new com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SimpleSharePointSync(ds);
                        System.out.println("SimpleSharePointSync created, calling syncAll()...");
                        sync.syncAll();
                        System.out.println("syncAll() completed");
                        
                        Platform.runLater(() -> {
                            showInfoAlert("Sync Complete", 
                                "SharePoint sync completed successfully.\n" +
                                (com.subliminalsearch.simpleprojectresourcemanager.integration.sharepoint.SharePointTestMode.isTestMode() ?
                                "Test results saved to: sharepoint-test-output/" : "Calendar events have been created."));
                        });
                    } catch (Exception e) {
                        System.out.println("ERROR in sync thread: " + e.getMessage());
                        e.printStackTrace();
                        logger.error("SharePoint sync failed", e);
                        Platform.runLater(() -> {
                            showErrorAlert("Sync Failed", "SharePoint sync failed: " + e.getMessage());
                        });
                    }
                }).start();
                System.out.println("Thread.start() called");
            } else {
                System.out.println("User did NOT click OK. Result was: " + result);
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION caught in syncSharePointNow: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error triggering SharePoint sync", e);
            showErrorAlert("Error", "Failed to trigger SharePoint sync: " + e.getMessage());
        } catch (Throwable t) {
            System.out.println("THROWABLE caught in syncSharePointNow: " + t.getMessage());
            t.printStackTrace();
        }
        System.out.println("syncSharePointNow() method exiting");
    }
    
    @FXML
    public void viewSyncLog() {
        try {
            String logPath = "sharepoint-test-output";
            java.io.File logDir = new java.io.File(logPath);
            
            if (!logDir.exists() || !logDir.isDirectory()) {
                showInfoAlert("No Logs", "No sync logs found. Run a sync first to generate logs.");
                return;
            }
            
            // Find the most recent log file
            java.io.File[] files = logDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (files == null || files.length == 0) {
                showInfoAlert("No Logs", "No sync logs found. Run a sync first to generate logs.");
                return;
            }
            
            // Sort by last modified to get most recent
            java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            java.io.File mostRecentLog = files[0];
            
            // Open the log file in default system editor
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(mostRecentLog);
                logger.info("Opened sync log: " + mostRecentLog.getName());
            } else {
                showInfoAlert("Log Location", "Latest sync log:\n" + mostRecentLog.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Error viewing sync log", e);
            showErrorAlert("Error", "Failed to view sync log: " + e.getMessage());
        }
    }
    
    private void showReportCenter() {
        try {
            javafx.stage.Window owner = timelineView.getScene() != null ? timelineView.getScene().getWindow() : null;
            ReportCenterView reportCenter = new ReportCenterView(schedulingService, null, owner);
            reportCenter.show();
            logger.info("Opened Report Center");
        } catch (Exception e) {
            logger.error("Error opening Report Center", e);
            showErrorAlert("Error", "Failed to open Report Center: " + e.getMessage());
        }
    }
    
    // Getters for testing
    public LocalDate getCurrentDisplayDate() {
        return currentDisplayDate;
    }
    
    public TimelineView getTimelineView() {
        return timelineView;
    }
}