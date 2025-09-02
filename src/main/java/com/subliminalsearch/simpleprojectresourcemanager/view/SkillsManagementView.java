package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Skill;
import com.subliminalsearch.simpleprojectresourcemanager.repository.SkillRepository;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.geometry.Orientation;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkillsManagementView {
    private static final Logger logger = LoggerFactory.getLogger(SkillsManagementView.class);
    
    private final Stage stage;
    private final SkillRepository repository;
    private final DatabaseConfig dbConfig;
    private TableView<Skill> tableView;
    private ObservableList<Skill> skills;
    private ComboBox<String> categoryFilter;
    
    public SkillsManagementView(Window owner) {
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.initOwner(owner);
        this.stage.setTitle("Skills Management");
        
        // Initialize repository
        this.dbConfig = new DatabaseConfig();
        this.repository = new SkillRepository(dbConfig.getDataSource());
        
        initializeUI();
        loadSkills();
        loadCategories();
        
        // Set size
        this.stage.setWidth(1000);
        this.stage.setHeight(600);
        
        // Center on owner if available
        if (owner != null) {
            stage.setX(owner.getX() + (owner.getWidth() - 1000) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 600) / 2);
        }
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Header
        Label titleLabel = new Label("Manage Skills");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        // Toolbar
        HBox toolbar = new HBox(10);
        Button addBtn = new Button("Add Skill");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        Button refreshBtn = new Button("Refresh");
        
        // Category filter
        Label filterLabel = new Label("Filter by Category:");
        categoryFilter = new ComboBox<>();
        categoryFilter.setPrefWidth(200);
        categoryFilter.setPromptText("All Categories");
        categoryFilter.setOnAction(e -> filterByCategory());
        
        addBtn.setOnAction(e -> showAddDialog());
        editBtn.setOnAction(e -> showEditDialog());
        deleteBtn.setOnAction(e -> deleteSkill());
        refreshBtn.setOnAction(e -> {
            loadSkills();
            loadCategories();
        });
        
        toolbar.getChildren().addAll(addBtn, editBtn, deleteBtn, refreshBtn,
            new Separator(Orientation.VERTICAL), filterLabel, categoryFilter);
        
        // Table
        tableView = new TableView<>();
        skills = FXCollections.observableArrayList();
        tableView.setItems(skills);
        
        // Columns
        TableColumn<Skill, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        
        TableColumn<Skill, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);
        
        TableColumn<Skill, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(350);
        
        TableColumn<Skill, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(200);
        
        TableColumn<Skill, String> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isActive() ? "Yes" : "No"));
        activeCol.setPrefWidth(100);
        
        // Add delete button column
        TableColumn<Skill, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(60);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("🗑");
            
            {
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc3545; -fx-font-size: 16px; -fx-cursor: hand;");
                deleteBtn.setTooltip(new Tooltip("Delete this skill"));
                deleteBtn.setOnAction(event -> {
                    Skill skill = getTableView().getItems().get(getIndex());
                    deleteSkillQuick(skill);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });
        
        tableView.getColumns().addAll(idCol, nameCol, descCol, categoryCol, activeCol, actionCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Enable row selection
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // Double-click to edit
        tableView.setRowFactory(tv -> {
            TableRow<Skill> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showEditDialog();
                }
            });
            return row;
        });
        
        // Layout
        VBox.setVgrow(tableView, Priority.ALWAYS);
        root.getChildren().addAll(titleLabel, toolbar, tableView);
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
    }
    
    private void loadSkills() {
        try {
            skills.clear();
            skills.addAll(repository.findAll());
            logger.info("Loaded {} skills", skills.size());
        } catch (Exception e) {
            logger.error("Failed to load skills", e);
            showError("Failed to load skills: " + e.getMessage());
        }
    }
    
    private void loadCategories() {
        try {
            // First try to load from skill_categories table
            List<String> categories = loadCategoriesFromTable();
            
            // If no categories in table, use repository method
            if (categories.isEmpty()) {
                categories = repository.findAllCategories();
            }
            
            categoryFilter.getItems().clear();
            categoryFilter.getItems().add("All Categories");
            categoryFilter.getItems().addAll(categories);
            
            // Add some default categories if none exist
            if (categories.isEmpty()) {
                categoryFilter.getItems().addAll(
                    "Technical", "Soft Skills", "Management", 
                    "Safety", "Equipment Operation", "Specialized"
                );
            }
        } catch (Exception e) {
            logger.error("Failed to load categories", e);
        }
    }
    
    private List<String> loadCategoriesFromTable() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT name FROM skill_categories WHERE is_active = 1 ORDER BY display_order, name";
        
        try (Connection conn = dbConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                categories.add(rs.getString("name"));
            }
        } catch (Exception e) {
            // Table might not exist yet, that's okay
            logger.debug("Could not load from skill_categories table", e);
        }
        
        return categories;
    }
    
    private void filterByCategory() {
        String selected = categoryFilter.getValue();
        if (selected == null || "All Categories".equals(selected)) {
            loadSkills();
        } else {
            try {
                skills.clear();
                skills.addAll(repository.findByCategory(selected));
            } catch (Exception e) {
                logger.error("Failed to filter by category", e);
                showError("Failed to filter skills: " + e.getMessage());
            }
        }
    }
    
    private void showAddDialog() {
        Dialog<Skill> dialog = new Dialog<>();
        dialog.setTitle("Add Skill");
        dialog.setHeaderText("Enter skill details");
        dialog.initOwner(stage);
        
        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Skill name");
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(3);
        
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setEditable(true);
        categoryCombo.setPromptText("Select or enter category");
        
        // Load categories from database
        List<String> categories = loadCategoriesFromTable();
        if (categories.isEmpty()) {
            // Fallback to defaults if table is empty
            categories.addAll(List.of(
                "Technical", "Soft Skills", "Management", 
                "Safety", "Equipment Operation", "Specialized"
            ));
        }
        categoryCombo.getItems().addAll(categories);
        
        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(true);
        
        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(activeCheck, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Enable/disable save button
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        nameField.textProperty().addListener((obs, old, text) -> 
            saveButton.setDisable(text.trim().isEmpty()));
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Skill skill = new Skill();
                skill.setName(nameField.getText().trim());
                skill.setDescription(descField.getText().trim());
                skill.setCategory(categoryCombo.getValue());
                skill.setActive(activeCheck.isSelected());
                return skill;
            }
            return null;
        });
        
        Optional<Skill> result = dialog.showAndWait();
        result.ifPresent(skill -> {
            try {
                repository.save(skill);
                loadSkills();
                loadCategories();
                showInfo("Skill added successfully.");
            } catch (Exception e) {
                logger.error("Failed to save skill", e);
                showError("Failed to save skill: " + e.getMessage());
            }
        });
    }
    
    private void showEditDialog() {
        Skill selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a skill to edit.");
            return;
        }
        
        Dialog<Skill> dialog = new Dialog<>();
        dialog.setTitle("Edit Skill");
        dialog.setHeaderText("Update skill details");
        dialog.initOwner(stage);
        
        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField(selected.getName());
        TextArea descField = new TextArea(selected.getDescription());
        descField.setPrefRowCount(3);
        
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setEditable(true);
        
        // Load categories from database
        List<String> categories = loadCategoriesFromTable();
        if (categories.isEmpty()) {
            // Fallback to defaults if table is empty
            categories.addAll(List.of(
                "Technical", "Soft Skills", "Management", 
                "Safety", "Equipment Operation", "Specialized"
            ));
        }
        categoryCombo.getItems().addAll(categories);
        categoryCombo.setValue(selected.getCategory());
        
        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(selected.isActive());
        
        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(activeCheck, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Enable/disable save button
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        nameField.textProperty().addListener((obs, old, text) -> 
            saveButton.setDisable(text.trim().isEmpty()));
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selected.setName(nameField.getText().trim());
                selected.setDescription(descField.getText().trim());
                selected.setCategory(categoryCombo.getValue());
                selected.setActive(activeCheck.isSelected());
                return selected;
            }
            return null;
        });
        
        Optional<Skill> result = dialog.showAndWait();
        result.ifPresent(skill -> {
            try {
                repository.save(skill);
                loadSkills();
                loadCategories();
                showInfo("Skill updated successfully.");
            } catch (Exception e) {
                logger.error("Failed to update skill", e);
                showError("Failed to update skill: " + e.getMessage());
            }
        });
    }
    
    private void deleteSkill() {
        Skill selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a skill to delete.");
            return;
        }
        
        Alert confirm = DialogUtils.createScreenAwareAlert(Alert.AlertType.CONFIRMATION, stage);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Skill");
        confirm.setContentText("Are you sure you want to delete '" + selected.getName() + "'?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                repository.delete(selected.getId());
                loadSkills();
                showInfo("Skill deleted successfully.");
            } catch (Exception e) {
                logger.error("Failed to delete skill", e);
                showError("Failed to delete skill: " + e.getMessage());
            }
        }
    }
    
    private void deleteSkillQuick(Skill skill) {
        if (skill == null) {
            return;
        }
        
        // Quick confirmation with simple yes/no
        Alert confirm = DialogUtils.createScreenAwareAlert(Alert.AlertType.CONFIRMATION, stage);
        confirm.setTitle("Delete Skill");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete '" + skill.getName() + "'?");
        
        // Make dialog more compact
        confirm.getDialogPane().setPrefWidth(300);
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                repository.delete(skill.getId());
                loadSkills();
                // No success message for quick delete - just refresh the list
            } catch (Exception e) {
                logger.error("Failed to delete skill", e);
                showError("Failed to delete skill: " + e.getMessage());
            }
        }
    }
    
    private void showInfo(String message) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.INFORMATION, stage);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.WARNING, stage);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.ERROR, stage);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void show() {
        stage.show();
    }
}