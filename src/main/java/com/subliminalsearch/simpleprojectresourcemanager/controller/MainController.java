package com.subliminalsearch.simpleprojectresourcemanager.controller;

import com.subliminalsearch.simpleprojectresourcemanager.component.TimelineView;
import com.subliminalsearch.simpleprojectresourcemanager.controller.ClientReportController;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.AssignmentDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.EmailSettingsDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.HolidayDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ProjectDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ProjectManagerDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.ResourceDialog;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.UnavailabilityDialog;
import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.model.CompanyHoliday;
import com.subliminalsearch.simpleprojectresourcemanager.model.EmailConfiguration;
import com.subliminalsearch.simpleprojectresourcemanager.model.HolidayType;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.TechnicianUnavailability;
import com.subliminalsearch.simpleprojectresourcemanager.model.UnavailabilityType;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.view.ExecutiveCommandCenter;
import com.subliminalsearch.simpleprojectresourcemanager.view.HolidayCalendarView;
import com.subliminalsearch.simpleprojectresourcemanager.view.ProjectGridView;
import com.subliminalsearch.simpleprojectresourcemanager.view.ReportCenterView;
import com.subliminalsearch.simpleprojectresourcemanager.view.ResourceAvailabilityView;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
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
import java.util.stream.Collectors;

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    private final SchedulingService schedulingService;

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
    @FXML private MenuItem menuProjectManagers;
    @FXML private MenuItem menuProjectGrid;
    @FXML private MenuItem menuProjectTasks;
    @FXML private MenuItem menuTechUnavailability;
    @FXML private MenuItem menuHolidayCalendar;
    @FXML private MenuItem menuHolidayCalendarView;
    @FXML private MenuItem menuLoadGardenData;
    @FXML private MenuItem menuClearOldData;
    @FXML private MenuItem menuResourceAvailabilityView;
    @FXML private MenuItem menuEmailSettings;
    @FXML private MenuItem menuReportCenter;

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

    // FXML Components - Action Buttons
    @FXML private Button btnNewProject;
    @FXML private Button btnNewResource;
    @FXML private Button btnNewAssignment;
    @FXML private Button btnEditSelected;
    @FXML private Button btnDeleteSelected;
    @FXML private Button btnExecutiveView;

    // Timeline Component
    private TimelineView timelineView;
    private LocalDate currentDisplayDate;

    public MainController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
        this.currentDisplayDate = LocalDate.now();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing main controller...");
        
        setupToolbar();
        setupFilters();
        setupTimeline();
        setupEventHandlers();
        
        refreshData();
        updateStatusLabel();
        
        logger.info("Main controller initialized successfully");
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
        
        // Set button tooltips
        btnPrevDay.setTooltip(new Tooltip("Previous Day (Ctrl+Left)"));
        btnPrevWeek.setTooltip(new Tooltip("Previous Week (Shift+Left)"));
        btnPrevMonth.setTooltip(new Tooltip("Previous Month (Ctrl+Shift+Left)"));
        btnToday.setTooltip(new Tooltip("Today (Ctrl+Home)"));
        btnNextDay.setTooltip(new Tooltip("Next Day (Ctrl+Right)"));
        btnNextWeek.setTooltip(new Tooltip("Next Week (Shift+Right)"));
        btnNextMonth.setTooltip(new Tooltip("Next Month (Ctrl+Shift+Right)"));
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
        
        projectNames.addAll(projects.stream()
            .map(Project::getProjectId)
            .sorted()
            .toList());
        
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
        
        // Set up context menu callbacks
        timelineView.setOnEditProject(this::editProject);
        timelineView.setOnDeleteProject(this::deleteProject);
        timelineView.setOnEditResource(this::editResource);
        timelineView.setOnDeleteResource(this::deleteResource);
        timelineView.setOnEditAssignment(this::editAssignment);
        timelineView.setOnDeleteAssignment(this::deleteAssignment);
        timelineView.setOnDuplicateAssignment(this::duplicateAssignment);
        timelineView.setOnShowProjectDetails(ProjectDialog::showProjectDetails);
        timelineView.setOnShowResourceDetails(ResourceDialog::showResourceDetails);
        timelineView.setOnChangeProjectStatus(this::changeProjectStatus);
        timelineView.setOnGenerateReport(this::generateClientReport);
        timelineView.setOnShowProjectTasks(this::showProjectTasks);
        timelineView.setOnViewInReportCenter(this::viewInReportCenter);
        
        // Replace the timeline container content
        timelineContainer.setCenter(timelineView);
        
        // Set initial date range (current month)
        LocalDate startOfMonth = currentDisplayDate.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        timelineView.setDateRange(startOfMonth, endOfMonth);
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
        
        // Action button handlers
        btnNewProject.setOnAction(e -> createNewProject());
        btnNewResource.setOnAction(e -> createNewResource());
        btnNewAssignment.setOnAction(e -> createNewAssignment());
        btnEditSelected.setOnAction(e -> editSelected());
        btnDeleteSelected.setOnAction(e -> deleteSelected());
        btnExecutiveView.setOnAction(e -> showExecutiveCommandCenter());
        
        // Menu item handlers
        menuNewProject.setOnAction(e -> createNewProject());
        menuNewResource.setOnAction(e -> createNewResource());
        menuNewAssignment.setOnAction(e -> createNewAssignment());
        menuProjectManagers.setOnAction(e -> manageProjectManagers());
        menuProjectGrid.setOnAction(e -> showProjectGridView());
        menuProjectTasks.setOnAction(e -> showProjectTasks());
        menuTechUnavailability.setOnAction(e -> manageTechnicianUnavailability());
        menuHolidayCalendar.setOnAction(e -> manageHolidayCalendar());
        menuLoadGardenData.setOnAction(e -> loadGardenDemoData());
        menuClearOldData.setOnAction(e -> clearOldTestData());
        
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

    // Action Methods
    private void createNewProject() {
        // Pass the project repository and project managers
        List<ProjectManager> managers = schedulingService.getActiveProjectManagers();
        ProjectDialog dialog = new ProjectDialog(null, schedulingService.getProjectRepository(), managers);
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
                        showInfoAlert("Success", "Project Manager deleted successfully.");
                    } catch (Exception e) {
                        logger.error("Error deleting project manager", e);
                        showErrorAlert("Error", "Failed to delete project manager: " + e.getMessage());
                    }
                }
            },
            // Add handler
            () -> {
                ProjectManagerDialog addDialog = new ProjectManagerDialog();
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

    private void editSelected() {
        showInfoAlert("Edit Item", "Please right-click on projects or resources to edit them.");
    }

    private void deleteSelected() {
        showInfoAlert("Delete Item", "Please right-click on projects or resources to delete them.");
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
        // Pass project managers list for editing
        List<ProjectManager> managers = schedulingService.getActiveProjectManagers();
        ProjectDialog dialog = new ProjectDialog(project, schedulingService.getProjectRepository(), managers);
        Optional<Project> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                schedulingService.updateProject(result.get());
                logger.info("Updated project: {}", project.getProjectId());
                updateProjectManagerFilter(); // Update filter in case PM changed
                refreshData();
                showInfoAlert("Success", "Project updated successfully.");
                
            } catch (Exception e) {
                logger.error("Error updating project", e);
                showErrorAlert("Error", "Failed to update project: " + e.getMessage());
            }
        }
    }
    
    private void deleteProject(Project project) {
        if (ProjectDialog.showDeleteConfirmation(project)) {
            try {
                schedulingService.deleteProject(project.getId());
                logger.info("Deleted project: {}", project.getProjectId());
                refreshData();
                showInfoAlert("Success", "Project deleted successfully.");
                
            } catch (Exception e) {
                logger.error("Error deleting project", e);
                showErrorAlert("Error", "Failed to delete project: " + e.getMessage());
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
                
                // Show task list view
                TaskListView taskListView = new TaskListView(project, taskRepository, resources, projectAssignments, schedulingService);
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
                ReportCenterView reportCenter = new ReportCenterView(schedulingService, project);
                reportCenter.show();
            }
        } catch (Exception e) {
            logger.error("Error opening report center", e);
            showErrorAlert("Error", "Failed to open report center: " + e.getMessage());
        }
    }
    
    private void editResource(Resource resource) {
        ResourceDialog dialog = new ResourceDialog(resource);
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
    
    private void deleteResource(Resource resource) {
        if (ResourceDialog.showDeleteConfirmation(resource)) {
            try {
                // Note: We'd need a deleteResource method in the service
                logger.info("Resource delete requested: {}", resource.getName());
                refreshData();
                showInfoAlert("Success", "Resource deleted successfully.");
                
            } catch (Exception e) {
                logger.error("Error deleting resource", e);
                showErrorAlert("Error", "Failed to delete resource: " + e.getMessage());
            }
        }
    }
    
    private void editAssignment(Assignment assignment) {
        List<Project> projects = schedulingService.getAllProjects();
        List<Resource> resources = schedulingService.getAllResources();
        
        AssignmentDialog dialog = new AssignmentDialog(assignment, projects, resources, schedulingService);
        Optional<Assignment> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                schedulingService.updateAssignment(result.get());
                logger.info("Updated assignment: {}", assignment.getId());
                refreshData();
                showInfoAlert("Success", "Assignment updated successfully.");
                
            } catch (Exception e) {
                logger.error("Error updating assignment", e);
                showErrorAlert("Error", "Failed to update assignment: " + e.getMessage());
            }
        }
    }
    
    private void deleteAssignment(Assignment assignment) {
        // Find project and resource for confirmation dialog
        Project project = schedulingService.getProjectById(assignment.getProjectId()).orElse(null);
        Resource resource = schedulingService.getResourceById(assignment.getResourceId()).orElse(null);
        
        if (AssignmentDialog.showDeleteConfirmation(assignment, project, resource)) {
            try {
                schedulingService.deleteAssignment(assignment.getId());
                logger.info("Deleted assignment: {}", assignment.getId());
                refreshData();
                showInfoAlert("Success", "Assignment deleted successfully.");
                
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
    private void manageTechnicianUnavailability() {
        List<Resource> resources = schedulingService.getAllResources();
        
        if (resources.isEmpty()) {
            showErrorAlert("No Resources", "Please create at least one resource before managing unavailability.");
            return;
        }
        
        UnavailabilityDialog dialog = new UnavailabilityDialog(resources);
        Optional<TechnicianUnavailability> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            try {
                TechnicianUnavailability unavailability = result.get();
                // TODO: Add service method to save unavailability
                logger.info("Created new unavailability: {} for resource {}", 
                    unavailability.getType(), unavailability.getResourceId());
                
                showInfoAlert("Success", "Technician unavailability created successfully.\n\n" +
                    "Note: Database integration pending - this will be saved once the unavailability tables are created.");
                
            } catch (Exception e) {
                logger.error("Error creating unavailability", e);
                showErrorAlert("Error", "Failed to create unavailability: " + e.getMessage());
            }
        }
    }
    
    private void manageHolidayCalendar() {
        HolidayDialog dialog = new HolidayDialog();
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
                Optional<CompanyHoliday> result = dialog.showAndWait();
                if (result.isPresent()) {
                    // Add to view's holiday list (temporary until database integration)
                    holidayView.getHolidays().add(result.get());
                    logger.info("Added holiday: {}", result.get().getName());
                }
            });
            
            holidayView.setOnEditHoliday(holiday -> {
                HolidayDialog dialog = new HolidayDialog(holiday);
                Optional<CompanyHoliday> result = dialog.showAndWait();
                if (result.isPresent()) {
                    // Update in the list
                    int index = holidayView.getHolidays().indexOf(holiday);
                    if (index >= 0) {
                        holidayView.getHolidays().set(index, result.get());
                    }
                    logger.info("Updated holiday: {}", result.get().getName());
                }
            });
            
            holidayView.setOnDeleteHoliday(holiday -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Holiday");
                confirm.setHeaderText("Delete holiday: " + holiday.getName());
                confirm.setContentText("Are you sure you want to delete this holiday?");
                
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    holidayView.getHolidays().remove(holiday);
                    logger.info("Deleted holiday: {}", holiday.getName());
                }
            });
            
            // Create sample holidays for demonstration
            List<CompanyHoliday> sampleHolidays = createSampleHolidays();
            holidayView.setHolidays(sampleHolidays);
            
            // Create and show window
            Stage stage = new Stage();
            stage.setTitle("Holiday Calendar");
            stage.initModality(Modality.NONE);
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
            ProjectGridView gridView = new ProjectGridView(schedulingService);
            gridView.show();
        } catch (Exception e) {
            logger.error("Error showing project grid view", e);
            showErrorAlert("Error", "Failed to open project grid editor: " + e.getMessage());
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
            
            // Create choice dialog for project selection
            ChoiceDialog<Project> dialog = new ChoiceDialog<>(projects.get(0), projects);
            dialog.setTitle("Select Project");
            dialog.setHeaderText("Select a project to manage tasks");
            dialog.setContentText("Project:");
            
            // Get the combo box from the dialog and set custom converter
            ComboBox<Project> comboBox = (ComboBox<Project>) dialog.getDialogPane().lookup(".combo-box");
            if (comboBox != null) {
                comboBox.setConverter(new StringConverter<Project>() {
                    @Override
                    public String toString(Project project) {
                        if (project == null) return "";
                        return project.getProjectId() + " - " + project.getDescription();
                    }
                    
                    @Override
                    public Project fromString(String string) {
                        return null;
                    }
                });
            }
            
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
                TaskListView taskListView = new TaskListView(selectedProject, taskRepository, resources, projectAssignments, schedulingService);
                taskListView.show();
            }
        } catch (Exception e) {
            logger.error("Error showing project tasks", e);
            showErrorAlert("Error", "Failed to open task management: " + e.getMessage());
        }
    }
    
    private void showResourceAvailabilityView() {
        try {
            // Create the resource availability view
            ResourceAvailabilityView availabilityView = new ResourceAvailabilityView();
            
            // Set data
            availabilityView.setResources(schedulingService.getAllResources());
            availabilityView.setAssignments(schedulingService.getAssignmentsByDateRange(
                LocalDate.now().minusMonths(3), 
                LocalDate.now().plusMonths(3)
            ));
            
            // Create sample unavailabilities and holidays for demonstration
            List<TechnicianUnavailability> sampleUnavailabilities = createSampleUnavailabilities();
            availabilityView.setUnavailabilities(sampleUnavailabilities);
            
            List<CompanyHoliday> sampleHolidays = createSampleHolidays();
            availabilityView.setHolidays(sampleHolidays);
            
            // Create and show window
            Stage stage = new Stage();
            stage.setTitle("Resource Availability Calendar");
            stage.initModality(Modality.NONE);
            stage.setScene(new Scene(availabilityView, 1400, 850));
            stage.setMinWidth(1200);
            stage.setMinHeight(700);
            stage.show();
            
        } catch (Exception e) {
            logger.error("Error showing resource availability view", e);
            showErrorAlert("Error", "Failed to show resource availability: " + e.getMessage());
        }
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
                    // Show the entire month
                    startDate = currentDisplayDate.withDayOfMonth(1);
                    endDate = startDate.plusMonths(1).minusDays(1);
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
            List<Project> projects = schedulingService.getProjectsByDateRange(startDate, endDate);
            List<Resource> resources = schedulingService.getAllResources();
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
            
            // Filter assignments to only show those for filtered projects
            List<Long> projectIds = projects.stream().map(Project::getId).toList();
            logger.info("Project IDs after filtering: {}", projectIds);
            int originalAssignmentCount = assignments.size();
            assignments = assignments.stream()
                .filter(a -> projectIds.contains(a.getProjectId()))
                .toList();
            logger.info("Filtered assignments from {} to {} based on projects", originalAssignmentCount, assignments.size());
            
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
            }
            
            // Update timeline data
            logger.info("Setting timeline data - Projects: {}, Resources: {}, Assignments: {}", 
                projects.size(), resources.size(), assignments.size());
            timelineView.getProjects().setAll(projects);
            timelineView.getResources().setAll(resources);
            timelineView.getAssignments().setAll(assignments);
            
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Show loading dialog
                    Alert loading = new Alert(Alert.AlertType.INFORMATION);
                    loading.setTitle("Loading Data");
                    loading.setHeaderText("Loading Garden Demo Data...");
                    loading.setContentText("Please wait while the database is being populated with garden-themed projects.");
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
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // For now, show what would be deleted
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Data Cleanup");
                    info.setHeaderText("Old Test Data Identified");
                    info.setContentText("The following non-garden data would be removed:\n• Projects without garden/plant/flower themes\n• Resources without garden-related names\n• Associated tasks and assignments\n\nUse the DatabaseCleaner utility to perform actual cleanup.");
                    info.showAndWait();
                    
                    refreshData();
                } catch (Exception e) {
                    showErrorAlert("Error", "Failed to clear test data: " + e.getMessage());
                }
            }
        });
    }
    
    private void showExecutiveCommandCenter() {
        try {
            ExecutiveCommandCenter commandCenter = new ExecutiveCommandCenter(schedulingService);
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
    
    private void showReportCenter() {
        try {
            ReportCenterView reportCenter = new ReportCenterView(schedulingService);
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