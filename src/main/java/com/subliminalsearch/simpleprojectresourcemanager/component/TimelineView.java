package com.subliminalsearch.simpleprojectresourcemanager.component;

import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.model.CompanyHoliday;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import com.subliminalsearch.simpleprojectresourcemanager.model.Resource;
import com.subliminalsearch.simpleprojectresourcemanager.model.TechnicianUnavailability;
import com.subliminalsearch.simpleprojectresourcemanager.model.UnavailabilityType;
import com.subliminalsearch.simpleprojectresourcemanager.model.UtilizationSettings;
import com.subliminalsearch.simpleprojectresourcemanager.service.FinancialService;
import com.subliminalsearch.simpleprojectresourcemanager.service.OpenItemService;
import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.util.HelpButton;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import com.subliminalsearch.simpleprojectresourcemanager.view.FinancialTrackingDialog;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.Cursor;
import javafx.stage.Stage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

public class TimelineView extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(TimelineView.class);
    
    // Base constants for timeline display
    private static final double BASE_DAY_WIDTH = 30.0;
    private static final double BASE_ROW_HEIGHT = 60.0; // Increased to accommodate taller project bars
    private static final double BASE_PROJECT_BAR_HEIGHT = 45.0; // Increased for 3 lines of text
    private static final double BASE_RESOURCE_LABEL_WIDTH = 200.0; // Increased for longer resource names
    private static final double HEADER_HEIGHT = 45.0; // Fixed header height for both sides
    
    // Current zoom level (1.0 = 100%, 1.25 = 125%, etc.)
    private double zoomLevel = 1.0;
    
    // View mode for different timeline displays
    public enum ViewMode {
        WEEK,
        MONTH,
        QUARTER
    }
    private ViewMode currentViewMode = ViewMode.MONTH;
    
    // Computed dimensions based on zoom
    private double dayWidth = BASE_DAY_WIDTH;
    private double rowHeight = BASE_ROW_HEIGHT;
    private double projectBarHeight = BASE_PROJECT_BAR_HEIGHT;
    private double resourceLabelWidth = BASE_RESOURCE_LABEL_WIDTH;
    
    // Properties
    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>();
    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<Resource> resources = FXCollections.observableArrayList();
    private final ObservableList<Assignment> assignments = FXCollections.observableArrayList();
    private final ObservableList<TechnicianUnavailability> unavailabilities = FXCollections.observableArrayList();
    private final ObservableList<CompanyHoliday> companyHolidays = FXCollections.observableArrayList();
    private final ObservableList<com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager> projectManagers = FXCollections.observableArrayList();
    private Map<Long, com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager> projectManagerMap = new HashMap<>();
    
    // Conflict detection
    private Set<Long> conflictedAssignmentIds = new HashSet<>();
    
    // Utilization Settings
    private UtilizationSettings utilizationSettings = new UtilizationSettings();
    
    // Display Settings
    private boolean showUnavailability = true;
    
    // Services
    private OpenItemService openItemService;
    
    // UI Components
    private HBox headerRow;
    private ScrollPane scrollPane;
    private VBox contentContainer;
    private GridPane timelineGrid;
    
    // Fixed column components for resource labels
    private VBox fixedResourceColumn;
    private VBox fixedResourceContent;
    private Label fixedResourceHeader;
    private HBox mainContainer;
    private ScrollPane fixedColumnScrollPane;
    
    // Zoom property
    private final DoubleProperty zoomLevelProperty = new SimpleDoubleProperty(1.0);
    
    // Cache for performance
    private Map<LocalDate, Integer> dateColumnCache = new HashMap<>();
    
    // Callback handlers for context menu actions
    private Consumer<Project> onEditProject;
    private Consumer<Project> onManageOpenItems;
    private Consumer<Project> onDeleteProject;
    private Consumer<Project> onDeleteProjectWithAssignments;
    private Consumer<Resource> onEditResource;
    private Consumer<Resource> onDeleteResource;
    private Consumer<Resource> onMarkResourceUnavailable;
    private Consumer<Resource> onViewResourceUnavailability;
    private Consumer<Assignment> onEditAssignment;
    private Consumer<Assignment> onDeleteAssignment;
    private Consumer<Assignment> onDuplicateAssignment;
    private Consumer<Project> onShowProjectDetails;
    private Consumer<Resource> onShowResourceDetails;
    private BiConsumer<Project, ProjectStatus> onChangeProjectStatus;
    private Consumer<Project> onGenerateReport;
    private Consumer<Project> onShowProjectTasks;
    private Consumer<Project> onViewInReportCenter;
    private BiConsumer<String, Object> onApplyFilter;
    
    // Drag and drop state
    private Assignment draggedAssignment;
    private Label draggedBar;
    private Resource targetResource;
    private double dragStartOffsetX = 0; // Where in the bar the user clicked
    private LocalDate dragStartDate;
    private Label ghostPreview;
    
    public TimelineView() {
        this.getStyleClass().add("timeline-view");
        
        // OpenItemService will be set by controller
        
        initializeComponent();
        setupEventHandlers();
    }
    
    public void setDatabaseConfig(DatabaseConfig databaseConfig) {
        if (databaseConfig != null) {
            try {
                openItemService = new OpenItemService(databaseConfig);
            } catch (Exception e) {
                System.err.println("Could not initialize OpenItemService: " + e.getMessage());
            }
        }
    }
    
    private void initializeComponent() {
        // Create main horizontal container
        mainContainer = new HBox();
        mainContainer.getStyleClass().add("timeline-main-container");
        
        // Create fixed left column structure
        fixedResourceColumn = new VBox();
        fixedResourceColumn.setPrefWidth(resourceLabelWidth);
        fixedResourceColumn.setMinWidth(resourceLabelWidth);
        fixedResourceColumn.setMaxWidth(resourceLabelWidth);
        fixedResourceColumn.getStyleClass().add("fixed-resource-column");
        fixedResourceColumn.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 0 1 0 0;");
        
        // Create fixed header for resource column - match header row height exactly
        fixedResourceHeader = new Label("Resources");
        fixedResourceHeader.setPrefWidth(resourceLabelWidth);
        fixedResourceHeader.setMinWidth(resourceLabelWidth);
        fixedResourceHeader.setMaxWidth(resourceLabelWidth);
        fixedResourceHeader.setMinHeight(HEADER_HEIGHT);
        fixedResourceHeader.setPrefHeight(HEADER_HEIGHT);
        fixedResourceHeader.setMaxHeight(HEADER_HEIGHT);
        // Remove h5 style class which may add unwanted styling
        fixedResourceHeader.setAlignment(Pos.CENTER);
        fixedResourceHeader.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #d0d0d0; -fx-border-width: 0 1 1 0; -fx-padding: 0; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Create scrollable content area for resource labels
        fixedResourceContent = new VBox();
        fixedResourceContent.setSpacing(0);
        
        // Create scroll pane for fixed column content
        fixedColumnScrollPane = new ScrollPane(fixedResourceContent);
        fixedColumnScrollPane.setFitToWidth(true);
        fixedColumnScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fixedColumnScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Will scroll with main timeline
        fixedColumnScrollPane.getStyleClass().add("fixed-column-scroll");
        
        // Add header and scrollable content to fixed column
        fixedResourceColumn.getChildren().addAll(fixedResourceHeader, fixedColumnScrollPane);
        fixedResourceColumn.setSpacing(0); // Ensure no spacing
        VBox.setVgrow(fixedColumnScrollPane, Priority.ALWAYS);
        
        // Create scrollable right section
        // Create header row with date labels (no resource column now)
        headerRow = new HBox();
        headerRow.setAlignment(Pos.TOP_LEFT); // Align to top-left to match resource header
        headerRow.setSpacing(0); // No spacing between date cells
        headerRow.setMinHeight(HEADER_HEIGHT);
        headerRow.setPrefHeight(HEADER_HEIGHT);
        headerRow.setMaxHeight(HEADER_HEIGHT);
        headerRow.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 0;"); // Consistent background, no padding
        
        // Create main content area that includes header and timeline
        contentContainer = new VBox();
        contentContainer.setSpacing(0); // No gap between header and timeline grid
        contentContainer.getStyleClass().add("timeline-container");
        
        // Create timeline grid (no visible gridlines as they interfere with alignment)
        timelineGrid = new GridPane();
        timelineGrid.getStyleClass().add("timeline-grid");
        timelineGrid.setGridLinesVisible(false); // Grid lines can cause alignment issues
        timelineGrid.setHgap(0);
        timelineGrid.setVgap(0);
        
        // Add header row and timeline grid to scrollable container
        contentContainer.getChildren().addAll(headerRow, timelineGrid);
        
        // Create scroll pane for horizontal scrolling of timeline only
        scrollPane = new ScrollPane(contentContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("timeline-scroll-pane");
        
        // Synchronize vertical scrolling between fixed column and timeline
        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (fixedColumnScrollPane != null) {
                fixedColumnScrollPane.setVvalue(newVal.doubleValue());
            }
        });
        
        // Add fixed column and scrollable timeline to main container
        mainContainer.getChildren().addAll(fixedResourceColumn, scrollPane);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        
        // Create a header bar with help button
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_RIGHT);
        headerBar.setPadding(new Insets(5));
        Button helpBtn = HelpButton.create("Timeline View", HelpButton.HelpContent.TIMELINE_VIEW);
        headerBar.getChildren().add(helpBtn);
        
        // Add header bar and main container to this component
        this.getChildren().addAll(headerBar, mainContainer);
        VBox.setVgrow(mainContainer, Priority.ALWAYS);
        
        // Set initial date range (current month)
        LocalDate now = LocalDate.now();
        setDateRange(now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()));
    }
    
    private void setupEventHandlers() {
        // Listen for data changes
        projects.addListener((ListChangeListener<Project>) c -> Platform.runLater(this::refreshTimeline));
        resources.addListener((ListChangeListener<Resource>) c -> Platform.runLater(this::refreshTimeline));
        assignments.addListener((ListChangeListener<Assignment>) c -> Platform.runLater(this::refreshTimeline));
        
        // Listen for date range changes
        startDate.addListener((obs, oldVal, newVal) -> Platform.runLater(this::refreshTimeline));
        endDate.addListener((obs, oldVal, newVal) -> Platform.runLater(this::refreshTimeline));
        
        // Listen for zoom changes
        zoomLevelProperty.addListener((obs, oldVal, newVal) -> {
            setZoomLevel(newVal.doubleValue());
            Platform.runLater(this::refreshTimeline);
        });
    }
    
    public void setDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            logger.warn("Invalid date range: {} to {}", start, end);
            return;
        }
        
        this.startDate.set(start);
        this.endDate.set(end);
        
        // Auto-detect view mode based on date range
        long daysBetween = ChronoUnit.DAYS.between(start, end);
        if (daysBetween <= 7) {
            currentViewMode = ViewMode.WEEK;
        } else if (daysBetween <= 42) {  // 6 weeks = 42 days
            currentViewMode = ViewMode.MONTH;
        } else {
            currentViewMode = ViewMode.QUARTER;
        }
    }
    
    public void setViewMode(ViewMode mode) {
        this.currentViewMode = mode;
        refreshTimeline();
    }
    
    private void refreshTimeline() {
        LocalDate start = startDate.get();
        LocalDate end = endDate.get();
        
        if (start == null || end == null) {
            return;
        }
        
        logger.debug("Refreshing timeline from {} to {}", start, end);
        
        // Clean up any lingering ghost preview
        if (ghostPreview != null) {
            if (ghostPreview.getParent() != null) {
                ((Pane) ghostPreview.getParent()).getChildren().remove(ghostPreview);
            }
            ghostPreview = null;
        }
        
        // Clear existing content
        headerRow.getChildren().clear();
        timelineGrid.getChildren().clear();
        dateColumnCache.clear();
        
        // Clear and rebuild fixed resource content
        if (fixedResourceContent != null) {
            fixedResourceContent.getChildren().clear();
        }
        
        // Build date header (without resource column)
        buildDateHeader(start, end);
        
        // Build timeline content (including fixed resource labels)
        buildTimelineContent(start, end);
    }
    
    private void buildDateHeader(LocalDate start, LocalDate end) {
        long dayCount = ChronoUnit.DAYS.between(start, end) + 1;
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("d");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");
        DateTimeFormatter weekdayFormatter = DateTimeFormatter.ofPattern("EEE");
        DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("MMM d");
        
        for (long i = 0; i < dayCount; i++) {
            LocalDate date = start.plusDays(i);
            int columnIndex = (int) i; // No offset needed - resources are in fixed column
            dateColumnCache.put(date, columnIndex);
            
            Label dateLabel = new Label();
            // Don't add style class that might override height settings
            dateLabel.setPrefWidth(dayWidth);
            dateLabel.setMaxWidth(dayWidth);
            dateLabel.setMinWidth(dayWidth);
            dateLabel.setMinHeight(HEADER_HEIGHT);
            dateLabel.setPrefHeight(HEADER_HEIGHT);
            dateLabel.setMaxHeight(HEADER_HEIGHT);
            dateLabel.setAlignment(Pos.CENTER);
            
            // Check if this is a weekend, holiday, or today
            boolean isWeekend = date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || 
                               date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            boolean isHoliday = companyHolidays != null && companyHolidays.stream()
                .anyMatch(h -> h.getDate().equals(date));
            boolean isToday = date.equals(LocalDate.now());
            
            // Add visible border for day separation and ensure full height
            // Note: Only use bottom border to avoid cumulative width drift from right borders
            String baseStyle = "-fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0; -fx-padding: 0;";
            
            // Apply background colors to match timeline
            if (isToday) {
                // Today gets black background with white bold text
                baseStyle += " -fx-background-color: #000000; -fx-text-fill: white; -fx-font-weight: bold;";
            } else if (isHoliday) {
                // Light pastel pink/coral for holidays
                baseStyle += " -fx-background-color: #ffe0e6;";
            } else if (isWeekend) {
                // Light pastel blue for weekends
                baseStyle += " -fx-background-color: #e6f3ff;";
            } else {
                // Regular weekday background
                baseStyle += " -fx-background-color: #f5f5f5;";
            }
            
            // Format date display based on view mode and zoom level
            String displayText;
            switch (currentViewMode) {
                case WEEK:
                    // For week view, show weekday and date
                    if (zoomLevel >= 1.0) {
                        displayText = date.format(weekdayFormatter) + "\n" + date.format(fullDateFormatter);
                    } else {
                        displayText = date.format(weekdayFormatter) + "\n" + date.format(dayFormatter);
                    }
                    break;
                    
                case MONTH:
                    // For month view (6 weeks), show month name on first day of each month
                    // and at the start of the view
                    boolean isFirstDayOfMonth = date.getDayOfMonth() == 1;
                    boolean isFirstDayOfView = i == 0;
                    
                    if (isFirstDayOfMonth || isFirstDayOfView) {
                        // Show month name above day number
                        displayText = date.format(monthFormatter) + "\n" + date.format(dayFormatter);
                    } else {
                        displayText = date.format(dayFormatter);
                        // Show weekday initial if zoomed in enough
                        if (zoomLevel >= 1.25) {
                            displayText = date.format(weekdayFormatter).substring(0, 1) + "\n" + displayText;
                        }
                    }
                    break;
                    
                case QUARTER:
                    // For quarter view, show fewer labels
                    if (date.getDayOfMonth() == 1) {
                        displayText = date.format(monthFormatter) + "\n" + date.format(dayFormatter);
                    } else if (date.getDayOfMonth() % 5 == 0 || i == 0) {
                        displayText = date.format(dayFormatter);
                    } else {
                        displayText = "";
                    }
                    break;
                    
                default:
                    displayText = date.format(dayFormatter);
            }
            dateLabel.setText(displayText);
            
            dateLabel.setStyle(baseStyle);
            headerRow.getChildren().add(dateLabel);
        }
    }
    
    private void buildTimelineContent(LocalDate start, LocalDate end) {
        int rowIndex = 0;
        
        // First, add rows for unassigned projects (one row per project)
        List<Project> unassignedProjects = findUnassignedProjects(start, end);
        for (Project unassignedProject : unassignedProjects) {
            createUnassignedProjectRow(unassignedProject, start, end, rowIndex);
            rowIndex++;
        }
        
        if (resources.isEmpty()) {
            // Show message when no resources (but only if no unassigned projects either)
            if (unassignedProjects.isEmpty()) {
                Label emptyLabel = new Label("No resources or projects to display");
                emptyLabel.getStyleClass().add("text-muted");
                timelineGrid.add(emptyLabel, 0, 0, (int)(ChronoUnit.DAYS.between(start, end) + 1), 1);
                return;
            }
        }
        for (Resource resource : resources) {
            // Calculate resource metrics (utilization and billable)
            ResourceMetrics metrics = calculateResourceMetrics(resource, start, end);
            double utilizationPercentage = metrics.utilizationPercent;
            
            // Create container using StackPane to layer elements without spacing issues
            StackPane resourceContainer = new StackPane();
            resourceContainer.setPrefWidth(resourceLabelWidth);
            resourceContainer.setMaxWidth(resourceLabelWidth);
            resourceContainer.setMinWidth(resourceLabelWidth);
            // Minimal padding to keep content close together
            resourceContainer.setPadding(new Insets(3, 5, 3, 5));
            resourceContainer.setMinHeight(rowHeight);
            resourceContainer.setPrefHeight(rowHeight);
            resourceContainer.setMaxHeight(rowHeight);
            resourceContainer.setAlignment(Pos.CENTER_LEFT);
            // Clip content to prevent overflow
            resourceContainer.setStyle("-fx-background-clip: padding-box;");
            
            // Apply alternating row background to match timeline
            if (rowIndex % 2 == 0) {
                resourceContainer.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
            } else {
                resourceContainer.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
            }
            
            // Create a single label with both utilization and billable percentages
            String resourceText = String.format("%s\nUtil: %.0f%% | Bill: %.0f%%", 
                resource.getName(), 
                metrics.utilizationPercent,
                metrics.billablePercent);
            Label resourceLabel = new Label(resourceText);
            resourceLabel.setAlignment(Pos.CENTER_LEFT);
            resourceLabel.setPrefWidth(resourceLabelWidth - 10);
            resourceLabel.setMaxWidth(resourceLabelWidth - 10);
            // Adjust height scaling - at higher zoom levels, don't subtract as much
            double heightReduction = (zoomLevel >= 1.5) ? 5 : 10;
            resourceLabel.setPrefHeight(rowHeight - heightReduction);
            resourceLabel.setPadding(new Insets(5, 5, 5, 5));
            resourceLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            
            // Determine bar color based on utilization and billable percentage
            String barColor;
            String barColorHex;
            if (utilizationPercentage > 100) {
                barColorHex = "#dc3545"; // Red for overallocated
                barColor = "rgba(220, 53, 69, 0.3)";
            } else if (utilizationPercentage > 80) {
                // High utilization - check if it's mostly billable
                if (metrics.billablePercent > 60) {
                    barColorHex = "#28a745"; // Green for high billable utilization
                    barColor = "rgba(40, 167, 69, 0.3)";
                } else {
                    barColorHex = "#ffc107"; // Yellow for high utilization but low billable
                    barColor = "rgba(255, 193, 7, 0.3)";
                }
            } else if (utilizationPercentage > 50) {
                // Medium utilization
                if (metrics.billablePercent > 60) {
                    barColorHex = "#28a745"; // Green for good billable utilization
                    barColor = "rgba(40, 167, 69, 0.3)";
                } else {
                    barColorHex = "#17a2b8"; // Blue for medium utilization, low billable
                    barColor = "rgba(23, 162, 184, 0.3)";
                }
            } else {
                barColorHex = "#6c757d"; // Gray for low utilization
                barColor = "rgba(108, 117, 125, 0.3)";
            }
            
            // Calculate progress width (cap at 100% for display)
            double progressWidth = Math.min(utilizationPercentage, 100);
            
            // Apply style with integrated progress bar as background
            double fontSize = 11 * Math.min(zoomLevel, 1.5);
            String style = String.format(
                "-fx-font-size: %spx; " +
                "-fx-font-weight: bold; " +
                "-fx-background-color: linear-gradient(to right, %s 0%%, %s %s%%, transparent %s%%, transparent 100%%); " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 0 0 2 0; " +
                "-fx-border-radius: 2; " +
                "-fx-background-radius: 2;",
                fontSize, barColor, barColor, progressWidth, progressWidth, barColorHex
            );
            resourceLabel.setStyle(style);
            
            // Add the label directly to the container
            resourceContainer.getChildren().add(resourceLabel);
            
            // Add right-click context menu for resource
            resourceContainer.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY) {
                    showResourceContextMenu(resource, resourceLabel, event.getScreenX(), event.getScreenY());
                    event.consume();
                }
            });
            
            // Add resource container to fixed column content instead of grid
            fixedResourceContent.getChildren().add(resourceContainer);
            
            // Create timeline row for this resource
            createTimelineRow(resource, start, end, rowIndex);
            
            rowIndex++;
        }
        
        // Set up drop targets for drag and drop after all rows are created
        setupDropTargets();
    }
    
    private void createTimelineRow(Resource resource, LocalDate start, LocalDate end, int rowIndex) {
        // Get assignments for this resource
        List<Assignment> resourceAssignments = assignments.stream()
            .filter(a -> a.getResourceId().equals(resource.getId()))
            .filter(a -> {
                // Check if assignment overlaps with timeline range
                LocalDate assignStart = a.getStartDate();
                LocalDate assignEnd = a.getEndDate();
                return !(assignEnd.isBefore(start) || assignStart.isAfter(end));
            })
            .toList();
        
        // Get unavailabilities for this resource
        List<TechnicianUnavailability> resourceUnavailabilities = unavailabilities.stream()
            .filter(u -> u.getResourceId().equals(resource.getId()))
            .filter(u -> {
                // Check if unavailability overlaps with timeline range
                LocalDate unavailStart = u.getStartDate();
                LocalDate unavailEnd = u.getEndDate();
                return !(unavailEnd.isBefore(start) || unavailStart.isAfter(end));
            })
            .toList();
        
        // Create background for the entire row
        long dayCount = ChronoUnit.DAYS.between(start, end) + 1;
        Pane rowContainer = new Pane();  // Changed from HBox to Pane for absolute positioning
        rowContainer.setPrefHeight(rowHeight);
        rowContainer.setMaxHeight(rowHeight);
        rowContainer.setMinHeight(rowHeight);
        rowContainer.setPrefWidth(dayCount * dayWidth);
        rowContainer.setMaxWidth(dayCount * dayWidth);
        rowContainer.setMinWidth(dayCount * dayWidth);
        
        // Make row container transparent so day cell borders show through
        rowContainer.setStyle("-fx-background-color: transparent;");
        
        // Add assignment bars first (they appear behind)
        for (Assignment assignment : resourceAssignments) {
            // Check if assignment should be shown based on unavailability settings
            if (shouldShowAssignment(assignment, resourceUnavailabilities)) {
                boolean hasConflict = showUnavailability && hasUnavailabilityConflict(assignment, resourceUnavailabilities);
                createAssignmentBar(assignment, start, end, rowContainer, rowIndex, hasConflict);
            }
        }
        
        // Add unavailability bars on top (they appear in front) - only if toggle is on
        if (showUnavailability) {
            for (TechnicianUnavailability unavailability : resourceUnavailabilities) {
                createUnavailabilityBar(unavailability, start, end, rowContainer, rowIndex);
            }
        }
        
        // Add individual day cells with borders for grid visibility
        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            LocalDate cellDate = start.plusDays(dayIndex);
            Pane dayCell = new Pane();
            dayCell.setPrefWidth(dayWidth);
            dayCell.setMaxWidth(dayWidth);
            dayCell.setMinWidth(dayWidth);
            dayCell.setMinHeight(rowHeight);
            dayCell.setPrefHeight(rowHeight);
            dayCell.setMaxHeight(rowHeight);
            
            // Build style with borders and background based on day type
            // Only use bottom border to avoid cumulative width drift from right borders
            String cellStyle = "-fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;";
            
            // Check if this is a weekend or holiday
            boolean isWeekend = cellDate.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || 
                               cellDate.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            boolean isHoliday = companyHolidays != null && companyHolidays.stream()
                .anyMatch(h -> h.getDate().equals(cellDate));
            
            if (isHoliday) {
                // Light pastel pink/coral for holidays
                cellStyle += " -fx-background-color: #ffe0e6;";
            } else if (isWeekend) {
                // Light pastel blue for weekends
                cellStyle += " -fx-background-color: #e6f3ff;";
            } else if (rowIndex % 2 == 0) {
                // Regular alternating row color
                cellStyle += " -fx-background-color: #f8f9fa;";
            } else {
                cellStyle += " -fx-background-color: white;";
            }
            dayCell.setStyle(cellStyle);
            
            timelineGrid.add(dayCell, dayIndex, rowIndex);
        }
        
        // Add the row container with assignment bars on top
        timelineGrid.add(rowContainer, 0, rowIndex, (int)dayCount, 1);
    }
    
    private void createAssignmentBar(Assignment assignment, LocalDate timelineStart, LocalDate timelineEnd, 
                                   Pane rowContainer, int rowIndex) {
        createAssignmentBar(assignment, timelineStart, timelineEnd, rowContainer, rowIndex, false);
    }
    
    private void createAssignmentBar(Assignment assignment, LocalDate timelineStart, LocalDate timelineEnd, 
                                   Pane rowContainer, int rowIndex, boolean hasUnavailabilityConflict) {
        
        // Calculate assignment bar positioning
        LocalDate assignStart = assignment.getStartDate();
        LocalDate assignEnd = assignment.getEndDate();
        
        // Clamp assignment dates to timeline range
        LocalDate barStart = assignStart.isBefore(timelineStart) ? timelineStart : assignStart;
        LocalDate barEnd = assignEnd.isAfter(timelineEnd) ? timelineEnd : assignEnd;
        
        // Skip if assignment is completely outside the timeline range
        if (barStart.isAfter(timelineEnd) || barEnd.isBefore(timelineStart)) {
            return;
        }
        
        long startDayOffset = ChronoUnit.DAYS.between(timelineStart, barStart);
        long barDuration = ChronoUnit.DAYS.between(barStart, barEnd) + 1;
        
        // Ensure bar doesn't extend beyond the calendar grid
        long maxDaysInView = ChronoUnit.DAYS.between(timelineStart, timelineEnd) + 1;
        if (startDayOffset + barDuration > maxDaysInView) {
            barDuration = maxDaysInView - startDayOffset;
        }
        
        logger.debug("Creating assignment bar - ID: {}, Dates: {} to {}, Timeline: {} to {}", 
            assignment.getId(), assignStart, assignEnd, timelineStart, timelineEnd);
        logger.debug("  Bar positioning: offset={} days, duration={} days, pixelOffset={}, pixelWidth={}", 
            startDayOffset, barDuration, startDayOffset * dayWidth, barDuration * dayWidth);
        
        if (barDuration <= 0) {
            return; // No visible portion
        }
        
        // Get project info for styling
        Project project = projects.stream()
            .filter(p -> p.getId().equals(assignment.getProjectId()))
            .findFirst()
            .orElse(null);
        
        // Create a container for the assignment bar with resize handles
        StackPane barContainer = new StackPane();
        barContainer.setPrefWidth(barDuration * dayWidth);
        barContainer.setMaxWidth(barDuration * dayWidth);
        barContainer.setMinWidth(barDuration * dayWidth);
        barContainer.setPrefHeight(projectBarHeight);
        barContainer.setMaxHeight(projectBarHeight);
        barContainer.setMinHeight(projectBarHeight);
        
        // Create assignment bar
        Label assignmentBar = new Label();
        // Combine project ID, description, location, and address with line breaks
        String projectId = project != null ? project.getProjectId() : "Unknown";
        String description = project != null && project.getDescription() != null ? project.getDescription() : "";
        String location = assignment.getLocation() != null && !assignment.getLocation().isEmpty() ? 
                         " @ " + assignment.getLocation() : "";
        String address = project != null && project.getContactAddress() != null ? project.getContactAddress() : "";
        
        // Create multi-line text with project ID (+ location) on first line, description on second, address on third
        String displayText;
        boolean hasDescription = !description.isEmpty() && barDuration >= 3; // Only show description if bar is wide enough
        boolean hasAddress = !address.isEmpty() && barDuration >= 4; // Show address if bar is 4+ days wide
        
        // Special handling for SHOP projects - only show description
        if ("SHOP".equalsIgnoreCase(projectId) || "TRAINING".equalsIgnoreCase(projectId)) {
            displayText = description.isEmpty() ? projectId : description;
        } else {
            // Determine display text first for logging
            if (hasDescription && hasAddress) {
                // Truncate description and address if they're too long
                String truncatedDesc = description.length() > 30 ? description.substring(0, 27) + "..." : description;
                String truncatedAddr = address.length() > 30 ? address.substring(0, 27) + "..." : address;
                displayText = projectId + location + "\n" + truncatedDesc + "\n" + truncatedAddr;
            } else if (hasDescription) {
                // Truncate description if it's too long
                String truncatedDesc = description.length() > 30 ? description.substring(0, 27) + "..." : description;
                displayText = projectId + location + "\n" + truncatedDesc;
            } else {
                displayText = projectId + location;
            }
        }
        
        // Enhanced debug logging for CH-PBLD-2025-097
        if (projectId.equals("CH-PBLD-2025-097")) {
            logger.info("====== CH-PBLD-2025-097 Detailed Info ======");
            logger.info("Project Details:");
            logger.info("  - Project ID: {}", projectId);
            logger.info("  - Project Description: '{}'", project.getDescription());
            logger.info("  - Project Start: {}", project.getStartDate());
            logger.info("  - Project End: {}", project.getEndDate());
            logger.info("  - Project Duration: {} days", 
                java.time.temporal.ChronoUnit.DAYS.between(project.getStartDate(), project.getEndDate()) + 1);
            logger.info("Assignment Details:");
            logger.info("  - Assignment Start: {}", startDate);
            logger.info("  - Assignment End: {}", endDate);
            logger.info("  - Assignment Duration: {} days", barDuration);
            logger.info("  - Resource ID: {}", assignment.getResourceId());
            logger.info("Display Info:");
            logger.info("  - Bar width: {} pixels", barDuration * dayWidth);
            logger.info("  - Address from project: '{}'", address);
            logger.info("  - Address empty check: {}", address.isEmpty());
            logger.info("  - Has description: {} (needs 3+ days)", hasDescription);
            logger.info("  - Has address: {} (needs 4+ days)", hasAddress);
            logger.info("  - Final display text: '{}'", displayText.replace("\n", " | "));
            logger.info("==========================================");
        } else {
            // Regular debug logging
            logger.debug("Project {} - Description: '{}', Address: '{}', Bar duration: {} days, Width: {} pixels", 
                projectId, description, address, barDuration, barDuration * dayWidth);
            if (!address.isEmpty()) {
                logger.debug("Project {} has address: '{}', bar duration: {} days, will show: {}", 
                    projectId, address, barDuration, hasAddress);
            }
        }
        
        // Use consistent, readable font size for all bars
        double barWidth = barDuration * dayWidth;
        double fontSize = 10 * zoomLevel; // Consistent 10px base font size
        
        // Adjust display text based on bar width - but preserve multi-line format
        String finalDisplayText = displayText;
        if (barWidth < 80) {
            // Show only project ID or abbreviated version
            finalDisplayText = projectId;
            if (projectId.length() > 10 && barWidth < 60) {
                // Show even shorter version for very narrow bars
                String[] parts = projectId.split("-");
                if (parts.length >= 3) {
                    finalDisplayText = parts[0] + "-" + parts[parts.length - 1]; // e.g., "CH-093"
                }
            }
        } else if (barWidth < 120) {
            // Show only project ID for medium bars
            finalDisplayText = projectId;
        }
        // For wider bars (>= 120 pixels), keep the full multi-line text as already set
        
        // Debug log final text
        if (!finalDisplayText.equals(displayText)) {
            logger.debug("Display text adjusted for bar width {} from '{}' to '{}'", 
                barWidth, displayText.replace("\n", " | "), finalDisplayText.replace("\n", " | "));
        }
        
        // Set the display text (may be abbreviated)
        assignmentBar.setText(finalDisplayText);
        assignmentBar.getStyleClass().add("assignment-bar");
        assignmentBar.setPrefWidth(barDuration * dayWidth);
        assignmentBar.setMaxWidth(barDuration * dayWidth);
        assignmentBar.setMinWidth(barDuration * dayWidth);
        assignmentBar.setPrefHeight(projectBarHeight);
        assignmentBar.setMaxHeight(projectBarHeight);
        assignmentBar.setMinHeight(projectBarHeight);
        assignmentBar.setAlignment(Pos.CENTER);
        assignmentBar.setTextAlignment(TextAlignment.CENTER); // Center text alignment for multi-line
        assignmentBar.setWrapText(false); // Don't wrap, we control line breaks
        assignmentBar.setPadding(new Insets(2, 4, 2, 4)); // Minimal padding for more text space
        
        // Create left resize handle
        Region leftHandle = new Region();
        leftHandle.setPrefWidth(10);
        leftHandle.setMaxWidth(10);
        leftHandle.setMinWidth(10);
        leftHandle.setPrefHeight(projectBarHeight);
        leftHandle.setCursor(Cursor.W_RESIZE);
        // Make handles slightly visible for debugging
        leftHandle.setStyle("-fx-background-color: rgba(0, 0, 255, 0.2);");
        
        // Create right resize handle
        Region rightHandle = new Region();
        rightHandle.setPrefWidth(10);
        rightHandle.setMaxWidth(10);
        rightHandle.setMinWidth(10);
        rightHandle.setPrefHeight(projectBarHeight);
        rightHandle.setCursor(Cursor.E_RESIZE);
        // Make handles slightly visible for debugging
        rightHandle.setStyle("-fx-background-color: rgba(255, 0, 0, 0.2);");
        
        // Add components to container
        barContainer.getChildren().addAll(assignmentBar, leftHandle, rightHandle);
        StackPane.setAlignment(leftHandle, Pos.CENTER_LEFT);
        StackPane.setAlignment(rightHandle, Pos.CENTER_RIGHT);
        
        // Note: positioning is handled by setLayoutX/Y below, not margins
        // since rowContainer is a Pane, not an HBox
        
        // Color coding: Unavailability conflicts take priority, then regular conflicts, then project status
        if (hasUnavailabilityConflict && !("SHOP".equalsIgnoreCase(project != null ? project.getProjectId() : "") || 
            "TRAINING".equalsIgnoreCase(project != null ? project.getProjectId() : ""))) {
            // Unavailability conflict for project assignments (not shop/training) - purple/magenta striped
            String conflictText = displayText.contains("\n") ? 
                "🚫 " + displayText : 
                "🚫 " + displayText;
            assignmentBar.setText(conflictText);
            
            String lineSpacing = displayText.contains("\n") ? "-fx-line-spacing: -3; " : "";
            assignmentBar.setStyle(
                "-fx-background-color: " +
                    "repeating-linear-gradient(45deg, #9b59b6 0px, #9b59b6 5px, #c39bd3 5px, #c39bd3 10px); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: " + fontSize + "px; " +
                lineSpacing +
                "-fx-border-color: #6c3483; " +
                "-fx-border-width: 2; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(155,89,182,0.8), 6, 0.0, 0, 2);"
            );
            assignmentBar.getStyleClass().add("unavailability-conflict");
        } else if (isConflicted(assignment.getId())) {
            // Conflict styling - red background with striped pattern and warning icon
            // Add warning icon only to first line (project ID)
            String conflictText = displayText.contains("\n") ? 
                "⚠ " + displayText : 
                "⚠ " + displayText;
            assignmentBar.setText(conflictText);
            
            String lineSpacing = displayText.contains("\n") ? "-fx-line-spacing: -3; " : "";
            assignmentBar.setStyle(
                "-fx-background-color: " +
                    "repeating-linear-gradient(from 0px 0px to 10px 10px, " +
                    "#dc3545 0px, #dc3545 5px, #ff6b6b 5px, #ff6b6b 10px); " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: " + fontSize + "px; " +
                lineSpacing +
                "-fx-border-color: #721c24; " +
                "-fx-border-width: 2; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(220,53,69,0.8), 6, 0.0, 0, 2);"
            );
            assignmentBar.getStyleClass().add("conflict-indicator");
        } else if (project != null) {
            // Normal project status color coding
            String lineSpacing = displayText.contains("\n") ? "-fx-line-spacing: -3; " : "";
            String baseStyle = "-fx-font-size: " + fontSize + "px; " + lineSpacing;
            
            // Special styling for SHOP and TRAINING projects
            if ("SHOP".equalsIgnoreCase(project.getProjectId()) || "TRAINING".equalsIgnoreCase(project.getProjectId())) {
                // Bright yellow background (#F0D000 is a cleaner, more yellow gold) with black text
                assignmentBar.setStyle(baseStyle + "-fx-background-color: #F0D000; -fx-text-fill: black;");
            } else {
                // Color coding based on travel status
                if (project.isTravel()) {
                    // Travel projects are green
                    assignmentBar.setStyle(baseStyle + "-fx-background-color: #28a745; -fx-text-fill: white;");
                } else {
                    // Non-travel projects are pastel blue
                    assignmentBar.setStyle(baseStyle + "-fx-background-color: #87CEEB; -fx-text-fill: black;");
                }
            }
        }
        
        // Add comprehensive tooltip with all assignment and project details
        StringBuilder tooltipBuilder = new StringBuilder();
        
        if (isConflicted(assignment.getId())) {
            tooltipBuilder.append("⚠️ RESOURCE CONFLICT ⚠️\n");
            tooltipBuilder.append("This resource is double-booked!\n\n");
        }
        
        // Always show full project information in tooltip
        if (project != null) {
            tooltipBuilder.append("Project: ").append(project.getProjectId()).append("\n");
            tooltipBuilder.append("Description: ").append(project.getDescription() != null ? project.getDescription() : "N/A").append("\n");
            
            // Add Project Manager name
            if (project.getProjectManagerId() != null && projectManagerMap.containsKey(project.getProjectManagerId())) {
                com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager pm = projectManagerMap.get(project.getProjectManagerId());
                tooltipBuilder.append("Project Manager: ").append(pm.getName()).append("\n");
            } else {
                tooltipBuilder.append("Project Manager: Not assigned\n");
            }
            
            if (project.getContactAddress() != null && !project.getContactAddress().isEmpty()) {
                tooltipBuilder.append("Address: ").append(project.getContactAddress()).append("\n");
            }
            tooltipBuilder.append("Status: ").append(project.getStatus()).append("\n");
            tooltipBuilder.append("Travel: ").append(project.isTravel() ? "Yes" : "No").append("\n");
            
            // Add open items count
            if (openItemService != null) {
                try {
                    var openItems = openItemService.getItemsByProject(project.getId());
                    int openItemsCount = openItems.size();
                    tooltipBuilder.append("Open Items: ").append(openItemsCount);
                    
                    if (openItemsCount > 0) {
                        // Get breakdown by status
                        int notStarted = (int) openItems.stream()
                            .filter(item -> item.getStatus() == com.subliminalsearch.simpleprojectresourcemanager.model.OpenItem.ItemStatus.NOT_STARTED)
                            .count();
                        int inProgress = (int) openItems.stream()
                            .filter(item -> item.getStatus() == com.subliminalsearch.simpleprojectresourcemanager.model.OpenItem.ItemStatus.IN_PROGRESS)
                            .count();
                        int blocked = (int) openItems.stream()
                            .filter(item -> item.getStatus() == com.subliminalsearch.simpleprojectresourcemanager.model.OpenItem.ItemStatus.BLOCKED)
                            .count();
                        
                        tooltipBuilder.append(" (");
                        boolean needComma = false;
                        if (notStarted > 0) {
                            tooltipBuilder.append(notStarted).append(" pending");
                            needComma = true;
                        }
                        if (inProgress > 0) {
                            if (needComma) tooltipBuilder.append(", ");
                            tooltipBuilder.append(inProgress).append(" active");
                            needComma = true;
                        }
                        if (blocked > 0) {
                            if (needComma) tooltipBuilder.append(", ");
                            tooltipBuilder.append(blocked).append(" blocked");
                        }
                        tooltipBuilder.append(")");
                    }
                    tooltipBuilder.append("\n");
                } catch (Exception e) {
                    // Don't show open items if there's an error
                }
            }
        } else {
            tooltipBuilder.append("Project: Unknown\n");
        }
        
        tooltipBuilder.append("\nAssignment Details:\n");
        tooltipBuilder.append(String.format("Dates: %s to %s\n", 
            assignment.getStartDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
            assignment.getEndDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        ));
        
        // Add location if present
        if (assignment.getLocation() != null && !assignment.getLocation().trim().isEmpty()) {
            tooltipBuilder.append("Location/Phase: ").append(assignment.getLocation()).append("\n");
        }
        
        // Calculate duration
        long duration = ChronoUnit.DAYS.between(assignment.getStartDate(), assignment.getEndDate()) + 1;
        tooltipBuilder.append(String.format("Duration: %d days\n", duration));
        
        // Add resource info
        Resource assignedResource = resources.stream()
            .filter(r -> r.getId().equals(assignment.getResourceId()))
            .findFirst()
            .orElse(null);
        if (assignedResource != null) {
            tooltipBuilder.append("Resource: ").append(assignedResource.getName()).append("\n");
        }
        
        if (assignment.getNotes() != null && !assignment.getNotes().trim().isEmpty()) {
            tooltipBuilder.append("\nNotes: ").append(assignment.getNotes());
        }
        
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipBuilder.toString());
        // Configure tooltip to stay visible while hovering but hide when mouse leaves
        tooltip.setShowDelay(javafx.util.Duration.millis(500)); // Show after 500ms hover
        tooltip.setHideDelay(javafx.util.Duration.millis(100)); // Hide quickly after mouse leaves
        tooltip.setShowDuration(javafx.util.Duration.seconds(3600)); // Stay visible for 1 hour while hovering
        
        if (isConflicted(assignment.getId())) {
            tooltip.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-size: 11px;");
        } else {
            tooltip.setStyle("-fx-font-size: 11px;"); // Slightly larger font for readability
        }
        assignmentBar.setTooltip(tooltip);
        
        // Setup edge resize handlers
        setupEdgeResize(leftHandle, rightHandle, barContainer, assignment, assignmentBar, timelineStart);
        
        // Store assignment data that may be updated by edge drags
        barContainer.setUserData(assignment);
        
        // Add right-click context menu for assignment
        assignmentBar.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                // Get the potentially updated assignment from the container
                Assignment currentAssignment = (Assignment) barContainer.getUserData();
                showAssignmentContextMenu(currentAssignment, project, assignmentBar, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
        
        // Enable drag and drop for rescheduling
        setupDragAndDrop(assignmentBar, assignment);
        
        // Position the bar at the correct horizontal offset
        barContainer.setLayoutX(startDayOffset * dayWidth);
        barContainer.setLayoutY((rowHeight - projectBarHeight) / 2); // Center vertically in row
        
        rowContainer.getChildren().add(barContainer);
    }
    
    private boolean shouldShowAssignment(Assignment assignment, List<TechnicianUnavailability> unavailabilities) {
        if (!showUnavailability) {
            // If unavailability is not shown, show all assignments
            return true;
        }
        
        Project project = projects.stream()
            .filter(p -> p.getId().equals(assignment.getProjectId()))
            .findFirst()
            .orElse(null);
            
        // Shop and Training assignments should be hidden during unavailability
        if (project != null && ("SHOP".equalsIgnoreCase(project.getProjectId()) || 
            "TRAINING".equalsIgnoreCase(project.getProjectId()))) {
            // Check if this assignment overlaps with any unavailability
            return !hasUnavailabilityConflict(assignment, unavailabilities);
        }
        
        // Regular project assignments are always shown (but may be marked as conflicted)
        return true;
    }
    
    private boolean hasUnavailabilityConflict(Assignment assignment, List<TechnicianUnavailability> unavailabilities) {
        LocalDate assignStart = assignment.getStartDate();
        LocalDate assignEnd = assignment.getEndDate();
        
        for (TechnicianUnavailability unavailability : unavailabilities) {
            LocalDate unavailStart = unavailability.getStartDate();
            LocalDate unavailEnd = unavailability.getEndDate();
            
            // Check for overlap
            if (!(assignEnd.isBefore(unavailStart) || assignStart.isAfter(unavailEnd))) {
                return true;
            }
        }
        return false;
    }
    
    private void createUnavailabilityBar(TechnicianUnavailability unavailability, LocalDate timelineStart, 
                                        LocalDate timelineEnd, Pane rowContainer, int rowIndex) {
        // Calculate unavailability bar positioning
        LocalDate unavailStart = unavailability.getStartDate();
        LocalDate unavailEnd = unavailability.getEndDate();
        
        // Clamp unavailability dates to timeline range
        LocalDate barStart = unavailStart.isBefore(timelineStart) ? timelineStart : unavailStart;
        LocalDate barEnd = unavailEnd.isAfter(timelineEnd) ? timelineEnd : unavailEnd;
        
        long startDayOffset = ChronoUnit.DAYS.between(timelineStart, barStart);
        long barDuration = ChronoUnit.DAYS.between(barStart, barEnd) + 1;
        
        if (barDuration <= 0) {
            return; // No visible portion
        }
        
        // Create unavailability bar
        Label unavailabilityBar = new Label();
        unavailabilityBar.setPrefWidth(barDuration * dayWidth);
        unavailabilityBar.setMaxWidth(barDuration * dayWidth);
        unavailabilityBar.setMinWidth(barDuration * dayWidth);
        unavailabilityBar.setPrefHeight(rowHeight - 4); // Full row height minus small margin
        unavailabilityBar.setMaxHeight(rowHeight - 4);
        unavailabilityBar.setMinHeight(rowHeight - 4);
        
        // Set text based on unavailability type
        String typeText = unavailability.getType().getDisplayName();
        String reasonText = unavailability.getReason() != null ? unavailability.getReason() : "";
        String displayText = typeText;
        if (!reasonText.isEmpty() && barDuration >= 3) { // Show reason if bar is wide enough
            displayText = typeText + ": " + (reasonText.length() > 20 ? reasonText.substring(0, 17) + "..." : reasonText);
        }
        unavailabilityBar.setText(displayText);
        unavailabilityBar.setAlignment(Pos.CENTER);
        unavailabilityBar.setTextAlignment(TextAlignment.CENTER);
        
        // Style based on unavailability type - all using light violet as requested
        String baseStyle = "-fx-font-size: " + (11 * zoomLevel) + "px; " +
                          "-fx-font-weight: bold; ";
        
        // Light violet color scheme for all unavailability types
        String backgroundColor = "#E6D5FF";  // Light violet/lavender
        String borderColor = "#B794F4";      // Slightly darker violet for border
        String textColor = "#4A148C";        // Dark purple for text contrast
        
        // Optional: Keep different shades of violet for different types
        switch (unavailability.getType()) {
            case VACATION:
                backgroundColor = "#E6D5FF";  // Light violet
                borderColor = "#B794F4";
                break;
            case SICK_LEAVE:
                backgroundColor = "#EDE7F6";  // Very light violet
                borderColor = "#B39DDB";
                break;
            case TRAINING:
                backgroundColor = "#F3E5F5";  // Ultra light violet
                borderColor = "#CE93D8";
                break;
            case PERSONAL_TIME:
                backgroundColor = "#E1BEE7";  // Light purple-violet
                borderColor = "#BA68C8";
                break;
            case OTHER_ASSIGNMENT:
                backgroundColor = "#DDD6FE";  // Light indigo-violet
                borderColor = "#A78BFA";
                break;
            case RECURRING:
                backgroundColor = "#E9D5FF";  // Light lavender
                borderColor = "#C084FC";
                break;
            case EMERGENCY:
                backgroundColor = "#FCE4EC";  // Light pink-violet
                borderColor = "#F48FB1";
                break;
            default:
                backgroundColor = "#E6D5FF";  // Default light violet
                borderColor = "#B794F4";
                break;
        }
        
        // Apply styling with semi-transparent background to see conflicts underneath
        unavailabilityBar.setStyle(baseStyle + 
            "-fx-background-color: " + backgroundColor + "; " +
            "-fx-text-fill: " + textColor + "; " +
            "-fx-opacity: 0.75; " +  // More transparent to show assignments underneath
            "-fx-border-color: " + borderColor + "; " +
            "-fx-border-width: 2; " +  // Thicker border for emphasis
            "-fx-border-style: solid; " +  // Solid border for better visibility
            "-fx-padding: 2;"
        );
        
        // Add tooltip with unavailability details
        StringBuilder tooltipBuilder = new StringBuilder();
        tooltipBuilder.append("Unavailability: ").append(unavailability.getType().getDisplayName()).append("\n");
        tooltipBuilder.append("Dates: ").append(
            unavailability.getStartDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))).append(" to ").append(
            unavailability.getEndDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))).append("\n");
        if (unavailability.getReason() != null) {
            tooltipBuilder.append("Reason: ").append(unavailability.getReason()).append("\n");
        }
        if (!unavailability.isApproved()) {
            tooltipBuilder.append("Status: Pending Approval");
        } else {
            tooltipBuilder.append("Status: Approved");
            if (unavailability.getApprovedBy() != null) {
                tooltipBuilder.append(" by ").append(unavailability.getApprovedBy());
            }
        }
        
        Tooltip tooltip = new Tooltip(tooltipBuilder.toString());
        // Configure tooltip to stay visible while hovering but hide when mouse leaves
        tooltip.setShowDelay(javafx.util.Duration.millis(500)); // Show after 500ms hover
        tooltip.setHideDelay(javafx.util.Duration.millis(100)); // Hide quickly after mouse leaves
        tooltip.setShowDuration(javafx.util.Duration.seconds(3600)); // Stay visible for 1 hour while hovering
        tooltip.setStyle("-fx-font-size: 11px;"); // Slightly larger font for readability
        unavailabilityBar.setTooltip(tooltip);
        
        // Position the unavailability bar at the correct horizontal offset
        unavailabilityBar.setLayoutX(startDayOffset * dayWidth);
        unavailabilityBar.setLayoutY(2); // Small margin from top
        
        rowContainer.getChildren().add(unavailabilityBar);
    }
    
    // Context menu methods
    private void showResourceContextMenu(Resource resource, Label resourceLabel, double screenX, double screenY) {
        ContextMenu contextMenu = new ContextMenu();
        
        // Details menu item
        MenuItem detailsItem = new MenuItem("View Details");
        detailsItem.setOnAction(e -> {
            if (onShowResourceDetails != null) {
                onShowResourceDetails.accept(resource);
            }
        });
        
        // Edit menu item
        MenuItem editItem = new MenuItem("Edit Resource");
        editItem.setOnAction(e -> {
            if (onEditResource != null) {
                onEditResource.accept(resource);
            }
        });
        
        // Delete menu item
        MenuItem deleteItem = new MenuItem("Delete Resource");
        deleteItem.setOnAction(e -> {
            if (onDeleteResource != null) {
                onDeleteResource.accept(resource);
            }
        });
        
        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();
        
        // Status toggle
        MenuItem statusItem = new MenuItem(resource.isActive() ? "Deactivate" : "Activate");
        statusItem.setOnAction(e -> {
            resource.setActive(!resource.isActive());
            if (onEditResource != null) {
                onEditResource.accept(resource);
            }
        });
        
        // Mark Unavailable menu item
        MenuItem markUnavailableItem = new MenuItem("Mark Unavailable...");
        markUnavailableItem.setOnAction(e -> {
            if (onMarkResourceUnavailable != null) {
                onMarkResourceUnavailable.accept(resource);
            }
        });
        
        // View Unavailability menu item
        MenuItem viewUnavailabilityItem = new MenuItem("View Unavailability");
        viewUnavailabilityItem.setOnAction(e -> {
            if (onViewResourceUnavailability != null) {
                onViewResourceUnavailability.accept(resource);
            }
        });
        
        contextMenu.getItems().addAll(
            detailsItem, 
            separator, 
            editItem, 
            deleteItem, 
            new SeparatorMenuItem(), 
            markUnavailableItem,
            viewUnavailabilityItem,
            new SeparatorMenuItem(),
            statusItem
        );
        contextMenu.show(resourceLabel, screenX, screenY);
    }
    
    private void showAssignmentContextMenu(Assignment assignment, Project project, Label assignmentBar, double screenX, double screenY) {
        ContextMenu contextMenu = new ContextMenu();
        
        // Project details menu item
        if (project != null) {
            MenuItem projectDetailsItem = new MenuItem("View Project: " + project.getProjectId());
            projectDetailsItem.setOnAction(e -> {
                if (onShowProjectDetails != null) {
                    onShowProjectDetails.accept(project);
                }
            });
            contextMenu.getItems().add(projectDetailsItem);
            
            // Edit Project & Client Info menu item
            MenuItem editProjectItem = new MenuItem("Edit Project & Client Info...");
            editProjectItem.setOnAction(e -> {
                showEditProjectDialog(project, assignment);
            });
            contextMenu.getItems().add(editProjectItem);
            
            // Open Items menu item
            MenuItem openItemsItem = new MenuItem("Manage Open Items...");
            openItemsItem.setOnAction(e -> {
                if (onManageOpenItems != null) {
                    onManageOpenItems.accept(project);
                }
            });
            contextMenu.getItems().add(openItemsItem);
            
            // Track Financials menu item
            MenuItem trackFinancialsItem = new MenuItem("Track Financials");
            trackFinancialsItem.setOnAction(e -> {
                showFinancialTrackingDialog(project);
            });
            contextMenu.getItems().add(trackFinancialsItem);
            
            // Add separator
            contextMenu.getItems().add(new SeparatorMenuItem());
            
            // Project Status submenu
            Menu statusMenu = new Menu("Change Project Status");
            
            // Create menu items for each status
            for (ProjectStatus status : ProjectStatus.values()) {
                MenuItem statusItem = new MenuItem(status.getDisplayName());
                
                // Show current status with a checkmark
                if (project.getStatus() == status) {
                    statusItem.setText("✓ " + status.getDisplayName());
                    statusItem.setDisable(true);
                } else {
                    statusItem.setOnAction(e -> {
                        if (onChangeProjectStatus != null) {
                            onChangeProjectStatus.accept(project, status);
                        }
                    });
                }
                statusMenu.getItems().add(statusItem);
            }
            
            contextMenu.getItems().add(statusMenu);
            
            // Generate Report menu item
            MenuItem generateReportItem = new MenuItem("Generate Client Report");
            generateReportItem.setOnAction(e -> {
                if (onGenerateReport != null) {
                    onGenerateReport.accept(project);
                }
            });
            contextMenu.getItems().add(generateReportItem);
            
            // View Tasks menu item
            MenuItem viewTasksItem = new MenuItem("View Tasks");
            viewTasksItem.setOnAction(e -> {
                if (onShowProjectTasks != null) {
                    onShowProjectTasks.accept(project);
                }
            });
            contextMenu.getItems().add(viewTasksItem);
            
            // View in Report Center menu item
            MenuItem viewInReportCenterItem = new MenuItem("View in Report Center");
            viewInReportCenterItem.setOnAction(e -> {
                if (onViewInReportCenter != null) {
                    onViewInReportCenter.accept(project);
                }
            });
            contextMenu.getItems().add(viewInReportCenterItem);
            
            // Add Filter submenu
            contextMenu.getItems().add(new SeparatorMenuItem());
            
            Menu filterMenu = new Menu("Filter Timeline");
            
            MenuItem filterByProject = new MenuItem("Filter by This Project");
            filterByProject.setOnAction(e -> {
                if (onApplyFilter != null) {
                    onApplyFilter.accept("project", project.getProjectId());
                }
            });
            filterMenu.getItems().add(filterByProject);
            
            MenuItem filterByStatus = new MenuItem("Filter by Status: " + project.getStatus().getDisplayName());
            filterByStatus.setOnAction(e -> {
                if (onApplyFilter != null) {
                    onApplyFilter.accept("status", project.getStatus());
                }
            });
            filterMenu.getItems().add(filterByStatus);
            
            if (project.getProjectManagerId() != null) {
                MenuItem filterByManager = new MenuItem("Filter by Project Manager");
                filterByManager.setOnAction(e -> {
                    if (onApplyFilter != null) {
                        onApplyFilter.accept("manager", project.getProjectManagerId());
                    }
                });
                filterMenu.getItems().add(filterByManager);
            }
            
            contextMenu.getItems().add(filterMenu);
        }
        
        // Add resource filter if we have the resource
        Resource resource = null;
        for (Resource r : resources) {
            if (r.getId().equals(assignment.getResourceId())) {
                resource = r;
                break;
            }
        }
        
        if (resource != null) {
            final String resourceName = resource.getName();
            MenuItem filterByResource = new MenuItem("Filter by Resource: " + resourceName);
            filterByResource.setOnAction(e -> {
                if (onApplyFilter != null) {
                    onApplyFilter.accept("resource", resourceName);
                }
            });
            contextMenu.getItems().add(filterByResource);
        }
        
        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();
        contextMenu.getItems().add(separator);
        
        // Edit menu item
        MenuItem editItem = new MenuItem("Edit Assignment");
        editItem.setOnAction(e -> {
            if (onEditAssignment != null) {
                onEditAssignment.accept(assignment);
            }
        });
        
        // Extend to Project Duration menu item (if assignment duration != project duration)
        if (project != null && project.getStartDate() != null && project.getEndDate() != null) {
            LocalDate projStart = project.getStartDate();
            LocalDate projEnd = project.getEndDate();
            if (!assignment.getStartDate().equals(projStart) || !assignment.getEndDate().equals(projEnd)) {
                MenuItem extendItem = new MenuItem("Extend to Full Project Duration");
                long projDays = java.time.temporal.ChronoUnit.DAYS.between(projStart, projEnd) + 1;
                long assignDays = java.time.temporal.ChronoUnit.DAYS.between(assignment.getStartDate(), assignment.getEndDate()) + 1;
                extendItem.setText(String.format("Extend to Full Project Duration (%d → %d days)", assignDays, projDays));
                extendItem.setOnAction(e -> {
                    // Update assignment to match project dates
                    assignment.setStartDate(projStart);
                    assignment.setEndDate(projEnd);
                    if (onEditAssignment != null) {
                        onEditAssignment.accept(assignment);
                    }
                    // Refresh the timeline to show the extended assignment
                    refreshTimeline();
                });
                contextMenu.getItems().add(extendItem);
            }
        }
        
        // Duplicate menu item
        MenuItem duplicateItem = new MenuItem("Duplicate Assignment");
        duplicateItem.setOnAction(e -> {
            if (onDuplicateAssignment != null) {
                onDuplicateAssignment.accept(assignment);
            }
        });
        
        // Delete menu item
        MenuItem deleteItem = new MenuItem("Delete Assignment");
        deleteItem.setOnAction(e -> {
            if (onDeleteAssignment != null) {
                onDeleteAssignment.accept(assignment);
            }
        });
        
        // Conflict info
        if (isConflicted(assignment.getId())) {
            MenuItem conflictItem = new MenuItem("⚠️ This assignment has conflicts");
            conflictItem.setDisable(true);
            contextMenu.getItems().addAll(new SeparatorMenuItem(), conflictItem);
        }
        
        contextMenu.getItems().addAll(editItem, duplicateItem, deleteItem);
        
        // Add delete project option in separate section
        if (project != null) {
            contextMenu.getItems().add(new SeparatorMenuItem());
            
            // Count assignments for this project
            long assignmentCount = assignments.stream()
                .filter(a -> a.getProjectId().equals(project.getId()))
                .count();
            
            MenuItem deleteProjectItem = new MenuItem("DELETE PROJECT");
            deleteProjectItem.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            deleteProjectItem.setOnAction(e -> {
                // If project has assignments, use the comprehensive delete
                if (assignmentCount > 0) {
                    if (onDeleteProjectWithAssignments != null) {
                        onDeleteProjectWithAssignments.accept(project);
                    }
                } else {
                    // No assignments, just delete the project
                    if (onDeleteProject != null) {
                        onDeleteProject.accept(project);
                    }
                }
            });
            
            contextMenu.getItems().add(deleteProjectItem);
        }
        
        contextMenu.show(assignmentBar, screenX, screenY);
    }
    
    // Property getters
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
    public ObjectProperty<LocalDate> endDateProperty() { return endDate; }
    public ObservableList<Project> getProjects() { return projects; }
    public ObservableList<Resource> getResources() { return resources; }
    public ObservableList<Assignment> getAssignments() { return assignments; }
    public ObservableList<TechnicianUnavailability> getUnavailabilities() { return unavailabilities; }
    public ObservableList<CompanyHoliday> getCompanyHolidays() { return companyHolidays; }
    public ObservableList<com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager> getProjectManagers() { return projectManagers; }
    
    public void setProjectManagers(List<com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager> managers) {
        this.projectManagers.clear();
        this.projectManagers.addAll(managers);
        this.projectManagerMap.clear();
        for (com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager pm : managers) {
            this.projectManagerMap.put(pm.getId(), pm);
        }
    }
    
    public void setUtilizationSettings(UtilizationSettings settings) {
        this.utilizationSettings = settings;
        refreshTimeline(); // Refresh to apply new settings
    }
    
    public UtilizationSettings getUtilizationSettings() {
        return utilizationSettings;
    }
    
    public void setShowUnavailability(boolean showUnavailability) {
        this.showUnavailability = showUnavailability;
        refreshTimeline(); // Refresh to apply visibility change
    }
    
    public boolean isShowUnavailability() {
        return showUnavailability;
    }
    
    // Utility methods
    public void scrollToDate(LocalDate date) {
        Integer columnIndex = dateColumnCache.get(date);
        if (columnIndex != null) {
            double scrollPosition = (columnIndex - 1) * dayWidth / scrollPane.getContent().getBoundsInLocal().getWidth();
            scrollPane.setHvalue(scrollPosition);
        }
    }
    
    public void zoomToFit() {
        Platform.runLater(() -> scrollPane.setHvalue(0));
    }
    
    
    // Zoom control methods
    public void setZoomLevel(double level) {
        // Clamp zoom level to reasonable bounds
        this.zoomLevel = Math.max(0.5, Math.min(3.0, level));
        
        // Update computed dimensions
        this.dayWidth = BASE_DAY_WIDTH * zoomLevel;
        // Scale row height less aggressively at high zoom levels
        if (zoomLevel >= 2.0) {
            // At 200%+ zoom, use reduced scaling (half the normal scaling above 100%)
            this.rowHeight = BASE_ROW_HEIGHT * (1.0 + (zoomLevel - 1.0) * 0.5);
        } else {
            this.rowHeight = BASE_ROW_HEIGHT * zoomLevel;
        }
        this.projectBarHeight = BASE_PROJECT_BAR_HEIGHT * zoomLevel;
        // Scale resource label width with zoom but cap at 1.8x for readability
        this.resourceLabelWidth = BASE_RESOURCE_LABEL_WIDTH * Math.min(zoomLevel, 1.8);
        
        // Update fixed column width when zoom changes
        if (fixedResourceColumn != null) {
            fixedResourceColumn.setPrefWidth(resourceLabelWidth);
            fixedResourceColumn.setMinWidth(resourceLabelWidth);
            fixedResourceColumn.setMaxWidth(resourceLabelWidth);
        }
        
        if (fixedResourceHeader != null) {
            fixedResourceHeader.setPrefWidth(resourceLabelWidth);
            fixedResourceHeader.setMinWidth(resourceLabelWidth);
            fixedResourceHeader.setMaxWidth(resourceLabelWidth);
        }
        
        // Update property
        if (zoomLevelProperty.get() != this.zoomLevel) {
            zoomLevelProperty.set(this.zoomLevel);
        }
    }
    
    public double getZoomLevel() {
        return zoomLevel;
    }
    
    public DoubleProperty zoomLevelProperty() {
        return zoomLevelProperty;
    }
    
    // Convenience methods for common zoom levels
    public void setZoom100() { setZoomLevel(1.0); }
    public void setZoom125() { setZoomLevel(1.25); }
    public void setZoom150() { setZoomLevel(1.5); }
    public void setZoom200() { setZoomLevel(2.0); }
    
    // Conflict detection methods
    public void updateConflicts(Set<Long> conflictedIds) {
        this.conflictedAssignmentIds = conflictedIds != null ? conflictedIds : new HashSet<>();
        // Refresh timeline to apply conflict highlighting
        Platform.runLater(this::refreshTimeline);
    }
    
    public boolean isConflicted(Long assignmentId) {
        return conflictedAssignmentIds.contains(assignmentId);
    }
    
    public Set<Long> getConflictedAssignmentIds() {
        return new HashSet<>(conflictedAssignmentIds);
    }
    
    // Check if placing an assignment at this position would create a conflict
    private boolean checkForConflictAtPosition(Resource resource, LocalDate startDate, LocalDate endDate, Long excludeAssignmentId) {
        // Get all assignments for this resource
        List<Assignment> resourceAssignments = assignments.stream()
            .filter(a -> a.getResourceId().equals(resource.getId()))
            .filter(a -> !a.getId().equals(excludeAssignmentId)) // Exclude the assignment being moved
            .collect(Collectors.toList());
        
        // Check for overlaps with the proposed dates
        for (Assignment existing : resourceAssignments) {
            if (datesOverlap(startDate, endDate, existing.getStartDate(), existing.getEndDate())) {
                return true; // Conflict found
            }
        }
        
        return false; // No conflicts
    }
    
    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }
    
    // Calculate resource utilization percentage for the visible timeline period
    private double calculateResourceUtilization(Resource resource, LocalDate start, LocalDate end) {
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        
        // Get all assignments for this resource in the timeline period
        List<Assignment> resourceAssignments = assignments.stream()
            .filter(a -> a.getResourceId().equals(resource.getId()))
            .filter(a -> datesOverlap(a.getStartDate(), a.getEndDate(), start, end))
            .toList();
        
        // Calculate total assigned days (counting overlaps as overallocation)
        long assignedDays = 0;
        for (Assignment assignment : resourceAssignments) {
            LocalDate assignStart = assignment.getStartDate().isBefore(start) ? start : assignment.getStartDate();
            LocalDate assignEnd = assignment.getEndDate().isAfter(end) ? end : assignment.getEndDate();
            assignedDays += ChronoUnit.DAYS.between(assignStart, assignEnd) + 1;
        }
        
        // Calculate percentage (can be > 100% if resource is overallocated)
        return (assignedDays * 100.0) / totalDays;
    }
    
    // Calculate both utilization and billable percentages
    private ResourceMetrics calculateResourceMetrics(Resource resource, LocalDate start, LocalDate end) {
        // Calculate available days based on utilization settings
        long availableDays = calculateAvailableDays(start, end);
        
        // Get all assignments for this resource in the timeline period
        List<Assignment> resourceAssignments = assignments.stream()
            .filter(a -> a.getResourceId().equals(resource.getId()))
            .filter(a -> datesOverlap(a.getStartDate(), a.getEndDate(), start, end))
            .toList();
        
        // Calculate total assigned days and billable days
        long totalAssignedDays = 0;
        long billableDays = 0;
        long shopDays = 0;
        long trainingDays = 0;
        
        for (Assignment assignment : resourceAssignments) {
            LocalDate assignStart = assignment.getStartDate().isBefore(start) ? start : assignment.getStartDate();
            LocalDate assignEnd = assignment.getEndDate().isAfter(end) ? end : assignment.getEndDate();
            
            // Count only working days if configured
            long days = 0;
            if (utilizationSettings.getCalculationMethod() == UtilizationSettings.CalculationMethod.WORKING_DAYS) {
                days = calculateWorkingDays(assignStart, assignEnd);
            } else {
                days = ChronoUnit.DAYS.between(assignStart, assignEnd) + 1;
            }
            
            // Check if this is a SHOP/TRAINING assignment
            Project project = projects.stream()
                .filter(p -> p.getId().equals(assignment.getProjectId()))
                .findFirst()
                .orElse(null);
                
            if (project != null) {
                String projectId = project.getProjectId();
                if ("SHOP".equalsIgnoreCase(projectId)) {
                    shopDays += days;
                    // Count SHOP as utilized based on settings
                    if (utilizationSettings.isCountShopAsUtilized()) {
                        totalAssignedDays += days;
                    }
                } else if ("TRAINING".equalsIgnoreCase(projectId)) {
                    trainingDays += days;
                    // Count TRAINING as utilized based on settings
                    if (utilizationSettings.isCountTrainingAsUtilized()) {
                        totalAssignedDays += days;
                    }
                } else {
                    // Regular project - always counts as utilized and billable
                    totalAssignedDays += days;
                    billableDays += days;
                }
            } else {
                // If project not found, assume it's billable
                totalAssignedDays += days;
                billableDays += days;
            }
        }
        
        // Calculate percentages
        double utilizationPercent = availableDays > 0 ? (totalAssignedDays * 100.0) / availableDays : 0;
        double billablePercent = totalAssignedDays > 0 ? (billableDays * 100.0) / totalAssignedDays : 0;
        
        return new ResourceMetrics(utilizationPercent, billablePercent, totalAssignedDays, billableDays, shopDays);
    }
    
    // Calculate available days based on utilization settings
    private long calculateAvailableDays(LocalDate start, LocalDate end) {
        if (utilizationSettings.getCalculationMethod() == UtilizationSettings.CalculationMethod.CALENDAR_DAYS) {
            // All calendar days
            return ChronoUnit.DAYS.between(start, end) + 1;
        } else {
            // Working days only
            return calculateWorkingDays(start, end);
        }
    }
    
    // Calculate working days (excluding weekends and optionally holidays)
    private long calculateWorkingDays(LocalDate start, LocalDate end) {
        long workingDays = 0;
        LocalDate current = start;
        
        while (!current.isAfter(end)) {
            boolean isWorkingDay = true;
            
            // Check weekends
            if (current.getDayOfWeek() == DayOfWeek.SATURDAY) {
                isWorkingDay = utilizationSettings.isIncludeSaturdays();
            } else if (current.getDayOfWeek() == DayOfWeek.SUNDAY) {
                isWorkingDay = utilizationSettings.isIncludeWeekends();
            }
            
            // Check holidays
            if (isWorkingDay && !utilizationSettings.isIncludeHolidays()) {
                LocalDate currentDate = current;
                boolean isHoliday = companyHolidays.stream()
                    .anyMatch(h -> h.getDate().equals(currentDate) && h.isActive());
                if (isHoliday) {
                    isWorkingDay = false;
                }
            }
            
            if (isWorkingDay) {
                workingDays++;
            }
            
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    // Inner class to hold resource metrics
    private static class ResourceMetrics {
        final double utilizationPercent;
        final double billablePercent;
        final long totalAssignedDays;
        final long billableDays;
        final long shopDays;
        
        ResourceMetrics(double utilizationPercent, double billablePercent, 
                       long totalAssignedDays, long billableDays, long shopDays) {
            this.utilizationPercent = utilizationPercent;
            this.billablePercent = billablePercent;
            this.totalAssignedDays = totalAssignedDays;
            this.billableDays = billableDays;
            this.shopDays = shopDays;
        }
    }
    
    // Context menu callback setters
    public void setOnEditProject(Consumer<Project> onEditProject) {
        this.onEditProject = onEditProject;
    }
    
    public void setOnManageOpenItems(Consumer<Project> onManageOpenItems) {
        this.onManageOpenItems = onManageOpenItems;
    }
    
    public void setOnDeleteProject(Consumer<Project> onDeleteProject) {
        this.onDeleteProject = onDeleteProject;
    }
    
    public void setOnDeleteProjectWithAssignments(Consumer<Project> onDeleteProjectWithAssignments) {
        this.onDeleteProjectWithAssignments = onDeleteProjectWithAssignments;
    }
    
    public void setOnEditResource(Consumer<Resource> onEditResource) {
        this.onEditResource = onEditResource;
    }
    
    public void setOnDeleteResource(Consumer<Resource> onDeleteResource) {
        this.onDeleteResource = onDeleteResource;
    }
    
    public void setOnMarkResourceUnavailable(Consumer<Resource> onMarkResourceUnavailable) {
        this.onMarkResourceUnavailable = onMarkResourceUnavailable;
    }
    
    public void setOnViewResourceUnavailability(Consumer<Resource> onViewResourceUnavailability) {
        this.onViewResourceUnavailability = onViewResourceUnavailability;
    }
    
    public void setOnEditAssignment(Consumer<Assignment> onEditAssignment) {
        this.onEditAssignment = onEditAssignment;
    }
    
    public void setOnDeleteAssignment(Consumer<Assignment> onDeleteAssignment) {
        this.onDeleteAssignment = onDeleteAssignment;
    }
    
    public void setOnDuplicateAssignment(Consumer<Assignment> onDuplicateAssignment) {
        this.onDuplicateAssignment = onDuplicateAssignment;
    }
    
    public void setOnShowProjectDetails(Consumer<Project> onShowProjectDetails) {
        this.onShowProjectDetails = onShowProjectDetails;
    }
    
    public void setOnShowResourceDetails(Consumer<Resource> onShowResourceDetails) {
        this.onShowResourceDetails = onShowResourceDetails;
    }
    
    public void setOnChangeProjectStatus(BiConsumer<Project, ProjectStatus> onChangeProjectStatus) {
        this.onChangeProjectStatus = onChangeProjectStatus;
    }
    
    public void setOnGenerateReport(Consumer<Project> onGenerateReport) {
        this.onGenerateReport = onGenerateReport;
    }
    
    public void setOnShowProjectTasks(Consumer<Project> onShowProjectTasks) {
        this.onShowProjectTasks = onShowProjectTasks;
    }
    
    public void setOnViewInReportCenter(Consumer<Project> onViewInReportCenter) {
        this.onViewInReportCenter = onViewInReportCenter;
    }
    
    public void setOnApplyFilter(BiConsumer<String, Object> onApplyFilter) {
        this.onApplyFilter = onApplyFilter;
    }
    
    private void setupEdgeResize(Region leftHandle, Region rightHandle, StackPane barContainer, 
                                  Assignment assignment, Label assignmentBar, LocalDate timelineStart) {
        // Store initial drag positions
        final double[] dragStartX = new double[1];
        final double[] initialLeft = new double[1];
        final double[] initialWidth = new double[1];
        
        // Store the current assignment data (will be updated after drags)
        final Assignment[] currentAssignment = new Assignment[] { assignment };
        
        // Left edge drag handler
        leftHandle.setOnMousePressed(event -> {
            // Store initial positions
            dragStartX[0] = event.getSceneX();
            initialLeft[0] = barContainer.getLayoutX();
            initialWidth[0] = barContainer.getWidth();
            
            // Get the current assignment dates (may have been updated from previous drags)
            Assignment current = (Assignment) barContainer.getUserData();
            if (current == null) {
                current = assignment;
            }
            
            logger.info("LEFT EDGE DRAG START - Assignment ID: {}, Current dates: {} to {}", 
                current.getId(), current.getStartDate(), current.getEndDate());
            logger.info("  Initial scene X: {}, Initial container left: {}, Initial width: {}", 
                dragStartX[0], initialLeft[0], initialWidth[0]);
            logger.info("  Container should be at day {} based on margin {}/dayWidth {}", 
                (int)(initialLeft[0] / dayWidth), initialLeft[0], dayWidth);
            event.consume();
        });
        
        leftHandle.setOnMouseDragged(event -> {
            // Calculate drag delta from initial position
            double deltaX = event.getSceneX() - dragStartX[0];
            
            // Calculate new left position
            double newLeft = initialLeft[0] + deltaX;
            newLeft = Math.max(0, newLeft); // Don't go past left edge
            
            // Snap to day grid - find which day cell the left edge is in
            // Use floor to get the day the mouse is actually over
            int dayIndex = (int) Math.floor(newLeft / dayWidth);
            // If we're more than halfway through the day, snap to the next day
            double remainder = newLeft - (dayIndex * dayWidth);
            if (remainder > dayWidth / 2) {
                dayIndex++;
            }
            dayIndex = Math.max(0, dayIndex);
            double snappedLeft = dayIndex * dayWidth;
            
            // Calculate new width (right edge stays fixed)
            double rightEdgePosition = initialLeft[0] + initialWidth[0];
            double newWidth = rightEdgePosition - snappedLeft;
            
            logger.debug("LEFT EDGE DRAGGING - Scene X: {}, Delta: {}, New left: {}", 
                event.getSceneX(), deltaX, newLeft);
            logger.debug("  Floor: {}, Remainder: {}, Day index: {}, Snapped: {}", 
                Math.floor(newLeft / dayWidth), remainder, dayIndex, snappedLeft);
            logger.debug("  Right edge: {}, New width: {}", 
                rightEdgePosition, newWidth);
            
            if (newWidth >= dayWidth) { // Minimum 1 day width
                // Update visual position
                barContainer.setLayoutX(snappedLeft);
                barContainer.setPrefWidth(newWidth);
                barContainer.setMaxWidth(newWidth);
                barContainer.setMinWidth(newWidth);
                assignmentBar.setPrefWidth(newWidth);
                assignmentBar.setMaxWidth(newWidth);
                assignmentBar.setMinWidth(newWidth);
                
                logger.debug("  Visual update: New margin left: {}, New width: {}", snappedLeft, newWidth);
            } else {
                logger.debug("  Constraint violated: width {} < {}", newWidth, dayWidth);
            }
            
            event.consume();
        });
        
        leftHandle.setOnMouseReleased(event -> {
            // Calculate final position
            double finalLeft = barContainer.getLayoutX();
            // The position should already be snapped to a day boundary, so just divide
            int dayIndex = (int) Math.round(finalLeft / dayWidth);
            
            // Calculate new start date
            LocalDate newStartDate = timelineStart.plusDays(dayIndex);
            
            // Keep the same end date from the current assignment (may have been updated)
            LocalDate endDate = currentAssignment[0].getEndDate();
            
            logger.info("LEFT EDGE DRAG END - Assignment ID: {}", assignment.getId());
            logger.info("  Final left margin: {}, Day index: {}, dayWidth: {}", 
                finalLeft, dayIndex, dayWidth);
            logger.info("  Calculation: {} / {} = {} -> rounded to {}", 
                finalLeft, dayWidth, finalLeft/dayWidth, dayIndex);
            logger.info("  Timeline start: {}, New start date: {} ({}+{} days), End date: {}", 
                timelineStart, newStartDate, timelineStart, dayIndex, endDate);
            logger.info("  Visual check: Bar is at pixel {} which is day {}, date should be {}", 
                finalLeft, dayIndex, timelineStart.plusDays(dayIndex));
            
            // Ensure start date is before end date
            if (newStartDate.isBefore(endDate) || newStartDate.isEqual(endDate)) {
                logger.info("  UPDATING ASSIGNMENT: {} -> Start: {} (was {}), End: {} (unchanged)",
                    assignment.getId(), newStartDate, assignment.getStartDate(), endDate);
                
                // Update assignment
                Assignment updatedAssignment = new Assignment(
                    currentAssignment[0].getProjectId(),
                    currentAssignment[0].getResourceId(),
                    newStartDate,
                    endDate,
                    currentAssignment[0].getTravelOutDays(),
                    currentAssignment[0].getTravelBackDays()
                );
                updatedAssignment.setId(currentAssignment[0].getId());
                updatedAssignment.setOverride(currentAssignment[0].isOverride());
                updatedAssignment.setOverrideReason(currentAssignment[0].getOverrideReason());
                updatedAssignment.setNotes(currentAssignment[0].getNotes());
                
                // Update the stored assignment reference
                currentAssignment[0] = updatedAssignment;
                barContainer.setUserData(updatedAssignment); // Update the container's user data
                
                // Fire edit event
                if (onEditAssignment != null) {
                    onEditAssignment.accept(updatedAssignment);
                }
            } else {
                logger.warn("  REJECTED: New start date {} would be after end date {}", newStartDate, endDate);
            }
            
            event.consume();
        });
        
        // Store initial drag positions for right edge
        final double[] rightDragStartX = new double[1];
        final double[] rightInitialWidth = new double[1];
        final double[] rightInitialLeft = new double[1];
        
        // Right edge drag handler
        rightHandle.setOnMousePressed(event -> {
            // Store initial positions
            rightDragStartX[0] = event.getSceneX();
            rightInitialWidth[0] = barContainer.getWidth();
            rightInitialLeft[0] = barContainer.getLayoutX();
            
            // Get the current assignment dates (may have been updated from previous drags)
            Assignment current = (Assignment) barContainer.getUserData();
            if (current == null) {
                current = assignment;
            }
            
            logger.info("RIGHT EDGE DRAG START - Assignment ID: {}, Current dates: {} to {}", 
                current.getId(), current.getStartDate(), current.getEndDate());
            logger.info("  Initial scene X: {}, Container width: {}, Left margin: {}", 
                rightDragStartX[0], rightInitialWidth[0], rightInitialLeft[0]);
            event.consume();
        });
        
        rightHandle.setOnMouseDragged(event -> {
            // Calculate drag delta from initial position
            double deltaX = event.getSceneX() - rightDragStartX[0];
            
            // Calculate new width
            double newWidth = rightInitialWidth[0] + deltaX;
            newWidth = Math.max(dayWidth, newWidth); // Minimum 1 day width
            
            // Calculate right edge position and snap to grid
            double rightEdgePosition = rightInitialLeft[0] + newWidth;
            int dayIndex = (int) Math.round(rightEdgePosition / dayWidth);
            double snappedRightEdge = dayIndex * dayWidth;
            
            // Calculate final width based on snapped position
            double snappedWidth = snappedRightEdge - rightInitialLeft[0];
            
            logger.debug("RIGHT EDGE DRAGGING - Scene X: {}, Delta: {}, New width: {}, Snapped width: {}", 
                event.getSceneX(), deltaX, newWidth, snappedWidth);
            logger.debug("  Right edge pos: {}, Day index: {}, Snapped edge: {}", 
                rightEdgePosition, dayIndex, snappedRightEdge);
            
            if (snappedWidth >= dayWidth) { // Minimum 1 day width
                // Update visual width
                barContainer.setPrefWidth(snappedWidth);
                barContainer.setMaxWidth(snappedWidth);
                barContainer.setMinWidth(snappedWidth);
                assignmentBar.setPrefWidth(snappedWidth);
                assignmentBar.setMaxWidth(snappedWidth);
                assignmentBar.setMinWidth(snappedWidth);
                
                logger.debug("  Visual update: New width: {}", snappedWidth);
            } else {
                logger.debug("  Width constraint violated: {} < {}", snappedWidth, dayWidth);
            }
            
            event.consume();
        });
        
        rightHandle.setOnMouseReleased(event -> {
            // Calculate final position
            double finalWidth = barContainer.getWidth();
            double rightEdgePosition = barContainer.getLayoutX() + finalWidth;
            int dayIndex = (int) Math.round(rightEdgePosition / dayWidth);
            
            // Calculate new end date (subtract 1 because end date is inclusive)
            LocalDate newEndDate = timelineStart.plusDays(dayIndex - 1);
            
            // Keep the same start date from the current assignment (may have been updated)
            LocalDate startDate = currentAssignment[0].getStartDate();
            
            logger.info("RIGHT EDGE DRAG END - Final width: {}, Right edge position: {}, Day index: {}", 
                finalWidth, rightEdgePosition, dayIndex);
            logger.info("  Timeline start: {}, Start date: {}, New end date: {}", 
                timelineStart, startDate, newEndDate);
            logger.info("  Day calculation: {} + {} days - 1 = {}", timelineStart, dayIndex, newEndDate);
            
            // Ensure end date is after start date
            if (newEndDate.isAfter(startDate) || newEndDate.isEqual(startDate)) {
                logger.info("  UPDATING ASSIGNMENT: {} -> Start: {} (unchanged), End: {} (was {})",
                    assignment.getId(), startDate, newEndDate, assignment.getEndDate());
                
                // Update assignment
                Assignment updatedAssignment = new Assignment(
                    currentAssignment[0].getProjectId(),
                    currentAssignment[0].getResourceId(),
                    startDate,
                    newEndDate,
                    currentAssignment[0].getTravelOutDays(),
                    currentAssignment[0].getTravelBackDays()
                );
                updatedAssignment.setId(currentAssignment[0].getId());
                updatedAssignment.setOverride(currentAssignment[0].isOverride());
                updatedAssignment.setOverrideReason(currentAssignment[0].getOverrideReason());
                updatedAssignment.setNotes(currentAssignment[0].getNotes());
                
                // Update the stored assignment reference
                currentAssignment[0] = updatedAssignment;
                barContainer.setUserData(updatedAssignment); // Update the container's user data
                
                // Fire edit event
                if (onEditAssignment != null) {
                    onEditAssignment.accept(updatedAssignment);
                }
            } else {
                logger.warn("  REJECTED: New end date {} would be before start date {}", newEndDate, startDate);
            }
            
            event.consume();
        });
    }
    
    private void setupDragAndDrop(Label assignmentBar, Assignment assignment) {
        // Make the assignment bar draggable
        assignmentBar.setOnDragDetected(event -> {
            // Start drag operation
            Dragboard dragboard = assignmentBar.startDragAndDrop(TransferMode.MOVE);
            
            // Create a minimal transparent drag image since we use ghost preview for visual feedback
            WritableImage dragImage = new WritableImage(1, 1);
            dragImage.getPixelWriter().setColor(0, 0, javafx.scene.paint.Color.TRANSPARENT);
            
            // Set the drag image (required for drag to work but invisible)
            dragboard.setDragView(dragImage);
            
            // Store content in dragboard
            ClipboardContent content = new ClipboardContent();
            content.putString(assignment.getId().toString());
            dragboard.setContent(content);
            
            // Store the dragged assignment and bar
            draggedAssignment = assignment;
            draggedBar = assignmentBar;
            
            // Store where in the bar the user clicked (important for drop positioning)
            dragStartOffsetX = event.getX();
            logger.info("DRAG START - Click offset within bar: {} pixels", dragStartOffsetX);
            
            // Visual feedback - make dragged item semi-transparent
            assignmentBar.setOpacity(0.3);
            
            event.consume();
        });
        
        // Handle drag done (cleanup)
        assignmentBar.setOnDragDone(event -> {
            // Reset opacity
            assignmentBar.setOpacity(1.0);
            
            // Always clear ALL drag state regardless of success
            draggedAssignment = null;
            draggedBar = null;
            ghostPreview = null;
            dragStartOffsetX = 0;
            
            event.consume();
        });
    }
    
    private List<Project> findUnassignedProjects(LocalDate start, LocalDate end) {
        // Get all project IDs that have assignments
        Set<Long> assignedProjectIds = assignments.stream()
            .map(Assignment::getProjectId)
            .collect(Collectors.toSet());
        
        // Log details for debugging
        logger.debug("Total projects in timeline: {}", projects.size());
        logger.debug("Projects with assignments: {}", assignedProjectIds.size());
        
        // Find projects without assignments that overlap with the timeline date range
        List<Project> unassigned = projects.stream()
            .filter(p -> !assignedProjectIds.contains(p.getId()))
            .filter(p -> {
                // Check if project dates overlap with timeline range
                LocalDate projectStart = p.getStartDate();
                LocalDate projectEnd = p.getEndDate();
                return projectStart != null && projectEnd != null &&
                       !(projectEnd.isBefore(start) || projectStart.isAfter(end));
            })
            .sorted(Comparator.comparing(Project::getStartDate))
            .collect(Collectors.toList());
            
        logger.debug("Found {} unassigned projects in date range", unassigned.size());
        for (Project p : unassigned) {
            logger.debug("Unassigned project: {} - {} (ID: {})", p.getProjectId(), p.getDescription(), p.getId());
        }
        
        return unassigned;
    }
    
    private void createUnassignedProjectRow(Project unassignedProject, LocalDate start, LocalDate end, int rowIndex) {
        // Create label for this unassigned project
        StackPane labelContainer = new StackPane();
        labelContainer.setPrefWidth(resourceLabelWidth);
        labelContainer.setMaxWidth(resourceLabelWidth);
        labelContainer.setMinWidth(resourceLabelWidth);
        labelContainer.setPadding(new Insets(3, 5, 3, 5));
        labelContainer.setMinHeight(rowHeight);
        labelContainer.setPrefHeight(rowHeight);
        labelContainer.setMaxHeight(rowHeight);
        labelContainer.setAlignment(Pos.CENTER_LEFT);
        
        // Style for unassigned project row
        labelContainer.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffc107; -fx-border-width: 0 0 1 0;");
        
        // Create label showing project info with project manager on second line
        String projectInfo = unassignedProject.getProjectId();
        if (unassignedProject.getDescription() != null && !unassignedProject.getDescription().isEmpty()) {
            String desc = unassignedProject.getDescription();
            if (desc.length() > 20) {
                desc = desc.substring(0, 20) + "...";
            }
            projectInfo = unassignedProject.getProjectId() + " - " + desc;
        }
        
        // Get project manager name if available
        String pmName = "No PM Assigned";
        if (unassignedProject.getProjectManagerId() != null && projectManagerMap.containsKey(unassignedProject.getProjectManagerId())) {
            com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager pm = projectManagerMap.get(unassignedProject.getProjectManagerId());
            pmName = "PM: " + pm.getName();
        }
        
        // Create two-line label text
        String labelText = "Unassigned: " + projectInfo + "\n" + pmName;
        
        Label unassignedLabel = new Label(labelText);
        unassignedLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #856404;");
        unassignedLabel.setAlignment(Pos.CENTER_LEFT);
        unassignedLabel.setPrefWidth(resourceLabelWidth - 10);
        unassignedLabel.setMaxWidth(resourceLabelWidth - 10);
        unassignedLabel.setPrefHeight(rowHeight - 10);
        unassignedLabel.setPadding(new Insets(5, 5, 5, 5));
        unassignedLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        
        // Add tooltip with full project details
        String pmTooltipName = "Not assigned";
        if (unassignedProject.getProjectManagerId() != null && projectManagerMap.containsKey(unassignedProject.getProjectManagerId())) {
            pmTooltipName = projectManagerMap.get(unassignedProject.getProjectManagerId()).getName();
        }
        
        Tooltip tooltip = new Tooltip("Project: " + unassignedProject.getProjectId() + 
                                     "\nDescription: " + unassignedProject.getDescription() +
                                     "\nProject Manager: " + pmTooltipName +
                                     "\nStart: " + unassignedProject.getStartDate() +
                                     "\nEnd: " + unassignedProject.getEndDate() +
                                     "\n\nDrag to assign to a resource");
        unassignedLabel.setTooltip(tooltip);
        
        labelContainer.getChildren().add(unassignedLabel);
        fixedResourceContent.getChildren().add(labelContainer);
        
        // Create timeline row cells
        long dayCount = ChronoUnit.DAYS.between(start, end) + 1;
        for (long i = 0; i < dayCount; i++) {
            LocalDate date = start.plusDays(i);
            Pane dayCell = new Pane();
            dayCell.setPrefHeight(rowHeight);
            dayCell.setMinHeight(rowHeight);
            dayCell.setMaxHeight(rowHeight);
            dayCell.setPrefWidth(dayWidth);
            dayCell.setMinWidth(dayWidth);
            dayCell.setMaxWidth(dayWidth);
            
            // Light yellow background for unassigned project row
            boolean isWeekend = date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || 
                               date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            boolean isHoliday = companyHolidays != null && companyHolidays.stream()
                .anyMatch(h -> h.getDate().equals(date));
            boolean isToday = date.equals(LocalDate.now());
            
            String bgColor;
            if (isToday) {
                bgColor = "#d4d4d4"; // Darker gray for today
            } else if (isHoliday) {
                bgColor = "#ffe8ec"; // Light pink for holidays
            } else if (isWeekend) {
                bgColor = "#fff8e1"; // Light yellow-blue for weekends
            } else {
                bgColor = "#fffbf0"; // Very light yellow for regular days
            }
            
            // Only use bottom border to avoid cumulative width drift from right borders
            dayCell.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
            timelineGrid.add(dayCell, (int) i, rowIndex);
        }
        
        // Create a transparent row container for this project bar
        Pane rowContainer = new Pane();
        rowContainer.setPrefHeight(rowHeight);
        rowContainer.setMinHeight(rowHeight);
        rowContainer.setMaxHeight(rowHeight);
        rowContainer.setPickOnBounds(false);
        
        // Add the single project bar
        createUnassignedProjectBar(unassignedProject, start, end, rowContainer);
        
        // Add the row container on top of the day cells
        timelineGrid.add(rowContainer, 0, rowIndex, (int) dayCount, 1);
    }
    
    private void createUnassignedProjectBar(Project project, LocalDate timelineStart, LocalDate timelineEnd, Pane rowContainer) {
        LocalDate projectStart = project.getStartDate();
        LocalDate projectEnd = project.getEndDate();
        
        if (projectStart == null || projectEnd == null) {
            return;
        }
        
        // Calculate visible dates
        LocalDate visibleStart = projectStart.isBefore(timelineStart) ? timelineStart : projectStart;
        LocalDate visibleEnd = projectEnd.isAfter(timelineEnd) ? timelineEnd : projectEnd;
        
        long dayOffset = ChronoUnit.DAYS.between(timelineStart, visibleStart);
        long duration = ChronoUnit.DAYS.between(visibleStart, visibleEnd) + 1;
        
        if (dayOffset < 0 || duration <= 0) {
            return;
        }
        
        // Create project bar with placeholder styling
        Label projectBar = new Label();
        String displayText = project.getProjectId() + " - Needs Resources";
        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            displayText = project.getProjectId() + " | " + project.getDescription() + " (Unassigned)";
        }
        projectBar.setText(displayText);
        
        // Placeholder styling - dashed border, semi-transparent
        projectBar.setStyle(
            "-fx-background-color: rgba(255, 193, 7, 0.3); " +
            "-fx-border-color: #ff9800; " +
            "-fx-border-width: 2; " +
            "-fx-border-style: dashed; " +
            "-fx-padding: 3 6 3 6; " +
            "-fx-font-size: 11px; " +
            "-fx-text-fill: #856404; " +
            "-fx-font-weight: bold;"
        );
        
        projectBar.setPrefHeight(projectBarHeight);
        projectBar.setMaxHeight(projectBarHeight);
        projectBar.setMinHeight(projectBarHeight);
        projectBar.setPrefWidth(duration * dayWidth - 4);
        projectBar.setMaxWidth(duration * dayWidth - 4);
        projectBar.setAlignment(Pos.CENTER_LEFT);
        projectBar.setTextOverrun(OverrunStyle.ELLIPSIS);
        
        // Position the bar
        projectBar.setLayoutX(dayOffset * dayWidth + 2);
        projectBar.setLayoutY((rowHeight - projectBarHeight) / 2);
        
        // Add tooltip
        Tooltip tooltip = new Tooltip(
            "Project: " + project.getProjectId() + "\n" +
            "Description: " + (project.getDescription() != null ? project.getDescription() : "N/A") + "\n" +
            "Duration: " + project.getStartDate() + " to " + project.getEndDate() + "\n" +
            "Status: UNASSIGNED - Click to assign resources"
        );
        Tooltip.install(projectBar, tooltip);
        
        // Make it clickable to open assignment dialog
        projectBar.setCursor(Cursor.HAND);
        projectBar.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                // Double-click to open assignment dialog
                if (onEditAssignment != null) {
                    // Create a dummy assignment to trigger the assignment dialog
                    Assignment dummyAssignment = new Assignment();
                    dummyAssignment.setProjectId(project.getId());
                    dummyAssignment.setStartDate(project.getStartDate());
                    dummyAssignment.setEndDate(project.getEndDate());
                    onEditAssignment.accept(dummyAssignment);
                }
            }
        });
        
        // Add context menu
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem assignResources = new MenuItem("Assign Resources...");
        assignResources.setOnAction(e -> {
            if (onEditAssignment != null) {
                Assignment dummyAssignment = new Assignment();
                dummyAssignment.setProjectId(project.getId());
                dummyAssignment.setStartDate(project.getStartDate());
                dummyAssignment.setEndDate(project.getEndDate());
                onEditAssignment.accept(dummyAssignment);
            }
        });
        
        MenuItem editProject = new MenuItem("Edit Project...");
        editProject.setOnAction(e -> {
            logger.info("Edit Project menu clicked for project {} with travel={}", project.getProjectId(), project.isTravel());
            if (onEditProject != null) {
                onEditProject.accept(project);
            }
        });
        
        MenuItem openItems = new MenuItem("Manage Open Items...");
        openItems.setOnAction(e -> {
            logger.info("Manage Open Items menu clicked for project {}", project.getProjectId());
            if (onManageOpenItems != null) {
                onManageOpenItems.accept(project);
            }
        });
        
        // Add Filter submenu
        Menu filterMenu = new Menu("Filter Timeline");
        
        MenuItem filterByProject = new MenuItem("Filter by This Project");
        filterByProject.setOnAction(e -> {
            if (onApplyFilter != null) {
                onApplyFilter.accept("project", project.getProjectId());
            }
        });
        filterMenu.getItems().add(filterByProject);
        
        MenuItem filterByStatus = new MenuItem("Filter by Status: " + project.getStatus().getDisplayName());
        filterByStatus.setOnAction(e -> {
            if (onApplyFilter != null) {
                onApplyFilter.accept("status", project.getStatus());
            }
        });
        filterMenu.getItems().add(filterByStatus);
        
        if (project.getProjectManagerId() != null) {
            MenuItem filterByManager = new MenuItem("Filter by Project Manager");
            filterByManager.setOnAction(e -> {
                if (onApplyFilter != null) {
                    onApplyFilter.accept("manager", project.getProjectManagerId());
                }
            });
            filterMenu.getItems().add(filterByManager);
        }
        
        MenuItem clearFilters = new MenuItem("Clear All Filters");
        clearFilters.setOnAction(e -> {
            if (onApplyFilter != null) {
                onApplyFilter.accept("clear", null);
            }
        });
        filterMenu.getItems().addAll(new SeparatorMenuItem(), clearFilters);
        
        contextMenu.getItems().addAll(assignResources, new SeparatorMenuItem(), editProject, openItems, new SeparatorMenuItem(), filterMenu);
        projectBar.setOnContextMenuRequested(e -> contextMenu.show(projectBar, e.getScreenX(), e.getScreenY()));
        
        // Add the project bar to the row container
        rowContainer.getChildren().add(projectBar);
    }
    
    private void setupDropTargets() {
        // Set up each resource row as a potential drop target
        for (Node node : timelineGrid.getChildren()) {
            if (node instanceof HBox && GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) == 1) {
                HBox rowContainer = (HBox) node;
                Integer rowIndex = GridPane.getRowIndex(node);
                if (rowIndex != null && rowIndex >= 0 && rowIndex < resources.size()) {
                    Resource resource = resources.get(rowIndex);
                    
                    // Handle drag over event
                    rowContainer.setOnDragOver(event -> {
                        if (event.getGestureSource() != rowContainer && draggedAssignment != null) {
                            // Calculate duration
                            long duration = ChronoUnit.DAYS.between(
                                draggedAssignment.getStartDate(), 
                                draggedAssignment.getEndDate()
                            ) + 1;
                            
                            // Calculate position based on mouse and drag offset
                            double mouseX = event.getX();
                            
                            // Use the stored offset where the user clicked within the bar
                            double dragOffsetWithinBar = dragStartOffsetX;
                            
                            // Calculate the start position for the assignment
                            // This positions the bar so the cursor stays at the same relative position within the bar
                            double exactDayOffset = (mouseX - dragOffsetWithinBar) / dayWidth;
                            int dayOffset = (int) Math.round(exactDayOffset);
                            dayOffset = Math.max(0, dayOffset);
                            
                            LocalDate potentialDate = startDate.get().plusDays(dayOffset);
                            
                            // Accept the transfer if valid
                            event.acceptTransferModes(TransferMode.MOVE);
                            
                            // Visual feedback - highlight drop zone with gradient
                            rowContainer.setStyle(
                                "-fx-background-color: linear-gradient(to right, " +
                                "rgba(40, 167, 69, 0.05), rgba(40, 167, 69, 0.15), rgba(40, 167, 69, 0.05)); " +
                                "-fx-border-color: #28a745; -fx-border-width: 1 0 1 0;"
                            );
                            
                            // Clean up any existing ghost preview first
                            if (ghostPreview != null) {
                                // Remove from any parent it might be attached to
                                if (ghostPreview.getParent() != null) {
                                    ((Pane) ghostPreview.getParent()).getChildren().remove(ghostPreview);
                                }
                                ghostPreview = null;
                            }
                            
                            // Only create ghost if within valid bounds
                            long maxDays = ChronoUnit.DAYS.between(startDate.get(), endDate.get()) + 1;
                            if (dayOffset >= 0 && dayOffset + duration <= maxDays) {
                                // Check if this position would create a conflict
                                boolean wouldConflict = checkForConflictAtPosition(resource, potentialDate, 
                                    potentialDate.plusDays(duration - 1), draggedAssignment.getId());
                                
                                // Create new ghost preview
                                ghostPreview = new Label(draggedBar.getText());
                                ghostPreview.getStyleClass().add("ghost-preview");
                                
                                // Style based on whether this would create a conflict
                                if (wouldConflict) {
                                    ghostPreview.setStyle(
                                        "-fx-background-color: rgba(220, 53, 69, 0.3); " +
                                        "-fx-border-color: #dc3545; " +
                                        "-fx-border-width: 2; " +
                                        "-fx-border-style: dashed; " +
                                        "-fx-text-fill: #721c24; " +
                                        "-fx-font-weight: bold; " +
                                        "-fx-padding: 2 5 2 5;"
                                    );
                                    ghostPreview.setText("⚠ " + ghostPreview.getText() + " (CONFLICT!)");
                                } else {
                                    ghostPreview.setStyle(
                                        "-fx-background-color: rgba(40, 167, 69, 0.3); " +
                                        "-fx-border-color: #28a745; " +
                                        "-fx-border-width: 2; " +
                                        "-fx-border-style: dashed; " +
                                        "-fx-text-fill: #155724; " +
                                        "-fx-font-weight: bold; " +
                                        "-fx-padding: 2 5 2 5;"
                                    );
                                }
                                ghostPreview.setPrefHeight(projectBarHeight);
                                ghostPreview.setAlignment(Pos.CENTER);
                                
                                // Set width based on duration
                                ghostPreview.setPrefWidth(duration * dayWidth);
                                ghostPreview.setMaxWidth(duration * dayWidth);
                                ghostPreview.setMinWidth(duration * dayWidth);
                                
                                // Add spacer to position the ghost preview
                                Region spacer = new Region();
                                spacer.setPrefWidth(dayOffset * dayWidth);
                                spacer.setMinWidth(dayOffset * dayWidth);
                                spacer.setMaxWidth(dayOffset * dayWidth);
                                
                                // Create container for ghost with spacer
                                HBox ghostContainer = new HBox();
                                ghostContainer.setManaged(false); // Don't affect other children
                                ghostContainer.getChildren().addAll(spacer, ghostPreview);
                                
                                // Add to row container
                                rowContainer.getChildren().add(ghostContainer);
                                
                                // Position the container
                                double verticalPosition = (rowHeight - projectBarHeight) / 2;
                                ghostContainer.setLayoutY(verticalPosition);
                            }
                        }
                        event.consume();
                    });
                    
                    // Handle drag exited event
                    rowContainer.setOnDragExited(event -> {
                        // Remove visual feedback
                        rowContainer.setStyle("");
                        
                        // Clean up ghost preview completely
                        if (ghostPreview != null) {
                            if (rowContainer.getChildren().contains(ghostPreview)) {
                                rowContainer.getChildren().remove(ghostPreview);
                            }
                            // Also check if it's attached elsewhere
                            if (ghostPreview.getParent() != null) {
                                ((Pane) ghostPreview.getParent()).getChildren().remove(ghostPreview);
                            }
                            ghostPreview = null;
                        }
                        
                        event.consume();
                    });
                    
                    // Handle drop event
                    rowContainer.setOnDragDropped(event -> {
                        boolean success = false;
                        
                        if (draggedAssignment != null) {
                            // Calculate duration
                            long duration = ChronoUnit.DAYS.between(
                                draggedAssignment.getStartDate(), 
                                draggedAssignment.getEndDate()
                            );
                            
                            // Calculate position based on mouse and drag offset
                            double mouseX = event.getX();
                            
                            // Use the stored offset where the user clicked within the bar
                            double dragOffsetWithinBar = dragStartOffsetX;
                            
                            // Calculate the start position for the assignment
                            // The mouseX position represents where the cursor is in the rowContainer
                            // dragOffsetWithinBar is where the user clicked within the assignment bar
                            double barLeftEdgeX = mouseX - dragOffsetWithinBar;
                            double exactDayOffset = barLeftEdgeX / dayWidth;
                            int dayOffset = (int) Math.round(exactDayOffset);
                            dayOffset = Math.max(0, dayOffset);
                            
                            // Get the timeline start date
                            LocalDate timelineStartDate = startDate.get();
                            LocalDate newStartDate = timelineStartDate.plusDays(dayOffset);
                            LocalDate newEndDate = newStartDate.plusDays(duration);
                            
                            // Calculate original position for comparison
                            long originalDayOffset = ChronoUnit.DAYS.between(timelineStartDate, draggedAssignment.getStartDate());
                            
                            logger.info("DRAG DROP CALCULATION:");
                            logger.info("  Original assignment: {} to {} (day offset: {})", 
                                draggedAssignment.getStartDate(), draggedAssignment.getEndDate(), originalDayOffset);
                            logger.info("  Mouse X position in row: {}", mouseX);
                            logger.info("  Drag offset within bar: {}", dragOffsetWithinBar);
                            logger.info("  Calculated bar left edge X: {}", barLeftEdgeX);
                            logger.info("  Day width in pixels: {}", dayWidth);
                            logger.info("  Exact day offset: {}", exactDayOffset);
                            logger.info("  Rounded day offset: {}", dayOffset);
                            logger.info("  Timeline starts at: {}", timelineStartDate);
                            logger.info("  New assignment start: {} (timeline start + {} days)", newStartDate, dayOffset);
                            logger.info("  New assignment end: {} (start + {} days)", newEndDate, duration);
                            logger.info("  MOVEMENT: {} days (negative=left, positive=right)", dayOffset - originalDayOffset);
                            
                            logger.info("  Moving assignment {} to dates {} -> {} (was {} -> {})", 
                                draggedAssignment.getId(), newStartDate, newEndDate, 
                                draggedAssignment.getStartDate(), draggedAssignment.getEndDate());
                            
                            // Update the assignment
                            Assignment updatedAssignment = new Assignment(
                                draggedAssignment.getProjectId(),
                                resource.getId(),
                                newStartDate,
                                newEndDate,
                                draggedAssignment.getTravelOutDays(),
                                draggedAssignment.getTravelBackDays()
                            );
                            updatedAssignment.setId(draggedAssignment.getId());
                            updatedAssignment.setOverride(draggedAssignment.isOverride());
                            updatedAssignment.setOverrideReason(draggedAssignment.getOverrideReason());
                            updatedAssignment.setNotes(draggedAssignment.getNotes());
                            
                            // Fire edit event to update in database
                            if (onEditAssignment != null) {
                                onEditAssignment.accept(updatedAssignment);
                            }
                            
                            success = true;
                        }
                        
                        event.setDropCompleted(success);
                        event.consume();
                        
                        // Clear visual feedback
                        rowContainer.setStyle("");
                        
                        // Complete cleanup of ghost preview
                        if (ghostPreview != null) {
                            // Remove from current container if present
                            if (rowContainer.getChildren().contains(ghostPreview)) {
                                rowContainer.getChildren().remove(ghostPreview);
                            }
                            // Also remove from any parent it might be attached to
                            if (ghostPreview.getParent() != null) {
                                ((Pane) ghostPreview.getParent()).getChildren().remove(ghostPreview);
                            }
                            ghostPreview = null;
                        }
                        
                        // Clear ALL drag state
                        draggedAssignment = null;
                        draggedBar = null;
                        ghostPreview = null;
                    });
                }
            }
        }
    }
    
    private void showFinancialTrackingDialog(Project project) {
        try {
            // Get the data source from the configuration
            com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig dbConfig = 
                new com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig();
            
            // Get the project repository
            com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository projectRepository = 
                new com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository(dbConfig.getDataSource());
            
            // Create financial service
            FinancialService financialService = new FinancialService(dbConfig.getDataSource(), projectRepository);
            
            // Show the dialog with owner window for proper screen positioning
            javafx.stage.Window owner = getScene() != null ? getScene().getWindow() : null;
            FinancialTrackingDialog dialog = new FinancialTrackingDialog(project, financialService, owner);
            dialog.showAndWait();
        } catch (Exception e) {
            javafx.stage.Window errorOwner = getScene() != null ? getScene().getWindow() : null;
            Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.ERROR, errorOwner);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open Financial Tracking");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void showEditProjectDialog(Project project, Assignment assignment) {
        try {
            // Get the data source from the configuration
            com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig dbConfig = 
                new com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig();
            
            // Get the project repository
            com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository projectRepository = 
                new com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository(dbConfig.getDataSource());
            
            // Get project managers
            com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectManagerRepository pmRepository = 
                new com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectManagerRepository(dbConfig.getDataSource());
            List<com.subliminalsearch.simpleprojectresourcemanager.model.ProjectManager> managers = pmRepository.findAll();
            
            // Create a modified copy of the project with assignment dates for display
            Project projectCopy = new Project();
            projectCopy.setId(project.getId());
            projectCopy.setProjectId(project.getProjectId());
            projectCopy.setDescription(project.getDescription());
            projectCopy.setProjectManagerId(project.getProjectManagerId());
            projectCopy.setStatus(project.getStatus());
            
            // Use assignment dates for this edit session
            projectCopy.setStartDate(assignment.getStartDate());
            projectCopy.setEndDate(assignment.getEndDate());
            
            // Copy all client fields
            projectCopy.setContactName(project.getContactName());
            projectCopy.setContactEmail(project.getContactEmail());
            projectCopy.setContactPhone(project.getContactPhone());
            projectCopy.setContactCompany(project.getContactCompany());
            projectCopy.setContactRole(project.getContactRole());
            projectCopy.setContactAddress(project.getContactAddress());
            projectCopy.setClientProjectId(project.getClientProjectId());
            projectCopy.setClientProjectDescription(project.getClientProjectDescription());
            projectCopy.setSendReports(project.isSendReports());
            projectCopy.setReportFrequency(project.getReportFrequency());
            
            // Copy financial fields
            projectCopy.setBudgetAmount(project.getBudgetAmount());
            projectCopy.setActualCost(project.getActualCost());
            projectCopy.setRevenueAmount(project.getRevenueAmount());
            projectCopy.setCurrencyCode(project.getCurrencyCode());
            projectCopy.setLaborCost(project.getLaborCost());
            projectCopy.setMaterialCost(project.getMaterialCost());
            projectCopy.setTravelCost(project.getTravelCost());
            projectCopy.setOtherCost(project.getOtherCost());
            projectCopy.setCostNotes(project.getCostNotes());
            
            // Copy travel field (CRITICAL: was missing and causing travel checkbox bug!)
            projectCopy.setTravel(project.isTravel());
            
            // Copy timestamps
            projectCopy.setCreatedAt(project.getCreatedAt());
            projectCopy.setUpdatedAt(project.getUpdatedAt());
            
            // Create dialog with assignment dates
            com.subliminalsearch.simpleprojectresourcemanager.dialog.ProjectDialog dialog = 
                new com.subliminalsearch.simpleprojectresourcemanager.dialog.ProjectDialog(projectCopy, projectRepository, managers);
            
            // Set dialog title to indicate editing from assignment
            dialog.setTitle("Edit Project (Assignment: " + 
                assignment.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM d")) + " - " +
                assignment.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")) + ")");
            
            javafx.stage.Window owner = getScene() != null ? getScene().getWindow() : null;
            com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils.initializeDialog(dialog, owner);
            
            Optional<Project> result = dialog.showAndWait();
            if (result.isPresent()) {
                Project updatedProject = result.get();
                
                // Copy the updated client info back to the original project object
                project.setContactName(updatedProject.getContactName());
                project.setContactEmail(updatedProject.getContactEmail());
                project.setContactPhone(updatedProject.getContactPhone());
                project.setContactCompany(updatedProject.getContactCompany());
                project.setContactRole(updatedProject.getContactRole());
                project.setContactAddress(updatedProject.getContactAddress());
                project.setClientProjectId(updatedProject.getClientProjectId());
                project.setClientProjectDescription(updatedProject.getClientProjectDescription());
                project.setSendReports(updatedProject.isSendReports());
                project.setReportFrequency(updatedProject.getReportFrequency());
                project.setDescription(updatedProject.getDescription());
                project.setStatus(updatedProject.getStatus());
                project.setProjectManagerId(updatedProject.getProjectManagerId());
                
                // Copy travel field back to original project object (CRITICAL: was missing!)
                project.setTravel(updatedProject.isTravel());
                
                // Update the project using the original project object with updated fields
                projectRepository.update(project);
                
                // Refresh the timeline to show the updated information
                refreshTimeline();
                
                // Show success message
                Alert alert = com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils.createScreenAwareAlert(Alert.AlertType.INFORMATION, owner);
                alert.setTitle("Success");
                alert.setHeaderText("Project Updated");
                alert.setContentText("Project and client information has been updated successfully.");
                alert.showAndWait();
            }
            
        } catch (Exception e) {
            logger.error("Failed to edit project from timeline", e);
            e.printStackTrace();
            javafx.stage.Window errorOwner = getScene() != null ? getScene().getWindow() : null;
            Alert alert = com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils.createScreenAwareAlert(Alert.AlertType.ERROR, errorOwner);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to edit project");
            alert.setContentText("Failed to update project: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
            alert.showAndWait();
        }
    }
}