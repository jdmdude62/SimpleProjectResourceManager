package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.OpenItemDialog;
import com.subliminalsearch.simpleprojectresourcemanager.model.OpenItem;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.service.OpenItemService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class OpenItemsGridView extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(OpenItemsGridView.class);
    private final OpenItemService openItemService;
    private final TableView<OpenItem> tableView;
    private final ObservableList<OpenItem> items;
    private final ComboBox<Project> projectFilter;
    private final ComboBox<OpenItem.ItemStatus> statusFilter;
    private final ComboBox<OpenItem.HealthStatus> healthFilter;
    private final TextField searchField;
    private final Label summaryLabel;
    private Long currentProjectId;
    private Runnable onItemsChanged;
    
    public OpenItemsGridView(DatabaseConfig databaseConfig) {
        this.openItemService = new OpenItemService(databaseConfig);
        this.items = FXCollections.observableArrayList();
        this.tableView = new TableView<>();
        this.projectFilter = new ComboBox<>();
        this.statusFilter = new ComboBox<>();
        this.healthFilter = new ComboBox<>();
        this.searchField = new TextField();
        this.summaryLabel = new Label();
        
        setupUI();
        loadData();
    }
    
    private void setupUI() {
        setPadding(new Insets(10));
        setSpacing(10);
        
        // Header with filters
        HBox filterBar = createFilterBar();
        
        // Table setup
        setupTable();
        
        // Summary bar
        HBox summaryBar = createSummaryBar();
        
        // Add all components
        getChildren().addAll(filterBar, tableView, summaryBar);
        VBox.setVgrow(tableView, Priority.ALWAYS);
    }
    
    private HBox createFilterBar() {
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(5));
        
        // Search field
        searchField.setPromptText("Search items...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, text) -> filterItems());
        
        // Status filter
        statusFilter.setPromptText("All Statuses");
        statusFilter.getItems().add(null);
        statusFilter.getItems().addAll(OpenItem.ItemStatus.values());
        statusFilter.setConverter(new StringConverter<OpenItem.ItemStatus>() {
            @Override
            public String toString(OpenItem.ItemStatus status) {
                return status == null ? "All Statuses" : status.getDisplayName();
            }
            
            @Override
            public OpenItem.ItemStatus fromString(String string) {
                return null;
            }
        });
        statusFilter.setOnAction(e -> filterItems());
        
        // Health filter
        healthFilter.setPromptText("All Health");
        healthFilter.getItems().add(null);
        healthFilter.getItems().addAll(OpenItem.HealthStatus.values());
        healthFilter.setConverter(new StringConverter<OpenItem.HealthStatus>() {
            @Override
            public String toString(OpenItem.HealthStatus health) {
                return health == null ? "All Health" : health.getDisplayName();
            }
            
            @Override
            public OpenItem.HealthStatus fromString(String string) {
                return null;
            }
        });
        healthFilter.setOnAction(e -> filterItems());
        
        // Action buttons
        Button addButton = new Button("Add Item");
        addButton.setOnAction(e -> addNewItem());
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadData());
        
        Button templateButton = new Button("Add from Template");
        templateButton.setOnAction(e -> addFromTemplate());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        filterBar.getChildren().addAll(
            new Label("Search:"), searchField,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            new Label("Status:"), statusFilter,
            new Label("Health:"), healthFilter,
            spacer,
            addButton, templateButton, refreshButton
        );
        
        return filterBar;
    }
    
    @SuppressWarnings("unchecked")
    private void setupTable() {
        tableView.setEditable(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Item Number column
        TableColumn<OpenItem, String> itemNumberCol = new TableColumn<>("Item #");
        itemNumberCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemNumber()));
        itemNumberCol.setPrefWidth(100);
        
        // Title column
        TableColumn<OpenItem, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        titleCol.setCellFactory(TextFieldTableCell.forTableColumn());
        titleCol.setOnEditCommit(e -> {
            e.getRowValue().setTitle(e.getNewValue());
            openItemService.updateOpenItem(e.getRowValue());
        });
        titleCol.setPrefWidth(200);
        
        // Description column
        TableColumn<OpenItem, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(e -> {
            e.getRowValue().setDescription(e.getNewValue());
            openItemService.updateOpenItem(e.getRowValue());
        });
        descCol.setPrefWidth(250);
        
        // Priority column
        TableColumn<OpenItem, OpenItem.Priority> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getPriority()));
        priorityCol.setCellFactory(ComboBoxTableCell.forTableColumn(OpenItem.Priority.values()));
        priorityCol.setOnEditCommit(e -> {
            e.getRowValue().setPriority(e.getNewValue());
            openItemService.updateOpenItem(e.getRowValue());
        });
        priorityCol.setPrefWidth(80);
        
        // Status column
        TableColumn<OpenItem, OpenItem.ItemStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStatus()));
        statusCol.setCellFactory(column -> new TableCell<OpenItem, OpenItem.ItemStatus>() {
            @Override
            protected void updateItem(OpenItem.ItemStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status.getDisplayName());
                    switch (status) {
                        case COMPLETED:
                            setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                            break;
                        case IN_PROGRESS:
                            setStyle("-fx-background-color: #cce5ff; -fx-text-fill: #004085;");
                            break;
                        case ON_HOLD:
                            setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                            break;
                        case BLOCKED:
                            setStyle("-fx-background-color: #ffeaa7; -fx-text-fill: #d63031; -fx-font-weight: bold;");
                            break;
                        case CANCELLED:
                            setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        statusCol.setPrefWidth(100);
        
        // Progress column
        TableColumn<OpenItem, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setCellValueFactory(data -> 
            new SimpleObjectProperty<>(data.getValue().getProgressPercentage() / 100.0));
        progressCol.setCellFactory(ProgressBarTableCell.forTableColumn());
        progressCol.setPrefWidth(100);
        
        // Health Status column
        TableColumn<OpenItem, OpenItem.HealthStatus> healthCol = new TableColumn<>("Health");
        healthCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getHealthStatus()));
        healthCol.setCellFactory(column -> new TableCell<OpenItem, OpenItem.HealthStatus>() {
            @Override
            protected void updateItem(OpenItem.HealthStatus health, boolean empty) {
                super.updateItem(health, empty);
                if (empty || health == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label label = new Label(health.getDisplayName());
                    label.setTextFill(Color.WHITE);
                    label.setPadding(new Insets(2, 5, 2, 5));
                    label.setStyle("-fx-background-color: " + health.getColorCode() + "; -fx-background-radius: 3;");
                    setGraphic(label);
                }
            }
        });
        healthCol.setPrefWidth(80);
        
        // Dates columns
        TableColumn<OpenItem, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(data -> {
            LocalDate date = data.getValue().getEstimatedStartDate();
            return new SimpleStringProperty(date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
        });
        startDateCol.setPrefWidth(100);
        
        TableColumn<OpenItem, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(data -> {
            LocalDate date = data.getValue().getEstimatedEndDate();
            return new SimpleStringProperty(date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
        });
        endDateCol.setPrefWidth(100);
        
        // Days Remaining column
        TableColumn<OpenItem, Integer> daysCol = new TableColumn<>("Days Rem.");
        daysCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDaysRemaining()));
        daysCol.setCellFactory(column -> new TableCell<OpenItem, Integer>() {
            @Override
            protected void updateItem(Integer days, boolean empty) {
                super.updateItem(days, empty);
                if (empty || days == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(days.toString());
                    if (days < 0) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (days <= 3) {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        daysCol.setPrefWidth(80);
        
        // Add columns to table
        tableView.getColumns().addAll(
            itemNumberCol, titleCol, descCol, priorityCol, statusCol,
            progressCol, healthCol, startDateCol, endDateCol, daysCol
        );
        
        // Context menu
        ContextMenu contextMenu = createContextMenu();
        tableView.setContextMenu(contextMenu);
        
        // Double-click to edit
        tableView.setRowFactory(tv -> {
            TableRow<OpenItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editItem(row.getItem());
                }
            });
            return row;
        });
        
        tableView.setItems(items);
    }
    
    private ContextMenu createContextMenu() {
        ContextMenu menu = new ContextMenu();
        
        MenuItem editItem = new MenuItem("Edit Item");
        editItem.setOnAction(e -> {
            OpenItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) editItem(selected);
        });
        
        MenuItem deleteItem = new MenuItem("Delete Item");
        deleteItem.setOnAction(e -> {
            ObservableList<OpenItem> selected = tableView.getSelectionModel().getSelectedItems();
            if (!selected.isEmpty()) deleteItems(selected);
        });
        
        MenuItem markCompleted = new MenuItem("Mark as Completed");
        markCompleted.setOnAction(e -> {
            ObservableList<OpenItem> selected = tableView.getSelectionModel().getSelectedItems();
            selected.forEach(item -> openItemService.markAsCompleted(item.getId()));
            loadData();
        });
        
        MenuItem markInProgress = new MenuItem("Mark as In Progress");
        markInProgress.setOnAction(e -> {
            ObservableList<OpenItem> selected = tableView.getSelectionModel().getSelectedItems();
            selected.forEach(item -> openItemService.markAsStarted(item.getId()));
            loadData();
        });
        
        MenuItem updateProgress = new MenuItem("Update Progress");
        updateProgress.setOnAction(e -> {
            OpenItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) updateItemProgress(selected);
        });
        
        menu.getItems().addAll(
            editItem, deleteItem,
            new SeparatorMenuItem(),
            markCompleted, markInProgress, updateProgress
        );
        
        return menu;
    }
    
    private HBox createSummaryBar() {
        HBox summaryBar = new HBox(10);
        summaryBar.setAlignment(Pos.CENTER_LEFT);
        summaryBar.setPadding(new Insets(5));
        summaryBar.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 3;");
        
        summaryLabel.setStyle("-fx-font-weight: bold;");
        updateSummary();
        
        summaryBar.getChildren().add(summaryLabel);
        
        return summaryBar;
    }
    
    private void loadData() {
        logger.info("OpenItemsGridView.loadData called, currentProjectId: {}", currentProjectId);
        items.clear();
        if (currentProjectId != null) {
            ObservableList<OpenItem> projectItems = openItemService.getItemsByProject(currentProjectId);
            logger.info("Got {} items for project {}", projectItems.size(), currentProjectId);
            items.addAll(projectItems);
        } else {
            items.addAll(openItemService.getAllItems());
        }
        logger.info("GridView now displaying {} items", items.size());
        updateSummary();
    }
    
    public void setProject(Long projectId) {
        logger.info("OpenItemsGridView.setProject called with projectId: {}", projectId);
        this.currentProjectId = projectId;
        // Refresh the cache before loading data
        openItemService.refreshCache();
        loadData();
    }
    
    private void filterItems() {
        ObservableList<OpenItem> allItems = currentProjectId != null 
            ? openItemService.getItemsByProject(currentProjectId)
            : openItemService.getAllItems();
            
        ObservableList<OpenItem> filtered = FXCollections.observableArrayList();
        
        String searchText = searchField.getText().toLowerCase();
        OpenItem.ItemStatus statusValue = statusFilter.getValue();
        OpenItem.HealthStatus healthValue = healthFilter.getValue();
        
        for (OpenItem item : allItems) {
            boolean matches = true;
            
            // Search filter
            if (!searchText.isEmpty()) {
                matches = item.getTitle().toLowerCase().contains(searchText) ||
                         (item.getDescription() != null && item.getDescription().toLowerCase().contains(searchText)) ||
                         (item.getItemNumber() != null && item.getItemNumber().toLowerCase().contains(searchText));
            }
            
            // Status filter
            if (matches && statusValue != null) {
                matches = item.getStatus() == statusValue;
            }
            
            // Health filter
            if (matches && healthValue != null) {
                matches = item.getHealthStatus() == healthValue;
            }
            
            if (matches) {
                filtered.add(item);
            }
        }
        
        items.clear();
        items.addAll(filtered);
        updateSummary();
    }
    
    private void updateSummary() {
        long total = items.size();
        long completed = items.stream().filter(i -> i.getStatus() == OpenItem.ItemStatus.COMPLETED).count();
        long inProgress = items.stream().filter(i -> i.getStatus() == OpenItem.ItemStatus.IN_PROGRESS).count();
        long overdue = items.stream().filter(OpenItem::isOverdue).count();
        long atRisk = items.stream().filter(i -> i.getHealthStatus() != OpenItem.HealthStatus.ON_TRACK).count();
        
        double avgProgress = items.stream().mapToInt(OpenItem::getProgressPercentage).average().orElse(0);
        
        summaryLabel.setText(String.format(
            "Total: %d | Completed: %d | In Progress: %d | Overdue: %d | At Risk: %d | Avg Progress: %.1f%%",
            total, completed, inProgress, overdue, atRisk, avgProgress
        ));
    }
    
    private void addNewItem() {
        if (currentProjectId == null) {
            showAlert("No Project Selected", "Please select a project first.");
            return;
        }
        
        OpenItemDialog dialog = new OpenItemDialog(null, currentProjectId, openItemService);
        var result = dialog.showAndWait();
        if (result.isPresent()) {
            logger.info("Dialog returned item: {}", result.get());
            // Reload data from database to ensure consistency
            loadData();
            notifyItemsChanged();
        } else {
            logger.info("Dialog was cancelled or returned no item");
        }
    }
    
    private void addFromTemplate() {
        if (currentProjectId == null) {
            showAlert("No Project Selected", "Please select a project first.");
            return;
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>("QUICK_START", 
            Arrays.asList("QUICK_START", "FIELD_SERVICE", "PROJECT_KICKOFF", 
                         "WEEKLY_REVIEW", "INSTALLATION", "PUNCH_LIST"));
        dialog.setTitle("Select Template");
        dialog.setHeaderText("Choose a template to create open items");
        dialog.setContentText("Template:");
        
        dialog.showAndWait().ifPresent(template -> {
            openItemService.createOpenItemsFromTemplate(currentProjectId, template);
            loadData();
            notifyItemsChanged();
        });
    }
    
    private void editItem(OpenItem item) {
        OpenItemDialog dialog = new OpenItemDialog(item, item.getProjectId(), openItemService);
        dialog.showAndWait().ifPresent(updated -> {
            int index = items.indexOf(item);
            if (index >= 0) {
                items.set(index, updated);
            }
            updateSummary();
            notifyItemsChanged();
        });
    }
    
    private void deleteItems(ObservableList<OpenItem> itemsToDelete) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Open Items");
        confirm.setContentText("Are you sure you want to delete " + itemsToDelete.size() + " item(s)?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                itemsToDelete.forEach(item -> openItemService.deleteOpenItem(item.getId()));
                items.removeAll(itemsToDelete);
                updateSummary();
                notifyItemsChanged();
            }
        });
    }
    
    private void updateItemProgress(OpenItem item) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(item.getProgressPercentage()));
        dialog.setTitle("Update Progress");
        dialog.setHeaderText("Update progress for: " + item.getTitle());
        dialog.setContentText("Progress (0-100):");
        
        dialog.showAndWait().ifPresent(value -> {
            try {
                int progress = Integer.parseInt(value);
                openItemService.updateProgress(item.getId(), progress);
                loadData();
                notifyItemsChanged();
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid number between 0 and 100.");
            }
        });
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public void setOnItemsChanged(Runnable callback) {
        this.onItemsChanged = callback;
    }
    
    private void notifyItemsChanged() {
        if (onItemsChanged != null) {
            onItemsChanged.run();
        }
    }
}