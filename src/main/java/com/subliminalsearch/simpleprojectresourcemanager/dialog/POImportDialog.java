package com.subliminalsearch.simpleprojectresourcemanager.dialog;

import com.subliminalsearch.simpleprojectresourcemanager.service.POSpreadsheetImportService;
import com.subliminalsearch.simpleprojectresourcemanager.service.POSpreadsheetImportService.PORecord;
import com.subliminalsearch.simpleprojectresourcemanager.service.POSpreadsheetImportService.ImportResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.sql.DataSource;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialog for importing and managing Purchase Orders from company spreadsheet
 */
public class POImportDialog extends Stage {
    
    private final POSpreadsheetImportService importService;
    private final ObservableList<PORecord> poRecords = FXCollections.observableArrayList();
    
    private TableView<PORecord> poTable;
    private Label statusLabel;
    private Label lastImportLabel;
    private Label recordCountLabel;
    private ProgressIndicator progressIndicator;
    private Button importButton;
    private Button refreshButton;
    private TextField searchField;
    
    public POImportDialog(DataSource dataSource, Window owner) {
        this.importService = new POSpreadsheetImportService(dataSource);
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Company Purchase Order Import");
        
        initializeUI();
        loadCachedData();
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // Header with connection status
        HBox header = createHeader();
        
        // Control panel
        HBox controls = createControlPanel();
        
        // Table view
        VBox tableSection = createTableSection();
        VBox.setVgrow(tableSection, Priority.ALWAYS);
        
        // Status bar
        HBox statusBar = createStatusBar();
        
        root.getChildren().addAll(header, controls, tableSection, statusBar);
        
        Scene scene = new Scene(root, 1000, 600);
        setScene(scene);
    }
    
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; " +
                        "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label titleLabel = new Label("Company PO Spreadsheet Import");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Connection status indicator
        Label vpnStatusLabel = new Label();
        if (importService.isNetworkAccessible()) {
            vpnStatusLabel.setText("✓ Network Connected");
            vpnStatusLabel.setTextFill(Color.GREEN);
        } else {
            vpnStatusLabel.setText("✗ Network Disconnected");
            vpnStatusLabel.setTextFill(Color.RED);
        }
        vpnStatusLabel.setStyle("-fx-font-weight: bold;");
        
        header.getChildren().addAll(titleLabel, spacer, vpnStatusLabel);
        
