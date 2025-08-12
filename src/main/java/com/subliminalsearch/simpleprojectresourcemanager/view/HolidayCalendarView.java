package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.CompanyHoliday;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Consumer;

public class HolidayCalendarView extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(HolidayCalendarView.class);
    
    // UI Components
    private TabPane tabPane;
    private TableView<CompanyHoliday> holidayTable;
    private GridPane calendarGrid;
    private ComboBox<Integer> yearCombo;
    private ComboBox<String> monthCombo;
    private Label monthYearLabel;
    
    // Data
    private ObservableList<CompanyHoliday> holidays = FXCollections.observableArrayList();
    private YearMonth currentYearMonth;
    
    // Callbacks
    private Consumer<CompanyHoliday> onEditHoliday;
    private Consumer<CompanyHoliday> onDeleteHoliday;
    private Runnable onAddHoliday;
    
    public HolidayCalendarView() {
        this.currentYearMonth = YearMonth.now();
        initializeComponent();
        setupEventHandlers();
    }
    
    private void initializeComponent() {
        // Create toolbar
        ToolBar toolBar = createToolBar();
        setTop(toolBar);
        
        // Create tab pane with list and calendar views
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // List view tab
        Tab listTab = new Tab("List View");
        listTab.setContent(createListView());
        
        // Calendar view tab
        Tab calendarTab = new Tab("Calendar View");
        calendarTab.setContent(createCalendarView());
        
        tabPane.getTabs().addAll(listTab, calendarTab);
        setCenter(tabPane);
        
        // Status bar
        Label statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(5));
        setBottom(statusLabel);
    }
    
    private ToolBar createToolBar() {
        Button addButton = new Button("Add Holiday");
        addButton.setOnAction(e -> {
            if (onAddHoliday != null) {
                onAddHoliday.run();
            }
        });
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshViews());
        
        // Year selector
        yearCombo = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 5; year <= currentYear + 5; year++) {
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
        
        Button prevMonthButton = new Button("◀");
        prevMonthButton.setOnAction(e -> navigatePreviousMonth());
        
        Button nextMonthButton = new Button("▶");
        nextMonthButton.setOnAction(e -> navigateNextMonth());
        
        Button todayButton = new Button("Today");
        todayButton.setOnAction(e -> navigateToToday());
        
        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        
        return new ToolBar(
            addButton,
            refreshButton,
            separator1,
            new Label("Year:"), yearCombo,
            new Label("Month:"), monthCombo,
            separator2,
            prevMonthButton,
            todayButton,
            nextMonthButton
        );
    }
    
    private ScrollPane createListView() {
        // Create table for holiday list
        holidayTable = new TableView<>();
        holidayTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Date column
        TableColumn<CompanyHoliday, LocalDate> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateColumn.setCellFactory(col -> new TableCell<CompanyHoliday, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy (EEEE)")));
                }
            }
        });
        dateColumn.setPrefWidth(200);
        
        // Name column
        TableColumn<CompanyHoliday, String> nameColumn = new TableColumn<>("Holiday Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(250);
        
        // Description column
        TableColumn<CompanyHoliday, String> descColumn = new TableColumn<>("Description");
        descColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descColumn.setPrefWidth(300);
        
        // Working day column
        TableColumn<CompanyHoliday, Boolean> workingColumn = new TableColumn<>("Working Day");
        workingColumn.setCellValueFactory(new PropertyValueFactory<>("workingDay"));
        workingColumn.setCellFactory(col -> new TableCell<CompanyHoliday, Boolean>() {
            @Override
            protected void updateItem(Boolean working, boolean empty) {
                super.updateItem(working, empty);
                if (empty || working == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(working ? "Yes" : "No");
                    if (working) {
                        setStyle("-fx-text-fill: #ff9800;");
                    } else {
                        setStyle("-fx-text-fill: #4caf50;");
                    }
                }
            }
        });
        workingColumn.setPrefWidth(100);
        
        // Actions column
        TableColumn<CompanyHoliday, Void> actionsColumn = new TableColumn<>("Actions");
        actionsColumn.setCellFactory(col -> new TableCell<CompanyHoliday, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttons = new HBox(5, editButton, deleteButton);
            
            {
                editButton.setOnAction(e -> {
                    CompanyHoliday holiday = getTableRow().getItem();
                    if (holiday != null && onEditHoliday != null) {
                        onEditHoliday.accept(holiday);
                    }
                });
                
                deleteButton.setOnAction(e -> {
                    CompanyHoliday holiday = getTableRow().getItem();
                    if (holiday != null && onDeleteHoliday != null) {
                        onDeleteHoliday.accept(holiday);
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
        actionsColumn.setPrefWidth(150);
        
        holidayTable.getColumns().addAll(dateColumn, nameColumn, descColumn, workingColumn, actionsColumn);
        holidayTable.setItems(holidays);
        
        // Add context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editMenuItem = new MenuItem("Edit");
        editMenuItem.setOnAction(e -> {
            CompanyHoliday selected = holidayTable.getSelectionModel().getSelectedItem();
            if (selected != null && onEditHoliday != null) {
                onEditHoliday.accept(selected);
            }
        });
        
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(e -> {
            CompanyHoliday selected = holidayTable.getSelectionModel().getSelectedItem();
            if (selected != null && onDeleteHoliday != null) {
                onDeleteHoliday.accept(selected);
            }
        });
        
        contextMenu.getItems().addAll(editMenuItem, deleteMenuItem);
        holidayTable.setContextMenu(contextMenu);
        
        ScrollPane scrollPane = new ScrollPane(holidayTable);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        return scrollPane;
    }
    
    private BorderPane createCalendarView() {
        BorderPane calendarPane = new BorderPane();
        
        // Month/Year header
        monthYearLabel = new Label();
        monthYearLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        monthYearLabel.setPadding(new Insets(10));
        
        HBox headerBox = new HBox(monthYearLabel);
        headerBox.setAlignment(Pos.CENTER);
        calendarPane.setTop(headerBox);
        
        // Calendar grid
        calendarGrid = new GridPane();
        calendarGrid.setHgap(1);
        calendarGrid.setVgap(1);
        calendarGrid.setPadding(new Insets(10));
        calendarGrid.setStyle("-fx-background-color: #cccccc;");
        
        ScrollPane scrollPane = new ScrollPane(calendarGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        calendarPane.setCenter(scrollPane);
        
        updateCalendarView();
        
        return calendarPane;
    }
    
    private void updateCalendarView() {
        calendarGrid.getChildren().clear();
        
        // Update header
        monthYearLabel.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + 
                               " " + currentYearMonth.getYear());
        
        // Add day of week headers
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            dayLabel.setPrefWidth(100);
            dayLabel.setPrefHeight(30);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #999999;");
            calendarGrid.add(dayLabel, i, 0);
        }
        
        // Get first day of month and calculate grid position
        LocalDate firstDay = currentYearMonth.atDay(1);
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue() % 7; // Convert to Sunday=0
        
        // Get holidays for this month
        Map<LocalDate, CompanyHoliday> holidayMap = new HashMap<>();
        for (CompanyHoliday holiday : holidays) {
            if (YearMonth.from(holiday.getDate()).equals(currentYearMonth)) {
                holidayMap.put(holiday.getDate(), holiday);
            }
        }
        
        // Fill calendar with days
        int row = 1;
        int col = firstDayOfWeek;
        
        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate date = currentYearMonth.atDay(day);
            VBox dayCell = createDayCell(date, holidayMap.get(date));
            
            calendarGrid.add(dayCell, col, row);
            
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }
    
    private VBox createDayCell(LocalDate date, CompanyHoliday holiday) {
        VBox cell = new VBox(5);
        cell.setPrefWidth(100);
        cell.setPrefHeight(80);
        cell.setPadding(new Insets(5));
        cell.setAlignment(Pos.TOP_LEFT);
        
        // Day number
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Style based on day type
        if (holiday != null) {
            if (holiday.isWorkingHolidayAllowed()) {
                // Working holiday - orange
                cell.setStyle("-fx-background-color: #ffe0b2; -fx-border-color: #999999;");
                dayLabel.setTextFill(Color.DARKORANGE);
            } else {
                // Non-working holiday - green
                cell.setStyle("-fx-background-color: #c8e6c9; -fx-border-color: #999999;");
                dayLabel.setTextFill(Color.DARKGREEN);
            }
            
            // Add holiday name
            Label holidayLabel = new Label(holiday.getName());
            holidayLabel.setFont(Font.font("System", 10));
            holidayLabel.setWrapText(true);
            holidayLabel.setMaxWidth(90);
            
            // Add tooltip with full details
            Tooltip tooltip = new Tooltip(
                holiday.getName() + "\n" +
                (holiday.getDescription() != null ? holiday.getDescription() + "\n" : "") +
                (holiday.isWorkingHolidayAllowed() ? "Working Day" : "Day Off")
            );
            Tooltip.install(cell, tooltip);
            
            cell.getChildren().addAll(dayLabel, holidayLabel);
        } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            // Weekend - light gray
            cell.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #999999;");
            dayLabel.setTextFill(Color.GRAY);
            cell.getChildren().add(dayLabel);
        } else {
            // Weekday - white
            cell.setStyle("-fx-background-color: white; -fx-border-color: #999999;");
            cell.getChildren().add(dayLabel);
        }
        
        // Highlight today
        if (date.equals(LocalDate.now())) {
            dayLabel.setStyle("-fx-underline: true;");
            cell.setStyle(cell.getStyle() + " -fx-border-width: 2; -fx-border-color: #2196f3;");
        }
        
        return cell;
    }
    
    private void setupEventHandlers() {
        // Year/Month combo box handlers
        yearCombo.setOnAction(e -> {
            if (yearCombo.getValue() != null) {
                currentYearMonth = YearMonth.of(yearCombo.getValue(), 
                    currentYearMonth.getMonth().getValue());
                updateCalendarView();
            }
        });
        
        monthCombo.setOnAction(e -> {
            if (monthCombo.getValue() != null) {
                int monthIndex = monthCombo.getSelectionModel().getSelectedIndex() + 1;
                currentYearMonth = YearMonth.of(currentYearMonth.getYear(), monthIndex);
                updateCalendarView();
            }
        });
        
        // Tab change handler
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && "Calendar View".equals(newTab.getText())) {
                updateCalendarView();
            }
        });
    }
    
    private void navigatePreviousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        yearCombo.setValue(currentYearMonth.getYear());
        monthCombo.setValue(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        updateCalendarView();
    }
    
    private void navigateNextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        yearCombo.setValue(currentYearMonth.getYear());
        monthCombo.setValue(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        updateCalendarView();
    }
    
    private void navigateToToday() {
        currentYearMonth = YearMonth.now();
        yearCombo.setValue(currentYearMonth.getYear());
        monthCombo.setValue(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        updateCalendarView();
    }
    
    private void refreshViews() {
        holidayTable.refresh();
        updateCalendarView();
    }
    
    // Public methods
    public void setHolidays(List<CompanyHoliday> holidayList) {
        this.holidays.clear();
        this.holidays.addAll(holidayList);
        
        // Sort by date
        this.holidays.sort(Comparator.comparing(CompanyHoliday::getDate));
        
        refreshViews();
    }
    
    public ObservableList<CompanyHoliday> getHolidays() {
        return holidays;
    }
    
    // Callback setters
    public void setOnEditHoliday(Consumer<CompanyHoliday> onEditHoliday) {
        this.onEditHoliday = onEditHoliday;
    }
    
    public void setOnDeleteHoliday(Consumer<CompanyHoliday> onDeleteHoliday) {
        this.onDeleteHoliday = onDeleteHoliday;
    }
    
    public void setOnAddHoliday(Runnable onAddHoliday) {
        this.onAddHoliday = onAddHoliday;
    }
}