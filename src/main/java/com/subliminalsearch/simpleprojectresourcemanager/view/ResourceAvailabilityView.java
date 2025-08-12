package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceAvailabilityView extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ResourceAvailabilityView.class);
    
    // UI Components
    private ComboBox<Resource> resourceCombo;
    private ComboBox<Integer> yearCombo;
    private ComboBox<String> monthCombo;
    private RadioButton monthViewRadio;
    private RadioButton weekViewRadio;
    private GridPane availabilityGrid;
    private Label viewHeaderLabel;
    private VBox legendBox;
    private Label statusLabel;
    
    // Data
    private ObservableList<Resource> resources = FXCollections.observableArrayList();
    private ObservableList<Assignment> assignments = FXCollections.observableArrayList();
    private ObservableList<TechnicianUnavailability> unavailabilities = FXCollections.observableArrayList();
    private ObservableList<CompanyHoliday> holidays = FXCollections.observableArrayList();
    
    // Current view state
    private Resource selectedResource;
    private YearMonth currentYearMonth;
    private LocalDate currentWeekStart;
    private boolean isMonthView = true;
    
    // Colors for different statuses
    private static final String COLOR_AVAILABLE = "#c8e6c9";       // Light green
    private static final String COLOR_ASSIGNED = "#ffccbc";        // Light orange
    private static final String COLOR_UNAVAILABLE = "#ffcdd2";     // Light red
    private static final String COLOR_HOLIDAY = "#e1bee7";         // Light purple
    private static final String COLOR_WEEKEND = "#f5f5f5";         // Light gray
    private static final String COLOR_CONFLICT = "#dc3545";        // Red
    private static final String COLOR_PARTIAL = "#fff3cd";         // Light yellow
    
    public ResourceAvailabilityView() {
        this.currentYearMonth = YearMonth.now();
        this.currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        initializeComponent();
        setupEventHandlers();
    }
    
    private void initializeComponent() {
        // Create toolbar
        ToolBar toolBar = createToolBar();
        setTop(toolBar);
        
        // Create main content area
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.85);
        
        // Left side - Calendar grid
        BorderPane calendarPane = new BorderPane();
        
        // View header
        viewHeaderLabel = new Label();
        viewHeaderLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        viewHeaderLabel.setPadding(new Insets(10));
        HBox headerBox = new HBox(viewHeaderLabel);
        headerBox.setAlignment(Pos.CENTER);
        calendarPane.setTop(headerBox);
        
        // Availability grid
        availabilityGrid = new GridPane();
        availabilityGrid.setHgap(1);
        availabilityGrid.setVgap(1);
        availabilityGrid.setPadding(new Insets(10));
        availabilityGrid.setStyle("-fx-background-color: #cccccc;");
        
        ScrollPane gridScrollPane = new ScrollPane(availabilityGrid);
        gridScrollPane.setFitToWidth(true);
        gridScrollPane.setFitToHeight(true);
        calendarPane.setCenter(gridScrollPane);
        
        // Right side - Legend and statistics
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setMinWidth(200);
        
        Label legendTitle = new Label("Legend");
        legendTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        legendBox = new VBox(5);
        legendBox.getChildren().addAll(
            legendTitle,
            createLegendItem("Available", COLOR_AVAILABLE),
            createLegendItem("Assigned", COLOR_ASSIGNED),
            createLegendItem("Unavailable", COLOR_UNAVAILABLE),
            createLegendItem("Company Holiday", COLOR_HOLIDAY),
            createLegendItem("Weekend", COLOR_WEEKEND),
            createLegendItem("Conflict", COLOR_CONFLICT),
            createLegendItem("Partial Day", COLOR_PARTIAL)
        );
        
        Separator separator = new Separator();
        
        Label statsTitle = new Label("Statistics");
        statsTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        VBox statsBox = new VBox(5);
        statsBox.setId("statsBox");
        
        rightPanel.getChildren().addAll(legendBox, separator, statsTitle, statsBox);
        
        splitPane.getItems().addAll(calendarPane, rightPanel);
        setCenter(splitPane);
        
        // Status bar
        statusLabel = new Label("Select a resource to view availability");
        statusLabel.setPadding(new Insets(5));
        setBottom(statusLabel);
    }
    
    private ToolBar createToolBar() {
        // Resource selector
        Label resourceLabel = new Label("Resource:");
        resourceCombo = new ComboBox<>();
        resourceCombo.setPrefWidth(200);
        resourceCombo.setPromptText("Select a resource");
        
        // Set up string converter for proper display
        resourceCombo.setConverter(new javafx.util.StringConverter<Resource>() {
            @Override
            public String toString(Resource resource) {
                if (resource == null) return "";
                return resource.getName() + (resource.getResourceType() != null ? 
                    " (" + resource.getResourceType().getName() + ")" : "");
            }
            
            @Override
            public Resource fromString(String string) {
                return resources.stream()
                    .filter(r -> toString(r).equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        // View type toggle
        ToggleGroup viewGroup = new ToggleGroup();
        monthViewRadio = new RadioButton("Month");
        monthViewRadio.setToggleGroup(viewGroup);
        monthViewRadio.setSelected(true);
        
        weekViewRadio = new RadioButton("Week");
        weekViewRadio.setToggleGroup(viewGroup);
        
        // Year selector
        yearCombo = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 2; year <= currentYear + 2; year++) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setValue(currentYear);
        
        // Month selector
        monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        );
        monthCombo.setValue(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        
        // Navigation buttons
        Button prevButton = new Button("◀");
        prevButton.setOnAction(e -> navigatePrevious());
        
        Button nextButton = new Button("▶");
        nextButton.setOnAction(e -> navigateNext());
        
        Button todayButton = new Button("Today");
        todayButton.setOnAction(e -> navigateToToday());
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> updateAvailabilityView());
        
        Separator sep1 = new Separator();
        Separator sep2 = new Separator();
        Separator sep3 = new Separator();
        
        return new ToolBar(
            resourceLabel, resourceCombo,
            sep1,
            monthViewRadio, weekViewRadio,
            sep2,
            new Label("Year:"), yearCombo,
            new Label("Month:"), monthCombo,
            sep3,
            prevButton, todayButton, nextButton,
            refreshButton
        );
    }
    
    private HBox createLegendItem(String label, String color) {
        Rectangle rect = new Rectangle(20, 20);
        rect.setFill(Color.web(color));
        rect.setStroke(Color.GRAY);
        
        Label text = new Label(label);
        
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getChildren().addAll(rect, text);
        
        return item;
    }
    
    private void setupEventHandlers() {
        // Resource selection
        resourceCombo.setOnAction(e -> {
            selectedResource = resourceCombo.getValue();
            updateAvailabilityView();
        });
        
        // View type toggle
        monthViewRadio.setOnAction(e -> {
            isMonthView = true;
            updateAvailabilityView();
        });
        
        weekViewRadio.setOnAction(e -> {
            isMonthView = false;
            updateAvailabilityView();
        });
        
        // Year/Month selection
        yearCombo.setOnAction(e -> {
            if (yearCombo.getValue() != null) {
                currentYearMonth = YearMonth.of(yearCombo.getValue(), 
                    currentYearMonth.getMonth().getValue());
                updateAvailabilityView();
            }
        });
        
        monthCombo.setOnAction(e -> {
            if (monthCombo.getValue() != null) {
                int monthIndex = monthCombo.getSelectionModel().getSelectedIndex() + 1;
                currentYearMonth = YearMonth.of(currentYearMonth.getYear(), monthIndex);
                updateAvailabilityView();
            }
        });
    }
    
    private void updateAvailabilityView() {
        if (selectedResource == null) {
            availabilityGrid.getChildren().clear();
            statusLabel.setText("Please select a resource");
            return;
        }
        
        if (isMonthView) {
            updateMonthView();
        } else {
            updateWeekView();
        }
        
        updateStatistics();
    }
    
    private void updateMonthView() {
        availabilityGrid.getChildren().clear();
        availabilityGrid.getColumnConstraints().clear();
        availabilityGrid.getRowConstraints().clear();
        
        // Update header
        viewHeaderLabel.setText(selectedResource.getName() + " - " +
            currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + 
            " " + currentYearMonth.getYear());
        
        // Set up column constraints for equal width columns
        for (int i = 0; i < 7; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            colConstraints.setPercentWidth(100.0 / 7.0);
            colConstraints.setHgrow(Priority.ALWAYS);
            colConstraints.setFillWidth(true);
            availabilityGrid.getColumnConstraints().add(colConstraints);
        }
        
        // Set up row constraints
        // Header row - fixed height
        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(30);
        headerRow.setMinHeight(30);
        headerRow.setMaxHeight(30);
        availabilityGrid.getRowConstraints().add(headerRow);
        
        // Calculate number of calendar rows needed
        LocalDate firstDay = currentYearMonth.atDay(1);
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;
        int totalDays = currentYearMonth.lengthOfMonth();
        int numRows = (int) Math.ceil((firstDayOfWeek + totalDays) / 7.0);
        
        // Add row constraints for calendar rows
        for (int i = 0; i < numRows; i++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setPercentHeight(100.0 / numRows);
            rowConstraints.setVgrow(Priority.ALWAYS);
            rowConstraints.setFillHeight(true);
            availabilityGrid.getRowConstraints().add(rowConstraints);
        }
        
        // Add day of week headers
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setMaxHeight(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #999999;");
            GridPane.setFillWidth(dayLabel, true);
            GridPane.setFillHeight(dayLabel, true);
            availabilityGrid.add(dayLabel, i, 0);
        }
        
        // Fill calendar with days
        int row = 1;
        int col = firstDayOfWeek;
        
        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate date = currentYearMonth.atDay(day);
            VBox dayCell = createAvailabilityDayCell(date);
            
            availabilityGrid.add(dayCell, col, row);
            
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
        
        statusLabel.setText("Showing availability for " + selectedResource.getName());
    }
    
    private void updateWeekView() {
        availabilityGrid.getChildren().clear();
        availabilityGrid.getColumnConstraints().clear();
        availabilityGrid.getRowConstraints().clear();
        
        // Calculate week dates
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        
        // Update header
        viewHeaderLabel.setText(selectedResource.getName() + " - Week of " +
            currentWeekStart.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " +
            weekEnd.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        
        // Set up column constraints for equal width columns (7 days + 1 time label column)
        for (int i = 0; i < 8; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            if (i < 7) {
                colConstraints.setPercentWidth(100.0 / 8.0);
            } else {
                colConstraints.setPrefWidth(80);
                colConstraints.setMinWidth(80);
            }
            colConstraints.setHgrow(Priority.ALWAYS);
            colConstraints.setFillWidth(true);
            availabilityGrid.getColumnConstraints().add(colConstraints);
        }
        
        // Set up row constraints
        // Header row - fixed height
        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(30);
        headerRow.setMinHeight(30);
        headerRow.setMaxHeight(30);
        availabilityGrid.getRowConstraints().add(headerRow);
        
        // Time slot rows - equal height
        for (int i = 0; i < 3; i++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setPercentHeight(100.0 / 3.0);
            rowConstraints.setVgrow(Priority.ALWAYS);
            rowConstraints.setFillHeight(true);
            availabilityGrid.getRowConstraints().add(rowConstraints);
        }
        
        // Create day headers
        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            Label dayLabel = new Label(date.format(DateTimeFormatter.ofPattern("EEE dd")));
            dayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setMaxHeight(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #999999;");
            GridPane.setFillWidth(dayLabel, true);
            GridPane.setFillHeight(dayLabel, true);
            availabilityGrid.add(dayLabel, i, 0);
        }
        
        // Add time slots for each day
        String[] timeSlots = {"Morning", "Afternoon", "Evening"};
        for (int slot = 0; slot < timeSlots.length; slot++) {
            Label timeLabel = new Label(timeSlots[slot]);
            timeLabel.setMaxWidth(Double.MAX_VALUE);
            timeLabel.setMaxHeight(Double.MAX_VALUE);
            timeLabel.setAlignment(Pos.CENTER_RIGHT);
            timeLabel.setPadding(new Insets(0, 10, 0, 0));
            timeLabel.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #999999;");
            GridPane.setFillWidth(timeLabel, true);
            GridPane.setFillHeight(timeLabel, true);
            availabilityGrid.add(timeLabel, 7, slot + 1);
            
            for (int day = 0; day < 7; day++) {
                LocalDate date = currentWeekStart.plusDays(day);
                VBox slotCell = createAvailabilitySlotCell(date, slot);
                availabilityGrid.add(slotCell, day, slot + 1);
            }
        }
        
        statusLabel.setText("Showing week availability for " + selectedResource.getName());
    }
    
    private VBox createAvailabilityDayCell(LocalDate date) {
        VBox cell = new VBox(3);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMaxHeight(Double.MAX_VALUE);
        cell.setPadding(new Insets(5));
        cell.setAlignment(Pos.TOP_LEFT);
        
        // Make the cell fill the grid cell
        GridPane.setFillWidth(cell, true);
        GridPane.setFillHeight(cell, true);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        GridPane.setVgrow(cell, Priority.ALWAYS);
        
        // Day number
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        // Determine availability status
        AvailabilityStatus status = getAvailabilityStatus(date);
        
        // Style based on status
        switch (status.type) {
            case AVAILABLE:
                cell.setStyle("-fx-background-color: " + COLOR_AVAILABLE + "; -fx-border-color: #999999;");
                dayLabel.setTextFill(Color.DARKGREEN);
                break;
            case ASSIGNED:
                cell.setStyle("-fx-background-color: " + COLOR_ASSIGNED + "; -fx-border-color: #999999;");
                dayLabel.setTextFill(Color.DARKORANGE);
                break;
            case UNAVAILABLE:
                cell.setStyle("-fx-background-color: " + COLOR_UNAVAILABLE + "; -fx-border-color: #999999;");
                dayLabel.setTextFill(Color.DARKRED);
                break;
            case HOLIDAY:
                cell.setStyle("-fx-background-color: " + COLOR_HOLIDAY + "; -fx-border-color: #999999;");
                dayLabel.setTextFill(Color.PURPLE);
                break;
            case WEEKEND:
                cell.setStyle("-fx-background-color: " + COLOR_WEEKEND + "; -fx-border-color: #999999;");
                dayLabel.setTextFill(Color.GRAY);
                break;
            case CONFLICT:
                cell.setStyle("-fx-background-color: " + COLOR_CONFLICT + "; -fx-border-color: #999999;");
                dayLabel.setTextFill(Color.WHITE);
                break;
            case PARTIAL:
                cell.setStyle("-fx-background-color: " + COLOR_PARTIAL + "; -fx-border-color: #999999;");
                dayLabel.setTextFill(Color.DARKGOLDENROD);
                break;
        }
        
        cell.getChildren().add(dayLabel);
        
        // Add status label if needed
        if (status.label != null && !status.label.isEmpty()) {
            Label statusLabel = new Label(status.label);
            statusLabel.setFont(Font.font("System", 18));
            statusLabel.setWrapText(true);
            statusLabel.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(statusLabel, Priority.ALWAYS);
            cell.getChildren().add(statusLabel);
        }
        
        // Add tooltip with details
        if (status.details != null && !status.details.isEmpty()) {
            Tooltip tooltip = new Tooltip(status.details);
            Tooltip.install(cell, tooltip);
        }
        
        // Highlight today
        if (date.equals(LocalDate.now())) {
            cell.setStyle(cell.getStyle() + " -fx-border-width: 2; -fx-border-color: #2196f3;");
        }
        
        return cell;
    }
    
    private VBox createAvailabilitySlotCell(LocalDate date, int slot) {
        VBox cell = new VBox(2);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMaxHeight(Double.MAX_VALUE);
        cell.setPadding(new Insets(3));
        cell.setAlignment(Pos.CENTER);
        
        // Make the cell fill the grid cell
        GridPane.setFillWidth(cell, true);
        GridPane.setFillHeight(cell, true);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        GridPane.setVgrow(cell, Priority.ALWAYS);
        
        // Determine availability for this time slot
        AvailabilityStatus status = getAvailabilityStatus(date);
        
        // Style based on status
        String bgColor = COLOR_AVAILABLE;
        switch (status.type) {
            case AVAILABLE: bgColor = COLOR_AVAILABLE; break;
            case ASSIGNED: bgColor = COLOR_ASSIGNED; break;
            case UNAVAILABLE: bgColor = COLOR_UNAVAILABLE; break;
            case HOLIDAY: bgColor = COLOR_HOLIDAY; break;
            case WEEKEND: bgColor = COLOR_WEEKEND; break;
            case CONFLICT: bgColor = COLOR_CONFLICT; break;
            case PARTIAL: bgColor = COLOR_PARTIAL; break;
        }
        
        cell.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: #999999;");
        
        // Add abbreviated status label
        if (status.label != null && !status.label.isEmpty()) {
            Label statusLabel = new Label(status.label.length() > 15 ? 
                status.label.substring(0, 12) + "..." : status.label);
            statusLabel.setFont(Font.font("System", 18));
            cell.getChildren().add(statusLabel);
        }
        
        // Add tooltip
        if (status.details != null && !status.details.isEmpty()) {
            Tooltip tooltip = new Tooltip(status.details);
            Tooltip.install(cell, tooltip);
        }
        
        return cell;
    }
    
    private AvailabilityStatus getAvailabilityStatus(LocalDate date) {
        AvailabilityStatus status = new AvailabilityStatus();
        
        // Check if weekend
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            status.type = AvailabilityType.WEEKEND;
            status.label = "Weekend";
            status.details = "Weekend";
        }
        
        // Check for company holidays
        Optional<CompanyHoliday> holiday = holidays.stream()
            .filter(h -> h.getDate().equals(date))
            .findFirst();
        
        if (holiday.isPresent()) {
            status.type = AvailabilityType.HOLIDAY;
            status.label = holiday.get().getName();
            status.details = holiday.get().getName() + 
                (holiday.get().isWorkingHolidayAllowed() ? " (Working)" : " (Day Off)");
            
            if (holiday.get().isWorkingHolidayAllowed()) {
                // Still check for assignments on working holidays
                status.type = AvailabilityType.PARTIAL;
            } else {
                return status; // Day off, no need to check further
            }
        }
        
        // Check for unavailability
        List<TechnicianUnavailability> dayUnavailabilities = unavailabilities.stream()
            .filter(u -> u.getResourceId().equals(selectedResource.getId()))
            .filter(u -> !date.isBefore(u.getStartDate()) && !date.isAfter(u.getEndDate()))
            .collect(Collectors.toList());
        
        if (!dayUnavailabilities.isEmpty()) {
            status.type = AvailabilityType.UNAVAILABLE;
            status.label = dayUnavailabilities.get(0).getType().toString();
            status.details = dayUnavailabilities.stream()
                .map(u -> u.getType().toString() + 
                    (u.getReason() != null ? ": " + u.getReason() : ""))
                .collect(Collectors.joining("\n"));
            return status;
        }
        
        // Check for assignments
        List<Assignment> dayAssignments = assignments.stream()
            .filter(a -> a.getResourceId().equals(selectedResource.getId()))
            .filter(a -> !date.isBefore(a.getStartDate()) && !date.isAfter(a.getEndDate()))
            .collect(Collectors.toList());
        
        if (dayAssignments.size() > 1) {
            // Conflict - multiple assignments
            status.type = AvailabilityType.CONFLICT;
            status.label = "CONFLICT!";
            status.details = "Multiple assignments:\n" + 
                dayAssignments.stream()
                    .map(a -> "Project " + a.getProjectId())
                    .collect(Collectors.joining("\n"));
        } else if (dayAssignments.size() == 1) {
            // Single assignment
            status.type = AvailabilityType.ASSIGNED;
            Assignment assignment = dayAssignments.get(0);
            status.label = "Project " + assignment.getProjectId();
            status.details = "Assigned to Project " + assignment.getProjectId() + "\n" +
                "From " + assignment.getStartDate() + " to " + assignment.getEndDate();
        } else if (status.type != AvailabilityType.WEEKEND && 
                   status.type != AvailabilityType.HOLIDAY &&
                   status.type != AvailabilityType.PARTIAL) {
            // Available
            status.type = AvailabilityType.AVAILABLE;
            status.label = "";
            status.details = "Available for assignment";
        }
        
        return status;
    }
    
    private void updateStatistics() {
        VBox statsBox = (VBox) getCenter().lookup("#statsBox");
        if (statsBox != null && selectedResource != null) {
            statsBox.getChildren().clear();
            
            LocalDate startDate = isMonthView ? currentYearMonth.atDay(1) : currentWeekStart;
            LocalDate endDate = isMonthView ? currentYearMonth.atEndOfMonth() : currentWeekStart.plusDays(6);
            
            long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            long weekendDays = 0;
            long availableDays = 0;
            long assignedDays = 0;
            long unavailableDays = 0;
            long holidayDays = 0;
            
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                AvailabilityStatus status = getAvailabilityStatus(date);
                switch (status.type) {
                    case AVAILABLE: availableDays++; break;
                    case ASSIGNED: assignedDays++; break;
                    case UNAVAILABLE: unavailableDays++; break;
                    case HOLIDAY: holidayDays++; break;
                    case WEEKEND: weekendDays++; break;
                    case CONFLICT: assignedDays++; break; // Count as assigned
                    case PARTIAL: assignedDays++; break;  // Count as assigned
                }
            }
            
            double utilizationRate = totalDays > 0 ? 
                (double) assignedDays / (totalDays - weekendDays - holidayDays) * 100 : 0;
            
            statsBox.getChildren().addAll(
                new Label(String.format("Total Days: %d", totalDays)),
                new Label(String.format("Available: %d", availableDays)),
                new Label(String.format("Assigned: %d", assignedDays)),
                new Label(String.format("Unavailable: %d", unavailableDays)),
                new Label(String.format("Holidays: %d", holidayDays)),
                new Label(String.format("Weekends: %d", weekendDays)),
                new Separator(),
                new Label(String.format("Utilization: %.1f%%", utilizationRate))
            );
        }
    }
    
    private void navigatePrevious() {
        if (isMonthView) {
            currentYearMonth = currentYearMonth.minusMonths(1);
            yearCombo.setValue(currentYearMonth.getYear());
            monthCombo.setValue(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        } else {
            currentWeekStart = currentWeekStart.minusWeeks(1);
            currentYearMonth = YearMonth.from(currentWeekStart);
            yearCombo.setValue(currentYearMonth.getYear());
            monthCombo.setValue(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        }
        updateAvailabilityView();
    }
    
    private void navigateNext() {
        if (isMonthView) {
            currentYearMonth = currentYearMonth.plusMonths(1);
            yearCombo.setValue(currentYearMonth.getYear());
            monthCombo.setValue(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        } else {
            currentWeekStart = currentWeekStart.plusWeeks(1);
            currentYearMonth = YearMonth.from(currentWeekStart);
            yearCombo.setValue(currentYearMonth.getYear());
            monthCombo.setValue(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        }
        updateAvailabilityView();
    }
    
    private void navigateToToday() {
        currentYearMonth = YearMonth.now();
        currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        yearCombo.setValue(currentYearMonth.getYear());
        monthCombo.setValue(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        updateAvailabilityView();
    }
    
    // Public methods for data management
    public void setResources(List<Resource> resourceList) {
        this.resources.clear();
        this.resources.addAll(resourceList);
        
        resourceCombo.setItems(this.resources);
        if (!resources.isEmpty() && selectedResource == null) {
            resourceCombo.setValue(resources.get(0));
            selectedResource = resources.get(0);
        }
    }
    
    public void setAssignments(List<Assignment> assignmentList) {
        this.assignments.clear();
        this.assignments.addAll(assignmentList);
        updateAvailabilityView();
    }
    
    public void setUnavailabilities(List<TechnicianUnavailability> unavailabilityList) {
        this.unavailabilities.clear();
        this.unavailabilities.addAll(unavailabilityList);
        updateAvailabilityView();
    }
    
    public void setHolidays(List<CompanyHoliday> holidayList) {
        this.holidays.clear();
        this.holidays.addAll(holidayList);
        updateAvailabilityView();
    }
    
    // Inner classes
    private static class AvailabilityStatus {
        AvailabilityType type = AvailabilityType.AVAILABLE;
        String label = "";
        String details = "";
    }
    
    private enum AvailabilityType {
        AVAILABLE, ASSIGNED, UNAVAILABLE, HOLIDAY, WEEKEND, CONFLICT, PARTIAL
    }
}