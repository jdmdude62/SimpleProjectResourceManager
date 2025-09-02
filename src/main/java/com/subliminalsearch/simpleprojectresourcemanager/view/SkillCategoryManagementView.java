package com.subliminalsearch.simpleprojectresourcemanager.view;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.util.DialogUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class SkillCategoryManagementView {
    private static final Logger logger = LoggerFactory.getLogger(SkillCategoryManagementView.class);
    
    private final Stage stage;
    private final DataSource dataSource;
    private ListView<String> categoryListView;
    private ObservableList<String> categories;
    private TextArea descriptionArea;
    private Map<String, String> categoryDescriptions;
    
    public SkillCategoryManagementView(Window owner) {
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.initOwner(owner);
        this.stage.setTitle("Skill Category Management");
        
        // Initialize database
        DatabaseConfig dbConfig = new DatabaseConfig();
        this.dataSource = dbConfig.getDataSource();
        this.categoryDescriptions = new HashMap<>();
        
        // Create table and insert defaults if needed
        createCategoriesTableIfNotExists();
        
        // Initialize UI before loading data
        initializeUI();
        
        // Load categories after UI is ready
        loadCategories();
        
        // If still no categories, insert defaults again
        if (categories.isEmpty()) {
            insertDefaultCategories();
            loadCategories();
        }
        
        // Set size
        this.stage.setWidth(800);
        this.stage.setHeight(600);
        
        // Center on owner if available
        if (owner != null) {
            stage.setX(owner.getX() + (owner.getWidth() - 800) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 600) / 2);
        }
    }
    
    private void createCategoriesTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS skill_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                display_order INTEGER DEFAULT 0,
                is_active BOOLEAN DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            
            // Insert default categories if table is empty
            String countSql = "SELECT COUNT(*) FROM skill_categories";
            ResultSet rs = stmt.executeQuery(countSql);
            if (rs.next() && rs.getInt(1) == 0) {
                insertDefaultCategories();
            }
        } catch (SQLException e) {
            logger.error("Failed to create skill_categories table", e);
        }
    }
    
    private void insertDefaultCategories() {
        String[] defaultCategories = {
            "Technical|Core technical skills and competencies",
            "Soft Skills|Communication, teamwork, and interpersonal skills",
            "Management|Leadership and project management capabilities",
            "Safety|Safety procedures and certifications",
            "Equipment Operation|Machinery and equipment operation skills",
            "Specialized|Industry-specific or unique skills"
        };
        
        String sql = "INSERT INTO skill_categories (name, description, display_order) VALUES (?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int order = 0;
            for (String categoryData : defaultCategories) {
                String[] parts = categoryData.split("\\|");
                pstmt.setString(1, parts[0]);
                pstmt.setString(2, parts.length > 1 ? parts[1] : "");
                pstmt.setInt(3, order++);
                pstmt.executeUpdate();
            }
            
            logger.info("Inserted default skill categories");
        } catch (SQLException e) {
            logger.error("Failed to insert default categories", e);
        }
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Header
        Label titleLabel = new Label("Manage Skill Categories");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        Label infoLabel = new Label("Categories help organize skills for easier management and selection");
        infoLabel.setStyle("-fx-text-fill: #666666;");
        
        // Main content split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.5);
        
        // Left side - Category list
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(10));
        
        Label listLabel = new Label("Categories:");
        listLabel.setStyle("-fx-font-weight: bold;");
        
        categoryListView = new ListView<>();
        categories = FXCollections.observableArrayList();
        categoryListView.setItems(categories);
        categoryListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> showCategoryDetails(newVal));
        
        // Toolbar for list
        HBox listToolbar = new HBox(5);
        Button addBtn = new Button("Add");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        Button moveUpBtn = new Button("↑");
        Button moveDownBtn = new Button("↓");
        
        addBtn.setOnAction(e -> addCategory());
        editBtn.setOnAction(e -> editCategory());
        deleteBtn.setOnAction(e -> deleteCategory());
        moveUpBtn.setOnAction(e -> moveCategory(-1));
        moveDownBtn.setOnAction(e -> moveCategory(1));
        
        moveUpBtn.setTooltip(new Tooltip("Move category up"));
        moveDownBtn.setTooltip(new Tooltip("Move category down"));
        
        listToolbar.getChildren().addAll(addBtn, editBtn, deleteBtn, 
            new Separator(), moveUpBtn, moveDownBtn);
        
        VBox.setVgrow(categoryListView, Priority.ALWAYS);
        leftPane.getChildren().addAll(listLabel, categoryListView, listToolbar);
        
        // Right side - Category details
        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(10));
        
        Label detailsLabel = new Label("Category Details:");
        detailsLabel.setStyle("-fx-font-weight: bold;");
        
        descriptionArea = new TextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPromptText("Select a category to view details");
        descriptionArea.setPrefRowCount(5);
        
        // Statistics
        Label statsLabel = new Label("Usage Statistics:");
        statsLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setPrefRowCount(3);
        
        VBox.setVgrow(descriptionArea, Priority.ALWAYS);
        rightPane.getChildren().addAll(detailsLabel, descriptionArea, statsLabel, statsArea);
        
        splitPane.getItems().addAll(leftPane, rightPane);
        
        // Bottom buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button refreshBtn = new Button("Refresh");
        Button resetBtn = new Button("Reset to Defaults");
        Button closeBtn = new Button("Close");
        
        refreshBtn.setOnAction(e -> {
            loadCategories();
            if (categories.isEmpty()) {
                insertDefaultCategories();
                loadCategories();
            }
        });
        resetBtn.setOnAction(e -> resetToDefaults());
        closeBtn.setOnAction(e -> stage.close());
        
        buttonBar.getChildren().addAll(refreshBtn, resetBtn, closeBtn);
        
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        root.getChildren().addAll(titleLabel, infoLabel, splitPane, buttonBar);
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
    }
    
    private void loadCategories() {
        categories.clear();
        categoryDescriptions.clear();
        
        String sql = "SELECT name, description FROM skill_categories WHERE is_active = 1 ORDER BY display_order, name";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String name = rs.getString("name");
                String description = rs.getString("description");
                categories.add(name);
                if (description != null) {
                    categoryDescriptions.put(name, description);
                }
            }
            
            logger.info("Loaded {} skill categories", categories.size());
        } catch (SQLException e) {
            logger.error("Failed to load categories", e);
            showError("Failed to load categories: " + e.getMessage());
        }
    }
    
    private void showCategoryDetails(String categoryName) {
        if (categoryName == null) {
            descriptionArea.clear();
            return;
        }
        
        String description = categoryDescriptions.getOrDefault(categoryName, "No description available");
        descriptionArea.setText(description);
        
        // Could also show statistics like number of skills in this category
        // This would require joining with the skills table
    }
    
    private void resetToDefaults() {
        Alert confirm = DialogUtils.createScreenAwareAlert(Alert.AlertType.CONFIRMATION, stage);
        confirm.setTitle("Reset to Defaults");
        confirm.setHeaderText("Reset Skill Categories");
        confirm.setContentText("This will remove all current categories and restore the default categories. Continue?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Delete all existing categories
                stmt.executeUpdate("DELETE FROM skill_categories");
                
                // Insert defaults
                insertDefaultCategories();
                
                // Reload
                loadCategories();
                
                showInfo("Categories reset to defaults successfully.");
            } catch (SQLException e) {
                logger.error("Failed to reset categories", e);
                showError("Failed to reset categories: " + e.getMessage());
            }
        }
    }
    
    private void addCategory() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Add Skill Category");
        dialog.setHeaderText("Enter new category details");
        dialog.initOwner(stage);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Category name");
        nameField.setPrefWidth(300);
        
        TextArea descArea = new TextArea();
        descArea.setPromptText("Category description (optional)");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        
        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        nameField.textProperty().addListener((obs, old, text) -> 
            saveButton.setDisable(text.trim().isEmpty()));
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new String[] {
                    nameField.getText().trim(),
                    descArea.getText().trim()
                };
            }
            return null;
        });
        
        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                insertCategory(data[0], data[1]);
                loadCategories();
                showInfo("Category added successfully.");
            } catch (Exception e) {
                logger.error("Failed to add category", e);
                showError("Failed to add category: " + e.getMessage());
            }
        });
    }
    
    private void insertCategory(String name, String description) throws SQLException {
        String sql = "INSERT INTO skill_categories (name, description, display_order) " +
                    "VALUES (?, ?, (SELECT COALESCE(MAX(display_order), 0) + 1 FROM skill_categories))";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.executeUpdate();
        }
    }
    
    private void editCategory() {
        String selected = categoryListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a category to edit.");
            return;
        }
        
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Edit Skill Category");
        dialog.setHeaderText("Update category details");
        dialog.initOwner(stage);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField(selected);
        nameField.setPrefWidth(300);
        
        String currentDesc = categoryDescriptions.getOrDefault(selected, "");
        TextArea descArea = new TextArea(currentDesc);
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        
        grid.add(new Label("Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new String[] {
                    selected,  // Original name
                    nameField.getText().trim(),  // New name
                    descArea.getText().trim()  // Description
                };
            }
            return null;
        });
        
        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                updateCategory(data[0], data[1], data[2]);
                loadCategories();
                showInfo("Category updated successfully.");
            } catch (Exception e) {
                logger.error("Failed to update category", e);
                showError("Failed to update category: " + e.getMessage());
            }
        });
    }
    
    private void updateCategory(String oldName, String newName, String description) throws SQLException {
        String sql = "UPDATE skill_categories SET name = ?, description = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE name = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newName);
            pstmt.setString(2, description);
            pstmt.setString(3, oldName);
            pstmt.executeUpdate();
        }
    }
    
    private void deleteCategory() {
        String selected = categoryListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a category to delete.");
            return;
        }
        
        // Check if category is in use
        int skillCount = getSkillCountForCategory(selected);
        
        String message = skillCount > 0 
            ? String.format("Category '%s' is used by %d skill(s). Deleting will NOT remove the skills but they will have no category. Continue?", 
                selected, skillCount)
            : String.format("Are you sure you want to delete category '%s'?", selected);
        
        Alert confirm = DialogUtils.createScreenAwareAlert(Alert.AlertType.CONFIRMATION, stage);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Category");
        confirm.setContentText(message);
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Soft delete - mark as inactive
                String sql = "UPDATE skill_categories SET is_active = 0 WHERE name = ?";
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, selected);
                    pstmt.executeUpdate();
                }
                
                loadCategories();
                showInfo("Category deleted successfully.");
            } catch (Exception e) {
                logger.error("Failed to delete category", e);
                showError("Failed to delete category: " + e.getMessage());
            }
        }
    }
    
    private int getSkillCountForCategory(String category) {
        String sql = "SELECT COUNT(*) FROM skills WHERE category = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count skills for category", e);
        }
        
        return 0;
    }
    
    private void moveCategory(int direction) {
        String selected = categoryListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        int currentIndex = categories.indexOf(selected);
        int newIndex = currentIndex + direction;
        
        if (newIndex < 0 || newIndex >= categories.size()) {
            return;
        }
        
        // Update display order in database
        try {
            updateDisplayOrder(selected, newIndex);
            loadCategories();
            categoryListView.getSelectionModel().select(selected);
        } catch (Exception e) {
            logger.error("Failed to reorder category", e);
            showError("Failed to reorder category: " + e.getMessage());
        }
    }
    
    private void updateDisplayOrder(String categoryName, int newOrder) throws SQLException {
        // This is simplified - in production you'd want to swap orders properly
        String sql = "UPDATE skill_categories SET display_order = ? WHERE name = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, newOrder);
            pstmt.setString(2, categoryName);
            pstmt.executeUpdate();
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