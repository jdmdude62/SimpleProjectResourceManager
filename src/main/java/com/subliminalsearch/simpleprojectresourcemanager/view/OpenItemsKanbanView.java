package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.dialog.OpenItemDialog;
import com.subliminalsearch.simpleprojectresourcemanager.model.OpenItem;
import com.subliminalsearch.simpleprojectresourcemanager.service.OpenItemService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OpenItemsKanbanView extends VBox {
    private final OpenItemService openItemService;
    private final HBox kanbanBoard;
    private final ObservableList<OpenItem> items;
    private Long currentProjectId;
    private Runnable onItemsChanged;
    private OpenItem draggedItem; // Track the item being dragged
    
    // Use standard DataFormat.PLAIN_TEXT for better compatibility
    private static final DataFormat OPEN_ITEM_DATA_FORMAT = DataFormat.PLAIN_TEXT;
    private static final String CARD_STYLE = "-fx-background-color: #FFFFFF; -fx-padding: 10; " +
            "-fx-border-color: #333333; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);";
    
    public OpenItemsKanbanView(DatabaseConfig databaseConfig) {
        this.openItemService = new OpenItemService(databaseConfig);
        this.items = FXCollections.observableArrayList();
        this.kanbanBoard = new HBox(10);
        
        setupUI();
        loadData();
    }
    
    private void setupUI() {
        setPadding(new Insets(10));
        setSpacing(10);
        
        // Header
        HBox header = createHeader();
        
        // Kanban board scroll pane
        ScrollPane scrollPane = new ScrollPane(kanbanBoard);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        kanbanBoard.setPadding(new Insets(10));
        kanbanBoard.setSpacing(10);
        kanbanBoard.setFillHeight(true);
        
        // Create columns
        createKanbanColumns();
        
        getChildren().addAll(header, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5));
        
        Label titleLabel = new Label("Open Items Kanban Board");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button addButton = new Button("Add Item");
        addButton.setOnAction(e -> addNewItem());
        
        Button templateButton = new Button("Add from Template");
        templateButton.setOnAction(e -> addFromTemplate());
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadData());
        
        header.getChildren().addAll(titleLabel, spacer, addButton, templateButton, refreshButton);
        
        return header;
    }
    
    private void createKanbanColumns() {
        kanbanBoard.getChildren().clear();
        
        // Create a column for each status
        for (OpenItem.ItemStatus status : OpenItem.ItemStatus.values()) {
            VBox column = createStatusColumn(status);
            kanbanBoard.getChildren().add(column);
            HBox.setHgrow(column, Priority.ALWAYS);
        }
    }
    
    private VBox createStatusColumn(OpenItem.ItemStatus status) {
        VBox column = new VBox(5);
        column.setMinWidth(250);
        column.setPrefWidth(300);
        column.setMaxWidth(400);
        column.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-padding: 10;");
        
        // Column header
        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(status.getDisplayName());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Status color indicator
        String statusColor = getStatusColor(status);
        Region colorBar = new Region();
        colorBar.setPrefHeight(3);
        colorBar.setMaxHeight(3);
        colorBar.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 2;");
        HBox.setHgrow(colorBar, Priority.ALWAYS);
        
        // Count badge
        long count = items.stream().filter(item -> item.getStatus() == status).count();
        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                "-fx-padding: 2 6 2 6; -fx-background-radius: 10; -fx-font-size: 11;");
        
        header.getChildren().addAll(titleLabel, colorBar, countLabel);
        
        // Separator
        Separator separator = new Separator();
        separator.setPrefWidth(200);
        
        // Cards container
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        VBox cardsContainer = new VBox(5);
        cardsContainer.setPadding(new Insets(5, 0, 5, 0));
        cardsContainer.setUserData(status);
        cardsContainer.setMinHeight(200); // Ensure minimum height for drop target
        
        // Enable drop on the container
        setupDropTarget(cardsContainer);
        
        // Add items for this status
        List<OpenItem> statusItems = items.stream()
                .filter(item -> item.getStatus() == status)
                .collect(Collectors.toList());
        
        for (OpenItem item : statusItems) {
            cardsContainer.getChildren().add(createItemCard(item));
        }
        
        scrollPane.setContent(cardsContainer);
        
        column.getChildren().addAll(header, separator, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        return column;
    }
    
    private VBox createItemCard(OpenItem item) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-padding: 8; " +
                "-fx-border-color: black; -fx-border-width: 1; -fx-border-radius: 5;");
        card.setUserData(item);
        card.setCursor(javafx.scene.Cursor.HAND);
        
        // Priority indicator
        HBox priorityBar = new HBox(5);
        priorityBar.setAlignment(Pos.CENTER_LEFT);
        
        // Handle null priority
        if (item.getPriority() != null) {
            Label priorityLabel = new Label(item.getPriority().getDisplayName());
            priorityLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
            priorityLabel.setTextFill(Color.WHITE);
            priorityLabel.setPadding(new Insets(2, 5, 2, 5));
            priorityLabel.setStyle("-fx-background-color: " + getPriorityColor(item.getPriority()) + 
                    "; -fx-background-radius: 3;");
            priorityBar.getChildren().add(priorityLabel);
        }
        
        // Health indicator - handle null health status
        if (item.getHealthStatus() != null) {
            Label healthLabel = new Label(item.getHealthStatus().getDisplayName());
            healthLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
            healthLabel.setTextFill(Color.WHITE);
            healthLabel.setPadding(new Insets(2, 5, 2, 5));
            healthLabel.setStyle("-fx-background-color: " + item.getHealthStatus().getColorCode() + 
                    "; -fx-background-radius: 3;");
            priorityBar.getChildren().add(healthLabel);
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Item number - make it more visible with dark blue color
        Label itemNumber = new Label(item.getItemNumber() != null ? item.getItemNumber() : "NO-ID");
        itemNumber.setFont(Font.font("System", FontWeight.BOLD, 11));
        itemNumber.setTextFill(Color.DARKBLUE);
        itemNumber.setStyle("-fx-text-fill: #000080; -fx-font-weight: bold;");
        
        priorityBar.getChildren().addAll(spacer, itemNumber);
        
        // Title - make it more prominent with explicit black color
        String titleText = (item.getTitle() != null && !item.getTitle().isEmpty()) 
            ? item.getTitle() 
            : "Untitled Item";
        Label titleLabel = new Label(titleText);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setWrapText(true);
        titleLabel.setTextFill(Color.BLACK);
        titleLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-size: 14px;");
        titleLabel.setPrefWidth(240);
        titleLabel.setMaxWidth(240);
        
        // Description with explicit dark gray color
        String descText = (item.getDescription() != null && !item.getDescription().isEmpty()) 
            ? item.getDescription() 
            : "No description";
        Label descLabel = new Label(descText);
        descLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        descLabel.setTextFill(Color.DARKGRAY);
        descLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px;");
        descLabel.setWrapText(true);
        descLabel.setPrefWidth(240);
        descLabel.setMaxWidth(240);
        descLabel.setMaxHeight(50);
        
        // Add all text elements to card
        card.getChildren().addAll(priorityBar, titleLabel, descLabel);
        
        // Progress bar with label
        ProgressBar progressBar = new ProgressBar(item.getProgressPercentage() / 100.0);
        progressBar.setPrefWidth(180);
        progressBar.setPrefHeight(15);
        progressBar.setStyle("-fx-accent: #007bff;");
        
        Label progressLabel = new Label(item.getProgressPercentage() + "% Complete");
        progressLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
        progressLabel.setTextFill(Color.BLACK);
        progressLabel.setStyle("-fx-text-fill: #000000; -fx-font-size: 10px;");
        
        HBox progressBox = new HBox(5);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        progressBox.getChildren().addAll(progressBar, progressLabel);
        
        // Dates
        HBox dateBox = new HBox(10);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        
        if (item.getEstimatedEndDate() != null) {
            Label dueDateLabel = new Label("Due: " + 
                item.getEstimatedEndDate().format(DateTimeFormatter.ofPattern("MMM dd")));
            dueDateLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
            
            if (item.isOverdue()) {
                dueDateLabel.setTextFill(Color.RED);
                dueDateLabel.setStyle("-fx-text-fill: #FF0000; -fx-font-weight: bold;");
            } else if (item.getDaysRemaining() <= 3) {
                dueDateLabel.setTextFill(Color.ORANGE);
                dueDateLabel.setStyle("-fx-text-fill: #FFA500;");
            } else {
                dueDateLabel.setTextFill(Color.GRAY);
                dueDateLabel.setStyle("-fx-text-fill: #666666;");
            }
            
            dateBox.getChildren().add(dueDateLabel);
        }
        
        // Category/Tags
        if (item.getCategory() != null && !item.getCategory().isEmpty()) {
            Label categoryLabel = new Label(item.getCategory());
            categoryLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
            categoryLabel.setTextFill(Color.DARKBLUE);
            categoryLabel.setStyle("-fx-text-fill: #000080; -fx-border-color: #000080; -fx-border-radius: 3; -fx-padding: 2 4 2 4;");
            dateBox.getChildren().add(categoryLabel);
        }
        
        // Add the remaining elements (progressBox and dateBox were not added yet)
        card.getChildren().addAll(progressBox, dateBox);
        
        // Enable drag
        setupDragSource(card);
        
        // Double-click to edit
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                editItem(item);
            }
        });
        
        // Context menu
        ContextMenu contextMenu = createCardContextMenu(item);
        card.setOnContextMenuRequested(e -> contextMenu.show(card, e.getScreenX(), e.getScreenY()));
        
        return card;
    }
    
    private void setupDragSource(VBox card) {
        card.setOnDragDetected(event -> {
            System.out.println("Drag detected on card");
            OpenItem item = (OpenItem) card.getUserData();
            if (item != null) {
                System.out.println("Starting drag for item: " + item.getTitle() + " (ID: " + item.getId() + ")");
                Dragboard dragboard = card.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                
                // Store the item ID as a string for better compatibility
                content.putString(String.valueOf(item.getId()));
                dragboard.setContent(content);
                
                // Store the dragged item for reference
                draggedItem = item;
                
                // Add visual feedback
                card.setOpacity(0.5);
                card.setStyle(CARD_STYLE + "; -fx-border-color: blue; -fx-border-width: 2;");
            }
            event.consume();
        });
        
        card.setOnDragDone(event -> {
            // Clear the dragged item reference and restore opacity
            draggedItem = null;
            card.setOpacity(1.0);
            event.consume();
        });
    }
    
    private void setupDropTarget(VBox container) {
        container.setOnDragOver(event -> {
            // Accept the drag if it's not from the same container and we have a dragged item
            if (event.getDragboard().hasString() && draggedItem != null) {
                // Check if the source is a card (VBox) and not the same container
                if (event.getGestureSource() instanceof VBox && event.getGestureSource() != container) {
                    System.out.println("Accepting drag over container for status: " + container.getUserData());
                    event.acceptTransferModes(TransferMode.MOVE);
                }
            }
            event.consume();
        });
        
        container.setOnDragEntered(event -> {
            if (event.getDragboard().hasString() && draggedItem != null) {
                if (event.getGestureSource() instanceof VBox && event.getGestureSource() != container) {
                    container.setStyle("-fx-background-color: #e8f4fd; -fx-background-radius: 5; -fx-padding: 10; -fx-border-color: #007bff; -fx-border-width: 2; -fx-border-radius: 5;");
                }
            }
            event.consume();
        });
        
        container.setOnDragExited(event -> {
            container.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-padding: 10;");
            event.consume();
        });
        
        container.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            
            if (dragboard.hasString() && draggedItem != null) {
                OpenItem.ItemStatus newStatus = (OpenItem.ItemStatus) container.getUserData();
                
                // Only update if the status is actually changing
                if (draggedItem.getStatus() != newStatus) {
                    draggedItem.setStatus(newStatus);
                    
                    // Update in the service
                    openItemService.updateOpenItem(draggedItem);
                    
                    // Notify that items have changed
                    notifyItemsChanged();
                    
                    // Refresh the board
                    loadData();
                }
                success = true;
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
    }
    
    private ContextMenu createCardContextMenu(OpenItem item) {
        ContextMenu menu = new ContextMenu();
        
        MenuItem editItem = new MenuItem("Edit Item");
        editItem.setOnAction(e -> editItem(item));
        
        MenuItem deleteItem = new MenuItem("Delete Item");
        deleteItem.setOnAction(e -> deleteItem(item));
        
        Menu changeStatus = new Menu("Change Status");
        for (OpenItem.ItemStatus status : OpenItem.ItemStatus.values()) {
            if (status != item.getStatus()) {
                MenuItem statusItem = new MenuItem(status.getDisplayName());
                statusItem.setOnAction(e -> {
                    item.setStatus(status);
                    if (status == OpenItem.ItemStatus.COMPLETED) {
                        openItemService.markAsCompleted(item.getId());
                    } else if (status == OpenItem.ItemStatus.IN_PROGRESS) {
                        openItemService.markAsStarted(item.getId());
                    } else {
                        openItemService.updateOpenItem(item);
                    }
                    loadData();
                });
                changeStatus.getItems().add(statusItem);
            }
        }
        
        MenuItem updateProgress = new MenuItem("Update Progress");
        updateProgress.setOnAction(e -> updateItemProgress(item));
        
        menu.getItems().addAll(editItem, deleteItem, new SeparatorMenuItem(), changeStatus, updateProgress);
        
        return menu;
    }
    
    private void loadData() {
        items.clear();
        if (currentProjectId != null) {
            items.addAll(openItemService.getItemsByProject(currentProjectId));
        } else {
            items.addAll(openItemService.getAllItems());
        }
        createKanbanColumns();
    }
    
    public void setProject(Long projectId) {
        this.currentProjectId = projectId;
        // Refresh the cache before loading data
        openItemService.refreshCache();
        loadData();
    }
    
    private void addNewItem() {
        if (currentProjectId == null) {
            showAlert("No Project Selected", "Please select a project first.");
            return;
        }
        
        OpenItemDialog dialog = new OpenItemDialog(null, currentProjectId, openItemService);
        dialog.showAndWait().ifPresent(item -> {
            // Just reload the data - the service already updated its cache
            loadData();
            notifyItemsChanged();
        });
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
            loadData();
            notifyItemsChanged();
        });
    }
    
    private void deleteItem(OpenItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Open Item");
        confirm.setContentText("Are you sure you want to delete: " + item.getTitle() + "?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                openItemService.deleteOpenItem(item.getId());
                loadData();
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
    
    private String getStatusColor(OpenItem.ItemStatus status) {
        switch (status) {
            case NOT_STARTED: return "#6c757d";
            case IN_PROGRESS: return "#007bff";
            case ON_HOLD: return "#ffc107";
            case BLOCKED: return "#fd7e14";  // Orange for blocked
            case COMPLETED: return "#28a745";
            case CANCELLED: return "#dc3545";
            default: return "#6c757d";
        }
    }
    
    private String getPriorityColor(OpenItem.Priority priority) {
        switch (priority) {
            case HIGH: return "#dc3545";
            case MEDIUM: return "#ffc107";
            case LOW: return "#28a745";
            default: return "#6c757d";
        }
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