        return header;
    }
    
    private HBox createControlPanel() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10, 0, 10, 0));
        
        // Import from network button
        importButton = new Button("Import from Network");
        importButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                             "-fx-font-weight: bold; -fx-padding: 8 15 8 15;");
        importButton.setOnAction(e -> importFromNetwork());
        
        // Import from file button
        Button fileImportButton = new Button("Import from File...");
        fileImportButton.setOnAction(e -> importFromFile());
        
        // Refresh button
        refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadCachedData());
        
        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(30, 30);
        
        // Search field
        Label searchLabel = new Label("Search:");
        searchField = new TextField();
        searchField.setPromptText("PO Number, Vendor, or Project...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTable(newVal));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        controls.getChildren().addAll(
            importButton, fileImportButton, refreshButton, progressIndicator,
            spacer, searchLabel, searchField
        );
        
        return controls;
    }
    
    private VBox createTableSection() {
        VBox section = new VBox(5);
        
        // Record count label
        recordCountLabel = new Label("0 records");
        recordCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        // Create table
        poTable = new TableView<>(poRecords);
        poTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Define columns
        TableColumn<PORecord, String> poNumberCol = new TableColumn<>("PO Number");
        poNumberCol.setCellValueFactory(new PropertyValueFactory<>("poNumber"));
        poNumberCol.setPrefWidth(100);
        
        TableColumn<PORecord, LocalDate> poDateCol = new TableColumn<>("Date");
        poDateCol.setCellValueFactory(new PropertyValueFactory<>("poDate"));
        poDateCol.setPrefWidth(80);
        poDateCol.setCellFactory(col -> new TableCell<PORecord, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
                }
            }
        });
        
        TableColumn<PORecord, String> vendorCol = new TableColumn<>("Vendor");
        vendorCol.setCellValueFactory(new PropertyValueFactory<>("vendor"));
        vendorCol.setPrefWidth(150);
        
        TableColumn<PORecord, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(250);
        
        TableColumn<PORecord, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setPrefWidth(100);
        amountCol.setCellFactory(col -> new TableCell<PORecord, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.2f", amount));
                }
            }
        });
        
        TableColumn<PORecord, String> projectCol = new TableColumn<>("Project");
        projectCol.setCellValueFactory(new PropertyValueFactory<>("projectNumber"));
        projectCol.setPrefWidth(80);
        
        TableColumn<PORecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        
        poTable.getColumns().addAll(
            poNumberCol, poDateCol, vendorCol, descCol, 
            amountCol, projectCol, statusCol
        );
        
        // Context menu for copying PO data
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyPOItem = new MenuItem("Copy PO Number");
        copyPOItem.setOnAction(e -> {
            PORecord selected = poTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.poNumber != null) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(selected.poNumber);
                clipboard.setContent(content);
            }
        });
        
        MenuItem useInProjectItem = new MenuItem("Use in Project Financial Tracking");
        useInProjectItem.setOnAction(e -> {
            PORecord selected = poTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // This will be handled by the parent dialog
                setResult(selected);
                close();
            }
        });
        
        contextMenu.getItems().addAll(copyPOItem, useInProjectItem);
        poTable.setContextMenu(contextMenu);
        
        VBox.setVgrow(poTable, Priority.ALWAYS);
        section.getChildren().addAll(recordCountLabel, poTable);
        
        return section;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; " +
                           "-fx-border-width: 1 0 0 0;");
        
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 11px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        lastImportLabel = new Label();
        lastImportLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        updateLastImportTime();
        
        statusBar.getChildren().addAll(statusLabel, spacer, lastImportLabel);
        
        return statusBar;
    }
    
    private void importFromNetwork() {
        if (!importService.isNetworkAccessible()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Network Not Accessible");
            alert.setHeaderText("Cannot Access Company Network");
            alert.setContentText(
                "The PO spreadsheet on the network drive is not accessible.\n\n" +
                "Please ensure you are:\n" +
                "• Connected to the company VPN\n" +
                "• Or on-site at the office\n\n" +
                "Then try again."
            );
            alert.showAndWait();
            return;
        }
        
        // Run import in background
        Task<ImportResult> importTask = new Task<ImportResult>() {
            @Override
            protected ImportResult call() throws Exception {
                return importService.importPurchaseOrders();
            }
        };
        
        importTask.setOnRunning(e -> {
            progressIndicator.setVisible(true);
            importButton.setDisable(true);
            statusLabel.setText("Importing from network spreadsheet...");
        });
        
        importTask.setOnSucceeded(e -> {
            ImportResult result = importTask.getValue();
            progressIndicator.setVisible(false);
            importButton.setDisable(false);
            
            if (result.success) {
                statusLabel.setText(result.message);
                statusLabel.setTextFill(Color.GREEN);
                loadCachedData();
                updateLastImportTime();
                
                // Show success message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Import Successful");
                alert.setHeaderText("Purchase Orders Imported");
                alert.setContentText(result.message);
                alert.showAndWait();
            } else {
                statusLabel.setText("Import failed: " + result.message);
                statusLabel.setTextFill(Color.RED);
                
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Import Failed");
                alert.setHeaderText("Could Not Import Purchase Orders");
                alert.setContentText(result.message);
                alert.showAndWait();
            }
        });
        
        importTask.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            importButton.setDisable(false);
            statusLabel.setText("Import error: " + importTask.getException().getMessage());
            statusLabel.setTextFill(Color.RED);
        });
        
        Thread importThread = new Thread(importTask);
        importThread.setDaemon(true);
        importThread.start();
    }
    
    private void importFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PO Spreadsheet");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Files", "*.xls", "*.xlsx"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(this);
        if (file != null) {
            // TODO: Implement file-based import
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("File Import");
            alert.setHeaderText("File Import Not Yet Implemented");
            alert.setContentText("This feature will be available in a future update.\n" +
                                "Currently, please use the network import option.");
            alert.showAndWait();
        }
    }
    
    private void loadCachedData() {
        List<PORecord> cached = importService.getCachedPurchaseOrders();
        poRecords.clear();
        poRecords.addAll(cached);
        recordCountLabel.setText(cached.size() + " records");
        statusLabel.setText("Loaded " + cached.size() + " cached PO records");
        statusLabel.setTextFill(Color.BLACK);
    }
    
    private void filterTable(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            loadCachedData();
            return;
        }
        
        String search = searchText.toLowerCase();
        List<PORecord> allRecords = importService.getCachedPurchaseOrders();
        List<PORecord> filtered = allRecords.stream()
            .filter(po -> 
                (po.poNumber != null && po.poNumber.toLowerCase().contains(search)) ||
                (po.vendor != null && po.vendor.toLowerCase().contains(search)) ||
                (po.description != null && po.description.toLowerCase().contains(search)) ||
                (po.projectNumber != null && po.projectNumber.toLowerCase().contains(search))
            )
            .toList();
        
        poRecords.clear();
        poRecords.addAll(filtered);
        recordCountLabel.setText(filtered.size() + " records (filtered)");
    }
    
    private void updateLastImportTime() {
        LocalDateTime lastImport = importService.getLastImportTime();
        if (lastImport != null) {
            lastImportLabel.setText("Last import: " + 
                lastImport.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")));
        } else {
            lastImportLabel.setText("No previous import");
        }
    }
    
    private PORecord selectedRecord;
    
    public void setResult(PORecord record) {
        this.selectedRecord = record;
    }
    
    public PORecord getSelectedPO() {
        return selectedRecord;
    }
}