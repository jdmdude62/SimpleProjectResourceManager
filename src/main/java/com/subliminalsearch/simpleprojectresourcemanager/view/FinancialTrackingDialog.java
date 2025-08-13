package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.FinancialService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.application.Platform;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FinancialTrackingDialog extends Stage {
    private final Project project;
    private final FinancialService financialService;
    
    // UI Components
    private TabPane tabPane;
    private Label totalBudgetLabel;
    private Label totalSpentLabel;
    private Label remainingLabel;
    private ProgressBar budgetProgress;
    private TextArea summaryTextArea;
    
    // Data
    private ObservableList<PurchaseOrder> purchaseOrders;
    private ObservableList<ActualCost> actualCosts;
    private ObservableList<ChangeOrder> changeOrders;
    
    public FinancialTrackingDialog(Project project, FinancialService financialService) {
        this.project = project;
        this.financialService = financialService;
        
        initializeData();
        setupUI();
        loadData();
    }
    
    private void initializeData() {
        purchaseOrders = FXCollections.observableArrayList();
        actualCosts = FXCollections.observableArrayList();
        changeOrders = FXCollections.observableArrayList();
    }
    
    private void setupUI() {
        setTitle("Financial Tracking - " + project.getProjectId());
        initModality(Modality.APPLICATION_MODAL);
        
        BorderPane root = new BorderPane();
        
        // Top - Budget Summary
        VBox summaryBox = createBudgetSummary();
        root.setTop(summaryBox);
        
        // Center - Tabs
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        Tab budgetTab = createBudgetTab();
        Tab poTab = createPurchaseOrderTab();
        Tab actualTab = createActualCostsTab();
        Tab changeTab = createChangeOrderTab();
        Tab summaryTab = createSummaryTab();
        
        tabPane.getTabs().addAll(budgetTab, poTab, actualTab, changeTab, summaryTab);
        root.setCenter(tabPane);
        
        // Bottom - Action buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setPadding(new Insets(10));
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        
        Button exportButton = new Button("Export to CSV");
        exportButton.setOnAction(e -> exportFinancialData());
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadData());
        
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());
        
        buttonBar.getChildren().addAll(exportButton, refreshButton, closeButton);
        root.setBottom(buttonBar);
        
        // Set scene with larger default size to match typical timeline view
        Scene scene = new Scene(root, 1400, 800);
        setScene(scene);
        
        // Make the dialog resizable
        setResizable(true);
        
        // Set minimum size to ensure usability
        setMinWidth(1200);
        setMinHeight(700);
    }
    
    private VBox createBudgetSummary() {
        VBox summaryBox = new VBox(10);
        summaryBox.setPadding(new Insets(15));
        summaryBox.setStyle("-fx-background-color: #f0f0f0;");
        
        // Title
        Label titleLabel = new Label("Budget Overview");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Budget metrics in a grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(30);
        metricsGrid.setVgap(10);
        metricsGrid.setAlignment(Pos.CENTER);
        
        // Budget
        Label budgetTitle = new Label("Total Budget:");
        budgetTitle.setStyle("-fx-font-weight: bold;");
        totalBudgetLabel = new Label(String.format("$%,.2f", 
            project.getBudgetAmount() != null ? project.getBudgetAmount() : 0.0));
        totalBudgetLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #2196F3;");
        
        // Spent
        Label spentTitle = new Label("Total Spent:");
        spentTitle.setStyle("-fx-font-weight: bold;");
        totalSpentLabel = new Label("$0.00");
        totalSpentLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #F44336;");
        
        // Remaining
        Label remainingTitle = new Label("Remaining:");
        remainingTitle.setStyle("-fx-font-weight: bold;");
        remainingLabel = new Label("$0.00");
        remainingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4CAF50;");
        
        metricsGrid.add(budgetTitle, 0, 0);
        metricsGrid.add(totalBudgetLabel, 0, 1);
        metricsGrid.add(spentTitle, 1, 0);
        metricsGrid.add(totalSpentLabel, 1, 1);
        metricsGrid.add(remainingTitle, 2, 0);
        metricsGrid.add(remainingLabel, 2, 1);
        
        // Progress bar
        budgetProgress = new ProgressBar(0);
        budgetProgress.setPrefWidth(400);
        budgetProgress.setPrefHeight(20);
        
        Label progressLabel = new Label("Budget Utilization");
        progressLabel.setStyle("-fx-font-size: 12px;");
        
        summaryBox.getChildren().addAll(titleLabel, metricsGrid, progressLabel, budgetProgress);
        return summaryBox;
    }
    
    private Tab createBudgetTab() {
        Tab tab = new Tab("Budget & Estimates");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        int row = 0;
        
        // Budget Status
        Label statusLabel = new Label("Budget Status:");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Draft", "Approved", "Locked");
        statusCombo.setValue(project.getBudgetAmount() != null ? "Approved" : "Draft");
        grid.add(statusLabel, 0, row);
        grid.add(statusCombo, 1, row++);
        
        // Budget Amount
        Label budgetLabel = new Label("Total Budget:");
        TextField budgetField = new TextField();
        budgetField.setText(project.getBudgetAmount() != null ? 
            String.format("%.2f", project.getBudgetAmount()) : "");
        budgetField.setPromptText("Enter budget amount");
        grid.add(budgetLabel, 0, row);
        grid.add(budgetField, 1, row++);
        
        // Revenue
        Label revenueLabel = new Label("Expected Revenue:");
        TextField revenueField = new TextField();
        revenueField.setText(project.getRevenueAmount() != null ? 
            String.format("%.2f", project.getRevenueAmount()) : "");
        grid.add(revenueLabel, 0, row);
        grid.add(revenueField, 1, row++);
        
        // Separator
        Separator separator = new Separator();
        grid.add(separator, 0, row++, 2, 1);
        
        // Cost Estimates by Category
        Label estimatesLabel = new Label("Cost Estimates by Category");
        estimatesLabel.setStyle("-fx-font-weight: bold;");
        grid.add(estimatesLabel, 0, row++, 2, 1);
        
        // Labor Cost
        Label laborLabel = new Label("Labor Cost:");
        TextField laborField = new TextField();
        laborField.setText(project.getLaborCost() != null ? 
            String.format("%.2f", project.getLaborCost()) : "");
        grid.add(laborLabel, 0, row);
        grid.add(laborField, 1, row++);
        
        // Material Cost
        Label materialLabel = new Label("Material Cost:");
        TextField materialField = new TextField();
        materialField.setText(project.getMaterialCost() != null ? 
            String.format("%.2f", project.getMaterialCost()) : "");
        grid.add(materialLabel, 0, row);
        grid.add(materialField, 1, row++);
        
        // Travel Cost
        Label travelLabel = new Label("Travel Cost:");
        TextField travelField = new TextField();
        travelField.setText(project.getTravelCost() != null ? 
            String.format("%.2f", project.getTravelCost()) : "");
        grid.add(travelLabel, 0, row);
        grid.add(travelField, 1, row++);
        
        // Other Cost
        Label otherLabel = new Label("Other Cost:");
        TextField otherField = new TextField();
        otherField.setText(project.getOtherCost() != null ? 
            String.format("%.2f", project.getOtherCost()) : "");
        grid.add(otherLabel, 0, row);
        grid.add(otherField, 1, row++);
        
        // Notes
        Label notesLabel = new Label("Notes:");
        TextArea notesArea = new TextArea();
        notesArea.setText(project.getCostNotes() != null ? project.getCostNotes() : "");
        notesArea.setPrefRowCount(3);
        grid.add(notesLabel, 0, row);
        grid.add(notesArea, 1, row++);
        
        // Save button
        Button saveButton = new Button("Save Budget");
        saveButton.setOnAction(e -> {
            try {
                project.setBudgetAmount(parseDouble(budgetField.getText()));
                project.setRevenueAmount(parseDouble(revenueField.getText()));
                project.setLaborCost(parseDouble(laborField.getText()));
                project.setMaterialCost(parseDouble(materialField.getText()));
                project.setTravelCost(parseDouble(travelField.getText()));
                project.setOtherCost(parseDouble(otherField.getText()));
                project.setCostNotes(notesArea.getText());
                
                financialService.updateProjectFinancials(project);
                showInfo("Budget information saved successfully!");
                updateBudgetSummary();
            } catch (Exception ex) {
                showError("Failed to save budget: " + ex.getMessage());
            }
        });
        grid.add(saveButton, 1, row);
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        return tab;
    }
    
    private Tab createPurchaseOrderTab() {
        Tab tab = new Tab("Purchase Orders");
        
        BorderPane pane = new BorderPane();
        
        // Toolbar
        ToolBar toolBar = new ToolBar();
        Button addButton = new Button("New PO");
        addButton.setOnAction(e -> showPurchaseOrderDialog(null));
        
        Button editButton = new Button("Edit");
        Button deleteButton = new Button("Delete");
        Button approveButton = new Button("Approve");
        
        toolBar.getItems().addAll(addButton, editButton, deleteButton, new Separator(), approveButton);
        pane.setTop(toolBar);
        
        // Table
        TableView<PurchaseOrder> table = new TableView<>(purchaseOrders);
        
        TableColumn<PurchaseOrder, String> poNumberCol = new TableColumn<>("PO Number");
        poNumberCol.setCellValueFactory(new PropertyValueFactory<>("poNumber"));
        poNumberCol.setPrefWidth(100);
        
        TableColumn<PurchaseOrder, String> vendorCol = new TableColumn<>("Vendor");
        vendorCol.setCellValueFactory(new PropertyValueFactory<>("vendor"));
        vendorCol.setPrefWidth(150);
        
        TableColumn<PurchaseOrder, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(250);
        
        TableColumn<PurchaseOrder, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(tc -> new TableCell<PurchaseOrder, Double>() {
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
        amountCol.setPrefWidth(100);
        
        TableColumn<PurchaseOrder, PurchaseOrder.POStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);
        
        TableColumn<PurchaseOrder, LocalDate> dateCol = new TableColumn<>("Order Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        dateCol.setPrefWidth(100);
        
        table.getColumns().addAll(poNumberCol, vendorCol, descCol, amountCol, statusCol, dateCol);
        
        editButton.setOnAction(e -> {
            PurchaseOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showPurchaseOrderDialog(selected);
            }
        });
        
        deleteButton.setOnAction(e -> {
            PurchaseOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && confirmDelete("Delete this purchase order?")) {
                purchaseOrders.remove(selected);
                financialService.deletePurchaseOrder(selected.getId());
                updateBudgetSummary();
            }
        });
        
        pane.setCenter(table);
        tab.setContent(pane);
        return tab;
    }
    
    private Tab createActualCostsTab() {
        Tab tab = new Tab("Actual Costs");
        
        BorderPane pane = new BorderPane();
        
        // Toolbar
        ToolBar toolBar = new ToolBar();
        Button addButton = new Button("Add Cost");
        addButton.setOnAction(e -> showActualCostDialog(null));
        
        Button quickEntryButton = new Button("Quick Entry");
        quickEntryButton.setOnAction(e -> showQuickCostEntryDialog());
        
        Button editButton = new Button("Edit");
        Button deleteButton = new Button("Delete");
        Button verifyButton = new Button("Verify");
        
        toolBar.getItems().addAll(addButton, quickEntryButton, new Separator(), 
                                  editButton, deleteButton, verifyButton);
        pane.setTop(toolBar);
        
        // Table
        TableView<ActualCost> table = new TableView<>(actualCosts);
        
        TableColumn<ActualCost, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("costDate"));
        dateCol.setPrefWidth(100);
        
        TableColumn<ActualCost, ActualCost.CostCategory> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(120);
        
        TableColumn<ActualCost, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(250);
        
        TableColumn<ActualCost, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(tc -> new TableCell<ActualCost, Double>() {
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
        amountCol.setPrefWidth(100);
        
        TableColumn<ActualCost, Double> varianceCol = new TableColumn<>("Variance");
        varianceCol.setCellValueFactory(cellData -> {
            ActualCost cost = cellData.getValue();
            Double variance = cost.getVariance();
            return new SimpleDoubleProperty(variance != null ? variance : 0.0).asObject();
        });
        varianceCol.setCellFactory(tc -> new TableCell<ActualCost, Double>() {
            @Override
            protected void updateItem(Double variance, boolean empty) {
                super.updateItem(variance, empty);
                if (empty || variance == null) {
                    setText(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(String.format("$%,.2f", variance));
                    setTextFill(variance > 0 ? Color.RED : Color.GREEN);
                }
            }
        });
        varianceCol.setPrefWidth(100);
        
        TableColumn<ActualCost, ActualCost.CostStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);
        
        TableColumn<ActualCost, String> poCol = new TableColumn<>("Linked PO");
        poCol.setCellValueFactory(cellData -> {
            ActualCost actualCost = cellData.getValue();
            if (actualCost.getPurchaseOrderId() != null) {
                // Find the linked PO
                Optional<PurchaseOrder> linkedPO = purchaseOrders.stream()
                    .filter(po -> po.getId().equals(actualCost.getPurchaseOrderId()))
                    .findFirst();
                if (linkedPO.isPresent()) {
                    return new SimpleStringProperty(linkedPO.get().getPoNumber());
                }
            }
            return new SimpleStringProperty("");
        });
        poCol.setPrefWidth(100);
        
        table.getColumns().addAll(dateCol, categoryCol, descCol, amountCol, varianceCol, statusCol, poCol);
        
        editButton.setOnAction(e -> {
            ActualCost selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showActualCostDialog(selected);
            }
        });
        
        deleteButton.setOnAction(e -> {
            ActualCost selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && confirmDelete("Delete this actual cost record?")) {
                actualCosts.remove(selected);
                if (selected.getId() != null) {
                    financialService.deleteActualCost(selected.getId());
                }
                updateBudgetSummary();
            }
        });
        
        pane.setCenter(table);
        tab.setContent(pane);
        return tab;
    }
    
    private Tab createChangeOrderTab() {
        Tab tab = new Tab("Change Orders");
        
        BorderPane pane = new BorderPane();
        
        // Toolbar
        ToolBar toolBar = new ToolBar();
        Button addButton = new Button("New Change Order");
        addButton.setOnAction(e -> showChangeOrderDialog(null));
        
        Button editButton = new Button("Edit");
        Button deleteButton = new Button("Delete");
        Button submitButton = new Button("Submit for Approval");
        
        toolBar.getItems().addAll(addButton, editButton, deleteButton, new Separator(), submitButton);
        pane.setTop(toolBar);
        
        // Table
        TableView<ChangeOrder> table = new TableView<>(changeOrders);
        
        TableColumn<ChangeOrder, String> numberCol = new TableColumn<>("CO Number");
        numberCol.setCellValueFactory(new PropertyValueFactory<>("changeOrderNumber"));
        numberCol.setPrefWidth(100);
        
        TableColumn<ChangeOrder, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(250);
        
        TableColumn<ChangeOrder, Double> costCol = new TableColumn<>("Additional Cost");
        costCol.setCellValueFactory(new PropertyValueFactory<>("additionalCost"));
        costCol.setCellFactory(tc -> new TableCell<ChangeOrder, Double>() {
            @Override
            protected void updateItem(Double cost, boolean empty) {
                super.updateItem(cost, empty);
                if (empty || cost == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.2f", cost));
                }
            }
        });
        costCol.setPrefWidth(120);
        
        TableColumn<ChangeOrder, ChangeOrder.ChangeReason> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reasonCol.setPrefWidth(150);
        
        TableColumn<ChangeOrder, ChangeOrder.ChangeStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);
        
        table.getColumns().addAll(numberCol, descCol, costCol, reasonCol, statusCol);
        
        // Edit button action
        editButton.setOnAction(e -> {
            ChangeOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showChangeOrderDialog(selected);
            }
        });
        
        // Delete button action
        deleteButton.setOnAction(e -> {
            ChangeOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && confirmDelete("Delete this change order?")) {
                changeOrders.remove(selected);
                if (selected.getId() != null) {
                    financialService.deleteChangeOrder(selected.getId());
                }
                updateBudgetSummary();
            }
        });
        
        // Submit for approval button action
        submitButton.setOnAction(e -> {
            ChangeOrder selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getStatus() == ChangeOrder.ChangeStatus.DRAFT) {
                selected.setStatus(ChangeOrder.ChangeStatus.SUBMITTED);
                financialService.saveChangeOrder(selected);
                table.refresh();
            }
        });
        
        pane.setCenter(table);
        tab.setContent(pane);
        return tab;
    }
    
    private Tab createSummaryTab() {
        Tab tab = new Tab("Summary & Export");
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        // Summary section
        Label summaryLabel = new Label("Financial Summary");
        summaryLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        summaryTextArea = new TextArea();
        summaryTextArea.setEditable(false);
        summaryTextArea.setPrefRowCount(15);
        summaryTextArea.setStyle("-fx-font-family: monospace");
        
        // Refresh button
        Button refreshButton = new Button("Refresh Summary");
        refreshButton.setOnAction(e -> updateFinancialSummary());
        
        // Export section
        Label exportLabel = new Label("Export Options");
        exportLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        HBox exportBox = new HBox(15);
        Button csvButton = new Button("Export to CSV");
        csvButton.setOnAction(e -> exportFinancialData());
        
        Button sageButton = new Button("Export for Sage");
        sageButton.setOnAction(e -> exportForSage());
        
        Button quickBooksButton = new Button("Export for QuickBooks");
        quickBooksButton.setOnAction(e -> exportForQuickBooks());
        
        exportBox.getChildren().addAll(csvButton, sageButton, quickBooksButton);
        
        content.getChildren().addAll(summaryLabel, summaryTextArea, refreshButton, exportLabel, exportBox);
        
        tab.setContent(content);
        
        // Listen for tab selection to refresh summary
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                updateFinancialSummary();
            }
        });
        
        return tab;
    }
    
    private void showPurchaseOrderDialog(PurchaseOrder po) {
        Dialog<PurchaseOrder> dialog = new Dialog<>();
        dialog.setTitle(po == null ? "New Purchase Order" : "Edit Purchase Order");
        dialog.setHeaderText(null);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField poNumberField = new TextField(po != null ? po.getPoNumber() : "");
        TextField vendorField = new TextField(po != null ? po.getVendor() : "");
        TextField descField = new TextField(po != null ? po.getDescription() : "");
        TextField amountField = new TextField(po != null ? String.format("%.2f", po.getAmount()) : "");
        DatePicker orderDatePicker = new DatePicker(po != null ? po.getOrderDate() : LocalDate.now());
        DatePicker expectedDatePicker = new DatePicker(po != null ? po.getExpectedDate() : null);
        ComboBox<PurchaseOrder.POStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(PurchaseOrder.POStatus.values());
        statusCombo.setValue(po != null ? po.getStatus() : PurchaseOrder.POStatus.DRAFT);
        
        grid.add(new Label("PO Number:"), 0, 0);
        grid.add(poNumberField, 1, 0);
        grid.add(new Label("Vendor:"), 0, 1);
        grid.add(vendorField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descField, 1, 2);
        grid.add(new Label("Amount:"), 0, 3);
        grid.add(amountField, 1, 3);
        grid.add(new Label("Order Date:"), 0, 4);
        grid.add(orderDatePicker, 1, 4);
        grid.add(new Label("Expected Date:"), 0, 5);
        grid.add(expectedDatePicker, 1, 5);
        grid.add(new Label("Status:"), 0, 6);
        grid.add(statusCombo, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                PurchaseOrder result = po != null ? po : new PurchaseOrder();
                result.setProjectId(project.getId());
                result.setPoNumber(poNumberField.getText());
                result.setVendor(vendorField.getText());
                result.setDescription(descField.getText());
                result.setAmount(parseDouble(amountField.getText()));
                result.setOrderDate(orderDatePicker.getValue());
                result.setExpectedDate(expectedDatePicker.getValue());
                result.setStatus(statusCombo.getValue());
                return result;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            financialService.savePurchaseOrder(result);
            // Refresh the list to get the updated data with generated IDs
            loadPurchaseOrders();
            updateBudgetSummary();
        });
    }
    
    private void showActualCostDialog(ActualCost cost) {
        Dialog<ActualCost> dialog = new Dialog<>();
        dialog.setTitle(cost == null ? "New Actual Cost" : "Edit Actual Cost");
        dialog.setHeaderText(null);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Date
        DatePicker costDatePicker = new DatePicker(cost != null ? cost.getCostDate() : LocalDate.now());
        
        // Category
        ComboBox<ActualCost.CostCategory> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(ActualCost.CostCategory.values());
        categoryCombo.setValue(cost != null ? cost.getCategory() : ActualCost.CostCategory.MATERIALS);
        
        // Description
        TextField descField = new TextField(cost != null ? cost.getDescription() : "");
        descField.setPrefWidth(300);
        
        // Amount
        TextField amountField = new TextField(cost != null && cost.getAmount() != null ? 
            String.format("%.2f", cost.getAmount()) : "");
        
        // Estimated Amount (for variance calculation)
        TextField estimatedField = new TextField(cost != null && cost.getEstimatedAmount() != null ? 
            String.format("%.2f", cost.getEstimatedAmount()) : "");
        
        // Invoice Number
        TextField invoiceField = new TextField(cost != null ? cost.getInvoiceNumber() : "");
        
        // Receipt Number
        TextField receiptField = new TextField(cost != null ? cost.getReceiptNumber() : "");
        
        // Status
        ComboBox<ActualCost.CostStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(ActualCost.CostStatus.values());
        statusCombo.setValue(cost != null ? cost.getStatus() : ActualCost.CostStatus.PENDING);
        
        // Link to Purchase Order (optional)
        ComboBox<PurchaseOrder> poCombo = new ComboBox<>();
        poCombo.setPromptText("Select Purchase Order (Optional)");
        poCombo.setPrefWidth(300);
        
        // Load POs for this project
        List<PurchaseOrder> projectPOs = financialService.getPurchaseOrdersForProject(project.getId());
        poCombo.getItems().addAll(projectPOs);
        
        // Custom cell factory to display PO information
        poCombo.setCellFactory(param -> new ListCell<PurchaseOrder>() {
            @Override
            protected void updateItem(PurchaseOrder po, boolean empty) {
                super.updateItem(po, empty);
                if (empty || po == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s ($%.2f)", 
                        po.getPoNumber(), po.getVendor(), po.getAmount()));
                }
            }
        });
        
        poCombo.setButtonCell(new ListCell<PurchaseOrder>() {
            @Override
            protected void updateItem(PurchaseOrder po, boolean empty) {
                super.updateItem(po, empty);
                if (empty || po == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s", po.getPoNumber(), po.getVendor()));
                }
            }
        });
        
        // If editing, set the selected PO
        if (cost != null && cost.getPurchaseOrderId() != null) {
            projectPOs.stream()
                .filter(po -> po.getId().equals(cost.getPurchaseOrderId()))
                .findFirst()
                .ifPresent(poCombo::setValue);
        }
        
        // When a PO is selected, auto-fill some fields
        poCombo.setOnAction(e -> {
            PurchaseOrder selectedPO = poCombo.getValue();
            if (selectedPO != null) {
                // Auto-fill vendor info in description if empty
                if (descField.getText().isEmpty()) {
                    descField.setText(selectedPO.getDescription());
                }
                // Auto-fill estimated amount from PO if empty
                if (estimatedField.getText().isEmpty() && selectedPO.getAmount() != null) {
                    estimatedField.setText(String.format("%.2f", selectedPO.getAmount()));
                }
            }
        });
        
        // Notes
        TextArea notesArea = new TextArea(cost != null ? cost.getNotes() : "");
        notesArea.setPrefRowCount(3);
        notesArea.setPrefColumnCount(30);
        
        // Layout
        int row = 0;
        grid.add(new Label("Cost Date:"), 0, row);
        grid.add(costDatePicker, 1, row++);
        
        grid.add(new Label("Category:"), 0, row);
        grid.add(categoryCombo, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(descField, 1, row++);
        
        grid.add(new Label("Actual Amount:"), 0, row);
        grid.add(amountField, 1, row++);
        
        grid.add(new Label("Estimated Amount:"), 0, row);
        grid.add(estimatedField, 1, row++);
        
        grid.add(new Label("Variance:"), 0, row);
        Label varianceLabel = new Label("$0.00");
        grid.add(varianceLabel, 1, row++);
        
        // Update variance when amounts change
        Runnable updateVariance = () -> {
            try {
                Double actual = parseDouble(amountField.getText());
                Double estimated = parseDouble(estimatedField.getText());
                if (actual != null && estimated != null) {
                    double variance = actual - estimated;
                    varianceLabel.setText(String.format("$%.2f", variance));
                    if (variance > 0) {
                        varianceLabel.setTextFill(javafx.scene.paint.Color.RED);
                    } else if (variance < 0) {
                        varianceLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                    } else {
                        varianceLabel.setTextFill(javafx.scene.paint.Color.BLACK);
                    }
                } else {
                    varianceLabel.setText("N/A");
                    varianceLabel.setTextFill(javafx.scene.paint.Color.BLACK);
                }
            } catch (Exception e) {
                varianceLabel.setText("N/A");
                varianceLabel.setTextFill(javafx.scene.paint.Color.BLACK);
            }
        };
        
        amountField.textProperty().addListener((obs, old, newVal) -> updateVariance.run());
        estimatedField.textProperty().addListener((obs, old, newVal) -> updateVariance.run());
        updateVariance.run();
        
        grid.add(new Label("Invoice #:"), 0, row);
        grid.add(invoiceField, 1, row++);
        
        grid.add(new Label("Receipt #:"), 0, row);
        grid.add(receiptField, 1, row++);
        
        grid.add(new Label("Status:"), 0, row);
        grid.add(statusCombo, 1, row++);
        
        grid.add(new Label("Link to PO:"), 0, row);
        grid.add(poCombo, 1, row++);
        
        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                ActualCost result = cost != null ? cost : new ActualCost();
                result.setProjectId(project.getId());
                result.setCostDate(costDatePicker.getValue());
                result.setCategory(categoryCombo.getValue());
                result.setDescription(descField.getText());
                result.setAmount(parseDouble(amountField.getText()));
                result.setEstimatedAmount(parseDouble(estimatedField.getText()));
                result.setInvoiceNumber(invoiceField.getText());
                result.setReceiptNumber(receiptField.getText());
                result.setStatus(statusCombo.getValue());
                result.setNotes(notesArea.getText());
                
                // Set PO link if selected
                PurchaseOrder selectedPO = poCombo.getValue();
                if (selectedPO != null) {
                    result.setPurchaseOrderId(selectedPO.getId());
                } else {
                    result.setPurchaseOrderId(null);
                }
                
                return result;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            financialService.saveActualCost(result);
            // Refresh the list to get the updated data with generated IDs
            loadActualCosts();
            updateBudgetSummary();
        });
    }
    
    private void showChangeOrderDialog(ChangeOrder co) {
        Dialog<ChangeOrder> dialog = new Dialog<>();
        dialog.setTitle(co == null ? "New Change Order" : "Edit Change Order");
        dialog.setHeaderText(null);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // CO Number
        TextField coNumberField = new TextField(co != null ? co.getChangeOrderNumber() : "");
        coNumberField.setPromptText("CO-001");
        
        // Description
        TextArea descArea = new TextArea(co != null ? co.getDescription() : "");
        descArea.setPrefRowCount(3);
        descArea.setPrefColumnCount(30);
        descArea.setPromptText("Describe the change...");
        
        // Reason for change
        ComboBox<ChangeOrder.ChangeReason> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().addAll(ChangeOrder.ChangeReason.values());
        reasonCombo.setValue(co != null ? co.getReason() : ChangeOrder.ChangeReason.CLIENT_REQUEST);
        
        // Additional Cost
        TextField costField = new TextField(co != null && co.getAdditionalCost() != null ? 
            String.format("%.2f", co.getAdditionalCost()) : "");
        costField.setPromptText("0.00");
        
        // Additional Days
        TextField daysField = new TextField(co != null && co.getAdditionalDays() != null ? 
            co.getAdditionalDays().toString() : "");
        daysField.setPromptText("0");
        
        // Request Date
        DatePicker requestDatePicker = new DatePicker(co != null ? co.getRequestDate() : LocalDate.now());
        
        // Requested By
        TextField requestedByField = new TextField(co != null ? co.getRequestedBy() : "");
        requestedByField.setPromptText("Name of requester");
        
        // Status
        ComboBox<ChangeOrder.ChangeStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(ChangeOrder.ChangeStatus.values());
        statusCombo.setValue(co != null ? co.getStatus() : ChangeOrder.ChangeStatus.DRAFT);
        
        // Approval fields (only shown if status is approved)
        Label approvalDateLabel = new Label("Approval Date:");
        DatePicker approvalDatePicker = new DatePicker(co != null ? co.getApprovalDate() : null);
        Label approvedByLabel = new Label("Approved By:");
        TextField approvedByField = new TextField(co != null ? co.getApprovedBy() : "");
        approvedByField.setPromptText("Name of approver");
        
        // Impact Description
        TextArea impactArea = new TextArea(co != null ? co.getImpactDescription() : "");
        impactArea.setPrefRowCount(2);
        impactArea.setPrefColumnCount(30);
        impactArea.setPromptText("Describe the impact on timeline and budget...");
        
        // Layout
        int row = 0;
        grid.add(new Label("CO Number:"), 0, row);
        grid.add(coNumberField, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(descArea, 1, row++);
        
        grid.add(new Label("Reason:"), 0, row);
        grid.add(reasonCombo, 1, row++);
        
        grid.add(new Label("Additional Cost:"), 0, row);
        grid.add(costField, 1, row++);
        
        grid.add(new Label("Additional Days:"), 0, row);
        grid.add(daysField, 1, row++);
        
        grid.add(new Label("Request Date:"), 0, row);
        grid.add(requestDatePicker, 1, row++);
        
        grid.add(new Label("Requested By:"), 0, row);
        grid.add(requestedByField, 1, row++);
        
        grid.add(new Label("Status:"), 0, row);
        grid.add(statusCombo, 1, row++);
        
        // Show/hide approval fields based on status
        int approvalRow = row;
        grid.add(approvalDateLabel, 0, approvalRow);
        grid.add(approvalDatePicker, 1, approvalRow++);
        grid.add(approvedByLabel, 0, approvalRow);
        grid.add(approvedByField, 1, approvalRow++);
        
        // Initially hide approval fields
        boolean showApproval = co != null && 
            (co.getStatus() == ChangeOrder.ChangeStatus.APPROVED || 
             co.getStatus() == ChangeOrder.ChangeStatus.COMPLETED);
        approvalDateLabel.setVisible(showApproval);
        approvalDatePicker.setVisible(showApproval);
        approvedByLabel.setVisible(showApproval);
        approvedByField.setVisible(showApproval);
        
        // Show approval fields when status changes to approved
        statusCombo.setOnAction(e -> {
            boolean shouldShow = statusCombo.getValue() == ChangeOrder.ChangeStatus.APPROVED || 
                               statusCombo.getValue() == ChangeOrder.ChangeStatus.COMPLETED;
            approvalDateLabel.setVisible(shouldShow);
            approvalDatePicker.setVisible(shouldShow);
            approvedByLabel.setVisible(shouldShow);
            approvedByField.setVisible(shouldShow);
            
            // Auto-set approval date if approving
            if (shouldShow && approvalDatePicker.getValue() == null) {
                approvalDatePicker.setValue(LocalDate.now());
            }
        });
        
        row = approvalRow + 2;
        grid.add(new Label("Impact:"), 0, row);
        grid.add(impactArea, 1, row++);
        
        // Calculate total impact summary
        Label impactSummaryLabel = new Label("Impact Summary:");
        impactSummaryLabel.setStyle("-fx-font-weight: bold;");
        Label impactSummaryText = new Label("$0.00 / 0 days");
        
        Runnable updateSummary = () -> {
            try {
                Double additionalCost = parseDouble(costField.getText());
                Integer additionalDays = daysField.getText().isEmpty() ? 0 : Integer.parseInt(daysField.getText());
                impactSummaryText.setText(String.format("$%,.2f / %d days", 
                    additionalCost != null ? additionalCost : 0.0, additionalDays));
                
                // Color code based on impact
                if ((additionalCost != null && additionalCost > 10000) || additionalDays > 7) {
                    impactSummaryText.setTextFill(javafx.scene.paint.Color.RED);
                } else if ((additionalCost != null && additionalCost > 5000) || additionalDays > 3) {
                    impactSummaryText.setTextFill(javafx.scene.paint.Color.ORANGE);
                } else {
                    impactSummaryText.setTextFill(javafx.scene.paint.Color.GREEN);
                }
            } catch (Exception ex) {
                impactSummaryText.setText("Invalid values");
                impactSummaryText.setTextFill(javafx.scene.paint.Color.BLACK);
            }
        };
        
        costField.textProperty().addListener((obs, old, newVal) -> updateSummary.run());
        daysField.textProperty().addListener((obs, old, newVal) -> updateSummary.run());
        updateSummary.run();
        
        grid.add(impactSummaryLabel, 0, row);
        grid.add(impactSummaryText, 1, row++);
        
        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                ChangeOrder result = co != null ? co : new ChangeOrder();
                result.setProjectId(project.getId());
                result.setChangeOrderNumber(coNumberField.getText());
                result.setDescription(descArea.getText());
                result.setReason(reasonCombo.getValue());
                result.setAdditionalCost(parseDouble(costField.getText()));
                
                try {
                    String daysText = daysField.getText().trim();
                    result.setAdditionalDays(daysText.isEmpty() ? null : Integer.parseInt(daysText));
                } catch (NumberFormatException e) {
                    result.setAdditionalDays(null);
                }
                
                result.setRequestDate(requestDatePicker.getValue());
                result.setRequestedBy(requestedByField.getText());
                result.setStatus(statusCombo.getValue());
                result.setApprovalDate(approvalDatePicker.getValue());
                result.setApprovedBy(approvedByField.getText());
                result.setImpactDescription(impactArea.getText());
                
                return result;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            financialService.saveChangeOrder(result);
            // Refresh the list to get the updated data with generated IDs
            loadChangeOrders();
            updateBudgetSummary();
        });
    }
    
    private void showQuickCostEntryDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Quick Cost Entry");
        dialog.setHeaderText("Add multiple costs quickly");
        dialog.setResizable(true);
        
        // Get the actual window size from the stage
        Stage parentStage = (Stage) this.getScene().getWindow();
        double parentWidth = parentStage.getWidth();
        double parentHeight = parentStage.getHeight();
        
        // Set dialog to nearly full parent size
        dialog.setWidth(parentWidth * 0.95);
        dialog.setHeight(parentHeight * 0.9);
        
        // Also set the dialog pane size
        dialog.getDialogPane().setPrefWidth(parentWidth * 0.95);
        dialog.getDialogPane().setPrefHeight(parentHeight * 0.9);
        dialog.getDialogPane().setMinWidth(1200);
        dialog.getDialogPane().setMinHeight(700);
        
        // Center the dialog on parent
        dialog.setX(parentStage.getX() + (parentStage.getWidth() - dialog.getWidth()) / 2);
        dialog.setY(parentStage.getY() + (parentStage.getHeight() - dialog.getHeight()) / 2);
        
        BorderPane mainPane = new BorderPane();
        mainPane.setPadding(new Insets(10));
        
        // Top - Quick entry form
        GridPane entryForm = new GridPane();
        entryForm.setHgap(10);
        entryForm.setVgap(10);
        entryForm.setPadding(new Insets(10));
        
        // Quick entry fields
        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<ActualCost.CostCategory> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(ActualCost.CostCategory.values());
        categoryCombo.setValue(ActualCost.CostCategory.MATERIALS);
        TextField descField = new TextField();
        descField.setPromptText("Description");
        descField.setPrefWidth(300);
        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setPrefWidth(120);
        TextField invoiceField = new TextField();
        invoiceField.setPromptText("Invoice #");
        invoiceField.setPrefWidth(120);
        
        Button addButton = new Button("Add");
        addButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        
        // Layout form
        entryForm.add(new Label("Date:"), 0, 0);
        entryForm.add(datePicker, 1, 0);
        entryForm.add(new Label("Category:"), 2, 0);
        entryForm.add(categoryCombo, 3, 0);
        entryForm.add(new Label("Description:"), 0, 1);
        entryForm.add(descField, 1, 1, 2, 1);
        entryForm.add(new Label("Amount:"), 3, 1);
        entryForm.add(amountField, 4, 1);
        entryForm.add(new Label("Invoice:"), 0, 2);
        entryForm.add(invoiceField, 1, 2);
        entryForm.add(addButton, 2, 2);
        
        // Center - Table showing entered costs
        TableView<ActualCost> tempTable = new TableView<>();
        ObservableList<ActualCost> tempCosts = FXCollections.observableArrayList();
        tempTable.setItems(tempCosts);
        
        TableColumn<ActualCost, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("costDate"));
        dateCol.setPrefWidth(120);
        
        TableColumn<ActualCost, ActualCost.CostCategory> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setPrefWidth(150);
        
        TableColumn<ActualCost, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(400);
        
        TableColumn<ActualCost, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(tc -> new TableCell<ActualCost, Double>() {
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
        amountCol.setPrefWidth(120);
        
        TableColumn<ActualCost, String> invoiceCol = new TableColumn<>("Invoice");
        invoiceCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        invoiceCol.setPrefWidth(150);
        
        TableColumn<ActualCost, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(param -> new TableCell<ActualCost, Void>() {
            private final Button deleteBtn = new Button("Remove");
            {
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 11px;");
                deleteBtn.setOnAction(event -> {
                    ActualCost cost = getTableView().getItems().get(getIndex());
                    tempCosts.remove(cost);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });
        actionCol.setPrefWidth(80);
        
        tempTable.getColumns().addAll(dateCol, catCol, descCol, amountCol, invoiceCol, actionCol);
        
        // Add button action
        addButton.setOnAction(e -> {
            String amountText = amountField.getText().trim();
            String description = descField.getText().trim();
            
            if (!amountText.isEmpty() && !description.isEmpty()) {
                try {
                    ActualCost newCost = new ActualCost();
                    newCost.setProjectId(project.getId());
                    newCost.setCostDate(datePicker.getValue());
                    newCost.setCategory(categoryCombo.getValue());
                    newCost.setDescription(description);
                    newCost.setAmount(parseDouble(amountText));
                    newCost.setInvoiceNumber(invoiceField.getText());
                    newCost.setStatus(ActualCost.CostStatus.PENDING);
                    
                    tempCosts.add(newCost);
                    
                    // Clear fields for next entry
                    descField.clear();
                    amountField.clear();
                    invoiceField.clear();
                    descField.requestFocus();
                    
                } catch (Exception ex) {
                    showError("Invalid amount format");
                }
            }
        });
        
        // Allow Enter key to add
        amountField.setOnAction(e -> addButton.fire());
        invoiceField.setOnAction(e -> addButton.fire());
        
        // Bottom - Summary
        HBox summaryBox = new HBox(20);
        summaryBox.setPadding(new Insets(10));
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setStyle("-fx-background-color: #f0f0f0;");
        
        Label countLabel = new Label("Items: 0");
        Label totalLabel = new Label("Total: $0.00");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Update summary when items change
        tempCosts.addListener((javafx.collections.ListChangeListener<ActualCost>) c -> {
            int count = tempCosts.size();
            double total = tempCosts.stream()
                .mapToDouble(cost -> cost.getAmount() != null ? cost.getAmount() : 0)
                .sum();
            countLabel.setText("Items: " + count);
            totalLabel.setText(String.format("Total: $%,.2f", total));
        });
        
        summaryBox.getChildren().addAll(countLabel, totalLabel);
        
        // Layout
        mainPane.setTop(entryForm);
        mainPane.setCenter(tempTable);
        mainPane.setBottom(summaryBox);
        
        dialog.getDialogPane().setContent(mainPane);
        
        // Buttons
        ButtonType saveAllType = new ButtonType("Save All", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveAllType, ButtonType.CANCEL);
        
        // Save all costs when OK is clicked
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveAllType && !tempCosts.isEmpty()) {
                // Save all costs to database
                int savedCount = 0;
                for (ActualCost cost : tempCosts) {
                    try {
                        financialService.saveActualCost(cost);
                        savedCount++;
                    } catch (Exception ex) {
                        showError("Failed to save cost: " + cost.getDescription());
                    }
                }
                
                if (savedCount > 0) {
                    // Refresh the main actual costs list
                    loadActualCosts();
                    updateBudgetSummary();
                    showInfo(String.format("Successfully saved %d costs", savedCount));
                }
            }
            return null;
        });
        
        dialog.showAndWait();
        
        // Set focus to description field when dialog opens
        Platform.runLater(() -> descField.requestFocus());
    }
    
    private void loadData() {
        // Load data from financial service
        loadPurchaseOrders();
        loadActualCosts();
        loadChangeOrders();
        
        updateBudgetSummary();
    }
    
    private void loadPurchaseOrders() {
        purchaseOrders.clear();
        purchaseOrders.addAll(financialService.getPurchaseOrdersForProject(project.getId()));
    }
    
    private void loadActualCosts() {
        actualCosts.clear();
        actualCosts.addAll(financialService.getActualCostsForProject(project.getId()));
    }
    
    private void loadChangeOrders() {
        changeOrders.clear();
        changeOrders.addAll(financialService.getChangeOrdersForProject(project.getId()));
    }
    
    private void updateBudgetSummary() {
        double budget = project.getBudgetAmount() != null ? project.getBudgetAmount() : 0.0;
        double spent = calculateTotalSpent();
        double remaining = budget - spent;
        
        totalBudgetLabel.setText(String.format("$%,.2f", budget));
        totalSpentLabel.setText(String.format("$%,.2f", spent));
        remainingLabel.setText(String.format("$%,.2f", remaining));
        
        if (remaining < 0) {
            remainingLabel.setTextFill(Color.RED);
        } else if (remaining < budget * 0.1) {
            remainingLabel.setTextFill(Color.ORANGE);
        } else {
            remainingLabel.setTextFill(Color.GREEN);
        }
        
        double progress = budget > 0 ? spent / budget : 0;
        budgetProgress.setProgress(progress);
        
        if (progress > 1.0) {
            budgetProgress.setStyle("-fx-accent: red;");
        } else if (progress > 0.9) {
            budgetProgress.setStyle("-fx-accent: orange;");
        } else {
            budgetProgress.setStyle("-fx-accent: green;");
        }
        
        // Also update the financial summary if it exists
        if (summaryTextArea != null) {
            updateFinancialSummary();
        }
    }
    
    private void updateFinancialSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=" .repeat(60)).append("\n");
        summary.append(String.format("PROJECT: %s - %s\n", project.getProjectId(), project.getDescription()));
        summary.append("=" .repeat(60)).append("\n\n");
        
        // Budget Overview
        double budget = project.getBudgetAmount() != null ? project.getBudgetAmount() : 0.0;
        double laborCost = project.getLaborCost() != null ? project.getLaborCost() : 0.0;
        double materialCost = project.getMaterialCost() != null ? project.getMaterialCost() : 0.0;
        double travelCost = project.getTravelCost() != null ? project.getTravelCost() : 0.0;
        double otherCost = project.getOtherCost() != null ? project.getOtherCost() : 0.0;
        
        summary.append("BUDGET BREAKDOWN:\n");
        summary.append("-" .repeat(40)).append("\n");
        summary.append(String.format("  Total Budget:      $%,12.2f\n", budget));
        summary.append(String.format("  Labor:             $%,12.2f\n", laborCost));
        summary.append(String.format("  Materials:         $%,12.2f\n", materialCost));
        summary.append(String.format("  Travel:            $%,12.2f\n", travelCost));
        summary.append(String.format("  Other:             $%,12.2f\n", otherCost));
        summary.append("\n");
        
        // Purchase Orders Summary
        summary.append("PURCHASE ORDERS:\n");
        summary.append("-" .repeat(40)).append("\n");
        double poTotal = 0;
        int poCount = purchaseOrders.size();
        for (PurchaseOrder po : purchaseOrders) {
            if (po.getAmount() != null) {
                poTotal += po.getAmount();
            }
        }
        summary.append(String.format("  Total POs:         %d\n", poCount));
        summary.append(String.format("  Total Amount:      $%,12.2f\n", poTotal));
        
        // PO Status breakdown
        long poApproved = purchaseOrders.stream().filter(po -> po.getStatus() == PurchaseOrder.POStatus.APPROVED).count();
        long poPending = purchaseOrders.stream().filter(po -> po.getStatus() == PurchaseOrder.POStatus.PENDING).count();
        long poOrdered = purchaseOrders.stream().filter(po -> po.getStatus() == PurchaseOrder.POStatus.ORDERED).count();
        summary.append(String.format("  Approved:          %d\n", poApproved));
        summary.append(String.format("  Pending:           %d\n", poPending));
        summary.append(String.format("  Ordered:           %d\n", poOrdered));
        summary.append("\n");
        
        // Actual Costs Summary
        summary.append("ACTUAL COSTS:\n");
        summary.append("-" .repeat(40)).append("\n");
        double actualTotal = 0;
        int costCount = actualCosts.size();
        for (ActualCost cost : actualCosts) {
            if (cost.getAmount() != null && cost.getStatus() != ActualCost.CostStatus.REJECTED) {
                actualTotal += cost.getAmount();
            }
        }
        summary.append(String.format("  Total Entries:     %d\n", costCount));
        summary.append(String.format("  Total Spent:       $%,12.2f\n", actualTotal));
        
        // Cost category breakdown
        Map<ActualCost.CostCategory, Double> categoryTotals = new HashMap<>();
        for (ActualCost cost : actualCosts) {
            if (cost.getAmount() != null && cost.getStatus() != ActualCost.CostStatus.REJECTED) {
                categoryTotals.merge(cost.getCategory(), cost.getAmount(), Double::sum);
            }
        }
        for (Map.Entry<ActualCost.CostCategory, Double> entry : categoryTotals.entrySet()) {
            if (entry.getKey() != null) {
                summary.append(String.format("  %-15s    $%,12.2f\n", entry.getKey().toString() + ":", entry.getValue()));
            }
        }
        summary.append("\n");
        
        // Change Orders Summary
        summary.append("CHANGE ORDERS:\n");
        summary.append("-" .repeat(40)).append("\n");
        double coTotal = 0;
        int coCount = changeOrders.size();
        int coApprovedCount = 0;
        int additionalDaysTotal = 0;
        for (ChangeOrder co : changeOrders) {
            if (co.getStatus() == ChangeOrder.ChangeStatus.APPROVED || 
                co.getStatus() == ChangeOrder.ChangeStatus.COMPLETED) {
                if (co.getAdditionalCost() != null) {
                    coTotal += co.getAdditionalCost();
                }
                if (co.getAdditionalDays() != null) {
                    additionalDaysTotal += co.getAdditionalDays();
                }
                coApprovedCount++;
            }
        }
        summary.append(String.format("  Total COs:         %d\n", coCount));
        summary.append(String.format("  Approved:          %d\n", coApprovedCount));
        summary.append(String.format("  Additional Cost:   $%,12.2f\n", coTotal));
        summary.append(String.format("  Additional Days:   %d\n", additionalDaysTotal));
        summary.append("\n");
        
        // Final Summary
        summary.append("FINAL SUMMARY:\n");
        summary.append("=" .repeat(40)).append("\n");
        double totalCommitted = poTotal;
        double totalSpent = actualTotal + coTotal;
        double budgetRemaining = budget - totalSpent;
        double percentSpent = budget > 0 ? (totalSpent / budget) * 100 : 0;
        
        summary.append(String.format("  Original Budget:   $%,12.2f\n", budget));
        summary.append(String.format("  POs Committed:     $%,12.2f\n", totalCommitted));
        summary.append(String.format("  Actual Spent:      $%,12.2f\n", actualTotal));
        summary.append(String.format("  Change Orders:     $%,12.2f\n", coTotal));
        summary.append(String.format("  Total Spent:       $%,12.2f\n", totalSpent));
        summary.append(String.format("  Remaining:         $%,12.2f\n", budgetRemaining));
        summary.append(String.format("  Percent Spent:     %.1f%%\n", percentSpent));
        
        if (budgetRemaining < 0) {
            summary.append("\n*** OVER BUDGET BY $").append(String.format("%,.2f", Math.abs(budgetRemaining))).append(" ***\n");
        }
        
        summaryTextArea.setText(summary.toString());
    }
    
    private double calculateTotalSpent() {
        double total = 0;
        
        // Sum up actual costs
        for (ActualCost cost : actualCosts) {
            if (cost.getStatus() != ActualCost.CostStatus.REJECTED) {
                total += cost.getAmount() != null ? cost.getAmount() : 0;
            }
        }
        
        // Add approved change orders
        for (ChangeOrder co : changeOrders) {
            if (co.getStatus() == ChangeOrder.ChangeStatus.APPROVED || 
                co.getStatus() == ChangeOrder.ChangeStatus.COMPLETED) {
                total += co.getAdditionalCost() != null ? co.getAdditionalCost() : 0;
            }
        }
        
        return total;
    }
    
    private void exportFinancialData() {
        // Export to CSV
        // Implementation would create CSV file with all financial data
    }
    
    private void exportForSage() {
        // Export in Sage-compatible format
        // Would need specific formatting based on Sage requirements
    }
    
    private void exportForQuickBooks() {
        // Export in QuickBooks IIF format
        // Would create IIF file with proper formatting
    }
    
    private Double parseDouble(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.replaceAll("[,$]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private boolean confirmDelete(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}